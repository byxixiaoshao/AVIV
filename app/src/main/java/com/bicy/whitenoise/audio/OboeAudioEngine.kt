package com.bicy.whitenoise.audio

import com.bicy.whitenoise.service.AnomalyType
import com.bicy.whitenoise.service.MemoryLockService

object OboeAudioEngine {
    
    init {
        System.loadLibrary("oboe")
        System.loadLibrary("whitenoise")
        registerNatives()
    }
    
    fun init(): Boolean {
        return try {
            nativeInit()
        } catch (e: Exception) {
            MemoryLockService.reportAnomaly(AnomalyType.AUDIO_PLAYER_ERROR, "Oboe引擎初始化失败", e.stackTraceToString())
            false
        }
    }
    
    fun warmup() {
        nativeWarmup()
    }
    
    fun release() {
        nativeRelease()
    }

    // P2-7: 轻量重建音频流（保留已加载的 tracks）
    // 用于 onErrorAfterClose 后的优雅恢复，避免全量 release+init 重新加载文件
    fun recreateStream(): Boolean {
        return nativeRecreateStream()
    }
    
    fun loadSound(soundId: String, filePath: String): Int {
        return nativeLoadSound(soundId, filePath)
    }
    
    fun loadSoundFromFd(soundId: String, fd: Int, offset: Long = 0, length: Long = -1, filePath: String = ""): Int {
        return nativeLoadSoundFromFd(soundId, fd, offset, length, filePath)
    }
    
    fun unloadSound(soundId: String) {
        nativeUnloadSound(soundId)
    }
    
    fun playSound(soundId: String) {
        nativePlaySound(soundId)
    }
    
    fun stopSound(soundId: String) {
        nativeStopSound(soundId)
    }
    
    fun stopAllSounds() {
        nativeStopAllSounds()
    }
    
    fun pauseSound(soundId: String) {
        nativePauseSound(soundId)
    }
    
    fun resumeSound(soundId: String) {
        nativeResumeSound(soundId)
    }
    
    fun setVolume(soundId: String, volume: Float) {
        nativeSetVolume(soundId, volume)
    }
    
    fun getVolume(soundId: String): Float {
        return nativeGetVolume(soundId)
    }
    
    fun pauseAll() {
        nativePauseAll()
    }
    
    fun resumeAll() {
        nativeResumeAll()
    }
    
    fun isPlaying(soundId: String): Boolean {
        return nativeIsPlaying(soundId)
    }
    
    fun isLoaded(soundId: String): Boolean {
        return nativeIsLoaded(soundId)
    }
    
    fun isLoading(soundId: String): Boolean {
        return nativeIsLoading(soundId)
    }
    
    fun setEffectEnabled(soundId: String, enabled: Boolean) {
        nativeSetEffectEnabled(soundId, enabled)
    }
    
    fun setReverbParams(soundId: String, roomSize: Float, damping: Float, wetLevel: Float) {
        nativeSetReverbParams(soundId, roomSize, damping, wetLevel)
    }
    
    fun setInsulation(soundId: String, insulation: Float) {
        nativeSetInsulation(soundId, insulation)
    }
    
    fun setReverbDecayTime(soundId: String, decayTime: Float) {
        nativeSetReverbDecayTime(soundId, decayTime)
    }
    
    fun setReverbPreDelay(soundId: String, preDelay: Float) {
        nativeSetReverbPreDelay(soundId, preDelay)
    }
    
    fun setReverbDryLevel(soundId: String, dryLevel: Float) {
        nativeSetReverbDryLevel(soundId, dryLevel)
    }
    
    fun setReflectionDensity(soundId: String, density: Float) {
        nativeSetReflectionDensity(soundId, density)
    }
    
    fun setReflectionSpread(soundId: String, spread: Float) {
        nativeSetReflectionSpread(soundId, spread)
    }
    
    fun setHighpassCutoff(soundId: String, cutoff: Float) {
        nativeSetHighpassCutoff(soundId, cutoff)
    }
    
    fun setEarlyReflectionLevel(soundId: String, level: Float) {
        nativeSetEarlyReflectionLevel(soundId, level)
    }
    
    fun setCreativeEffectIntensity(soundId: String, effectType: Int, intensity: Float) {
        nativeSetCreativeEffectIntensity(soundId, effectType, intensity)
    }

    /**
     * 设置播放速率（独立于音调），由 SoundTouch 实时 time-stretch 实现。
     * @param speed 1.0=原速，0.5=半速，2.0=倍速（范围 0.25~4.0）
     */
    fun setPlaybackSpeed(soundId: String, speed: Float) {
        nativeSetPlaybackSpeed(soundId, speed)
    }

    /**
     * 设置音调偏移（独立于速率），由 SoundTouch 实时 pitch-shift 实现。
     * @param semitones 半音数，0=原调，+12=高八度，-12=低八度（范围 -24~+24）
     */
    fun setPitchShift(soundId: String, semitones: Float) {
        nativeSetPitchShift(soundId, semitones)
    }
    
    fun seekTo(soundId: String, positionMs: Long) {
        nativeSeekTo(soundId, positionMs)
    }
    
    fun getPosition(soundId: String): Long {
        return nativeGetPosition(soundId)
    }
    
    fun getDuration(soundId: String): Long {
        return nativeGetDuration(soundId)
    }
    
    fun setLooping(soundId: String, looping: Boolean) {
        nativeSetLooping(soundId, looping)
    }
    
    fun isLooping(soundId: String): Boolean {
        return nativeIsLooping(soundId)
    }
    
    fun needsRestart(): Boolean {
        return nativeNeedsRestart()
    }
    
    fun clearRestartFlag() {
        nativeClearRestartFlag()
    }
    
    fun getXRunCount(): Int {
        return nativeGetXRunCount()
    }
    
    fun hasUnderrun(): Boolean {
        return nativeHasUnderrun()
    }
    
    fun clearUnderrunFlag() {
        nativeClearUnderrunFlag()
    }
    
    fun setEqualizerCurve(soundId: String, frequencies: FloatArray, gains: FloatArray, filterTypes: IntArray, qValues: FloatArray, curveIns: IntArray, curveOuts: IntArray) {
        nativeSetEqualizerCurve(soundId, frequencies, gains, filterTypes, qValues, curveIns, curveOuts)
    }
    
    fun getFilterResponse(soundId: String, frequency: Float): Float {
        return nativeGetFilterResponse(soundId, frequency)
    }
    
    fun setEqBandGain(soundId: String, bandIndex: Int, gain: Float) {
        nativeSetEqBandGain(soundId, bandIndex, gain)
    }
    
    fun getEqBandGain(soundId: String, bandIndex: Int): Float {
        return nativeGetEqBandGain(soundId, bandIndex)
    }
    
    fun setEqEnabled(soundId: String, enabled: Boolean) {
        nativeSetEqEnabled(soundId, enabled)
    }
    
    fun setEqLimiterEnabled(soundId: String, enabled: Boolean) {
        nativeSetEqLimiterEnabled(soundId, enabled)
    }
    
    fun setEqGains(soundId: String, gains: FloatArray) {
        nativeSetEqGains(soundId, gains)
    }
    
    fun getEqGains(soundId: String): FloatArray {
        return nativeGetEqGains(soundId)
    }
    
    fun setAutoEqEnabled(soundId: String, enabled: Boolean, filePath: String = "") {
        if (filePath.isNotEmpty()) {
            nativeSetTrackFilePath(soundId, filePath)
        }
        nativeSetAutoEqEnabled(soundId, enabled, true)
    }
    
    fun setAutoEqModeEnabled(soundId: String, enabled: Boolean) {
        nativeSetAutoEqEnabled(soundId, enabled, false)
    }
    
    private external fun nativeSetTrackFilePath(soundId: String, filePath: String)
    
    fun isAutoEqEnabled(soundId: String): Boolean {
        return nativeIsAutoEqEnabled(soundId)
    }
    
    fun setAutoEqTargetCurve(soundId: String, targetType: String) {
        nativeSetAutoEqTargetCurve(soundId, targetType)
    }
    
    fun setAutoEqIntensity(soundId: String, intensity: Float) {
        nativeSetAutoEqIntensity(soundId, intensity)
    }
    
    fun setAutoEqBassBias(soundId: String, bias: Float) {
        nativeSetAutoEqBassBias(soundId, bias)
    }
    
    fun setAutoEqMidBias(soundId: String, bias: Float) {
        nativeSetAutoEqMidBias(soundId, bias)
    }
    
    fun setAutoEqTrebleBias(soundId: String, bias: Float) {
        nativeSetAutoEqTrebleBias(soundId, bias)
    }
    
    fun setAutoEqResponseSpeed(soundId: String, speed: String) {
        nativeSetAutoEqResponseSpeed(soundId, speed)
    }
    
    fun setAutoEqMaxBoost(soundId: String, db: Float) {
        nativeSetAutoEqMaxBoost(soundId, db)
    }
    
    fun setAutoEqMaxCut(soundId: String, db: Float) {
        nativeSetAutoEqMaxCut(soundId, db)
    }
    
    fun setAutoEqBrightnessTarget(soundId: String, db: Float) {
        nativeSetAutoEqBrightnessTarget(soundId, db)
    }
    
    fun setAutoEqLoudnessTarget(soundId: String, db: Float) {
        nativeSetAutoEqLoudnessTarget(soundId, db)
    }
    
    fun setAutoEqDynamicQEnabled(soundId: String, enabled: Boolean) {
        nativeSetAutoEqDynamicQEnabled(soundId, enabled)
    }
    
    fun setAutoEqAttack(soundId: String, attackMs: Float) {
        nativeSetAutoEqAttack(soundId, attackMs)
    }
    
    fun setAutoEqRelease(soundId: String, releaseMs: Float) {
        nativeSetAutoEqRelease(soundId, releaseMs)
    }
    
    fun setAutoEqMaxSlope(soundId: String, slope: Float) {
        nativeSetAutoEqMaxSlope(soundId, slope)
    }
    
    fun setAutoEqCouplingCoeff(soundId: String, coeff: Float) {
        nativeSetAutoEqCouplingCoeff(soundId, coeff)
    }
    
    fun setAutoEqHysteresis(soundId: String, db: Float) {
        nativeSetAutoEqHysteresis(soundId, db)
    }
    
    fun setAutoEqBandCount(soundId: String, count: Int) {
        nativeSetAutoEqBandCount(soundId, count)
    }
    
    fun setAutoEqBandRatios(soundId: String, low: Float, mid: Float) {
        nativeSetAutoEqBandRatios(soundId, low, mid)
    }

    fun setSpeakerPreset(soundId: String, preset: String) {
        nativeSetSpeakerPreset(soundId, preset)
    }

    // Per-filter overrides (user-editable gain / frequency / Q for each AutoEQ band)
    fun setAutoEqFilterOverride(soundId: String, bandIndex: Int, gainDb: Float, freqHz: Float, q: Float) {
        nativeSetAutoEqFilterOverride(soundId, bandIndex, gainDb, freqHz, q)
    }

    fun clearAutoEqFilterOverride(soundId: String, bandIndex: Int) {
        nativeClearAutoEqFilterOverride(soundId, bandIndex)
    }

    fun clearAllAutoEqFilterOverrides(soundId: String) {
        nativeClearAllAutoEqFilterOverrides(soundId)
    }
    
    fun getHybridEqProgress(soundId: String): Int {
        return nativeGetHybridEqProgress(soundId)
    }
    
    fun isHybridEqAnalyzing(soundId: String): Boolean {
        return nativeIsHybridEqAnalyzing(soundId)
    }
    
    fun hasHybridEqCurve(soundId: String): Boolean {
        return nativeHasHybridEqCurve(soundId)
    }
    
    fun getAutoEqGains(soundId: String): FloatArray {
        return nativeGetAutoEqGains(soundId)
    }
    
    fun getAutoEqFrequencies(soundId: String): FloatArray {
        return nativeGetAutoEqFrequencies(soundId)
    }
    
    fun setSpatialEnabled(soundId: String, enabled: Boolean) {
        nativeSetSpatialEnabled(soundId, enabled)
    }
    
    fun setSpatialIntensity(soundId: String, intensity: Float) {
        nativeSetSpatialIntensity(soundId, intensity)
    }
    
    fun setSpatialOffsetType(soundId: String, type: Int) {
        nativeSetSpatialOffsetType(soundId, type)
    }
    
    fun setSpatialFixedOffset(soundId: String, leftRight: Float, upDown: Float, frontBack: Float, multiplier: Float) {
        nativeSetSpatialFixedOffset(soundId, leftRight, upDown, frontBack, multiplier)
    }
    
    fun setSpatialSurroundParams(soundId: String, mode: Int, radius: Float, periodSeconds: Float) {
        nativeSetSpatialSurroundParams(soundId, mode, radius, periodSeconds)
    }
    
    fun setSpatialRandomParams(soundId: String, maxDistance: Float, minDistance: Float, randomValue: Float, speed: Float) {
        nativeSetSpatialRandomParams(soundId, maxDistance, minDistance, randomValue, speed)
    }
    
    fun setSpatialScatterParams(
        soundId: String,
        minRadius: Float, maxRadius: Float,
        xEnabled: Boolean, yEnabled: Boolean, zEnabled: Boolean,
        moveEnabled: Boolean, moveRandomValue: Float, moveSpeed: Float, directionRandom: Float
    ) {
        nativeSetSpatialScatterParams(
            soundId, minRadius, maxRadius,
            xEnabled, yEnabled, zEnabled,
            moveEnabled, moveRandomValue, moveSpeed, directionRandom
        )
    }
    
    fun setFadeDuration(soundId: String, durationSeconds: Float) {
        nativeSetFadeDuration(soundId, durationSeconds)
    }
    
    fun isFadingOut(soundId: String): Boolean {
        return nativeIsFadingOut(soundId)
    }
    
    fun cancelFadeOut(soundId: String) {
        nativeCancelFadeOut(soundId)
    }
    
    fun clearAllEffectBuffers() {
        nativeClearAllEffectBuffers()
    }
    
    fun setEffectOrder(soundId: String, order: IntArray) {
        nativeSetEffectOrder(soundId, order)
    }
    
    fun getVisualizationData(): FloatArray {
        return nativeGetVisualizationData()
    }
    
    fun getWhiteNoiseVisualizationData(): FloatArray {
        return nativeGetWhiteNoiseVisualizationData()
    }
    
    fun getMusicVisualizationData(): FloatArray {
        return nativeGetMusicVisualizationData()
    }
    
    fun getVisualizationEnergy(): Float {
        return nativeGetVisualizationEnergy()
    }
    
    fun getWhiteNoiseVisualizationEnergy(): Float {
        return nativeGetWhiteNoiseVisualizationEnergy()
    }
    
    fun getMusicVisualizationEnergy(): Float {
        return nativeGetMusicVisualizationEnergy()
    }
    
    fun setGlobalLimiterConfig(
        enabled: Boolean,
        limitEqualizer: Boolean,
        limitEffects: Boolean,
        limitReverb: Boolean,
        limitSpatial: Boolean,
        threshold: Float = 0.9f,
        attack: Float = 5.0f,
        release: Float = 50.0f
    ) {
        nativeSetGlobalLimiterConfig(
            enabled, limitEqualizer, limitEffects, limitReverb,
            limitSpatial, threshold, attack, release
        )
    }
    
    fun getGlobalLimiterConfig(): BooleanArray {
        return nativeGetGlobalLimiterConfig()
    }
    
    fun setGlobalLimiterEnabled(enabled: Boolean) {
        nativeSetGlobalLimiterEnabled(enabled)
    }
    
    fun isGlobalLimiterEnabled(): Boolean {
        return nativeIsGlobalLimiterEnabled()
    }
    
    private external fun registerNatives(): Boolean
    private external fun nativeInit(): Boolean
    private external fun nativeWarmup()
    private external fun nativeRelease()
    private external fun nativeRecreateStream(): Boolean
    private external fun nativeLoadSound(soundId: String, filePath: String): Int
    private external fun nativeLoadSoundFromFd(soundId: String, fd: Int, offset: Long, length: Long, filePath: String): Int
    private external fun nativeUnloadSound(soundId: String)
    private external fun nativePlaySound(soundId: String)
    private external fun nativeStopSound(soundId: String)
    private external fun nativeStopAllSounds()
    private external fun nativePauseSound(soundId: String)
    private external fun nativeResumeSound(soundId: String)
    private external fun nativeSetVolume(soundId: String, volume: Float)
    private external fun nativeGetVolume(soundId: String): Float
    private external fun nativePauseAll()
    private external fun nativeResumeAll()
    private external fun nativeIsPlaying(soundId: String): Boolean
    private external fun nativeIsLoaded(soundId: String): Boolean
    private external fun nativeIsLoading(soundId: String): Boolean
    private external fun nativeSetEffectEnabled(soundId: String, enabled: Boolean)
    private external fun nativeSetReverbParams(soundId: String, roomSize: Float, damping: Float, wetLevel: Float)
    private external fun nativeSetInsulation(soundId: String, insulation: Float)
    private external fun nativeSetReverbDecayTime(soundId: String, decayTime: Float)
    private external fun nativeSetReverbPreDelay(soundId: String, preDelay: Float)
    private external fun nativeSetReverbDryLevel(soundId: String, dryLevel: Float)
    private external fun nativeSetReflectionDensity(soundId: String, density: Float)
    private external fun nativeSetReflectionSpread(soundId: String, spread: Float)
    private external fun nativeSetHighpassCutoff(soundId: String, cutoff: Float)
    private external fun nativeSetEarlyReflectionLevel(soundId: String, level: Float)
    private external fun nativeSetCreativeEffectIntensity(soundId: String, effectType: Int, intensity: Float)
    private external fun nativeSetPlaybackSpeed(soundId: String, speed: Float)
    private external fun nativeSetPitchShift(soundId: String, semitones: Float)
    private external fun nativeSeekTo(soundId: String, positionMs: Long)
    private external fun nativeGetPosition(soundId: String): Long
    private external fun nativeGetDuration(soundId: String): Long
    private external fun nativeSetLooping(soundId: String, looping: Boolean)
    private external fun nativeIsLooping(soundId: String): Boolean
    private external fun nativeNeedsRestart(): Boolean
    private external fun nativeClearRestartFlag()
    private external fun nativeGetXRunCount(): Int
    private external fun nativeHasUnderrun(): Boolean
    private external fun nativeClearUnderrunFlag()
    private external fun nativeSetEqualizerCurve(soundId: String, frequencies: FloatArray, gains: FloatArray, filterTypes: IntArray, qValues: FloatArray, curveIns: IntArray, curveOuts: IntArray)
    private external fun nativeGetFilterResponse(soundId: String, frequency: Float): Float
    private external fun nativeSetEqBandGain(soundId: String, bandIndex: Int, gain: Float)
    private external fun nativeGetEqBandGain(soundId: String, bandIndex: Int): Float
    private external fun nativeSetEqEnabled(soundId: String, enabled: Boolean)
    private external fun nativeSetEqLimiterEnabled(soundId: String, enabled: Boolean)
    private external fun nativeSetEqGains(soundId: String, gains: FloatArray)
    private external fun nativeGetEqGains(soundId: String): FloatArray
    private external fun nativeSetAutoEqEnabled(soundId: String, enabled: Boolean, startAnalysis: Boolean)
    private external fun nativeIsAutoEqEnabled(soundId: String): Boolean
    private external fun nativeSetAutoEqTargetCurve(soundId: String, targetType: String)
    private external fun nativeSetAutoEqIntensity(soundId: String, intensity: Float)
    private external fun nativeSetAutoEqBassBias(soundId: String, bias: Float)
    private external fun nativeSetAutoEqMidBias(soundId: String, bias: Float)
    private external fun nativeSetAutoEqTrebleBias(soundId: String, bias: Float)
    private external fun nativeSetAutoEqResponseSpeed(soundId: String, speed: String)
    private external fun nativeSetAutoEqMaxBoost(soundId: String, db: Float)
    private external fun nativeSetAutoEqMaxCut(soundId: String, db: Float)
    private external fun nativeSetAutoEqBrightnessTarget(soundId: String, db: Float)
    private external fun nativeSetAutoEqLoudnessTarget(soundId: String, db: Float)
    private external fun nativeSetAutoEqDynamicQEnabled(soundId: String, enabled: Boolean)
    private external fun nativeSetAutoEqAttack(soundId: String, attackMs: Float)
    private external fun nativeSetAutoEqRelease(soundId: String, releaseMs: Float)
    private external fun nativeSetAutoEqMaxSlope(soundId: String, slope: Float)
    private external fun nativeSetAutoEqCouplingCoeff(soundId: String, coeff: Float)
    private external fun nativeSetAutoEqHysteresis(soundId: String, db: Float)
    private external fun nativeSetAutoEqBandCount(soundId: String, count: Int)
    private external fun nativeSetAutoEqBandRatios(soundId: String, low: Float, mid: Float)
    private external fun nativeSetSpeakerPreset(soundId: String, preset: String)
    private external fun nativeSetAutoEqFilterOverride(soundId: String, bandIndex: Int, gainDb: Float, freqHz: Float, q: Float)
    private external fun nativeClearAutoEqFilterOverride(soundId: String, bandIndex: Int)
    private external fun nativeClearAllAutoEqFilterOverrides(soundId: String)
    private external fun nativeGetHybridEqProgress(soundId: String): Int
    private external fun nativeIsHybridEqAnalyzing(soundId: String): Boolean
    private external fun nativeHasHybridEqCurve(soundId: String): Boolean
    private external fun nativeGetAutoEqGains(soundId: String): FloatArray
    private external fun nativeGetAutoEqFrequencies(soundId: String): FloatArray
    private external fun nativeSetSpatialEnabled(soundId: String, enabled: Boolean)
    private external fun nativeSetSpatialIntensity(soundId: String, intensity: Float)
    private external fun nativeSetSpatialOffsetType(soundId: String, type: Int)
    private external fun nativeSetSpatialFixedOffset(soundId: String, leftRight: Float, upDown: Float, frontBack: Float, multiplier: Float)
    private external fun nativeSetSpatialSurroundParams(soundId: String, mode: Int, radius: Float, periodSeconds: Float)
    private external fun nativeSetSpatialRandomParams(soundId: String, maxDistance: Float, minDistance: Float, randomValue: Float, speed: Float)
    private external fun nativeSetSpatialScatterParams(
        soundId: String,
        minRadius: Float, maxRadius: Float,
        xEnabled: Boolean, yEnabled: Boolean, zEnabled: Boolean,
        moveEnabled: Boolean, moveRandomValue: Float, moveSpeed: Float, directionRandom: Float
    )
    private external fun nativeSetFadeDuration(soundId: String, durationSeconds: Float)
    private external fun nativeIsFadingOut(soundId: String): Boolean
    private external fun nativeCancelFadeOut(soundId: String)
    private external fun nativeClearAllEffectBuffers()
    private external fun nativeSetEffectOrder(soundId: String, order: IntArray)
    private external fun nativeGetVisualizationData(): FloatArray
    private external fun nativeGetWhiteNoiseVisualizationData(): FloatArray
    private external fun nativeGetMusicVisualizationData(): FloatArray
    private external fun nativeGetVisualizationEnergy(): Float
    private external fun nativeGetWhiteNoiseVisualizationEnergy(): Float
    private external fun nativeGetMusicVisualizationEnergy(): Float
    private external fun nativeSetGlobalLimiterConfig(
        enabled: Boolean,
        limitEqualizer: Boolean,
        limitEffects: Boolean,
        limitReverb: Boolean,
        limitSpatial: Boolean,
        threshold: Float,
        attack: Float,
        release: Float
    )
    private external fun nativeGetGlobalLimiterConfig(): BooleanArray
    private external fun nativeSetGlobalLimiterEnabled(enabled: Boolean)
    private external fun nativeIsGlobalLimiterEnabled(): Boolean
    
    fun setLimitEffectsEnabled(soundId: String, enabled: Boolean) {
        nativeSetLimitEffectsEnabled(soundId, enabled)
    }
    
    fun setLimitReverbEnabled(soundId: String, enabled: Boolean) {
        nativeSetLimitReverbEnabled(soundId, enabled)
    }
    
    fun setLimitSpatialEnabled(soundId: String, enabled: Boolean) {
        nativeSetLimitSpatialEnabled(soundId, enabled)
    }
    
    private external fun nativeSetLimitEffectsEnabled(soundId: String, enabled: Boolean)
    private external fun nativeSetLimitReverbEnabled(soundId: String, enabled: Boolean)
    private external fun nativeSetLimitSpatialEnabled(soundId: String, enabled: Boolean)
    
    // ======================== 流式解码 ========================
    
    /** 创建流缓存 */
    fun createStream(streamId: String, bufferSize: Int = 0): Boolean {
        return nativeCreateStream(streamId, bufferSize)
    }
    
    /** 从流加载音频 */
    fun loadSoundFromStream(soundId: String, streamId: String): Int {
        return nativeLoadSoundFromStream(soundId, streamId)
    }
    
    /** 写入数据到流缓存 */
    fun writeStreamData(streamId: String, data: ByteArray): Int {
        return nativeWriteStreamData(streamId, data)
    }
    
    /** 标记流数据下载完成 */
    fun setStreamComplete(streamId: String) {
        nativeSetStreamComplete(streamId)
    }
    
    /** 销毁流缓存 */
    fun destroyStream(streamId: String) {
        nativeDestroyStream(streamId)
    }
    
    /** 检查流是否存在 */
    fun hasStream(streamId: String): Boolean {
        return nativeHasStream(streamId)
    }
    
    private external fun nativeCreateStream(streamId: String, bufferSize: Int): Boolean
    private external fun nativeLoadSoundFromStream(soundId: String, streamId: String): Int
    private external fun nativeWriteStreamData(streamId: String, data: ByteArray): Int
    private external fun nativeSetStreamComplete(streamId: String)
    private external fun nativeDestroyStream(streamId: String)
    private external fun nativeHasStream(streamId: String): Boolean
}
