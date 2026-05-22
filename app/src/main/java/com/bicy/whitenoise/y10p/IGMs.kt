package com.bicy.whitenoise.y10p

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import java.lang.ref.WeakReference

object RingtoneManager {
    private const val TAG = "RingtoneManager"
    private const val RINGTONE_FILE = "Beep.wav"
    
    private var contextRef: WeakReference<Context>? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var handler: Handler? = null
    private var isPlaying = false
    private var vibrationRunnable: Runnable? = null
    
    fun init(context: Context) {
        contextRef = WeakReference(context.applicationContext)
        this.vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        this.handler = Handler(Looper.getMainLooper())
    }
    
    fun startRingtone() {
        if (isPlaying) return
        
        isPlaying = true
        playRingtone()
        startVibration()
        Log.d(TAG, "Ringtone started")
    }
    
    fun stopRingtone() {
        isPlaying = false
        stopMediaPlayer()
        stopVibration()
        Log.d(TAG, "Ringtone stopped")
    }
    
    private fun playRingtone() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                
                contextRef?.get()?.let { ctx ->
                    val afd: AssetFileDescriptor = ctx.assets.openFd(RINGTONE_FILE)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }
                
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ringtone", e)
        }
    }
    
    private fun stopMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
    
    private fun startVibration() {
        vibrationRunnable = object : Runnable {
            override fun run() {
                if (!isPlaying) return
                
                performVibrationPattern()
                
                handler?.postDelayed(this, 1300)
            }
        }
        handler?.post(vibrationRunnable!!)
    }
    
    @Suppress("DEPRECATION")
    private fun performVibrationPattern() {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(
                    20, 130, 50, 130
                )
                val amplitudes = intArrayOf(
                    0, 255, 0, 255
                )
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                v.vibrate(longArrayOf(20, 130, 50, 130), -1)
            }
        }
    }
    
    private fun stopVibration() {
        vibrationRunnable?.let {
            handler?.removeCallbacks(it)
            vibrationRunnable = null
        }
        vibrator?.cancel()
    }
    
    fun isRingtonePlaying(): Boolean = isPlaying
}
