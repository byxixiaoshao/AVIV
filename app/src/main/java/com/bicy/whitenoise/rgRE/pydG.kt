package com.bicy.whitenoise.rgRE

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.bicy.whitenoise.H3HO.OboeAudioEngine
import com.bicy.whitenoise.H3HO.PlaybackStateManager
import com.bicy.whitenoise.H3HO.ReverbConfig
import com.bicy.whitenoise.H3HO.ReverbManager
import com.bicy.whitenoise.H3HO.ScatteredPlayerManager
import com.bicy.whitenoise.rgRE.uY6e.AudioFocusManager
import com.bicy.whitenoise.rgRE.uY6e.MusicNotificationManager
import com.bicy.whitenoise.JwJY.sBYh.kcFp.ScatteredAudioClipData
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialScatterRangeData
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
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
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
            if (OboeAudioEngine.needsRestart()) {
                Log.w(TAG, "检测到音频流需要重启")
                OboeAudioEngine.clearRestartFlag()
                
                onAudioStreamDisconnect?.invoke()
                
                OboeAudioEngine.clearAllEffectBuffers()
                
                val wasPlaying = PlaybackStateManager.getPlayingSounds()
                Log.w(TAG, "audioStreamCheckRunnable: wasPlaying=$wasPlaying")
                
                OboeAudioEngine.release()
                val reinitialized = OboeAudioEngine.init()
                Log.w(TAG, "音频引擎重新初始化: $reinitialized")
                
                if (reinitialized) {
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
                        
                        Log.w(TAG, "audioStreamCheckRunnable: Scheduling post-delayed effect application in 500ms")
                        mainHandler.postDelayed({
                            Log.w(TAG, "audioStreamCheckRunnable: Post-delayed execution starting")
                            for (soundId in wasPlaying) {
                                val isLoaded = OboeAudioEngine.isLoaded(soundId)
                                Log.w(TAG, "audioStreamCheckRunnable: Sound $soundId isLoaded=$isLoaded")
                                
                                if (isLoaded) {
                                    val volume = PlaybackStateManager.getVolume(soundId)
                                    Log.w(TAG, "audioStreamCheckRunnable: Setting volume for $soundId to $volume")
                                    OboeAudioEngine.setVolume(soundId, volume)
                                    
                                    val config = ReverbManager.getConfig(soundId)
                                    Log.w(TAG, "audioStreamCheckRunnable: Reverb config for $soundId: $config")
                                    if (config != null && config.enabled) {
                                        Log.w(TAG, "audioStreamCheckRunnable: Applying reverb config for $soundId")
                                        OboeAudioEngine.setEffectEnabled(soundId, true)
                                        OboeAudioEngine.setReverbParams(soundId, config.roomSize, config.damping, config.wetLevel)
                                    } else {
                                        Log.w(TAG, "audioStreamCheckRunnable: Reverb disabled for $soundId, skipping")
                                    }
                                    
                                    Log.w(TAG, "audioStreamCheckRunnable: Applying spatial and creative effects for $soundId")
                                    com.bicy.whitenoise.H3HO.SpatialAudioManager.applySpatialConfig(soundId)
                                    com.bicy.whitenoise.H3HO.CreativeEffectManager.applyCreativeEffectConfig(soundId)
                                    
                                    Log.w(TAG, "audioStreamCheckRunnable: Starting playback for $soundId")
                                    OboeAudioEngine.setFadeDuration(soundId, 0.5f)
                                    OboeAudioEngine.resumeAll()
                                    OboeAudioEngine.playSound(soundId)
                                    
                                    PlaybackStateManager.resumeSound(soundId)
                                    Log.w(TAG, "audioStreamCheckRunnable: Sound $soundId marked as playing")
                                } else {
                                    Log.w(TAG, "audioStreamCheckRunnable: Sound $soundId not loaded yet, skipping")
                                }
                            }
                            
                            if (PlaybackStateManager.getPlayingSounds().isNotEmpty()) {
                                Log.w(TAG, "audioStreamCheckRunnable: Updating service state")
                                isServicePlaying = true
                                updatePlayingAudioIds()
                                updateWakeLockState()
                                updateNotification()
                            }
                        }, 500)
                    }
                    
                    mainHandler.post {
                        Log.w(TAG, "audioStreamCheckRunnable: Calling onAudioStreamRestarted callback, callback is ${if (onAudioStreamRestarted != null) "set" else "null"}")
                        onAudioStreamRestarted?.invoke()
                    }
                }
            }
            mainHandler.postDelayed(this, 500)
        }
    }
    
    private val _playingAudioIds = MutableStateFlow<Set<String>>(emptySet())
    val playingAudioIds: StateFlow<Set<String>> = _playingAudioIds.asStateFlow()
    
    private var isServicePlaying = false
    private var wasPlayingBeforePause = false
    
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
                Log.d(TAG, "onAudioFocusDuck callback triggered - pausing all")
                OboeAudioEngine.pauseAll()
            }
        }
        
        MusicNotificationManager.createNotificationChannel(this)
        val notification = MusicNotificationManager.buildNotification(this, isServicePlaying, getPlayingCount())
        MusicNotificationManager.startForeground(this, notification)
        audioFocusManager.requestAudioFocus()
        
        val initialized = OboeAudioEngine.init()
        Log.d(TAG, "服务创建，Oboe引擎初始化: $initialized")
        
        PlaybackStateManager.init()
        ScatteredPlayerManager.init(this)
        
        mainHandler.post(audioStreamCheckRunnable)
        
        onServiceReadyListener?.invoke()
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            MusicNotificationManager.ACTION_PLAY_PAUSE -> handlePlayPause()
            MusicNotificationManager.ACTION_STOP -> handleStop()
            ACTION_PLAY_SOUND -> {
                val soundId = intent.getStringExtra(EXTRA_SOUND_ID)
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                val soundName = intent.getStringExtra(EXTRA_SOUND_NAME) ?: ""
                if (soundId != null && filePath != null) {
                    playSound(soundId, File(filePath), soundName)
                }
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(audioStreamCheckRunnable)
        mainHandler.removeCallbacks(loadCheckRunnable)
        stopAllSounds()
        OboeAudioEngine.release()
        ScatteredPlayerManager.release()
        audioFocusManager.abandonAudioFocus()
        releaseWakeLock()
        instance = null
        Log.d(TAG, "服务销毁")
    }
    
    fun onAppResume() {
        Log.d(TAG, "应用回到前台")
        
        if (OboeAudioEngine.needsRestart()) {
            Log.w(TAG, "检测到音频流需要重启")
            OboeAudioEngine.clearRestartFlag()
            
            OboeAudioEngine.clearAllEffectBuffers()
            
            val wasPlaying = PlaybackStateManager.getPlayingSounds()
            
            OboeAudioEngine.release()
            val reinitialized = OboeAudioEngine.init()
            Log.d(TAG, "音频引擎重新初始化: $reinitialized")
            
            if (reinitialized) {
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
                                
                                com.bicy.whitenoise.H3HO.SpatialAudioManager.applySpatialConfig(soundId)
                                com.bicy.whitenoise.H3HO.CreativeEffectManager.applyCreativeEffectConfig(soundId)
                                
                                OboeAudioEngine.setFadeDuration(soundId, 0.5f)
                                OboeAudioEngine.resumeAll()
                                OboeAudioEngine.playSound(soundId)
                                
                                PlaybackStateManager.resumeSound(soundId)
                            }
                        }
                        
                        if (PlaybackStateManager.getPlayingSounds().isNotEmpty()) {
                            isServicePlaying = true
                            updatePlayingAudioIds()
                            updateWakeLockState()
                            updateNotification()
                        }
                    }, 500)
                }
                
                onAudioStreamRestarted?.invoke()
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
        wasPlayingBeforePause = PlaybackStateManager.getPlayingSounds().isNotEmpty()
    }
    
    private fun getPlayingCount(): Int = PlaybackStateManager.getPlayingSounds().size
    
    private fun updateNotification() {
        val notification = MusicNotificationManager.buildNotification(this, isServicePlaying, getPlayingCount())
        MusicNotificationManager.updateNotification(this, notification)
    }
    
    private fun updatePlayingAudioIds() {
        val playingIds = PlaybackStateManager.getPlayingSounds().toSet()
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
                if (OboeAudioEngine.isLoaded(soundId) || OboeAudioEngine.isLoading(soundId)) {
                    Log.d(TAG, "音频已加载或正在加载: $soundId")
                    return true
                }
                
                OboeAudioEngine.loadSound(soundId, filePath)
                PlaybackStateManager.setLoadedSoundPath(soundId, filePath)
                
                Log.d(TAG, "预加载开始: $soundId")
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "预加载异常: $soundId", e)
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
            
            if (PlaybackStateManager.isPlaying(soundId)) {
                Log.d(TAG, "声音已在播放，停止: $soundId")
                stopSound(soundId)
                return
            }
            
            if (PlaybackStateManager.getSoundConfig(soundId) == null) {
                val savedConfig = com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == soundId }
                if (savedConfig != null) {
                    PlaybackStateManager.playSound(soundId, audioFile.absolutePath, savedConfig)
                } else {
                    PlaybackStateManager.setLoadedSoundPath(soundId, audioFile.absolutePath)
                }
            }
            
            val filePath = audioFile.absolutePath
            
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
        }
    }
    
    private fun startPlayback(soundId: String) {
        val volume = PlaybackStateManager.getVolume(soundId)
        OboeAudioEngine.setVolume(soundId, volume)
        
        val config = ReverbManager.getConfig(soundId)
        if (config != null && config.enabled) {
            Log.d(TAG, "startPlayback: Applying reverb config for $soundId, roomSize=${config.roomSize}, damping=${config.damping}, wetLevel=${config.wetLevel}")
            OboeAudioEngine.setEffectEnabled(soundId, true)
            OboeAudioEngine.setReverbParams(soundId, config.roomSize, config.damping, config.wetLevel)
        } else {
            Log.w(TAG, "startPlayback: No reverb config or disabled for $soundId")
        }
        
        com.bicy.whitenoise.H3HO.SpatialAudioManager.applySpatialConfig(soundId)
        com.bicy.whitenoise.H3HO.CreativeEffectManager.applyCreativeEffectConfig(soundId)
        
        OboeAudioEngine.setFadeDuration(soundId, 0.5f)
        OboeAudioEngine.resumeAll()
        OboeAudioEngine.playSound(soundId)
        
        PlaybackStateManager.resumeSound(soundId)
        isServicePlaying = true
        
        updatePlayingAudioIds()
        updateWakeLockState()
        onPlaybackStateChangeListener?.invoke(soundId, true)
        updateNotification()
        Log.d(TAG, "开始播放: $soundId")
    }
    
    fun stopSound(soundId: String) {
        try {
            OboeAudioEngine.stopSound(soundId)
            OboeAudioEngine.unloadSound(soundId)
            
            PlaybackStateManager.stopSound(soundId)
            
            ReverbManager.removeReverbEffect(soundId)
            com.bicy.whitenoise.H3HO.SpatialAudioManager.removeConfig(soundId)
            com.bicy.whitenoise.H3HO.CreativeEffectManager.removeConfig(soundId)
            
            updatePlayingAudioIds()
            updateWakeLockState()
            onPlaybackStateChangeListener?.invoke(soundId, false)
            
            if (PlaybackStateManager.getPlayingSounds().isEmpty()) {
                isServicePlaying = false
            }
            
            updateNotification()
            Log.d(TAG, "停止播放: $soundId")
            
        } catch (e: Exception) {
            Log.e(TAG, "停止失败: $soundId", e)
        }
    }
    
    fun pauseSound(soundId: String) {
        try {
            OboeAudioEngine.setFadeDuration(soundId, 0.5f)
            OboeAudioEngine.pauseSound(soundId)
            
            PlaybackStateManager.pauseSound(soundId)
            
            updatePlayingAudioIds()
            
            if (PlaybackStateManager.getPlayingSounds().isEmpty()) {
                isServicePlaying = false
            }
            
            updateNotification()
            Log.d(TAG, "暂停播放(淡出): $soundId")
            
        } catch (e: Exception) {
            Log.e(TAG, "暂停失败: $soundId", e)
        }
    }
    
    fun resumeSound(soundId: String) {
        try {
            if (OboeAudioEngine.isFadingOut(soundId)) {
                OboeAudioEngine.cancelFadeOut(soundId)
                Log.d(TAG, "取消淡出并恢复播放: $soundId")
            } else {
                OboeAudioEngine.setFadeDuration(soundId, 0.5f)
                OboeAudioEngine.resumeSound(soundId)
            }
            
            PlaybackStateManager.resumeSound(soundId)
            
            updatePlayingAudioIds()
            
            isServicePlaying = true
            
            updateNotification()
            Log.d(TAG, "恢复播放(淡入): $soundId")
            
        } catch (e: Exception) {
            Log.e(TAG, "恢复失败: $soundId", e)
        }
    }
    
    fun stopAllSounds() {
        OboeAudioEngine.stopAllSounds()
        
        PlaybackStateManager.clearAll()
        
        ScatteredPlayerManager.stopAll()
        
        isServicePlaying = false
        updatePlayingAudioIds()
        updateWakeLockState()
        updateNotification()
    }
    
    fun pauseAllSounds() {
        val playingSoundIds = PlaybackStateManager.getPlayingSounds()
        
        playingSoundIds.forEach { soundId ->
            pauseSound(soundId)
        }
        
        ScatteredPlayerManager.pauseAll()
        
        isServicePlaying = false
        updatePlayingAudioIds()
        updateNotification()
    }
    
    fun resumeAllSounds() {
        val pausedSoundIds = PlaybackStateManager.getAllSoundIds().filter { !PlaybackStateManager.isPlaying(it) }
        
        pausedSoundIds.forEach { soundId ->
            resumeSound(soundId)
        }
        
        ScatteredPlayerManager.resumeAll()
    }

    private fun restoreStreamAndResume() {
        val soundsToRestore = PlaybackStateManager.getAllSoundIds().filter { !PlaybackStateManager.isPlaying(it) }
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
            Log.e(TAG, "音频引擎重新初始化失败")
            return
        }

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
                    com.bicy.whitenoise.H3HO.SpatialAudioManager.applySpatialConfig(soundId)
                    com.bicy.whitenoise.H3HO.CreativeEffectManager.applyCreativeEffectConfig(soundId)
                    
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
            
            if (PlaybackStateManager.getPlayingSounds().isNotEmpty()) {
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
        OboeAudioEngine.setVolume(soundId, volume)
        ScatteredPlayerManager.updateTrackConfig(trackId = soundId, volume = volume)
    }
    
    fun getVolume(soundId: String): Float {
        return PlaybackStateManager.getVolume(soundId)
    }
    
    fun isPlaying(soundId: String): Boolean {
        return PlaybackStateManager.isPlaying(soundId)
    }
    
    fun isSoundPlaying(soundId: String): Boolean {
        return PlaybackStateManager.isPlaying(soundId)
    }
    
    fun getPlayingSounds(): Set<String> {
        return PlaybackStateManager.getPlayingSounds().toSet()
    }
    
    fun isLoaded(soundId: String): Boolean {
        return OboeAudioEngine.isLoaded(soundId)
    }
    
    fun setReverbConfig(soundId: String, config: ReverbConfig) {
        ReverbManager.setReverbEffect(soundId, config)
        PlaybackStateManager.updateReverbConfig(soundId, config)
        if (PlaybackStateManager.isPlaying(soundId)) {
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
        if (isServicePlaying) {
            pauseAllSounds()
        } else {
            resumeAllSounds()
        }
    }
    
    private fun handleStop() {
        stopAllSounds()
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
        if (PlaybackStateManager.getPlayingSounds().isNotEmpty()) {
            acquireWakeLock()
        } else {
            releaseWakeLock()
        }
    }
    
    fun reloadAllTracksWithNewEffectOrder() {
        val wasPlaying = PlaybackStateManager.getPlayingSounds()
        
        if (wasPlaying.isEmpty()) {
            return
        }
        
        Log.d(TAG, "重载所有音轨以应用新的效果顺序")
        
        val effectOrder = com.bicy.whitenoise.JwJY.NATg.ConfigStorage.getAudioEffectOrder()
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
