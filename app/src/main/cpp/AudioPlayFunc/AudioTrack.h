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
#include "../AudioEffect/LimiterEffect.h"
#include "../AudioEffect/AutoEqEngine.h"
#include "../AudioEffect/SoundTouchProcessor.h"

class SpatialAudioProcessor;

using audiofx::ControlPoint;
using audiofx::EqFilterType;

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
    float stereoWidenerIntensity = 0.5f;
    float virtualBassIntensity = 0.3f;
    float multibandCompressorIntensity = 0.5f;
    
    std::vector<ControlPoint> eqCurve;
    bool eqEnabled = false;
    bool eqLimiterEnabled = true;
    
    bool limitEffectsEnabled = true;
    bool limitReverbEnabled = true;
    bool limitSpatialEnabled = true;
    
    std::vector<int> effectOrder = {0, 1, 2, 3};
};

class AudioTrack {
public:
    AudioTrack(const std::string& trackId, int32_t sampleRate, int32_t channelCount);
    ~AudioTrack();

    bool load(const std::string& filePath);
    bool loadFromFd(int fd, int64_t offset = 0, int64_t length = -1, const std::string& filePath = "");
    bool loadFromStream(ffmpeg::StreamContext* streamCtx);
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

    /** 设置播放速率（0.5~2.0），保持音调不变（time-stretch）。 */
    void setPlaybackSpeed(float speed);
    /** 设置音调偏移（半音，-12~+12），保持速率不变（pitch-shift）。 */
    void setPitchShift(float semitones);
    
    void setEqualizerCurve(const std::vector<ControlPoint>& points);
    const std::vector<ControlPoint>& getEqualizerCurve() const { return config_.eqCurve; }
    float getFilterResponse(float frequency) const;
    void setEqEnabled(bool enabled);
    bool isEqEnabled() const { return config_.eqEnabled; }
    void setEqLimiterEnabled(bool enabled);
    bool isEqLimiterEnabled() const { return config_.eqLimiterEnabled; }
    
    // Deprecated: retained for backward compatibility
    void setEqBandGain(int bandIndex, float gain);
    float getEqBandGain(int bandIndex) const;

    void setSpatialEnabled(bool enabled);
    void setSpatialIntensity(float intensity);
    void setSpatialOffsetType(int type);
    void setSpatialFixedOffset(float leftRight, float upDown, float frontBack, float multiplier);
    // periodSeconds 语义为"秒/圈"（数值越大转得越慢）
    void setSpatialSurroundParams(int mode, float radius, float periodSeconds);
    void setSpatialRandomParams(float maxDistance, float minDistance, float randomValue, float speed);
    void setSpatialScatterParams(
        float minRadius, float maxRadius,
        bool xEnabled, bool yEnabled, bool zEnabled,
        bool moveEnabled, float moveRandomValue, float moveSpeed, float directionRandom
    );
    
    void setEffectOrder(const std::vector<int>& order);
    
    void setLimitEffectsEnabled(bool enabled);
    void setLimitReverbEnabled(bool enabled);
    void setLimitSpatialEnabled(bool enabled);
    
    // AutoEQ
    void setAutoEqEnabled(bool enabled);
    bool isAutoEqEnabled() const;
    void setAutoEqTargetCurve(const std::string& type);
    void setAutoEqIntensity(float intensity);
    void setAutoEqBassBias(float bias);
    void setAutoEqMidBias(float bias);
    void setAutoEqTrebleBias(float bias);
    void setAutoEqResponseSpeed(const std::string& speed);
    void setAutoEqMaxBoost(float db);
    void setAutoEqMaxCut(float db);
    void setAutoEqBrightnessTarget(float db);
    void setAutoEqLoudnessTarget(float db);
    void setAutoEqDynamicQEnabled(bool enabled);
    void setAutoEqAttack(float ms);
    void setAutoEqRelease(float ms);
    void setAutoEqMaxSlope(float slope);
    void setAutoEqCouplingCoeff(float coeff);
    void setAutoEqHysteresis(float db);
    void setAutoEqBandCount(int count);
    void setAutoEqBandRatios(float low, float mid);
    void setSpeakerPreset(const std::string& preset);
    std::vector<float> getAutoEqGains() const;
    std::vector<float> getAutoEqFrequencies() const;
    // Per-filter overrides (user-editable gain / frequency / Q)
    void setAutoEqFilterOverride(int bandIndex, float gainDb, float freqHz, float q);
    void clearAutoEqFilterOverride(int bandIndex);
    void clearAllAutoEqFilterOverrides();
    audiofx::AudioEffectManager* getEffectManager() { return effectManager_.get(); }

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
    audiofx::AutoEqEngine autoEqEngine_;
    
    audiofx::Limiter effectsLimiter_;
    audiofx::Limiter reverbLimiter_;
    audiofx::Limiter spatialLimiter_;

    std::vector<float> decodeBuffer_;
    std::vector<float> processBuffer_;
    std::vector<float> resampleBuffer_;
    std::vector<float> timeStretchBuffer_;

    // SoundTouch (LGPL v2.1)：实时 time-stretch + pitch-shift，替代旧线性插值重采样
    audiofx::SoundTouchProcessor soundTouch_;
    std::vector<float> soundTouchOutBuffer_;
    bool soundTouchEngaged_ = false;  // 当前是否走 soundtouch 管线
    // SoundTouch 预热标志：reset（首次启用/seek/loop）后内部管道为空，首段 putSamples 无输出，
    // 需一次性喂入较大预读量填充 WSOLA 管道；之后稳态按 deficit 按需喂入，避免缓冲膨胀。
    bool soundTouchNeedsPriming_ = false;

    std::atomic<PlaybackState> state_{PlaybackState::Stopped};
    std::atomic<bool> isLoaded_{false};
    bool isStreamMode_{false};

    TrackConfig config_;
    mutable std::shared_mutex mutex_;

    // 实时可调参数（atomic）：setPlaybackSpeed/setPitchShift 不再获取 mutex_，
    // 避免与音频回调 process() 持有的 shared_lock 互斥阻塞，从而消除
    // 滑块拖动时的 UI 卡顿与参数延迟生效问题。
    // config_.speedIntensity / config_.pitchIntensity 仅作初始值容器，热路径只读 atomic。
    std::atomic<float> speedIntensityAtomic_{1.0f};
    std::atomic<float> pitchIntensityAtomic_{0.0f};

    // SoundTouch 参数缓存：仅当 atomic 值变化时才调用 setTempo/setPitchSemiTones，
    // 避免每帧重复设置（setTempo 内部会触发序列重排，高频调用导致卡顿）。
    float lastAppliedSpeed_{1.0f};
    float lastAppliedPitch_{0.0f};

    int64_t currentPositionMs_{0};
    int64_t durationMs_{0};
    // 累计已播放内容帧数：用于精确跟踪播放进度。
    // 基于 SoundTouch retrieve 的输出帧数 × speed 计算（进度增量 = got × speed），
    // 而非"解码帧数"。旧实现用 decodedFrames_ 累计解码帧数，但变调时 WSOLA
    // 预热阶段 decode > retrieve（多轮循环填满 WSOLA 管道），导致累计值
    // 远超实际播放内容 → 变调时进度条速度变快。
    // 新方案只累计 retrieve 出来的帧数（乘以 speed 还原为输入内容帧数），
    // 消除预热阶段和多轮循环导致的进度漂移。
    int64_t playedFrames_{0};

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
