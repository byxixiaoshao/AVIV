package com.bicy.whitenoise.oJft

import android.content.Context
import android.os.Build
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.runtime.Stable
import com.bicy.whitenoise.xnef.MusicPlayerController
import com.bicy.whitenoise.rgRE.MusicService
import com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage
import com.bicy.whitenoise.y10p.RingtoneManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.Locale

enum class PauseType(val displayName: String) {
    ALL("全局"),
    MUSIC_ONLY("仅音乐"),
    WHITE_NOISE_ONLY("仅白噪音")
}

@Stable
data class TimerState(
    val isActive: Boolean = false,
    val remainingTime: Long = 0L,
    val hours: Int = 0,
    val minutes: Int = 0,
    val totalMinutes: Int = 0,
    val pauseType: PauseType = PauseType.ALL,
    val snoozeMinutes: Int = 5,
    val ringEnabled: Boolean = true,
    val isFinished: Boolean = false
)

object TimerManager {
    
    private const val TAG = "TimerManager"
    
    private var contextRef: WeakReference<Context>? = null
    
    private var hours = 0
    private var minutes = 0
    private var totalMinutes = 0
    private var pauseType = PauseType.ALL
    private var snoozeMinutes = 5
    private var ringEnabled = true
    
    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    private var lastVibrateTime = 0L
    
    fun init(context: Context) {
        contextRef = WeakReference(context.applicationContext)
        RingtoneManager.init(context)
    }
    
    fun setPauseType(type: PauseType) {
        pauseType = type
        updateState()
    }
    
    fun setSnoozeMinutes(minutes: Int) {
        snoozeMinutes = minutes.coerceIn(1, 60)
        updateState()
    }
    
    fun setRingEnabled(enabled: Boolean) {
        ringEnabled = enabled
        updateState()
    }
    
    fun adjustTime(deltaMinutes: Int) {
        totalMinutes = (totalMinutes + deltaMinutes).coerceAtLeast(0)
        hours = totalMinutes / 60
        minutes = totalMinutes % 60
        
        updateState()
        
        if (deltaMinutes != 0 && totalMinutes > 0) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastVibrateTime > 100) {
                vibrate(30)
                lastVibrateTime = currentTime
            }
        }
        
        if (totalMinutes == 0) {
            vibrateLimitReached()
        }
    }
    
    fun setTime(targetMinutes: Int) {
        totalMinutes = targetMinutes.coerceAtLeast(0)
        hours = totalMinutes / 60
        minutes = totalMinutes % 60
        
        updateState()
    }
    
    fun setTime(h: Int, m: Int) {
        hours = h.coerceAtLeast(0)
        minutes = m.coerceIn(0, 59)
        totalMinutes = hours * 60 + minutes
        
        updateState()
    }
    
    fun setHours(h: Int) {
        hours = h.coerceAtLeast(0)
        totalMinutes = hours * 60 + minutes
        updateState()
    }
    
    fun setMinutes(m: Int) {
        minutes = m.coerceIn(0, 59)
        totalMinutes = hours * 60 + minutes
        updateState()
    }
    
    private fun updateState() {
        _timerState.value = TimerState(
            isActive = isTimerRunning,
            remainingTime = _timerState.value.remainingTime,
            hours = hours,
            minutes = minutes,
            totalMinutes = totalMinutes,
            pauseType = pauseType,
            snoozeMinutes = snoozeMinutes,
            ringEnabled = ringEnabled,
            isFinished = _timerState.value.isFinished
        )
    }
    
    @Suppress("DEPRECATION")
    private fun vibrateLimitReached() {
        val vibrator = contextRef?.get()?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(50, 100, 200)
                val amplitudes = intArrayOf(255, 255, 0)
                it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(50, 100, 200), -1)
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun vibrate(duration: Long) {
        val vibrator = contextRef?.get()?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(duration)
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun vibrateTimerEnd() {
        val vibrator = contextRef?.get()?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 80, 60, 80, 60, 80)
                val amplitudes = intArrayOf(0, 80, 0, 200, 0, 140)
                it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 80, 60, 80, 60, 80), -1)
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun vibrateTimerStart() {
        val vibrator = contextRef?.get()?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 300, 100, 300, 100)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0)
                it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 300, 100, 300, 100), -1)
            }
        }
    }
    
    fun formatTimeDisplay(): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    }
    
    fun formatRemainingTime(): String {
        val totalSeconds = _timerState.value.remainingTime / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }
    
    fun startTimer() {
        if (totalMinutes == 0) return
        
        countDownTimer?.cancel()
        
        val totalMillis = totalMinutes * 60 * 1000L
        
        vibrateTimerStart()
        
        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timerState.value = _timerState.value.copy(remainingTime = millisUntilFinished)
            }
            
            override fun onFinish() {
                isTimerRunning = false
                vibrateTimerEnd()
                Log.d(TAG, "定时结束")
                
                when (pauseType) {
                    PauseType.ALL -> {
                        MusicService.getInstance()?.pauseAllSounds()
                        WhiteNoiseStorage.setPlaybackPaused(true)
                        if (MusicPlayerController.isPlaying) {
                            MusicPlayerController.pause()
                            Log.d(TAG, "音乐播放器已暂停")
                        }
                    }
                    PauseType.MUSIC_ONLY -> {
                        if (MusicPlayerController.isPlaying) {
                            MusicPlayerController.pause()
                            Log.d(TAG, "音乐播放器已暂停")
                        }
                    }
                    PauseType.WHITE_NOISE_ONLY -> {
                        MusicService.getInstance()?.pauseAllSounds()
                        WhiteNoiseStorage.setPlaybackPaused(true)
                        Log.d(TAG, "白噪音已暂停")
                    }
                }
                
                if (ringEnabled) {
                    RingtoneManager.startRingtone()
                    
                    _timerState.value = TimerState(
                        isActive = false,
                        remainingTime = 0L,
                        hours = hours,
                        minutes = minutes,
                        totalMinutes = totalMinutes,
                        pauseType = pauseType,
                        snoozeMinutes = snoozeMinutes,
                        ringEnabled = ringEnabled,
                        isFinished = true
                    )
                } else {
                    _timerState.value = TimerState(
                        isActive = false,
                        remainingTime = 0L,
                        hours = hours,
                        minutes = minutes,
                        totalMinutes = totalMinutes,
                        pauseType = pauseType,
                        snoozeMinutes = snoozeMinutes,
                        ringEnabled = ringEnabled,
                        isFinished = false
                    )
                }
            }
        }.start()
        
        isTimerRunning = true
        _timerState.value = _timerState.value.copy(
            isActive = true,
            remainingTime = totalMillis
        )
    }
    
    fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        isTimerRunning = false
        RingtoneManager.stopRingtone()
        _timerState.value = _timerState.value.copy(isActive = false, remainingTime = 0L, isFinished = false)
    }
    
    fun resetTimer() {
        stopTimer()
        totalMinutes = 0
        hours = 0
        minutes = 0
        _timerState.value = TimerState()
    }
    
    fun snooze() {
        RingtoneManager.stopRingtone()
        _timerState.value = _timerState.value.copy(isFinished = false)
        totalMinutes = snoozeMinutes
        hours = snoozeMinutes / 60
        minutes = snoozeMinutes % 60
        startTimer()
    }
    
    fun dismissTimer() {
        RingtoneManager.stopRingtone()
        _timerState.value = _timerState.value.copy(isFinished = false)
    }
    
    fun isRunning(): Boolean = isTimerRunning
    
    fun getTotalMinutes(): Int = totalMinutes
    
    fun getHours(): Int = hours
    
    fun getMinutes(): Int = minutes
}
