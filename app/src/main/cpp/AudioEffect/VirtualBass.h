#ifndef VIRTUAL_BASS_H
#define VIRTUAL_BASS_H

#include "AudioEffectBase.h"
#include <cmath>
#include <algorithm>

namespace audiofx {

// 低频激励器 (Bass Exciter)
// 原理：分离低频 -> 动态饱和 -> 混合回原始信号
// 增强低频的"质感"和"冲击感"，而不是简单提升增益
class VirtualBass : public AudioEffectBase {
public:
    VirtualBass() : intensity_(0.5f), sampleRate_(44100) {}

    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        updateCoefficients();
        clear();
    }

    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || intensity_ < 0.01f) return;

        for (int i = 0; i < numFrames; i++) {
            for (int c = 0; c < channels && c < 2; c++) {
                int idx = i * channels + c;
                float input = samples[idx];

                // Step 1: 分离低频 (below 200Hz)
                float low = lp_[c].process(input);

                // Step 2: 高通滤除超低频 (below 40Hz)，避免处理 DC 和隆隆声
                float lowFiltered = hp_[c].process(low);

                // Step 3: 动态饱和 - 使用软削波产生谐波
                // 饱和度随信号强度动态变化
                float absLow = std::abs(lowFiltered);
                float drive = 1.0f + intensity_ * 2.0f;  // 1.0 - 7.0

                // 软饱和函数: tanh with drive
                float saturated = std::tanh(lowFiltered * drive) / std::tanh(drive);

                // Step 4: 包络跟随 - 平滑处理，避免瞬态失真
                // 检测低频能量
                float envelope = env_[c].process(absLow);

                // 根据包络调整激励量：信号强时激励多，信号弱时激励少
                float exciterAmount = envelope * intensity_;

                // Step 5: 混合激励信号
                // 只添加饱和后的低频，不添加原始低频（避免重复）
                float excited = saturated * exciterAmount * 0.7f;

                // Step 6: 添加少量高频"点击感"（瞬态增强）
                // 检测瞬态：当前值 - 前一个值
                float transient = lowFiltered - prevLow_[c];
                prevLow_[c] = lowFiltered;

                // 瞬态经过高通，提取"点击"
                float click = clickHp_[c].process(transient);
                float clickAmount = std::abs(click) * intensity_ * 0.3f;

                // 最终输出
                samples[idx] = input + excited + click * clickAmount;
            }
        }
    }

    void clear() override {
        for (int c = 0; c < 2; c++) {
            lp_[c].clear();
            hp_[c].clear();
            clickHp_[c].clear();
            env_[c].clear();
            prevLow_[c] = 0.0f;
        }
    }

    void setParameter(int paramId, float value) override {
        if (paramId == 0) {
            intensity_ = std::clamp(value, 0.0f, 3.0f);
        }
    }

    float getParameter(int paramId) const override {
        return paramId == 0 ? intensity_ : 0.0f;
    }

    EffectType getType() const override { return EffectType::VirtualBass; }
    std::string getName() const override { return "低频激励器"; }
    std::string getCategory() const override { return "音质增强"; }

private:
    void updateCoefficients() {
        float dt = 1.0f / sampleRate_;

        // Lowpass: 分离低频 (below 200Hz)
        float lpCutoff = 200.0f;
        float lpRc = 1.0f / (2.0f * 3.14159265358979323846f * lpCutoff);
        float lpAlpha = dt / (lpRc + dt);

        // Highpass 1: 滤除超低频 (below 40Hz)
        float hpCutoff = 40.0f;
        float hpRc = 1.0f / (2.0f * 3.14159265358979323846f * hpCutoff);
        float hpAlpha = hpRc / (hpRc + dt);

        // Highpass 2: 提取瞬态点击 (above 100Hz)
        float clickCutoff = 100.0f;
        float clickRc = 1.0f / (2.0f * 3.14159265358979323846f * clickCutoff);
        float clickAlpha = clickRc / (clickRc + dt);

        for (int c = 0; c < 2; c++) {
            lp_[c].setAlpha(lpAlpha);
            hp_[c].setAlpha(hpAlpha);
            clickHp_[c].setAlpha(clickAlpha);
            env_[c].setAttackRelease(0.005f, 0.05f, sampleRate_);  // 5ms attack, 50ms release
        }
    }

    // One-pole Lowpass
    struct OnePoleLP {
        float state_ = 0.0f;
        float alpha_ = 0.1f;
        void setAlpha(float a) { alpha_ = a; }
        float process(float input) {
            state_ = alpha_ * input + (1.0f - alpha_) * state_;
            return state_;
        }
        void clear() { state_ = 0.0f; }
    };

    // One-pole Highpass
    struct OnePoleHP {
        float state_ = 0.0f;
        float prevIn_ = 0.0f;
        float alpha_ = 0.1f;
        void setAlpha(float a) { alpha_ = a; }
        float process(float input) {
            float output = alpha_ * (state_ + input - prevIn_);
            prevIn_ = input;
            state_ = output;
            return output;
        }
        void clear() { state_ = 0.0f; prevIn_ = 0.0f; }
    };

    // Envelope Follower (用于动态处理)
    struct EnvelopeFollower {
        float state_ = 0.0f;
        float attackCoeff_ = 0.1f;
        float releaseCoeff_ = 0.01f;

        void setAttackRelease(float attackMs, float releaseMs, int sampleRate) {
            attackCoeff_ = 1.0f - std::exp(-1.0f / (attackMs * sampleRate));
            releaseCoeff_ = 1.0f - std::exp(-1.0f / (releaseMs * sampleRate));
        }

        float process(float input) {
            float absIn = std::abs(input);
            float coeff = (absIn > state_) ? attackCoeff_ : releaseCoeff_;
            state_ = coeff * absIn + (1.0f - coeff) * state_;
            return state_;
        }

        void clear() { state_ = 0.0f; }
    };

    float intensity_;
    int sampleRate_;
    OnePoleLP lp_[2];
    OnePoleHP hp_[2];
    OnePoleHP clickHp_[2];
    EnvelopeFollower env_[2];
    float prevLow_[2] = {0.0f, 0.0f};
};

}

#endif
