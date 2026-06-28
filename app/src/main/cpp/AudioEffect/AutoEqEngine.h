#ifndef AUTO_EQ_ENGINE_H
#define AUTO_EQ_ENGINE_H

#include <array>
#include <cmath>
#include <string>
#include <algorithm>
#include <cstring>

namespace audiofx {

constexpr int AUTO_EQ_BANDS = 12;

// Speaker compensation presets: gain in dB per band
// Bands: 25Hz, 50Hz, 100Hz, 200Hz, 400Hz, 800Hz, 1.6kHz, 3.2kHz, 6.3kHz, 10kHz, 14kHz, 16kHz
using CompensationCurve = std::array<float, AUTO_EQ_BANDS>;

inline CompensationCurve getSpeakerCompensation(const std::string& preset) {
    if (preset == "phone") {
        // Phone speaker: severe low-frequency rolloff, harsh midrange peak
        // Compensation: boost lows, cut harsh mids, smooth highs
        return {6.0f, 5.0f, 4.0f, 2.0f, 0.0f, -1.0f, -2.0f, -3.0f, -1.0f, 0.5f, 1.0f, 1.5f};
    }
    if (preset == "earphone") {
        // Earphone/Headphone: typically V-shaped or neutral
        // Compensation: mild adjustments for typical earphone response
        return {2.0f, 1.5f, 1.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.5f, 1.0f, 1.5f, 1.5f, 2.0f};
    }
    if (preset == "bluetooth") {
        // Bluetooth speaker: often bass-boosted, lacks clarity
        // Compensation: tame bass, enhance mids and highs
        return {-2.0f, -1.5f, -1.0f, 0.0f, 1.0f, 1.5f, 2.0f, 2.5f, 2.0f, 1.5f, 1.0f, 1.0f};
    }
    if (preset == "car") {
        // Car audio: road noise masks highs, boomy bass
        // Compensation: cut boomy bass, boost presence and highs
        return {-3.0f, -2.0f, -1.0f, 0.0f, 1.0f, 1.5f, 2.0f, 3.0f, 3.5f, 4.0f, 4.0f, 4.0f};
    }
    // Default: flat (no compensation)
    return {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
}

class AutoEqEngine {
public:
    AutoEqEngine() = default;

    void init(int sampleRate) {
        sampleRate_ = sampleRate;
        clear();
    }

    void process(float* samples, int numFrames, int channels) {
        // Static compensation mode: no real-time processing needed
        // Gains are pre-computed and applied directly to EQ bands
        // This function is kept for API compatibility but does nothing
    }

    void clear() {
        for (int b = 0; b < AUTO_EQ_BANDS; b++) {
            currentGains_[b] = 0.0f;
        }
    }

    // --- Setters ---
    void setEnabled(bool e) {
        enabled_ = e;
        if (!e) {
            // Clear gains when disabled
            for (int b = 0; b < AUTO_EQ_BANDS; b++) {
                currentGains_[b] = 0.0f;
            }
        } else {
            // Recompute gains when enabled
            updateGains();
        }
    }
    bool isEnabled() const { return enabled_; }

    void setSpeakerPreset(const std::string& preset) {
        speakerPreset_ = preset;
        baseCurve_ = getSpeakerCompensation(preset);
        updateGains();
    }
    const std::string& getSpeakerPreset() const { return speakerPreset_; }

    void setIntensity(float v) { intensity_ = std::clamp(v, 0.0f, 1.0f); updateGains(); }
    float getIntensity() const { return intensity_; }

    void setBassBias(float v) { bassBias_ = std::clamp(v, -12.0f, 12.0f); updateGains(); }
    void setMidBias(float v) { midBias_ = std::clamp(v, -12.0f, 12.0f); updateGains(); }
    void setTrebleBias(float v) { trebleBias_ = std::clamp(v, -12.0f, 12.0f); updateGains(); }

    void setBrightnessTarget(float db) { brightnessTarget_ = std::clamp(db, -12.0f, 12.0f); updateGains(); }
    void setLoudnessTarget(float db) { loudnessTarget_ = std::clamp(db, -12.0f, 12.0f); updateGains(); }

    const std::array<float, AUTO_EQ_BANDS>& getCurrentGains() const { return currentGains_; }

private:
    void updateGains() {
        // Always compute gains regardless of enabled_ state
        // The enabled_ state only affects whether gains are applied to EQ
        for (int b = 0; b < AUTO_EQ_BANDS; b++) {
            // Start with base compensation curve
            float gain = baseCurve_[b];

            // Apply intensity (0 = no compensation, 1 = full compensation)
            gain *= intensity_;

            // Apply frequency bias (12 bands: 0-3 bass, 4-7 mid, 8-11 treble)
            float bias = bassBias_;
            if (b >= 4 && b < 8) bias = midBias_;
            if (b >= 8) bias = trebleBias_;
            gain += bias * intensity_;

            // Apply brightness: boost high bands, cut low bands
            // Center at band 5.5 (between 800Hz and 1.6kHz)
            float bright = brightnessTarget_ * (static_cast<float>(b - 5.5f) / 6.0f);
            gain += bright * intensity_;

            // Apply loudness: boost both lows and highs
            float loud = loudnessTarget_ * std::abs(static_cast<float>(b - 5.5f)) / 6.0f;
            gain += loud * intensity_;

            // Clamp to reasonable range
            currentGains_[b] = std::clamp(gain, -24.0f, 24.0f);
        }
    }

    int sampleRate_ = 44100;
    bool enabled_ = false;

    std::string speakerPreset_ = "phone";
    CompensationCurve baseCurve_ = {0.0f};

    float intensity_ = 0.5f;
    float bassBias_ = 0.0f;
    float midBias_ = 0.0f;
    float trebleBias_ = 0.0f;
    float brightnessTarget_ = 0.0f;
    float loudnessTarget_ = 0.0f;

    std::array<float, AUTO_EQ_BANDS> currentGains_ = {0.0f};
};

}

#endif
