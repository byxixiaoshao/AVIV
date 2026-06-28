#ifndef LIMITER_EFFECT_H
#define LIMITER_EFFECT_H

#include <cmath>
#include <array>

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
        limiter_.setThreshold(20.0f * std::log10(config.threshold));
        limiter_.setAttack(config.attack);
        limiter_.setRelease(config.release);
    }
    
    const LimiterConfig& getConfig() const { return config_; }
    
    void setEnabled(bool enabled) { config_.enabled = enabled; }
    bool isEnabled() const { return config_.enabled; }
    
    void clear() {
        limiter_.clear();
    }

private:
    Limiter limiter_;
    LimiterConfig config_;
};

}

#endif
