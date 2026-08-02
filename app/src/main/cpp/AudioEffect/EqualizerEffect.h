#pragma once
#include "AudioEffectBase.h"
#include <array>
#include <cmath>
#include <vector>
#include <mutex>
#include <algorithm>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace audiofx {

enum class EqFilterType { Peaking, LowShelf, HighShelf };
enum class CurveInterpolation { Linear, CatmullRom, CubicBezier, StepHold };

struct ControlPoint {
    float frequencyHz;
    float gainDb;
    EqFilterType filterType = EqFilterType::Peaking;
    float Q = 1.0f;
    CurveInterpolation curveIn = CurveInterpolation::CatmullRom;
    CurveInterpolation curveOut = CurveInterpolation::CatmullRom;
};

/**
 * 无极均衡器 — 动态BiQuad池 + 曲线插值 + 增益平滑 + 线程安全
 *
 * 用法：
 *   1. setCurve(points) — 设置控制点（UI线程）
 *   2. process(samples) — 音频处理（音频回调线程）
 *   3. getFrequencyResponse(freq) — 获取频响（UI线程，用于Canvas绘制）
 *   4. getTargetGainAt(freq) — 获取插值目标增益（UI线程，用于预览曲线）
 */
class EqualizerEffect : public AudioEffectBase {
public:
    static constexpr int kDefaultMaxPoints = 16;
    static constexpr float kFreqMin = 10.0f;
    static constexpr float kFreqMax = 24000.0f;
    static constexpr float kGainMin = -24.0f;
    static constexpr float kGainMax = 24.0f;

    EqualizerEffect() { pool_.reserve(kDefaultMaxPoints); }

    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
    }

    // --- 控制点配置（UI线程调用，线程安全） ---

    void setCurve(const std::vector<ControlPoint>& points) {
        std::lock_guard<std::mutex> lock(mutex_);

        for (auto& slot : pool_) slot.active = false;

        while (pool_.size() < points.size()) {
            pool_.emplace_back();
        }

        for (size_t i = 0; i < points.size(); i++) {
            auto& slot = pool_[i];
            const auto& pt = points[i];

            bool wasActive = slot.active;
            bool paramsChanged = !slot.active
                || std::abs(slot.point.frequencyHz - pt.frequencyHz) > 0.5f
                || slot.point.filterType != pt.filterType
                || std::abs(slot.point.Q - pt.Q) > 0.001f;

            slot.point = pt;
            slot.active = true;
            if (!wasActive) slot.smoothedGain = 0.0f;

            if (paramsChanged) {
                slot.dirty = true;
            }
        }

        // Sort active slots by frequency (low → high) for correct cascaded filtering
        std::vector<Slot*> activeSlots;
        for (auto& slot : pool_) {
            if (slot.active) activeSlots.push_back(&slot);
        }
        std::sort(activeSlots.begin(), activeSlots.end(),
            [](const Slot* a, const Slot* b) {
                return a->point.frequencyHz < b->point.frequencyHz;
            });

        // Rebuild pool in sorted order
        std::vector<Slot> sorted;
        sorted.reserve(pool_.size());
        for (auto* s : activeSlots) sorted.push_back(std::move(*s));
        while (sorted.size() < pool_.size()) sorted.emplace_back();
        pool_ = std::move(sorted);
    }

    const std::vector<ControlPoint>& getPoints() const {
        std::lock_guard<std::mutex> lock(mutex_);
        cachedPoints_.clear();
        for (auto& slot : pool_) {
            if (slot.active) cachedPoints_.push_back(slot.point);
        }
        return cachedPoints_;
    }

    // --- 音频处理（音频回调线程，线程安全） ---

    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_) return;
        if (numFrames <= 0 || channels <= 0) return;

        std::lock_guard<std::mutex> lock(mutex_);

        bool anyProcessed = false;
        for (auto& slot : pool_) {
            if (!slot.active) continue;

            float targetGain = slot.point.gainDb;

            // 如果目标增益接近零且已平滑到零，跳过
            if (std::abs(targetGain) < 0.05f && std::abs(slot.smoothedGain) < 0.05f) {
                slot.smoothedGain = 0.0f;
                slot.dirty = false;
                continue;
            }

            // 时序平滑：RC低通平滑增益过渡，防止click/pop
            float prevGain = slot.smoothedGain;
            slot.smoothedGain += smoothingCoeff_ * (targetGain - slot.smoothedGain);

            // 仅在参数变化或增益实际变化时更新系数（避免每帧重算sin/cos/pow）
            bool gainChanged = std::abs(slot.smoothedGain - prevGain) > 0.005f;
            if (slot.dirty || gainChanged) {
                updateFilterCoefs(slot.filter, slot.point, slot.smoothedGain, sampleRate_);
                slot.dirty = false;
            }

            // 级联in-place处理
            slot.filter.process(samples, numFrames, channels);
            anyProcessed = true;
        }

        (void)anyProcessed;
    }

    // --- 频响计算（UI线程，用于双线对比显示） ---

    float getFrequencyResponse(float freq) const {
        std::lock_guard<std::mutex> lock(mutex_);

        // 计算所有活跃BiQuad在频率freq处的级联幅度响应
        float magLinear = 1.0f;
        for (auto& slot : pool_) {
            if (!slot.active) continue;
            float response = slot.filter.magnitudeAtFreq(freq, sampleRate_);
            magLinear *= response;
        }
        return 20.0f * std::log10(std::max(magLinear, 1e-10f));
    }

    /**
     * 获取插值目标增益（依据控制点 curveIn/curveOut 选择算法）
     * 用于UI绘制预览曲线（虚线），而非实际BiQuad响应。
     * 段 [i, i+1] 的算法 = merge(curveOut[i], curveIn[i+1])，取较硬的，
     * 确保 in/out 都能影响曲线形状（旧实现硬编码 CatmullRom，调整无效果）。
     */
    float getTargetGainAt(float targetFreq) const {
        std::lock_guard<std::mutex> lock(mutex_);

        // 收集活跃控制点（pool_ 已按频率升序排列）
        std::vector<ControlPoint> pts;
        for (auto& slot : pool_) {
            if (slot.active) pts.push_back(slot.point);
        }
        if (pts.empty()) return 0.0f;
        if (pts.size() == 1) return pts[0].gainDb;

        targetFreq = std::clamp(targetFreq, kFreqMin, kFreqMax);

        // 找到频率区间
        if (targetFreq <= pts.front().frequencyHz) return pts.front().gainDb;
        if (targetFreq >= pts.back().frequencyHz) return pts.back().gainDb;

        for (size_t i = 0; i < pts.size() - 1; i++) {
            if (targetFreq >= pts[i].frequencyHz && targetFreq <= pts[i + 1].frequencyHz) {
                CurveInterpolation mode = mergeInterpolation(pts[i].curveOut, pts[i + 1].curveIn);
                switch (mode) {
                    case CurveInterpolation::StepHold:
                        return pts[i].gainDb;  // 阶梯保持：段内维持前点增益
                    case CurveInterpolation::Linear: {
                        float t = (targetFreq - pts[i].frequencyHz) /
                                  std::max(pts[i + 1].frequencyHz - pts[i].frequencyHz, 1e-6f);
                        return pts[i].gainDb + t * (pts[i + 1].gainDb - pts[i].gainDb);
                    }
                    case CurveInterpolation::CubicBezier:
                        return cubicHermiteMono(pts, i, targetFreq);  // 单调三次（防过冲）
                    case CurveInterpolation::CatmullRom:
                    default:
                        return catmullRomInterp(pts, i, targetFreq);  // 自然样条（可能过冲）
                }
            }
        }
        return 0.0f;
    }

    void setSmoothingCoeff(float coeff) {
        smoothingCoeff_ = std::clamp(coeff, 0.05f, 0.5f);
    }

    void setMaxPoints(int max) {
        maxPoints_ = std::max(2, max);
    }

    int getMaxPoints() const { return maxPoints_; }

    void clear() override {
        std::lock_guard<std::mutex> lock(mutex_);
        for (auto& slot : pool_) {
            slot.filter.clear();
            slot.active = false;
            slot.smoothedGain = 0.0f;
            slot.dirty = true;
        }
    }

    void setParameter(int paramId, float value) override {
        // 保留兼容接口，实际通过 setCurve() 配置
        (void)paramId;
        (void)value;
    }

    float getParameter(int paramId) const override {
        (void)paramId;
        return 0.0f;
    }

    void setLimiterEnabled(bool enabled) {
        limiterEnabled_ = enabled;
    }

    bool isLimiterEnabled() const { return limiterEnabled_; }

    // 覆盖基类
    EffectType getType() const override { return EffectType::Equalizer; }
    std::string getName() const override { return "Equalizer"; }
    std::string getCategory() const override { return "professional"; }

private:
    // ============ 双二阶滤波器 ============
    struct BiQuad {
        float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f;
        float a1 = 0.0f, a2 = 0.0f;
        float x1[2] = {0, 0}, x2[2] = {0, 0};
        float y1[2] = {0, 0}, y2[2] = {0, 0};

        void clear() {
            x1[0] = x1[1] = x2[0] = x2[1] = 0;
            y1[0] = y1[1] = y2[0] = y2[1] = 0;
        }

        // Transposed Direct Form II — in-place处理
        void process(float* inOut, int numFrames, int channels) {
            for (int i = 0; i < numFrames; i++) {
                for (int c = 0; c < channels; c++) {
                    int idx = i * channels + c;
                    float in = inOut[idx];
                    float out = b0 * in + b1 * x1[c] + b2 * x2[c]
                              - a1 * y1[c] - a2 * y2[c];
                    x2[c] = x1[c];
                    x1[c] = in;
                    y2[c] = y1[c];
                    y1[c] = out;
                    inOut[idx] = out;
                }
            }
        }

        // 计算单个BiQuad在频率f处的幅度响应（线性）
        float magnitudeAtFreq(float freq, int sampleRate) const {
            float w = 2.0f * (float)M_PI * freq / sampleRate;
            float cosW = std::cos(w);
            float sinW = std::sin(w);

            // H(z) = (b0 + b1*z^-1 + b2*z^-2) / (1 + a1*z^-1 + a2*z^-2)
            // |H(w)| = |num| / |den|
            float numRe = b0 + b1 * cosW + b2 * std::cos(2.0f * w);
            float numIm = -(b1 * sinW + b2 * std::sin(2.0f * w));
            float denRe = 1.0f + a1 * cosW + a2 * std::cos(2.0f * w);
            float denIm = -(a1 * sinW + a2 * std::sin(2.0f * w));

            float numMag = std::sqrt(numRe * numRe + numIm * numIm);
            float denMag = std::sqrt(denRe * denRe + denIm * denIm);

            return denMag > 1e-12f ? numMag / denMag : numMag;
        }
    };

    // ============ 池槽位 ============
    struct Slot {
        BiQuad filter;
        bool active = false;
        bool dirty = true;
        ControlPoint point;
        float smoothedGain = 0.0f;
    };

    // ============ 滤波器系数计算 ============
    static void updateFilterCoefs(BiQuad& f, const ControlPoint& pt, float gainDb, int sampleRate) {
        // 频率钳位: 不超过 0.48 * sampleRate，留出安全余量避免接近 Nyquist 时
        // sin(w0) 变负导致滤波器系数异常（产生 NaN/静音）
        float clampedFreq = std::min(pt.frequencyHz, sampleRate * 0.48f);
        float A = std::pow(10.0f, gainDb / 40.0f);
        float w0 = 2.0f * (float)M_PI * clampedFreq / sampleRate;
        float cosW0 = std::cos(w0);

        switch (pt.filterType) {
            case EqFilterType::Peaking: {
                float alpha = std::sin(w0) / (2.0f * pt.Q);
                float a0Inv = 1.0f / (1.0f + alpha / A);
                f.b0 = (1.0f + alpha * A) * a0Inv;
                f.b1 = -2.0f * cosW0 * a0Inv;
                f.b2 = (1.0f - alpha * A) * a0Inv;
                f.a1 = f.b1;
                f.a2 = (1.0f - alpha / A) * a0Inv;
                break;
            }
            case EqFilterType::LowShelf: {
                float S = pt.Q > 0.01f ? 1.0f / pt.Q : 0.7f;
                float alpha = std::sin(w0) / 2.0f * std::sqrt((A + 1.0f / A) * (1.0f / S - 1.0f) + 2.0f);
                float sqrtA2 = 2.0f * std::sqrt(A) * alpha;
                float a0Inv = 1.0f / ((A + 1.0f) + (A - 1.0f) * cosW0 + sqrtA2);
                f.b0 = A * ((A + 1.0f) - (A - 1.0f) * cosW0 + sqrtA2) * a0Inv;
                f.b1 = 2.0f * A * ((A - 1.0f) - (A + 1.0f) * cosW0) * a0Inv;
                f.b2 = A * ((A + 1.0f) - (A - 1.0f) * cosW0 - sqrtA2) * a0Inv;
                f.a1 = -2.0f * ((A - 1.0f) + (A + 1.0f) * cosW0) * a0Inv;
                f.a2 = ((A + 1.0f) + (A - 1.0f) * cosW0 - sqrtA2) * a0Inv;
                break;
            }
            case EqFilterType::HighShelf: {
                float S = pt.Q > 0.01f ? 1.0f / pt.Q : 0.7f;
                float alpha = std::sin(w0) / 2.0f * std::sqrt((A + 1.0f / A) * (1.0f / S - 1.0f) + 2.0f);
                float sqrtA2 = 2.0f * std::sqrt(A) * alpha;
                float a0Inv = 1.0f / ((A + 1.0f) - (A - 1.0f) * cosW0 + sqrtA2);
                f.b0 = A * ((A + 1.0f) + (A - 1.0f) * cosW0 + sqrtA2) * a0Inv;
                f.b1 = -2.0f * A * ((A - 1.0f) + (A + 1.0f) * cosW0) * a0Inv;
                f.b2 = A * ((A + 1.0f) + (A - 1.0f) * cosW0 - sqrtA2) * a0Inv;
                f.a1 = 2.0f * ((A - 1.0f) - (A + 1.0f) * cosW0) * a0Inv;
                f.a2 = ((A + 1.0f) - (A - 1.0f) * cosW0 - sqrtA2) * a0Inv;
                break;
            }
        }
    }

    // ============ 插值类型合并：取较硬的（StepHold > Linear > CubicBezier > CatmullRom） ============
    // 段 [i, i+1] 的算法 = merge(curveOut[i], curveIn[i+1])。
    // 硬算法（StepHold/Linear）是"约束"，软算法（Cat/Cub）是"自由"，
    // 任一端要求硬约束即采用硬算法，确保 in/out 都能影响曲线形状。
    static CurveInterpolation mergeInterpolation(CurveInterpolation out, CurveInterpolation inn) {
        auto hardness = [](CurveInterpolation m) -> int {
            switch (m) {
                case CurveInterpolation::StepHold:    return 3;
                case CurveInterpolation::Linear:      return 2;
                case CurveInterpolation::CubicBezier: return 1;
                case CurveInterpolation::CatmullRom:  return 0;
            }
            return 0;
        };
        return hardness(out) >= hardness(inn) ? out : inn;
    }

    // ============ 单调三次 Hermite 插值（Fritsch-Carlson 单调性约束，防过冲/振铃） ============
    // 与 CatmullRom（自然样条，可能过冲）区分：CubicBezier 更平稳，不产生超出控制点范围的振荡。
    // 切线由相邻点差分计算，斜率变号处切线归零，保证单调段不过冲。
    static float cubicHermiteMono(const std::vector<ControlPoint>& pts, size_t seg, float x) {
        size_t n = pts.size();
        float x1 = pts[seg].frequencyHz, y1 = pts[seg].gainDb;
        float x2 = pts[seg + 1].frequencyHz, y2 = pts[seg + 1].gainDb;
        float dx = x2 - x1;
        if (dx <= 0.0f) return y1;
        float t = (x - x1) / dx;

        // 相邻段斜率
        float dPrev = (seg > 0)
            ? (y1 - pts[seg - 1].gainDb) / std::max(x1 - pts[seg - 1].frequencyHz, 1e-6f)
            : (y2 - y1) / dx;
        float dNext = (seg + 2 < n)
            ? (pts[seg + 2].gainDb - y2) / std::max(pts[seg + 2].frequencyHz - x2, 1e-6f)
            : (y2 - y1) / dx;
        float dCur = (y2 - y1) / dx;

        // 单调性约束（Fritsch-Carlson）：斜率变号处切线归零；同号时切线不超过 3·min(|d₋|,|d₊|)，
        // 确保单调段绝不产生超出控制点范围的过冲/振铃（CatmullRom 无此约束，可能过冲）。
        auto monotoneTangent = [](float dLo, float dHi) -> float {
            if (dLo * dHi <= 0.0f) return 0.0f;
            float m = (dLo + dHi) * 0.5f;
            float cap = 3.0f * std::min(std::fabs(dLo), std::fabs(dHi));
            return std::fabs(m) > cap ? std::copysign(cap, m) : m;
        };
        float m1 = monotoneTangent(dPrev, dCur);
        float m2 = monotoneTangent(dCur, dNext);

        // 三次 Hermite 基函数（切线需乘段宽 dx，因 m 是关于 x 的导数）
        float t2 = t * t, t3 = t2 * t;
        float h00 = 2.0f * t3 - 3.0f * t2 + 1.0f;
        float h10 = t3 - 2.0f * t2 + t;
        float h01 = -2.0f * t3 + 3.0f * t2;
        float h11 = t3 - t2;
        return h00 * y1 + h10 * (m1 * dx) + h01 * y2 + h11 * (m2 * dx);
    }

    // ============ Catmull-Rom样条插值 ============
    static float catmullRomInterp(const std::vector<ControlPoint>& pts, size_t seg, float x) {
        float x0, x1, x2, x3, y0, y1, y2, y3;

        x1 = pts[seg].frequencyHz;
        y1 = pts[seg].gainDb;
        x2 = pts[seg + 1].frequencyHz;
        y2 = pts[seg + 1].gainDb;

        if (seg > 0) {
            x0 = pts[seg - 1].frequencyHz;
            y0 = pts[seg - 1].gainDb;
        } else {
            x0 = x1 - (x2 - x1);
            y0 = y1;
        }

        if (seg + 2 < pts.size()) {
            x3 = pts[seg + 2].frequencyHz;
            y3 = pts[seg + 2].gainDb;
        } else {
            x3 = x2 + (x2 - x1);
            y3 = y2;
        }

        float t = (x - x1) / (x2 - x1);
        float t2 = t * t;
        float t3 = t2 * t;

        return 0.5f * (
            (2.0f * y1) +
            (-y0 + y2) * t +
            (2.0f * y0 - 5.0f * y1 + 4.0f * y2 - y3) * t2 +
            (-y0 + 3.0f * y1 - 3.0f * y2 + y3) * t3
        );
    }

    // ============ 成员变量 ============
    std::vector<Slot> pool_;
    mutable std::mutex mutex_;
    int sampleRate_ = 44100;
    float smoothingCoeff_ = 0.25f;
    bool limiterEnabled_ = true;
    int maxPoints_ = kDefaultMaxPoints;
    mutable std::vector<ControlPoint> cachedPoints_;
};

} // namespace audiofx
