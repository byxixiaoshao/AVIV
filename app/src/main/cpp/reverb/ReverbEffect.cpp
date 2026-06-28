#include "ReverbEffect.h"
#include <algorithm>

namespace reverb {

const int ReverbEffect::comb_delays_[8] = {
    1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617
};

const int ReverbEffect::allpass_delays_[4] = {
    556, 441, 341, 225
};

ReverbEffect::ReverbEffect()
    : sampleRate_(44100)
    , room_size_(0.0f)
    , decay_time_(1.5f)
    , damping_(0.0f)
    , wet_level_(0.0f)
    , dry_level_(1.0f)
    , pre_delay_ms_(0.0f)
    , reflection_density_(0.5f)
    , reflection_spread_(0.5f)
    , highpass_cutoff_(100.0f)
    , early_reflection_level_(0.0f)
    , source_azimuth_(0.0f)
    , source_elevation_(0.0f)
    , source_distance_(1.0f)
    , spatial_reflection_enabled_(false)
    , hp_state_l_(0.0f)
    , hp_state_r_(0.0f)
    , wet_hp_state_l_(0.0f)
    , wet_hp_state_r_(0.0f)
    , wet_hp_prev_l_(0.0f)
    , wet_hp_prev_r_(0.0f)
    , smooth_left_gain_(1.0f)
    , smooth_right_gain_(1.0f)
    , smooth_early_left_(1.0f)
    , smooth_early_right_(1.0f) {
}

ReverbEffect::~ReverbEffect() {
}

void ReverbEffect::init(int sampleRate) {
    sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
    
    float scale = static_cast<float>(sampleRate_) / 44100.0f;
    
    int max_delay = static_cast<int>(2000 * scale) + 100;
    
    for (int i = 0; i < NUM_COMBS; i++) {
        comb_filters_[i].init(max_delay);
    }
    
    for (int i = 0; i < NUM_ALLPASSES; i++) {
        allpass_filters_[i].init(max_delay);
    }
    
    pre_delay_.init(static_cast<int>(0.15f * sampleRate_) + 1);
    early_reflection_.init(sampleRate_);
    
    updateDelays();
    clear();
}

void ReverbEffect::updateDelays() {
    float scale = static_cast<float>(sampleRate_) / 44100.0f;
    
    float size_scale = 0.1f + room_size_ * 0.9f;
    
    for (int i = 0; i < NUM_COMBS; i++) {
        int delay = static_cast<int>(comb_delays_[i] * scale * size_scale);
        delay = std::max(delay, 16);
        comb_filters_[i].setDelay(delay);
    }
    
    for (int i = 0; i < NUM_ALLPASSES; i++) {
        int delay = static_cast<int>(allpass_delays_[i] * scale);
        delay = std::max(delay, 8);
        allpass_filters_[i].setDelay(delay);
    }
    
    int pre_delay_samples = static_cast<int>(pre_delay_ms_ * sampleRate_ / 1000.0f);
    pre_delay_.setDelay(pre_delay_samples);
    
    float effective_decay = std::max(decay_time_, 0.1f);
    
    float feedback = 0.5f + (effective_decay / 10.0f) * 0.49f;
    feedback = std::clamp(feedback, 0.5f, 0.99f);
    
    for (int i = 0; i < NUM_COMBS; i++) {
        comb_filters_[i].feedback = feedback;
    }
    
    for (int i = 0; i < NUM_ALLPASSES; i++) {
        allpass_filters_[i].feedback = 0.5f;
    }
}

float ReverbEffect::softClip(float x) {
    return std::tanh(x);
}

void ReverbEffect::setRoomSize(float room_size) {
    room_size_ = std::clamp(room_size, 0.0f, 1.0f);
    updateDelays();
}

void ReverbEffect::setDecayTime(float decay_time) {
    decay_time_ = std::clamp(decay_time, 0.1f, 10.0f);
    updateDelays();
}

void ReverbEffect::setDamping(float damping) {
    damping_ = std::clamp(damping, 0.0f, 1.0f);
}

void ReverbEffect::setWetLevel(float wet_level) {
    wet_level_ = std::clamp(wet_level, 0.0f, 1.0f);
}

void ReverbEffect::setDryLevel(float dry_level) {
    dry_level_ = std::clamp(dry_level, 0.0f, 1.0f);
}

void ReverbEffect::setPreDelay(float delayMs) {
    pre_delay_ms_ = std::clamp(delayMs, 0.0f, 100.0f);
    int pre_delay_samples = static_cast<int>(pre_delay_ms_ * sampleRate_ / 1000.0f);
    pre_delay_.setDelay(pre_delay_samples);
}

void ReverbEffect::setReflectionDensity(float density) {
    reflection_density_ = std::clamp(density, 0.0f, 1.0f);
    updateDelays();
}

void ReverbEffect::setReflectionSpread(float spread) {
    reflection_spread_ = std::clamp(spread, 0.0f, 1.0f);
}

void ReverbEffect::setHighpassCutoff(float cutoff) {
    highpass_cutoff_ = std::clamp(cutoff, 20.0f, 500.0f);
}

void ReverbEffect::setEarlyReflectionLevel(float level) {
    early_reflection_level_ = std::clamp(level, 0.0f, 1.0f);
}

void ReverbEffect::setSourcePosition(float azimuth, float elevation, float distance) {
    source_azimuth_ = azimuth;
    source_elevation_ = elevation;
    source_distance_ = std::max(0.1f, distance);
}

void ReverbEffect::setSpatialReflectionEnabled(bool enabled) {
    spatial_reflection_enabled_ = enabled;
}

void ReverbEffect::calculateReflectionGains(float azimuth, float elevation,
                                             float& leftGain, float& rightGain,
                                             float& earlyLeftGain, float& earlyRightGain) {
    float azimuthRad = azimuth * 3.14159265f / 180.0f;
    
    float cosAzimuth = std::cos(azimuthRad);
    float sinAzimuth = std::sin(azimuthRad);
    
    leftGain = 0.5f * (1.0f - sinAzimuth);
    rightGain = 0.5f * (1.0f + sinAzimuth);
    
    float frontness = std::cos(azimuthRad);
    float elevationFactor = std::cos(elevation * 3.14159265f / 180.0f);
    
    float sideReflection = 0.3f * std::abs(sinAzimuth);
    
    if (azimuth > 0) {
        earlyLeftGain = 1.0f + sideReflection;
        earlyRightGain = 1.0f - sideReflection * 0.5f;
    } else {
        earlyLeftGain = 1.0f - sideReflection * 0.5f;
        earlyRightGain = 1.0f + sideReflection;
    }
    
    earlyLeftGain *= elevationFactor;
    earlyRightGain *= elevationFactor;
    
    earlyLeftGain = std::clamp(earlyLeftGain, 0.3f, 1.5f);
    earlyRightGain = std::clamp(earlyRightGain, 0.3f, 1.5f);
}

float ReverbEffect::calculateDistanceWetRatio(float distance) {
    float normalizedDistance = std::max(0.1f, distance) / 5.0f;
    normalizedDistance = std::min(normalizedDistance, 2.0f);
    
    float wetRatio = 0.3f + normalizedDistance * 0.5f;
    return std::clamp(wetRatio, 0.3f, 1.3f);
}

void ReverbEffect::clear() {
    for (int i = 0; i < NUM_COMBS; i++) {
        comb_filters_[i].clear();
    }
    for (int i = 0; i < NUM_ALLPASSES; i++) {
        allpass_filters_[i].clear();
    }
    pre_delay_.clear();
    early_reflection_.clear();
    hp_state_l_ = 0.0f;
    hp_state_r_ = 0.0f;
    wet_hp_state_l_ = 0.0f;
    wet_hp_state_r_ = 0.0f;
    wet_hp_prev_l_ = 0.0f;
    wet_hp_prev_r_ = 0.0f;
}

void ReverbEffect::process(float* input, float* output, int num_samples) {
    process(input, num_samples);
    for (int i = 0; i < num_samples; i++) {
        output[i] = input[i];
    }
}

void ReverbEffect::process(float* samples, int num_samples) {
    float effective_damping = 0.1f + damping_ * 0.8f;
    float effective_wet = wet_level_ * room_size_;
    
    float hp_alpha = std::exp(-2.0f * 3.14159265f * highpass_cutoff_ / static_cast<float>(sampleRate_));
    
    for (int i = 0; i < num_samples; ++i) {
        float dry_sample = samples[i];
        
        float hp_out = dry_sample - hp_state_l_;
        hp_state_l_ = dry_sample * (1.0f - hp_alpha) + hp_state_l_ * hp_alpha;
        
        float delayed = pre_delay_.process(hp_out);
        
        float comb_sum = 0.0f;
        for (int j = 0; j < NUM_COMBS; j++) {
            comb_sum += comb_filters_[j].process(delayed, effective_damping);
        }
        comb_sum /= NUM_COMBS;
        
        float wet_sample = comb_sum;
        for (int j = 0; j < NUM_ALLPASSES; j++) {
            wet_sample = allpass_filters_[j].process(wet_sample);
        }
        
        wet_sample = std::tanh(wet_sample * 0.7f);
        
        float out = dry_sample * dry_level_ + wet_sample * effective_wet;
        samples[i] = std::clamp(out, -1.0f, 1.0f);
    }
}

void ReverbEffect::processInterleaved(float* samples, int numFrames, int channels) {
    if (channels == 1) {
        process(samples, numFrames);
        return;
    }
    
    float effective_damping = 0.1f + damping_ * 0.8f;
    float effective_wet = wet_level_ * room_size_;
    
    float hp_alpha = std::exp(-2.0f * 3.14159265f * highpass_cutoff_ / static_cast<float>(sampleRate_));
    constexpr float GAIN_SMOOTHING = 0.02f;
    
    float wet_hp_alpha = std::exp(-2.0f * 3.14159265f * highpass_cutoff_ / static_cast<float>(sampleRate_));
    
    float leftGain, rightGain, earlyLeftGain, earlyRightGain;
    if (spatial_reflection_enabled_) {
        calculateReflectionGains(source_azimuth_, source_elevation_,
                                  leftGain, rightGain, earlyLeftGain, earlyRightGain);
    } else {
        leftGain = 1.0f;
        rightGain = 1.0f;
        earlyLeftGain = 1.0f;
        earlyRightGain = 1.0f;
    }
    
    smooth_left_gain_ += (leftGain - smooth_left_gain_) * GAIN_SMOOTHING;
    smooth_right_gain_ += (rightGain - smooth_right_gain_) * GAIN_SMOOTHING;
    smooth_early_left_ += (earlyLeftGain - smooth_early_left_) * GAIN_SMOOTHING;
    smooth_early_right_ += (earlyRightGain - smooth_early_right_) * GAIN_SMOOTHING;
    
    float density_gain[NUM_COMBS];
    for (int j = 0; j < NUM_COMBS; j++) {
        if (reflection_density_ > 0.7f) {
            density_gain[j] = 1.0f;
        } else {
            float phase = static_cast<float>(j) / static_cast<float>(NUM_COMBS - 1);
            float center_weight = 1.0f - std::abs(phase - 0.5f) * 2.0f;
            float density_effect = (0.7f - reflection_density_) / 0.7f;
            density_gain[j] = 0.5f + center_weight * 0.5f * (1.0f - density_effect);
            density_gain[j] = std::max(density_gain[j], 0.3f);
        }
    }
    
    float spread_factor = reflection_spread_;
    
    for (int i = 0; i < numFrames; ++i) {
        float input_left = samples[i * channels];
        float input_right = samples[i * channels + 1];
        
        float input_mono = (input_left + input_right) * 0.5f;
        
        float hp_out = input_mono - hp_state_l_;
        hp_state_l_ = input_mono * (1.0f - hp_alpha) + hp_state_l_ * hp_alpha;
        
        float delayed = pre_delay_.process(hp_out);
        
        float early_left = 0.0f, early_right = 0.0f;
        if (spatial_reflection_enabled_ || early_reflection_level_ > 0.0f) {
            early_reflection_.process(delayed, early_left, early_right,
                                       smooth_early_left_, smooth_early_right_);
            early_left *= early_reflection_level_;
            early_right *= early_reflection_level_;
        }
        
        float comb_sum = 0.0f;
        for (int j = 0; j < NUM_COMBS; j++) {
            comb_sum += comb_filters_[j].process(delayed, effective_damping) * density_gain[j];
        }
        comb_sum /= NUM_COMBS;
        
        float wet_sample = comb_sum;
        for (int j = 0; j < NUM_ALLPASSES; j++) {
            wet_sample = allpass_filters_[j].process(wet_sample);
        }
        
        wet_sample = std::tanh(wet_sample * 0.7f);
        
        float wet_left = wet_sample * smooth_left_gain_;
        float wet_right = wet_sample * smooth_right_gain_;
        
        float spread_amount = spread_factor * 0.3f;
        float wet_mid = (wet_left + wet_right) * 0.5f;
        float wet_side = (wet_left - wet_right) * 0.5f;
        wet_side *= (1.0f + spread_amount);
        wet_left = wet_mid + wet_side;
        wet_right = wet_mid - wet_side;
        
        wet_left += early_left;
        wet_right += early_right;
        
        float wet_hp_out_l = wet_hp_alpha * (wet_hp_state_l_ + wet_left - wet_hp_prev_l_);
        wet_hp_state_l_ = wet_hp_out_l;
        wet_hp_prev_l_ = wet_left;
        
        float wet_hp_out_r = wet_hp_alpha * (wet_hp_state_r_ + wet_right - wet_hp_prev_r_);
        wet_hp_state_r_ = wet_hp_out_r;
        wet_hp_prev_r_ = wet_right;
        
        float out_left = input_left * dry_level_ + wet_hp_out_l * effective_wet;
        float out_right = input_right * dry_level_ + wet_hp_out_r * effective_wet;
        
        samples[i * channels] = std::clamp(out_left, -1.0f, 1.0f);
        samples[i * channels + 1] = std::clamp(out_right, -1.0f, 1.0f);
    }
}

}
