#ifndef CREATIVE_EFFECTS_H
#define CREATIVE_EFFECTS_H

#include "AudioEffectBase.h"
#include <cmath>
#include <algorithm>
#include <vector>

namespace audiofx {

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

class SimpleFilter {
public:
    void setLowpass(float cutoff, int sampleRate) {
        float rc = 1.0f / (2.0f * M_PI * cutoff);
        float dt = 1.0f / sampleRate;
        alpha_ = dt / (rc + dt);
        mode_ = 0;
    }
    
    void setHighpass(float cutoff, int sampleRate) {
        float rc = 1.0f / (2.0f * M_PI * cutoff);
        float dt = 1.0f / sampleRate;
        alpha_ = rc / (rc + dt);
        mode_ = 1;
    }
    
    void setBandpass(float low, float high, int sampleRate) {
        lowAlpha_ = 1.0f / (1.0f + 2.0f * M_PI * low / sampleRate);
        highAlpha_ = (2.0f * M_PI * high) / (sampleRate + 2.0f * M_PI * high);
        mode_ = 2;
    }
    
    float process(float input) {
        if (mode_ == 0) {
            state_ = alpha_ * input + (1.0f - alpha_) * state_;
            return state_;
        } else if (mode_ == 1) {
            float output = alpha_ * (prevOutput_ + input - prevInput_);
            prevInput_ = input;
            prevOutput_ = output;
            return output;
        } else {
            float low = lowAlpha_ * input + (1.0f - lowAlpha_) * lowState_;
            lowState_ = low;
            float high = highAlpha_ * (input - prevInput_) + (1.0f - highAlpha_) * highState_;
            highState_ = high;
            prevInput_ = input;
            return low + high;
        }
    }
    
    void clear() {
        state_ = 0.0f;
        prevInput_ = 0.0f;
        prevOutput_ = 0.0f;
        lowState_ = 0.0f;
        highState_ = 0.0f;
    }

private:
    float alpha_ = 0.1f;
    float lowAlpha_ = 0.1f;
    float highAlpha_ = 0.1f;
    float state_ = 0.0f;
    float prevInput_ = 0.0f;
    float prevOutput_ = 0.0f;
    float lowState_ = 0.0f;
    float highState_ = 0.0f;
    int mode_ = 0;
};

class LoFiEffect : public AudioEffectBase {
public:
    LoFiEffect() : intensity_(0.5f), holdSampleL_(0.0f), holdSampleR_(0.0f), holdCount_(0) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        filter_.setLowpass(3000.0f, sampleRate_);
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || intensity_ < 0.001f) return;
        
        int bitDepth = static_cast<int>(8 - intensity_ * 4);
        bitDepth = std::max(4, std::min(8, bitDepth));
        float steps = std::pow(2.0f, bitDepth);
        
        int decimate = 1 + static_cast<int>(intensity_ * 4);
        
        for (int i = 0; i < numFrames; i++) {
            holdCount_++;
            if (holdCount_ >= decimate) {
                holdCount_ = 0;
                holdSampleL_ = samples[i * channels];
                if (channels > 1) holdSampleR_ = samples[i * channels + 1];
            }
            
            for (int c = 0; c < channels; c++) {
                float sample = samples[i * channels + c];
                
                float downsampled = (c == 0) ? holdSampleL_ : holdSampleR_;
                
                float quantized = std::round(downsampled * steps) / steps;
                
                quantized = filter_.process(quantized);
                
                samples[i * channels + c] = sample * (1.0f - intensity_) + quantized * intensity_;
            }
        }
    }
    
    void clear() override { filter_.clear(); holdSampleL_ = 0.0f; holdSampleR_ = 0.0f; holdCount_ = 0; }
    void setParameter(int paramId, float value) override { if (paramId == 0) intensity_ = std::clamp(value, 0.0f, 1.0f); }
    float getParameter(int paramId) const override { return paramId == 0 ? intensity_ : 0.0f; }
    EffectType getType() const override { return EffectType::LoFi; }
    std::string getName() const override { return "Lo-Fi"; }
    std::string getCategory() const override { return "音质效果"; }

private:
    float intensity_;
    float holdSampleL_;
    float holdSampleR_;
    int holdCount_;
    SimpleFilter filter_;
};

class EightBitEffect : public AudioEffectBase {
public:
    EightBitEffect() : intensity_(0.5f), holdSampleL_(0.0f), holdSampleR_(0.0f), holdCount_(0) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        filter_.setLowpass(5000.0f, sampleRate_);
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || intensity_ < 0.001f) return;
        
        int bitDepth = static_cast<int>(5 - intensity_ * 2);
        bitDepth = std::max(3, std::min(5, bitDepth));
        int levels = 1 << bitDepth;
        float step = 2.0f / levels;
        
        int decimate = 2 + static_cast<int>(intensity_ * 3);
        
        for (int i = 0; i < numFrames; i++) {
            holdCount_++;
            if (holdCount_ >= decimate) {
                holdCount_ = 0;
                holdSampleL_ = samples[i * channels];
                if (channels > 1) holdSampleR_ = samples[i * channels + 1];
            }
            
            for (int c = 0; c < channels; c++) {
                float sample = samples[i * channels + c];
                
                float downsampled = (c == 0) ? holdSampleL_ : holdSampleR_;
                
                float normalized = (downsampled + 1.0f) / 2.0f;
                int level = static_cast<int>(normalized * levels);
                level = std::max(0, std::min(levels - 1, level));
                float quantized = (level + 0.5f) * step - 1.0f;
                
                quantized = filter_.process(quantized);
                
                samples[i * channels + c] = sample * (1.0f - intensity_) + quantized * intensity_;
            }
        }
    }
    
    void clear() override { filter_.clear(); holdSampleL_ = 0.0f; holdSampleR_ = 0.0f; holdCount_ = 0; }
    void setParameter(int paramId, float value) override { if (paramId == 0) intensity_ = std::clamp(value, 0.0f, 1.0f); }
    float getParameter(int paramId) const override { return paramId == 0 ? intensity_ : 0.0f; }
    EffectType getType() const override { return EffectType::EightBit; }
    std::string getName() const override { return "8-bit游戏"; }
    std::string getCategory() const override { return "音质效果"; }

private:
    float intensity_;
    float holdSampleL_;
    float holdSampleR_;
    int holdCount_;
    SimpleFilter filter_;
};

class UnderwaterEffect : public AudioEffectBase {
public:
    UnderwaterEffect() : intensity_(0.5f), phase_(0.0f) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || intensity_ < 0.001f) return;
        
        float cutoff = 1000.0f - intensity_ * 700.0f;
        cutoff = std::max(300.0f, cutoff);
        
        float rc = 1.0f / (2.0f * M_PI * cutoff);
        float dt = 1.0f / sampleRate_;
        float alpha = dt / (rc + dt);
        
        float modRate = 0.2f;
        
        for (int i = 0; i < numFrames; i++) {
            phase_ += 2.0f * M_PI * modRate / sampleRate_;
            float mod = std::sin(phase_) * 0.15f * intensity_;
            
            for (int c = 0; c < channels; c++) {
                float sample = samples[i * channels + c];
                
                float filtered = alpha * sample + (1.0f - alpha) * filterState_[c];
                filterState_[c] = filtered;
                filtered = alpha * filtered + (1.0f - alpha) * filterState2_[c];
                filterState2_[c] = filtered;
                
                float modSample = filtered * (1.0f - std::abs(mod));
                
                samples[i * channels + c] = sample * (1.0f - intensity_) + modSample * intensity_;
            }
        }
    }
    
    void clear() override { 
        std::fill(filterState_, filterState_ + 2, 0.0f);
        std::fill(filterState2_, filterState2_ + 2, 0.0f);
        phase_ = 0.0f;
    }
    void setParameter(int paramId, float value) override { if (paramId == 0) intensity_ = std::clamp(value, 0.0f, 1.0f); }
    float getParameter(int paramId) const override { return paramId == 0 ? intensity_ : 0.0f; }
    EffectType getType() const override { return EffectType::Underwater; }
    std::string getName() const override { return "水下"; }
    std::string getCategory() const override { return "音质效果"; }

private:
    float intensity_;
    float phase_;
    float filterState_[2] = {0.0f, 0.0f};
    float filterState2_[2] = {0.0f, 0.0f};
};

class AlienSignalEffect : public AudioEffectBase {
public:
    AlienSignalEffect() : intensity_(0.5f), ringPhase_(0.0f), tremoloPhase_(0.0f) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        filter_.setBandpass(300.0f, 4000.0f, sampleRate_);
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || intensity_ < 0.001f) return;
        
        float carrierFreq1 = 25.0f + intensity_ * 30.0f;
        float carrierFreq2 = 80.0f + intensity_ * 100.0f;
        float tremoloRate = 1.5f + intensity_ * 2.0f;
        
        for (int i = 0; i < numFrames; i++) {
            ringPhase_ += 2.0f * M_PI * carrierFreq1 / sampleRate_;
            float carrier1 = std::sin(ringPhase_);
            
            float carrier2 = std::sin(ringPhase_ * (carrierFreq2 / carrierFreq1));
            
            tremoloPhase_ += 2.0f * M_PI * tremoloRate / sampleRate_;
            float tremolo = 0.6f + 0.4f * std::sin(tremoloPhase_);
            
            for (int c = 0; c < channels; c++) {
                float sample = samples[i * channels + c];
                
                float filtered = filter_.process(sample);
                
                float ringMod1 = filtered * carrier1 * 0.4f;
                float ringMod2 = filtered * carrier2 * 0.25f;
                
                float alien = (ringMod1 + ringMod2) * tremolo;
                
                samples[i * channels + c] = sample * (1.0f - intensity_) + alien * intensity_;
            }
        }
    }
    
    void clear() override { filter_.clear(); ringPhase_ = 0.0f; tremoloPhase_ = 0.0f; }
    void setParameter(int paramId, float value) override { if (paramId == 0) intensity_ = std::clamp(value, 0.0f, 1.0f); }
    float getParameter(int paramId) const override { return paramId == 0 ? intensity_ : 0.0f; }
    EffectType getType() const override { return EffectType::AlienSignal; }
    std::string getName() const override { return "外星信号"; }
    std::string getCategory() const override { return "音质效果"; }

private:
    float intensity_;
    float ringPhase_;
    float tremoloPhase_;
    SimpleFilter filter_;
};

class MegaphoneEffect : public AudioEffectBase {
public:
    MegaphoneEffect() : intensity_(0.5f) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        lowpass_.setLowpass(2200.0f, sampleRate_);
        highpass_.setHighpass(500.0f, sampleRate_);
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || intensity_ < 0.001f) return;
        
        for (int i = 0; i < numFrames; i++) {
            for (int c = 0; c < channels; c++) {
                float sample = samples[i * channels + c];
                
                float filtered = lowpass_.process(sample);
                filtered = highpass_.process(filtered);
                
                float softClipped = std::tanh(filtered * 2.5f);
                
                samples[i * channels + c] = sample * (1.0f - intensity_) + softClipped * intensity_;
            }
        }
    }
    
    void clear() override { lowpass_.clear(); highpass_.clear(); }
    void setParameter(int paramId, float value) override { if (paramId == 0) intensity_ = std::clamp(value, 0.0f, 1.0f); }
    float getParameter(int paramId) const override { return paramId == 0 ? intensity_ : 0.0f; }
    EffectType getType() const override { return EffectType::Megaphone; }
    std::string getName() const override { return "扩音器"; }
    std::string getCategory() const override { return "音质效果"; }

private:
    float intensity_;
    SimpleFilter lowpass_;
    SimpleFilter highpass_;
};

}

#endif
