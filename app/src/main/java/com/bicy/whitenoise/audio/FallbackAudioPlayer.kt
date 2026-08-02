package com.bicy.whitenoise.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.bicy.whitenoise.service.AnomalyType
import com.bicy.whitenoise.service.MemoryLockService
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * P2-8: 降级播放策略
 *
 * 当 Oboe 引擎持续失败（连续多次 recreateStream + 全量 release+init 都无法恢复）时，
 * 降级到基于 Android MediaPlayer 的兜底播放，保证音频不中断。
 *
 * 注意：
 * - 此降级路径不包含混音、效果（混响/EQ/空间音频）等高级功能
 * - 仅保证基本的文件播放、循环、音量控制
 * - 当 Oboe 恢复后应切回主路径（[switchBackToOboe]）
 *
 * 使用 [useFallbackAudioTrack] 标志位在 MusicService 中控制切换。
 */
object FallbackAudioPlayer {

    private const val TAG = "FallbackAudioPlayer"

    /** 全局开关：是否使用降级播放（true 时 MusicService 走兜底路径） */
    @Volatile
    var useFallbackAudioTrack: Boolean = false
        private set

    private var appContext: Context? = null

    /** soundId -> MediaPlayer 实例 */
    private val players = ConcurrentHashMap<String, MediaPlayer>()

    /** soundId -> 原始音量（0..1），用于 ducking */
    private val volumes = ConcurrentHashMap<String, Float>()

    fun init(context: Context) {
        appContext = context.applicationContext
        Log.i(TAG, "FallbackAudioPlayer initialized (context=$appContext)")
    }

    /** 启用降级播放模式 */
    fun enableFallback() {
        if (useFallbackAudioTrack) return
        useFallbackAudioTrack = true
        Log.w(TAG, "降级播放模式已启用（Oboe 持续失败，切换到 MediaPlayer 兜底）")
        MemoryLockService.reportAnomaly(
            AnomalyType.AUDIO_PLAYER_ERROR,
            "Oboe 持续失败，降级到 MediaPlayer 兜底播放"
        )
    }

    /** 禁用降级播放模式（Oboe 已恢复） */
    fun disableFallback() {
        if (!useFallbackAudioTrack) return
        Log.i(TAG, "降级播放模式已禁用，切回 Oboe 主路径")
        useFallbackAudioTrack = false
    }

    /**
     * 切回 Oboe：停止所有 MediaPlayer 并清理，由 MusicService 重新通过 Oboe 加载
     * 调用方应在 Oboe init 成功后调用此方法
     */
    fun switchBackToOboe() {
        Log.i(TAG, "switchBackToOboe: stopping all fallback players")
        stopAll()
        disableFallback()
    }

    fun loadSound(soundId: String, filePath: String): Boolean {
        val context = appContext ?: run {
            Log.e(TAG, "loadSound: appContext is null, call init() first")
            return false
        }
        if (!File(filePath).exists()) {
            Log.e(TAG, "loadSound: file not found: $filePath")
            return false
        }
        return try {
            // 已存在则先释放
            players[soundId]?.let { releasePlayer(soundId) }

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(filePath)
                isLooping = true  // 白噪音默认循环
                setOnPreparedListener { player ->
                    Log.d(TAG, "MediaPlayer prepared: $soundId")
                    // 应用已设置的音量（默认 1.0）
                    val v = volumes[soundId] ?: 1.0f
                    player.setVolume(v, v)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: soundId=$soundId, what=$what, extra=$extra")
                    MemoryLockService.reportAnomaly(
                        AnomalyType.AUDIO_PLAYER_ERROR,
                        "降级 MediaPlayer 错误: soundId=$soundId, what=$what, extra=$extra"
                    )
                    releasePlayer(soundId)
                    true
                }
                prepareAsync()
            }
            players[soundId] = mp
            Log.i(TAG, "Fallback MediaPlayer loaded: $soundId -> $filePath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "loadSound failed: $soundId", e)
            false
        }
    }

    fun playSound(soundId: String) {
        val mp = players[soundId] ?: run {
            Log.w(TAG, "playSound: not loaded, $soundId")
            return
        }
        try {
            if (!mp.isPlaying) {
                mp.start()
                Log.d(TAG, "Fallback play: $soundId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "playSound failed: $soundId", e)
        }
    }

    fun pauseSound(soundId: String) {
        val mp = players[soundId] ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                Log.d(TAG, "Fallback pause: $soundId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "pauseSound failed: $soundId", e)
        }
    }

    fun stopSound(soundId: String) {
        val mp = players[soundId] ?: return
        try {
            if (mp.isPlaying) mp.stop()
            Log.d(TAG, "Fallback stop: $soundId")
        } catch (e: Exception) {
            Log.e(TAG, "stopSound failed: $soundId", e)
        }
    }

    fun unloadSound(soundId: String) {
        releasePlayer(soundId)
        volumes.remove(soundId)
    }

    fun setVolume(soundId: String, volume: Float) {
        volumes[soundId] = volume
        val mp = players[soundId] ?: return
        try {
            mp.setVolume(volume, volume)
        } catch (e: Exception) {
            Log.e(TAG, "setVolume failed: $soundId", e)
        }
    }

    fun getVolume(soundId: String): Float {
        return volumes[soundId] ?: 1.0f
    }

    fun setLooping(soundId: String, looping: Boolean) {
        val mp = players[soundId] ?: return
        try {
            mp.isLooping = looping
        } catch (e: Exception) {
            Log.e(TAG, "setLooping failed: $soundId", e)
        }
    }

    fun isPlaying(soundId: String): Boolean {
        val mp = players[soundId] ?: return false
        return try {
            mp.isPlaying
        } catch (e: Exception) {
            false
        }
    }

    fun isLoaded(soundId: String): Boolean {
        return players.containsKey(soundId)
    }

    fun pauseAll() {
        players.forEach { (soundId, mp) ->
            try {
                if (mp.isPlaying) mp.pause()
            } catch (e: Exception) {
                Log.e(TAG, "pauseAll failed: $soundId", e)
            }
        }
        Log.d(TAG, "Fallback pauseAll: ${players.size} players")
    }

    fun resumeAll() {
        players.forEach { (soundId, mp) ->
            try {
                if (!mp.isPlaying) mp.start()
            } catch (e: Exception) {
                Log.e(TAG, "resumeAll failed: $soundId", e)
            }
        }
        Log.d(TAG, "Fallback resumeAll: ${players.size} players")
    }

    fun stopAll() {
        players.keys.toList().forEach { soundId ->
            releasePlayer(soundId)
        }
        volumes.clear()
        Log.d(TAG, "Fallback stopAll: all players released")
    }

    /** 应用 ducking（降低音量到 30%） */
    fun duckAll() {
        players.forEach { (soundId, mp) ->
            try {
                val v = volumes[soundId] ?: 1.0f
                mp.setVolume(v * 0.3f, v * 0.3f)
            } catch (e: Exception) {
                Log.e(TAG, "duckAll failed: $soundId", e)
            }
        }
        Log.d(TAG, "Fallback duckAll: ${players.size} players")
    }

    /** 取消 ducking（恢复原始音量） */
    fun unduckAll() {
        players.forEach { (soundId, mp) ->
            try {
                val v = volumes[soundId] ?: 1.0f
                mp.setVolume(v, v)
            } catch (e: Exception) {
                Log.e(TAG, "unduckAll failed: $soundId", e)
            }
        }
        Log.d(TAG, "Fallback unduckAll: ${players.size} players")
    }

    private fun releasePlayer(soundId: String) {
        players.remove(soundId)?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
            } catch (e: Exception) {
                // stop 可能抛出 IllegalStateException（未 prepared），忽略
            }
            try {
                mp.release()
            } catch (e: Exception) {
                Log.e(TAG, "releasePlayer release failed: $soundId", e)
            }
        }
    }

    fun release() {
        stopAll()
        appContext = null
        useFallbackAudioTrack = false
        Log.i(TAG, "FallbackAudioPlayer released")
    }
}
