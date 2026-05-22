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
                                                                      jlong offset, jlong length) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    int result = engine->loadTrackFromFd(std::string(id), fd, offset, length);
    
    env->ReleaseStringUTFChars(soundId, id);
    
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

static void nativeSetEqGains_impl(JNIEnv* env, jobject thiz,
                                                                 jstring soundId, jfloatArray gains) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    jfloat* gainsPtr = env->GetFloatArrayElements(gains, nullptr);
    jsize length = env->GetArrayLength(gains);
    
    std::array<float, EQ_BAND_COUNT> gainsArray = {};
    for (jsize i = 0; i < length && i < EQ_BAND_COUNT; i++) {
        gainsArray[i] = gainsPtr[i];
    }
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackEqGains(std::string(id), gainsArray);
    
    env->ReleaseFloatArrayElements(gains, gainsPtr, JNI_ABORT);
    env->ReleaseStringUTFChars(soundId, id);
}

static jfloatArray nativeGetEqGains_impl(JNIEnv* env, jobject thiz,
                                                                 jstring soundId) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    auto gains = engine->getTrackEqGains(std::string(id));
    
    jfloatArray result = env->NewFloatArray(EQ_BAND_COUNT);
    env->SetFloatArrayRegion(result, 0, EQ_BAND_COUNT, gains.data());
    
    env->ReleaseStringUTFChars(soundId, id);
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

static void nativeSetSpatialSurroundParams_impl(JNIEnv* env, jobject thiz,
                                                                                  jstring soundId,
                                                                                  jint mode, jfloat radius, jfloat speed) {
    const char* id = env->GetStringUTFChars(soundId, nullptr);
    
    auto* engine = AudioEngine::getInstance();
    engine->setTrackSpatialSurroundParams(std::string(id), mode, radius, speed);
    
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

JNIEXPORT jboolean JNICALL
Java_com_bicy_whitenoise_H3HO_OboeAudioEngine_registerNatives(JNIEnv* env, jobject thiz) {
    jclass clazz = env->GetObjectClass(thiz);
    JNINativeMethod methods[] = {
        {"nativeInit", "()Z", (void*)nativeInit_impl},
        {"nativeRelease", "()V", (void*)nativeRelease_impl},
        {"nativeWarmup", "()V", (void*)nativeWarmup_impl},
        {"nativeLoadSound", "(Ljava/lang/String;Ljava/lang/String;)I", (void*)nativeLoadSound_impl},
        {"nativeLoadSoundFromFd", "(Ljava/lang/String;IJJ)I", (void*)nativeLoadSoundFromFd_impl},
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
        {"nativeSeekTo", "(Ljava/lang/String;J)V", (void*)nativeSeekTo_impl},
        {"nativeGetPosition", "(Ljava/lang/String;)J", (void*)nativeGetPosition_impl},
        {"nativeGetDuration", "(Ljava/lang/String;)J", (void*)nativeGetDuration_impl},
        {"nativeSetLooping", "(Ljava/lang/String;Z)V", (void*)nativeSetLooping_impl},
        {"nativeIsLooping", "(Ljava/lang/String;)Z", (void*)nativeIsLooping_impl},
        {"nativeNeedsRestart", "()Z", (void*)nativeNeedsRestart_impl},
        {"nativeClearRestartFlag", "()V", (void*)nativeClearRestartFlag_impl},
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
        {"nativeSetFadeDuration", "(Ljava/lang/String;F)V", (void*)nativeSetFadeDuration_impl},
        {"nativeIsFadingOut", "(Ljava/lang/String;)Z", (void*)nativeIsFadingOut_impl},
        {"nativeCancelFadeOut", "(Ljava/lang/String;)V", (void*)nativeCancelFadeOut_impl},
        {"nativeClearAllEffectBuffers", "()V", (void*)nativeClearAllEffectBuffers_impl},
        {"nativeSetEffectOrder", "(Ljava/lang/String;[I)V", (void*)nativeSetEffectOrder_impl},
    };
    jint result = env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
    LOGI("RegisterNatives result: %d (0=OK)", result);
    return result == JNI_OK ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
