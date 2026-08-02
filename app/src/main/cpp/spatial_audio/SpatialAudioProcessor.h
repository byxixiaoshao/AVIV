#ifndef SPATIAL_AUDIO_PROCESSOR_H
#define SPATIAL_AUDIO_PROCESSOR_H

#include <mutex>
#include <string>
#include <atomic>
#include <vector>
#include <cmath>
#include <algorithm>

enum class OffsetType {
    Fixed = 0,
    Surround = 1,
    Random = 2,
    Scatter = 3  // 散点模式
};

enum class SurroundMode {
    Horizontal = 0,
    Vertical = 1,
    Transverse = 2
};

struct SpatialParams {
    std::atomic<int> offsetType{0};
    std::atomic<float> intensity{1.0f};
    
    std::atomic<float> leftRight{0.0f};
    std::atomic<float> upDown{0.0f};
    std::atomic<float> frontBack{0.0f};
    std::atomic<float> multiplier{1.0f};
    
    std::atomic<int> surroundMode{0};
    std::atomic<float> surroundRadius{1.0f};
    // 环绕周期（秒/圈）：数值越大转得越慢，角速度 ω = 2π / surroundPeriodSeconds
    std::atomic<float> surroundPeriodSeconds{5.0f};
    
    // 散点随机参数
    std::atomic<float> scatterMinRadius{0.5f};
    std::atomic<float> scatterMaxRadius{5.0f};
    std::atomic<bool> scatterXEnabled{true};
    std::atomic<bool> scatterYEnabled{true};
    std::atomic<bool> scatterZEnabled{true};
    std::atomic<bool> scatterMoveEnabled{false};
    std::atomic<float> scatterMoveRandomValue{0.5f};
    std::atomic<float> scatterMoveSpeed{1.0f};
    std::atomic<float> scatterDirectionRandom{0.3f};
    
    // 旧参数（保留兼容）
    std::atomic<float> randomMaxDistance{5.0f};
    std::atomic<float> randomMinDistance{0.5f};
    std::atomic<float> randomValue{0.5f};
    std::atomic<float> randomSpeed{1.0f};
};

class SpatialAudioProcessor {
public:
    SpatialAudioProcessor();
    ~SpatialAudioProcessor();
    
    bool init(int sampleRate, int framesPerBuffer);
    void reset();
    void clearBuffers();
    
    void process(float* input, float* output, int numFrames);
    
    void setEnabled(bool enabled);
    bool isEnabled() const { return enabled_.load(); }
    
    void setIntensity(float intensity);
    float getIntensity() const { return params_.intensity.load(); }
    
    void setOffsetType(int type);
    void setFixedOffset(float leftRight, float upDown, float frontBack, float multiplier);
    // 设置环绕参数。periodSeconds 语义为"秒/圈"（数值越大转得越慢）
    void setSurroundParams(int mode, float radius, float periodSeconds);
    void setRandomParams(float maxDistance, float minDistance, float randomValue, float speed);
    
    // 散点参数设置
    void setScatterParams(
        float minRadius, float maxRadius,
        bool xEnabled, bool yEnabled, bool zEnabled,
        bool moveEnabled, float moveRandomValue, float moveSpeed, float directionRandom
    );
    
    void getCurrentPosition(float& azimuth, float& elevation, float& distance) const;
    
private:
    void updateSurroundPosition();
    void updateRandomPosition();
    void updateScatterPosition();
    void calculateHrtf(float azimuth, float elevation, float distance,
                       float& leftGain, float& rightGain, float& itdSamples, float& frontBackFactor);
    
    std::atomic<bool> initialized_{false};
    std::atomic<bool> enabled_{false};
    std::atomic<int> sampleRate_{48000};
    std::atomic<int> framesPerBuffer_{256};
    
    SpatialParams params_;
    std::mutex initMutex_;
    
    float currentAngle_ = 0.0f;
    
    float randomTargetX_ = 0.0f;
    float randomTargetY_ = 0.0f;
    float randomTargetZ_ = 1.0f;
    float randomCurrentX_ = 0.0f;
    float randomCurrentY_ = 0.0f;
    float randomCurrentZ_ = 1.0f;
    float randomTimeAccumulator_ = 0.0f;
    bool scatterPositionInitialized_ = false;
    
    float lastX_ = 0.0f;
    float lastY_ = 0.0f;
    float lastZ_ = 1.0f;
    
    float lastAzimuth_ = 0.0f;
    float lastElevation_ = 0.0f;
    float lastDistance_ = 1.0f;
    float lastLeftGain_ = 0.5f;
    float lastRightGain_ = 0.5f;
    float lastFrontBackFactor_ = 1.0f;
    float lastItdSamples_ = 0.0f;
    
    float frontBackFilterState_[4] = {0.0f, 0.0f, 0.0f, 0.0f};
    
    std::vector<float> leftDelayBuffer_;
    std::vector<float> rightDelayBuffer_;
    int delayWriteIndex_ = 0;
    static constexpr int MAX_ITD_DELAY_SAMPLES = 64;
    
    static constexpr float POSITION_SMOOTHING = 0.08f;
    static constexpr float GAIN_SMOOTHING = 0.06f;
    static constexpr float ITD_SMOOTHING = 0.05f;
    static constexpr float MIN_DISTANCE = 0.1f;
};

#endif
