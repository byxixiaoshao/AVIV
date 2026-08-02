package com.bicy.whitenoise.audio.AudioEditorPart

object AudioEditorUtils {
    
    init {
        System.loadLibrary("whitenoise")
    }
    
    external fun applyADSR(
        audioData: ByteArray,
        attack: Float,
        decay: Float,
        sustain: Float,
        release: Float,
        duration: Float,
        sampleRate: Int
    ): ByteArray
}
