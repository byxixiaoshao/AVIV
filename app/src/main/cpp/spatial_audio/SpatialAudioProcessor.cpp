#include "SpatialAudioProcessor.h"
#include <algorithm>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "SpatialAudioProcessor", __VA_ARGS__)

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

void SpatialAudioProcessor::setSurroundParams(int mode, float radius, float speed) {
    params_.surroundMode.store(mode);
    params_.surroundRadius.store(radius);
    params_.surroundSpeed.store(speed);
}

void SpatialAudioProcessor::setRandomParams(float maxDistance, float minDistance, float randomValue, float speed) {
    params_.randomMaxDistance.store(maxDistance);
    params_.randomMinDistance.store(minDistance);
    params_.randomValue.store(randomValue);
    params_.randomSpeed.store(speed);
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
    float speed = params_.surroundSpeed.load();
    
    if (speed > 0.001f) {
        float angleIncrement = (2.0f * 3.14159265f / speed) * 
                               static_cast<float>(framesPerBuffer_.load()) / 
                               static_cast<float>(sampleRate_.load());
        currentAngle_ += angleIncrement;
        if (currentAngle_ > 2.0f * 3.14159265f) {
            currentAngle_ -= 2.0f * 3.14159265f;
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
        
        float theta = static_cast<float>(rand()) / static_cast<float>(RAND_MAX) * 2.0f * 3.14159265f;
        float phi = static_cast<float>(rand()) / static_cast<float>(RAND_MAX) * 3.14159265f;
        float r = minDistance + static_cast<float>(rand()) / static_cast<float>(RAND_MAX) * (maxDistance - minDistance);
        
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
    
    float filterCoeff = frontBackFactor * 0.3f;
    
    for (int i = 0; i < numFrames; ++i) {
        float inLeft = input[i * 2];
        float inRight = input[i * 2 + 1];
        
        float mono = (inLeft + inRight) * 0.5f;
        
        leftDelayBuffer_[delayWriteIndex_] = mono;
        rightDelayBuffer_[delayWriteIndex_] = mono;
        
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
        
        int leftReadIndex0 = (delayWriteIndex_ - static_cast<int>(leftDelayInt) + MAX_ITD_DELAY_SAMPLES) % MAX_ITD_DELAY_SAMPLES;
        int leftReadIndex1 = (leftReadIndex0 - 1 + MAX_ITD_DELAY_SAMPLES) % MAX_ITD_DELAY_SAMPLES;
        
        int rightReadIndex0 = (delayWriteIndex_ - static_cast<int>(rightDelayInt) + MAX_ITD_DELAY_SAMPLES) % MAX_ITD_DELAY_SAMPLES;
        int rightReadIndex1 = (rightReadIndex0 - 1 + MAX_ITD_DELAY_SAMPLES) % MAX_ITD_DELAY_SAMPLES;
        
        float delayedLeft = leftDelayBuffer_[leftReadIndex0] * (1.0f - leftDelayFrac) + 
                           leftDelayBuffer_[leftReadIndex1] * leftDelayFrac;
        float delayedRight = rightDelayBuffer_[rightReadIndex0] * (1.0f - rightDelayFrac) + 
                            rightDelayBuffer_[rightReadIndex1] * rightDelayFrac;
        
        float outLeft = delayedLeft * leftGain;
        float outRight = delayedRight * rightGain;
        
        outLeft = outLeft + filterCoeff * (frontBackFilterState_[0] - outLeft);
        outRight = outRight + filterCoeff * (frontBackFilterState_[1] - outRight);
        frontBackFilterState_[0] = outLeft;
        frontBackFilterState_[1] = outRight;
        
        output[i * 2] = outLeft * wetGain + inLeft * dryGain;
        output[i * 2 + 1] = outRight * wetGain + inRight * dryGain;
        
        delayWriteIndex_ = (delayWriteIndex_ + 1) % MAX_ITD_DELAY_SAMPLES;
    }
}
