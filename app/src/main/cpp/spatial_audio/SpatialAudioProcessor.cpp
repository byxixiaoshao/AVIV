#include "SpatialAudioProcessor.h"
#include <algorithm>
#include <random>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "SpatialAudioProcessor", __VA_ARGS__)

// 高质量随机数生成器（替代 rand()）
namespace {
    std::mt19937& rng() {
        static thread_local std::mt19937 gen(std::random_device{}());
        return gen;
    }
    inline float randf() {
        static thread_local std::uniform_real_distribution<float> dist(0.0f, 1.0f);
        return dist(rng());
    }
}

SpatialAudioProcessor::SpatialAudioProcessor() {
    leftDelayBuffer_.resize(MAX_ITD_DELAY_SAMPLES, 0.0f);
    rightDelayBuffer_.resize(MAX_ITD_DELAY_SAMPLES, 0.0f);
}

SpatialAudioProcessor::~SpatialAudioProcessor() {
    reset();
}

bool SpatialAudioProcessor::init(int sampleRate, int framesPerBuffer) {
    std::lock_guard<std::mutex> lock(initMutex_);
    
    sampleRate_.store(sampleRate);
    framesPerBuffer_.store(framesPerBuffer);
    
    leftDelayBuffer_.resize(MAX_ITD_DELAY_SAMPLES, 0.0f);
    rightDelayBuffer_.resize(MAX_ITD_DELAY_SAMPLES, 0.0f);
    delayWriteIndex_ = 0;
    
    initialized_.store(true);
    LOGI("Spatial Audio Processor initialized: sampleRate=%d, framesPerBuffer=%d", sampleRate, framesPerBuffer);
    return true;
}

void SpatialAudioProcessor::reset() {
    std::lock_guard<std::mutex> lock(initMutex_);
    initialized_.store(false);
    
    leftDelayBuffer_.clear();
    rightDelayBuffer_.clear();
    delayWriteIndex_ = 0;
    
    lastAzimuth_ = 0.0f;
    lastElevation_ = 0.0f;
    lastDistance_ = 1.0f;
    lastLeftGain_ = 0.5f;
    lastRightGain_ = 0.5f;
    lastX_ = 0.0f;
    lastY_ = 0.0f;
    lastZ_ = 1.0f;
    
    currentAngle_ = 0.0f;
    randomTargetX_ = 0.0f;
    randomTargetY_ = 0.0f;
    randomTargetZ_ = 1.0f;
    randomCurrentX_ = 0.0f;
    randomCurrentY_ = 0.0f;
    randomCurrentZ_ = 1.0f;
    randomTimeAccumulator_ = 0.0f;
}

void SpatialAudioProcessor::clearBuffers() {
    std::lock_guard<std::mutex> lock(initMutex_);
    
    std::fill(leftDelayBuffer_.begin(), leftDelayBuffer_.end(), 0.0f);
    std::fill(rightDelayBuffer_.begin(), rightDelayBuffer_.end(), 0.0f);
    delayWriteIndex_ = 0;
    
    frontBackFilterState_[0] = 0.0f;
    frontBackFilterState_[1] = 0.0f;
    frontBackFilterState_[2] = 0.0f;
    frontBackFilterState_[3] = 0.0f;
    
    LOGI("SpatialAudioProcessor buffers cleared");
}

void SpatialAudioProcessor::setEnabled(bool enabled) {
    enabled_.store(enabled);
}

void SpatialAudioProcessor::setIntensity(float intensity) {
    params_.intensity.store(intensity);
}

void SpatialAudioProcessor::setOffsetType(int type) {
    params_.offsetType.store(type);
}

void SpatialAudioProcessor::setFixedOffset(float leftRight, float upDown, float frontBack, float multiplier) {
    params_.leftRight.store(leftRight);
    params_.upDown.store(upDown);
    params_.frontBack.store(frontBack);
    params_.multiplier.store(multiplier);
}

void SpatialAudioProcessor::setSurroundParams(int mode, float radius, float periodSeconds) {
    params_.surroundMode.store(mode);
    params_.surroundRadius.store(radius);
    // periodSeconds 语义为"秒/圈"（数值越大转得越慢）
    params_.surroundPeriodSeconds.store(periodSeconds);
}

void SpatialAudioProcessor::setRandomParams(float maxDistance, float minDistance, float randomValue, float speed) {
    params_.randomMaxDistance.store(maxDistance);
    params_.randomMinDistance.store(minDistance);
    params_.randomValue.store(randomValue);
    params_.randomSpeed.store(speed);
}

void SpatialAudioProcessor::setScatterParams(
    float minRadius, float maxRadius,
    bool xEnabled, bool yEnabled, bool zEnabled,
    bool moveEnabled, float moveRandomValue, float moveSpeed, float directionRandom
) {
    params_.scatterMinRadius.store(minRadius);
    params_.scatterMaxRadius.store(maxRadius);
    params_.scatterXEnabled.store(xEnabled);
    params_.scatterYEnabled.store(yEnabled);
    params_.scatterZEnabled.store(zEnabled);
    params_.scatterMoveEnabled.store(moveEnabled);
    params_.scatterMoveRandomValue.store(moveRandomValue);
    params_.scatterMoveSpeed.store(moveSpeed);
    params_.scatterDirectionRandom.store(directionRandom);
}

void SpatialAudioProcessor::getCurrentPosition(float& azimuth, float& elevation, float& distance) const {
    azimuth = lastAzimuth_;
    elevation = lastElevation_;
    distance = lastDistance_;
}

void SpatialAudioProcessor::calculateHrtf(float azimuth, float elevation, float distance,
                                          float& leftGain, float& rightGain, float& itdSamples, float& frontBackFactor) {
    constexpr float HEAD_RADIUS = 0.0875f;
    constexpr float SPEED_OF_SOUND = 343.0f;
    constexpr float kStereoLeftRadians = 90.0f * 3.14159265f / 180.0f;
    constexpr float kStereoRightRadians = -90.0f * 3.14159265f / 180.0f;
    
    float azimuthRad = azimuth * 3.14159265f / 180.0f;
    float elevationRad = elevation * 3.14159265f / 180.0f;
    
    float cosElevation = std::cos(elevationRad);
    
    leftGain = 0.5f * (1.0f + std::cos(kStereoLeftRadians - azimuthRad) * cosElevation);
    rightGain = 0.5f * (1.0f + std::cos(kStereoRightRadians - azimuthRad) * cosElevation);
    
    float sinAzimuth = std::sin(azimuthRad);
    float itdSeconds = (HEAD_RADIUS / SPEED_OF_SOUND) * (sinAzimuth + azimuthRad * std::cos(azimuthRad));
    itdSamples = itdSeconds * static_cast<float>(sampleRate_.load());
    itdSamples = std::clamp(itdSamples, -static_cast<float>(MAX_ITD_DELAY_SAMPLES / 2), 
                            static_cast<float>(MAX_ITD_DELAY_SAMPLES / 2));
    
    float clampedDistance = std::max(0.1f, distance);
    float distanceGain = 1.0f / clampedDistance;
    distanceGain = std::min(distanceGain, 2.0f);
    
    float totalGain = leftGain + rightGain;
    if (totalGain > 0.001f) {
        leftGain = (leftGain / totalGain) * distanceGain;
        rightGain = (rightGain / totalGain) * distanceGain;
    } else {
        leftGain = 0.5f * distanceGain;
        rightGain = 0.5f * distanceGain;
    }
    
    float absAzimuth = std::abs(azimuth);
    if (absAzimuth <= 90.0f) {
        frontBackFactor = 1.0f;
    } else {
        float backness = (absAzimuth - 90.0f) / 90.0f;
        frontBackFactor = 1.0f - backness * 0.6f;
    }
}

void SpatialAudioProcessor::updateSurroundPosition() {
    int mode = params_.surroundMode.load();
    float radius = params_.surroundRadius.load();
    // 参数语义为"秒/圈"（periodSeconds）：数值越大转得越慢
    float periodSeconds = params_.surroundPeriodSeconds.load();
    
    // 角速度 ω = 2π / T（T 为周期秒数）。periodSeconds 必须为正数，
    // 过小会导致角速度爆炸，这里钳制到 0.05s（即最快 20 圈/秒）。
    constexpr float kTwoPi = 2.0f * 3.14159265f;
    constexpr float kMinPeriodSeconds = 0.05f;
    if (periodSeconds > 0.001f) {
        if (periodSeconds < kMinPeriodSeconds) {
            periodSeconds = kMinPeriodSeconds;
        }
        float deltaTime = static_cast<float>(framesPerBuffer_.load()) /
                          static_cast<float>(sampleRate_.load());
        float angleIncrement = (kTwoPi / periodSeconds) * deltaTime;
        currentAngle_ += angleIncrement;
        if (currentAngle_ > kTwoPi) {
            currentAngle_ -= kTwoPi;
        }
    }
    
    switch (static_cast<SurroundMode>(mode)) {
        case SurroundMode::Horizontal:
            lastX_ = std::sin(currentAngle_) * radius;
            lastY_ = 0.0f;
            lastZ_ = -std::cos(currentAngle_) * radius;
            break;
        case SurroundMode::Vertical:
            lastX_ = 0.0f;
            lastY_ = std::sin(currentAngle_) * radius;
            lastZ_ = -std::cos(currentAngle_) * radius;
            break;
        case SurroundMode::Transverse:
            lastX_ = std::sin(currentAngle_) * radius;
            lastY_ = std::cos(currentAngle_) * radius;
            lastZ_ = 0.0f;
            break;
    }
}

void SpatialAudioProcessor::updateRandomPosition() {
    float maxDistance = params_.randomMaxDistance.load();
    float minDistance = params_.randomMinDistance.load();
    float randomValue = params_.randomValue.load();
    float speed = params_.randomSpeed.load();
    
    randomTimeAccumulator_ += speed * 0.001f;
    
    if (randomTimeAccumulator_ > 1.0f) {
        randomTimeAccumulator_ = 0.0f;
        
        float theta = randf() * 2.0f * 3.14159265f;
        float phi = randf() * 3.14159265f;
        float r = minDistance + randf() * (maxDistance - minDistance);
        
        randomTargetX_ = r * std::sin(phi) * std::cos(theta);
        randomTargetY_ = r * std::sin(phi) * std::sin(theta);
        randomTargetZ_ = r * std::cos(phi);
    }
    
    float smoothing = 0.01f * randomValue;
    randomCurrentX_ += (randomTargetX_ - randomCurrentX_) * smoothing;
    randomCurrentY_ += (randomTargetY_ - randomCurrentY_) * smoothing;
    randomCurrentZ_ += (randomTargetZ_ - randomCurrentZ_) * smoothing;
    
    lastX_ = randomCurrentX_;
    lastY_ = randomCurrentY_;
    lastZ_ = randomCurrentZ_;
}

void SpatialAudioProcessor::updateScatterPosition() {
    float minRadius = params_.scatterMinRadius.load();
    float maxRadius = params_.scatterMaxRadius.load();
    bool xEnabled = params_.scatterXEnabled.load();
    bool yEnabled = params_.scatterYEnabled.load();
    bool zEnabled = params_.scatterZEnabled.load();
    bool moveEnabled = params_.scatterMoveEnabled.load();
    float moveRandomValue = params_.scatterMoveRandomValue.load();
    float moveSpeed = params_.scatterMoveSpeed.load();
    float directionRandom = params_.scatterDirectionRandom.load();
    
    // 如果位置未初始化，先设置一次随机位置
    if (!scatterPositionInitialized_) {
        float r = minRadius + randf() * (maxRadius - minRadius);
        float theta = randf() * 2.0f * 3.14159265f;
        float phi = randf() * 3.14159265f;
        
        randomCurrentX_ = xEnabled ? (r * std::sin(phi) * std::cos(theta)) : 0.0f;
        randomCurrentY_ = yEnabled ? (r * std::sin(phi) * std::sin(theta)) : 0.0f;
        randomCurrentZ_ = zEnabled ? (r * std::cos(phi)) : r;
        
        randomTargetX_ = randomCurrentX_;
        randomTargetY_ = randomCurrentY_;
        randomTargetZ_ = randomCurrentZ_;
        
        scatterPositionInitialized_ = true;
    }
    
    // 如果动态移动未启用，保持位置固定
    if (!moveEnabled) {
        lastX_ = randomCurrentX_;
        lastY_ = randomCurrentY_;
        lastZ_ = randomCurrentZ_;
        return;
    }
    
    // 动态移动启用时，执行移动逻辑
    randomTimeAccumulator_ += moveSpeed * 0.001f;
    
    // 判断是否需要移动（基于移动随机值）
    bool shouldMove = randf() <= moveRandomValue;
    
    if (randomTimeAccumulator_ > 1.0f || shouldMove) {
        randomTimeAccumulator_ = 0.0f;
        
        float r = minRadius + randf() * (maxRadius - minRadius);
        
        float theta = randf() * 2.0f * 3.14159265f;
        float phi = randf() * 3.14159265f;
        
        // 根据轴启用状态设置目标位置
        float targetX = xEnabled ? (r * std::sin(phi) * std::cos(theta)) : 0.0f;
        float targetY = yEnabled ? (r * std::sin(phi) * std::sin(theta)) : 0.0f;
        float targetZ = zEnabled ? (r * std::cos(phi)) : r;  // 如果Z禁用，保持固定距离
        
        // 方向随机值影响目标位置的偏移
        if (directionRandom > 0.0f) {
            float dirOffset = directionRandom * (randf() - 0.5f) * 2.0f;
            if (xEnabled) targetX += dirOffset * r * 0.2f;
            if (yEnabled) targetY += dirOffset * r * 0.2f;
            if (zEnabled) targetZ += dirOffset * r * 0.1f;
        }
        
        randomTargetX_ = targetX;
        randomTargetY_ = targetY;
        randomTargetZ_ = targetZ;
    }
    
    // 平滑过渡
    float smoothing = 0.02f * moveSpeed;
    randomCurrentX_ += (randomTargetX_ - randomCurrentX_) * smoothing;
    randomCurrentY_ += (randomTargetY_ - randomCurrentY_) * smoothing;
    randomCurrentZ_ += (randomTargetZ_ - randomCurrentZ_) * smoothing;
    
    lastX_ = randomCurrentX_;
    lastY_ = randomCurrentY_;
    lastZ_ = randomCurrentZ_;
}

void SpatialAudioProcessor::process(float* input, float* output, int numFrames) {
    if (!initialized_.load() || !enabled_.load()) {
        if (input != output) {
            std::copy(input, input + numFrames * 2, output);
        }
        return;
    }
    
    float azimuth = 0.0f;
    float elevation = 0.0f;
    float distance = 1.0f;
    
    int offsetType = params_.offsetType.load();
    
    switch (offsetType) {
        case static_cast<int>(OffsetType::Fixed): {
            float rotX = params_.leftRight.load();
            float rotY = params_.upDown.load();
            float rotZ = params_.frontBack.load();
            float dist = params_.multiplier.load();
            
            if (dist < 0.001f) {
                break;
            }
            
            float pitch = rotX * 3.14159265f / 180.0f;
            float yaw = rotY * 3.14159265f / 180.0f;
            float roll = rotZ * 3.14159265f / 180.0f;
            
            float x = -std::sin(yaw) * std::cos(pitch);
            float y = std::sin(pitch);
            float z = -std::cos(yaw) * std::cos(pitch);
            
            float cr = std::cos(roll);
            float sr = std::sin(roll);
            float newX = x * cr - y * sr;
            float newY = x * sr + y * cr;
            
            x = newX * dist;
            y = newY * dist;
            z = z * dist;
            
            float actualDistance = std::sqrt(x * x + y * y + z * z);
            
            if (actualDistance > 0.001f) {
                azimuth = std::atan2(x, -z) * 180.0f / 3.14159265f;
                elevation = std::asin(std::clamp(y / actualDistance, -1.0f, 1.0f)) * 180.0f / 3.14159265f;
                distance = actualDistance;
            }
            break;
        }
            
        case static_cast<int>(OffsetType::Surround): {
            updateSurroundPosition();
            float x = lastX_;
            float y = lastY_;
            float z = lastZ_;
            distance = std::sqrt(x * x + y * y + z * z);
            if (distance > 0.001f) {
                azimuth = std::atan2(x, -z) * 180.0f / 3.14159265f;
                elevation = std::asin(y / distance) * 180.0f / 3.14159265f;
            }
            break;
        }
            
        case static_cast<int>(OffsetType::Random): {
            updateRandomPosition();
            float x = lastX_;
            float y = lastY_;
            float z = lastZ_;
            distance = std::sqrt(x * x + y * y + z * z);
            if (distance > 0.001f) {
                azimuth = std::atan2(x, -z) * 180.0f / 3.14159265f;
                elevation = std::asin(y / distance) * 180.0f / 3.14159265f;
            }
            break;
        }
            
        case static_cast<int>(OffsetType::Scatter): {
            updateScatterPosition();
            float x = lastX_;
            float y = lastY_;
            float z = lastZ_;
            distance = std::sqrt(x * x + y * y + z * z);
            if (distance > 0.001f) {
                azimuth = std::atan2(x, -z) * 180.0f / 3.14159265f;
                elevation = std::asin(y / distance) * 180.0f / 3.14159265f;
            }
            break;
        }
    }
    
    float azimuthDiff = azimuth - lastAzimuth_;
    if (azimuthDiff > 180.0f) {
        azimuth -= 360.0f;
    } else if (azimuthDiff < -180.0f) {
        azimuth += 360.0f;
    }
    
    azimuth = lastAzimuth_ + (azimuth - lastAzimuth_) * POSITION_SMOOTHING;
    elevation = lastElevation_ + (elevation - lastElevation_) * POSITION_SMOOTHING;
    distance = lastDistance_ + (distance - lastDistance_) * POSITION_SMOOTHING;
    
    while (azimuth > 180.0f) azimuth -= 360.0f;
    while (azimuth < -180.0f) azimuth += 360.0f;
    
    lastAzimuth_ = azimuth;
    lastElevation_ = elevation;
    lastDistance_ = distance;
    
    float leftGain, rightGain, itdSamples, frontBackFactor;
    calculateHrtf(azimuth, elevation, distance, leftGain, rightGain, itdSamples, frontBackFactor);
    
    leftGain = lastLeftGain_ + (leftGain - lastLeftGain_) * GAIN_SMOOTHING;
    rightGain = lastRightGain_ + (rightGain - lastRightGain_) * GAIN_SMOOTHING;
    frontBackFactor = lastFrontBackFactor_ + (frontBackFactor - lastFrontBackFactor_) * GAIN_SMOOTHING;
    itdSamples = lastItdSamples_ + (itdSamples - lastItdSamples_) * ITD_SMOOTHING;
    
    lastLeftGain_ = leftGain;
    lastRightGain_ = rightGain;
    lastFrontBackFactor_ = frontBackFactor;
    lastItdSamples_ = itdSamples;
    
    float wetGain = params_.intensity.load();
    float dryGain = 1.0f - wetGain;
    
    float leftDelayFloat = itdSamples > 0 ? itdSamples : 0.0f;
    float rightDelayFloat = itdSamples < 0 ? -itdSamples : 0.0f;
    
    float filterCoeff = std::tan(3.14159265f * frontBackFactor * 6000.0f / sampleRate_.load());
    filterCoeff = filterCoeff / (1.0f + filterCoeff);
    
    for (int i = 0; i < numFrames; ++i) {
        float inLeft = input[i * 2];
        float inRight = input[i * 2 + 1];
        
        // 保留立体声：左右声道独立写入延迟缓冲区
        leftDelayBuffer_[delayWriteIndex_] = inLeft;
        rightDelayBuffer_[delayWriteIndex_] = inRight;
        
        // 4点 Hermite 插值（替代线性插值，减少高频频谱畸变）
        float leftDelayInt = 0.0f, leftDelayFrac = 0.0f;
        float rightDelayInt = 0.0f, rightDelayFrac = 0.0f;
        
        if (leftDelayFloat > 0.001f) {
            leftDelayInt = std::floor(leftDelayFloat);
            leftDelayFrac = leftDelayFloat - leftDelayInt;
        }
        if (rightDelayFloat > 0.001f) {
            rightDelayInt = std::floor(rightDelayFloat);
            rightDelayFrac = rightDelayFloat - rightDelayInt;
        }
        
        int leftIdx0 = (delayWriteIndex_ - static_cast<int>(leftDelayInt) + MAX_ITD_DELAY_SAMPLES) % MAX_ITD_DELAY_SAMPLES;
        int leftIdx1 = (leftIdx0 - 1 + MAX_ITD_DELAY_SAMPLES) % MAX_ITD_DELAY_SAMPLES;
        int leftIdxM1 = (leftIdx0 + 1) % MAX_ITD_DELAY_SAMPLES;
        int leftIdx2 = (leftIdx1 - 1 + MAX_ITD_DELAY_SAMPLES) % MAX_ITD_DELAY_SAMPLES;
        
        int rightIdx0 = (delayWriteIndex_ - static_cast<int>(rightDelayInt) + MAX_ITD_DELAY_SAMPLES) % MAX_ITD_DELAY_SAMPLES;
        int rightIdx1 = (rightIdx0 - 1 + MAX_ITD_DELAY_SAMPLES) % MAX_ITD_DELAY_SAMPLES;
        int rightIdxM1 = (rightIdx0 + 1) % MAX_ITD_DELAY_SAMPLES;
        int rightIdx2 = (rightIdx1 - 1 + MAX_ITD_DELAY_SAMPLES) % MAX_ITD_DELAY_SAMPLES;
        
        // Hermite 4点插值: c0=idxM1, c1=idx0, c2=idx1, c3=idx2
        auto hermiteInterp = [](float c0, float c1, float c2, float c3, float t) {
            float t2 = t * t, t3 = t2 * t;
            return 0.5f * ((2.0f * c1) + (-c0 + c2) * t +
                           (2.0f * c0 - 5.0f * c1 + 4.0f * c2 - c3) * t2 +
                           (-c0 + 3.0f * c1 - 3.0f * c2 + c3) * t3);
        };
        
        float delayedLeft = hermiteInterp(
            leftDelayBuffer_[leftIdxM1], leftDelayBuffer_[leftIdx0],
            leftDelayBuffer_[leftIdx1], leftDelayBuffer_[leftIdx2], leftDelayFrac);
        float delayedRight = hermiteInterp(
            rightDelayBuffer_[rightIdxM1], rightDelayBuffer_[rightIdx0],
            rightDelayBuffer_[rightIdx1], rightDelayBuffer_[rightIdx2], rightDelayFrac);
        
        float outLeft = delayedLeft * leftGain;
        float outRight = delayedRight * rightGain;
        
        // 前后遮蔽：级联二阶 IIR 低通（替代一阶，后方音色更真实）
        {
            float tmpL = outLeft + filterCoeff * (frontBackFilterState_[0] - outLeft);
            frontBackFilterState_[0] = tmpL;
            outLeft = tmpL + filterCoeff * (frontBackFilterState_[2] - tmpL);
            frontBackFilterState_[2] = outLeft;
            
            float tmpR = outRight + filterCoeff * (frontBackFilterState_[1] - outRight);
            frontBackFilterState_[1] = tmpR;
            outRight = tmpR + filterCoeff * (frontBackFilterState_[3] - tmpR);
            frontBackFilterState_[3] = outRight;
        }
        
        output[i * 2] = outLeft * wetGain + inLeft * dryGain;
        output[i * 2 + 1] = outRight * wetGain + inRight * dryGain;
        
        delayWriteIndex_ = (delayWriteIndex_ + 1) % MAX_ITD_DELAY_SAMPLES;
    }
}
