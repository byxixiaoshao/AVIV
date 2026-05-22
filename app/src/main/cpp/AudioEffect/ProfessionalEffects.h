#ifndef PROFESSIONAL_EFFECTS_H
#define PROFESSIONAL_EFFECTS_H

#include "AudioEffectBase.h"
#include <cmath>
#include <algorithm>

namespace audiofx {

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

class PitchEffect : public AudioEffectBase {
public:
    PitchEffect() : semitones_(0.0f) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || std::abs(semitones_) < 0.01f) return;
        
        float pitchRatio = std::pow(2.0f, semitones_ / 12.0f);
        
        for (int i = 0; i < numFrames; i++) {
            for (int c = 0; c < channels; c++) {
                int idx = i * channels + c;
                float sample = samples[idx];
                
                float phaseInc = pitchRatio * 2.0f * M_PI * 440.0f / sampleRate_;
                phase_[c] += phaseInc;
                if (phase_[c] > 2.0f * M_PI) phase_[c] -= 2.0f * M_PI;
                
                float harmonic1 = std::sin(phase_[c]) * 0.1f * (pitchRatio - 1.0f);
                float harmonic2 = std::sin(phase_[c] * 2.0f) * 0.05f * (pitchRatio - 1.0f);
                float harmonic3 = std::sin(phase_[c] * 3.0f) * 0.025f * (pitchRatio - 1.0f);
                
                float formantShift = 1.0f / pitchRatio;
                float formantSample = sample;
                prevSample_[c] = formantSample;
                
                float output = sample + (harmonic1 + harmonic2 + harmonic3) * sample;
                output = output * 0.5f + formantSample * 0.5f;
                
                samples[idx] = std::tanh(output * 1.2f) * 0.8f;
            }
        }
    }
    
    void clear() override {
        std::fill(phase_, phase_ + 2, 0.0f);
        std::fill(prevSample_, prevSample_ + 2, 0.0f);
    }
    
    void setParameter(int paramId, float value) override { 
        if (paramId == 0) semitones_ = std::clamp(value, -12.0f, 12.0f); 
    }
    float getParameter(int paramId) const override { return paramId == 0 ? semitones_ : 0.0f; }
    EffectType getType() const override { return EffectType::Pitch; }
    std::string getName() const override { return "声调"; }
    std::string getCategory() const override { return "额外参数"; }

private:
    float semitones_;
    float phase_[2] = {0.0f, 0.0f};
    float prevSample_[2] = {0.0f, 0.0f};
};

class SpeedEffect : public AudioEffectBase {
public:
    SpeedEffect() : speed_(1.0f) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || std::abs(speed_ - 1.0f) < 0.01f) return;
        
        float speedRatio = speed_;
        
        for (int i = 0; i < numFrames; i++) {
            for (int c = 0; c < channels; c++) {
                int idx = i * channels + c;
                float sample = samples[idx];
                
                float energyMult = std::sqrt(speedRatio);
                
                float brightness = 0.0f;
                if (speedRatio > 1.0f) {
                    brightness = (speedRatio - 1.0f) * 0.3f;
                } else {
                    brightness = (1.0f - speedRatio) * 0.2f;
                }
                
                float highFreq = sample - prevSample_[c];
                prevSample_[c] = sample;
                
                float output = sample * energyMult;
                output += highFreq * brightness;
                
                samples[idx] = std::tanh(output) * 0.9f;
            }
        }
    }
    
    void clear() override {
        std::fill(prevSample_, prevSample_ + 2, 0.0f);
    }
    
    void setParameter(int paramId, float value) override { 
        if (paramId == 0) speed_ = std::clamp(value, 0.3f, 3.0f); 
    }
    float getParameter(int paramId) const override { return paramId == 0 ? speed_ : 1.0f; }
    EffectType getType() const override { return EffectType::Speed; }
    std::string getName() const override { return "速度"; }
    std::string getCategory() const override { return "额外参数"; }

private:
    float speed_;
    float prevSample_[2] = {0.0f, 0.0f};
};

class HiFiEffect : public AudioEffectBase {
public:
    HiFiEffect() : intensity_(0.0f) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        clear();
        
        float highCutoff = 3000.0f;
        float rc = 1.0f / (2.0f * M_PI * highCutoff);
        float dt = 1.0f / sampleRate_;
        highpassAlpha_ = rc / (rc + dt);
        
        float lowCutoff = 200.0f;
        rc = 1.0f / (2.0f * M_PI * lowCutoff);
        lowpassAlpha_ = dt / (rc + dt);
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || intensity_ < 0.001f) return;
        
        for (int i = 0; i < numFrames; i++) {
            for (int c = 0; c < channels; c++) {
                int idx = i * channels + c;
                float sample = samples[idx];
                
                float highpass = sample - prevSample_[c];
                highpassState_[c] = highpassAlpha_ * (highpassState_[c] + highpass);
                prevSample_[c] = sample;
                float highFreq = highpassState_[c];
                
                float harmonic = std::tanh(highFreq * 5.0f * intensity_) * 0.3f * intensity_;
                
                float exciter = highFreq * intensity_ * 0.5f;
                
                lowpassState_[c] = lowpassAlpha_ * sample + (1.0f - lowpassAlpha_) * lowpassState_[c];
                float bassEnhance = lowpassState_[c] * intensity_ * 0.4f;
                
                float presence = sample * intensity_ * 0.15f;
                
                float output = sample * (1.0f + intensity_ * 0.3f) + harmonic + exciter + bassEnhance + presence;
                
                samples[idx] = std::tanh(output) * 0.85f;
            }
        }
    }
    
    void clear() override {
        std::fill(prevSample_, prevSample_ + 2, 0.0f);
        std::fill(highpassState_, highpassState_ + 2, 0.0f);
        std::fill(lowpassState_, lowpassState_ + 2, 0.0f);
    }
    
    void setParameter(int paramId, float value) override { 
        if (paramId == 0) intensity_ = std::clamp(value, 0.0f, 1.0f); 
    }
    float getParameter(int paramId) const override { return paramId == 0 ? intensity_ : 0.0f; }
    EffectType getType() const override { return EffectType::HiFi; }
    std::string getName() const override { return "Hi-Fi"; }
    std::string getCategory() const override { return "音质增强"; }

private:
    float intensity_;
    float highpassAlpha_ = 0.9f;
    float lowpassAlpha_ = 0.1f;
    float prevSample_[2] = {0.0f, 0.0f};
    float highpassState_[2] = {0.0f, 0.0f};
    float lowpassState_[2] = {0.0f, 0.0f};
};

class DistortionEffect : public AudioEffectBase {
public:
    DistortionEffect() : drive_(0.0f), tone_(0.5f) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || drive_ < 0.001f) return;
        
        for (int i = 0; i < numFrames; i++) {
            for (int c = 0; c < channels; c++) {
                int idx = i * channels + c;
                float x = samples[idx];
                
                float k = drive_ * 0.5f;
                samples[idx] = x + k * (x * x * x - x) / 3.0f;
            }
        }
    }
    
    void clear() override {}
    
    void setParameter(int paramId, float value) override { 
        if (paramId == 0) drive_ = std::clamp(value, 0.0f, 1.0f);
        else if (paramId == 1) tone_ = std::clamp(value, 0.0f, 1.0f);
    }
    float getParameter(int paramId) const override { 
        if (paramId == 0) return drive_;
        else if (paramId == 1) return tone_;
        return 0.0f;
    }
    EffectType getType() const override { return EffectType::Distortion; }
    std::string getName() const override { return "失真"; }
    std::string getCategory() const override { return "音质增强"; }

private:
    float drive_;
    float tone_;
};

class NoiseEffect : public AudioEffectBase {
public:
    NoiseEffect() : intensity_(0.0f), noiseType_(0) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        clear();
        std::srand(42);
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || intensity_ < 0.001f) return;
        
        for (int i = 0; i < numFrames; i++) {
            float white = (std::rand() / (float)RAND_MAX) * 2.0f - 1.0f;
            
            pinkState_[0] = 0.99765f * pinkState_[0] + white * 0.0990460f;
            pinkState_[1] = 0.96300f * pinkState_[1] + white * 0.2965164f;
            pinkState_[2] = 0.57000f * pinkState_[2] + white * 1.0526913f;
            float pink = (pinkState_[0] + pinkState_[1] + pinkState_[2] + white * 0.1848f) * 0.05f;
            
            float noise;
            switch (noiseType_) {
                case 0: noise = white; break;
                case 1: noise = pink; break;
                case 2: noise = (white + pink) * 0.5f; break;
                default: noise = white; break;
            }
            
            for (int c = 0; c < channels; c++) {
                int idx = i * channels + c;
                float sample = samples[idx];
                
                samples[idx] = sample + noise * intensity_ * 0.3f;
            }
        }
    }
    
    void clear() override {
        std::fill(pinkState_, pinkState_ + 3, 0.0f);
    }
    
    void setParameter(int paramId, float value) override { 
        if (paramId == 0) intensity_ = std::clamp(value, 0.0f, 1.0f);
        else if (paramId == 1) noiseType_ = std::clamp((int)value, 0, 2);
    }
    float getParameter(int paramId) const override { 
        if (paramId == 0) return intensity_;
        else if (paramId == 1) return (float)noiseType_;
        return 0.0f;
    }
    EffectType getType() const override { return EffectType::Noise; }
    std::string getName() const override { return "噪声"; }
    std::string getCategory() const override { return "音质增强"; }

private:
    float intensity_;
    int noiseType_;
    float pinkState_[3] = {0.0f, 0.0f, 0.0f};
};

}

#endif
