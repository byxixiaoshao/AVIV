#ifndef VIRTUAL_BASS_H
#define VIRTUAL_BASS_H

#include "AudioEffectBase.h"
#include <cmath>
#include <algorithm>

namespace audiofx {

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

/**
 * 虚拟低音 (Virtual Bass) - 基于 FFmpeg af_virtualbass.c 移植与优化
 *
 * 原理：利用"缺失基频"(missing fundamental) 心理声学效应，通过非线性器件(NLD)
 *       在低频处生成谐波，使小型扬声器无法重放的低频在听感上被"补全"。
 *
 * 信号链：
 *   (L+R)/2 → 二阶Butterworth低通(SVF) → 软NLD谐波生成 → 带通滤波 →
 *   软膝限幅 → DC阻断 → 混合回 L/R
 *
 * 相对 FFmpeg 原版的优化（解决伪还原爆音）：
 *   1. 软NLD：保留 atan+sqrt 结构，钳位输入避免 sqrt 产生 NaN；
 *      输出端追加软膝限幅，避免硬削波点击声。
 *   2. 带通滤波：~150Hz 带通去除 NLD 产生的带外互调失真(IMD)。
 *   3. 软膝限幅：NLD 输出后软压缩，抑制瞬态过冲。
 *   4. DC阻断：一阶 DC 阻断器(20Hz 高通)滤除 NLD 直流偏移。
 *   5. 平滑过渡：intensity 一阶平滑，避免滑块拖动 zipper 噪声。
 *   6. 保守混合增益：最大 0.15（旧实现可达 2.1，是爆音根因）。
 *   7. 强度反向映射：UI intensity(0..3) → FFmpeg strength(3..0.5)，
 *      使"高强度 = 更强虚拟低音"符合用户直觉。
 */
class VirtualBass : public AudioEffectBase {
public:
    VirtualBass() : intensity_(0.0f), sampleRate_(44100) {}

    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        updateLowpass(250.0f);                          // FFmpeg 默认 cutoff=250Hz
        bandpass_.design(sampleRate_, 150.0f, 0.7f);    // 带通中心 150Hz
        dcBlocker_.design(sampleRate_);                 // DC 阻断 20Hz
        clear();
    }

    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || intensity_ < 0.01f) return;
        int ch = std::min(channels, 2);

        // intensity 一阶平滑（~25ms 时间常数），避免参数跳变产生 zipper 噪声
        const float kSmooth = 0.999f;

        for (int i = 0; i < numFrames; i++) {
            smoothIntensity_ += (intensity_ - smoothIntensity_) * (1.0f - kSmooth);
            if (smoothIntensity_ < 0.01f) continue;  // 淡入期不处理，直通

            // 反向映射：UI intensity(0..3) → FFmpeg strength(3..0.5)
            // 高强度 → 低 strength → 大 st → sin 更多 wrapping → 更丰富谐波
            float ffStrength = 3.5f - smoothIntensity_;
            if (ffStrength > 3.0f) ffStrength = 3.0f;
            else if (ffStrength < 0.5f) ffStrength = 0.5f;
            const float st = static_cast<float>(M_PI) / ffStrength;

            // 取中心声道 (L+R)/2 作为低频提取源
            float l = samples[i * channels + 0];
            float r = (ch > 1) ? samples[i * channels + 1] : l;
            float center = (l + r) * 0.5f;

            // Step 1: 二阶 Butterworth 低通（SVF 拓扑，移植自 FFmpeg vb_stereo）
            // 拓扑：v3 = v0 - b1; v1 = a0*b0 + a1*v3; v2 = b1 + a1*b0 + a2*v3;
            //       b0 = 2*v1 - b0; b1 = 2*v2 - b1; 输出 = v2 (m[2]=1)
            float v0 = center;
            float v3 = v0 - lpB1_;
            float v1 = lpA0_ * lpB0_ + lpA1_ * v3;
            float v2 = lpB1_ + lpA1_ * lpB0_ + lpA2_ * v3;
            lpB0_ = 2.0f * v1 - lpB0_;
            lpB1_ = 2.0f * v2 - lpB1_;
            float bass = v2;

            // Step 2: 软 NLD 谐波生成
            // FFmpeg 原版：vb_fun(x) = 2.5*atan(0.9*x) + 2.5*sqrt(1-(0.9*x)^2) - 2.5
            // 改进：钳位 0.9*x 到 [-1,1] 避免 sqrt(负数) 产生 NaN
            float xn = 0.9f * bass;
            if (xn > 1.0f) xn = 1.0f;
            else if (xn < -1.0f) xn = -1.0f;
            float y = 2.5f * std::atan(xn) + 2.5f * std::sqrt(1.0f - xn * xn) - 2.5f;
            float nld = (y < 0.0f) ? std::sin(y) : y;
            float vb = std::sin(nld * st);  // FFmpeg 最终包络

            // Step 3: 带通滤波，去除带外互调失真
            vb = bandpass_.process(vb);

            // Step 4: 软膝限幅，抑制 NLD 瞬态过冲
            vb = softKneeLimit(vb, 0.6f, 0.9f);

            // Step 5: DC 阻断（20Hz 高通），滤除 NLD 直流偏移
            vb = dcBlocker_.process(vb);

            // Step 6: 混合回 L/R
            // mix 增益随 intensity 线性增长，最大 0.5
            // （旧值 0.15 太保守，intensity=0.3 时 mix 仅 0.015，几乎听不到效果）
            // 过载由 softKneeLimit + stage3 的 effectsLimiter_ 兜底
            float mix = (smoothIntensity_ / 3.0f) * 0.5f;
            float wet = vb * mix;
            samples[i * channels + 0] = l + wet;
            if (ch > 1) {
                samples[i * channels + 1] = r + wet;
            }
        }
    }

    void clear() override {
        lpB0_ = 0.0f;
        lpB1_ = 0.0f;
        bandpass_.clear();
        dcBlocker_.clear();
        smoothIntensity_ = 0.0f;  // 淡入，避免启用瞬间爆音
    }

    // 重新启用时重置滤波器状态，避免陈旧状态导致点击声
    void setEnabled(bool enabled) override {
        if (enabled && !enabled_) {
            lpB0_ = 0.0f;
            lpB1_ = 0.0f;
            bandpass_.clear();
            dcBlocker_.clear();
            smoothIntensity_ = 0.0f;
        }
        enabled_ = enabled;
    }

    void setParameter(int paramId, float value) override {
        if (paramId == 0) {
            // UI 传 0..3 强度
            intensity_ = std::clamp(value, 0.0f, 3.0f);
        }
    }

    float getParameter(int paramId) const override {
        return paramId == 0 ? intensity_ : 0.0f;
    }

    EffectType getType() const override { return EffectType::VirtualBass; }
    std::string getName() const override { return "虚拟低音"; }
    std::string getCategory() const override { return "音质增强"; }

private:
    // 二阶 Butterworth 低通系数计算（SVF 拓扑，移植自 FFmpeg config_input）
    void updateLowpass(float cutoff) {
        const float Q = 0.707f;
        float g = std::tan(static_cast<float>(M_PI) * cutoff / sampleRate_);
        float k = 1.0f / Q;
        lpA0_ = 1.0f / (1.0f + g * (g + k));
        lpA1_ = g * lpA0_;
        lpA2_ = g * lpA1_;
        // m[0]=0, m[1]=0, m[2]=1 → 输出取 v2
    }

    // 软膝限幅：|x| <= thresh 直通；[thresh, limit] 内 tanh 平滑压缩
    static float softKneeLimit(float x, float thresh, float limit) {
        float absX = std::abs(x);
        if (absX <= thresh) return x;
        float sign = (x >= 0.0f) ? 1.0f : -1.0f;
        float t = (absX - thresh) / (limit - thresh);
        if (t > 1.0f) t = 1.0f;
        float shaped = thresh + (limit - thresh) * std::tanh(t);
        return sign * shaped;
    }

    // 双二阶滤波器（用于带通）
    struct Biquad {
        float b0_ = 1.0f, b1_ = 0.0f, b2_ = 0.0f;
        float a1_ = 0.0f, a2_ = 0.0f;  // a0 归一化为 1
        float z1_ = 0.0f, z2_ = 0.0f;

        // BPF (constant 0 dB peak gain)
        void design(int sampleRate, float freq, float Q) {
            float w0 = 2.0f * static_cast<float>(M_PI) * freq / sampleRate;
            float cosW = std::cos(w0);
            float sinW = std::sin(w0);
            float alpha = sinW / (2.0f * Q);
            float a0 = 1.0f + alpha;
            b0_ = alpha / a0;
            b1_ = 0.0f;
            b2_ = -alpha / a0;
            a1_ = -2.0f * cosW / a0;
            a2_ = (1.0f - alpha) / a0;
        }

        // Direct Form I Transposed（数值稳定）
        float process(float x) {
            float y = b0_ * x + z1_;
            z1_ = b1_ * x - a1_ * y + z2_;
            z2_ = b2_ * x - a2_ * y;
            return y;
        }

        void clear() { z1_ = 0.0f; z2_ = 0.0f; }
    };

    // 一阶 DC 阻断器（高通，fc≈20Hz）
    struct DCBlocker {
        float r_ = 0.997f;
        float prevIn_ = 0.0f;
        float prevOut_ = 0.0f;

        void design(int sampleRate) {
            r_ = 1.0f - std::min(1.0f, 2.0f * static_cast<float>(M_PI) * 20.0f / sampleRate);
        }

        float process(float x) {
            float y = x - prevIn_ + r_ * prevOut_;
            prevIn_ = x;
            prevOut_ = y;
            return y;
        }

        void clear() { prevIn_ = 0.0f; prevOut_ = 0.0f; }
    };

    float intensity_;
    float smoothIntensity_ = 0.0f;  // 平滑后的 intensity（用于 process）
    int sampleRate_;

    // 低通滤波器系数与状态（FFmpeg SVF 拓扑，对应 a[3] / cf[2]）
    float lpA0_ = 1.0f, lpA1_ = 0.0f, lpA2_ = 0.0f;
    float lpB0_ = 0.0f, lpB1_ = 0.0f;

    Biquad bandpass_;
    DCBlocker dcBlocker_;
};

}

#endif
