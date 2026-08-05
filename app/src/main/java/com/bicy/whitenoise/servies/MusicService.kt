package com.bicy.whitenoise.servies

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.bicy.whitenoise.audio.OboeAudioEngine
import com.bicy.whitenoise.audio.PlaybackStateManager
import com.bicy.whitenoise.audio.ReverbConfig
import com.bicy.whitenoise.audio.ReverbManager
import com.bicy.whitenoise.service.AnomalyType
import com.bicy.whitenoise.service.MemoryLockService
import com.bicy.whitenoise.audio.ScatteredPlayerManagerPart.ScatteredPlayerManager
import com.bicy.whitenoise.servies.MusicServicePart.AudioFocusManager
import com.bicy.whitenoise.servies.MusicServicePart.MusicNotificationManager
import com.bicy.whitenoise.servies.MusicServicePart.mS7k
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.ScatteredAudioClipData
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SpatialScatterRangeData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MusicService : Service() {
    
    companion object {
        private const val TAG = "MusicService"
        private const val WAKE_LOCK_TAG = "MyApp:AudioPlaybackWakeLock"
        
        const val ACTION_PLAY_SOUND = "com.bicy.whitenoise.PLAY_SOUND"
        const val EXTRA_SOUND_ID = "sound_id"
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_SOUND_NAME = "sound_name"
        
        private var instance: MusicService? = null
        
        fun getInstance(): MusicService? = instance
        
        private var onPlaybackStateChangeListenerCallback: ((String, Boolean) -> Unit)? = null
        var onPlaybackStateChangeListener: ((String, Boolean) -> Unit)?
            get() = onPlaybackStateChangeListenerCallback
            set(value) {
                onPlaybackStateChangeListenerCallback = value
            }
        
        private var onServiceReadyListenerCallback: (() -> Unit)? = null
        var onServiceReadyListener: (() -> Unit)?
            get() = onServiceReadyListenerCallback
            set(value) {
                onServiceReadyListenerCallback = value
            }
        
        private var onAudioStreamRestartedCallback: (() -> Unit)? = null
        var onAudioStreamRestarted: (() -> Unit)?
            get() = onAudioStreamRestartedCallback
            set(value) {
                onAudioStreamRestartedCallback = value
            }
        
        private var onAudioStreamDisconnectCallback: (() -> Unit)? = null
        var onAudioStreamDisconnect: (() -> Unit)?
            get() = onAudioStreamDisconnectCallback
            set(value) {
                onAudioStreamDisconnectCallback = value
            }

        private var onAudioFocusLostCallback: (() -> Unit)? = null
        var onAudioFocusLost: (() -> Unit)?
            get() = onAudioFocusLostCallback
            set(value) {
                onAudioFocusLostCallback = value
            }
    }
    
    private val binder = MusicServiceBinder()
    private lateinit var audioFocusManager: AudioFocusManager
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val pendingPlayRequests = ConcurrentHashMap<String, Boolean>()
    private val loadRetryCount = ConcurrentHashMap<String, Int>()
    private val hasBeenPlayed = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioCheckThread = HandlerThread("AudioCheckThread").apply { start() }
    private val audioCheckHandler = Handler(audioCheckThread.looper)

    // P0-2: 音频流断开重启的容错状态
    // 连续失败计数与下次检查延迟，避免一次抖动就全量 release+init
    private var streamRestartFailureCount = 0
    private var nextStreamCheckDelayMs: Long = 1000L
    private val maxStreamRestartFailures = 4  // 连续 4 次失败才执行全量 release+init
    private val baseStreamCheckDelayMs: Long = 1000L  // 基础退避 1s
    private val maxStreamCheckDelayMs: Long = 8000L   // 最大退避 8s

    // P0: Oboe 引擎初始化状态跟踪
    // 解决"服务异常关闭后无法恢复"的核心漏洞：
    // 服务被 START_STICKY 重启时，若 Oboe init() 失败（设备占用/驱动异常），
    // 旧代码仅 Log，未降级到 FallbackAudioPlayer，且 audioStreamCheckRunnable
    // 检查 needsRestart() 时因引擎未初始化返回 false，形成死锁——服务"假活"但无音频。
    @Volatile
    private var isOboeInitialized = false
    private var oboeInitRetryCount = 0
    private val maxOboeInitRetries = 5  // 周期性重试 5 次，仍失败则保持降级

    // 音频缓冲区欠载（XRun）持续捕获状态
    // 旧逻辑依赖 hasUnderrun() 一次性标志，clearUnderrunFlag() 后即停止上报，
    // 无法捕获 XRun 持续增长（如减速卡顿导致的连续欠载，计数可达数百并持续增长）。
    // 新逻辑直接轮询 getXRunCount() 跟踪计数变化：
    //   计数增长  → 欠载持续中，节流上报（持续捕获）
    //   计数停止增长 / 被重置 → 异常结束，停止上报
    private var lastSeenXrunCount = 0
    private var underrunActive = false
    private var lastUnderrunReportTimeMs = 0L
    private val underrunReportIntervalMs = 3000L  // 持续期间最小上报间隔，防诊断日志文件爆炸
    
    private val loadCheckRunnable = object : Runnable {
        override fun run() {
            val pendingIds = pendingPlayRequests.keys().toList()
            for (soundId in pendingIds) {
                if (OboeAudioEngine.isLoaded(soundId)) {
                    pendingPlayRequests.remove(soundId)
                    loadRetryCount.remove(soundId)
                    startPlayback(soundId)
                } else if (!OboeAudioEngine.isLoading(soundId)) {
                    val retryCount = loadRetryCount[soundId] ?: 0
                    if (retryCount < 3) {
                        loadRetryCount[soundId] = retryCount + 1
                        val filePath = PlaybackStateManager.getLoadedSoundPath(soundId)
                        if (filePath != null) {
                            Log.d(TAG, "重试加载: $soundId, 第${retryCount + 1}次")
                            OboeAudioEngine.loadSound(soundId, filePath)
                        }
                    } else {
                        pendingPlayRequests.remove(soundId)
                        loadRetryCount.remove(soundId)
                        Log.e(TAG, "加载失败，已重试3次: $soundId")
                        MemoryLockService.reportAnomaly(AnomalyType.AUDIO_LOAD_TIMEOUT, "音频加载失败(重试3次): $soundId")
                    }
                }
            }
            if (pendingPlayRequests.isNotEmpty()) {
                mainHandler.postDelayed(this, 100)
            }
        }
    }
    
    private val audioStreamCheckRunnable = object : Runnable {
        override fun run() {
            // P0: Oboe 引擎健康检查——若 init() 失败导致服务降级，周期性重试恢复
            // 这是修复"服务异常关闭后无法恢复"的关键：避免引擎未初始化时死锁
            if (!isOboeInitialized) {
                if (oboeInitRetryCount < maxOboeInitRetries) {
                    oboeInitRetryCount++
                    Log.w(TAG, "audioStreamCheckRunnable: Oboe 未初始化，重试 $oboeInitRetryCount/$maxOboeInitRetries")
                    val retryOk = try {
                        OboeAudioEngine.release()
                        OboeAudioEngine.init()
                    } catch (e: Exception) {
                        Log.e(TAG, "audioStreamCheckRunnable: Oboe 重试初始化异常", e)
                        false
                    }
                    if (retryOk) {
                        Log.i(TAG, "audioStreamCheckRunnable: Oboe 重试初始化成功，切回主路径")
                        isOboeInitialized = true
                        oboeInitRetryCount = 0
                        com.bicy.whitenoise.audio.FallbackAudioPlayer.switchBackToOboe()
                        // 恢复后重新加载已保存的 sounds（用户播放列表）
                        restoreSavedSounds()
                    } else {
                        Log.w(TAG, "audioStreamCheckRunnable: Oboe 重试失败 ($oboeInitRetryCount/$maxOboeInitRetries)")
                    }
                }
                // 无论是否重试，降级状态下也保持轮询（等待设备空闲后重试成功）
                audioCheckHandler.postDelayed(this, 2000)
                return
            }

            // 检测音频缓冲区欠载（持续捕获）
            // 直接轮询 getXRunCount() 跟踪计数变化，替代旧的 hasUnderrun() 一次性标志：
            // 旧逻辑 clearUnderrunFlag() 后即停止上报，无法捕获 XRun 持续增长的情况。
            val currentXRun = OboeAudioEngine.getXRunCount()
            if (currentXRun < lastSeenXrunCount) {
                // 计数被重置（引擎重启/recreateStream，C++ openStream 中 xrunCount_ 归零）→ 之前的欠载已结束
                if (underrunActive) {
                    Log.i(TAG, "音频缓冲区欠载已结束（XRun 重置: $lastSeenXrunCount -> $currentXRun）")
                    underrunActive = false
                }
            } else if (currentXRun > lastSeenXrunCount) {
                // XRun 计数增长 → 欠载持续中
                val delta = currentXRun - lastSeenXrunCount
                if (!underrunActive) {
                    underrunActive = true
                    Log.w(TAG, "音频缓冲区欠载开始，XRun计数: $currentXRun (+$delta)")
                }
                // 节流上报：持续期间每 underrunReportIntervalMs 上报一次。
                // reportAnomaly 每次写一个诊断日志文件，无节流会导致日志爆炸；
                // 节流后既能"持续捕获"异常全程，又控制日志量。
                val now = SystemClock.elapsedRealtime()
                if (now - lastUnderrunReportTimeMs >= underrunReportIntervalMs) {
                    MemoryLockService.reportAnomaly(AnomalyType.AUDIO_BUFFER_UNDERRUN,
                        "音频缓冲区欠载持续中，XRun计数: $currentXRun (本次+$delta)")
                    lastUnderrunReportTimeMs = now
                }
            } else if (underrunActive) {
                // currentXRun == lastSeenXrunCount 且之前处于活跃 → 计数停止增长，异常结束
                Log.i(TAG, "音频缓冲区欠载已结束（XRun 稳定: $currentXRun）")
                underrunActive = false
            }
            lastSeenXrunCount = currentXRun

            if (OboeAudioEngine.needsRestart()) {
                Log.w(TAG, "检测到音频流需要重启 (failureCount=${streamRestartFailureCount + 1}/$maxStreamRestartFailures)")
                OboeAudioEngine.clearRestartFlag()

                streamRestartFailureCount++
                val attempt = streamRestartFailureCount

                if (attempt < maxStreamRestartFailures) {
                    // P0-2: 未达阈值前，先尝试轻量恢复——recreateStream（保留 tracks，不 release）
                    // 配合 P2-7 的 recreateStream，可在不断开音频流的情况下恢复播放
                    // 这样可避免一次抖动就触发全量 release，显著降低 AUDIO_ENGINE_RESTART 频率
                    Log.w(TAG, "audioStreamCheckRunnable: 轻量恢复尝试 $attempt/$maxStreamRestartFailures (recreateStream)")

                    // 重新请求音频焦点（焦点丢失可能导致流断开）
                    if (!audioFocusManager.hasAudioFocus) {
                        Log.d(TAG, "audioStreamCheckRunnable: 重新请求音频焦点")
                        audioFocusManager.requestAudioFocus()
                    }

                    // 尝试轻量重建音频流（C++ 侧 onErrorAfterClose 已尝试过一次，这里再重试）
                    val recreated = OboeAudioEngine.recreateStream()
                    Log.w(TAG, "audioStreamCheckRunnable: recreateStream result=$recreated")

                    if (recreated) {
                        // 恢复成功——重置计数，通知 UI 已恢复
                        Log.i(TAG, "audioStreamCheckRunnable: 轻量恢复成功，无需全量重启")
                        streamRestartFailureCount = 0
                        nextStreamCheckDelayMs = baseStreamCheckDelayMs
                        // 恢复成功后通知 callback（若有），不调用 onAudioStreamDisconnect
                        mainHandler.post { onAudioStreamRestarted?.invoke() }
                        audioCheckHandler.postDelayed(this, 500)
                        return
                    }

                    // 计算退避: 1s, 2s, 4s（指数退避，封顶 maxStreamCheckDelayMs）
                    nextStreamCheckDelayMs = (baseStreamCheckDelayMs shl (attempt - 1))
                        .coerceAtMost(maxStreamCheckDelayMs)
                    Log.d(TAG, "audioStreamCheckRunnable: 下次检查延迟 ${nextStreamCheckDelayMs}ms")

                    // 仅在最后一次轻量尝试时通知 UI（避免频繁弹出"音频流断开"提示）
                    if (attempt == maxStreamRestartFailures - 1) {
                        mainHandler.post { onAudioStreamDisconnect?.invoke() }
                    }

                    audioCheckHandler.postDelayed(this, nextStreamCheckDelayMs)
                    return
                }

                // 连续失败达阈值：执行全量 release + init
                Log.w(TAG, "audioStreamCheckRunnable: 连续 $attempt 次轻量恢复失败，执行全量重启")
                MemoryLockService.reportAnomaly(AnomalyType.AUDIO_ENGINE_RESTART,
                    "音频流断开(连续${attempt}次轻量恢复失败)，正在全量重启引擎")

                mainHandler.post { onAudioStreamDisconnect?.invoke() }

                OboeAudioEngine.clearAllEffectBuffers()

                val wasPlaying = PlaybackStateManager.getAllSoundIds().filter { OboeAudioEngine.isPlaying(it) }
                Log.w(TAG, "audioStreamCheckRunnable: wasPlaying=$wasPlaying")

                OboeAudioEngine.release()
                val reinitialized = OboeAudioEngine.init()
                Log.w(TAG, "音频引擎重新初始化: $reinitialized")

                if (reinitialized) {
                    // P0: 标记 Oboe 已初始化，重置健康检查重试计数
                    isOboeInitialized = true
                    oboeInitRetryCount = 0
                    // P2-8: Oboe 恢复成功——如果之前在降级模式，切回 Oboe
                    if (com.bicy.whitenoise.audio.FallbackAudioPlayer.useFallbackAudioTrack) {
                        Log.i(TAG, "audioStreamCheckRunnable: Oboe 恢复成功，切回主路径")
                        com.bicy.whitenoise.audio.FallbackAudioPlayer.switchBackToOboe()
                    }

                    // 重置失败计数与退避
                    streamRestartFailureCount = 0
                    nextStreamCheckDelayMs = baseStreamCheckDelayMs

                    if (wasPlaying.isNotEmpty()) {
                        Log.w(TAG, "audioStreamCheckRunnable: Loading ${wasPlaying.size} sounds")
                        for (soundId in wasPlaying) {
                            val filePath = PlaybackStateManager.getLoadedSoundPath(soundId)
                            Log.w(TAG, "audioStreamCheckRunnable: Loading sound $soundId from $filePath")
                            if (filePath != null) {
                                pendingPlayRequests[soundId] = true
                                loadRetryCount[soundId] = 0
                                OboeAudioEngine.loadSound(soundId, filePath)
                            }
                        }
                        mainHandler.post(loadCheckRunnable)
                    }

                    mainHandler.post {
                        Log.w(TAG, "audioStreamCheckRunnable: Calling onAudioStreamRestarted callback, callback is ${if (onAudioStreamRestarted != null) "set" else "null"}")
                        onAudioStreamRestarted?.invoke()
                    }
                } else {
                    // P2-8: Oboe 全量重启也失败——降级到 MediaPlayer 兜底播放
                    Log.e(TAG, "audioStreamCheckRunnable: Oboe 全量重启失败，启用降级播放")
                    com.bicy.whitenoise.audio.FallbackAudioPlayer.enableFallback()
                    // P0: 标记 Oboe 未初始化，触发健康检查周期性重试恢复
                    isOboeInitialized = false
                    oboeInitRetryCount = 0
                    if (wasPlaying.isNotEmpty()) {
                        for (soundId in wasPlaying) {
                            val filePath = PlaybackStateManager.getLoadedSoundPath(soundId)
                            if (filePath != null) {
                                com.bicy.whitenoise.audio.FallbackAudioPlayer.loadSound(soundId, filePath)
                                com.bicy.whitenoise.audio.FallbackAudioPlayer.setVolume(
                                    soundId, PlaybackStateManager.getVolume(soundId)
                                )
                                com.bicy.whitenoise.audio.FallbackAudioPlayer.playSound(soundId)
                            }
                        }
                    }
                    // 重置计数，避免反复尝试全量重启（已降级，等 Oboe 自然恢复）
                    streamRestartFailureCount = 0
                    nextStreamCheckDelayMs = baseStreamCheckDelayMs
                    mainHandler.post { onAudioStreamRestarted?.invoke() }
                }
            } else {
                // 流正常——重置失败计数与退避
                if (streamRestartFailureCount > 0) {
                    Log.d(TAG, "audioStreamCheckRunnable: 流恢复正常，重置失败计数 ($streamRestartFailureCount -> 0)")
                    streamRestartFailureCount = 0
                    nextStreamCheckDelayMs = baseStreamCheckDelayMs
                }
            }
            audioCheckHandler.postDelayed(this, 500)
        }
    }
    
    private val _playingAudioIds = MutableStateFlow<Set<String>>(emptySet())
    val playingAudioIds: StateFlow<Set<String>> = _playingAudioIds.asStateFlow()
    
    private var isServicePlaying = false
    private var wasPlayingBeforePause = false
    
    private fun syncPlayingState() {
        val allSoundIds = PlaybackStateManager.getAllSoundIds()
        isServicePlaying = allSoundIds.any {
            if (isOboeInitialized) {
                OboeAudioEngine.isPlaying(it) && !OboeAudioEngine.isFadingOut(it)
            } else {
                com.bicy.whitenoise.audio.FallbackAudioPlayer.isPlaying(it)
            }
        }
    }
    
    inner class MusicServiceBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.w(TAG, "MusicService.onCreate() called, instance set")
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        )
        wakeLock?.setReferenceCounted(false)
        
        audioFocusManager = AudioFocusManager(this).apply {
            onAudioFocusLoss = { isPermanent ->
                Log.d(TAG, "onAudioFocusLoss callback triggered, isPermanent=$isPermanent")
                onAudioFocusLost?.invoke()
                if (isPermanent) {
                    Log.d(TAG, "Permanent focus loss - pausing all sounds")
                    pauseAllSounds()
                } else {
                    Log.d(TAG, "Temporary focus loss - pausing all sounds")
                    pauseAllSounds()
                }
            }
            onAudioFocusGain = {
                Log.i(TAG, "onAudioFocusGain callback triggered - calling restoreStreamAndResume")
                restoreStreamAndResume()
            }
            onAudioFocusDuck = {
                Log.d(TAG, "onAudioFocusDuck callback triggered - ducking all sounds to 30%")
                duckAllSounds()
            }
            onAudioFocusUnduck = {
                Log.d(TAG, "onAudioFocusUnduck callback triggered - restoring original volumes")
                unduckAllSounds()
            }
        }
        
        MusicNotificationManager.createNotificationChannel(this)
        val notification = MusicNotificationManager.buildNotification(this, isServicePlaying, getPlayingCount())
        MusicNotificationManager.startForeground(this, notification)
        audioFocusManager.requestAudioFocus()
        
        mS7k.initialize(this)

        // P2-8: 初始化降级播放器（兜底）
        com.bicy.whitenoise.audio.FallbackAudioPlayer.init(this)

        val initialized = try {
            // 防御性清理：服务可能被异常杀后由 START_STICKY 重启，此时 onDestroy 未执行，
            // nativeRelease 未调用，C++ AudioEngine 单例的 isInitialized_=true 但音频流已失效。
            // 先 release 清理残留状态（tracks_、audioStream_），再 init 重新打开流。
            // 进程首次启动时 release 是 no-op（isInitialized_=false），无副作用。
            OboeAudioEngine.release()
            OboeAudioEngine.init()
        } catch (e: Exception) {
            Log.e(TAG, "Oboe引擎初始化异常，尝试降级播放器", e)
            false
        }
        isOboeInitialized = initialized
        Log.d(TAG, "服务创建，Oboe引擎初始化: $initialized")

        // P0: Oboe 初始化失败——立即降级到 MediaPlayer 兜底，避免服务"假活"状态
        // 旧代码仅 Log，导致 audioStreamCheckRunnable 因 needsRestart()=false 不触发恢复，
        // 形成死锁：服务在运行但无任何音频输出，用户必须重启应用。
        if (!initialized) {
            Log.e(TAG, "onCreate: Oboe 初始化失败，启用降级播放器（MediaPlayer 兜底）")
            com.bicy.whitenoise.audio.FallbackAudioPlayer.enableFallback()
            MemoryLockService.reportAnomaly(
                AnomalyType.AUDIO_ENGINE_RESTART,
                "服务启动时 Oboe 引擎初始化失败，已降级到 MediaPlayer 兜底播放，将周期性重试恢复 Oboe"
            )
        }

        PlaybackStateManager.init()
        ScatteredPlayerManager.init(this)
        
        audioCheckHandler.post(audioStreamCheckRunnable)
        
        restoreSavedSounds()
        
        onServiceReadyListener?.invoke()
    }
    
    private fun restoreSavedSounds() {
        val savedSoundIds = PlaybackStateManager.getAllSoundIds()
        Log.d(TAG, "restoreSavedSounds: Found ${savedSoundIds.size} saved sounds, isOboeInitialized=$isOboeInitialized")

        if (savedSoundIds.isEmpty()) {
            Log.d(TAG, "restoreSavedSounds: No saved sounds to restore")
            return
        }

        savedSoundIds.forEach { soundId ->
            val filePath = PlaybackStateManager.getLoadedSoundPath(soundId)
            if (filePath != null) {
                val audioFile = File(filePath)
                if (audioFile.exists()) {
                    Log.d(TAG, "restoreSavedSounds: Pre-loading sound $soundId from $filePath")
                    if (isOboeInitialized) {
                        pendingPlayRequests[soundId] = false
                        loadRetryCount[soundId] = 0
                        OboeAudioEngine.loadSound(soundId, filePath)
                    } else {
                        // P0: Oboe 未初始化（降级模式）——用 FallbackAudioPlayer 加载
                        com.bicy.whitenoise.audio.FallbackAudioPlayer.loadSound(soundId, filePath)
                        com.bicy.whitenoise.audio.FallbackAudioPlayer.setVolume(
                            soundId, PlaybackStateManager.getVolume(soundId)
                        )
                    }
                } else {
                    Log.w(TAG, "restoreSavedSounds: File not found for $soundId: $filePath")
                }
            } else {
                Log.w(TAG, "restoreSavedSounds: No file path for $soundId")
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            MusicNotificationManager.ACTION_PLAY_PAUSE -> handlePlayPause()
            MusicNotificationManager.ACTION_STOP -> handleStop()
            MusicNotificationManager.ACTION_NEXT -> handleNext()
            MusicNotificationManager.ACTION_PREVIOUS -> handlePrevious()
            ACTION_PLAY_SOUND -> {
                val soundId = intent.getStringExtra(EXTRA_SOUND_ID)
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                val soundName = intent.getStringExtra(EXTRA_SOUND_NAME) ?: ""
                if (soundId != null && filePath != null) {
                    playSound(soundId, File(filePath), soundName)
                }
            }
            "android.intent.action.MEDIA_BUTTON" -> {
                @Suppress("DEPRECATION")
                val keyEvent = intent.getParcelableExtra<android.view.KeyEvent>(android.content.Intent.EXTRA_KEY_EVENT)
                if (keyEvent?.action == android.view.KeyEvent.ACTION_DOWN) {
                    Log.d(TAG, "MEDIA_BUTTON routed to mS7k: keyCode=${keyEvent.keyCode}")
                    mS7k.handleMediaButton(keyEvent)
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(loadCheckRunnable)
        audioCheckHandler.removeCallbacks(audioStreamCheckRunnable)
        audioCheckThread.quitSafely()
        stopAllSounds()
        OboeAudioEngine.release()
        // P2-8: 释放降级播放器
        com.bicy.whitenoise.audio.FallbackAudioPlayer.release()
        ScatteredPlayerManager.release()
        audioFocusManager.abandonAudioFocus()
        releaseWakeLock()
        mS7k.release()
        instance = null
        Log.d(TAG, "服务销毁")
    }
    
    fun onAppResume() {
        Log.d(TAG, "应用回到前台")
        
        // 后台期间的缓冲区欠载已由 audioStreamCheckRunnable 持续捕获（轮询 XRun 计数增长），
        // 此处不再重复检查 hasUnderrun() 一次性标志。
        
        if (OboeAudioEngine.needsRestart()) {
            Log.w(TAG, "检测到音频流需要重启 (onAppResume)")
            OboeAudioEngine.clearRestartFlag()

            // P0-2/P2-7: 优先尝试轻量重建流，保留 tracks，避免重新加载文件
            val recreated = OboeAudioEngine.recreateStream()
            Log.w(TAG, "onAppResume: recreateStream result=$recreated")

            if (recreated) {
                // 轻量恢复成功——tracks 已保留，播放状态由新流的数据回调自动恢复
                // 无需重新 loadSound 或 playSound，仅刷新音量/效果/UI 状态
                Log.i(TAG, "onAppResume: 轻量恢复成功，tracks 保留")
                val wasPlaying = PlaybackStateManager.getAllSoundIds().filter { OboeAudioEngine.isPlaying(it) }
                if (wasPlaying.isNotEmpty()) {
                    for (soundId in wasPlaying) {
                        // 重新应用音量与效果配置（recreateStream 不影响这些，但确保一致）
                        val volume = PlaybackStateManager.getVolume(soundId)
                        OboeAudioEngine.setVolume(soundId, volume)

                        val config = ReverbManager.getConfig(soundId)
                        if (config != null) {
                            Log.d(TAG, "onAppResume: Applying reverb config for $soundId")
                            OboeAudioEngine.setEffectEnabled(soundId, true)
                            OboeAudioEngine.setReverbParams(soundId, config.roomSize, config.damping, config.wetLevel)
                        }

                        com.bicy.whitenoise.audio.SpatialAudioManager.applySpatialConfig(soundId)
                        com.bicy.whitenoise.audio.CreativeEffectManager.applyCreativeEffectConfig(soundId)
                    }

                    if (PlaybackStateManager.getAllSoundIds().any { OboeAudioEngine.isPlaying(it) }) {
                        isServicePlaying = true
                        updatePlayingAudioIds()
                        updateWakeLockState()
                        updateNotification()
                    }
                }
                onAudioStreamRestarted?.invoke()
            } else {
                // 轻量恢复失败——走全量 release+init 路径
                Log.w(TAG, "onAppResume: 轻量恢复失败，执行全量重启")
                MemoryLockService.reportAnomaly(AnomalyType.AUDIO_ENGINE_RESTART, "音频流断开(onAppResume,轻量恢复失败)，正在全量重启引擎")

                OboeAudioEngine.clearAllEffectBuffers()

                val wasPlaying = PlaybackStateManager.getAllSoundIds().filter { OboeAudioEngine.isPlaying(it) }

                OboeAudioEngine.release()
                val reinitialized = OboeAudioEngine.init()
                Log.d(TAG, "音频引擎重新初始化: $reinitialized")

                if (reinitialized) {
                    isOboeInitialized = true
                    oboeInitRetryCount = 0
                    if (com.bicy.whitenoise.audio.FallbackAudioPlayer.useFallbackAudioTrack) {
                        com.bicy.whitenoise.audio.FallbackAudioPlayer.switchBackToOboe()
                    }
                    if (wasPlaying.isNotEmpty()) {
                        for (soundId in wasPlaying) {
                            val filePath = PlaybackStateManager.getLoadedSoundPath(soundId)
                            if (filePath != null) {
                                pendingPlayRequests[soundId] = true
                                loadRetryCount[soundId] = 0
                                OboeAudioEngine.loadSound(soundId, filePath)
                            }
                        }
                        mainHandler.post(loadCheckRunnable)

                        mainHandler.postDelayed({
                            for (soundId in wasPlaying) {
                                if (OboeAudioEngine.isLoaded(soundId)) {
                                    val volume = PlaybackStateManager.getVolume(soundId)
                                    OboeAudioEngine.setVolume(soundId, volume)

                                    val config = ReverbManager.getConfig(soundId)
                                    if (config != null) {
                                        Log.d(TAG, "onAppResume: Applying reverb config for $soundId")
                                        OboeAudioEngine.setEffectEnabled(soundId, true)
                                        OboeAudioEngine.setReverbParams(soundId, config.roomSize, config.damping, config.wetLevel)
                                    }

                                    com.bicy.whitenoise.audio.SpatialAudioManager.applySpatialConfig(soundId)
                                    com.bicy.whitenoise.audio.CreativeEffectManager.applyCreativeEffectConfig(soundId)

                                    OboeAudioEngine.setFadeDuration(soundId, 0.5f)
                                    OboeAudioEngine.resumeAll()
                                    OboeAudioEngine.playSound(soundId)

                                    PlaybackStateManager.resumeSound(soundId)
                                }
                            }

                            if (PlaybackStateManager.getAllSoundIds().any { OboeAudioEngine.isPlaying(it) }) {
                                isServicePlaying = true
                                updatePlayingAudioIds()
                                updateWakeLockState()
                                updateNotification()
                            }
                        }, 500)
                    }

                    onAudioStreamRestarted?.invoke()
                } else {
                    // P0: onAppResume 全量重启失败——降级并标记未初始化，交给健康检查重试
                    Log.e(TAG, "onAppResume: 全量重启失败，启用降级播放")
                    isOboeInitialized = false
                    oboeInitRetryCount = 0
                    com.bicy.whitenoise.audio.FallbackAudioPlayer.enableFallback()
                    if (wasPlaying.isNotEmpty()) {
                        for (soundId in wasPlaying) {
                            val filePath = PlaybackStateManager.getLoadedSoundPath(soundId)
                            if (filePath != null) {
                                com.bicy.whitenoise.audio.FallbackAudioPlayer.loadSound(soundId, filePath)
                                com.bicy.whitenoise.audio.FallbackAudioPlayer.setVolume(
                                    soundId, PlaybackStateManager.getVolume(soundId)
                                )
                                com.bicy.whitenoise.audio.FallbackAudioPlayer.playSound(soundId)
                            }
                        }
                    }
                    mainHandler.post { onAudioStreamRestarted?.invoke() }
                }
            }
        }

        if (!audioFocusManager.hasAudioFocus) {
            audioFocusManager.requestAudioFocus()
        }
        
        if (wasPlayingBeforePause) {
            resumeAllSounds()
            wasPlayingBeforePause = false
        }
    }
    
    fun onAppPause() {
        Log.d(TAG, "应用进入后台")
        wasPlayingBeforePause = PlaybackStateManager.getAllSoundIds().any { isActuallyPlaying(it) }
    }

    // P0: 统一播放状态查询——降级模式下也检查 FallbackAudioPlayer
    private fun isActuallyPlaying(soundId: String): Boolean {
        if (isOboeInitialized) return OboeAudioEngine.isPlaying(soundId)
        return com.bicy.whitenoise.audio.FallbackAudioPlayer.isPlaying(soundId)
    }

    private fun getPlayingCount(): Int {
        return PlaybackStateManager.getAllSoundIds().count { isActuallyPlaying(it) }
    }
    
    private fun updateNotification() {
        syncPlayingState()
        val notification = MusicNotificationManager.buildNotification(this, isServicePlaying, getPlayingCount())
        MusicNotificationManager.updateNotification(this, notification)
        mS7k.updatePlaybackState(isServicePlaying, getPlayingCount())
    }
    
    fun refreshNotification() {
        updateNotification()
    }
    
    private fun updatePlayingAudioIds() {
        val allSoundIds = PlaybackStateManager.getAllSoundIds()
        val playingIds = allSoundIds.filter { isActuallyPlaying(it) }.toSet()
        _playingAudioIds.value = playingIds
    }
    
    fun preloadSound(soundId: String, audioFile: File): Boolean {
        try {
            if (!audioFile.exists()) {
                Log.e(TAG, "预加载失败，文件不存在: $soundId")
                return false
            }
            
            if (audioFile.length() == 0L) {
                Log.e(TAG, "预加载失败，文件大小为0: $soundId")
                return false
            }
            
            val filePath = audioFile.absolutePath
            
            if (PlaybackStateManager.getLoadedSoundPath(soundId) != filePath) {
                // 文件路径不同时，先卸载旧音频
                if (OboeAudioEngine.isLoaded(soundId)) {
                    OboeAudioEngine.unloadSound(soundId)
                }
                // 如果正在加载中，先停止再重新加载
                if (OboeAudioEngine.isLoading(soundId)) {
                    OboeAudioEngine.unloadSound(soundId)
                }

                OboeAudioEngine.loadSound(soundId, filePath)
                PlaybackStateManager.setLoadedSoundPath(soundId, filePath)

                Log.d(TAG, "预加载开始: $soundId")
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "预加载异常: $soundId", e)
            MemoryLockService.reportAnomaly(AnomalyType.AUDIO_PLAYER_ERROR, "预加载异常: $soundId", e.stackTraceToString())
            return false
        }
    }
    
    fun playSound(soundId: String, audioFile: File, soundName: String = "") {
        try {
            Log.d(TAG, "尝试播放: $soundId, 文件路径: ${audioFile.absolutePath}")
            
            if (!audioFile.exists()) {
                Log.e(TAG, "文件不存在: $soundId")
                return
            }
            
            if (audioFile.length() == 0L) {
                Log.e(TAG, "文件大小为0，可能下载不完整: $soundId")
                audioFile.delete()
                return
            }

            // P0: Oboe 未初始化（降级模式）——走 FallbackAudioPlayer 路径，避免 native 调用崩溃
            if (!isOboeInitialized) {
                Log.w(TAG, "playSound: Oboe 未初始化，使用降级播放器: $soundId")
                if (PlaybackStateManager.getSoundConfig(soundId) == null) {
                    val savedConfig = com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == soundId }
                    if (savedConfig != null) {
                        PlaybackStateManager.playSound(soundId, audioFile.absolutePath, savedConfig)
                    } else {
                        val newConfig = com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SoundPlayConfig(
                            id = soundId, name = soundName, filePath = audioFile.absolutePath
                        )
                        PlaybackStateManager.playSound(soundId, audioFile.absolutePath, newConfig)
                    }
                } else {
                    PlaybackStateManager.setLoadedSoundPath(soundId, audioFile.absolutePath)
                }
                com.bicy.whitenoise.audio.FallbackAudioPlayer.loadSound(soundId, audioFile.absolutePath)
                com.bicy.whitenoise.audio.FallbackAudioPlayer.setVolume(soundId, PlaybackStateManager.getVolume(soundId))
                com.bicy.whitenoise.audio.FallbackAudioPlayer.playSound(soundId)
                hasBeenPlayed.add(soundId)
                PlaybackStateManager.resumeSound(soundId)
                isServicePlaying = true
                updatePlayingAudioIds()
                updateWakeLockState()
                onPlaybackStateChangeListener?.invoke(soundId, true)
                val soundName = PlaybackStateManager.getSoundConfig(soundId)?.name ?: soundId
                mS7k.updateMetadata(title = soundName, artist = "添空", album = "白噪音")
                mS7k.updatePlaybackState(true, getPlayingCount())
                updateNotification()
                return
            }

            if (OboeAudioEngine.isPlaying(soundId)) {
                Log.d(TAG, "声音已在播放，先停止再重新播放: $soundId")
                OboeAudioEngine.stopSound(soundId)
                OboeAudioEngine.unloadSound(soundId)
            }
            
            if (PlaybackStateManager.getSoundConfig(soundId) == null) {
                val savedConfig = com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == soundId }
                if (savedConfig != null) {
                    PlaybackStateManager.playSound(soundId, audioFile.absolutePath, savedConfig)
                } else {
                    val newConfig = com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SoundPlayConfig(
                        id = soundId,
                        name = soundName,
                        filePath = audioFile.absolutePath
                    )
                    PlaybackStateManager.playSound(soundId, audioFile.absolutePath, newConfig)
                }
            } else {
                PlaybackStateManager.setLoadedSoundPath(soundId, audioFile.absolutePath)
                val existingConfig = PlaybackStateManager.getSoundConfig(soundId)
                if (existingConfig != null && existingConfig.filePath != audioFile.absolutePath) {
                    PlaybackStateManager.updateSoundConfig(soundId, existingConfig.copy(filePath = audioFile.absolutePath))
                }
            }
            
            val filePath = audioFile.absolutePath

            // 如果已加载但文件路径不同，先卸载旧音频
            if (OboeAudioEngine.isLoaded(soundId)) {
                val loadedPath = PlaybackStateManager.getLoadedSoundPath(soundId)
                if (loadedPath != null && loadedPath != filePath) {
                    OboeAudioEngine.unloadSound(soundId)
                }
            }

            if (OboeAudioEngine.isLoaded(soundId)) {
                startPlayback(soundId)
            } else if (OboeAudioEngine.isLoading(soundId)) {
                Log.d(TAG, "音频正在加载中，等待: $soundId")
                pendingPlayRequests[soundId] = true
                mainHandler.removeCallbacks(loadCheckRunnable)
                mainHandler.post(loadCheckRunnable)
            } else {
                pendingPlayRequests[soundId] = true
                loadRetryCount[soundId] = 0
                OboeAudioEngine.loadSound(soundId, filePath)
                Log.d(TAG, "加载开始: $soundId")
                mainHandler.removeCallbacks(loadCheckRunnable)
                mainHandler.post(loadCheckRunnable)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "播放失败: $soundId", e)
            MemoryLockService.reportAnomaly(AnomalyType.AUDIO_PLAYER_ERROR, "播放失败: $soundId", e.stackTraceToString())
        }
    }
    
    private fun applySoundConfigs(soundId: String) {
        val volume = PlaybackStateManager.getVolume(soundId)
        OboeAudioEngine.setVolume(soundId, volume)
        Log.d(TAG, "applySoundConfigs: Set volume for $soundId: $volume")
        
        val reverbConfig = ReverbManager.getConfig(soundId)
        if (reverbConfig != null && reverbConfig.enabled) {
            OboeAudioEngine.setEffectEnabled(soundId, true)
            ReverbManager.applyReverbConfig(soundId)
            Log.d(TAG, "applySoundConfigs: Applied reverb config for $soundId")
        } else {
            OboeAudioEngine.setEffectEnabled(soundId, false)
            Log.d(TAG, "applySoundConfigs: No reverb config or disabled for $soundId")
        }
        
        com.bicy.whitenoise.audio.SpatialAudioManager.applySpatialConfig(soundId)
        com.bicy.whitenoise.audio.CreativeEffectManager.applyCreativeEffectConfig(soundId)
    }
    
    private fun startPlayback(soundId: String) {
        applySoundConfigs(soundId)

        OboeAudioEngine.setFadeDuration(soundId, 0.5f)
        OboeAudioEngine.playSound(soundId)

        hasBeenPlayed.add(soundId)

        PlaybackStateManager.resumeSound(soundId)
        isServicePlaying = true

        updatePlayingAudioIds()
        updateWakeLockState()
        onPlaybackStateChangeListener?.invoke(soundId, true)

        // P1-4: 更新 MediaSession 的 Metadata，让系统识别为媒体播放器
        // 标题取自 PlaybackStateManager 中的配置名，未设置时用 soundId 兜底
        val soundName = PlaybackStateManager.getSoundConfig(soundId)?.name ?: soundId
        val playingCount = getPlayingCount()
        mS7k.updateMetadata(
            title = if (playingCount > 1) "添空 · $playingCount 个音频" else soundName,
            artist = "添空",
            album = "白噪音"
        )
        mS7k.updatePlaybackState(true, playingCount)

        updateNotification()
        Log.d(TAG, "开始播放: $soundId")
    }
    
    fun stopSound(soundId: String) {
        try {
            if (isOboeInitialized) {
                OboeAudioEngine.stopSound(soundId)
                OboeAudioEngine.unloadSound(soundId)
            } else {
                com.bicy.whitenoise.audio.FallbackAudioPlayer.stopSound(soundId)
                com.bicy.whitenoise.audio.FallbackAudioPlayer.unloadSound(soundId)
            }

            hasBeenPlayed.remove(soundId)

            PlaybackStateManager.stopSound(soundId)

            ReverbManager.removeReverbEffect(soundId)
            com.bicy.whitenoise.audio.SpatialAudioManager.removeConfig(soundId)
            com.bicy.whitenoise.audio.CreativeEffectManager.removeConfig(soundId)

            updatePlayingAudioIds()
            updateWakeLockState()
            onPlaybackStateChangeListener?.invoke(soundId, false)

            if (!PlaybackStateManager.getAllSoundIds().any { isActuallyPlaying(it) }) {
                isServicePlaying = false
            }

            updateNotification()
            Log.d(TAG, "停止播放: $soundId")

        } catch (e: Exception) {
            Log.e(TAG, "停止失败: $soundId", e)
            MemoryLockService.reportAnomaly(AnomalyType.AUDIO_PLAYER_ERROR, "停止失败: $soundId", e.stackTraceToString())
        }
    }

    fun pauseSound(soundId: String) {
        try {
            if (isOboeInitialized) {
                OboeAudioEngine.setFadeDuration(soundId, 0.5f)
                OboeAudioEngine.pauseSound(soundId)
            } else {
                com.bicy.whitenoise.audio.FallbackAudioPlayer.pauseSound(soundId)
            }

            PlaybackStateManager.pauseSound(soundId)

            updatePlayingAudioIds()

            if (!PlaybackStateManager.getAllSoundIds().any { isActuallyPlaying(it) }) {
                isServicePlaying = false
            }

            updateNotification()
            Log.d(TAG, "暂停播放(淡出): $soundId")

        } catch (e: Exception) {
            Log.e(TAG, "暂停失败: $soundId", e)
            MemoryLockService.reportAnomaly(AnomalyType.AUDIO_PLAYER_ERROR, "暂停失败: $soundId", e.stackTraceToString())
        }
    }

    fun resumeSound(soundId: String) {
        try {
            if (!isOboeInitialized) {
                // P0: 降级模式——直接通过 FallbackAudioPlayer 恢复
                com.bicy.whitenoise.audio.FallbackAudioPlayer.playSound(soundId)
                Log.d(TAG, "恢复播放(降级模式): $soundId")
            } else if (OboeAudioEngine.isFadingOut(soundId)) {
                OboeAudioEngine.cancelFadeOut(soundId)
                Log.d(TAG, "取消淡出: $soundId")
            } else if (hasBeenPlayed.contains(soundId)) {
                OboeAudioEngine.setFadeDuration(soundId, 0.5f)
                OboeAudioEngine.resumeSound(soundId)
                Log.d(TAG, "恢复播放(淡入): $soundId")
            } else {
                applySoundConfigs(soundId)
                OboeAudioEngine.setFadeDuration(soundId, 0.5f)
                OboeAudioEngine.playSound(soundId)
                hasBeenPlayed.add(soundId)
                Log.d(TAG, "首次播放(淡入): $soundId")
            }

            PlaybackStateManager.resumeSound(soundId)

            updatePlayingAudioIds()

            isServicePlaying = true

            updateNotification()
            Log.d(TAG, "恢复播放(淡入): $soundId")

        } catch (e: Exception) {
            Log.e(TAG, "恢复失败: $soundId", e)
            MemoryLockService.reportAnomaly(AnomalyType.AUDIO_PLAYER_ERROR, "恢复失败: $soundId", e.stackTraceToString())
        }
    }
    
    fun stopAllSounds() {
        if (isOboeInitialized) {
            OboeAudioEngine.stopAllSounds()
        }

        hasBeenPlayed.clear()

        PlaybackStateManager.clearAll()

        ScatteredPlayerManager.stopAll()

        // P0: 降级模式下也要停止
        if (!isOboeInitialized) {
            com.bicy.whitenoise.audio.FallbackAudioPlayer.stopAll()
        }

        isServicePlaying = false
        updatePlayingAudioIds()
        updateWakeLockState()
        updateNotification()
    }
    
    fun pauseAllSounds() {
        val allSoundIds = PlaybackStateManager.getAllSoundIds()
        Log.d(TAG, "pauseAllSounds: allSoundIds=$allSoundIds")

        allSoundIds.forEach { soundId ->
            // P0: 降级模式下 isFadingOut 恒为 false，用 isActuallyPlaying 判断
            if (isOboeInitialized && OboeAudioEngine.isFadingOut(soundId)) {
                Log.d(TAG, "pauseAllSounds: soundId=$soundId already fading out, skip")
                return@forEach
            }
            val playing = isActuallyPlaying(soundId)
            Log.d(TAG, "pauseAllSounds: soundId=$soundId, isPlaying=$playing")
            if (playing) {
                pauseSound(soundId)
            }
        }

        ScatteredPlayerManager.pauseAll()

        // P2-8: 降级模式下也要暂停（pauseSound 已分流，但兜底调用 pauseAll 确保一致性）
        if (!isOboeInitialized) {
            com.bicy.whitenoise.audio.FallbackAudioPlayer.pauseAll()
        }

        isServicePlaying = false
        updatePlayingAudioIds()
        updateNotification()
    }
    
    // Duck (降低音量) 相关方法
    private val duckedVolumes = mutableMapOf<String, Float>()
    private var isDucking = false
    
    fun duckAllSounds() {
        if (isDucking) return
        isDucking = true

        val allSoundIds = PlaybackStateManager.getAllSoundIds()
        Log.d(TAG, "duckAllSounds: ducking ${allSoundIds.size} sounds")

        if (isOboeInitialized) {
            allSoundIds.forEach { soundId ->
                val currentVolume = OboeAudioEngine.getVolume(soundId)
                duckedVolumes[soundId] = currentVolume
                OboeAudioEngine.setVolume(soundId, currentVolume * 0.3f)
                Log.d(TAG, "duckAllSounds: $soundId volume $currentVolume -> ${currentVolume * 0.3f}")
            }
        }

        ScatteredPlayerManager.duckAll()

        // P0: 降级模式下由 FallbackAudioPlayer 统一处理 duck
        if (!isOboeInitialized) {
            com.bicy.whitenoise.audio.FallbackAudioPlayer.duckAll()
        }
    }

    fun unduckAllSounds() {
        if (!isDucking) return
        isDucking = false

        Log.d(TAG, "unduckAllSounds: restoring ${duckedVolumes.size} sounds")

        if (isOboeInitialized) {
            duckedVolumes.forEach { (soundId, originalVolume) ->
                OboeAudioEngine.setVolume(soundId, originalVolume)
                Log.d(TAG, "unduckAllSounds: $soundId volume restored to $originalVolume")
            }
        }
        duckedVolumes.clear()

        ScatteredPlayerManager.unduckAll()

        // P0: 降级模式下由 FallbackAudioPlayer 统一处理 unduck
        if (!isOboeInitialized) {
            com.bicy.whitenoise.audio.FallbackAudioPlayer.unduckAll()
        }
    }
    
    fun resumeAllSounds() {
        val allSoundIds = PlaybackStateManager.getAllSoundIds()
        Log.d(TAG, "resumeAllSounds: allSoundIds=$allSoundIds, count=${allSoundIds.size}")
        
        if (allSoundIds.isEmpty()) {
            Log.w(TAG, "resumeAllSounds: No sounds in PlaybackStateManager!")
            return
        }
        
        var resumedCount = 0
        allSoundIds.forEach { soundId ->
            // P0: 降级模式下用 FallbackAudioPlayer 查询
            val loaded = if (isOboeInitialized) OboeAudioEngine.isLoaded(soundId)
                else com.bicy.whitenoise.audio.FallbackAudioPlayer.isLoaded(soundId)
            val playing = isActuallyPlaying(soundId)
            val fadingOut = isOboeInitialized && OboeAudioEngine.isFadingOut(soundId)
            Log.d(TAG, "resumeAllSounds: soundId=$soundId, isLoaded=$loaded, isPlaying=$playing, isFadingOut=$fadingOut")
            if (fadingOut) {
                Log.d(TAG, "resumeAllSounds: Cancelling fade-out for $soundId")
                resumeSound(soundId)
                resumedCount++
            } else if (loaded && !playing) {
                resumeSound(soundId)
                resumedCount++
            } else if (!loaded) {
                Log.w(TAG, "resumeAllSounds: Sound $soundId not loaded, reloading...")
                val filePath = PlaybackStateManager.getLoadedSoundPath(soundId)
                if (filePath != null) {
                    val audioFile = java.io.File(filePath)
                    if (audioFile.exists()) {
                        playSound(soundId, audioFile)
                        resumedCount++
                    } else {
                        Log.e(TAG, "resumeAllSounds: File not found for $soundId: $filePath")
                    }
                } else {
                    Log.e(TAG, "resumeAllSounds: No file path for $soundId")
                }
            }
        }

        Log.d(TAG, "resumeAllSounds: Resumed $resumedCount sounds")

        ScatteredPlayerManager.resumeAll()

        // P0: 降级模式下 resumeSound 已分流，兜底调用 resumeAll 确保一致性
        if (!isOboeInitialized) {
            com.bicy.whitenoise.audio.FallbackAudioPlayer.resumeAll()
        }

        isServicePlaying = true
        updatePlayingAudioIds()
        updateNotification()
    }

    private fun restoreStreamAndResume() {
        val soundsToRestore = PlaybackStateManager.getAllSoundIds().filter { !isActuallyPlaying(it) }
        Log.i(TAG, "restoreStreamAndResume: soundsToRestore=$soundsToRestore")
        
        if (soundsToRestore.isEmpty()) {
            Log.w(TAG, "restoreStreamAndResume: No sounds to restore, returning")
            return
        }

        Log.i(TAG, "音频焦点恢复，重启音频流以适配新输出设备")

        OboeAudioEngine.clearAllEffectBuffers()
        OboeAudioEngine.release()
        val reinitialized = OboeAudioEngine.init()
        Log.i(TAG, "音频引擎重新初始化: $reinitialized")

        if (!reinitialized) {
            // P0: 重启失败——降级到 MediaPlayer 兜底，并标记未初始化交由健康检查重试
            Log.e(TAG, "音频引擎重新初始化失败，启用降级播放")
            isOboeInitialized = false
            oboeInitRetryCount = 0
            com.bicy.whitenoise.audio.FallbackAudioPlayer.enableFallback()
            for (soundId in soundsToRestore) {
                val filePath = PlaybackStateManager.getLoadedSoundPath(soundId)
                if (filePath != null) {
                    com.bicy.whitenoise.audio.FallbackAudioPlayer.loadSound(soundId, filePath)
                    com.bicy.whitenoise.audio.FallbackAudioPlayer.setVolume(
                        soundId, PlaybackStateManager.getVolume(soundId)
                    )
                    com.bicy.whitenoise.audio.FallbackAudioPlayer.playSound(soundId)
                }
            }
            mainHandler.post { onAudioStreamRestarted?.invoke() }
            return
        }

        isOboeInitialized = true
        oboeInitRetryCount = 0

        for (soundId in soundsToRestore) {
            val filePath = PlaybackStateManager.getLoadedSoundPath(soundId)
            Log.d(TAG, "restoreStreamAndResume: Loading sound $soundId from $filePath")
            if (filePath != null) {
                pendingPlayRequests[soundId] = true
                loadRetryCount[soundId] = 0
                OboeAudioEngine.loadSound(soundId, filePath)
            }
        }

        if (pendingPlayRequests.isNotEmpty()) {
            mainHandler.post(loadCheckRunnable)
        }

        mainHandler.postDelayed({
            Log.i(TAG, "restoreStreamAndResume: Post-delayed execution starting")
            for (soundId in soundsToRestore) {
                val isLoaded = OboeAudioEngine.isLoaded(soundId)
                Log.d(TAG, "restoreStreamAndResume: Sound $soundId isLoaded=$isLoaded")
                
                if (isLoaded) {
                    val volume = PlaybackStateManager.getVolume(soundId)
                    Log.d(TAG, "restoreStreamAndResume: Setting volume for $soundId to $volume")
                    OboeAudioEngine.setVolume(soundId, volume)
                    
                    val config = ReverbManager.getConfig(soundId)
                    Log.d(TAG, "restoreStreamAndResume: Reverb config for $soundId: $config")
                    if (config != null && config.enabled) {
                        Log.i(TAG, "restoreStreamAndResume: Applying reverb config for $soundId")
                        OboeAudioEngine.setEffectEnabled(soundId, true)
                        OboeAudioEngine.setReverbParams(soundId, config.roomSize, config.damping, config.wetLevel)
                    } else {
                        Log.w(TAG, "restoreStreamAndResume: Reverb disabled for $soundId, skipping")
                    }
                    
                    Log.d(TAG, "restoreStreamAndResume: Applying spatial and creative effects for $soundId")
                    com.bicy.whitenoise.audio.SpatialAudioManager.applySpatialConfig(soundId)
                    com.bicy.whitenoise.audio.CreativeEffectManager.applyCreativeEffectConfig(soundId)
                    
                    Log.i(TAG, "restoreStreamAndResume: Starting playback for $soundId")
                    OboeAudioEngine.setFadeDuration(soundId, 0.5f)
                    OboeAudioEngine.resumeAll()
                    OboeAudioEngine.playSound(soundId)
                    
                    PlaybackStateManager.resumeSound(soundId)
                    Log.i(TAG, "restoreStreamAndResume: Sound $soundId marked as playing")
                } else {
                    Log.w(TAG, "restoreStreamAndResume: Sound $soundId not loaded yet, skipping")
                }
            }
            
            if (PlaybackStateManager.getAllSoundIds().any { OboeAudioEngine.isPlaying(it) }) {
                Log.i(TAG, "restoreStreamAndResume: Updating service state")
                isServicePlaying = true
                updatePlayingAudioIds()
                updateWakeLockState()
                updateNotification()
            }
            
            Log.i(TAG, "restoreStreamAndResume: Calling onAudioStreamRestarted callback")
            onAudioStreamRestarted?.invoke()
        }, 500)
    }
    
    fun setVolume(soundId: String, volume: Float) {
        PlaybackStateManager.updateVolume(soundId, volume)
        if (isOboeInitialized) {
            OboeAudioEngine.setVolume(soundId, volume)
        } else {
            com.bicy.whitenoise.audio.FallbackAudioPlayer.setVolume(soundId, volume)
        }
        ScatteredPlayerManager.updateTrackConfig(trackId = soundId, volume = volume)
    }

    fun getVolume(soundId: String): Float {
        return PlaybackStateManager.getVolume(soundId)
    }

    fun isPlaying(soundId: String): Boolean = isActuallyPlaying(soundId)

    fun isSoundPlaying(soundId: String): Boolean = isActuallyPlaying(soundId)

    fun getPlayingSounds(): Set<String> {
        return PlaybackStateManager.getAllSoundIds().filter { isActuallyPlaying(it) }.toSet()
    }

    fun isLoaded(soundId: String): Boolean {
        if (isOboeInitialized) return OboeAudioEngine.isLoaded(soundId)
        return com.bicy.whitenoise.audio.FallbackAudioPlayer.isLoaded(soundId)
    }
    
    fun setReverbConfig(soundId: String, config: ReverbConfig) {
        ReverbManager.setReverbEffect(soundId, config)
        PlaybackStateManager.updateReverbConfig(soundId, config)
        if (OboeAudioEngine.isPlaying(soundId)) {
            if (config.enabled) {
                OboeAudioEngine.setEffectEnabled(soundId, true)
                OboeAudioEngine.setReverbParams(soundId, config.roomSize, config.damping, config.wetLevel)
            } else {
                OboeAudioEngine.setEffectEnabled(soundId, false)
            }
        }
    }
    
    fun setEffectEnabled(soundId: String, enabled: Boolean) {
        OboeAudioEngine.setEffectEnabled(soundId, enabled)
    }
    
    fun setReverbParams(soundId: String, roomSize: Float, damping: Float, wetLevel: Float) {
        OboeAudioEngine.setReverbParams(soundId, roomSize, damping, wetLevel)
    }
    
    private fun handlePlayPause() {
        mS7k.handleExternalPlayPause()
    }
    
    private fun handleStop() {
        mS7k.handleExternalStop()
    }
    
    private fun handleNext() {
        mS7k.handleExternalNext()
    }
    
    private fun handlePrevious() {
        mS7k.handleExternalPrevious()
    }
    
    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire()
                Log.d(TAG, "WakeLock 已获取")
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取 WakeLock 失败", e)
        }
    }
    
    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock 已释放")
            }
        } catch (e: Exception) {
            Log.e(TAG, "释放 WakeLock 失败", e)
        }
    }
    
    private fun updateWakeLockState() {
        if (PlaybackStateManager.getAllSoundIds().any { isActuallyPlaying(it) }) {
            acquireWakeLock()
        } else {
            releaseWakeLock()
        }
    }
    
    fun reloadAllTracksWithNewEffectOrder() {
        val wasPlaying = PlaybackStateManager.getAllSoundIds().filter { OboeAudioEngine.isPlaying(it) }
        
        if (wasPlaying.isEmpty()) {
            return
        }
        
        Log.d(TAG, "重载所有音轨以应用新的效果顺序")
        
        val effectOrder = com.bicy.whitenoise.storage.config.ConfigStorage.getAudioEffectOrder()
        val orderIntArray = effectOrder.map { 
            when (it) {
                "spatial" -> 0
                "reverb" -> 1
                "equalizer" -> 2
                "quality" -> 3
                else -> 0
            }
        }.toIntArray()
        
        for (soundId in wasPlaying) {
            OboeAudioEngine.setEffectOrder(soundId, orderIntArray)
        }
        
        Log.d(TAG, "已为 ${wasPlaying.size} 个音轨应用新的效果顺序")
    }
    
    fun registerScatteredTrack(
        trackId: String,
        audioClips: List<ScatteredAudioClipData>,
        minIntervalMs: Long,
        maxIntervalMs: Long,
        volume: Float,
        spatialRange: SpatialScatterRangeData,
        spatialEnabled: Boolean,
        overlayMode: Boolean
    ) {
        ScatteredPlayerManager.registerTrack(
            trackId = trackId,
            audioClips = audioClips,
            minIntervalMs = minIntervalMs,
            maxIntervalMs = maxIntervalMs,
            volume = volume,
            spatialRange = spatialRange,
            spatialEnabled = spatialEnabled,
            overlayMode = overlayMode
        )
        
        PlaybackStateManager.registerScatteredTrack(
            trackId = trackId,
            audioClips = audioClips,
            minIntervalMs = minIntervalMs,
            maxIntervalMs = maxIntervalMs,
            volume = volume,
            spatialRange = spatialRange,
            spatialEnabled = spatialEnabled,
            overlayMode = overlayMode
        )
    }
    
    fun unregisterScatteredTrack(trackId: String) {
        ScatteredPlayerManager.unregisterTrack(trackId)
        PlaybackStateManager.stopSound(trackId)
    }
    
    fun startScatteredTrack(trackId: String) {
        ScatteredPlayerManager.startTrack(trackId)
        PlaybackStateManager.resumeSound(trackId)
    }
    
    fun stopScatteredTrack(trackId: String) {
        ScatteredPlayerManager.stopTrack(trackId)
        PlaybackStateManager.pauseSound(trackId)
    }
    
    fun updateScatteredTrackClips(trackId: String, audioClips: List<ScatteredAudioClipData>) {
        ScatteredPlayerManager.updateTrackClips(trackId, audioClips)
    }
}
