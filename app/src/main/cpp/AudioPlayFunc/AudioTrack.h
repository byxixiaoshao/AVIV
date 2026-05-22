#ifndef AUDIO_TRACK_H
#define AUDIO_TRACK_H

#include <string>
#include <vector>
#include <array>
#include <atomic>
#include <mutex>
#include <shared_mutex>
#include <memory>
#include "../ffmpeg/FFmpegDecoder.h"
#include "../reverb/ReverbEffect.h"
#include "../AudioEffect/AudioEffectManager.h"

class SpatialAudioProcessor;

using audiofx::EQ_BAND_COUNT;

enum class PlaybackState {
    Stopped,
    Playing,
    Paused,
    FadingOut
};

enum class FadeState {
    None,
    FadingIn,
    FadingOut
};

struct TrackConfig {
    float volume = 1.0f;
    bool looping = true;
    bool effectEnabled = false;
    float roomSize = 0.0f;
    float decayTime = 1.5f;
    float damping = 0.0f;
    float wetLevel = 0.0f;
    float dryLevel = 1.0f;
    float insulation = 0.0f;
    float preDelay = 0.0f;
    float reflectionDensity = 0.5f;
    float reflectionSpread = 0.5f;
    float highpassCutoff = 100.0f;
    float earlyReflectionLevel = 0.0f;
    
    float loFiIntensity = 0.0f;
    float eightBitIntensity = 0.0f;
    float underwaterIntensity = 0.0f;
    float alienSignalIntensity = 0.0f;
    float megaphoneIntensity = 0.0f;
    
    float pitchIntensity = 0.0f;
    float speedIntensity = 1.0f;
    
    float hifiIntensity = 0.0f;
    float distortionIntensity = 0.0f;
    float noiseIntensity = 0.0f;
    
    std::array<float, EQ_BAND_COUNT> eqGains = {0.0f};
    bool eqEnabled = false;
    bool eqLimiterEnabled = true;
    
    std::vector<int> effectOrder = {0, 1, 2, 3};
};

class AudioTrack {
public:
    AudioTrack(const std::string& trackId, int32_t sampleRate, int32_t channelCount);
    ~AudioTrack();

    bool load(const std::string& filePath);
    bool loadFromFd(int fd, int64_t offset = 0, int64_t length = -1);
    void unload();
    bool isLoaded() const { return isLoaded_.load(); }

    void play();
    void pause();
    void stop();
    void seekTo(int64_t positionMs);

    PlaybackState getState() const { return state_.load(); }
    bool isPlaying() const { return state_.load() == PlaybackState::Playing; }

    void setVolume(float volume);
    float getVolume() const { return config_.volume; }

    void setLooping(bool looping);
    bool isLooping() const { return config_.looping; }

    void setEffectEnabled(bool enabled);
    bool isEffectEnabled() const { return config_.effectEnabled; }

    void setRoomSize(float value);
    void setDecayTime(float value);
    void setDamping(float value);
    void setWetLevel(float value);
    void setDryLevel(float value);
    void setInsulation(float value);
    void setPreDelay(float value);
    void setReflectionDensity(float density);
    void setReflectionSpread(float spread);
    void setHighpassCutoff(float cutoff);
    void setEarlyReflectionLevel(float level);
    
    void setCreativeEffectIntensity(audiofx::EffectType type, float intensity);
    
    void setEqBandGain(int bandIndex, float gain);
    float getEqBandGain(int bandIndex) const;
    void setEqEnabled(bool enabled);
    bool isEqEnabled() const { return config_.eqEnabled; }
    void setEqLimiterEnabled(bool enabled);
    bool isEqLimiterEnabled() const { return config_.eqLimiterEnabled; }
    void setEqGains(const std::array<float, EQ_BAND_COUNT>& gains);
    const std::array<float, EQ_BAND_COUNT>& getEqGains() const { return config_.eqGains; }

    void setSpatialEnabled(bool enabled);
    void setSpatialIntensity(float intensity);
    void setSpatialOffsetType(int type);
    void setSpatialFixedOffset(float leftRight, float upDown, float frontBack, float multiplier);
    void setSpatialSurroundParams(int mode, float radius, float speed);
    void setSpatialRandomParams(float maxDistance, float minDistance, float randomValue, float speed);
    
    void setEffectOrder(const std::vector<int>& order);

    int64_t getDuration() const;
    int64_t getPosition() const;

    const std::string& getTrackId() const { return trackId_; }

    void process(float* output, int32_t numFrames);
    
    void setFadeDuration(float durationSeconds);
    bool isFadingOut() const { return fadeState_.load() == FadeState::FadingOut; }
    void cancelFadeOut();
    void clearEffectBuffers();

private:
    void applyInsulation(float* samples, int32_t numFrames, int32_t channels);
    void updateCreativeEffects();
    void applyFade(float* samples, int32_t numFrames);
    void updateFade(int32_t numFrames);

    std::string trackId_;
    int32_t sampleRate_;
    int32_t channelCount_;

    std::unique_ptr<ffmpeg::FFmpegDecoder> decoder_;
    std::unique_ptr<reverb::ReverbEffect> reverb_;
    std::unique_ptr<audiofx::AudioEffectManager> effectManager_;
    std::unique_ptr<SpatialAudioProcessor> spatialProcessor_;

    std::vector<float> decodeBuffer_;
    std::vector<float> processBuffer_;
    std::vector<float> resampleBuffer_;
    std::vector<float> timeStretchBuffer_;

    std::atomic<PlaybackState> state_{PlaybackState::Stopped};
    std::atomic<bool> isLoaded_{false};

    TrackConfig config_;
    mutable std::shared_mutex mutex_;

    int64_t currentPositionMs_{0};
    int64_t durationMs_{0};

    float insulationState1_{0.0f};
    float insulationState2_{0.0f};
    
    std::atomic<FadeState> fadeState_{FadeState::None};
    std::atomic<float> fadeProgress_{0.0f};
    std::atomic<float> fadeDuration_{0.5f};
    std::atomic<float> fadeVolume_{1.0f};
    
    double resamplePosition_ = 0.0;
    double timeStretchPosition_ = 0.0;
    float prevSamples_[4] = {0.0f, 0.0f, 0.0f, 0.0f};
    
    static constexpr int WSOLA_FRAME_SIZE = 512;
    static constexpr int WSOLA_OVERLAP = 128;
    std::vector<float> wsolaOverlapBuffer_;
    int wsolaOverlapSamples_ = 0;
};

#endif
