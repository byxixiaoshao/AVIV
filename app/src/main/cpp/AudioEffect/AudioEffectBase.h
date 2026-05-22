#ifndef AUDIO_EFFECT_BASE_H
#define AUDIO_EFFECT_BASE_H

#include <string>

namespace audiofx {

enum class EffectType {
    None = 0,
    
    Insulation = 100,
    
    LoFi = 400,
    EightBit,
    Underwater,
    AlienSignal,
    Megaphone,
    
    Pitch = 500,
    Speed,
    HiFi,
    Distortion,
    Noise,
    
    Equalizer = 600
};

class AudioEffectBase {
public:
    virtual ~AudioEffectBase() = default;
    
    virtual void init(int sampleRate) = 0;
    virtual void process(float* samples, int numFrames, int channels) = 0;
    virtual void clear() = 0;
    
    virtual void setParameter(int paramId, float value) = 0;
    virtual float getParameter(int paramId) const = 0;
    
    virtual EffectType getType() const = 0;
    virtual std::string getName() const = 0;
    virtual std::string getCategory() const = 0;
    
    virtual bool isEnabled() const { return enabled_; }
    virtual void setEnabled(bool enabled) { enabled_ = enabled; }

protected:
    int sampleRate_ = 44100;
    bool enabled_ = true;
};

}

#endif
