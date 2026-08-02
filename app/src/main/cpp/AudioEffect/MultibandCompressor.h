#ifndef MULTIBAND_COMPRESSOR_H
#define MULTIBAND_COMPRESSOR_H

#include "AudioEffectBase.h"
#include <cmath>
#include <algorithm>

namespace audiofx {

class MultibandCompressor : public AudioEffectBase {
public:
    MultibandCompressor() : intensity_(0.5f), sampleRate_(44100) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        updateCoefficients();
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || intensity_ < 0.001f) return;
        
        for (int i = 0; i < numFrames; i++) {
            // Split into 3 bands per channel
            float bandL[3], bandM[3], bandH[3];
            
            for (int c = 0; c < channels && c < 2; c++) {
                int idx = i * channels + c;
                float input = samples[idx];
                
                // Crossover filters: split into Lo, Mid, Hi
                float lo = lp_[c].process(input);
                float hi = hp_[c].process(input);
                float mid = input - lo - hi;
                
                // Band-specific compression
                bandL[c] = compressBand(lo, 0, c);
                bandM[c] = compressBand(mid, 1, c);
                bandH[c] = compressBand(hi, 2, c);
            }
            
            // Recombine
            for (int c = 0; c < channels && c < 2; c++) {
                int idx = i * channels + c;
                float mix = bandL[c] + bandM[c] + bandH[c];
                
                // Dry/wet blend based on intensity
                // intensity <= 1: dry/wet 线性混合
                // intensity > 1: 全 wet + 温和提升（非线性，避免倍数放大导致爆音）
                if (intensity_ <= 1.0f) {
                    samples[idx] = samples[idx] * (1.0f - intensity_) + mix * intensity_;
                } else {
                    // intensity>1 时不再做 intensity_ 倍线性放大（旧代码 mix*intensity_ 在 intensity=3 时输出 3 倍 → 爆音）
                    // 改为全 wet + 平方根压缩的温和提升，并软限幅
                    float extra = intensity_ - 1.0f;
                    float boosted = mix * (1.0f + std::sqrt(extra) * 0.3f);
                    samples[idx] = std::tanh(boosted) * 0.95f;
                }
            }
        }
    }
    
    void clear() override {
        for (int c = 0; c < 2; c++) {
            lp_[c].clear();
            hp_[c].clear();
        }
        for (int b = 0; b < 3; b++) {
            for (int c = 0; c < 2; c++) {
                env_[b][c] = 1.0f;
            }
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
    
    EffectType getType() const override { return EffectType::MultibandCompressor; }
    std::string getName() const override { return "多段压缩器"; }
    std::string getCategory() const override { return "音质增强"; }

private:
    // Crossover frequencies
    static constexpr float kCrossoverLo = 150.0f;   // Low <-> Mid
    static constexpr float kCrossoverHi = 3000.0f;   // Mid <-> High
    
    // Per-band compressor params (threshold ratio, attack, release, makeup)
    const float threshold_[3] = {0.5f, 0.4f, 0.3f};   // Normalized (0-1)
    const float ratio_[3] = {4.0f, 2.5f, 2.0f};
    const float attackMs_[3] = {10.0f, 5.0f, 2.0f};
    const float releaseMs_[3] = {80.0f, 40.0f, 20.0f};
    const float makeup_[3] = {1.2f, 1.05f, 1.0f};
    
    void updateCoefficients() {
        float dt = 1.0f / sampleRate_;
        
        // Lowpass for crossover (Lo band extraction)
        float rcLo = 1.0f / (2.0f * 3.14159265358979323846f * kCrossoverLo);
        lpAlpha_ = dt / (rcLo + dt);
        
        // Highpass for crossover (Hi band extraction)
        float rcHi = 1.0f / (2.0f * 3.14159265358979323846f * kCrossoverHi);
        hpAlpha_ = rcHi / (rcHi + dt);
        
        // Compressor time constants
        for (int b = 0; b < 3; b++) {
            attackCoeff_[b] = std::exp(-1.0f / (attackMs_[b] * sampleRate_ / 1000.0f));
            releaseCoeff_[b] = std::exp(-1.0f / (releaseMs_[b] * sampleRate_ / 1000.0f));
        }
    }
    
    float compressBand(float input, int band, int channel) {
        if (input == 0.0f) return 0.0f;
        
        // RMS envelope follower
        float absIn = std::abs(input);
        float targetEnv = absIn;
        
        if (targetEnv > env_[band][channel]) {
            env_[band][channel] = attackCoeff_[band] * env_[band][channel] 
                                + (1.0f - attackCoeff_[band]) * targetEnv;
        } else {
            env_[band][channel] = releaseCoeff_[band] * env_[band][channel] 
                                + (1.0f - releaseCoeff_[band]) * targetEnv;
        }
        env_[band][channel] = std::max(env_[band][channel], 0.0001f);
        
        // Compression: above threshold, reduce by ratio
        float gain = 1.0f;
        if (env_[band][channel] > threshold_[band]) {
            float over = env_[band][channel] / threshold_[band];
            float gainDb = -(20.0f * std::log10(over) * (1.0f - 1.0f / ratio_[band]));
            gain = std::pow(10.0f, gainDb / 20.0f);
        }
        
        // Makeup gain
        gain *= makeup_[band];
        
        return input * gain;
    }
    
    struct OnePoleLP {
        float state_ = 0.0f;
        float alpha_ = 0.1f;
        OnePoleLP() = default;
        void setAlpha(float a) { alpha_ = a; }
        float process(float input) {
            state_ = alpha_ * input + (1.0f - alpha_) * state_;
            return state_;
        }
        void clear() { state_ = 0.0f; }
    };
    
    struct OnePoleHP {
        float state_ = 0.0f;
        float prevIn_ = 0.0f;
        float alpha_ = 0.1f;
        OnePoleHP() = default;
        void setAlpha(float a) { alpha_ = a; }
        float process(float input) {
            float output = alpha_ * (state_ + input - prevIn_);
            prevIn_ = input;
            state_ = output;
            return output;
        }
        void clear() { state_ = 0.0f; prevIn_ = 0.0f; }
    };
    
    float intensity_;
    int sampleRate_;
    float lpAlpha_ = 0.1f;
    float hpAlpha_ = 0.1f;
    float attackCoeff_[3] = {};
    float releaseCoeff_[3] = {};
    float env_[3][2] = {{1.0f, 1.0f}, {1.0f, 1.0f}, {1.0f, 1.0f}};
    
    OnePoleLP lp_[2];
    OnePoleHP hp_[2];
};

}

#endif
