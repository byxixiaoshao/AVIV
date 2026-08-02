#ifndef PROFESSIONAL_EFFECTS_H
#define PROFESSIONAL_EFFECTS_H

#include "AudioEffectBase.h"
#include <cmath>
#include <algorithm>
#include <vector>

namespace audiofx {

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

/**
 * SOLA (Synchronized Overlap-Add) granular pitch shifter.
 * Uses Hann-windowed grains with cross-correlation alignment
 * for artifact-free real-time pitch shifting.
 */
class PitchEffect : public AudioEffectBase {
public:
    PitchEffect() : semitones_(0.0f) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        maxRing_ = sampleRate_ * 2;  // 2 seconds ring buffer
        grainSize_ = 1024;
        synthesisHop_ = grainSize_ / 2;  // 50% output overlap
        
        for (int c = 0; c < 2; c++) {
            ringBuffer_[c].assign(maxRing_, 0.0f);
            lastGrain_[c].assign(grainSize_, 0.0f);
        }
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || std::abs(semitones_) < 0.1f) return;
        int ch = std::min(channels, 2);
        
        float pitchRatio = std::pow(2.0f, semitones_ / 12.0f);
        float idealHop = (float)synthesisHop_ / pitchRatio;
        float targetFill = maxRing_ * 0.4f;
        
        // Save input to temp (planar)
        std::vector<float> tmp[2];
        for (int c = 0; c < ch; c++) {
            tmp[c].resize(numFrames);
            for (int i = 0; i < numFrames; i++) tmp[c][i] = samples[i * channels + c];
        }
        
        // Process each channel
        for (int c = 0; c < ch; c++) {
            processChannel(tmp[c].data(), numFrames, pitchRatio, idealHop, targetFill, c);
        }
        
        // Write back interleaved
        for (int i = 0; i < numFrames; i++) {
            for (int c = 0; c < ch; c++) samples[i * channels + c] = tmp[c][i];
        }
    }
    
    void clear() override {
        for (int c = 0; c < 2; c++) {
            std::fill(ringBuffer_[c].begin(), ringBuffer_[c].end(), 0.0f);
            std::fill(lastGrain_[c].begin(), lastGrain_[c].end(), 0.0f);
            writePos_[c] = 0;
            grainReadPos_[c] = 0;
            buffered_[c] = 0;
        }
    }
    
    void setParameter(int paramId, float value) override { 
        if (paramId == 0) {
            semitones_ = std::clamp(value, -12.0f, 12.0f);
            clear();
        }
    }
    float getParameter(int paramId) const override { return paramId == 0 ? semitones_ : 0.0f; }
    EffectType getType() const override { return EffectType::Pitch; }
    std::string getName() const override { return "声调"; }
    std::string getCategory() const override { return "专业处理"; }

private:
    float semitones_;
    
    // Per-channel ring buffers and state
    std::vector<float> ringBuffer_[2];
    int maxRing_ = 0;
    int writePos_[2] = {0, 0};
    int buffered_[2] = {0, 0};
    
    // SOLA
    int grainSize_ = 1024;
    int synthesisHop_ = 512;
    static constexpr int CORR_LEN = 128;
    static constexpr int CORR_SEARCH = 64;
    
    int grainReadPos_[2] = {0, 0};
    std::vector<float> lastGrain_[2];
    
    /** Process one channel */
    void processChannel(float* samples, int numFrames, float pitchRatio,
                        float idealHop, float targetFill, int chIdx) {
        auto& rb = ringBuffer_[chIdx];
        auto& wpos = writePos_[chIdx];
        auto& rpos = grainReadPos_[chIdx];
        auto& bufCnt = buffered_[chIdx];
        auto& lg = lastGrain_[chIdx];
        
        // 1. Write input to ring buffer
        for (int i = 0; i < numFrames; i++) {
            rb[wpos] = samples[i];
            wpos = (wpos + 1) % maxRing_;
            bufCnt++;
        }
        
        // 2. Clear output
        std::fill(samples, samples + numFrames, 0.0f);
        
        // 3. Generate grains
        int written = 0;
        int safety = grainSize_ + CORR_SEARCH;
        
        while (written < numFrames && bufCnt > safety) {
            // Adaptive hop
            float fillRatio = (float)bufCnt / targetFill;
            float hopAdj = 0.85f + fillRatio * 0.3f;  // 0.85..1.15
            float actualHop = idealHop * hopAdj;
            
            // Correlation alignment
            int offset = 0;
            if (written > 0) offset = findCorrelation(rb, rpos, actualHop, lg, chIdx);
            
            int srcPos = (rpos + (int)actualHop + offset + maxRing_) % maxRing_;
            
            // Generate Hann-windowed grain, overlap-add to output
            int avail = numFrames - written;
            int outHop = std::min(synthesisHop_, avail);
            
            for (int j = 0; j < grainSize_; j++) {
                float val = rb[(srcPos + j) % maxRing_];
                float w = 0.5f * (1.0f - std::cos(2.0f * M_PI * j / (grainSize_ - 1)));
                float ws = val * w;
                
                // Overlap-add each output sample
                for (int k = 0; k < outHop && (written + k) < numFrames; k++) {
                    int outIdx = written + k;
                    if (j >= k && j < k + grainSize_) {
                        float alpha = (float)(j - k) / (grainSize_ - outHop);
                        alpha = std::clamp(alpha, 0.0f, 1.0f);
                        samples[outIdx] += ws + lg[j] * (1.0f - alpha);
                    }
                }
                
                // Store windowed grain for next overlap
                lg[j] = ws;
            }
            
            // Advance
            int consumed = (int)actualHop + offset;
            rpos = (rpos + consumed) % maxRing_;
            bufCnt = std::max(0, bufCnt - consumed);
            written += outHop;
        }
        
        // Fallback: pass through with attenuation
        for (int i = 0; i < (numFrames - written) && bufCnt > 0; i++) {
            samples[written + i] = rb[(rpos + i) % maxRing_] * 0.4f;
        }
        rpos = (rpos + (numFrames - written)) % maxRing_;
        bufCnt = std::max(0, bufCnt - (numFrames - written));
    }
    
    int findCorrelation(std::vector<float>& rb, int rpos, float idealHop,
                        std::vector<float>& lg, int chIdx) {
        float best = -1e9f;
        int bestOff = 0;
        int start = (int)idealHop - CORR_SEARCH / 2;
        int end = (int)idealHop + CORR_SEARCH / 2;
        
        for (int off = start; off <= end; off++) {
            int base = (rpos + off + maxRing_) % maxRing_;
            float corr = 0.0f;
            for (int j = 0; j < CORR_LEN; j++) {
                corr += rb[(base + j) % maxRing_] * lg[grainSize_ - CORR_LEN + j];
            }
            if (corr > best) { best = corr; bestOff = off; }
        }
        return std::clamp(bestOff - (int)idealHop, -CORR_SEARCH/2, CORR_SEARCH/2);
    }
};


/**
 * SOLA time stretching / compression.
 * Uses crossfaded grain overlap with dynamic rate adjustment
 * to prevent buffer overflow/underrun in real-time.
 */
class SpeedEffect : public AudioEffectBase {
public:
    SpeedEffect() : speed_(1.0f) {}
    
    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        maxRing_ = sampleRate_ * 2;
        grainSize_ = 1024;
        overlapLen_ = grainSize_ / 4;
        
        for (int c = 0; c < 2; c++) {
            ring_[c].assign(maxRing_, 0.0f);
            grain_[c].assign(grainSize_, 0.0f);
        }
        clear();
    }
    
    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || std::abs(speed_ - 1.0f) < 0.01f) return;
        int ch = std::min(channels, 2);
        
        float readRate = speed_;
        float targetFill = maxRing_ * 0.4f;
        
        // 1. Write input to ring buffers
        for (int i = 0; i < numFrames; i++) {
            for (int c = 0; c < ch; c++) {
                ring_[c][writePos_] = samples[i * channels + c];
            }
            writePos_ = (writePos_ + 1) % maxRing_;
            buffered_ = std::min(buffered_ + 1, maxRing_);
        }
        
        // 2. Generate output
        for (int i = 0; i < numFrames; i++) {
            // Check buffer
            if (buffered_ < grainSize_) {
                for (int c = 0; c < ch; c++) samples[i * channels + c] = 0.0f;
                continue;
            }
            
            // Adaptive read rate
            float fillRatio = (float)buffered_ / targetFill;
            float adjRate = readRate;
            if (fillRatio > 1.5f) adjRate *= 1.15f;
            else if (fillRatio < 0.4f) adjRate *= 0.85f;
            
            // Extract new grain when needed
            if (needGrain_ || grainPos_ >= grainSize_) {
                int grainStart = (int)readPosFloat_;
                if (grainStart + grainSize_ >= maxRing_) grainStart = maxRing_ - grainSize_;
                grainStart = std::max(0, grainStart);
                
                // Correlation alignment
                int offset = 0;
                if (haveGrain_) {
                    float best = -1e9f;
                    int range = overlapLen_ / 2;
                    for (int off = -range; off <= range; off++) {
                        int tst = grainStart + off;
                        if (tst < 0 || tst + overlapLen_ >= maxRing_) continue;
                        float corr = 0.0f;
                        for (int j = 0; j < overlapLen_; j++) corr += ring_[0][tst + j] * grain_[0][j];
                        if (corr > best) { best = corr; offset = off; }
                    }
                }
                
                int actual = std::clamp(grainStart + offset, 0, maxRing_ - grainSize_);
                for (int c = 0; c < ch; c++)
                    std::copy(&ring_[c][actual], &ring_[c][actual + grainSize_], grain_[c].begin());
                
                grainPos_ = 0;
                needGrain_ = false;
                haveGrain_ = true;
            }
            
            // Output with crossfade
            if (grainPos_ < overlapLen_ && haveGrain_) {
                float alpha = (float)(grainPos_ + 1) / overlapLen_;
                for (int c = 0; c < ch; c++) {
                    // Blend new grain head with old tail from ring buffer
                    int oldIdx = ((int)readPosFloat_ - overlapLen_ + grainPos_ + maxRing_) % maxRing_;
                    samples[i * channels + c] = grain_[c][grainPos_] * alpha + ring_[c][oldIdx] * (1.0f - alpha);
                }
            } else {
                for (int c = 0; c < ch; c++)
                    samples[i * channels + c] = grain_[c][grainPos_];
            }
            
            grainPos_++;
            
            // Advance read position
            readPosFloat_ += adjRate;
            int consumed = (int)readPosFloat_ - prevReadInt_;
            prevReadInt_ = (int)readPosFloat_;
            buffered_ = std::max(0, buffered_ - consumed);
            
            if (grainPos_ >= grainSize_ - overlapLen_) needGrain_ = true;
        }
    }
    
    void clear() override {
        for (int c = 0; c < 2; c++) {
            std::fill(ring_[c].begin(), ring_[c].end(), 0.0f);
            std::fill(grain_[c].begin(), grain_[c].end(), 0.0f);
        }
        writePos_ = 0;
        readPosFloat_ = 0.0f;
        prevReadInt_ = 0;
        buffered_ = 0;
        grainPos_ = 0;
        needGrain_ = true;
        haveGrain_ = false;
    }
    
    void setParameter(int paramId, float value) override { 
        if (paramId == 0) speed_ = std::clamp(value, 0.3f, 3.0f); 
    }
    float getParameter(int paramId) const override { return paramId == 0 ? speed_ : 1.0f; }
    EffectType getType() const override { return EffectType::Speed; }
    std::string getName() const override { return "速度"; }
    std::string getCategory() const override { return "专业处理"; }

private:
    float speed_;
    
    std::vector<float> ring_[2];
    std::vector<float> grain_[2];
    int maxRing_ = 0;
    int writePos_ = 0;
    float readPosFloat_ = 0.0f;
    int prevReadInt_ = 0;
    int buffered_ = 0;
    
    int grainSize_ = 1024;
    int overlapLen_ = 256;
    int grainPos_ = 0;
    bool needGrain_ = true;
    bool haveGrain_ = false;
};

class HiFiEffect : public AudioEffectBase {
public:
    HiFiEffect() : intensity_(0.0f) {}

    void init(int sampleRate) override {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        // DC blocker for a ~20 Hz high-pass: strips the DC pumped by the
        // sample*sample term while keeping the musical 2nd harmonic.
        dcCoeff_ = 1.0f - std::min(1.0f, static_cast<float>(2.0f * M_PI * 20.0f / sampleRate_));
        clear();
    }

    void process(float* samples, int numFrames, int channels) override {
        if (!enabled_ || intensity_ < 0.001f) return;
        int ch = std::min(channels, 2);

        // Ramp intensity per sample (~25 ms at 44.1 kHz) so slider moves and
        // (re)enable never cause a gain jump -> no click on parameter changes.
        const float kSmooth = 0.999f;
        for (int i = 0; i < numFrames; i++) {
            smoothIntensity_ += (intensity_ - smoothIntensity_) * (1.0f - kSmooth);
            float inten = smoothIntensity_;

            for (int c = 0; c < ch; c++) {
                int idx = i * channels + c;
                float sample = samples[idx];

                // While fading in, keep prevSample_ tracking so the transient
                // detector never fires against a stale value at full strength.
                if (inten < 0.001f) {
                    prevSample_[c] = sample;
                    continue;
                }

                // Transient (derivative) exciter. Original used a hard 0.02
                // threshold that switched the term on/off -> waveform step =
                // audible click. softKnee gives a continuous gate instead.
                float diff = sample - prevSample_[c];
                diff = std::clamp(diff, -2.0f, 2.0f);  // 限制瞬态项幅度，防止前级过载时爆音
                float attack = std::abs(diff);
                float gate = softKnee(attack, 0.01f, 0.06f);
                float transient = diff * inten * 0.8f * gate;

                // Even (2nd) harmonic from squaring. sample*sample is always
                // >= 0, i.e. a DC offset that pumps the waveform / woofer.
                // DC-block it: keeps cos(2w), drops the DC term.
                // 平方项对输入幅度敏感：前级（如多段压缩 intensity>1）过载时 sample 可远超 1，
                // 平方后急剧放大推入 tanh 饱和区产生刺耳失真。用 clamp 后的值计算平方项。
                float clampedForSquare = std::clamp(sample, -1.0f, 1.0f);
                float evenHarmonic = dcBlock(clampedForSquare * clampedForSquare * inten * 0.08f, c);

                // Expansion bounded to a safe range (original could exceed
                // unity on loud peaks, over-driving the tanh into harshness).
                float envelope = std::abs(sample);
                float expansion = std::clamp(
                    1.0f + (envelope - 0.5f) * inten * 0.4f, 0.8f, 1.2f);

                // Combine, pre-limit so tanh stays in its soft region, and
                // soft-clip. Output remains bounded to [-0.95, 0.95].
                float output = (sample + transient + evenHarmonic) * expansion;
                output = std::clamp(output, -3.0f, 3.0f);

                prevSample_[c] = sample;
                samples[idx] = std::tanh(output) * 0.95f;
            }
        }
    }

    void clear() override {
        std::fill(prevSample_, prevSample_ + 2, 0.0f);
        std::fill(dcPrevIn_, dcPrevIn_ + 2, 0.0f);
        std::fill(dcPrevOut_, dcPrevOut_ + 2, 0.0f);
        smoothIntensity_ = 0.0f;  // fade in on (re)enable to avoid attack pop
    }

    // Base setEnabled only flips the flag; reset state on re-enable so a stale
    // prevSample_ (frozen while disabled) doesn't fire a transient click.
    void setEnabled(bool enabled) override {
        if (enabled && !enabled_) {
            std::fill(prevSample_, prevSample_ + 2, 0.0f);
            std::fill(dcPrevIn_, dcPrevIn_ + 2, 0.0f);
            std::fill(dcPrevOut_, dcPrevOut_ + 2, 0.0f);
            smoothIntensity_ = 0.0f;
        }
        enabled_ = enabled;
    }

    void setParameter(int paramId, float value) override {
        if (paramId == 0) intensity_ = std::clamp(value, 0.0f, 1.0f);
    }
    float getParameter(int paramId) const override { return paramId == 0 ? intensity_ : 0.0f; }
    EffectType getType() const override { return EffectType::HiFi; }
    std::string getName() const override { return "伪还原二次处理"; }
    std::string getCategory() const override { return "音质增强"; }

private:
    float intensity_;
    float smoothIntensity_ = 0.0f;        // ramped copy used in process()
    float prevSample_[2] = {0.0f, 0.0f};
    float dcCoeff_ = 0.997f;
    float dcPrevIn_[2] = {0.0f, 0.0f};
    float dcPrevOut_[2] = {0.0f, 0.0f};

    // Continuous 0..1 ramp over [low, high]; replaces the hard ternary gate.
    static float softKnee(float x, float low, float high) {
        if (x <= low) return 0.0f;
        if (x >= high) return 1.0f;
        float t = (x - low) / (high - low);
        return t * t * (3.0f - 2.0f * t);  // smoothstep
    }

    // One-pole DC blocker (high-pass): y[n] = x[n] - x[n-1] + R*y[n-1].
    float dcBlock(float x, int c) {
        float y = x - dcPrevIn_[c] + dcCoeff_ * dcPrevOut_[c];
        dcPrevIn_[c] = x;
        dcPrevOut_[c] = y;
        return y;
    }
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

