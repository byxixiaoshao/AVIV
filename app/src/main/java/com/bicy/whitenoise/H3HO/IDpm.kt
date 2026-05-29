package com.bicy.whitenoise.H3HO

object OboeAudioEngine {
    
    init {
        System.loadLibrary("oboe")
        System.loadLibrary("whitenoise")
        registerNatives()
    }
    
    fun init(): Boolean {
        return nativeInit()
    }
    
    fun warmup() {
        nativeWarmup()
    }
    
    fun release() {
        nativeRelease()
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
    
    fun setAutoEqSmoothing(soundId: String, s: Float) {
        nativeSetAutoEqSmoothing(soundId, s)
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
    
    fun setSpatialSurroundParams(soundId: String, mode: Int, radius: Float, speed: Float) {
        nativeSetSpatialSurroundParams(soundId, mode, radius, speed)
    }
    
    fun setSpatialRandomParams(soundId: String, maxDistance: Float, minDistance: Float, randomValue: Float, speed: Float) {
        nativeSetSpatialRandomParams(soundId, maxDistance, minDistance, randomValue, speed)
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
    private external fun nativeSeekTo(soundId: String, positionMs: Long)
    private external fun nativeGetPosition(soundId: String): Long
    private external fun nativeGetDuration(soundId: String): Long
    private external fun nativeSetLooping(soundId: String, looping: Boolean)
    private external fun nativeIsLooping(soundId: String): Boolean
    private external fun nativeNeedsRestart(): Boolean
    private external fun nativeClearRestartFlag()
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
    private external fun nativeSetAutoEqSmoothing(soundId: String, s: Float)
    private external fun nativeSetAutoEqBrightnessTarget(soundId: String, db: Float)
    private external fun nativeSetAutoEqLoudnessTarget(soundId: String, db: Float)
    private external fun nativeSetAutoEqDynamicQEnabled(soundId: String, enabled: Boolean)
    private external fun nativeGetHybridEqProgress(soundId: String): Int
    private external fun nativeIsHybridEqAnalyzing(soundId: String): Boolean
    private external fun nativeHasHybridEqCurve(soundId: String): Boolean
    private external fun nativeGetAutoEqGains(soundId: String): FloatArray
    private external fun nativeSetSpatialEnabled(soundId: String, enabled: Boolean)
    private external fun nativeSetSpatialIntensity(soundId: String, intensity: Float)
    private external fun nativeSetSpatialOffsetType(soundId: String, type: Int)
    private external fun nativeSetSpatialFixedOffset(soundId: String, leftRight: Float, upDown: Float, frontBack: Float, multiplier: Float)
    private external fun nativeSetSpatialSurroundParams(soundId: String, mode: Int, radius: Float, speed: Float)
    private external fun nativeSetSpatialRandomParams(soundId: String, maxDistance: Float, minDistance: Float, randomValue: Float, speed: Float)
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
}
