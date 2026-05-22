#include "AudioEngine.h"
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "AudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

AudioEngine* AudioEngine::instance_ = nullptr;
std::mutex AudioEngine::instanceMutex_;

AudioEngine* AudioEngine::getInstance() {
    std::lock_guard<std::mutex> lock(instanceMutex_);
    if (instance_ == nullptr) {
        instance_ = new AudioEngine();
    }
    return instance_;
}

AudioEngine::AudioEngine() {
    mixBuffer_.resize(4096 * 2);
}

AudioEngine::~AudioEngine() {
    release();
}

bool AudioEngine::init() {
    if (isInitialized_) return true;

    if (!openStream()) {
        LOGE("Failed to open audio stream");
        return false;
    }

    isInitialized_ = true;
    LOGI("AudioEngine initialized, sampleRate=%d, channels=%d", sampleRate_, channelCount_);
    return true;
}

void AudioEngine::release() {
    if (!isInitialized_) return;

    closeStream();

    {
        std::unique_lock<std::shared_mutex> lock(tracksMutex_);
        tracks_.clear();
    }

    isInitialized_ = false;
    LOGI("AudioEngine released");
}

bool AudioEngine::openStream() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Shared);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(channelCount_);
    builder.setDataCallback(this);
    builder.setErrorCallback(this);

    oboe::Result result = builder.openStream(audioStream_);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open stream: %s", oboe::convertToText(result));
        return false;
    }

    sampleRate_ = audioStream_->getSampleRate();
    channelCount_ = audioStream_->getChannelCount();
    mixBuffer_.resize(4096 * channelCount_);

    result = audioStream_->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start stream: %s", oboe::convertToText(result));
        return false;
    }

    needsRestart_.store(false);
    LOGI("Oboe stream opened: rate=%d, channels=%d", sampleRate_, channelCount_);
    return true;
}

void AudioEngine::closeStream() {
    if (audioStream_) {
        audioStream_->requestStop();
        audioStream_->close();
        audioStream_.reset();
    }
}

int AudioEngine::loadTrack(const std::string& trackId, const std::string& filePath) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);

    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->unload();
    } else {
        auto track = std::make_unique<AudioTrack>(trackId, sampleRate_, channelCount_);
        tracks_[trackId] = std::move(track);
    }

    if (!tracks_[trackId]->load(filePath)) {
        tracks_.erase(trackId);
        LOGE("Failed to load track: %s", trackId.c_str());
        return -1;
    }

    LOGI("Track loaded: %s, engine rate=%d, channels=%d", trackId.c_str(), sampleRate_, channelCount_);
    return 0;
}

int AudioEngine::loadTrackFromFd(const std::string& trackId, int fd, int64_t offset, int64_t length) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);

    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->unload();
    } else {
        auto track = std::make_unique<AudioTrack>(trackId, sampleRate_, channelCount_);
        tracks_[trackId] = std::move(track);
    }

    if (!tracks_[trackId]->loadFromFd(fd, offset, length)) {
        tracks_.erase(trackId);
        LOGE("Failed to load track from fd: %s", trackId.c_str());
        return -1;
    }

    LOGI("Track loaded from fd: %s, engine rate=%d, channels=%d", trackId.c_str(), sampleRate_, channelCount_);
    return 0;
}

void AudioEngine::unloadTrack(const std::string& trackId) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->unload();
        tracks_.erase(it);
        LOGI("Track unloaded: %s", trackId.c_str());
    }
}

bool AudioEngine::isTrackLoaded(const std::string& trackId) {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    return it != tracks_.end() && it->second->isLoaded();
}

void AudioEngine::playTrack(const std::string& trackId) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->play();
    }
}

void AudioEngine::pauseTrack(const std::string& trackId) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->pause();
    }
}

void AudioEngine::stopTrack(const std::string& trackId) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->stop();
    }
}

void AudioEngine::setTrackFadeDuration(const std::string& trackId, float durationSeconds) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setFadeDuration(durationSeconds);
    }
}

bool AudioEngine::isTrackFadingOut(const std::string& trackId) {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        return it->second->isFadingOut();
    }
    return false;
}

void AudioEngine::cancelTrackFadeOut(const std::string& trackId) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->cancelFadeOut();
    }
}

void AudioEngine::stopAllTracks() {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    for (auto& pair : tracks_) {
        pair.second->stop();
    }
}

void AudioEngine::setTrackVolume(const std::string& trackId, float volume) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setVolume(volume);
    }
}

float AudioEngine::getTrackVolume(const std::string& trackId) {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        return it->second->getVolume();
    }
    return 0.0f;
}

void AudioEngine::setTrackLooping(const std::string& trackId, bool looping) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setLooping(looping);
    }
}

bool AudioEngine::isTrackLooping(const std::string& trackId) {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        return it->second->isLooping();
    }
    return false;
}

void AudioEngine::setTrackEffectEnabled(const std::string& trackId, bool enabled) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setEffectEnabled(enabled);
    }
}

void AudioEngine::setTrackReverbParams(const std::string& trackId, float roomSize, float damping, float wetLevel, float dryLevel) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setRoomSize(roomSize);
        it->second->setDamping(damping);
        it->second->setWetLevel(wetLevel);
        it->second->setDryLevel(dryLevel);
    }
}

void AudioEngine::setTrackDecayTime(const std::string& trackId, float decayTime) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setDecayTime(decayTime);
    }
}

void AudioEngine::setTrackPreDelay(const std::string& trackId, float preDelay) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setPreDelay(preDelay);
    }
}

void AudioEngine::setTrackDryLevel(const std::string& trackId, float dryLevel) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setDryLevel(dryLevel);
    }
}

void AudioEngine::setTrackInsulation(const std::string& trackId, float insulation) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setInsulation(insulation);
    }
}

void AudioEngine::setTrackReflectionDensity(const std::string& trackId, float density) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setReflectionDensity(density);
    }
}

void AudioEngine::setTrackReflectionSpread(const std::string& trackId, float spread) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setReflectionSpread(spread);
    }
}

void AudioEngine::setTrackHighpassCutoff(const std::string& trackId, float cutoff) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setHighpassCutoff(cutoff);
    }
}

void AudioEngine::setTrackEarlyReflectionLevel(const std::string& trackId, float level) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setEarlyReflectionLevel(level);
    }
}

void AudioEngine::setTrackCreativeEffectIntensity(const std::string& trackId, int effectType, float intensity) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setCreativeEffectIntensity(static_cast<audiofx::EffectType>(effectType), intensity);
    }
}

void AudioEngine::setTrackEqBandGain(const std::string& trackId, int bandIndex, float gain) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setEqBandGain(bandIndex, gain);
    }
}

float AudioEngine::getTrackEqBandGain(const std::string& trackId, int bandIndex) {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        return it->second->getEqBandGain(bandIndex);
    }
    return 0.0f;
}

void AudioEngine::setTrackEqEnabled(const std::string& trackId, bool enabled) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setEqEnabled(enabled);
    }
}

void AudioEngine::setTrackEqLimiterEnabled(const std::string& trackId, bool enabled) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setEqLimiterEnabled(enabled);
    }
}

void AudioEngine::setTrackEqGains(const std::string& trackId, const std::array<float, EQ_BAND_COUNT>& gains) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setEqGains(gains);
    }
}

std::array<float, EQ_BAND_COUNT> AudioEngine::getTrackEqGains(const std::string& trackId) {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        return it->second->getEqGains();
    }
    return {};
}

void AudioEngine::setTrackSpatialEnabled(const std::string& trackId, bool enabled) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setSpatialEnabled(enabled);
    }
}

void AudioEngine::setTrackSpatialIntensity(const std::string& trackId, float intensity) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setSpatialIntensity(intensity);
    }
}

void AudioEngine::setTrackSpatialOffsetType(const std::string& trackId, int type) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setSpatialOffsetType(type);
    }
}

void AudioEngine::setTrackSpatialFixedOffset(const std::string& trackId, float leftRight, float upDown, float frontBack, float multiplier) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setSpatialFixedOffset(leftRight, upDown, frontBack, multiplier);
    }
}

void AudioEngine::setTrackSpatialSurroundParams(const std::string& trackId, int mode, float radius, float speed) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setSpatialSurroundParams(mode, radius, speed);
    }
}

void AudioEngine::setTrackSpatialRandomParams(const std::string& trackId, float maxDistance, float minDistance, float randomValue, float speed) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setSpatialRandomParams(maxDistance, minDistance, randomValue, speed);
    }
}

void AudioEngine::setTrackEffectOrder(const std::string& trackId, const std::vector<int>& order) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setEffectOrder(order);
    }
}

void AudioEngine::seekTrack(const std::string& trackId, int64_t positionMs) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->seekTo(positionMs);
    }
}

int64_t AudioEngine::getTrackPosition(const std::string& trackId) {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        return it->second->getPosition();
    }
    return 0;
}

int64_t AudioEngine::getTrackDuration(const std::string& trackId) {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        return it->second->getDuration();
    }
    return 0;
}

bool AudioEngine::isTrackPlaying(const std::string& trackId) {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        return it->second->isPlaying();
    }
    return false;
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream* stream, void* audioData, int32_t numFrames) {
    float* output = static_cast<float*>(audioData);
    int32_t totalSamples = numFrames * channelCount_;

    std::fill(output, output + totalSamples, 0.0f);

    std::shared_lock<std::shared_mutex> lock(tracksMutex_);

    for (auto& pair : tracks_) {
        auto& track = pair.second;
        if (track->isPlaying()) {
            track->process(mixBuffer_.data(), numFrames);

            for (int32_t i = 0; i < totalSamples; ++i) {
                output[i] += mixBuffer_[i];
            }
        }
    }

    for (int32_t i = 0; i < totalSamples; ++i) {
        output[i] = std::clamp(output[i], -1.0f, 1.0f);
    }

    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorBeforeClose(oboe::AudioStream* stream, oboe::Result error) {
    LOGE("Audio stream error before close: %s", oboe::convertToText(error));
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) {
    LOGE("Audio stream error after close: %s", oboe::convertToText(error));
    
    if (error == oboe::Result::ErrorDisconnected) {
        LOGW("Audio stream disconnected, needs restart");
        needsRestart_.store(true);
    }
}

void AudioEngine::clearAllEffectBuffers() {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    
    for (auto& pair : tracks_) {
        pair.second->clearEffectBuffers();
    }
    
    LOGI("All track effect buffers cleared");
}
