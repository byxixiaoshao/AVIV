#ifndef AUTO_EQ_ENGINE_H
#define AUTO_EQ_ENGINE_H

#include <array>
#include <cmath>
#include <string>
#include <vector>
#include <algorithm>
#include <cstring>
#include <unordered_map>

namespace audiofx {

// Per-filter user override for AutoEQ bands.
// When present for a given band index, the override replaces the corresponding
// gain / frequency / Q that would otherwise be auto-computed by the engine.
struct AutoEqFilterOverride {
    float gainDb      = 0.0f;
    float frequencyHz = 1000.0f;
    float q           = 1.0f;
    bool  active      = false;
};

// 12-band reference speaker compensation presets (gain in dB)
// Reference bands: 25, 50, 100, 200, 400, 800, 1600, 3200, 6300, 10000, 14000, 16000 Hz
constexpr int REF_BANDS = 12;
constexpr float REF_FREQS[REF_BANDS] = {25, 50, 100, 200, 400, 800, 1600, 3200, 6300, 10000, 14000, 16000};

// Frequency range boundaries for band allocation
constexpr float FREQ_LOW_START  = 20.0f;
constexpr float FREQ_LOW_END    = 250.0f;
constexpr float FREQ_MID_START  = 250.0f;
constexpr float FREQ_MID_END    = 4000.0f;
constexpr float FREQ_HIGH_START = 4000.0f;
constexpr float FREQ_HIGH_END   = 20000.0f;

using RefCurve = std::array<float, REF_BANDS>;

inline RefCurve getSpeakerRefCurve(const std::string& preset) {
    if (preset == "phone") {
        return {6.0f, 5.0f, 4.0f, 2.0f, 0.0f, -1.0f, -2.0f, -3.0f, -1.0f, 0.5f, 1.0f, 1.5f};
    }
    if (preset == "earphone") {
        return {2.0f, 1.5f, 1.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.5f, 1.0f, 1.5f, 1.5f, 2.0f};
    }
    if (preset == "bluetooth") {
        return {-2.0f, -1.5f, -1.0f, 0.0f, 1.0f, 1.5f, 2.0f, 2.5f, 2.0f, 1.5f, 1.0f, 1.0f};
    }
    if (preset == "car") {
        return {-3.0f, -2.0f, -1.0f, 0.0f, 1.0f, 1.5f, 2.0f, 3.0f, 3.5f, 4.0f, 4.0f, 4.0f};
    }
    return {0.0f};
}

class AutoEqEngine {
public:
    AutoEqEngine() {
        bands_.resize(1);
        bandFrequencies_.resize(1);
    }

    void init(int sampleRate) {
        sampleRate_ = sampleRate;
        computeAttackReleaseCoeffs();
        recomputeAll();
    }

    // --- Real-time per-frame gain smoothing ---
    void process(float* /*samples*/, int numFrames, int /*channels*/) {
        if (!enabled_ || numFrames <= 0) return;

        for (int frame = 0; frame < numFrames; frame++) {
            for (size_t b = 0; b < bands_.size(); b++) {
                auto& band = bands_[b];
                float coeff = (band.targetGain > band.currentGain) ? attackCoeff_ : releaseCoeff_;
                band.currentGain += coeff * (band.targetGain - band.currentGain);
            }
        }
    }

    void clear() {
        for (auto& band : bands_) {
            band.targetGain = 0.0f;
            band.currentGain = 0.0f;
        }
    }

    // --- Enable/Disable ---
    void setEnabled(bool e) {
        enabled_ = e;
        if (!e) {
            for (auto& band : bands_) {
                band.targetGain = 0.0f;
                band.currentGain = 0.0f;
            }
        } else {
            updateGains();
        }
    }
    bool isEnabled() const { return enabled_; }

    // --- Speaker preset ---
    void setSpeakerPreset(const std::string& preset) {
        speakerPreset_ = preset;
        updateGains();
    }
    const std::string& getSpeakerPreset() const { return speakerPreset_; }

    // --- Core parameters ---
    void setIntensity(float v) { intensity_ = std::clamp(v, 0.0f, 1.0f); updateGains(); }
    float getIntensity() const { return intensity_; }

    void setBassBias(float v)  { bassBias_ = std::clamp(v, -12.0f, 12.0f); updateGains(); }
    void setMidBias(float v)   { midBias_ = std::clamp(v, -12.0f, 12.0f); updateGains(); }
    void setTrebleBias(float v){ trebleBias_ = std::clamp(v, -12.0f, 12.0f); updateGains(); }

    void setBrightnessTarget(float db) { brightnessTarget_ = std::clamp(db, -12.0f, 12.0f); updateGains(); }
    void setLoudnessTarget(float db)   { loudnessTarget_ = std::clamp(db, -12.0f, 12.0f); updateGains(); }

    // --- Pro parameters ---
    void setAttack(float ms) {
        attackMs_ = std::clamp(ms, 1.0f, 500.0f);
        computeAttackReleaseCoeffs();
    }
    float getAttack() const { return attackMs_; }

    void setRelease(float ms) {
        releaseMs_ = std::clamp(ms, 10.0f, 2000.0f);
        computeAttackReleaseCoeffs();
    }
    float getRelease() const { return releaseMs_; }

    void setMaxSlope(float dbPerOctave) { maxSlope_ = std::clamp(dbPerOctave, 1.0f, 48.0f); updateGains(); }
    float getMaxSlope() const { return maxSlope_; }

    void setMaxBoost(float db) { maxBoost_ = std::clamp(db, 0.0f, 24.0f); updateGains(); }
    float getMaxBoost() const { return maxBoost_; }

    void setMaxCut(float db) { maxCut_ = std::clamp(db, 0.0f, 24.0f); updateGains(); }
    float getMaxCut() const { return maxCut_; }

    void setCouplingCoeff(float coeff) { couplingCoeff_ = std::clamp(coeff, 0.0f, 1.0f); updateGains(); }
    float getCouplingCoeff() const { return couplingCoeff_; }

    void setHysteresis(float db) { hysteresisDb_ = std::clamp(db, 0.0f, 6.0f); updateGains(); }
    float getHysteresis() const { return hysteresisDb_; }

    void setDynamicQEnabled(bool enabled) { dynamicQEnabled_ = enabled; }
    bool isDynamicQEnabled() const { return dynamicQEnabled_; }

    // --- Band configuration ---
    void setBandCount(int count) {
        bandCount_ = std::clamp(count, 0, 256);
        // Drop overrides that no longer fit the new band range
        for (auto it = filterOverrides_.begin(); it != filterOverrides_.end(); ) {
            if (it->first < 0 || it->first >= bandCount_) it = filterOverrides_.erase(it);
            else ++it;
        }
        recomputeAll();
    }
    int getBandCount() const { return bandCount_; }

    void setBandRatios(float low, float mid) {
        // high = 1.0 - low - mid
        float total = low + mid;
        float high = 1.0f - total;
        constexpr float minRatio = 0.20f;
        if (low < minRatio || mid < minRatio || high < minRatio) return;
        lowRatio_ = low;
        midRatio_ = mid;
        recomputeAll();
    }
    float getLowRatio() const { return lowRatio_; }
    float getMidRatio() const { return midRatio_; }

    // --- Per-filter overrides (user-editable gain / frequency / Q) ---
    // Setting an override marks the band as user-edited; the override is applied
    // when the curve is pushed to the BiQuad pool (see applyAutoEqToEq).
    void setFilterOverride(int bandIndex, float gainDb, float freqHz, float q) {
        if (bandIndex < 0 || bandIndex >= bandCount_) return;
        AutoEqFilterOverride& o = filterOverrides_[bandIndex];
        o.gainDb      = std::clamp(gainDb, -12.0f, 12.0f);
        o.frequencyHz = std::clamp(freqHz,  20.0f, 20000.0f);
        o.q           = std::clamp(q,        0.1f, 10.0f);
        o.active      = true;
    }
    void clearFilterOverride(int bandIndex) {
        filterOverrides_.erase(bandIndex);
    }
    void clearAllFilterOverrides() {
        filterOverrides_.clear();
    }
    bool hasFilterOverride(int bandIndex) const {
        auto it = filterOverrides_.find(bandIndex);
        return it != filterOverrides_.end() && it->second.active;
    }
    const AutoEqFilterOverride* getFilterOverride(int bandIndex) const {
        auto it = filterOverrides_.find(bandIndex);
        if (it == filterOverrides_.end() || !it->second.active) return nullptr;
        return &it->second;
    }
    const std::unordered_map<int, AutoEqFilterOverride>& getFilterOverrides() const {
        return filterOverrides_;
    }

    // --- Output ---
    const std::vector<float>& getBandFrequencies() const { return bandFrequencies_; }
    const std::vector<float>& getCurrentGains() const { return currentGains_; }

    // DynamicQ factor: pow(2, -gain_dB / 12.0), clamped [0.5, 2.0]
    static float computeDynamicQFactor(float gainDb) {
        return std::clamp(std::pow(2.0f, -gainDb / 12.0f), 0.5f, 2.0f);
    }

private:
    void recomputeAll() {
        computeBandFrequencies();
        computeAttackReleaseCoeffs();
        updateGains();
    }

    void computeBandFrequencies() {
        if (bandCount_ <= 0) {
            bands_.resize(1);
            bandFrequencies_.resize(1);
            currentGains_.resize(1);
            bands_[0] = {};
            bandFrequencies_[0] = 20.0f;
            currentGains_[0] = 0.0f;
            prevTargetGains_.resize(1);
            prevTargetGains_[0] = 0.0f;
            return;
        }

        float totalRatio = lowRatio_ + midRatio_ + (1.0f - lowRatio_ - midRatio_);
        int lowBands  = std::max(0, (int)std::round(bandCount_ * lowRatio_ / totalRatio));
        int midBands  = std::max(0, (int)std::round(bandCount_ * midRatio_ / totalRatio));
        int highBands = bandCount_ - lowBands - midBands;

        // Each range needs at least 1 band if ratio > 0
        if (lowRatio_ > 0.001f && lowBands == 0) lowBands = 1;
        if (midRatio_ > 0.001f && midBands == 0) midBands = 1;
        if (highBands <= 0 && (1.0f - lowRatio_ - midRatio_) > 0.001f) highBands = 1;

        // Rebalance if needed
        int total = lowBands + midBands + highBands;
        while (total < bandCount_) { lowBands++; total++; }
        while (total > bandCount_ && lowBands > 0) { lowBands--; total--; }
        while (total > bandCount_ && midBands > 0) { midBands--; total--; }
        while (total > bandCount_ && highBands > 0) { highBands--; total--; }

        bands_.resize(bandCount_);
        bandFrequencies_.resize(bandCount_);
        currentGains_.resize(bandCount_);
        prevTargetGains_.resize(bandCount_);

        int idx = 0;

        // Logarithmic spacing: f_i = f_start * (f_end / f_start)^(i / (N-1))
        for (int i = 0; i < lowBands && idx < bandCount_; i++, idx++) {
            float t = (lowBands > 1) ? (float)i / (lowBands - 1) : 0.5f;
            bandFrequencies_[idx] = FREQ_LOW_START * std::pow(FREQ_LOW_END / FREQ_LOW_START, t);
            bands_[idx] = {};
            currentGains_[idx] = 0.0f;
            prevTargetGains_[idx] = 0.0f;
        }
        for (int i = 0; i < midBands && idx < bandCount_; i++, idx++) {
            float t = (midBands > 1) ? (float)i / (midBands - 1) : 0.5f;
            bandFrequencies_[idx] = FREQ_MID_START * std::pow(FREQ_MID_END / FREQ_MID_START, t);
            bands_[idx] = {};
            currentGains_[idx] = 0.0f;
            prevTargetGains_[idx] = 0.0f;
        }
        for (int i = 0; i < highBands && idx < bandCount_; i++, idx++) {
            float t = (highBands > 1) ? (float)i / (highBands - 1) : 0.5f;
            bandFrequencies_[idx] = FREQ_HIGH_START * std::pow(FREQ_HIGH_END / FREQ_HIGH_START, t);
            bands_[idx] = {};
            currentGains_[idx] = 0.0f;
            prevTargetGains_[idx] = 0.0f;
        }
    }

    void computeAttackReleaseCoeffs() {
        float sr = (float)sampleRate_;
        // attackCoeff = 1 - exp(-1 / (attack_s * sr)), clamped to [0, 1]
        attackCoeff_ = 1.0f - std::exp(-1.0f / ((attackMs_ / 1000.0f) * sr));
        releaseCoeff_ = 1.0f - std::exp(-1.0f / ((releaseMs_ / 1000.0f) * sr));
    }

    void updateGains() {
        if (bandCount_ <= 0) return;

        // 1. Interpolate reference speaker curve to current band frequencies
        RefCurve ref = getSpeakerRefCurve(speakerPreset_);

        for (int b = 0; b < bandCount_; b++) {
            float freq = bandFrequencies_[b];

            // Linear interpolate from 12-band reference
            float refGain = 0.0f;
            if (freq <= REF_FREQS[0]) {
                refGain = ref[0];
            } else if (freq >= REF_FREQS[REF_BANDS - 1]) {
                refGain = ref[REF_BANDS - 1];
            } else {
                int i = 0;
                while (i < REF_BANDS - 1 && REF_FREQS[i + 1] < freq) i++;
                float t = (std::log10(freq) - std::log10(REF_FREQS[i])) /
                         (std::log10(REF_FREQS[i + 1]) - std::log10(REF_FREQS[i]));
                refGain = ref[i] + t * (ref[i + 1] - ref[i]);
            }

            // 2. Start with base compensation curve × intensity
            float gain = refGain * intensity_;

            // 3. Three-band bias (map band frequency to low/mid/high)
            float bias;
            if (freq < FREQ_MID_START) {
                bias = bassBias_;
            } else if (freq < FREQ_HIGH_START) {
                bias = midBias_;
            } else {
                bias = trebleBias_;
            }
            gain += bias * intensity_;

            // 4. Brightness tilt: centered at 1kHz (midpoint of mid range)
            float normFreq = (std::log10(freq) - std::log10(1000.0f)) / 2.0f;
            gain += brightnessTarget_ * normFreq * intensity_;

            // 5. Loudness contour: V-shape centered at 1kHz
            float loudShape = std::abs(std::log10(freq) - std::log10(1000.0f)) / 2.0f;
            gain += loudnessTarget_ * loudShape * intensity_;

            bands_[b].targetGain = gain;
        }

        // 6. Apply MaxSlope: limit adjacent band gain differences
        if (maxSlope_ > 0.0f && bandCount_ > 1) {
            std::vector<float> slopeLimited(bandCount_);
            slopeLimited[0] = bands_[0].targetGain;
            for (int b = 1; b < bandCount_; b++) {
                float octaves = std::log2(bandFrequencies_[b] / bandFrequencies_[b - 1]);
                float maxDiff = maxSlope_ * octaves;
                slopeLimited[b] = std::clamp(bands_[b].targetGain,
                    slopeLimited[b - 1] - maxDiff,
                    slopeLimited[b - 1] + maxDiff);
            }
            // Reverse pass
            for (int b = bandCount_ - 2; b >= 0; b--) {
                float octaves = std::log2(bandFrequencies_[b + 1] / bandFrequencies_[b]);
                float maxDiff = maxSlope_ * octaves;
                slopeLimited[b] = std::clamp(slopeLimited[b],
                    slopeLimited[b + 1] - maxDiff,
                    slopeLimited[b + 1] + maxDiff);
            }
            for (int b = 0; b < bandCount_; b++) {
                bands_[b].targetGain = slopeLimited[b];
            }
        }

        // 7. Apply CouplingCoeff: blend with neighbors
        if (couplingCoeff_ > 0.0f && bandCount_ > 1) {
            std::vector<float> coupled(bandCount_);
            for (int b = 0; b < bandCount_; b++) {
                float sum = bands_[b].targetGain;
                float weight = 1.0f;
                if (b > 0) {
                    sum += bands_[b - 1].targetGain * couplingCoeff_;
                    weight += couplingCoeff_;
                }
                if (b < bandCount_ - 1) {
                    sum += bands_[b + 1].targetGain * couplingCoeff_;
                    weight += couplingCoeff_;
                }
                coupled[b] = sum / weight;
            }
            for (int b = 0; b < bandCount_; b++) {
                bands_[b].targetGain = coupled[b];
            }
        }

        // 8. Apply Hysteresis: skip small changes
        for (int b = 0; b < bandCount_; b++) {
            float delta = std::abs(bands_[b].targetGain - prevTargetGains_[b]);
            if (delta < hysteresisDb_ && prevTargetGains_[b] != 0.0f) {
                bands_[b].targetGain = prevTargetGains_[b];
            }
            prevTargetGains_[b] = bands_[b].targetGain;
        }

        // 9. Clamp with MaxBoost/MaxCut
        for (int b = 0; b < bandCount_; b++) {
            bands_[b].targetGain = std::clamp(bands_[b].targetGain, -maxCut_, maxBoost_);
            currentGains_[b] = bands_[b].targetGain;
        }
    }

    int sampleRate_ = 44100;
    bool enabled_ = false;
    std::string speakerPreset_ = "phone";

    // Core parameters
    float intensity_ = 0.5f;
    float bassBias_ = 0.0f;
    float midBias_ = 0.0f;
    float trebleBias_ = 0.0f;
    float brightnessTarget_ = 0.0f;
    float loudnessTarget_ = 0.0f;

    // Pro parameters
    float attackMs_ = 100.0f;
    float releaseMs_ = 200.0f;
    float maxSlope_ = 10.0f;
    float maxBoost_ = 12.0f;
    float maxCut_ = 12.0f;
    float couplingCoeff_ = 0.3f;
    float hysteresisDb_ = 1.0f;
    bool dynamicQEnabled_ = true;
    float attackCoeff_ = 0.0f;
    float releaseCoeff_ = 0.0f;

    // Band configuration
    int bandCount_ = 12;
    float lowRatio_ = 0.33f;
    float midRatio_ = 0.34f;

    // Per-band state
    struct BandState {
        float targetGain = 0.0f;
        float currentGain = 0.0f;
    };
    std::vector<BandState> bands_;
    std::vector<float> bandFrequencies_;
    std::vector<float> currentGains_;
    std::vector<float> prevTargetGains_;

    // User-editable per-filter overrides (key = band index)
    std::unordered_map<int, AutoEqFilterOverride> filterOverrides_;
};

}

#endif
