#ifndef STEREO_WIDENER_H
#define STEREO_WIDENER_H

#include "AudioEffectBase.h"
#include <cmath>
#include <algorithm>

namespace audiofx {

class StereoWidener : public AudioEffectBase {
public:
    StereoWidener() : width_(0.5f), crossfeed_(0.0f), sampleRate_(44100) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        updateCoefficients();
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || (width_ < 0.01f && crossfeed_ < 0.01f) || channels < 2) return;
        
        for (int i = 0; i < numFrames; i++) {
            int idxL = i * channels;
            int idxR = idxL + 1;
            
            float left = samples[idxL];
            float right = samples[idxR];
            
            // Crossfeed: mix filtered+dumped L into R, R into L
            float cfL = cfLpL_.process(left) * crossfeed_;
            float cfR = cfLpR_.process(right) * crossfeed_;
            
            left += cfR;
            right += cfL;
            
            // Mid/Side stereo widening
            float mid = (left + right) * 0.5f;
            float side = (left - right) * 0.5f;
            
            float widenedSide = side * (1.0f + width_ * 2.0f);
            
            float widenedL = mid + widenedSide;
            float widenedR = mid - widenedSide;
            
            samples[idxL] = widenedL;
            samples[idxR] = widenedR;
        }
    }
    
    void clear() override {
        cfLpL_.clear();
        cfLpR_.clear();
    }
    
    void setParameter(int paramId, float value) override {
        if (paramId == 0) {
            width_ = std::clamp(value, 0.0f, 3.0f);
        } else if (paramId == 1) {
            crossfeed_ = std::clamp(value, 0.0f, 1.0f);
            updateCoefficients();
        }
    }
    
    float getParameter(int paramId) const override {
        if (paramId == 0) return width_;
        if (paramId == 1) return crossfeed_;
        return 0.0f;
    }
    
    EffectType getType() const override { return EffectType::StereoWidener; }
    std::string getName() const override { return "立体声展宽"; }
    std::string getCategory() const override { return "音质增强"; }

private:
    void updateCoefficients() {
        float delayMs = 0.3f + crossfeed_ * 0.7f;
        int delaySamples = static_cast<int>(delayMs * sampleRate_ / 1000.0f);
        delaySamples = std::max(1, std::min(delaySamples, static_cast<int>(kMaxDelay)));
        
        int delayIdx = delaySamples - 1;
        size_t newSize = static_cast<size_t>(delaySamples + 1);
        
        cfLpL_.setDelay(delayIdx);
        cfLpR_.setDelay(delayIdx);
        
        if (cfLpL_.needsResize(newSize)) {
            cfLpL_.resize(newSize);
            cfLpR_.resize(newSize);
        }
        
        float lpfFreq = 700.0f + crossfeed_ * 1300.0f;
        float rc = 1.0f / (2.0f * 3.14159265358979323846f * lpfFreq);
        float dt = 1.0f / sampleRate_;
        float alpha = dt / (rc + dt);
        cfLpL_.setAlpha(alpha);
        cfLpR_.setAlpha(alpha);
    }
    
    static constexpr int kMaxDelay = 48;
    
    struct DelayedLPFilter {
        float buffer[kMaxDelay + 1] = {};
        float alpha_ = 0.3f;
        int delayIndex_ = 5;
        int writePos_ = 0;
        int bufferSize_ = 6;
        float lpState_ = 0.0f;
        
        void setDelay(int idx) { delayIndex_ = idx; }
        void setAlpha(float a) { alpha_ = a; }
        bool needsResize(size_t s) { return s > static_cast<size_t>(kMaxDelay + 1); }
        void resize(size_t) {}
        
        float process(float input) {
            buffer[writePos_] = input;
            int readPos = writePos_ - delayIndex_;
            if (readPos < 0) readPos += bufferSize_;
            
            float delayed = buffer[readPos];
            lpState_ = alpha_ * delayed + (1.0f - alpha_) * lpState_;
            
            writePos_ = (writePos_ + 1) % bufferSize_;
            return lpState_;
        }
        
        void clear() {
            for (float& v : buffer) v = 0.0f;
            writePos_ = 0;
            lpState_ = 0.0f;
        }
    };
    
    float width_;
    float crossfeed_;
    int sampleRate_;
    DelayedLPFilter cfLpL_;
    DelayedLPFilter cfLpR_;
};

}

#endif
