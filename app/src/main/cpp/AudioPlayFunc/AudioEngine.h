#ifndef AUDIO_ENGINE_H
#define AUDIO_ENGINE_H

#include <oboe/Oboe.h>
#include <unordered_map>
#include <mutex>
#include <shared_mutex>
#include <memory>
#include <vector>
#include <atomic>
#include <string>
#include "AudioTrack.h"

class AudioEngine : public oboe::AudioStreamDataCallback, public oboe::AudioStreamErrorCallback {
public:
    static AudioEngine* getInstance();

    bool init();
    void release();

    int loadTrack(const std::string& trackId, const std::string& filePath);
    int loadTrackFromFd(const std::string& trackId, int fd, int64_t offset, int64_t length);
    void unloadTrack(const std::string& trackId);
    bool isTrackLoaded(const std::string& trackId);

    void playTrack(const std::string& trackId);
    void pauseTrack(const std::string& trackId);
    void stopTrack(const std::string& trackId);
    void stopAllTracks();
    
    void setTrackFadeDuration(const std::string& trackId, float durationSeconds);
    bool isTrackFadingOut(const std::string& trackId);
    void cancelTrackFadeOut(const std::string& trackId);

    void setTrackVolume(const std::string& trackId, float volume);
    float getTrackVolume(const std::string& trackId);

    void setTrackLooping(const std::string& trackId, bool looping);
    bool isTrackLooping(const std::string& trackId);

    void setTrackEffectEnabled(const std::string& trackId, bool enabled);
    void setTrackReverbParams(const std::string& trackId, float roomSize, float damping, float wetLevel, float dryLevel);
    void setTrackDecayTime(const std::string& trackId, float decayTime);
    void setTrackPreDelay(const std::string& trackId, float preDelay);
    void setTrackDryLevel(const std::string& trackId, float dryLevel);
    void setTrackInsulation(const std::string& trackId, float insulation);
    void setTrackReflectionDensity(const std::string& trackId, float density);
    void setTrackReflectionSpread(const std::string& trackId, float spread);
    void setTrackHighpassCutoff(const std::string& trackId, float cutoff);
    void setTrackEarlyReflectionLevel(const std::string& trackId, float level);
    void setTrackCreativeEffectIntensity(const std::string& trackId, int effectType, float intensity);
    
    void setTrackEqBandGain(const std::string& trackId, int bandIndex, float gain);
    float getTrackEqBandGain(const std::string& trackId, int bandIndex);
    void setTrackEqEnabled(const std::string& trackId, bool enabled);
    void setTrackEqLimiterEnabled(const std::string& trackId, bool enabled);
    void setTrackEqGains(const std::string& trackId, const std::array<float, EQ_BAND_COUNT>& gains);
    std::array<float, EQ_BAND_COUNT> getTrackEqGains(const std::string& trackId);

    void setTrackSpatialEnabled(const std::string& trackId, bool enabled);
    void setTrackSpatialIntensity(const std::string& trackId, float intensity);
    void setTrackSpatialOffsetType(const std::string& trackId, int type);
    void setTrackSpatialFixedOffset(const std::string& trackId, float leftRight, float upDown, float frontBack, float multiplier);
    void setTrackSpatialSurroundParams(const std::string& trackId, int mode, float radius, float speed);
    void setTrackSpatialRandomParams(const std::string& trackId, float maxDistance, float minDistance, float randomValue, float speed);
    
    void setTrackEffectOrder(const std::string& trackId, const std::vector<int>& order);

    void seekTrack(const std::string& trackId, int64_t positionMs);
    int64_t getTrackPosition(const std::string& trackId);
    int64_t getTrackDuration(const std::string& trackId);

    bool isTrackPlaying(const std::string& trackId);

    int32_t getSampleRate() const { return sampleRate_; }
    int32_t getChannelCount() const { return channelCount_; }

    bool needsRestart() const { return needsRestart_.load(); }
    void clearRestartFlag() { needsRestart_.store(false); }
    void clearAllEffectBuffers();

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData, int32_t numFrames) override;
    void onErrorBeforeClose(oboe::AudioStream* stream, oboe::Result error) override;
    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

private:
    AudioEngine();
    ~AudioEngine();

    AudioEngine(const AudioEngine&) = delete;
    AudioEngine& operator=(const AudioEngine&) = delete;

    bool openStream();
    void closeStream();

    static AudioEngine* instance_;
    static std::mutex instanceMutex_;

    mutable std::shared_mutex tracksMutex_;
    std::unordered_map<std::string, std::unique_ptr<AudioTrack>> tracks_;

    std::shared_ptr<oboe::AudioStream> audioStream_;
    std::atomic<bool> isInitialized_{false};
    std::atomic<bool> needsRestart_{false};

    int32_t sampleRate_{44100};
    int32_t channelCount_{2};

    std::vector<float> mixBuffer_;
};

#endif
