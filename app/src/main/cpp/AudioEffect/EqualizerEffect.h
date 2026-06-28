#ifndef EQUALIZER_EFFECT_H
#define EQUALIZER_EFFECT_H

#include "AudioEffectBase.h"
#include <array>
#include <cmath>
#include <vector>
#include <android/log.h>

#define EQ_LOG_TAG "Equalizer"
#define EQ_LOGI(...) __android_log_print(ANDROID_LOG_INFO, EQ_LOG_TAG, __VA_ARGS__)

namespace audiofx {

constexpr int EQ_BAND_COUNT = 12;

constexpr std::array<float, EQ_BAND_COUNT> EQ_FREQUENCIES = {
    25.0f,    // 0: 25 Hz (Sub Bass) - Low Shelf
    50.0f,    // 1: 50 Hz (Bass) - Peaking
    100.0f,   // 2: 100 Hz (Bass) - Peaking
    200.0f,   // 3: 200 Hz (Low Mid) - Peaking
    400.0f,   // 4: 400 Hz (Mid) - Peaking
    800.0f,   // 5: 800 Hz (Mid) - Peaking
    1600.0f,  // 6: 1.6 kHz (Upper Mid) - Peaking
    3200.0f,  // 7: 3.2 kHz (Presence) - Peaking
    6300.0f,  // 8: 6.3 kHz (Treble) - Peaking
    10000.0f, // 9: 10 kHz (Treble) - Peaking
    14000.0f, // 10: 14 kHz (Air) - Peaking
    16000.0f  // 11: 16 kHz (Air) - High Shelf
};

enum class EqFilterType {
    Peaking,
    LowShelf,
    HighShelf
};

constexpr std::array<EqFilterType, EQ_BAND_COUNT> EQ_FILTER_TYPES = {
    EqFilterType::LowShelf,   // 25 Hz
    EqFilterType::Peaking,    // 50 Hz
    EqFilterType::Peaking,    // 100 Hz
    EqFilterType::Peaking,    // 200 Hz
    EqFilterType::Peaking,    // 400 Hz
    EqFilterType::Peaking,    // 800 Hz
    EqFilterType::Peaking,    // 1.6 kHz
    EqFilterType::Peaking,    // 3.2 kHz
    EqFilterType::Peaking,    // 6.3 kHz
    EqFilterType::Peaking,    // 10 kHz
    EqFilterType::Peaking,    // 14 kHz
    EqFilterType::HighShelf   // 16 kHz
};

constexpr std::array<float, EQ_BAND_COUNT> EQ_Q_VALUES = {
    0.5f,   // Low Shelf - 很宽，平滑过渡
    0.6f,   // 50 Hz - 宽
    0.6f,   // 100 Hz - 宽
    0.7f,   // 200 Hz - 较宽
    0.7f,   // 400 Hz - 较宽
    0.7f,   // 800 Hz - 较宽
    0.7f,   // 1.6 kHz - 较宽
    0.7f,   // 3.2 kHz - 较宽
    0.7f,   // 6.3 kHz - 较宽
    0.6f,   // 10 kHz - 宽
    0.6f,   // 14 kHz - 宽
    0.5f    // High Shelf - 很宽，平滑过渡
};

class EqBand {
public:
    void init(float freq, int sampleRate, EqFilterType type, float Q) {
        freq_ = freq;
        sampleRate_ = sampleRate;
        filterType_ = type;
        Q_ = Q;
        setGain(0.0f);
    }
    
    void setGain(float gainDb) {
        gainDb_ = gainDb;
        updateCoeffs();
    }
    
    float getGain() const { return gainDb_; }
    
    void process(const float* input, float* output, int numFrames, int channels) {
        if (std::abs(gainDb_) < 0.05f) {
            for (int i = 0; i < numFrames * channels; i++) {
                output[i] = input[i];
            }
            return;
        }
        
        for (int i = 0; i < numFrames; i++) {
            for (int c = 0; c < channels; c++) {
                int idx = i * channels + c;
                float in = input[idx];
                
                float out = b0_ * in + b1_ * x1_[c] + b2_ * x2_[c] 
                          - a1_ * y1_[c] - a2_ * y2_[c];
                
                x2_[c] = x1_[c];
                x1_[c] = in;
                y2_[c] = y1_[c];
                y1_[c] = out;
                
                output[idx] = out;
            }
        }
    }
    
    void clear() {
        x1_.fill(0.0f);
        x2_.fill(0.0f);
        y1_.fill(0.0f);
        y2_.fill(0.0f);
    }

private:
    void updateCoeffs() {
        float A = std::pow(10.0f, gainDb_ / 40.0f);
        float w0 = 2.0f * M_PI * freq_ / sampleRate_;
        float sinw0 = std::sin(w0);
        float cosw0 = std::cos(w0);
        
        float alpha = sinw0 / (2.0f * Q_);
        float b0, b1, b2, a0, a1, a2;
        
        switch (filterType_) {
            case EqFilterType::Peaking:
                b0 = 1.0f + alpha * A;
                b1 = -2.0f * cosw0;
                b2 = 1.0f - alpha * A;
                a0 = 1.0f + alpha / A;
                a1 = -2.0f * cosw0;
                a2 = 1.0f - alpha / A;
                break;
                
            case EqFilterType::LowShelf:
                {
                    float beta = std::sqrt(A + 1.0f/A);
                    b0 = A * ((A + 1.0f) - (A - 1.0f) * cosw0 + beta * sinw0);
                    b1 = 2.0f * A * ((A - 1.0f) - (A + 1.0f) * cosw0);
                    b2 = A * ((A + 1.0f) - (A - 1.0f) * cosw0 - beta * sinw0);
                    a0 = (A + 1.0f) + (A - 1.0f) * cosw0 + beta * sinw0;
                    a1 = -2.0f * ((A - 1.0f) + (A + 1.0f) * cosw0);
                    a2 = (A + 1.0f) + (A - 1.0f) * cosw0 - beta * sinw0;
                }
                break;
                
            case EqFilterType::HighShelf:
                {
                    float beta = std::sqrt(A + 1.0f/A);
                    b0 = A * ((A + 1.0f) + (A - 1.0f) * cosw0 + beta * sinw0);
                    b1 = -2.0f * A * ((A - 1.0f) + (A + 1.0f) * cosw0);
                    b2 = A * ((A + 1.0f) + (A - 1.0f) * cosw0 - beta * sinw0);
                    a0 = (A + 1.0f) - (A - 1.0f) * cosw0 + beta * sinw0;
                    a1 = 2.0f * ((A - 1.0f) - (A + 1.0f) * cosw0);
                    a2 = (A + 1.0f) - (A - 1.0f) * cosw0 - beta * sinw0;
                }
                break;
        }
        
        float invA0 = 1.0f / a0;
        b0_ = b0 * invA0;
        b1_ = b1 * invA0;
        b2_ = b2 * invA0;
        a1_ = a1 * invA0;
        a2_ = a2 * invA0;
    }
    
    float freq_ = 1000.0f;
    int sampleRate_ = 44100;
    float gainDb_ = 0.0f;
    float Q_ = 1.0f;
    EqFilterType filterType_ = EqFilterType::Peaking;
    
    float b0_ = 1.0f, b1_ = 0.0f, b2_ = 0.0f;
    float a1_ = 0.0f, a2_ = 0.0f;
    
    std::array<float, 2> x1_ = {0.0f, 0.0f};
    std::array<float, 2> x2_ = {0.0f, 0.0f};
    std::array<float, 2> y1_ = {0.0f, 0.0f};
    std::array<float, 2> y2_ = {0.0f, 0.0f};
};

class TrueLimiter {
public:
    void init(int sampleRate) {
        sampleRate_ = sampleRate;
        attackCoeff_ = std::exp(-1.0f / (attackMs_ * sampleRate_ / 1000.0f));
        releaseCoeff_ = std::exp(-1.0f / (releaseMs_ * sampleRate_ / 1000.0f));
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
        attackCoeff_ = std::exp(-1.0f / (attackMs_ * sampleRate_ / 1000.0f));
    }
    
    void setRelease(float releaseMs) {
        releaseMs_ = releaseMs;
        releaseCoeff_ = std::exp(-1.0f / (releaseMs_ * sampleRate_ / 1000.0f));
    }
    
    void clear() {
        currentGain_ = 1.0f;
    }

private:
    int sampleRate_ = 44100;
    float threshold_ = 0.9f;
    float attackMs_ = 5.0f;
    float releaseMs_ = 50.0f;
    float attackCoeff_ = 0.0f;
    float releaseCoeff_ = 0.0f;
    float currentGain_ = 1.0f;
    float minGain_ = 0.1f;
};

class EqualizerEffect : public AudioEffectBase {
public:
    EqualizerEffect() {
        gains_.fill(0.0f);
    }
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        for (int i = 0; i < EQ_BAND_COUNT; i++) {
            bands_[i].init(EQ_FREQUENCIES[i], sampleRate_, EQ_FILTER_TYPES[i], EQ_Q_VALUES[i]);
        }
        limiter_.init(sampleRate_);
    }
    
    void setLimiterEnabled(bool enabled) { limiterEnabled_ = enabled; }
    bool isLimiterEnabled() const { return limiterEnabled_; }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_) return;
        
        bool hasGain = false;
        for (int i = 0; i < EQ_BAND_COUNT; i++) {
            if (std::abs(gains_[i]) > 0.05f) {
                hasGain = true;
                break;
            }
        }
        if (!hasGain) return;
        
        std::vector<float> temp(numFrames * channels);
        
        std::copy(samples, samples + numFrames * channels, temp.begin());
        
        for (int i = 0; i < EQ_BAND_COUNT; i++) {
            if (std::abs(gains_[i]) > 0.05f) {
                bands_[i].process(temp.data(), samples, numFrames, channels);
                std::copy(samples, samples + numFrames * channels, temp.begin());
            }
        }
        
        std::copy(temp.begin(), temp.end(), samples);
        
        if (limiterEnabled_) {
            limiter_.process(samples, numFrames, channels);
        }
    }
    
    void clear() override {
        for (auto& band : bands_) {
            band.clear();
        }
        limiter_.clear();
    }
    
    void setParameter(int paramId, float value) override {
        if (paramId >= 0 && paramId < EQ_BAND_COUNT) {
            gains_[paramId] = std::clamp(value, -12.0f, 12.0f);
            // Re-apply smoothing when a single band changes
            applySmoothedGains();
        }
    }

    float getParameter(int paramId) const override {
        if (paramId >= 0 && paramId < EQ_BAND_COUNT) {
            return gains_[paramId];
        }
        return 0.0f;
    }

    void setGains(const std::array<float, EQ_BAND_COUNT>& gains) {
        gains_ = gains;
        applySmoothedGains();
    }

private:
    void applySmoothedGains() {
        // Apply gain smoothing for curve-like adjustment
        // Each band's effective gain is a weighted average of itself and neighbors
        std::array<float, EQ_BAND_COUNT> smoothedGains;
        for (int i = 0; i < EQ_BAND_COUNT; i++) {
            float selfWeight = 0.6f;

            float selfGain = gains_[i];
            float leftGain = (i > 0) ? gains_[i - 1] : gains_[i];
            float rightGain = (i < EQ_BAND_COUNT - 1) ? gains_[i + 1] : gains_[i];

            // Weighted average: self + neighbors
            smoothedGains[i] = selfGain * selfWeight + (leftGain + rightGain) * 0.5f * (1.0f - selfWeight);
        }

        // Apply smoothed gains to bands
        for (int i = 0; i < EQ_BAND_COUNT; i++) {
            bands_[i].setGain(smoothedGains[i]);
        }
    }

public:
    
    const std::array<float, EQ_BAND_COUNT>& getGains() const {
        return gains_;
    }
    
    EffectType getType() const override { return EffectType::Equalizer; }
    std::string getName() const override { return "Equalizer"; }
    std::string getCategory() const override { return "professional"; }

private:
    std::array<EqBand, EQ_BAND_COUNT> bands_;
    std::array<float, EQ_BAND_COUNT> gains_;
    bool limiterEnabled_ = true;
    TrueLimiter limiter_;
};

}

#endif
