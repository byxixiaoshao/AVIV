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

    autoEqEngine_.init(sampleRate_);

    effectsLimiter_.init(sampleRate_);
    reverbLimiter_.init(sampleRate_);
    spatialLimiter_.init(sampleRate_);

    // SoundTouch：实时 time-stretch + pitch-shift，延迟初始化至首次 process
    soundTouch_.init(sampleRate_, channelCount_);

    decodeBuffer_.resize(4096 * channelCount_);
    processBuffer_.resize(4096 * channelCount_);

    // 同步 TrackConfig 默认值到效果器，确保效果器在 UI 首次调参前就能按默认强度工作。
    // 不调用此函数时，效果器使用各自的构造函数默认值（如 VirtualBass intensity_=0 → 无效果）。
    updateCreativeEffects();
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
    playedFrames_ = 0;
    state_ = PlaybackState::Stopped;

    LOGI("Track loaded: %s, duration=%lldms, output rate=%d, output channels=%d",
         trackId_.c_str(), (long long)durationMs_, sampleRate_, channelCount_);
    return true;
}

bool AudioTrack::loadFromFd(int fd, int64_t offset, int64_t length, const std::string& filePath) {
    std::unique_lock<std::shared_mutex> lock(mutex_);

    if (isLoaded_) {
        unload();
    }

    if (!decoder_->openFromFd(fd, offset, length)) {
        LOGE("Failed to open fd: %d, path: %s", fd, filePath.c_str());
        return false;
    }

    auto srcInfo = decoder_->getAudioInfo();
    LOGI("Source audio from fd: rate=%d, channels=%d, path: %s", srcInfo.sampleRate, srcInfo.channels, filePath.c_str());

    decoder_->setOutputFormat(sampleRate_, channelCount_);

    auto info = decoder_->getAudioInfo();
    durationMs_ = info.duration / 1000;

    isLoaded_ = true;
    currentPositionMs_ = 0;
    playedFrames_ = 0;
    state_ = PlaybackState::Stopped;

    LOGI("Track loaded from fd: %s, duration=%lldms, output rate=%d, output channels=%d, path: %s",
         trackId_.c_str(), (long long)durationMs_, sampleRate_, channelCount_, filePath.c_str());
    return true;
}

bool AudioTrack::loadFromStream(ffmpeg::StreamContext* streamCtx) {
    std::unique_lock<std::shared_mutex> lock(mutex_);

    if (isLoaded_) {
        unload();
    }

    if (!decoder_->openFromStream(streamCtx)) {
        LOGE("Failed to open stream for track: %s", trackId_.c_str());
        return false;
    }

    auto srcInfo = decoder_->getAudioInfo();
    LOGI("Source audio from stream: rate=%d, channels=%d", srcInfo.sampleRate, srcInfo.channels);

    decoder_->setOutputFormat(sampleRate_, channelCount_);

    auto info = decoder_->getAudioInfo();
    durationMs_ = info.duration / 1000;

    isLoaded_ = true;
    isStreamMode_ = true;
    currentPositionMs_ = 0;
    playedFrames_ = 0;
    state_ = PlaybackState::Stopped;

    LOGI("Track loaded from stream: %s, duration=%lldms, output rate=%d, output channels=%d",
         trackId_.c_str(), (long long)durationMs_, sampleRate_, channelCount_);
    return true;
}

void AudioTrack::unload() {
    std::unique_lock<std::shared_mutex> lock(mutex_);

    if (!isLoaded_) return;

    state_ = PlaybackState::Stopped;
    decoder_->close();
    isLoaded_ = false;
    isStreamMode_ = false;
    currentPositionMs_ = 0;
    playedFrames_ = 0;
    // 重置 SoundTouch 状态：unload 后可能 load 新 track，若 soundTouchEngaged_ 残留为 true，
    // 新 track 首次 process 不会进入 !soundTouchEngaged_ 分支 reset+prime，
    // 导致 SoundTouch 内部残留旧 track 数据与新 track 混合（音频伪影）。
    soundTouch_.reset();
    soundTouchEngaged_ = false;
    soundTouchNeedsPriming_ = false;

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
    playedFrames_ = 0;
    reverb_->clear();
    resamplePosition_ = 0.0;
    timeStretchPosition_ = 0.0;
    wsolaOverlapSamples_ = 0;
    std::fill(prevSamples_, prevSamples_ + 4, 0.0f);
    soundTouch_.reset();
    soundTouchEngaged_ = false;
    LOGI("Track stopped: %s", trackId_.c_str());
}

void AudioTrack::seekTo(int64_t positionMs) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    if (!isLoaded_) return;
    decoder_->seekTo(positionMs);
    currentPositionMs_ = positionMs;
    playedFrames_ = positionMs * sampleRate_ / 1000;
    reverb_->clear();
    resamplePosition_ = 0.0;
    timeStretchPosition_ = 0.0;
    wsolaOverlapSamples_ = 0;
    std::fill(prevSamples_, prevSamples_ + 4, 0.0f);
    soundTouch_.reset();
    // seek 后 SoundTouch 内部管道清空，需重新预热 WSOLA，否则首段 retrieve=0 输出静音
    soundTouchNeedsPriming_ = true;
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

    // SoundTouch：pitch（半音）与 speed（倍率）独立调节，作用于效果器之后。
    // 旧版线性重采样 + WSOLA 已被 SoundTouch 取代，pitch/speed 独立可调。
    // 读取 atomic 而非 config_，避免与 setPlaybackSpeed/setPitchShift 的互斥阻塞。
    float speedRatio = speedIntensityAtomic_.load(std::memory_order_relaxed);
    // SoundTouch setTempo(speed)：1.0=原速，2.0=2倍速（加速，output=input/speed），0.5=半速。
    // setPitchSemiTones 直接接收半音数（+12=高八度）。
    // timeRatio = input/output 比：speed=2 加速时需 2 倍输入帧产出 1 倍输出帧。
    // 旧值 1/speed 方向反了，导致 framesToDecode 估算偏小（靠固定+512预读掩盖，但引发
    // SoundTouch 缓冲持续膨胀→进度超前/参数延迟生效/XRun增长）。
    double timeRatio = static_cast<double>(speedRatio);  // speed=2 → 输入是输出的2倍

    bool needsSoundTouch = (std::abs(speedRatio - 1.0f) > 0.01f)
                        || (std::abs(pitchIntensityAtomic_.load(std::memory_order_relaxed)) > 0.01f);

    // 同步 SoundTouch 参数（与 process 同线程，实时安全）
    // 仅当值变化时才调用 setTempo/setPitchSemiTones，避免每帧重复设置导致卡顿。
    if (needsSoundTouch) {
        if (!soundTouchEngaged_) {
            soundTouch_.reset();  // 从旁路切入时清空旧缓冲
            soundTouchEngaged_ = true;
            lastAppliedSpeed_ = -1.0f;   // 强制下次更新
            lastAppliedPitch_ = 999.0f;  // 强制下次更新
            soundTouchNeedsPriming_ = true;  // 标记需预热 WSOLA 管道
        }
        float currentPitch = pitchIntensityAtomic_.load(std::memory_order_relaxed);
        if (std::abs(speedRatio - lastAppliedSpeed_) > 0.001f) {
            soundTouch_.setTempo(speedRatio);
            // 大幅变化（点击跳转）时清空输出缓冲，让新参数立即生效；
            // 小幅变化（滑动拖动）时不清空，避免频繁断音。
            // SoundTouch 内部缓冲了约 50ms 旧参数数据，若不清空会先播放旧数据
            // → 点击调整后效果延迟约 50ms 才生效（"没能及时变化"问题）。
            // 阈值 0.3：滑动每帧变化通常 <0.05，点击跳转通常 >0.3
            if (std::abs(speedRatio - lastAppliedSpeed_) > 0.3f) {
                soundTouch_.reset();  // clear 内部缓冲（保留 tempo/pitch 设置）
                soundTouchNeedsPriming_ = true;
            }
            lastAppliedSpeed_ = speedRatio;
        }
        if (std::abs(currentPitch - lastAppliedPitch_) > 0.01f) {
            soundTouch_.setPitchSemiTones(currentPitch);
            // 大幅变化（点击跳转）时清空输出缓冲，让新参数立即生效（同 speed 逻辑）。
            // 阈值 0.8 半音：滑动每帧变化通常 <0.3，点击跳转通常 >0.8
            if (std::abs(currentPitch - lastAppliedPitch_) > 0.8f) {
                soundTouch_.reset();  // clear 内部缓冲（保留 tempo/pitch 设置）
                soundTouchNeedsPriming_ = true;
            }
            lastAppliedPitch_ = currentPitch;
        }
    } else {
        soundTouchEngaged_ = false;
    }

    // SoundTouch 旁路时直接写入 output；启用时先入 soundTouchOutBuffer_ 再 retrieve。
    // 注意：先 assign 再取 data()，避免首次分配时指针失效。
    if (needsSoundTouch) {
        soundTouchOutBuffer_.assign(static_cast<size_t>(totalSamples), 0.0f);
    }
    float* writeTarget = needsSoundTouch ? soundTouchOutBuffer_.data() : output;

    int32_t samplesWritten = 0;

    while (samplesWritten < totalSamples) {
        int32_t remainingOutputFrames = (totalSamples - samplesWritten) / channelCount_;

        // 按需喂入：input = output * timeRatio（speed=2 → 2倍输入）。
        // 旧固定+512预读每轮累积导致 SoundTouch 缓冲膨胀（进度超前/参数延迟/XRun），
        // 改为仅在首次启用/seek/loop 后一次性预热 1024 帧填充 WSOLA 管道；
        // 稳态每轮 decode=input 消耗量，产出≈retrieve，缓冲净变化为0，进度精确。
        int32_t framesToDecode = needsSoundTouch
            ? static_cast<int32_t>(remainingOutputFrames * timeRatio)
            : remainingOutputFrames;
        if (needsSoundTouch && soundTouchNeedsPriming_) {
            framesToDecode += 1024;  // 一次性预热 WSOLA 管道（约 23ms）
            soundTouchNeedsPriming_ = false;
        }
        if (framesToDecode < 1) framesToDecode = 1;

        decodeBuffer_.clear();
        bool success = decoder_->decodeChunk(decodeBuffer_, framesToDecode);

        if (!success || decodeBuffer_.empty()) {
            if (isStreamMode_ && decoder_->isStreamActive()) {
                // 流式解码：数据还未下载完，输出静音等待更多数据
                break;
            }
            if (config_.looping) {
                decoder_->seekTo(0);
                currentPositionMs_ = 0;
                playedFrames_ = 0;
                reverb_->clear();
                if (soundTouchEngaged_) {
                    soundTouch_.reset();
                    // loop 回到开头后 SoundTouch 内部管道清空，需重新预热 WSOLA
                    soundTouchNeedsPriming_ = true;
                }
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

        // 进度不再基于"解码帧数"累计，改为基于 SoundTouch retrieve 的输出帧数 × speed。
        // 旧实现在此累计 chunkFrames，但变调时 WSOLA 预热阶段 decode > retrieve，
        // 多轮循环导致 playedFrames_ 累计远超实际播放内容 → 变调时进度条速度变快。
        // 新方案在下方 retrieve 后累计，准确反映"播放到哪里"。

        // 仅应用音量，淡入淡出延迟到 process 末尾对最终 output 生效，
        // 避免 SoundTouch 缓冲延迟导致 fade 失效（旧代码在 decode 循环内乘 fadeVolume，
        // 被 SoundTouch 内部缓冲延迟数百毫秒，fade 效果被掩盖）
        for (int32_t i = 0; i < chunkSamples; ++i) {
            decodeBuffer_[i] *= config_.volume;
        }

        if (config_.insulation > 0.001f) {
            applyInsulation(decodeBuffer_.data(), chunkFrames, channelCount_);
        }

        for (int stage : config_.effectOrder) {
            switch (stage) {
                case 0:
                    if (spatialProcessor_ && spatialProcessor_->isEnabled()) {
                        spatialProcessor_->process(decodeBuffer_.data(), decodeBuffer_.data(), chunkFrames);

                        if (config_.limitSpatialEnabled) {
                            spatialLimiter_.process(decodeBuffer_.data(), chunkFrames, channelCount_);
                        }

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

                        if (config_.limitReverbEnabled) {
                            reverbLimiter_.process(decodeBuffer_.data(), chunkFrames, channelCount_);
                        }
                    }
                    break;
                case 2:
                    // AutoEQ: per-frame gain envelope smoothing
                    if (autoEqEngine_.isEnabled()) {
                        autoEqEngine_.process(decodeBuffer_.data(), chunkFrames, channelCount_);
                    }
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
                        config_.noiseIntensity > 0.001f ||
                        config_.stereoWidenerIntensity > 0.001f ||
                        config_.virtualBassIntensity > 0.001f ||
                        config_.multibandCompressorIntensity > 0.001f) {
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

                        if (config_.limitEffectsEnabled) {
                            effectsLimiter_.process(decodeBuffer_.data(), chunkFrames, channelCount_);
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

        if (needsSoundTouch) {
            // 喂入 SoundTouch，再 retrieve 出 time/pitch 处理后的数据
            soundTouch_.process(decodeBuffer_.data(), chunkFrames);

            int32_t remainingSamples = totalSamples - samplesWritten;
            int32_t maxOutFrames = remainingSamples / channelCount_;
            int32_t available = soundTouch_.available();
            if (available > 0) {
                int32_t toRetrieve = std::min(available, maxOutFrames);
                int32_t got = soundTouch_.retrieve(writeTarget + samplesWritten, toRetrieve);
                samplesWritten += got * channelCount_;
                // 进度增量 = retrieve 输出帧数 × speed。
                // 输出 got 帧代表"播放了 got 帧"，但实际消耗了 got×speed 帧的输入内容：
                //   加速(speed=2)：1 输出帧 = 2 输入帧内容 → 进度 += got×2
                //   减速(speed=0.5)：1 输出帧 = 0.5 输入帧内容 → 进度 += got×0.5
                //   变调(speed=1)：1 输出帧 = 1 输入帧内容 → 进度 += got×1
                // 此公式在所有 speed/pitch 组合下都精确，且不受 WSOLA 预热延迟影响。
                playedFrames_ += static_cast<int64_t>(got * speedRatio);
            }
            // available()=0 时跳过本帧：SoundTouch 正在预热，下轮循环再取
        } else {
            int32_t samplesToCopy = std::min(chunkSamples, totalSamples - samplesWritten);
            std::copy(decodeBuffer_.begin(), decodeBuffer_.begin() + samplesToCopy,
                      writeTarget + samplesWritten);
            samplesWritten += samplesToCopy;
            // 旁路路径：播放内容 = 拷贝帧数（speed=1, pitch=0，无变换）
            playedFrames_ += samplesToCopy / channelCount_;
        }
    }

    // SoundTouch 启用但末尾仍有缓冲数据：尽量取出填满输出，避免尾部欠载
    if (needsSoundTouch) {
        while (samplesWritten < totalSamples && soundTouch_.available() > 0) {
            int32_t remainingSamples = totalSamples - samplesWritten;
            int32_t maxOutFrames = remainingSamples / channelCount_;
            int32_t toRetrieve = std::min(soundTouch_.available(), maxOutFrames);
            int32_t got = soundTouch_.retrieve(soundTouchOutBuffer_.data() + samplesWritten, toRetrieve);
            if (got <= 0) break;
            samplesWritten += got * channelCount_;
            // 尾部 retrieve 同样计入进度（与主循环一致的 got×speed 公式）
            playedFrames_ += static_cast<int64_t>(got * speedRatio);
        }
        // SoundTouch 仍在预热/欠载时静音填充剩余部分(output 已被 std::fill 置零)
        std::copy(soundTouchOutBuffer_.begin(), soundTouchOutBuffer_.begin() + samplesWritten, output);
    }

    // 淡入淡出直接作用于最终输出，确保 fade 不受 SoundTouch 缓冲延迟影响。
    // fadeVolume_ 在 process 开头由 updateFade() 更新一次，整个调用期间保持不变，
    // 因此对最终 output 一次性乘 fadeVolume_ 等价于逐 chunk 应用，但不受 SoundTouch 延迟掩盖。
    float fadeVol = fadeVolume_.load();
    if (fadeVol < 0.999f) {
        int32_t total = numFrames * channelCount_;
        for (int32_t i = 0; i < total; ++i) {
            output[i] *= fadeVol;
        }
    }

    // 进度基于 retrieve 输出帧数 × speed 的累计（playedFrames_），
    // 而非"解码帧数"或"输出帧数 × tempo"估算。
    // 旧实现在 decode 后累计 chunkFrames，但变调时 WSOLA 预热阶段 decode > retrieve，
    // 多轮循环导致进度累计远超实际播放内容 → 变调时进度条速度变快。
    // 新方案只在 retrieve 后累计 got×speed，准确反映"播放到哪里"，
    // 消除预热阶段和多轮循环导致的进度漂移。
    currentPositionMs_ = playedFrames_ * 1000 / sampleRate_;
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
            pitchIntensityAtomic_.store(intensity, std::memory_order_relaxed);
            break;
        case audiofx::EffectType::Speed:
            config_.speedIntensity = intensity;
            speedIntensityAtomic_.store(intensity, std::memory_order_relaxed);
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
        case audiofx::EffectType::StereoWidener:
            config_.stereoWidenerIntensity = intensity;
            break;
        case audiofx::EffectType::VirtualBass:
            config_.virtualBassIntensity = intensity;
            break;
        case audiofx::EffectType::MultibandCompressor:
            config_.multibandCompressorIntensity = intensity;
            break;
        default:
            break;
    }
    
    updateCreativeEffects();
}

void AudioTrack::setPlaybackSpeed(float speed) {
    // 无锁 atomic 写入：避免与音频回调 process() 的 shared_lock 互斥阻塞，
    // 消除滑块拖动时的 UI 卡顿与参数延迟生效。
    // speed 单位为倍率：1.0=原速，0.5=半速，2.0=倍速
    speedIntensityAtomic_.store(std::clamp(speed, 0.25f, 4.0f), std::memory_order_relaxed);
}

void AudioTrack::setPitchShift(float semitones) {
    // 无锁 atomic 写入：同 setPlaybackSpeed。
    // pitch 单位为半音：0=原调，+12=高八度，-12=低八度
    pitchIntensityAtomic_.store(std::clamp(semitones, -24.0f, 24.0f), std::memory_order_relaxed);
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
    
    auto* stereoWidener = effectManager_->getEffect(audiofx::EffectType::StereoWidener);
    if (stereoWidener) {
        stereoWidener->setEnabled(config_.stereoWidenerIntensity > 0.001f);
        stereoWidener->setParameter(0, config_.stereoWidenerIntensity);
    }
    
    auto* virtualBass = effectManager_->getEffect(audiofx::EffectType::VirtualBass);
    if (virtualBass) {
        virtualBass->setEnabled(config_.virtualBassIntensity > 0.001f);
        virtualBass->setParameter(0, config_.virtualBassIntensity);
    }
    
    auto* multibandCompressor = effectManager_->getEffect(audiofx::EffectType::MultibandCompressor);
    if (multibandCompressor) {
        multibandCompressor->setEnabled(config_.multibandCompressorIntensity > 0.001f);
        multibandCompressor->setParameter(0, config_.multibandCompressorIntensity);
    }
}

void AudioTrack::setEffectOrder(const std::vector<int>& order) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.effectOrder = order;
}

void AudioTrack::setEqualizerCurve(const std::vector<ControlPoint>& points) {
    config_.eqCurve = points;
    auto* eq = effectManager_->getEqualizer();
    if (eq) {
        eq->setCurve(points);
    }
}

float AudioTrack::getFilterResponse(float frequency) const {
    auto* eq = effectManager_->getEqualizer();
    if (eq) {
        return eq->getFrequencyResponse(frequency);
    }
    return 0.0f;
}

// Deprecated: no-op for backward compatibility (fixed bands replaced by dynamic curve)
void AudioTrack::setEqBandGain(int bandIndex, float gain) {
    (void)bandIndex;
    (void)gain;
}

float AudioTrack::getEqBandGain(int bandIndex) const {
    (void)bandIndex;
    return 0.0f;
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

void AudioTrack::setSpatialSurroundParams(int mode, float radius, float periodSeconds) {
    if (spatialProcessor_) {
        spatialProcessor_->setSurroundParams(mode, radius, periodSeconds);
    }
}

void AudioTrack::setSpatialRandomParams(float maxDistance, float minDistance, float randomValue, float speed) {
    if (spatialProcessor_) {
        spatialProcessor_->setRandomParams(maxDistance, minDistance, randomValue, speed);
    }
}

void AudioTrack::setSpatialScatterParams(
    float minRadius, float maxRadius,
    bool xEnabled, bool yEnabled, bool zEnabled,
    bool moveEnabled, float moveRandomValue, float moveSpeed, float directionRandom
) {
    if (spatialProcessor_) {
        spatialProcessor_->setScatterParams(
            minRadius, maxRadius,
            xEnabled, yEnabled, zEnabled,
            moveEnabled, moveRandomValue, moveSpeed, directionRandom
        );
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
    
    effectsLimiter_.clear();
    reverbLimiter_.clear();
    spatialLimiter_.clear();
    autoEqEngine_.clear();
    soundTouch_.reset();
    soundTouchEngaged_ = false;

    LOGI("Track effect buffers cleared: %s", trackId_.c_str());
}

void AudioTrack::setLimitEffectsEnabled(bool enabled) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.limitEffectsEnabled = enabled;
}

void AudioTrack::setLimitReverbEnabled(bool enabled) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.limitReverbEnabled = enabled;
}

void AudioTrack::setLimitSpatialEnabled(bool enabled) {
    std::unique_lock<std::shared_mutex> lock(mutex_);
    config_.limitSpatialEnabled = enabled;
}

// --- AutoEQ ---

// Helper: push AutoEQ gains as ControlPoints to EqualizerEffect
// Per-filter overrides (gain / frequency / Q) are applied here so the BiQuad
// pool always reflects the user-edited curve.
static void applyAutoEqToEq(AudioTrack* track, audiofx::AutoEqEngine& engine) {
    auto* eq = track->getEffectManager()->getEqualizer();
    if (!eq) return;

    const auto& gains = engine.getCurrentGains();
    const auto& freqs = engine.getBandFrequencies();
    int bandCount = engine.getBandCount();
    if (bandCount <= 0) return;

    // 性能保护：最多推入 48 个频段点到 EQ 滤波器池
    // 超出部分均匀抽取，防止级联 biquad 过多导致 CPU 过载爆音
    constexpr int kMaxEqFilterPoints = 48;
    int stride = 1;
    int actualCount = bandCount;
    if (bandCount > kMaxEqFilterPoints) {
        stride = bandCount / kMaxEqFilterPoints;
        actualCount = bandCount / stride;
    }

    std::vector<audiofx::ControlPoint> pts;
    pts.reserve(actualCount);
    for (int b = 0; b < bandCount; b += stride) {
        audiofx::ControlPoint pt;
        pt.filterType = audiofx::EqFilterType::Peaking;

        // Apply user override if present, otherwise use auto-computed values
        if (const auto* o = engine.getFilterOverride(b)) {
            pt.frequencyHz = o->frequencyHz;
            pt.gainDb      = o->gainDb;
            pt.Q           = o->q;
        } else {
            pt.frequencyHz = freqs[b];
            pt.gainDb      = gains[b];
            pt.Q = engine.isDynamicQEnabled()
                    ? audiofx::AutoEqEngine::computeDynamicQFactor(gains[b])
                    : 1.0f;
        }
        pts.push_back(pt);
    }
    eq->setCurve(pts);
}

void AudioTrack::setAutoEqEnabled(bool enabled) {
    autoEqEngine_.setEnabled(enabled);
}

bool AudioTrack::isAutoEqEnabled() const {
    return autoEqEngine_.isEnabled();
}

void AudioTrack::setAutoEqTargetCurve(const std::string& type) {
    // No-op: speaker preset handles target curve selection
}

void AudioTrack::setAutoEqIntensity(float intensity) {
    autoEqEngine_.setIntensity(intensity);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqBassBias(float bias) {
    autoEqEngine_.setBassBias(bias);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqMidBias(float bias) {
    autoEqEngine_.setMidBias(bias);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqTrebleBias(float bias) {
    autoEqEngine_.setTrebleBias(bias);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqResponseSpeed(const std::string& speed) {
    // No-op: Attack/Release replaces response speed concept
}

void AudioTrack::setAutoEqMaxBoost(float db) {
    autoEqEngine_.setMaxBoost(db);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqMaxCut(float db) {
    autoEqEngine_.setMaxCut(db);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqBrightnessTarget(float db) {
    autoEqEngine_.setBrightnessTarget(db);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqLoudnessTarget(float db) {
    autoEqEngine_.setLoudnessTarget(db);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqDynamicQEnabled(bool enabled) {
    autoEqEngine_.setDynamicQEnabled(enabled);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqAttack(float ms) {
    autoEqEngine_.setAttack(ms);
}

void AudioTrack::setAutoEqRelease(float ms) {
    autoEqEngine_.setRelease(ms);
}

void AudioTrack::setAutoEqMaxSlope(float slope) {
    autoEqEngine_.setMaxSlope(slope);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqCouplingCoeff(float coeff) {
    autoEqEngine_.setCouplingCoeff(coeff);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqHysteresis(float db) {
    autoEqEngine_.setHysteresis(db);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqBandCount(int count) {
    autoEqEngine_.setBandCount(count);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setAutoEqBandRatios(float low, float mid) {
    autoEqEngine_.setBandRatios(low, mid);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::setSpeakerPreset(const std::string& preset) {
    autoEqEngine_.setSpeakerPreset(preset);
    applyAutoEqToEq(this, autoEqEngine_);
}

std::vector<float> AudioTrack::getAutoEqGains() const {
    return autoEqEngine_.getCurrentGains();
}

std::vector<float> AudioTrack::getAutoEqFrequencies() const {
    return autoEqEngine_.getBandFrequencies();
}

// --- Per-filter overrides (user-editable gain / frequency / Q) ---
void AudioTrack::setAutoEqFilterOverride(int bandIndex, float gainDb, float freqHz, float q) {
    autoEqEngine_.setFilterOverride(bandIndex, gainDb, freqHz, q);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::clearAutoEqFilterOverride(int bandIndex) {
    autoEqEngine_.clearFilterOverride(bandIndex);
    applyAutoEqToEq(this, autoEqEngine_);
}

void AudioTrack::clearAllAutoEqFilterOverrides() {
    autoEqEngine_.clearAllFilterOverrides();
    applyAutoEqToEq(this, autoEqEngine_);
}
