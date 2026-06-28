#include "AudioEngine.h"
#include <algorithm>
#include <cmath>
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

    globalLimiter_.init(sampleRate_);
    LOGI("Global limiter initialized with sample rate %d", sampleRate_);

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

int AudioEngine::loadTrackFromFd(const std::string& trackId, int fd, int64_t offset, int64_t length, const std::string& filePath) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);

    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->unload();
    } else {
        auto track = std::make_unique<AudioTrack>(trackId, sampleRate_, channelCount_);
        tracks_[trackId] = std::move(track);
    }

    if (!tracks_[trackId]->loadFromFd(fd, offset, length, filePath)) {
        tracks_.erase(trackId);
        LOGE("Failed to load track from fd: %s, path: %s", trackId.c_str(), filePath.c_str());
        return -1;
    }

    LOGI("Track loaded from fd: %s, engine rate=%d, channels=%d, path: %s", trackId.c_str(), sampleRate_, channelCount_, filePath.c_str());
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

// --- AutoEQ ---

void AudioEngine::setTrackAutoEqEnabled(const std::string& trackId, bool enabled) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqEnabled(enabled);
    }
}

bool AudioEngine::isTrackAutoEqEnabled(const std::string& trackId) {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        return it->second->isAutoEqEnabled();
    }
    return false;
}

void AudioEngine::setTrackAutoEqTargetCurve(const std::string& trackId, const std::string& type) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqTargetCurve(type);
    }
}

void AudioEngine::setTrackAutoEqIntensity(const std::string& trackId, float intensity) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqIntensity(intensity);
    }
}

void AudioEngine::setTrackAutoEqBassBias(const std::string& trackId, float bias) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqBassBias(bias);
    }
}

void AudioEngine::setTrackAutoEqMidBias(const std::string& trackId, float bias) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqMidBias(bias);
    }
}

void AudioEngine::setTrackAutoEqTrebleBias(const std::string& trackId, float bias) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqTrebleBias(bias);
    }
}

void AudioEngine::setTrackAutoEqResponseSpeed(const std::string& trackId, const std::string& speed) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqResponseSpeed(speed);
    }
}

void AudioEngine::setTrackAutoEqMaxBoost(const std::string& trackId, float db) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqMaxBoost(db);
    }
}

void AudioEngine::setTrackAutoEqMaxCut(const std::string& trackId, float db) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqMaxCut(db);
    }
}

void AudioEngine::setTrackAutoEqSmoothing(const std::string& trackId, float s) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqSmoothing(s);
    }
}

void AudioEngine::setTrackAutoEqBrightnessTarget(const std::string& trackId, float db) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqBrightnessTarget(db);
    }
}

void AudioEngine::setTrackAutoEqLoudnessTarget(const std::string& trackId, float db) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqLoudnessTarget(db);
    }
}

void AudioEngine::setTrackAutoEqDynamicQEnabled(const std::string& trackId, bool enabled) {
    std::unique_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        it->second->setAutoEqDynamicQEnabled(enabled);
    }
}

std::array<float, audiofx::AUTO_EQ_BANDS> AudioEngine::getTrackAutoEqGains(const std::string& trackId) {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        return it->second->getAutoEqGains();
    }
    return {};
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

AudioTrack* AudioEngine::getTrack(const std::string& trackId) {
    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    auto it = tracks_.find(trackId);
    if (it != tracks_.end()) {
        return it->second.get();
    }
    return nullptr;
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream* stream, void* audioData, int32_t numFrames) {
    float* output = static_cast<float*>(audioData);
    int32_t totalSamples = numFrames * channelCount_;

    std::fill(output, output + totalSamples, 0.0f);

    std::shared_lock<std::shared_mutex> lock(tracksMutex_);
    
    std::vector<float> whiteNoiseBuffer(totalSamples, 0.0f);
    std::vector<float> musicBuffer(totalSamples, 0.0f);

    for (auto& pair : tracks_) {
        auto& track = pair.second;
        const std::string& trackId = pair.first;
        
        if (track->isPlaying()) {
            track->process(mixBuffer_.data(), numFrames);

            bool isMusic = trackId.find("music_") == 0;
            
            for (int32_t i = 0; i < totalSamples; ++i) {
                output[i] += mixBuffer_[i];
                
                if (isMusic) {
                    musicBuffer[i] += mixBuffer_[i];
                } else {
                    whiteNoiseBuffer[i] += mixBuffer_[i];
                }
            }
        }
    }

    globalLimiter_.process(output, numFrames, channelCount_);

    for (int32_t i = 0; i < totalSamples; ++i) {
        output[i] = std::clamp(output[i], -1.0f, 1.0f);
    }
    
    {
        std::lock_guard<std::mutex> vizLock(vizMutex_);
        constexpr int bandCount = 16;
        int samplesPerBand = totalSamples / bandCount;
        
        if (samplesPerBand > 0) {
            float totalEnergy = 0.0f;
            float whiteNoiseEnergy = 0.0f;
            float musicEnergy = 0.0f;
            
            for (int b = 0; b < bandCount; ++b) {
                float sum = 0.0f;
                float wnSum = 0.0f;
                float musicSum = 0.0f;
                int start = b * samplesPerBand;
                int end = std::min(start + samplesPerBand, totalSamples);
                
                for (int i = start; i < end; ++i) {
                    sum += std::abs(output[i]);
                    wnSum += std::abs(whiteNoiseBuffer[i]);
                    musicSum += std::abs(musicBuffer[i]);
                }
                
                // Calculate values and ensure they are valid (not NaN, not negative)
                float vizValue = sum / samplesPerBand;
                float wnValue = wnSum / samplesPerBand;
                float musicValue = musicSum / samplesPerBand;
                
                // Filter out NaN and invalid values, clamp to valid range
                vizData_[b] = (std::isnan(vizValue) || vizValue < 0.0f) ? 0.0f : std::clamp(vizValue, 0.0f, 1.0f);
                whiteNoiseVizData_[b] = (std::isnan(wnValue) || wnValue < 0.0f) ? 0.0f : std::clamp(wnValue, 0.0f, 1.0f);
                musicVizData_[b] = (std::isnan(musicValue) || musicValue < 0.0f) ? 0.0f : std::clamp(musicValue, 0.0f, 1.0f);
                
                totalEnergy += vizData_[b];
                whiteNoiseEnergy += whiteNoiseVizData_[b];
                musicEnergy += musicVizData_[b];
            }
            
            // Ensure energy values are valid
            vizEnergy_ = (std::isnan(totalEnergy / bandCount) || totalEnergy < 0.0f) ? 0.0f : std::clamp(totalEnergy / bandCount, 0.0f, 1.0f);
            whiteNoiseVizEnergy_ = (std::isnan(whiteNoiseEnergy / bandCount) || whiteNoiseEnergy < 0.0f) ? 0.0f : std::clamp(whiteNoiseEnergy / bandCount, 0.0f, 1.0f);
            musicVizEnergy_ = (std::isnan(musicEnergy / bandCount) || musicEnergy < 0.0f) ? 0.0f : std::clamp(musicEnergy / bandCount, 0.0f, 1.0f);
        }
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

std::array<float, 16> AudioEngine::getVisualizationData() const {
    std::lock_guard<std::mutex> lock(vizMutex_);
    return vizData_;
}

std::array<float, 16> AudioEngine::getWhiteNoiseVisualizationData() const {
    std::lock_guard<std::mutex> lock(vizMutex_);
    return whiteNoiseVizData_;
}

std::array<float, 16> AudioEngine::getMusicVisualizationData() const {
    std::lock_guard<std::mutex> lock(vizMutex_);
    return musicVizData_;
}

float AudioEngine::getVisualizationEnergy() const {
    std::lock_guard<std::mutex> lock(vizMutex_);
    return vizEnergy_;
}

float AudioEngine::getWhiteNoiseVisualizationEnergy() const {
    std::lock_guard<std::mutex> lock(vizMutex_);
    return whiteNoiseVizEnergy_;
}

float AudioEngine::getMusicVisualizationEnergy() const {
    std::lock_guard<std::mutex> lock(vizMutex_);
    return musicVizEnergy_;
}

void AudioEngine::setGlobalLimiterConfig(const audiofx::LimiterConfig& config) {
    globalLimiter_.setConfig(config);
    LOGI("Global limiter config updated: enabled=%d, limitEq=%d, limitEffects=%d, limitReverb=%d, limitSpatial=%d",
         config.enabled, config.limitEqualizer, config.limitEffects, config.limitReverb, config.limitSpatial);
}

audiofx::LimiterConfig AudioEngine::getGlobalLimiterConfig() const {
    return globalLimiter_.getConfig();
}

void AudioEngine::setGlobalLimiterEnabled(bool enabled) {
    globalLimiter_.setEnabled(enabled);
    LOGI("Global limiter enabled: %d", enabled);
}

bool AudioEngine::isGlobalLimiterEnabled() const {
    return globalLimiter_.isEnabled();
}
