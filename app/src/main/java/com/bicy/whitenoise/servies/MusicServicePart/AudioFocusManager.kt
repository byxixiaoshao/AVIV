package com.bicy.whitenoise.servies.MusicServicePart

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.bicy.whitenoise.service.AnomalyType
import com.bicy.whitenoise.service.MemoryLockService

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
                // 永久性焦点丢失（如其他音乐播放器、电话占用）
                // 关键：不释放音频流，仅降低音量（ducking），保留 Oboe 引擎与已加载的音轨
                // 这样恢复焦点后可立即恢复播放，避免触发"音频流断开"重启
                Log.w(TAG, "AUDIOFOCUS_LOSS - Permanent focus loss, ducking instead of releasing stream")
                wasPlayingBeforeFocusLoss = true
                hasAudioFocus = false
                if (allowDucking) {
                    duckAudio()
                } else {
                    onAudioFocusLoss?.invoke(true)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 短暂焦点丢失（如来电、通知音）
                // 仍然不释放音频流，使用 ducking 降低音量到 30%
                Log.w(TAG, "AUDIOFOCUS_LOSS_TRANSIENT - Temporary focus loss, ducking instead of releasing stream")
                wasPlayingBeforeFocusLoss = true
                if (allowDucking) {
                    duckAudio()
                } else {
                    onAudioFocusLoss?.invoke(false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // 系统明确允许 ducking（如导航语音、提示音）
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
            // 30% 音量：保证背景音可被其他应用覆盖，但白噪音仍轻微可闻
            // 之前为 20%，提高到 30% 更符合 Android ducking 规范
            Log.d(TAG, "Ducking audio to 30% volume")
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

            // P0-1: 始终使用 AUDIOFOCUS_GAIN 而非 TRANSIENT_MAY_DUCK
            // 原因：白噪音是长时间持续播放的媒体类型，应作为"主媒体"持有焦点
            //   - AUDIOFOCUS_GAIN 表明我们是长期播放，系统会让我们保留音频流
            //   - TRANSIENT_MAY_DUCK 表明我们是短暂播放，系统更可能在其他应用请求焦点时
            //     主动断开我们的音频流，导致 AUDIO_ENGINE_RESTART 重启
            // 配合 onAudioFocusChangeListener 中的 ducking 策略，仍可与其他应用混音
            val focusType = AudioManager.AUDIOFOCUS_GAIN

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(focusType)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .setWillPauseWhenDucked(false) // 不在duck时暂停，仅降低音量
                    .setAcceptsDelayedFocusGain(false) // 不接受延迟焦点，避免长时间静默
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
            MemoryLockService.reportAnomaly(AnomalyType.AUDIO_FOCUS_ERROR, "请求音频焦点失败", e.stackTraceToString())
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
            MemoryLockService.reportAnomaly(AnomalyType.AUDIO_FOCUS_ERROR, "放弃音频焦点失败", e.stackTraceToString())
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
