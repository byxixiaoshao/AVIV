#include <jni.h>
#include <string>
#include <android/log.h>
#include "AudioPlayFunc/AudioEngine.h"

#define LOG_TAG "JniInterface"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

static jboolean nativeInit_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    bool result = engine->init();
    LOGI("AudioEngine init result: %d", result);
    return result ? JNI_TRUE : JNI_FALSE;
}

static void nativeRelease_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    engine->release();
    LOGI("AudioEngine released");
}

// P2-7: 轻量重建音频流（保留已加载的 tracks）
static jboolean nativeRecreateStream_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    bool result = engine->recreateStream();
    LOGI("AudioEngine recreateStream result: %d", result);
    return result ? JNI_TRUE : JNI_FALSE;
}

static void nativeWarmup_impl(JNIEnv* env, jobject thiz) {
    LOGI("AudioEngine warmup (no-op)");
}

static jint nativeLoadSound_impl(JNIEnv* env, jobject thiz,
                                                                jstring soundId, jstring filePath) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    int result = engine->loadTrack(std::string(id), std::string(path));
    
    env->ReleaseStringUTFChars(soundId, id);
    env->ReleaseStringUTFChars(filePath, path);
    
    return result;
}

static jint nativeLoadSoundFromFd_impl(JNIEnv* env, jobject thiz,
                                                                      jstring soundId, jint fd,
                                                                      jlong offset, jlong length,
                                                                      jstring filePath) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    const char* path = filePath ? env->GetStringUTFChars(filePath, nullptr) : "";
    
    auto* engine = AudioEngine::getInstance();
    int result = engine->loadTrackFromFd(std::string(id), fd, offset, length, std::string(path));
    
    env->ReleaseStringUTFChars(soundId, id);
    if (filePath) env->ReleaseStringUTFChars(filePath, path);
    
    return result;
}

static void nativeUnloadSound_impl(JNIEnv* env, jobject thiz,
                                                                  jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->unloadTrack(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativePlaySound_impl(JNIEnv* env, jobject thiz,
                                                                jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->playTrack(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeStopSound_impl(JNIEnv* env, jobject thiz,
                                                                jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->stopTrack(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeStopAllSounds_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    engine->stopAllTracks();
}

static void nativePauseSound_impl(JNIEnv* env, jobject thiz,
                                                                 jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->pauseTrack(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeResumeSound_impl(JNIEnv* env, jobject thiz,
                                                                  jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->playTrack(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetVolume_impl(JNIEnv* env, jobject thiz,
                                                                jstring soundId, jfloat volume) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackVolume(std::string(id), volume);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static jfloat nativeGetVolume_impl(JNIEnv* env, jobject thiz,
                                                                jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    float volume = engine->getTrackVolume(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
    return volume;
}

static void nativePauseAll_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    engine->stopAllTracks();
}

static void nativeResumeAll_impl(JNIEnv* env, jobject thiz) {
    LOGI("nativeResumeAll (not implemented)");
}

static jboolean nativeIsPlaying_impl(JNIEnv* env, jobject thiz,
                                                                jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    bool playing = engine->isTrackPlaying(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
    return playing ? JNI_TRUE : JNI_FALSE;
}

static jboolean nativeIsLoaded_impl(JNIEnv* env, jobject thiz,
                                                               jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    bool loaded = engine->isTrackLoaded(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
    return loaded ? JNI_TRUE : JNI_FALSE;
}

static jboolean nativeIsLoading_impl(JNIEnv* env, jobject thiz,
                                                                jstring soundId) {
    return JNI_FALSE;
}

static void nativeSetEffectEnabled_impl(JNIEnv* env, jobject thiz,
                                                                       jstring soundId, jboolean enabled) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackEffectEnabled(std::string(id), enabled == JNI_TRUE);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetReverbParams_impl(JNIEnv* env, jobject thiz,
                                                                      jstring soundId,
                                                                      jfloat roomSize, jfloat damping,
                                                                      jfloat wetLevel) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackReverbParams(std::string(id), roomSize, damping, wetLevel, 0.7f);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetInsulation_impl(JNIEnv* env, jobject thiz,
                                                                    jstring soundId, jfloat insulation) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackInsulation(std::string(id), insulation);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetReverbDecayTime_impl(JNIEnv* env, jobject thiz,
                                                                         jstring soundId, jfloat decayTime) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackDecayTime(std::string(id), decayTime);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetReverbPreDelay_impl(JNIEnv* env, jobject thiz,
                                                                        jstring soundId, jfloat preDelay) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackPreDelay(std::string(id), preDelay);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetReverbDryLevel_impl(JNIEnv* env, jobject thiz,
                                                                        jstring soundId, jfloat dryLevel) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackDryLevel(std::string(id), dryLevel);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetReflectionDensity_impl(JNIEnv* env, jobject thiz,
                                                                           jstring soundId, jfloat density) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackReflectionDensity(std::string(id), density);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetReflectionSpread_impl(JNIEnv* env, jobject thiz,
                                                                          jstring soundId, jfloat spread) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackReflectionSpread(std::string(id), spread);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetHighpassCutoff_impl(JNIEnv* env, jobject thiz,
                                                                        jstring soundId, jfloat cutoff) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackHighpassCutoff(std::string(id), cutoff);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetEarlyReflectionLevel_impl(JNIEnv* env, jobject thiz,
                                                                              jstring soundId, jfloat level) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackEarlyReflectionLevel(std::string(id), level);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetCreativeEffectIntensity_impl(JNIEnv* env, jobject thiz,
                                                                                 jstring soundId, jint effectType, jfloat intensity) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);

    auto* engine = AudioEngine::getInstance();
    engine->setTrackCreativeEffectIntensity(std::string(id), effectType, intensity);

    env->ReleaseStringUTFChars(soundId, id);
}

// SoundTouch：独立播放速率 / 音调调节（pitch 与 speed 解耦）
static void nativeSetPlaybackSpeed_impl(JNIEnv* env, jobject thiz,
                                                                      jstring soundId, jfloat speed) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);

    auto* engine = AudioEngine::getInstance();
    engine->setTrackPlaybackSpeed(std::string(id), speed);

    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetPitchShift_impl(JNIEnv* env, jobject thiz,
                                                                    jstring soundId, jfloat semitones) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);

    auto* engine = AudioEngine::getInstance();
    engine->setTrackPitchShift(std::string(id), semitones);

    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSeekTo_impl(JNIEnv* env, jobject thiz,
                                                             jstring soundId, jlong positionMs) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->seekTrack(std::string(id), positionMs);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static jlong nativeGetPosition_impl(JNIEnv* env, jobject thiz,
                                                                  jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    int64_t position = engine->getTrackPosition(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
    return position;
}

static jlong nativeGetDuration_impl(JNIEnv* env, jobject thiz,
                                                                  jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    int64_t duration = engine->getTrackDuration(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
    return duration;
}

static void nativeSetLooping_impl(JNIEnv* env, jobject thiz,
                                                                 jstring soundId, jboolean looping) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackLooping(std::string(id), looping == JNI_TRUE);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static jboolean nativeIsLooping_impl(JNIEnv* env, jobject thiz,
                                                                jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    bool looping = engine->isTrackLooping(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
    return looping ? JNI_TRUE : JNI_FALSE;
}

static jboolean nativeNeedsRestart_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    return engine->needsRestart() ? JNI_TRUE : JNI_FALSE;
}

static void nativeClearRestartFlag_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    engine->clearRestartFlag();
}

static jint nativeGetXRunCount_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    return engine->getXRunCount();
}

static jboolean nativeHasUnderrun_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    return engine->hasUnderrun() ? JNI_TRUE : JNI_FALSE;
}

static void nativeClearUnderrunFlag_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    engine->clearUnderrunFlag();
}

static void nativeSetEqBandGain_impl(JNIEnv* env, jobject thiz,
                                                                    jstring soundId, jint bandIndex, jfloat gain) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackEqBandGain(std::string(id), bandIndex, gain);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static jfloat nativeGetEqBandGain_impl(JNIEnv* env, jobject thiz,
                                                                    jstring soundId, jint bandIndex) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    float gain = engine->getTrackEqBandGain(std::string(id), bandIndex);
    
    env->ReleaseStringUTFChars(soundId, id);
    return gain;
}

static void nativeSetEqEnabled_impl(JNIEnv* env, jobject thiz,
                                                                   jstring soundId, jboolean enabled) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackEqEnabled(std::string(id), enabled == JNI_TRUE);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetEqLimiterEnabled_impl(JNIEnv* env, jobject thiz,
                                                                          jstring soundId, jboolean enabled) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackEqLimiterEnabled(std::string(id), enabled == JNI_TRUE);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetEqualizerCurve_impl(JNIEnv* env, jobject thiz,
    jstring soundId, jfloatArray frequencies, jfloatArray gains, jintArray filterTypes,
    jfloatArray qValues, jintArray curveIns, jintArray curveOuts) {
const char* id = env->GetStringUTFChars(soundId, nullptr);

jsize numPoints = env->GetArrayLength(frequencies);
jfloat* freqPtr = env->GetFloatArrayElements(frequencies, nullptr);
jfloat* gainPtr = env->GetFloatArrayElements(gains, nullptr);
jint* typePtr = env->GetIntArrayElements(filterTypes, nullptr);
jfloat* qPtr = env->GetFloatArrayElements(qValues, nullptr);
jint* cInPtr = env->GetIntArrayElements(curveIns, nullptr);
jint* cOutPtr = env->GetIntArrayElements(curveOuts, nullptr);

std::vector<audiofx::ControlPoint> points;
points.reserve(numPoints);
for (jsize i = 0; i < numPoints; i++) {
    audiofx::ControlPoint pt;
    pt.frequencyHz = freqPtr[i];
    pt.gainDb = gainPtr[i];
    pt.filterType = static_cast<audiofx::EqFilterType>(typePtr[i]);
    pt.Q = qPtr[i];
    pt.curveIn = static_cast<audiofx::CurveInterpolation>(cInPtr[i]);
    pt.curveOut = static_cast<audiofx::CurveInterpolation>(cOutPtr[i]);
    points.push_back(pt);
}

auto* engine = AudioEngine::getInstance();
engine->setTrackEqualizerCurve(std::string(id), points);

env->ReleaseFloatArrayElements(frequencies, freqPtr, JNI_ABORT);
env->ReleaseFloatArrayElements(gains, gainPtr, JNI_ABORT);
env->ReleaseIntArrayElements(filterTypes, typePtr, JNI_ABORT);
env->ReleaseFloatArrayElements(qValues, qPtr, JNI_ABORT);
env->ReleaseIntArrayElements(curveIns, cInPtr, JNI_ABORT);
env->ReleaseIntArrayElements(curveOuts, cOutPtr, JNI_ABORT);
env->ReleaseStringUTFChars(soundId, id);
}

static jfloat nativeGetFilterResponse_impl(JNIEnv* env, jobject thiz,
    jstring soundId, jfloat frequency) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    float response = engine->getTrackFilterResponse(std::string(id), frequency);
    env->ReleaseStringUTFChars(soundId, id);
    return response;
}

// Deprecated stubs
static void nativeSetEqGains_impl(JNIEnv* env, jobject thiz,
                                                                 jstring soundId, jfloatArray gains) {
    (void)env; (void)thiz; (void)soundId; (void)gains;
}

static jfloatArray nativeGetEqGains_impl(JNIEnv* env, jobject thiz,
                                                                 jstring soundId) {
    (void)thiz; (void)soundId;
    jfloatArray result = env->NewFloatArray(12);
    float zeros[12] = {0};
    env->SetFloatArrayRegion(result, 0, 12, zeros);
    return result;
}

static void nativeSetSpatialEnabled_impl(JNIEnv* env, jobject thiz,
    jstring soundId, jboolean enabled) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackSpatialEnabled(std::string(id), enabled == JNI_TRUE);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetSpatialIntensity_impl(JNIEnv* env, jobject thiz,
                                                                            jstring soundId, jfloat intensity) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackSpatialIntensity(std::string(id), intensity);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetSpatialOffsetType_impl(JNIEnv* env, jobject thiz,
                                                                             jstring soundId, jint type) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackSpatialOffsetType(std::string(id), type);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetSpatialFixedOffset_impl(JNIEnv* env, jobject thiz,
                                                                              jstring soundId,
                                                                              jfloat leftRight, jfloat upDown,
                                                                              jfloat frontBack, jfloat multiplier) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackSpatialFixedOffset(std::string(id), leftRight, upDown, frontBack, multiplier);
    
    env->ReleaseStringUTFChars(soundId, id);
}

// 设置声向环绕参数。periodSeconds 语义为"秒/圈"（数值越大转得越慢），
// C++ 内部按角速度 ω = 2π / periodSeconds 计算位置增量。
static void nativeSetSpatialSurroundParams_impl(JNIEnv* env, jobject thiz,
                                                                                  jstring soundId,
                                                                                  jint mode, jfloat radius, jfloat periodSeconds) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackSpatialSurroundParams(std::string(id), mode, radius, periodSeconds);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetSpatialRandomParams_impl(JNIEnv* env, jobject thiz,
                                                                                 jstring soundId,
                                                                                 jfloat maxDistance, jfloat minDistance,
                                                                                 jfloat randomValue, jfloat speed) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackSpatialRandomParams(std::string(id), maxDistance, minDistance, randomValue, speed);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetSpatialScatterParams_impl(JNIEnv* env, jobject thiz,
                                                                                  jstring soundId,
                                                                                  jfloat minRadius, jfloat maxRadius,
                                                                                  jboolean xEnabled, jboolean yEnabled, jboolean zEnabled,
                                                                                  jboolean moveEnabled, jfloat moveRandomValue,
                                                                                  jfloat moveSpeed, jfloat directionRandom) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackSpatialScatterParams(
        std::string(id),
        minRadius, maxRadius,
        xEnabled == JNI_TRUE, yEnabled == JNI_TRUE, zEnabled == JNI_TRUE,
        moveEnabled == JNI_TRUE, moveRandomValue, moveSpeed, directionRandom
    );
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetFadeDuration_impl(JNIEnv* env, jobject thiz,
                                                                          jstring soundId, jfloat durationSeconds) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackFadeDuration(std::string(id), durationSeconds);
    
    env->ReleaseStringUTFChars(soundId, id);
}

static jboolean nativeIsFadingOut_impl(JNIEnv* env, jobject thiz,
                                                                      jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    bool fadingOut = engine->isTrackFadingOut(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
    return fadingOut ? JNI_TRUE : JNI_FALSE;
}

static void nativeCancelFadeOut_impl(JNIEnv* env, jobject thiz,
                                                                        jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->cancelTrackFadeOut(std::string(id));
    
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeClearAllEffectBuffers_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    engine->clearAllEffectBuffers();
    LOGI("All effect buffers cleared via JNI");
}

static void nativeSetEffectOrder_impl(JNIEnv* env, jobject thiz,
                                                                      jstring soundId, jintArray order) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    jint* orderPtr = env->GetIntArrayElements(order, nullptr);
    jsize len = env->GetArrayLength(order);
    
    std::vector<int> orderVec;
    for (jsize i = 0; i < len; i++) {
        orderVec.push_back(orderPtr[i]);
    }
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackEffectOrder(std::string(id), orderVec);
    
    env->ReleaseIntArrayElements(order, orderPtr, JNI_ABORT);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetGlobalLimiterConfig_impl(JNIEnv* env, jobject thiz,
                                              jboolean enabled,
                                              jboolean limitEqualizer,
                                              jboolean limitEffects,
                                              jboolean limitReverb,
                                              jboolean limitSpatial,
                                              jfloat threshold,
                                              jfloat attack,
                                              jfloat release) {
    auto* engine = AudioEngine::getInstance();
    audiofx::LimiterConfig config;
    config.enabled = enabled == JNI_TRUE;
    config.limitEqualizer = limitEqualizer == JNI_TRUE;
    config.limitEffects = limitEffects == JNI_TRUE;
    config.limitReverb = limitReverb == JNI_TRUE;
    config.limitSpatial = limitSpatial == JNI_TRUE;
    config.threshold = threshold;
    config.attack = attack;
    config.release = release;
    engine->setGlobalLimiterConfig(config);
    LOGI("Global limiter config set via JNI");
}

static jbooleanArray nativeGetGlobalLimiterConfig_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    auto config = engine->getGlobalLimiterConfig();
    
    jbooleanArray result = env->NewBooleanArray(5);
    if (result != nullptr) {
        jboolean flags[5] = {
            static_cast<jboolean>(config.enabled ? JNI_TRUE : JNI_FALSE),
            static_cast<jboolean>(config.limitEqualizer ? JNI_TRUE : JNI_FALSE),
            static_cast<jboolean>(config.limitEffects ? JNI_TRUE : JNI_FALSE),
            static_cast<jboolean>(config.limitReverb ? JNI_TRUE : JNI_FALSE),
            static_cast<jboolean>(config.limitSpatial ? JNI_TRUE : JNI_FALSE)
        };
        env->SetBooleanArrayRegion(result, 0, 5, flags);
    }
    return result;
}

static void nativeSetGlobalLimiterEnabled_impl(JNIEnv* env, jobject thiz, jboolean enabled) {
    auto* engine = AudioEngine::getInstance();
    engine->setGlobalLimiterEnabled(enabled == JNI_TRUE);
    LOGI("Global limiter enabled: %d", enabled == JNI_TRUE);
}

static jboolean nativeIsGlobalLimiterEnabled_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    return engine->isGlobalLimiterEnabled() ? JNI_TRUE : JNI_FALSE;
}

static void nativeSetLimitEffectsEnabled_impl(JNIEnv* env, jobject thiz,
                                               jstring soundId, jboolean enabled) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    auto* track = engine->getTrack(std::string(id));
    if (track) {
        track->setLimitEffectsEnabled(enabled == JNI_TRUE);
    }
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetLimitReverbEnabled_impl(JNIEnv* env, jobject thiz,
                                              jstring soundId, jboolean enabled) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    auto* track = engine->getTrack(std::string(id));
    if (track) {
        track->setLimitReverbEnabled(enabled == JNI_TRUE);
    }
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqEnabled_impl(JNIEnv* env, jobject thiz,
                                         jstring soundId, jboolean enabled, jboolean startAnalysis) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqEnabled(std::string(id), enabled == JNI_TRUE);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetTrackFilePath_impl(JNIEnv* env, jobject thiz,
                                         jstring soundId, jstring filePath) {
    // File path setting for potential future analysis - no-op for now
}

static jboolean nativeIsAutoEqEnabled_impl(JNIEnv* env, jobject thiz, jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    bool result = engine->isTrackAutoEqEnabled(std::string(id));
    env->ReleaseStringUTFChars(soundId, id);
    return result ? JNI_TRUE : JNI_FALSE;
}

static void nativeSetAutoEqTargetCurve_impl(JNIEnv* env, jobject thiz,
                                             jstring soundId, jstring targetType) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    const char* type = env->GetStringUTFChars(targetType, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqTargetCurve(std::string(id), std::string(type));
    env->ReleaseStringUTFChars(soundId, id);
    env->ReleaseStringUTFChars(targetType, type);
}

static void nativeSetAutoEqIntensity_impl(JNIEnv* env, jobject thiz,
                                           jstring soundId, jfloat intensity) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqIntensity(std::string(id), intensity);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqBassBias_impl(JNIEnv* env, jobject thiz,
                                          jstring soundId, jfloat bias) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqBassBias(std::string(id), bias);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqMidBias_impl(JNIEnv* env, jobject thiz,
                                         jstring soundId, jfloat bias) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqMidBias(std::string(id), bias);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqTrebleBias_impl(JNIEnv* env, jobject thiz,
                                            jstring soundId, jfloat bias) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqTrebleBias(std::string(id), bias);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqResponseSpeed_impl(JNIEnv* env, jobject thiz,
                                               jstring soundId, jstring speed) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    const char* s = env->GetStringUTFChars(speed, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqResponseSpeed(std::string(id), std::string(s));
    env->ReleaseStringUTFChars(soundId, id);
    env->ReleaseStringUTFChars(speed, s);
}

static void nativeSetAutoEqMaxBoost_impl(JNIEnv* env, jobject thiz,
                                          jstring soundId, jfloat db) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqMaxBoost(std::string(id), db);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqMaxCut_impl(JNIEnv* env, jobject thiz,
                                        jstring soundId, jfloat db) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqMaxCut(std::string(id), db);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqBrightnessTarget_impl(JNIEnv* env, jobject thiz,
                                                  jstring soundId, jfloat db) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqBrightnessTarget(std::string(id), db);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqLoudnessTarget_impl(JNIEnv* env, jobject thiz,
                                                jstring soundId, jfloat db) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqLoudnessTarget(std::string(id), db);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqDynamicQEnabled_impl(JNIEnv* env, jobject thiz,
                                                 jstring soundId, jboolean enabled) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqDynamicQEnabled(std::string(id), enabled == JNI_TRUE);
    env->ReleaseStringUTFChars(soundId, id);
}

static jfloatArray nativeGetAutoEqGains_impl(JNIEnv* env, jobject thiz, jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    auto gains = engine->getTrackAutoEqGains(std::string(id));
    env->ReleaseStringUTFChars(soundId, id);
    
    int count = static_cast<int>(gains.size());
    jfloatArray result = env->NewFloatArray(count);
    if (result != nullptr && count > 0) {
        env->SetFloatArrayRegion(result, 0, count, gains.data());
    }
    return result;
}

static jfloatArray nativeGetAutoEqFrequencies_impl(JNIEnv* env, jobject thiz, jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    auto freqs = engine->getTrackAutoEqFrequencies(std::string(id));
    env->ReleaseStringUTFChars(soundId, id);
    
    int count = static_cast<int>(freqs.size());
    jfloatArray result = env->NewFloatArray(count);
    if (result != nullptr && count > 0) {
        env->SetFloatArrayRegion(result, 0, count, freqs.data());
    }
    return result;
}

static void nativeSetAutoEqAttack_impl(JNIEnv* env, jobject thiz,
                                        jstring soundId, jfloat ms) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    auto* track = engine->getTrack(std::string(id));
    if (track) track->setAutoEqAttack(ms);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqRelease_impl(JNIEnv* env, jobject thiz,
                                         jstring soundId, jfloat ms) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    auto* track = engine->getTrack(std::string(id));
    if (track) track->setAutoEqRelease(ms);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqMaxSlope_impl(JNIEnv* env, jobject thiz,
                                          jstring soundId, jfloat slope) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    auto* track = engine->getTrack(std::string(id));
    if (track) track->setAutoEqMaxSlope(slope);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqCouplingCoeff_impl(JNIEnv* env, jobject thiz,
                                               jstring soundId, jfloat coeff) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    auto* track = engine->getTrack(std::string(id));
    if (track) track->setAutoEqCouplingCoeff(coeff);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqHysteresis_impl(JNIEnv* env, jobject thiz,
                                            jstring soundId, jfloat db) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    auto* track = engine->getTrack(std::string(id));
    if (track) track->setAutoEqHysteresis(db);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqBandCount_impl(JNIEnv* env, jobject thiz,
                                           jstring soundId, jint count) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqBandCount(std::string(id), count);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetAutoEqBandRatios_impl(JNIEnv* env, jobject thiz,
                                            jstring soundId, jfloat low, jfloat mid) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqBandRatios(std::string(id), low, mid);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeSetSpeakerPreset_impl(JNIEnv* env, jobject thiz,
                                         jstring soundId, jstring preset) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    const char* presetStr = env->GetStringUTFChars(preset, nullptr);
    auto* engine = AudioEngine::getInstance();
    auto* track = engine->getTrack(std::string(id));
    if (track) track->setSpeakerPreset(std::string(presetStr));
    env->ReleaseStringUTFChars(soundId, id);
    env->ReleaseStringUTFChars(preset, presetStr);
}

// Per-filter overrides (user-editable gain / frequency / Q)
static void nativeSetAutoEqFilterOverride_impl(JNIEnv* env, jobject thiz,
                                                jstring soundId, jint bandIndex,
                                                jfloat gainDb, jfloat freqHz, jfloat q) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->setTrackAutoEqFilterOverride(std::string(id), bandIndex, gainDb, freqHz, q);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeClearAutoEqFilterOverride_impl(JNIEnv* env, jobject thiz,
                                                  jstring soundId, jint bandIndex) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->clearTrackAutoEqFilterOverride(std::string(id), bandIndex);
    env->ReleaseStringUTFChars(soundId, id);
}

static void nativeClearAllAutoEqFilterOverrides_impl(JNIEnv* env, jobject thiz,
                                                      jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    engine->clearAllTrackAutoEqFilterOverrides(std::string(id));
    env->ReleaseStringUTFChars(soundId, id);
}

// HybridEq stubs (not yet implemented)
static jint nativeGetHybridEqProgress_impl(JNIEnv* env, jobject thiz, jstring soundId) {
    return 0;
}
static jboolean nativeIsHybridEqAnalyzing_impl(JNIEnv* env, jobject thiz, jstring soundId) {
    return JNI_FALSE;
}
static jboolean nativeHasHybridEqCurve_impl(JNIEnv* env, jobject thiz, jstring soundId) {
    return JNI_FALSE;
}

// ======================== 流式解码 JNI 绑定 ========================

static jboolean nativeCreateStream_impl(JNIEnv* env, jobject thiz,
                                         jstring streamId, jint bufferSize) {
    const char* id = env->GetStringUTFChars(streamId, nullptr);
    auto* engine = AudioEngine::getInstance();
    bool result = engine->createStream(std::string(id), static_cast<size_t>(bufferSize));
    env->ReleaseStringUTFChars(streamId, id);
    return result ? JNI_TRUE : JNI_FALSE;
}

static jint nativeLoadSoundFromStream_impl(JNIEnv* env, jobject thiz,
                                            jstring soundId, jstring streamId) {
    const char* trackId = env->GetStringUTFChars(soundId, nullptr);
    const char* sId = env->GetStringUTFChars(streamId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    int result = engine->loadTrackFromStream(std::string(trackId), std::string(sId));
    
    env->ReleaseStringUTFChars(soundId, trackId);
    env->ReleaseStringUTFChars(streamId, sId);
    return result;
}

static jint nativeWriteStreamData_impl(JNIEnv* env, jobject thiz,
                                        jstring streamId, jbyteArray data) {
    const char* id = env->GetStringUTFChars(streamId, nullptr);
    
    jbyte* dataPtr = env->GetByteArrayElements(data, nullptr);
    jsize len = env->GetArrayLength(data);
    
    auto* engine = AudioEngine::getInstance();
    size_t written = 0;
    engine->writeStreamData(std::string(id), reinterpret_cast<const uint8_t*>(dataPtr),
                            static_cast<size_t>(len), written);
    
    env->ReleaseByteArrayElements(data, dataPtr, JNI_ABORT);
    env->ReleaseStringUTFChars(streamId, id);
    return static_cast<jint>(written);
}

static void nativeSetStreamComplete_impl(JNIEnv* env, jobject thiz,
                                          jstring streamId) {
    const char* id = env->GetStringUTFChars(streamId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setStreamComplete(std::string(id));
    
    env->ReleaseStringUTFChars(streamId, id);
}

static void nativeDestroyStream_impl(JNIEnv* env, jobject thiz,
                                      jstring streamId) {
    const char* id = env->GetStringUTFChars(streamId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->destroyStream(std::string(id));
    
    env->ReleaseStringUTFChars(streamId, id);
}

static jboolean nativeHasStream_impl(JNIEnv* env, jobject thiz,
                                      jstring streamId) {
    const char* id = env->GetStringUTFChars(streamId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    bool result = engine->hasStream(std::string(id));
    
    env->ReleaseStringUTFChars(streamId, id);
    return result ? JNI_TRUE : JNI_FALSE;
}

static void nativeSetLimitSpatialEnabled_impl(JNIEnv* env, jobject thiz,
                                               jstring soundId, jboolean enabled) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    auto* engine = AudioEngine::getInstance();
    auto* track = engine->getTrack(std::string(id));
    if (track) {
        track->setLimitSpatialEnabled(enabled == JNI_TRUE);
    }
    env->ReleaseStringUTFChars(soundId, id);
}

static jfloatArray nativeGetVisualizationData_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    auto data = engine->getVisualizationData();
    
    jfloatArray result = env->NewFloatArray(16);
    if (result != nullptr) {
        env->SetFloatArrayRegion(result, 0, 16, data.data());
    }
    return result;
}

static jfloatArray nativeGetWhiteNoiseVisualizationData_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    auto data = engine->getWhiteNoiseVisualizationData();
    
    jfloatArray result = env->NewFloatArray(16);
    if (result != nullptr) {
        env->SetFloatArrayRegion(result, 0, 16, data.data());
    }
    return result;
}

static jfloatArray nativeGetMusicVisualizationData_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    auto data = engine->getMusicVisualizationData();
    
    jfloatArray result = env->NewFloatArray(16);
    if (result != nullptr) {
        env->SetFloatArrayRegion(result, 0, 16, data.data());
    }
    return result;
}

static jfloat nativeGetVisualizationEnergy_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    return engine->getVisualizationEnergy();
}

static jfloat nativeGetWhiteNoiseVisualizationEnergy_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    return engine->getWhiteNoiseVisualizationEnergy();
}

static jfloat nativeGetMusicVisualizationEnergy_impl(JNIEnv* env, jobject thiz) {
    auto* engine = AudioEngine::getInstance();
    return engine->getMusicVisualizationEnergy();
}

JNIEXPORT jboolean JNICALL
Java_com_bicy_whitenoise_audio_OboeAudioEngine_registerNatives(JNIEnv* env, jobject thiz) {
    jclass clazz = env->GetObjectClass(thiz);
    JNINativeMethod methods[] = {
        {"nativeInit", "()Z", (void*)nativeInit_impl},
        {"nativeRelease", "()V", (void*)nativeRelease_impl},
        {"nativeRecreateStream", "()Z", (void*)nativeRecreateStream_impl},
        {"nativeWarmup", "()V", (void*)nativeWarmup_impl},
        {"nativeLoadSound", "(Ljava/lang/String;Ljava/lang/String;)I", (void*)nativeLoadSound_impl},
        {"nativeLoadSoundFromFd", "(Ljava/lang/String;IJJLjava/lang/String;)I", (void*)nativeLoadSoundFromFd_impl},
        {"nativeUnloadSound", "(Ljava/lang/String;)V", (void*)nativeUnloadSound_impl},
        {"nativePlaySound", "(Ljava/lang/String;)V", (void*)nativePlaySound_impl},
        {"nativeStopSound", "(Ljava/lang/String;)V", (void*)nativeStopSound_impl},
        {"nativeStopAllSounds", "()V", (void*)nativeStopAllSounds_impl},
        {"nativePauseSound", "(Ljava/lang/String;)V", (void*)nativePauseSound_impl},
        {"nativeResumeSound", "(Ljava/lang/String;)V", (void*)nativeResumeSound_impl},
        {"nativeSetVolume", "(Ljava/lang/String;F)V", (void*)nativeSetVolume_impl},
        {"nativeGetVolume", "(Ljava/lang/String;)F", (void*)nativeGetVolume_impl},
        {"nativePauseAll", "()V", (void*)nativePauseAll_impl},
        {"nativeResumeAll", "()V", (void*)nativeResumeAll_impl},
        {"nativeIsPlaying", "(Ljava/lang/String;)Z", (void*)nativeIsPlaying_impl},
        {"nativeIsLoaded", "(Ljava/lang/String;)Z", (void*)nativeIsLoaded_impl},
        {"nativeIsLoading", "(Ljava/lang/String;)Z", (void*)nativeIsLoading_impl},
        {"nativeSetEffectEnabled", "(Ljava/lang/String;Z)V", (void*)nativeSetEffectEnabled_impl},
        {"nativeSetReverbParams", "(Ljava/lang/String;FFF)V", (void*)nativeSetReverbParams_impl},
        {"nativeSetInsulation", "(Ljava/lang/String;F)V", (void*)nativeSetInsulation_impl},
        {"nativeSetReverbDecayTime", "(Ljava/lang/String;F)V", (void*)nativeSetReverbDecayTime_impl},
        {"nativeSetReverbPreDelay", "(Ljava/lang/String;F)V", (void*)nativeSetReverbPreDelay_impl},
        {"nativeSetReverbDryLevel", "(Ljava/lang/String;F)V", (void*)nativeSetReverbDryLevel_impl},
        {"nativeSetReflectionDensity", "(Ljava/lang/String;F)V", (void*)nativeSetReflectionDensity_impl},
        {"nativeSetReflectionSpread", "(Ljava/lang/String;F)V", (void*)nativeSetReflectionSpread_impl},
        {"nativeSetHighpassCutoff", "(Ljava/lang/String;F)V", (void*)nativeSetHighpassCutoff_impl},
        {"nativeSetEarlyReflectionLevel", "(Ljava/lang/String;F)V", (void*)nativeSetEarlyReflectionLevel_impl},
        {"nativeSetCreativeEffectIntensity", "(Ljava/lang/String;IF)V", (void*)nativeSetCreativeEffectIntensity_impl},
        {"nativeSetPlaybackSpeed", "(Ljava/lang/String;F)V", (void*)nativeSetPlaybackSpeed_impl},
        {"nativeSetPitchShift", "(Ljava/lang/String;F)V", (void*)nativeSetPitchShift_impl},
        {"nativeSeekTo", "(Ljava/lang/String;J)V", (void*)nativeSeekTo_impl},
        {"nativeGetPosition", "(Ljava/lang/String;)J", (void*)nativeGetPosition_impl},
        {"nativeGetDuration", "(Ljava/lang/String;)J", (void*)nativeGetDuration_impl},
        {"nativeSetLooping", "(Ljava/lang/String;Z)V", (void*)nativeSetLooping_impl},
        {"nativeIsLooping", "(Ljava/lang/String;)Z", (void*)nativeIsLooping_impl},
        {"nativeNeedsRestart", "()Z", (void*)nativeNeedsRestart_impl},
        {"nativeClearRestartFlag", "()V", (void*)nativeClearRestartFlag_impl},
        {"nativeGetXRunCount", "()I", (void*)nativeGetXRunCount_impl},
        {"nativeHasUnderrun", "()Z", (void*)nativeHasUnderrun_impl},
        {"nativeClearUnderrunFlag", "()V", (void*)nativeClearUnderrunFlag_impl},
        {"nativeSetEqualizerCurve", "(Ljava/lang/String;[F[F[I[F[I[I)V", (void*)nativeSetEqualizerCurve_impl},
        {"nativeGetFilterResponse", "(Ljava/lang/String;F)F", (void*)nativeGetFilterResponse_impl},
        {"nativeSetEqBandGain", "(Ljava/lang/String;IF)V", (void*)nativeSetEqBandGain_impl},
        {"nativeGetEqBandGain", "(Ljava/lang/String;I)F", (void*)nativeGetEqBandGain_impl},
        {"nativeSetEqEnabled", "(Ljava/lang/String;Z)V", (void*)nativeSetEqEnabled_impl},
        {"nativeSetEqLimiterEnabled", "(Ljava/lang/String;Z)V", (void*)nativeSetEqLimiterEnabled_impl},
        {"nativeSetEqGains", "(Ljava/lang/String;[F)V", (void*)nativeSetEqGains_impl},
        {"nativeGetEqGains", "(Ljava/lang/String;)[F", (void*)nativeGetEqGains_impl},
        {"nativeSetSpatialEnabled", "(Ljava/lang/String;Z)V", (void*)nativeSetSpatialEnabled_impl},
        {"nativeSetSpatialIntensity", "(Ljava/lang/String;F)V", (void*)nativeSetSpatialIntensity_impl},
        {"nativeSetSpatialOffsetType", "(Ljava/lang/String;I)V", (void*)nativeSetSpatialOffsetType_impl},
        {"nativeSetSpatialFixedOffset", "(Ljava/lang/String;FFFF)V", (void*)nativeSetSpatialFixedOffset_impl},
        {"nativeSetSpatialSurroundParams", "(Ljava/lang/String;IFF)V", (void*)nativeSetSpatialSurroundParams_impl},
        {"nativeSetSpatialRandomParams", "(Ljava/lang/String;FFFF)V", (void*)nativeSetSpatialRandomParams_impl},
        {"nativeSetSpatialScatterParams", "(Ljava/lang/String;FFZZZZFFF)V", (void*)nativeSetSpatialScatterParams_impl},
        {"nativeSetFadeDuration", "(Ljava/lang/String;F)V", (void*)nativeSetFadeDuration_impl},
        {"nativeIsFadingOut", "(Ljava/lang/String;)Z", (void*)nativeIsFadingOut_impl},
        {"nativeCancelFadeOut", "(Ljava/lang/String;)V", (void*)nativeCancelFadeOut_impl},
        {"nativeClearAllEffectBuffers", "()V", (void*)nativeClearAllEffectBuffers_impl},
        {"nativeSetEffectOrder", "(Ljava/lang/String;[I)V", (void*)nativeSetEffectOrder_impl},
        {"nativeGetVisualizationData", "()[F", (void*)nativeGetVisualizationData_impl},
        {"nativeGetWhiteNoiseVisualizationData", "()[F", (void*)nativeGetWhiteNoiseVisualizationData_impl},
        {"nativeGetMusicVisualizationData", "()[F", (void*)nativeGetMusicVisualizationData_impl},
        {"nativeGetVisualizationEnergy", "()F", (void*)nativeGetVisualizationEnergy_impl},
        {"nativeGetWhiteNoiseVisualizationEnergy", "()F", (void*)nativeGetWhiteNoiseVisualizationEnergy_impl},
        {"nativeGetMusicVisualizationEnergy", "()F", (void*)nativeGetMusicVisualizationEnergy_impl},
        {"nativeSetGlobalLimiterConfig", "(ZZZZZFFF)V", (void*)nativeSetGlobalLimiterConfig_impl},
        {"nativeGetGlobalLimiterConfig", "()[Z", (void*)nativeGetGlobalLimiterConfig_impl},
        {"nativeSetGlobalLimiterEnabled", "(Z)V", (void*)nativeSetGlobalLimiterEnabled_impl},
        {"nativeIsGlobalLimiterEnabled", "()Z", (void*)nativeIsGlobalLimiterEnabled_impl},
        {"nativeSetLimitEffectsEnabled", "(Ljava/lang/String;Z)V", (void*)nativeSetLimitEffectsEnabled_impl},
        {"nativeSetLimitReverbEnabled", "(Ljava/lang/String;Z)V", (void*)nativeSetLimitReverbEnabled_impl},
        {"nativeSetLimitSpatialEnabled", "(Ljava/lang/String;Z)V", (void*)nativeSetLimitSpatialEnabled_impl},
        {"nativeSetAutoEqEnabled", "(Ljava/lang/String;ZZ)V", (void*)nativeSetAutoEqEnabled_impl},
        {"nativeSetTrackFilePath", "(Ljava/lang/String;Ljava/lang/String;)V", (void*)nativeSetTrackFilePath_impl},
        {"nativeIsAutoEqEnabled", "(Ljava/lang/String;)Z", (void*)nativeIsAutoEqEnabled_impl},
        {"nativeSetAutoEqTargetCurve", "(Ljava/lang/String;Ljava/lang/String;)V", (void*)nativeSetAutoEqTargetCurve_impl},
        {"nativeSetAutoEqIntensity", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqIntensity_impl},
        {"nativeSetAutoEqBassBias", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqBassBias_impl},
        {"nativeSetAutoEqMidBias", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqMidBias_impl},
        {"nativeSetAutoEqTrebleBias", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqTrebleBias_impl},
        {"nativeSetAutoEqResponseSpeed", "(Ljava/lang/String;Ljava/lang/String;)V", (void*)nativeSetAutoEqResponseSpeed_impl},
        {"nativeSetAutoEqMaxBoost", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqMaxBoost_impl},
        {"nativeSetAutoEqMaxCut", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqMaxCut_impl},
        {"nativeSetAutoEqBrightnessTarget", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqBrightnessTarget_impl},
        {"nativeSetAutoEqLoudnessTarget", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqLoudnessTarget_impl},
        {"nativeSetAutoEqDynamicQEnabled", "(Ljava/lang/String;Z)V", (void*)nativeSetAutoEqDynamicQEnabled_impl},
        {"nativeGetAutoEqGains", "(Ljava/lang/String;)[F", (void*)nativeGetAutoEqGains_impl},
        {"nativeGetAutoEqFrequencies", "(Ljava/lang/String;)[F", (void*)nativeGetAutoEqFrequencies_impl},
        {"nativeSetAutoEqAttack", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqAttack_impl},
        {"nativeSetAutoEqRelease", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqRelease_impl},
        {"nativeSetAutoEqMaxSlope", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqMaxSlope_impl},
        {"nativeSetAutoEqCouplingCoeff", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqCouplingCoeff_impl},
        {"nativeSetAutoEqHysteresis", "(Ljava/lang/String;F)V", (void*)nativeSetAutoEqHysteresis_impl},
        {"nativeSetAutoEqBandCount", "(Ljava/lang/String;I)V", (void*)nativeSetAutoEqBandCount_impl},
        {"nativeSetAutoEqBandRatios", "(Ljava/lang/String;FF)V", (void*)nativeSetAutoEqBandRatios_impl},
        {"nativeSetSpeakerPreset", "(Ljava/lang/String;Ljava/lang/String;)V", (void*)nativeSetSpeakerPreset_impl},
        {"nativeSetAutoEqFilterOverride", "(Ljava/lang/String;IFFF)V", (void*)nativeSetAutoEqFilterOverride_impl},
        {"nativeClearAutoEqFilterOverride", "(Ljava/lang/String;I)V", (void*)nativeClearAutoEqFilterOverride_impl},
        {"nativeClearAllAutoEqFilterOverrides", "(Ljava/lang/String;)V", (void*)nativeClearAllAutoEqFilterOverrides_impl},
        {"nativeGetHybridEqProgress", "(Ljava/lang/String;)I", (void*)nativeGetHybridEqProgress_impl},
        {"nativeIsHybridEqAnalyzing", "(Ljava/lang/String;)Z", (void*)nativeIsHybridEqAnalyzing_impl},
        {"nativeHasHybridEqCurve", "(Ljava/lang/String;)Z", (void*)nativeHasHybridEqCurve_impl},
        {"nativeCreateStream", "(Ljava/lang/String;I)Z", (void*)nativeCreateStream_impl},
        {"nativeLoadSoundFromStream", "(Ljava/lang/String;Ljava/lang/String;)I", (void*)nativeLoadSoundFromStream_impl},
        {"nativeWriteStreamData", "(Ljava/lang/String;[B)I", (void*)nativeWriteStreamData_impl},
        {"nativeSetStreamComplete", "(Ljava/lang/String;)V", (void*)nativeSetStreamComplete_impl},
        {"nativeDestroyStream", "(Ljava/lang/String;)V", (void*)nativeDestroyStream_impl},
        {"nativeHasStream", "(Ljava/lang/String;)Z", (void*)nativeHasStream_impl},
    };
    jint result = env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
    LOGI("RegisterNatives result: %d (0=OK)", result);
    return result == JNI_OK ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
