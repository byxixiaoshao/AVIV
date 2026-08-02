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
#include "../AudioEffect/LimiterEffect.h"

class AudioEngine : public oboe::AudioStreamDataCallback, public oboe::AudioStreamErrorCallback {
public:
    static AudioEngine* getInstance();

    bool init();
    void release();

    // P2-7: 轻量重建音频流（保留已加载的 tracks），用于 onErrorAfterClose 后的优雅恢复
    // 返回 true 表示新流已成功打开并启动；false 表示需要调用方走全量 release+init 路径
    bool recreateStream();

    int loadTrack(const std::string& trackId, const std::string& filePath);
    int loadTrackFromFd(const std::string& trackId, int fd, int64_t offset, int64_t length, const std::string& filePath = "");
    int loadTrackFromStream(const std::string& trackId, const std::string& streamId);
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

    // SoundTouch：独立调节播放速率与音调（替代旧版线性重采样 + WSOLA）
    void setTrackPlaybackSpeed(const std::string& trackId, float speed);
    void setTrackPitchShift(const std::string& trackId, float semitones);
    
    void setTrackEqualizerCurve(const std::string& trackId, const std::vector<ControlPoint>& points);
    float getTrackFilterResponse(const std::string& trackId, float frequency);
    void setTrackEqBandGain(const std::string& trackId, int bandIndex, float gain);
    float getTrackEqBandGain(const std::string& trackId, int bandIndex);
    void setTrackEqEnabled(const std::string& trackId, bool enabled);
    void setTrackEqLimiterEnabled(const std::string& trackId, bool enabled);

    void setTrackSpatialEnabled(const std::string& trackId, bool enabled);
    void setTrackSpatialIntensity(const std::string& trackId, float intensity);
    void setTrackSpatialOffsetType(const std::string& trackId, int type);
    void setTrackSpatialFixedOffset(const std::string& trackId, float leftRight, float upDown, float frontBack, float multiplier);
    // periodSeconds 语义为"秒/圈"（数值越大转得越慢）
    void setTrackSpatialSurroundParams(const std::string& trackId, int mode, float radius, float periodSeconds);
    void setTrackSpatialRandomParams(const std::string& trackId, float maxDistance, float minDistance, float randomValue, float speed);
    void setTrackSpatialScatterParams(
        const std::string& trackId,
        float minRadius, float maxRadius,
        bool xEnabled, bool yEnabled, bool zEnabled,
        bool moveEnabled, float moveRandomValue, float moveSpeed, float directionRandom
    );
    
    void setTrackEffectOrder(const std::string& trackId, const std::vector<int>& order);
    
    // AutoEQ
    void setTrackAutoEqEnabled(const std::string& trackId, bool enabled);
    bool isTrackAutoEqEnabled(const std::string& trackId);
    void setTrackAutoEqTargetCurve(const std::string& trackId, const std::string& type);
    void setTrackAutoEqIntensity(const std::string& trackId, float intensity);
    void setTrackAutoEqBassBias(const std::string& trackId, float bias);
    void setTrackAutoEqMidBias(const std::string& trackId, float bias);
    void setTrackAutoEqTrebleBias(const std::string& trackId, float bias);
    void setTrackAutoEqResponseSpeed(const std::string& trackId, const std::string& speed);
    void setTrackAutoEqMaxBoost(const std::string& trackId, float db);
    void setTrackAutoEqMaxCut(const std::string& trackId, float db);
    void setTrackAutoEqBrightnessTarget(const std::string& trackId, float db);
    void setTrackAutoEqLoudnessTarget(const std::string& trackId, float db);
    void setTrackAutoEqDynamicQEnabled(const std::string& trackId, bool enabled);
    void setTrackAutoEqBandCount(const std::string& trackId, int count);
    void setTrackAutoEqBandRatios(const std::string& trackId, float low, float mid);
    std::vector<float> getTrackAutoEqGains(const std::string& trackId);
    std::vector<float> getTrackAutoEqFrequencies(const std::string& trackId);
    // Per-filter overrides (user-editable gain / frequency / Q)
    void setTrackAutoEqFilterOverride(const std::string& trackId, int bandIndex, float gainDb, float freqHz, float q);
    void clearTrackAutoEqFilterOverride(const std::string& trackId, int bandIndex);
    void clearAllTrackAutoEqFilterOverrides(const std::string& trackId);
    
    void setGlobalLimiterConfig(const audiofx::LimiterConfig& config);
    audiofx::LimiterConfig getGlobalLimiterConfig() const;
    void setGlobalLimiterEnabled(bool enabled);
    bool isGlobalLimiterEnabled() const;

    void seekTrack(const std::string& trackId, int64_t positionMs);
    int64_t getTrackPosition(const std::string& trackId);
    int64_t getTrackDuration(const std::string& trackId);

    bool isTrackPlaying(const std::string& trackId);
    
    AudioTrack* getTrack(const std::string& trackId);

    int32_t getSampleRate() const { return sampleRate_; }
    int32_t getChannelCount() const { return channelCount_; }
    
    std::array<float, 16> getVisualizationData() const;
    std::array<float, 16> getWhiteNoiseVisualizationData() const;
    std::array<float, 16> getMusicVisualizationData() const;
    float getVisualizationEnergy() const;
    float getWhiteNoiseVisualizationEnergy() const;
    float getMusicVisualizationEnergy() const;

    bool needsRestart() const { return needsRestart_.load(); }
    void clearRestartFlag() { needsRestart_.store(false); }
    
    int32_t getXRunCount() const { return xrunCount_.load(); }
    bool hasUnderrun() const { return needsUnderrunReport_.load(); }
    void clearUnderrunFlag() { needsUnderrunReport_.store(false); }
    
    void clearAllEffectBuffers();
    
    // 流式解码管理
    bool createStream(const std::string& streamId, size_t bufferSize = 0);
    bool writeStreamData(const std::string& streamId, const uint8_t* data, size_t len, size_t& written);
    void setStreamComplete(const std::string& streamId);
    void destroyStream(const std::string& streamId);
    bool hasStream(const std::string& streamId) const;

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

    mutable std::mutex streamsMutex_;
    std::unordered_map<std::string, std::unique_ptr<ffmpeg::StreamContext>> streams_;

    std::shared_ptr<oboe::AudioStream> audioStream_;
    // P2-7: 保护 audioStream_ 的重建操作，防止 onErrorAfterClose 与上层 recreateStream 并发竞争
    mutable std::mutex streamMutex_;
    std::atomic<bool> isInitialized_{false};
    std::atomic<bool> needsRestart_{false};
    std::atomic<int32_t> xrunCount_{0};
    std::atomic<int32_t> lastCheckedXrunCount_{0};
    std::atomic<bool> needsUnderrunReport_{false};
    std::atomic<bool> audioThreadStalled_{false};
    int32_t callbackCounter_{0};

    int32_t sampleRate_{44100};
    int32_t channelCount_{2};

    std::vector<float> mixBuffer_;
    // 预分配可视化混音缓冲：避免在 onAudioReady 实时回调中每次堆分配 std::vector，
    // 堆分配会引发音频线程抖动与 XRun（卡顿成因之一）。
    std::vector<float> whiteNoiseBuffer_;
    std::vector<float> musicBuffer_;
    
    audiofx::GlobalLimiter globalLimiter_;
    
    mutable std::mutex vizMutex_;
    std::array<float, 16> vizData_{};
    float vizEnergy_{0.0f};
    
    std::array<float, 16> whiteNoiseVizData_{};
    float whiteNoiseVizEnergy_{0.0f};
    
    std::array<float, 16> musicVizData_{};
    float musicVizEnergy_{0.0f};
};

#endif
