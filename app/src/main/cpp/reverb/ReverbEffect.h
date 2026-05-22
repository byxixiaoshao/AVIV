#ifndef REVERB_EFFECT_H
#define REVERB_EFFECT_H

#include <vector>
#include <cmath>

namespace reverb {

class ReverbEffect {
public:
    ReverbEffect();
    ~ReverbEffect();

    void init(int sampleRate);
    
    void setRoomSize(float room_size);
    void setDecayTime(float decay_time);
    void setDamping(float damping);
    void setWetLevel(float wet_level);
    void setDryLevel(float dry_level);
    void setPreDelay(float delayMs);
    
    void setReflectionDensity(float density);
    void setReflectionSpread(float spread);
    void setHighpassCutoff(float cutoff);
    void setEarlyReflectionLevel(float level);
    
    void setSourcePosition(float azimuth, float elevation, float distance);
    void setSpatialReflectionEnabled(bool enabled);

    void process(float* input, float* output, int num_samples);
    void process(float* samples, int num_samples);
    void processInterleaved(float* samples, int numFrames, int channels);
    
    void clear();

private:
    void updateDelays();
    float softClip(float x);
    void calculateReflectionGains(float azimuth, float elevation, 
                                   float& leftGain, float& rightGain,
                                   float& earlyLeftGain, float& earlyRightGain);
    float calculateDistanceWetRatio(float distance);

    int sampleRate_;
    float room_size_;
    float decay_time_;
    float damping_;
    float wet_level_;
    float dry_level_;
    float pre_delay_ms_;
    
    float reflection_density_;
    float reflection_spread_;
    float highpass_cutoff_;
    float early_reflection_level_;
    
    float source_azimuth_;
    float source_elevation_;
    float source_distance_;
    bool spatial_reflection_enabled_;
    
    float hp_state_l_;
    float hp_state_r_;
    
    float wet_hp_state_l_;
    float wet_hp_state_r_;
    float wet_hp_prev_l_;
    float wet_hp_prev_r_;
    
    float smooth_left_gain_;
    float smooth_right_gain_;
    float smooth_early_left_;
    float smooth_early_right_;

    struct CombFilter {
        std::vector<float> buffer;
        int write_pos;
        int delay_samples;
        float feedback;
        float damp_state;
        
        CombFilter() : write_pos(0), delay_samples(0), feedback(0.0f), damp_state(0.0f) {}
        
        void init(int max_size) {
            buffer.resize(max_size, 0.0f);
            write_pos = 0;
            damp_state = 0.0f;
        }
        
        void setDelay(int samples) {
            delay_samples = std::min(samples, static_cast<int>(buffer.size()) - 1);
        }
        
        float process(float input, float damping) {
            int read_pos = write_pos - delay_samples;
            if (read_pos < 0) read_pos += buffer.size();
            
            float delayed = buffer[read_pos];
            
            damp_state = delayed * (1.0f - damping) + damp_state * damping;
            
            float output = damp_state;
            buffer[write_pos] = input + output * feedback;
            
            write_pos = (write_pos + 1) % buffer.size();
            
            return output;
        }
        
        void clear() {
            std::fill(buffer.begin(), buffer.end(), 0.0f);
            write_pos = 0;
            damp_state = 0.0f;
        }
    };

    struct AllPassFilter {
        std::vector<float> buffer;
        int write_pos;
        int delay_samples;
        float feedback;
        
        AllPassFilter() : write_pos(0), delay_samples(0), feedback(0.5f) {}
        
        void init(int max_size) {
            buffer.resize(max_size, 0.0f);
            write_pos = 0;
        }
        
        void setDelay(int samples) {
            delay_samples = std::min(samples, static_cast<int>(buffer.size()) - 1);
        }
        
        float process(float input) {
            int read_pos = write_pos - delay_samples;
            if (read_pos < 0) read_pos += buffer.size();
            
            float delayed = buffer[read_pos];
            float output = -input + delayed;
            buffer[write_pos] = input + delayed * feedback;
            
            write_pos = (write_pos + 1) % buffer.size();
            
            return output;
        }
        
        void clear() {
            std::fill(buffer.begin(), buffer.end(), 0.0f);
            write_pos = 0;
        }
    };

    struct PreDelay {
        std::vector<float> buffer;
        int write_pos;
        int delay_samples;
        
        PreDelay() : write_pos(0), delay_samples(0) {}
        
        void init(int max_size) {
            buffer.resize(max_size, 0.0f);
            write_pos = 0;
        }
        
        void setDelay(int samples) {
            delay_samples = std::min(samples, static_cast<int>(buffer.size()) - 1);
        }
        
        float process(float input) {
            if (delay_samples <= 0) {
                return input;
            }
            
            int read_pos = write_pos - delay_samples;
            if (read_pos < 0) read_pos += buffer.size();
            
            float output = buffer[read_pos];
            buffer[write_pos] = input;
            
            write_pos = (write_pos + 1) % buffer.size();
            
            return output;
        }
        
        void clear() {
            std::fill(buffer.begin(), buffer.end(), 0.0f);
            write_pos = 0;
        }
    };

    struct EarlyReflection {
        std::vector<float> left_buffer;
        std::vector<float> right_buffer;
        int write_pos;
        
        static constexpr int NUM_TAPS = 8;
        int left_delays[NUM_TAPS];
        int right_delays[NUM_TAPS];
        float left_gains[NUM_TAPS];
        float right_gains[NUM_TAPS];
        
        EarlyReflection() : write_pos(0) {
            for (int i = 0; i < NUM_TAPS; i++) {
                left_delays[i] = 0;
                right_delays[i] = 0;
                left_gains[i] = 0.0f;
                right_gains[i] = 0.0f;
            }
        }
        
        void init(int sampleRate) {
            int max_delay = static_cast<int>(0.1f * sampleRate);
            left_buffer.resize(max_delay, 0.0f);
            right_buffer.resize(max_delay, 0.0f);
            write_pos = 0;
            
            float base_delays[NUM_TAPS] = {0.005f, 0.011f, 0.019f, 0.027f, 
                                            0.037f, 0.047f, 0.059f, 0.073f};
            
            for (int i = 0; i < NUM_TAPS; i++) {
                int delay_samples = static_cast<int>(base_delays[i] * sampleRate);
                left_delays[i] = delay_samples;
                right_delays[i] = delay_samples + static_cast<int>((i % 2 == 0 ? 0.001f : -0.001f) * sampleRate);
                
                float gain = 0.6f / (1.0f + i * 0.3f);
                left_gains[i] = gain;
                right_gains[i] = gain;
            }
        }
        
        void process(float input, float& left_out, float& right_out,
                     float left_gain_mod, float right_gain_mod) {
            left_buffer[write_pos] = input;
            right_buffer[write_pos] = input;
            
            left_out = 0.0f;
            right_out = 0.0f;
            
            int buffer_size = static_cast<int>(left_buffer.size());
            
            for (int i = 0; i < NUM_TAPS; i++) {
                int left_read = write_pos - left_delays[i];
                if (left_read < 0) left_read += buffer_size;
                
                int right_read = write_pos - right_delays[i];
                if (right_read < 0) right_read += buffer_size;
                
                left_out += left_buffer[left_read] * left_gains[i] * left_gain_mod;
                right_out += right_buffer[right_read] * right_gains[i] * right_gain_mod;
            }
            
            write_pos = (write_pos + 1) % buffer_size;
        }
        
        void clear() {
            std::fill(left_buffer.begin(), left_buffer.end(), 0.0f);
            std::fill(right_buffer.begin(), right_buffer.end(), 0.0f);
            write_pos = 0;
        }
    };

    CombFilter comb_filters_[8];
    AllPassFilter allpass_filters_[4];
    PreDelay pre_delay_;
    EarlyReflection early_reflection_;

    static constexpr int NUM_COMBS = 8;
    static constexpr int NUM_ALLPASSES = 4;
    
    static const int comb_delays_[8];
    static const int allpass_delays_[4];
};

}

#endif
