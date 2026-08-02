package com.bicy.whitenoise.audio.AudioEditorPart

class AudioEditor(private val audioHandle: Long) {
    
    companion object {
        init {
            System.loadLibrary("whitenoise")
        }
        
        external fun loadAudio(audioPath: String): Long
    }
    
    external fun adjustPitch(pitchRatio: Float): ByteArray
    
    external fun adjustVolume(volumeRatio: Float): ByteArray
    
    external fun trimDuration(durationMs: Int): ByteArray
    
    external fun applyFadeIn(fadeInMs: Int): ByteArray
    
    external fun applyFadeOut(fadeOutMs: Int): ByteArray
    
    external fun destroy()
    
    protected fun finalize() {
        destroy()
    }
}
