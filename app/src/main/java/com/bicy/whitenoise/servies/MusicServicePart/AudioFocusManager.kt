package com.bicy.whitenoise.servies.MusicServicePart

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

class AudioFocusManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioFocusManager"
    }
    
    private var audioFocusRequest: AudioFocusRequest? = null
    var hasAudioFocus = false
        private set
    
    var wasPlayingBeforeFocusLoss = false
        private set
    
    // 白噪音是否允许与其他应用共存（降低音量而不是暂停）
    private var _allowDucking = true
    val allowDucking: Boolean get() = _allowDucking
    
    private var isDucking = false
    private var originalVolumes = mutableMapOf<String, Float>()
    
    val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(TAG, "Audio focus changed: $focusChange, wasPlayingBeforeFocusLoss=$wasPlayingBeforeFocusLoss, allowDucking=$allowDucking")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.w(TAG, "AUDIOFOCUS_LOSS - Permanent focus loss")
                wasPlayingBeforeFocusLoss = true
                hasAudioFocus = false
                if (allowDucking) {
                    // 降低音量而不是暂停
                    Log.d(TAG, "Ducking instead of pausing due to permanent focus loss")
                    duckAudio()
                } else {
                    onAudioFocusLoss?.invoke(true)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.w(TAG, "AUDIOFOCUS_LOSS_TRANSIENT - Temporary focus loss")
                wasPlayingBeforeFocusLoss = true
                if (allowDucking) {
                    // 降低音量而不是暂停
                    Log.d(TAG, "Ducking instead of pausing due to transient focus loss")
                    duckAudio()
                } else {
                    onAudioFocusLoss?.invoke(false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK - Ducking allowed")
                duckAudio()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.i(TAG, "AUDIOFOCUS_GAIN - Focus regained, wasPlayingBeforeFocusLoss=$wasPlayingBeforeFocusLoss")
                hasAudioFocus = true
                unduckAudio()
                if (wasPlayingBeforeFocusLoss) {
                    Log.i(TAG, "Calling onAudioFocusGain callback")
                    wasPlayingBeforeFocusLoss = false
                    onAudioFocusGain?.invoke()
                } else {
                    Log.d(TAG, "Not calling onAudioFocusGain - wasPlayingBeforeFocusLoss is false")
                }
            }
        }
    }
    
    private fun duckAudio() {
        if (!isDucking) {
            isDucking = true
            Log.d(TAG, "Ducking audio to 20% volume")
            onAudioFocusDuck?.invoke()
        }
    }
    
    private fun unduckAudio() {
        if (isDucking) {
            isDucking = false
            Log.d(TAG, "Restoring audio to original volume")
            onAudioFocusUnduck?.invoke()
        }
    }
    
    var onAudioFocusLoss: ((Boolean) -> Unit)? = null
    var onAudioFocusGain: (() -> Unit)? = null
    var onAudioFocusDuck: (() -> Unit)? = null
    var onAudioFocusUnduck: (() -> Unit)? = null
    
    fun requestAudioFocus(): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            // 对于白噪音应用，使用 AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            // 允许与其他应用共存，只需降低音量
            val focusType = if (allowDucking) {
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            } else {
                AudioManager.AUDIOFOCUS_GAIN
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(focusType)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .setWillPauseWhenDucked(false) // 不在duck时暂停
                    .build()
                
                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                Log.d(TAG, "Audio focus request result: $result, hasAudioFocus=$hasAudioFocus, focusType=$focusType")
                hasAudioFocus
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    focusType
                )
                hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                Log.d(TAG, "Audio focus request result (legacy): $result, hasAudioFocus=$hasAudioFocus, focusType=$focusType")
                hasAudioFocus
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求音频焦点失败: ${e.message}")
            false
        }
    }
    
    fun abandonAudioFocus() {
        try {
            unduckAudio()
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
            hasAudioFocus = false
        } catch (e: Exception) {
            Log.e(TAG, "放弃音频焦点失败: ${e.message}")
        }
    }
    
    fun resetFocusLossState() {
        wasPlayingBeforeFocusLoss = false
    }
    
    fun setAllowDucking(allow: Boolean) {
        _allowDucking = allow
        Log.d(TAG, "Allow ducking set to: $allow")
    }
}
