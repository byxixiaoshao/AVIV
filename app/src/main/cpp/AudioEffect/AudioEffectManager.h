#ifndef AUDIO_EFFECT_MANAGER_H
#define AUDIO_EFFECT_MANAGER_H

#include "AudioEffectBase.h"
#include "InsulationEffect.h"
#include "CreativeEffects.h"
#include "ProfessionalEffects.h"
#include "EqualizerEffect.h"
#include <memory>
#include <vector>
#include <unordered_map>
#include <string>

namespace audiofx {

class AudioEffectManager {
public:
    AudioEffectManager() = default;
    ~AudioEffectManager() = default;
    
    void init(int sampleRate) {
        sampleRate_ = sampleRate;
        
        creativeEffects_.push_back(std::make_unique<InsulationEffect>());
        creativeEffects_.push_back(std::make_unique<LoFiEffect>());
        creativeEffects_.push_back(std::make_unique<EightBitEffect>());
        creativeEffects_.push_back(std::make_unique<UnderwaterEffect>());
        creativeEffects_.push_back(std::make_unique<AlienSignalEffect>());
        creativeEffects_.push_back(std::make_unique<MegaphoneEffect>());
        
        professionalEffects_.push_back(std::make_unique<PitchEffect>());
        professionalEffects_.push_back(std::make_unique<SpeedEffect>());
        
        qualityEffects_.push_back(std::make_unique<HiFiEffect>());
        qualityEffects_.push_back(std::make_unique<DistortionEffect>());
        
        equalizer_ = std::make_unique<EqualizerEffect>();
        equalizer_->init(sampleRate);
        
        for (auto& effect : creativeEffects_) {
            effect->init(sampleRate);
            effectsByType_[effect->getType()] = effect.get();
        }
        for (auto& effect : professionalEffects_) {
            effect->init(sampleRate);
            effectsByType_[effect->getType()] = effect.get();
        }
        for (auto& effect : qualityEffects_) {
            effect->init(sampleRate);
            effectsByType_[effect->getType()] = effect.get();
        }
        effectsByType_[EffectType::Equalizer] = equalizer_.get();
    }
    
    void setLimiterEnabled(bool enabled) { 
        limiterEnabled_ = enabled;
        if (equalizer_) {
            equalizer_->setLimiterEnabled(enabled);
        }
    }
    bool isLimiterEnabled() const { return limiterEnabled_; }
    
    void process(float* samples, int numFrames, int channels) {
        if (equalizer_ && equalizer_->isEnabled()) {
            equalizer_->process(samples, numFrames, channels);
        }
        
        for (auto& effect : creativeEffects_) {
            if (effect->isEnabled()) {
                effect->process(samples, numFrames, channels);
            }
        }
        for (auto& effect : professionalEffects_) {
            if (effect->isEnabled()) {
                effect->process(samples, numFrames, channels);
            }
        }
        for (auto& effect : qualityEffects_) {
            if (effect->isEnabled()) {
                effect->process(samples, numFrames, channels);
            }
        }
    }
    
    AudioEffectBase* getEffect(EffectType type) {
        auto it = effectsByType_.find(type);
        if (it != effectsByType_.end()) {
            return it->second;
        }
        return nullptr;
    }
    
    void setEffectEnabled(EffectType type, bool enabled) {
        auto* effect = getEffect(type);
        if (effect) {
            effect->setEnabled(enabled);
        }
    }
    
    void setEffectParameter(EffectType type, int paramId, float value) {
        auto* effect = getEffect(type);
        if (effect) {
            effect->setParameter(paramId, value);
        }
    }
    
    float getEffectParameter(EffectType type, int paramId) {
        auto* effect = getEffect(type);
        if (effect) {
            return effect->getParameter(paramId);
        }
        return 0.0f;
    }
    
    void clear() {
        for (auto& effect : creativeEffects_) effect->clear();
        for (auto& effect : professionalEffects_) effect->clear();
        for (auto& effect : qualityEffects_) effect->clear();
        if (equalizer_) equalizer_->clear();
    }
    
    const std::vector<std::unique_ptr<AudioEffectBase>>& getCreativeEffects() const { return creativeEffects_; }
    const std::vector<std::unique_ptr<AudioEffectBase>>& getProfessionalEffects() const { return professionalEffects_; }
    const std::vector<std::unique_ptr<AudioEffectBase>>& getQualityEffects() const { return qualityEffects_; }
    std::vector<std::unique_ptr<AudioEffectBase>>& getCreativeEffects() { return creativeEffects_; }
    std::vector<std::unique_ptr<AudioEffectBase>>& getQualityEffects() { return qualityEffects_; }
    
    EqualizerEffect* getEqualizer() { return equalizer_.get(); }

private:
    int sampleRate_ = 44100;
    bool limiterEnabled_ = true;
    
    std::vector<std::unique_ptr<AudioEffectBase>> creativeEffects_;
    std::vector<std::unique_ptr<AudioEffectBase>> professionalEffects_;
    std::vector<std::unique_ptr<AudioEffectBase>> qualityEffects_;
    std::unique_ptr<EqualizerEffect> equalizer_;
    
    std::unordered_map<EffectType, AudioEffectBase*> effectsByType_;
};

}

#endif
