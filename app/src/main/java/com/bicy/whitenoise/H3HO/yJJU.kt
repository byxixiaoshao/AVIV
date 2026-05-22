package com.bicy.whitenoise.H3HO

object NativeAudio {

    init {
        System.loadLibrary("whitenoise")
    }

    fun init() {
        nativeInit()
    }

    fun release() {
        nativeRelease()
    }

    fun openFile(filePath: String): Boolean {
        return nativeOpenFile(filePath)
    }

    fun closeFile() {
        nativeCloseFile()
    }

    fun getSampleRate(): Int {
        return nativeGetSampleRate()
    }

    fun getChannels(): Int {
        return nativeGetChannels()
    }

    fun getDuration(): Long {
        return nativeGetDuration()
    }

    fun decodeAll(): FloatArray? {
        return nativeDecodeAll()
    }

    fun decodeChunk(maxSamples: Int): FloatArray? {
        return nativeDecodeChunk(maxSamples)
    }

    fun seekTo(positionMs: Long): Boolean {
        return nativeSeekTo(positionMs)
    }

    fun setOutputFormat(sampleRate: Int, channels: Int) {
        nativeSetOutputFormat(sampleRate, channels)
    }

    fun initReverb(sampleRate: Int) {
        nativeInitReverb(sampleRate)
    }

    fun setReverbRoomSize(value: Float) {
        nativeSetReverbRoomSize(value)
    }

    fun setReverbDamping(value: Float) {
        nativeSetReverbDamping(value)
    }

    fun setReverbWetLevel(value: Float) {
        nativeSetReverbWetLevel(value)
    }

    fun setReverbDryLevel(value: Float) {
        nativeSetReverbDryLevel(value)
    }

    fun setReverbWidth(value: Float) {
        nativeSetReverbWidth(value)
    }

    fun setReverbPreDelay(delayMs: Float) {
        nativeSetReverbPreDelay(delayMs)
    }

    fun setReverbConfig(
        roomSize: Float,
        damping: Float,
        wetLevel: Float,
        dryLevel: Float,
        width: Float,
        preDelay: Float
    ) {
        nativeSetReverbConfig(roomSize, damping, wetLevel, dryLevel, width, preDelay)
    }

    fun processReverb(samples: FloatArray, channels: Int): FloatArray? {
        return nativeProcessReverb(samples, channels)
    }

    fun clearReverb() {
        nativeClearReverb()
    }

    fun resetReverb() {
        nativeResetReverb()
    }

    private external fun nativeInit()
    private external fun nativeRelease()
    private external fun nativeOpenFile(filePath: String): Boolean
    private external fun nativeCloseFile()
    private external fun nativeGetSampleRate(): Int
    private external fun nativeGetChannels(): Int
    private external fun nativeGetDuration(): Long
    private external fun nativeDecodeAll(): FloatArray?
    private external fun nativeDecodeChunk(maxSamples: Int): FloatArray?
    private external fun nativeSeekTo(positionMs: Long): Boolean
    private external fun nativeSetOutputFormat(sampleRate: Int, channels: Int)
    private external fun nativeInitReverb(sampleRate: Int)
    private external fun nativeSetReverbRoomSize(value: Float)
    private external fun nativeSetReverbDamping(value: Float)
    private external fun nativeSetReverbWetLevel(value: Float)
    private external fun nativeSetReverbDryLevel(value: Float)
    private external fun nativeSetReverbWidth(value: Float)
    private external fun nativeSetReverbPreDelay(delayMs: Float)
    private external fun nativeSetReverbConfig(
        roomSize: Float,
        damping: Float,
        wetLevel: Float,
        dryLevel: Float,
        width: Float,
        preDelay: Float
    )
    private external fun nativeProcessReverb(samples: FloatArray, channels: Int): FloatArray?
    private external fun nativeClearReverb()
    private external fun nativeResetReverb()
}
