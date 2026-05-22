package com.bicy.whitenoise.H3HO

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
