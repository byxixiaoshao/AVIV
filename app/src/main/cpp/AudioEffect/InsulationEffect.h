#ifndef INSULATION_EFFECT_H
#define INSULATION_EFFECT_H

#include "AudioEffectBase.h"
#include <cmath>
#include <algorithm>

namespace audiofx {

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

class InsulationEffect : public AudioEffectBase {
public:
    InsulationEffect() {
        insulation_ = 0.0f;
        for (int c = 0; c < 2; c++) {
            lowState_[c] = midState1_[c] = midState2_[c] = highState_[c] = 0.0f;
            resonState1_[c] = resonState2_[c] = 0.0f;
        }
    }
    
    ~InsulationEffect() override = default;
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || insulation_ <= 0.001f) return;
        
        float mappedInsulation = std::pow(insulation_, 0.6f);
        
        float lowCutoff = 400.0f + mappedInsulation * 200.0f;
        float midCutoff = 1500.0f - mappedInsulation * 800.0f;
        float highCutoff = 4000.0f - mappedInsulation * 2500.0f;
        
        float lowGain = 1.0f - mappedInsulation * 0.1f;
        float midGain = 1.0f - mappedInsulation * 0.4f;
        float highGain = 1.0f - mappedInsulation * 0.7f;
        
        float resonFreq = 180.0f + mappedInsulation * 120.0f;
        float resonQ = 2.0f + mappedInsulation * 3.0f;
        float resonGain = 0.15f * mappedInsulation;
        
        float dt = 1.0f / sampleRate_;
        
        float lowAlpha = dt / (dt + 1.0f / (2.0f * M_PI * lowCutoff));
        float midAlpha = dt / (dt + 1.0f / (2.0f * M_PI * midCutoff));
        float highAlpha = dt / (dt + 1.0f / (2.0f * M_PI * highCutoff));
        
        float omega = 2.0f * M_PI * resonFreq / sampleRate_;
        float sinOmega = std::sin(omega);
        float cosOmega = std::cos(omega);
        float alpha = sinOmega / (2.0f * resonQ);
        
        float b0 = alpha;
        float b1 = 0.0f;
        float b2 = -alpha;
        float a0 = 1.0f + alpha;
        float a1 = -2.0f * cosOmega;
        float a2 = 1.0f - alpha;
        
        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0;
        
        for (int i = 0; i < numFrames; ++i) {
            for (int c = 0; c < channels && c < 2; ++c) {
                int idx = i * channels + c;
                float input = samples[idx];
                
                float low = lowAlpha * input + (1.0f - lowAlpha) * lowState_[c];
                lowState_[c] = low;
                
                float mid = midAlpha * input + (1.0f - midAlpha) * midState1_[c];
                midState1_[c] = mid;
                mid = midAlpha * mid + (1.0f - midAlpha) * midState2_[c];
                midState2_[c] = mid;
                
                float high = highAlpha * input + (1.0f - highAlpha) * highState_[c];
                highState_[c] = high;
                
                float multiband = low * lowGain + mid * midGain + high * highGain;
                
                float reson = b0 * input + b1 * resonState1_[c] + b2 * resonState2_[c]
                            - a1 * resonState1_[c] - a2 * resonState2_[c];
                resonState2_[c] = resonState1_[c];
                resonState1_[c] = reson;
                
                float output = multiband + reson * resonGain;
                
                samples[idx] = input * (1.0f - mappedInsulation) + output * mappedInsulation;
            }
        }
    }
    
    void clear() override {
        for (int c = 0; c < 2; c++) {
            lowState_[c] = midState1_[c] = midState2_[c] = highState_[c] = 0.0f;
            resonState1_[c] = resonState2_[c] = 0.0f;
        }
    }
    
    void setParameter(int paramId, float value) override {
        if (paramId == 0) {
            insulation_ = std::clamp(value, 0.0f, 1.0f);
        }
    }
    
    float getParameter(int paramId) const override {
        if (paramId == 0) return insulation_;
        return 0.0f;
    }
    
    EffectType getType() const override { return EffectType::Insulation; }
    std::string getName() const override { return "隔音系数"; }
    std::string getCategory() const override { return "空间环境类"; }

private:
    float insulation_;
    
    float lowState_[2];
    float midState1_[2];
    float midState2_[2];
    float highState_[2];
    
    float resonState1_[2];
    float resonState2_[2];
};

}

#endif
