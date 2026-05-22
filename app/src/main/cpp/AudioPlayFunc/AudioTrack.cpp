#include "AudioTrack.h"
#include "spatial_audio/SpatialAudioProcessor.h"
#include <algorithm>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "AudioTrack"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

AudioTrack::AudioTrack(const std::string& trackId, int32_t sampleRate, int32_t channelCount)
    : trackId_(trackId)
    , sampleRate_(sampleRate > 0 ? sampleRate : 44100)
    , channelCount_(channelCount > 0 ? channelCount : 2) {
    decoder_ = std::make_unique<ffmpeg::FFmpegDecoder>();
    reverb_ = std::make_unique<reverb::ReverbEffect>();
    reverb_->init(sampleRate_);
    effectManager_ = std::make_unique<audiofx::AudioEffectManager>();
    effectManager_->init(sampleRate_);
    spatialProcessor_ = std::make_unique<SpatialAudioProcessor>();
    spatialProcessor_->init(sampleRate_, 256);
    decodeBuffer_.resize(4096 * channelCount_);
    processBuffer_.resize(4096 * channelCount_);
}

AudioTrack::~AudioTrack() {
    unload();
}

bool AudioTrack::load(const std::string& filePath) {
    std::unique_lock<std::shared_mutex> lock(mutex_);

    if (isLoaded_) {
        unload();
    }

    if (!decoder_->open(filePath)) {
        LOGE("Failed to open file: %s", filePath.c_str());
        return false;
    }

    auto srcInfo = decoder_->getAudioInfo();
    LOGI("Source audio: rate=%d, channels=%d", srcInfo.sampleRate, srcInfo.channels);

    decoder_->setOutputFormat(sampleRate_, channelCount_);

    auto info = decoder_->getAudioInfo();
    durationMs_ = info.duration / 1000;

    isLoaded_ = true;
    currentPositionMs_ = 0;
    state_ = PlaybackState::Stopped;

    LOGI("Track loaded: %s, duration=%lldms, output rate=%d, output channels=%d",
         trackId_.c_str(), (long long)durationMs_, sampleRate_, channelCount_);
    return true;
}

bool AudioTrack::loadFromFd(int fd, int64_t offset, int64_t length) {
    std::unique_lock<std::shared_mutex> lock(mutex_);

    if (isLoaded_) {
        unload();
    }

    if (!decoder_->openFromFd(fd, offset, length)) {
        LOGE("Failed to open fd: %d", fd);
        return false;
    }

    auto srcInfo = decoder_->getAudioInfo();
    LOGI("Source audio from fd: rate=%d, channels=%d", srcInfo.sampleRate, srcInfo.channels);

    decoder_->setOutputFormat(sampleRate_, channelCount_);

    auto info = decoder_->getAudioInfo();
    durationMs_ = info.duration / 1000;

    isLoaded_ = true;
    currentPositionMs_ = 0;
    state_ = PlaybackState::Stopped;

    LOGI("Track loaded from fd: %s, duration=%lldms, output rate=%d, output channels=%d",
         trackId_.c_str(), (long long)durationMs_, sampleRate_, channelCount_);
    return true;
}

void AudioTrack::unload() {
    std::unique_lock<std::shared_mutex> lock(mutex_);

    if (!isLoaded_) return;

    state_ = PlaybackState::Stopped;
    decoder_->close();
    isLoaded_ = false;
    currentPositionMs_ = 0;

    LOGI("Track unloaded: %s", trackId_.c_str());
}

void AudioTrack::play() {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    if (!isLoaded_) return;
    
    FadeState currentFadeState = fadeState_.load();
    if (currentFadeState == FadeState::FadingOut) {
        float currentProgress = fadeProgress_.load();
        fadeProgress_.store(1.0f - currentProgress);
        fadeState_.store(FadeState::FadingIn);
    } else {
        fadeProgress_.store(0.0f);
        fadeVolume_.store(0.0f);
        fadeState_.store(FadeState::FadingIn);
    }
    
    state_ = PlaybackState::Playing;
    LOGI("Track playing with fade-in: %s", trackId_.c_str());
}

void AudioTrack::pause() {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    if (!isLoaded_) return;
    
    FadeState currentFadeState = fadeState_.load();
    if (currentFadeState == FadeState::FadingIn) {
        float currentProgress = fadeProgress_.load();
        fadeProgress_.store(1.0f - currentProgress);
    } else {
        fadeProgress_.store(0.0f);
    }
    
    fadeState_.store(FadeState::FadingOut);
    state_ = PlaybackState::Playing;
    LOGI("Track starting fade-out: %s", trackId_.c_str());
}

void AudioTrack::cancelFadeOut() {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    if (fadeState_.load() == FadeState::FadingOut) {
        float currentProgress = fadeProgress_.load();
        fadeProgress_.store(1.0f - currentProgress);
        fadeState_.store(FadeState::FadingIn);
        state_ = PlaybackState::Playing;
        LOGI("Fade-out cancelled, resuming: %s", trackId_.c_str());
    }
}

void AudioTrack::setFadeDuration(float durationSeconds) {
    fadeDuration_.store(std::clamp(durationSeconds, 0.1f, 5.0f));
}

void AudioTrack::stop() {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    if (!isLoaded_) return;
    state_ = PlaybackState::Stopped;
    decoder_->seekTo(0);
    currentPositionMs_ = 0;
    reverb_->clear();
    resamplePosition_ = 0.0;
    timeStretchPosition_ = 0.0;
    wsolaOverlapSamples_ = 0;
    std::fill(prevSamples_, prevSamples_ + 4, 0.0f);
    LOGI("Track stopped: %s", trackId_.c_str());
}

void AudioTrack::seekTo(int64_t positionMs) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    if (!isLoaded_) return;
    decoder_->seekTo(positionMs);
    currentPositionMs_ = positionMs;
    reverb_->clear();
    resamplePosition_ = 0.0;
    timeStretchPosition_ = 0.0;
    wsolaOverlapSamples_ = 0;
    std::fill(prevSamples_, prevSamples_ + 4, 0.0f);
}

void AudioTrack::setVolume(float volume) {
    config_.volume = std::clamp(volume, 0.0f, 3.0f);
}

void AudioTrack::setLooping(bool looping) {
    config_.looping = looping;
}

void AudioTrack::setEffectEnabled(bool enabled) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.effectEnabled = enabled;
    if (!enabled) {
        reverb_->clear();
    }
}

void AudioTrack::setRoomSize(float value) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.roomSize = value;
    reverb_->setRoomSize(value);
}

void AudioTrack::setDecayTime(float value) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.decayTime = value;
    reverb_->setDecayTime(value);
}

void AudioTrack::setDamping(float value) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.damping = value;
    reverb_->setDamping(value);
}

void AudioTrack::setWetLevel(float value) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.wetLevel = value;
    reverb_->setWetLevel(value);
}

void AudioTrack::setDryLevel(float value) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.dryLevel = value;
    reverb_->setDryLevel(value);
}

void AudioTrack::setInsulation(float value) {
    config_.insulation = std::clamp(value, 0.0f, 1.0f);
}

void AudioTrack::setPreDelay(float value) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.preDelay = value;
    reverb_->setPreDelay(value);
}

void AudioTrack::setReflectionDensity(float density) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.reflectionDensity = density;
    reverb_->setReflectionDensity(density);
}

void AudioTrack::setReflectionSpread(float spread) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.reflectionSpread = spread;
    reverb_->setReflectionSpread(spread);
}

void AudioTrack::setHighpassCutoff(float cutoff) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.highpassCutoff = cutoff;
    reverb_->setHighpassCutoff(cutoff);
}

void AudioTrack::setEarlyReflectionLevel(float level) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.earlyReflectionLevel = level;
    reverb_->setEarlyReflectionLevel(level);
}

int64_t AudioTrack::getDuration() const {
    return durationMs_;
}

int64_t AudioTrack::getPosition() const {
    return currentPositionMs_;
}

void AudioTrack::applyInsulation(float* samples, int32_t numFrames, int32_t channels) {
    if (config_.insulation <= 0.001f) return;

    float mappedInsulation = std::pow(config_.insulation, 0.5f);
    float cutoff = 1.0f - mappedInsulation;
    cutoff = std::clamp(cutoff, 0.01f, 1.0f);

    float rc = 1.0f / (2.0f * M_PI * cutoff * sampleRate_ / 2.0f);
    float dt = 1.0f / sampleRate_;
    float alpha = dt / (rc + dt);

    for (int32_t i = 0; i < numFrames; ++i) {
        for (int32_t c = 0; c < channels; ++c) {
            float input = samples[i * channels + c];
            float output = alpha * input + (1.0f - alpha) * insulationState1_;
            insulationState1_ = output;
            output = alpha * output + (1.0f - alpha) * insulationState2_;
            insulationState2_ = output;
            samples[i * channels + c] = output;
        }
    }
}

void AudioTrack::process(float* output, int32_t numFrames) {
    int32_t totalSamples = numFrames * channelCount_;
    std::fill(output, output + totalSamples, 0.0f);

    FadeState currentFadeState = fadeState_.load();
    
    if (!isLoaded_ || (state_ != PlaybackState::Playing && currentFadeState != FadeState::FadingOut)) {
        return;
    }
    
    updateFade(numFrames);
    currentFadeState = fadeState_.load();

    std::shared_lock<std::shared_mutex> lock(mutex_);

    float pitchRatio = std::pow(2.0f, config_.pitchIntensity / 12.0f);
    float speedRatio = config_.speedIntensity;
    
    bool needsPitchShift = std::abs(pitchRatio - 1.0f) > 0.01f;
    bool needsSpeedChange = std::abs(speedRatio - 1.0f) > 0.01f;
    
    float resampleRatio = 1.0f;
    float timeStretchRatio = 1.0f;
    
    if (needsPitchShift && needsSpeedChange) {
        resampleRatio = pitchRatio;
        timeStretchRatio = speedRatio / pitchRatio;
    } else if (needsPitchShift) {
        resampleRatio = pitchRatio;
        timeStretchRatio = 1.0f / pitchRatio;
    } else if (needsSpeedChange) {
        resampleRatio = 1.0f;
        timeStretchRatio = speedRatio;
    }
    
    bool needsResample = std::abs(resampleRatio - 1.0f) > 0.01f;
    bool needsTimeStretch = std::abs(timeStretchRatio - 1.0f) > 0.01f;

    int32_t samplesWritten = 0;

    while (samplesWritten < totalSamples) {
        int32_t remainingOutputFrames = (totalSamples - samplesWritten) / channelCount_;
        
        int32_t framesToDecode = remainingOutputFrames;
        if (needsTimeStretch) {
            framesToDecode = static_cast<int32_t>(framesToDecode * timeStretchRatio) + 256;
        }
        if (needsResample) {
            framesToDecode = static_cast<int32_t>(framesToDecode * resampleRatio) + 64;
        }
        
        decodeBuffer_.clear();
        bool success = decoder_->decodeChunk(decodeBuffer_, framesToDecode);

        if (!success || decodeBuffer_.empty()) {
            if (config_.looping) {
                decoder_->seekTo(0);
                currentPositionMs_ = 0;
                reverb_->clear();
                resamplePosition_ = 0.0;
                timeStretchPosition_ = 0.0;
                wsolaOverlapSamples_ = 0;
                std::fill(prevSamples_, prevSamples_ + 4, 0.0f);
                continue;
            } else {
                if (state_ == PlaybackState::Playing) {
                    state_ = PlaybackState::Stopped;
                    LOGI("Track finished: %s", trackId_.c_str());
                }
                break;
            }
        }

        int32_t chunkSamples = decodeBuffer_.size();
        int32_t chunkFrames = chunkSamples / channelCount_;

        if (needsResample) {
            resampleBuffer_.clear();
            resampleBuffer_.reserve(static_cast<size_t>(chunkSamples / resampleRatio) + 16);
            
            while (resamplePosition_ < static_cast<double>(chunkFrames - 1)) {
                int srcFrame = static_cast<int>(resamplePosition_);
                double frac = resamplePosition_ - srcFrame;
                
                for (int c = 0; c < channelCount_; c++) {
                    int idx0 = srcFrame * channelCount_ + c;
                    int idx1 = std::min((srcFrame + 1) * channelCount_ + c, chunkSamples - 1);
                    
                    float s0 = decodeBuffer_[idx0];
                    float s1 = decodeBuffer_[idx1];
                    
                    float sample = s0 * (1.0f - static_cast<float>(frac)) + s1 * static_cast<float>(frac);
                    resampleBuffer_.push_back(sample);
                }
                
                resamplePosition_ += resampleRatio;
            }
            
            resamplePosition_ -= chunkFrames;
            
            decodeBuffer_ = std::move(resampleBuffer_);
            chunkSamples = decodeBuffer_.size();
            chunkFrames = chunkSamples / channelCount_;
        }

        if (needsTimeStretch) {
            timeStretchBuffer_.clear();
            int outputFrames = static_cast<int>(chunkFrames / timeStretchRatio) + 16;
            timeStretchBuffer_.reserve(outputFrames * channelCount_);
            
            double readPos = timeStretchPosition_;
            
            while (readPos < static_cast<double>(chunkFrames - 1)) {
                int srcFrame = static_cast<int>(readPos);
                double frac = readPos - srcFrame;
                
                for (int c = 0; c < channelCount_; c++) {
                    int idx0 = srcFrame * channelCount_ + c;
                    int idx1 = std::min((srcFrame + 1) * channelCount_ + c, chunkSamples - 1);
                    
                    float s0 = decodeBuffer_[idx0];
                    float s1 = decodeBuffer_[idx1];
                    
                    float sample = s0 * (1.0f - static_cast<float>(frac)) + s1 * static_cast<float>(frac);
                    timeStretchBuffer_.push_back(sample);
                }
                
                readPos += timeStretchRatio;
            }
            
            timeStretchPosition_ = readPos - chunkFrames;
            
            decodeBuffer_ = std::move(timeStretchBuffer_);
            chunkSamples = decodeBuffer_.size();
            chunkFrames = chunkSamples / channelCount_;
        }

        float currentFadeVolume = fadeVolume_.load();
        for (int32_t i = 0; i < chunkSamples; ++i) {
            decodeBuffer_[i] *= config_.volume * currentFadeVolume;
        }

        if (config_.insulation > 0.001f) {
            applyInsulation(decodeBuffer_.data(), chunkFrames, channelCount_);
        }

        for (int stage : config_.effectOrder) {
            switch (stage) {
                case 0:
                    if (spatialProcessor_ && spatialProcessor_->isEnabled()) {
                        spatialProcessor_->process(decodeBuffer_.data(), decodeBuffer_.data(), chunkFrames);
                        
                        float azimuth, elevation, distance;
                        spatialProcessor_->getCurrentPosition(azimuth, elevation, distance);
                        reverb_->setSourcePosition(azimuth, elevation, distance);
                        reverb_->setSpatialReflectionEnabled(true);
                    } else {
                        reverb_->setSpatialReflectionEnabled(false);
                    }
                    break;
                case 1:
                    if (config_.effectEnabled) {
                        reverb_->processInterleaved(decodeBuffer_.data(), chunkFrames, channelCount_);
                    }
                    break;
                case 2:
                    if (config_.eqEnabled) {
                        auto* eq = effectManager_->getEqualizer();
                        if (eq) {
                            eq->process(decodeBuffer_.data(), chunkFrames, channelCount_);
                        }
                    }
                    break;
                case 3:
                    if (config_.loFiIntensity > 0.001f || config_.eightBitIntensity > 0.001f ||
                        config_.underwaterIntensity > 0.001f || config_.alienSignalIntensity > 0.001f ||
                        config_.megaphoneIntensity > 0.001f ||
                        config_.hifiIntensity > 0.001f ||
                        config_.distortionIntensity > 0.001f ||
                        config_.noiseIntensity > 0.001f) {
                        for (auto& effect : effectManager_->getCreativeEffects()) {
                            if (effect->isEnabled()) {
                                effect->process(decodeBuffer_.data(), chunkFrames, channelCount_);
                            }
                        }
                        for (auto& effect : effectManager_->getQualityEffects()) {
                            if (effect->isEnabled()) {
                                effect->process(decodeBuffer_.data(), chunkFrames, channelCount_);
                            }
                        }
                    }
                    break;
            }
        }

        for (int32_t i = 0; i < chunkSamples; ++i) {
            float sample = decodeBuffer_[i];
            if (std::isnan(sample) || std::isinf(sample)) {
                decodeBuffer_[i] = 0.0f;
            } else if (sample > 2.0f || sample < -2.0f) {
                decodeBuffer_[i] = std::clamp(sample, -1.0f, 1.0f);
            }
        }

        int32_t samplesToCopy = std::min(chunkSamples, totalSamples - samplesWritten);
        std::copy(decodeBuffer_.begin(), decodeBuffer_.begin() + samplesToCopy, output + samplesWritten);
        samplesWritten += samplesToCopy;
    }

    currentPositionMs_ += static_cast<int64_t>(numFrames * 1000.0 / sampleRate_ * config_.speedIntensity);
}

void AudioTrack::setCreativeEffectIntensity(audiofx::EffectType type, float intensity) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    
    switch (type) {
        case audiofx::EffectType::LoFi:
            config_.loFiIntensity = intensity;
            break;
        case audiofx::EffectType::EightBit:
            config_.eightBitIntensity = intensity;
            break;
        case audiofx::EffectType::Underwater:
            config_.underwaterIntensity = intensity;
            break;
        case audiofx::EffectType::AlienSignal:
            config_.alienSignalIntensity = intensity;
            break;
        case audiofx::EffectType::Megaphone:
            config_.megaphoneIntensity = intensity;
            break;
        case audiofx::EffectType::Pitch:
            config_.pitchIntensity = intensity;
            break;
        case audiofx::EffectType::Speed:
            config_.speedIntensity = intensity;
            break;
        case audiofx::EffectType::HiFi:
            config_.hifiIntensity = intensity;
            break;
        case audiofx::EffectType::Distortion:
            config_.distortionIntensity = intensity;
            break;
        case audiofx::EffectType::Noise:
            config_.noiseIntensity = intensity;
            break;
        default:
            break;
    }
    
    updateCreativeEffects();
}

void AudioTrack::updateCreativeEffects() {
    if (!effectManager_) return;
    
    auto* loFi = effectManager_->getEffect(audiofx::EffectType::LoFi);
    if (loFi) {
        loFi->setEnabled(config_.loFiIntensity > 0.001f);
        loFi->setParameter(0, config_.loFiIntensity);
    }
    
    auto* eightBit = effectManager_->getEffect(audiofx::EffectType::EightBit);
    if (eightBit) {
        eightBit->setEnabled(config_.eightBitIntensity > 0.001f);
        eightBit->setParameter(0, config_.eightBitIntensity);
    }
    
    auto* underwater = effectManager_->getEffect(audiofx::EffectType::Underwater);
    if (underwater) {
        underwater->setEnabled(config_.underwaterIntensity > 0.001f);
        underwater->setParameter(0, config_.underwaterIntensity);
    }
    
    auto* alienSignal = effectManager_->getEffect(audiofx::EffectType::AlienSignal);
    if (alienSignal) {
        alienSignal->setEnabled(config_.alienSignalIntensity > 0.001f);
        alienSignal->setParameter(0, config_.alienSignalIntensity);
    }
    
    auto* megaphone = effectManager_->getEffect(audiofx::EffectType::Megaphone);
    if (megaphone) {
        megaphone->setEnabled(config_.megaphoneIntensity > 0.001f);
        megaphone->setParameter(0, config_.megaphoneIntensity);
    }
    
    auto* hifi = effectManager_->getEffect(audiofx::EffectType::HiFi);
    if (hifi) {
        hifi->setEnabled(config_.hifiIntensity > 0.001f);
        hifi->setParameter(0, config_.hifiIntensity);
    }
    
    auto* distortion = effectManager_->getEffect(audiofx::EffectType::Distortion);
    if (distortion) {
        distortion->setEnabled(config_.distortionIntensity > 0.001f);
        distortion->setParameter(0, config_.distortionIntensity);
    }
    
    auto* noise = effectManager_->getEffect(audiofx::EffectType::Noise);
    if (noise) {
        noise->setEnabled(config_.noiseIntensity > 0.001f);
        noise->setParameter(0, config_.noiseIntensity);
    }
}

void AudioTrack::setEffectOrder(const std::vector<int>& order) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.effectOrder = order;
}

void AudioTrack::setEqBandGain(int bandIndex, float gain) {
    if (bandIndex < 0 || bandIndex >= EQ_BAND_COUNT) return;
    
    config_.eqGains[bandIndex] = gain;
    
    auto* eq = effectManager_->getEqualizer();
    if (eq) {
        eq->setParameter(bandIndex, gain);
    }
}

float AudioTrack::getEqBandGain(int bandIndex) const {
    if (bandIndex < 0 || bandIndex >= EQ_BAND_COUNT) return 0.0f;
    return config_.eqGains[bandIndex];
}

void AudioTrack::setEqEnabled(bool enabled) {
    config_.eqEnabled = enabled;
    
    auto* eq = effectManager_->getEqualizer();
    if (eq) {
        eq->setEnabled(enabled);
    }
}

void AudioTrack::setEqLimiterEnabled(bool enabled) {
    config_.eqLimiterEnabled = enabled;
    effectManager_->setLimiterEnabled(enabled);
}

void AudioTrack::setEqGains(const std::array<float, EQ_BAND_COUNT>& gains) {
    config_.eqGains = gains;
    
    auto* eq = effectManager_->getEqualizer();
    if (eq) {
        eq->setGains(gains);
    }
}

void AudioTrack::setSpatialEnabled(bool enabled) {
    if (spatialProcessor_) {
        spatialProcessor_->setEnabled(enabled);
    }
}

void AudioTrack::setSpatialIntensity(float intensity) {
    if (spatialProcessor_) {
        spatialProcessor_->setIntensity(intensity);
    }
}

void AudioTrack::setSpatialOffsetType(int type) {
    if (spatialProcessor_) {
        spatialProcessor_->setOffsetType(type);
    }
}

void AudioTrack::setSpatialFixedOffset(float leftRight, float upDown, float frontBack, float multiplier) {
    if (spatialProcessor_) {
        spatialProcessor_->setFixedOffset(leftRight, upDown, frontBack, multiplier);
    }
}

void AudioTrack::setSpatialSurroundParams(int mode, float radius, float speed) {
    if (spatialProcessor_) {
        spatialProcessor_->setSurroundParams(mode, radius, speed);
    }
}

void AudioTrack::setSpatialRandomParams(float maxDistance, float minDistance, float randomValue, float speed) {
    if (spatialProcessor_) {
        spatialProcessor_->setRandomParams(maxDistance, minDistance, randomValue, speed);
    }
}

void AudioTrack::updateFade(int32_t numFrames) {
    FadeState currentFadeState = fadeState_.load();
    if (currentFadeState == FadeState::None) {
        fadeVolume_.store(1.0f);
        return;
    }
    
    float duration = fadeDuration_.load();
    float fadeTime = duration * sampleRate_;
    float progress = fadeProgress_.load();
    float progressIncrement = static_cast<float>(numFrames) / fadeTime;
    
    progress += progressIncrement;
    
    if (progress >= 1.0f) {
        progress = 1.0f;
        if (currentFadeState == FadeState::FadingOut) {
            fadeState_.store(FadeState::None);
            fadeVolume_.store(0.0f);
            state_ = PlaybackState::Paused;
            LOGI("Fade-out complete, track paused: %s", trackId_.c_str());
        } else {
            fadeState_.store(FadeState::None);
            fadeVolume_.store(1.0f);
            LOGI("Fade-in complete: %s", trackId_.c_str());
        }
    } else {
        fadeProgress_.store(progress);
        float volume;
        if (currentFadeState == FadeState::FadingIn) {
            volume = progress;
        } else {
            volume = 1.0f - progress;
        }
        fadeVolume_.store(volume);
    }
}

void AudioTrack::applyFade(float* samples, int32_t numFrames) {
    float volume = fadeVolume_.load();
    int32_t totalSamples = numFrames * channelCount_;
    for (int32_t i = 0; i < totalSamples; ++i) {
        samples[i] *= volume;
    }
}

void AudioTrack::clearEffectBuffers() {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    
    if (reverb_) {
        reverb_->clear();
    }
    
    if (spatialProcessor_) {
        spatialProcessor_->clearBuffers();
    }
    
    insulationState1_ = 0.0f;
    insulationState2_ = 0.0f;
    
    std::fill(prevSamples_, prevSamples_ + 4, 0.0f);
    wsolaOverlapSamples_ = 0;
    resamplePosition_ = 0.0;
    timeStretchPosition_ = 0.0;
    
    LOGI("Track effect buffers cleared: %s", trackId_.c_str());
}
