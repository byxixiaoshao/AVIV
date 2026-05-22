package com.bicy.whitenoise.H3HO

class PeriodicPlayer(private val playerHandle: Long) {
    
    companion object {
        init {
            System.loadLibrary("whitenoise")
        }
        
        external fun create(audioData: ByteArray): Long
    }
    
    external fun createPeriodicAudio(
        repeatCount: Int,
        repeatFrequencyHz: Int,
        sampleRate: Int
    ): ByteArray
    
    external fun createPeriodicAudioWithCrossfade(
        repeatCount: Int,
        repeatFrequencyHz: Int,
        sampleRate: Int,
        crossfadeMs: Int
    ): ByteArray
    
    external fun destroy()
    
    protected fun finalize() {
        destroy()
    }
}
