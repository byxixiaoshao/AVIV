#ifndef LIMITER_EFFECT_H
#define LIMITER_EFFECT_H

#include <cmath>
#include <array>
#include <vector>

namespace audiofx {

class Limiter {
public:
    void init(int sampleRate) {
        sampleRate_ = sampleRate;
        updateCoefficients();
    }
    
    void process(float* samples, int numFrames, int channels) {
        for (int i = 0; i < numFrames; i++) {
            float maxSample = 0.0f;
            for (int c = 0; c < channels; c++) {
                float absSample = std::abs(samples[i * channels + c]);
                if (absSample > maxSample) maxSample = absSample;
            }
            
            float targetGain = 1.0f;
            if (maxSample > threshold_) {
                targetGain = threshold_ / maxSample;
            }
            
            if (targetGain < currentGain_) {
                currentGain_ = attackCoeff_ * currentGain_ + (1.0f - attackCoeff_) * targetGain;
            } else {
                currentGain_ = releaseCoeff_ * currentGain_ + (1.0f - releaseCoeff_) * targetGain;
            }
            
            currentGain_ = std::max(currentGain_, minGain_);
            
            for (int c = 0; c < channels; c++) {
                samples[i * channels + c] *= currentGain_;
            }
        }
    }
    
    void setThreshold(float thresholdDb) {
        threshold_ = std::pow(10.0f, thresholdDb / 20.0f);
    }
    
    void setAttack(float attackMs) {
        attackMs_ = attackMs;
        updateCoefficients();
    }
    
    void setRelease(float releaseMs) {
        releaseMs_ = releaseMs;
        updateCoefficients();
    }
    
    void clear() {
        currentGain_ = 1.0f;
    }

private:
    void updateCoefficients() {
        attackCoeff_ = std::exp(-1.0f / (attackMs_ * sampleRate_ / 1000.0f));
        releaseCoeff_ = std::exp(-1.0f / (releaseMs_ * sampleRate_ / 1000.0f));
    }
    
    int sampleRate_ = 44100;
    float threshold_ = 0.9f;
    float attackMs_ = 5.0f;
    float releaseMs_ = 50.0f;
    float attackCoeff_ = 0.0f;
    float releaseCoeff_ = 0.0f;
    float currentGain_ = 1.0f;
    float minGain_ = 0.1f;
};

struct LimiterConfig {
    bool enabled = true;
    bool limitEqualizer = true;
    bool limitEffects = true;
    bool limitReverb = true;
    bool limitSpatial = true;
    float threshold = 0.9f;
    float attack = 5.0f;
    float release = 50.0f;
};

// ── 多频段压限(全局总线专用) ──
// 根治「某一频段峰值过大 → 单频段全混音压限 → 整体音量被拉低」的问题:
// Linkwitz-Riley 4 阶分频成 低/中/高 三带, 各带独立 Limiter 检测峰值并分别压限, 再合成。
// 某一频段过载只压该频段, 不再拖累其他频段(如白噪音/高频层)。
// 重构保证: mid = x - low - high, 合成后 low+mid+high = x。

/** 二阶 biquad 滤波单元 (两级级联 = Linkwitz-Riley 4 阶) */
struct Biquad {
    float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f, a1 = 0.0f, a2 = 0.0f;
    float z1 = 0.0f, z2 = 0.0f;
    void reset() { z1 = z2 = 0.0f; }
    float process(float x) {
        float y = b0 * x + z1;
        z1 = b1 * x - a1 * y + z2;
        z2 = b2 * x - a2 * y;
        return y;
    }
};

class MultibandLimiter {
public:
    void init(int sampleRate) {
        sampleRate_ = sampleRate;
        limiterLow_.init(sampleRate);
        limiterMid_.init(sampleRate);
        limiterHigh_.init(sampleRate);
    }

    void setConfig(const LimiterConfig& config) {
        config_ = config;
        float thresholdDb = 20.0f * std::log10(config.threshold);
        limiterLow_.setThreshold(thresholdDb);
        limiterMid_.setThreshold(thresholdDb);
        limiterHigh_.setThreshold(thresholdDb);
        limiterLow_.setAttack(config.attack);
        limiterMid_.setAttack(config.attack);
        limiterHigh_.setAttack(config.attack);
        limiterLow_.setRelease(config.release);
        limiterMid_.setRelease(config.release);
        limiterHigh_.setRelease(config.release);
    }

    /** 设置分频点(默认 250/2500Hz): 低 < lowHz, 高 > highHz, 中间为中带 */
    void setCrossover(float lowHz, float highHz) {
        crossoverLow_ = lowHz;
        crossoverHigh_ = highHz;
        updateFilterCoeffs();
    }

    void process(float* samples, int numFrames, int channels) {
        if (!config_.enabled) return;
        if (channels <= 0 || numFrames <= 0) return;

        if ((int)lowFilters_.size() != channels) {
            lowFilters_.assign(channels, LR4());
            highFilters_.assign(channels, LR4());
            updateFilterCoeffs();
        }

        int n = numFrames * channels;
        if ((int)lowBuf_.size() < n) {
            lowBuf_.resize(n); midBuf_.resize(n); highBuf_.resize(n);
        }

        // 1. 每声道分频: low = LR4低通, high = LR4高通, mid = x - low - high
        for (int ch = 0; ch < channels; ch++) {
            LR4& lf = lowFilters_[ch];
            LR4& hf = highFilters_[ch];
            for (int i = 0; i < numFrames; i++) {
                float x = samples[i * channels + ch];
                float l = lf.processLow(x);
                float h = hf.processHigh(x);
                lowBuf_[i * channels + ch] = l;
                highBuf_[i * channels + ch] = h;
                midBuf_[i * channels + ch] = x - l - h;
            }
        }

        // 2. 各频段独立压限 (每个 Limiter 内部按帧取该频段全声道最大峰值)
        limiterLow_.process(lowBuf_.data(), numFrames, channels);
        limiterMid_.process(midBuf_.data(), numFrames, channels);
        limiterHigh_.process(highBuf_.data(), numFrames, channels);

        // 3. 合成
        for (int i = 0; i < n; i++) {
            samples[i] = lowBuf_[i] + midBuf_[i] + highBuf_[i];
        }
    }

    void clear() {
        limiterLow_.clear(); limiterMid_.clear(); limiterHigh_.clear();
        for (auto& f : lowFilters_) f.reset();
        for (auto& f : highFilters_) f.reset();
    }

private:
    /** 每声道两级级联 biquad (低/高通各一组) */
    struct LR4 {
        Biquad low1, low2, high1, high2;
        float processLow(float x) { return low2.process(low1.process(x)); }
        float processHigh(float x) { return high2.process(high1.process(x)); }
        void reset() { low1.reset(); low2.reset(); high1.reset(); high2.reset(); }
    };

    void updateFilterCoeffs() {
        int sr = std::max(sampleRate_, 1);
        for (auto& f : lowFilters_) {
            butterworth(f.low1, f.low2, crossoverLow_, sr, false);
            butterworth(f.high1, f.high2, crossoverHigh_, sr, true);
        }
    }

    /** 两级级联 Butterworth 二阶 (Q=1/√2) → Linkwitz-Riley 4 阶, 低/高通 */
    static void butterworth(Biquad& q1, Biquad& q2, float f0, int sr, bool highpass) {
        static constexpr float kPi = 3.14159265358979323846f;
        float w0 = 2.0f * kPi * f0 / sr;
        float cosw = std::cos(w0);
        float sinw = std::sin(w0);
        float alpha = sinw * 0.7071067811865476f;  // Q = 1/√2
        float a0 = 1.0f + alpha;
        float a1 = -2.0f * cosw;
        float a2 = 1.0f - alpha;
        float b0, b1, b2;
        if (highpass) {
            b0 = (1.0f + cosw) / 2.0f;
            b1 = -(1.0f + cosw);
            b2 = (1.0f + cosw) / 2.0f;
        } else {
            b0 = (1.0f - cosw) / 2.0f;
            b1 = 1.0f - cosw;
            b2 = (1.0f - cosw) / 2.0f;
        }
        setCoeffs(q1, b0, b1, b2, a0, a1, a2);
        setCoeffs(q2, b0, b1, b2, a0, a1, a2);
    }

    static void setCoeffs(Biquad& q, float b0, float b1, float b2, float a0, float a1, float a2) {
        q.b0 = b0 / a0; q.b1 = b1 / a0; q.b2 = b2 / a0; q.a1 = a1 / a0; q.a2 = a2 / a0;
        q.z1 = 0.0f; q.z2 = 0.0f;
    }

    int sampleRate_ = 44100;
    float crossoverLow_ = 250.0f;    // 低/中 分频点
    float crossoverHigh_ = 2500.0f;  // 中/高 分频点
    LimiterConfig config_;
    Limiter limiterLow_, limiterMid_, limiterHigh_;
    std::vector<LR4> lowFilters_, highFilters_;    // 分频滤波器状态(每声道)
    std::vector<float> lowBuf_, midBuf_, highBuf_; // 各频段工作缓冲
};

class GlobalLimiter {
public:
    void init(int sampleRate) {
        limiter_.init(sampleRate);
    }
    
    void process(float* samples, int numFrames, int channels) {
        if (!config_.enabled) return;
        limiter_.process(samples, numFrames, channels);
    }
    
    void setConfig(const LimiterConfig& config) {
        config_ = config;
        limiter_.setConfig(config);
    }
    
    const LimiterConfig& getConfig() const { return config_; }
    
    void setEnabled(bool enabled) { config_.enabled = enabled; }
    bool isEnabled() const { return config_.enabled; }
    
    void clear() {
        limiter_.clear();
    }

private:
    MultibandLimiter limiter_;
    LimiterConfig config_;
};

}

#endif
