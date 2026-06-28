package com.bicy.whitenoise.audio

object BandPassFilter {
    
    init {
        System.loadLibrary("whitenoise")
    }
    
    external fun applyFilter(
        audioData: ByteArray,
        centerFreq: Float,
        Q: Float,
        sampleRate: Int
    ): ByteArray
}
