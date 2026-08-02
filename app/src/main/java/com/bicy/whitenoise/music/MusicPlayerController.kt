package com.bicy.whitenoise.music

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.audio.OboeAudioEngine
import com.bicy.whitenoise.equalizer.EqMode
import com.bicy.whitenoise.equalizer.PresetStorage
import com.bicy.whitenoise.servies.MusicService
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.ui.components.toast.ToastManager
import com.bicy.whitenoise.storage.music.MusicStorage
import com.bicy.whitenoise.storage.music.MusicPlaybackState
import com.bicy.whitenoise.utils.UsageStatsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bicy.whitenoise.music.MusicLibraryPart.*

enum class MusicRepeatMode {
    OFF,
    ALL,
    ONE
}

enum class MusicShuffleMode {
    OFF,
    ON
}

data class MusicPlayerState(
    val currentTrack: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val repeatMode: MusicRepeatMode = MusicRepeatMode.OFF,
    val shuffleMode: MusicShuffleMode = MusicShuffleMode.OFF,
    val playlist: List<MusicTrack> = emptyList(),
    val playlistIndex: Int = -1
)

object MusicPlayerController {
    
    private const val TAG = "MusicPlayerController"
    private const val PROGRESS_UPDATE_INTERVAL = 100L
    private const val SKIP_DEBOUNCE_MS = 300L
    private const val SAVE_STATE_INTERVAL = 50
    
    private lateinit var context: Context
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val _state = MutableStateFlow(MusicPlayerState())
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()
    
    private var progressJob: Job? = null
    private var isInitialized = false
    private var wasPlayingBeforeFocusLoss = false
    private var wasPlayingBeforeStreamDisconnect = false
    private var lastSkipTime = 0L
    private var isLoading = false
    private var pendingPlay = false
    private var progressUpdateCount = 0
    
    val currentTrack: MusicTrack? get() = _state.value.currentTrack
    val isPlaying: Boolean get() = _state.value.isPlaying
    val position: Long get() = _state.value.position
    val duration: Long get() = _state.value.duration
    
    fun init(context: Context) {
        Log.w(TAG, "init() called, isInitialized=$isInitialized")
        if (isInitialized) return
        this.context = context.applicationContext
        MusicCacheManager.init(context)
        MusicStorage.init()
        
        val limiterConfig = MusicStorage.getLimiterConfig()
        OboeAudioEngine.setGlobalLimiterConfig(
            enabled = limiterConfig.enabled,
            limitEqualizer = limiterConfig.limitEqualizer,
            limitEffects = limiterConfig.limitEffects,
            limitReverb = limiterConfig.limitReverb,
            limitSpatial = limiterConfig.limitSpatial,
            threshold = limiterConfig.threshold,
            attack = limiterConfig.attack,
            release = limiterConfig.release
        )
        Log.d(TAG, "限幅器配置已应用: enabled=${limiterConfig.enabled}")
        
        MusicService.onAudioStreamRestarted = {
            Log.w(TAG, "onAudioStreamRestarted callback triggered")
            handleAudioStreamRestarted()
        }
        
        MusicService.onAudioStreamDisconnect = {
            Log.w(TAG, "onAudioStreamDisconnect callback triggered, isPlaying=${_state.value.isPlaying}")
            if (_state.value.isPlaying) {
                wasPlayingBeforeStreamDisconnect = true
                Log.w(TAG, "音频流断开，保存播放状态: wasPlayingBeforeStreamDisconnect=true")
            }
        }

        MusicService.onAudioFocusLost = {
            Log.w(TAG, "onAudioFocusLost callback triggered, isPlaying=${_state.value.isPlaying}")
            wasPlayingBeforeFocusLoss = _state.value.isPlaying
            _state.value = _state.value.copy(isPlaying = false)
        }
        
        isInitialized = true
        Log.w(TAG, "MusicPlayerController initialized, onAudioStreamRestarted callback set")
    }
    
    fun restoreLastPlayback(): Boolean {
        val playbackState = MusicStorage.getPlaybackState()
        val trackId = playbackState.trackId ?: return false
        
        val allTracks = MusicLibrary.tracks.value
        if (allTracks.isEmpty() && playbackState.onlineTracksJson == "{}") return false

        // 解析保存的在线曲目数据
        val onlineTrackData = parseOnlineTracks(playbackState.onlineTracksJson)
        
        // 查找最后播放的曲目：先在本地库找，再在在线数据中找
        val lastTrack = allTracks.find { it.id == trackId }
            ?: onlineTrackData[trackId]
        if (lastTrack == null) {
            Log.d(TAG, "恢复失败: 找不到 trackId=$trackId")
            return false
        }
        
        _state.value = _state.value.copy(
            repeatMode = when (playbackState.repeatMode) {
                "ALL" -> MusicRepeatMode.ALL
                "ONE" -> MusicRepeatMode.ONE
                else -> MusicRepeatMode.OFF
            },
            shuffleMode = if (playbackState.shuffleMode == "ON") MusicShuffleMode.ON else MusicShuffleMode.OFF
        )
        
        if (playbackState.playlistTrackIds.isNotEmpty()) {
            val savedPlaylist = playbackState.playlistTrackIds.mapNotNull { id ->
                allTracks.find { it.id == id } ?: onlineTrackData[id]
            }
            if (savedPlaylist.isNotEmpty()) {
                val validIndex = playbackState.playlistIndex.coerceIn(0, savedPlaylist.size - 1)
                setPlaylist(savedPlaylist, validIndex)
            } else {
                setPlaylist(allTracks, playbackState.playlistIndex.coerceAtLeast(0))
            }
        } else {
            setPlaylist(allTracks, playbackState.playlistIndex.coerceAtLeast(0))
        }
        
        if (playbackState.position > 0) {
            seekTo(playbackState.position)
        }
        
        restoreEffectIntensities()
        
        Log.d(TAG, "恢复上次播放: ${lastTrack.title}, 位置: ${playbackState.position}")
        return true
    }

    private fun parseOnlineTracks(json: String): Map<String, MusicTrack> {
        if (json == "{}") return emptyMap()
        return try {
            val obj = org.json.JSONObject(json)
            val result = mutableMapOf<String, MusicTrack>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val data = obj.getJSONObject(id)
                result[id] = MusicTrack(
                    id = id,
                    path = data.optString("path", ""),
                    title = data.getString("title"),
                    artist = data.optString("artist").takeIf { it.isNotEmpty() },
                    album = data.optString("album").takeIf { it.isNotEmpty() },
                    duration = data.optLong("duration", 0),
                    isOnline = true,
                    streamUrl = data.optString("streamUrl").takeIf { it.isNotEmpty() },
                    source = data.optString("source").takeIf { it.isNotEmpty() }
                )
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse onlineTracksJson", e)
            emptyMap()
        }
    }
    
    private fun restoreEffectIntensities() {
        applyEffectIntensitiesToCurrentTrack()
        Log.d(TAG, "恢复效果强度")
    }
    
    fun saveCurrentPlaybackState() {
        val track = _state.value.currentTrack ?: return

        val playlist = _state.value.playlist
        val onlineTracks = playlist.filter { it.isOnline }
        val onlineTracksJson = serializeOnlineTracks(onlineTracks)

        MusicStorage.savePlaybackState(
            MusicPlaybackState(
                trackId = track.id,
                trackTitle = track.title,
                trackArtist = track.artist,
                trackDuration = track.duration,
                position = _state.value.position,
                isPlaying = _state.value.isPlaying,
                repeatMode = _state.value.repeatMode.name,
                shuffleMode = _state.value.shuffleMode.name,
                playlistIndex = _state.value.playlistIndex,
                playlistTrackIds = playlist.map { it.id },
                onlineTracksJson = onlineTracksJson
            )
        )
    }

    private fun serializeOnlineTracks(tracks: List<MusicTrack>): String {
        if (tracks.isEmpty()) return "{}"
        val obj = org.json.JSONObject()
        tracks.forEach { track ->
            val data = org.json.JSONObject().apply {
                put("title", track.title)
                track.artist?.let { put("artist", it) }
                track.album?.let { put("album", it) }
                put("duration", track.duration)
                put("path", track.path)
                track.streamUrl?.let { put("streamUrl", it) }
                track.source?.let { put("source", it) }
            }
            obj.put(track.id, data)
        }
        return obj.toString()
    }
    
    fun setPlaylist(tracks: List<MusicTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        
        val validIndex = startIndex.coerceIn(0, tracks.size - 1)
        val targetTrack = tracks[validIndex]
        
        val shuffledPlaylist = if (_state.value.shuffleMode == MusicShuffleMode.ON) {
            val shuffled = tracks.shuffled()
            // 确保目标歌曲在正确的位置
            val targetIndex = shuffled.indexOf(targetTrack)
            if (targetIndex >= 0) {
                _state.value = _state.value.copy(
                    playlist = shuffled,
                    playlistIndex = targetIndex
                )
                loadTrack(shuffled[targetIndex])
            } else {
                // 如果找不到目标歌曲（不应该发生），使用第一个
                _state.value = _state.value.copy(
                    playlist = shuffled,
                    playlistIndex = 0
                )
                loadTrack(shuffled[0])
            }
        } else {
            _state.value = _state.value.copy(
                playlist = tracks,
                playlistIndex = validIndex
            )
            loadTrack(targetTrack)
        }
    }
    
    private fun loadTrack(track: MusicTrack) {
        isLoading = true
        val soundId = MusicCacheManager.getSoundId(track.id)
        
        stopAllMusicSounds()
        
        MusicCacheManager.setCurrentPlaying(track.id)
        
        if (MusicCacheManager.isTrackLoaded(track.id)) {
            _state.value = _state.value.copy(
                currentTrack = track,
                position = 0,
                duration = track.duration,
                isPlaying = false
            )
            isLoading = false
            applyEffectIntensitiesToCurrentTrack()
            if (pendingPlay) {
                pendingPlay = false
                play()
            }
            preloadNextTrack()
            Log.d(TAG, "Track already cached: ${track.title}")
            return
        }
        
        _state.value = _state.value.copy(
            currentTrack = track,
            position = 0,
            duration = track.duration,
            isPlaying = false
        )
        
        MusicCacheManager.loadTrack(track) { success ->
            scope.launch {
                if (success) {
                    var retries = 0
                    while (retries < 50 && !OboeAudioEngine.isLoaded(soundId)) {
                        delay(100)
                        retries++
                    }
                    
                    if (OboeAudioEngine.isLoaded(soundId)) {
                        val actualDuration = OboeAudioEngine.getDuration(soundId)
                        if (actualDuration > 0 && _state.value.currentTrack?.id == track.id) {
                            _state.value = _state.value.copy(duration = actualDuration)
                            Log.d(TAG, "Updated duration for ${track.title}: ${actualDuration}ms")
                        }
                    }
                }
                isLoading = false
                applyEffectIntensitiesToCurrentTrack()
                if (pendingPlay) {
                    pendingPlay = false
                    withContext(Dispatchers.Main) {
                        play()
                    }
                }
                preloadNextTrack()
            }
        }
        
        Log.d(TAG, "Track loading: ${track.title}")
    }
    
    private fun applyEffectIntensitiesToCurrentTrack() {
        val effectIntensities = MusicStorage.getEffectIntensities()
        val track = _state.value.currentTrack ?: return
        val soundId = MusicCacheManager.getSoundId(track.id)
        
        OboeAudioEngine.setCreativeEffectIntensity(soundId, 400, effectIntensities.loFi)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, 401, effectIntensities.eightBit)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, 402, effectIntensities.underwater)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, 403, effectIntensities.alienSignal)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, 404, effectIntensities.megaphone)
        // pitch / speed 由 SoundTouch 独立管线处理（time-stretch + pitch-shift 解耦）
        OboeAudioEngine.setPitchShift(soundId, effectIntensities.pitch)
        OboeAudioEngine.setPlaybackSpeed(soundId, effectIntensities.speed)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, 502, effectIntensities.hifi)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, 503, effectIntensities.distortion)
        // 音质增强类效果器：立体声扩展 / 虚拟低音 / 多段压缩
        // 之前缺失这三行，导致音乐播放时虚拟低频与多段压缩无效果（构造函数默认值会被后续 setEnabled(false) 覆盖）
        OboeAudioEngine.setCreativeEffectIntensity(soundId, 700, effectIntensities.stereoWidener)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, 701, effectIntensities.virtualBass)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, 702, effectIntensities.multibandCompressor)
        
        val reverbConfig = MusicStorage.getReverbConfig()
        OboeAudioEngine.setReverbParams(soundId, reverbConfig.roomSize, reverbConfig.damping, reverbConfig.wetLevel)
        OboeAudioEngine.setInsulation(soundId, reverbConfig.insulation)
        OboeAudioEngine.setReverbDecayTime(soundId, reverbConfig.decayTime)
        OboeAudioEngine.setReverbPreDelay(soundId, reverbConfig.preDelay * 1000f)
        OboeAudioEngine.setReverbDryLevel(soundId, reverbConfig.dryLevel)
        OboeAudioEngine.setEffectEnabled(soundId, true)
        
        val volume = MusicStorage.getVolume()
        OboeAudioEngine.setVolume(soundId, volume)

        val eqCurve = if (PresetStorage.eqMode.value == EqMode.GLOBAL) {
            PresetStorage.getGlobalCurve()
        } else {
            PresetStorage.getTrackCurve(soundId)
        }
        val sorted = eqCurve.points.sortedBy { it.frequencyHz }
        val freqs = FloatArray(sorted.size) { sorted[it].frequencyHz }
        val gains = FloatArray(sorted.size) { sorted[it].gainDb }
        val types = IntArray(sorted.size) { sorted[it].filterType.nativeValue }
        val qs = FloatArray(sorted.size) { sorted[it].qOverride }
        val cIns = IntArray(sorted.size) { sorted[it].curveIn.nativeValue }
        val cOuts = IntArray(sorted.size) { sorted[it].curveOut.nativeValue }
        OboeAudioEngine.setEqualizerCurve(soundId, freqs, gains, types, qs, cIns, cOuts)
        val autoEqEnabled = ConfigStorage.isAutoEqEnabled()
        // 手动 EQ 模式下尊重 Bypass 状态；AutoEQ 模式下 Bypass 不生效（EQ 始终启用）
        OboeAudioEngine.setEqEnabled(soundId, autoEqEnabled || !ConfigStorage.isEqBypassEnabled())

        if (autoEqEnabled) {
            // Static compensation mode: use EQ with compensation curve gains
            val intensity = ConfigStorage.getAutoEqIntensity()
            val speakerPreset = ConfigStorage.getSpeakerPreset()
            OboeAudioEngine.setAutoEqIntensity(soundId, intensity)
            OboeAudioEngine.setSpeakerPreset(soundId, speakerPreset)
            OboeAudioEngine.setAutoEqBassBias(soundId, ConfigStorage.getAutoEqBassBias())
            OboeAudioEngine.setAutoEqMidBias(soundId, ConfigStorage.getAutoEqMidBias())
            OboeAudioEngine.setAutoEqTrebleBias(soundId, ConfigStorage.getAutoEqTrebleBias())
            OboeAudioEngine.setAutoEqBrightnessTarget(soundId, ConfigStorage.getAutoEqProBrightnessTarget())
            OboeAudioEngine.setAutoEqLoudnessTarget(soundId, ConfigStorage.getAutoEqProLoudnessTarget())
            // Pro parameters
            OboeAudioEngine.setAutoEqAttack(soundId, ConfigStorage.getAutoEqProAttack())
            OboeAudioEngine.setAutoEqRelease(soundId, ConfigStorage.getAutoEqProRelease())
            OboeAudioEngine.setAutoEqMaxSlope(soundId, ConfigStorage.getAutoEqProMaxSlope())
            OboeAudioEngine.setAutoEqMaxBoost(soundId, ConfigStorage.getAutoEqProMaxBoost())
            OboeAudioEngine.setAutoEqMaxCut(soundId, ConfigStorage.getAutoEqProMaxCut())
            OboeAudioEngine.setAutoEqCouplingCoeff(soundId, ConfigStorage.getAutoEqProCouplingCoeff())
            OboeAudioEngine.setAutoEqHysteresis(soundId, ConfigStorage.getAutoEqProHysteresisDb())
            OboeAudioEngine.setAutoEqDynamicQEnabled(soundId, ConfigStorage.getAutoEqProDynamicQEnabled())
            // Band configuration
            OboeAudioEngine.setAutoEqBandCount(soundId, ConfigStorage.getAutoEqBandCount())
            OboeAudioEngine.setAutoEqBandRatios(soundId, ConfigStorage.getAutoEqLowRatio(), ConfigStorage.getAutoEqMidRatio())
            val filePath = MusicCacheManager.getFilePath(soundId)
            Log.d(TAG, "AutoEQ triggered on track load: $soundId, filePath=$filePath")
            OboeAudioEngine.setAutoEqEnabled(soundId, true, filePath ?: "")
            // Enable EQ to apply the compensation curve
            OboeAudioEngine.setEqEnabled(soundId, true)
        }
        
        val limiterConfig = MusicStorage.getLimiterConfig()
        OboeAudioEngine.setEqLimiterEnabled(soundId, limiterConfig.limitEqualizer)
        OboeAudioEngine.setLimitEffectsEnabled(soundId, limiterConfig.limitEffects)
        OboeAudioEngine.setLimitReverbEnabled(soundId, limiterConfig.limitReverb)
        OboeAudioEngine.setLimitSpatialEnabled(soundId, limiterConfig.limitSpatial)
        
        val spatialConfig = MusicStorage.getSpatialAudioConfig()
        OboeAudioEngine.setSpatialEnabled(soundId, spatialConfig.enabled)
        OboeAudioEngine.setSpatialOffsetType(soundId, spatialConfig.offsetType)
        OboeAudioEngine.setSpatialFixedOffset(soundId, spatialConfig.fixedLeftRight, spatialConfig.fixedUpDown, spatialConfig.fixedFrontBack, spatialConfig.fixedMultiplier)
        OboeAudioEngine.setSpatialSurroundParams(soundId, spatialConfig.surroundMode, spatialConfig.surroundRadius, spatialConfig.surroundSpeed)
        OboeAudioEngine.setSpatialRandomParams(soundId, spatialConfig.randomMaxDistance, spatialConfig.randomMinDistance, spatialConfig.randomValue, spatialConfig.randomSpeed)
        
        val effectOrder = ConfigStorage.getAudioEffectOrder()
        val orderIntArray = effectOrder.map { 
            when (it) {
                "spatial" -> 0
                "reverb" -> 1
                "equalizer" -> 2
                "quality" -> 3
                else -> 0
            }
        }.toIntArray()
        OboeAudioEngine.setEffectOrder(soundId, orderIntArray)
        
        Log.d(TAG, "应用效果强度和混响到新音轨")
    }
    
    private fun preloadNextTrack() {
        val playlist = _state.value.playlist
        val currentIndex = _state.value.playlistIndex
        
        if (playlist.isEmpty() || currentIndex < 0) return
        
        val nextIndex = if (currentIndex + 1 < playlist.size) currentIndex + 1 else 0
        val nextTrack = playlist[nextIndex]
        
        if (!MusicCacheManager.isTrackLoaded(nextTrack.id)) {
            MusicCacheManager.preloadNextTrack(nextTrack)
        }
    }
    
    private fun stopAllMusicSounds() {
        _state.value.playlist.forEach { existingTrack ->
            val existingSoundId = MusicCacheManager.getSoundId(existingTrack.id)
            if (OboeAudioEngine.isLoaded(existingSoundId)) {
                OboeAudioEngine.stopSound(existingSoundId)
            }
        }
        
        _state.value.currentTrack?.let { currentTrack ->
            val currentSoundId = MusicCacheManager.getSoundId(currentTrack.id)
            if (OboeAudioEngine.isLoaded(currentSoundId)) {
                OboeAudioEngine.stopSound(currentSoundId)
            }
        }
    }
    
    fun play() {
        val track = _state.value.currentTrack ?: return
        val soundId = MusicCacheManager.getSoundId(track.id)
        
        if (!OboeAudioEngine.isLoaded(soundId)) {
            pendingPlay = true
            return
        }
        
        if (isLoading) {
            pendingPlay = true
            return
        }
        
        if (OboeAudioEngine.isFadingOut(soundId)) {
            OboeAudioEngine.cancelFadeOut(soundId)
            Log.d(TAG, "取消淡出并恢复播放: ${track.title}")
        } else {
            OboeAudioEngine.setFadeDuration(soundId, 0.5f)
            OboeAudioEngine.setLooping(soundId, _state.value.repeatMode == MusicRepeatMode.ONE)
            OboeAudioEngine.playSound(soundId)
        }
        
        _state.value = _state.value.copy(isPlaying = true)
        startProgressUpdates()
        
        UsageStatsManager.onMusicStart()
        
        Log.d(TAG, "Playing: ${track.title}")
    }
    
    fun pause() {
        val track = _state.value.currentTrack ?: return
        val soundId = MusicCacheManager.getSoundId(track.id)
        
        OboeAudioEngine.setFadeDuration(soundId, 0.5f)
        OboeAudioEngine.pauseSound(soundId)
        
        _state.value = _state.value.copy(isPlaying = false)
        stopProgressUpdates()
        
        UsageStatsManager.onMusicStop()
        
        Log.d(TAG, "Paused (fading out): ${track.title}")
    }
    
    fun playPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }
    
    fun stop() {
        val track = _state.value.currentTrack ?: return
        val soundId = MusicCacheManager.getSoundId(track.id)
        
        OboeAudioEngine.stopSound(soundId)
        
        _state.value = _state.value.copy(
            isPlaying = false,
            position = 0
        )
        stopProgressUpdates()
        
        if (_state.value.isPlaying) {
            UsageStatsManager.onMusicStop()
        }
        
        Log.d(TAG, "Stopped: ${track.title}")
    }
    
    private fun handleAudioStreamRestarted() {
        if (isLoading) {
            Log.w(TAG, "音频流重启，但正在加载中，跳过")
            return
        }
        
        val track = _state.value.currentTrack
        val wasPlaying = _state.value.isPlaying || wasPlayingBeforeFocusLoss || wasPlayingBeforeStreamDisconnect
        val savedPosition = _state.value.position

        wasPlayingBeforeFocusLoss = false
        wasPlayingBeforeStreamDisconnect = false
        
        if (track == null) {
            Log.w(TAG, "音频流重启，但没有当前音轨")
            return
        }
        
        isLoading = true
        stopAllMusicSounds()
        Log.w(TAG, "音频流重启，恢复播放: ${track.title}, 位置: $savedPosition, 正在播放: $wasPlaying, isPlaying=${_state.value.isPlaying}, wasPlayingBeforeFocusLoss=$wasPlayingBeforeFocusLoss")
        
        val soundId = MusicCacheManager.getSoundId(track.id)
        
        MusicCacheManager.loadTrack(track) { success ->
            scope.launch {
                if (success) {
                    var retries = 0
                    while (retries < 50 && !OboeAudioEngine.isLoaded(soundId)) {
                        delay(100)
                        retries++
                    }
                    
                    if (OboeAudioEngine.isLoaded(soundId)) {
                        Log.w(TAG, "音频流重启后音轨加载成功: ${track.title}")
                        applyEffectIntensitiesToCurrentTrack()
                        
                        if (savedPosition > 0) {
                            OboeAudioEngine.seekTo(soundId, savedPosition)
                        }
                        
                        if (wasPlaying) {
                            OboeAudioEngine.setFadeDuration(soundId, 0.3f)
                            OboeAudioEngine.setLooping(soundId, _state.value.repeatMode == MusicRepeatMode.ONE)
                            OboeAudioEngine.playSound(soundId)
                            startProgressUpdates()
                            _state.value = _state.value.copy(isPlaying = true)
                            Log.w(TAG, "音频流重启后恢复播放成功")
                        }
                        
                        preloadNextTrack()
                    } else {
                        Log.e(TAG, "音频流重启后加载音轨失败: isLoaded=false after $retries retries")
                        _state.value = _state.value.copy(isPlaying = false)
                    }
                } else {
                    Log.e(TAG, "音频流重启后加载音轨失败: loadTrack returned false")
                    _state.value = _state.value.copy(isPlaying = false)
                }
                isLoading = false
            }
        }
    }
    
    fun next() {
        val now = System.currentTimeMillis()
        if (now - lastSkipTime < SKIP_DEBOUNCE_MS || isLoading) return
        lastSkipTime = now
        
        val playlist = _state.value.playlist
        if (playlist.isEmpty()) return
        
        val wasPlaying = _state.value.isPlaying
        
        val currentIndex = _state.value.playlistIndex
        var nextIndex = currentIndex + 1
        
        if (nextIndex >= playlist.size) {
            if (_state.value.repeatMode == MusicRepeatMode.ALL) {
                nextIndex = 0
            } else {
                stop()
                return
            }
        }
        
        _state.value = _state.value.copy(playlistIndex = nextIndex)
        loadTrack(playlist[nextIndex])
        
        if (wasPlaying) {
            play()
        }
    }
    
    fun previous() {
        val now = System.currentTimeMillis()
        if (now - lastSkipTime < SKIP_DEBOUNCE_MS || isLoading) return
        lastSkipTime = now
        
        val playlist = _state.value.playlist
        if (playlist.isEmpty()) return
        
        val wasPlaying = _state.value.isPlaying
        
        val currentIndex = _state.value.playlistIndex
        var prevIndex = currentIndex - 1
        
        if (prevIndex < 0) {
            if (_state.value.repeatMode == MusicRepeatMode.ALL) {
                prevIndex = playlist.size - 1
            } else {
                seekTo(0)
                return
            }
        }
        
        _state.value = _state.value.copy(playlistIndex = prevIndex)
        loadTrack(playlist[prevIndex])
        
        if (wasPlaying) {
            play()
        }
    }
    
    fun seekTo(positionMs: Long) {
        val track = _state.value.currentTrack ?: return
        val soundId = MusicCacheManager.getSoundId(track.id)
        
        OboeAudioEngine.seekTo(soundId, positionMs)
        _state.value = _state.value.copy(position = positionMs)
    }
    
    fun setVolume(volume: Float) {
        val track = _state.value.currentTrack ?: return
        val soundId = MusicCacheManager.getSoundId(track.id)
        
        OboeAudioEngine.setVolume(soundId, volume.coerceIn(0f, 1f))
    }
    
    fun toggleRepeatMode() {
        val newMode = when (_state.value.repeatMode) {
            MusicRepeatMode.OFF -> MusicRepeatMode.ALL
            MusicRepeatMode.ALL -> MusicRepeatMode.ONE
            MusicRepeatMode.ONE -> MusicRepeatMode.OFF
        }
        
        _state.value = _state.value.copy(repeatMode = newMode)
        
        val track = _state.value.currentTrack
        if (track != null) {
            val soundId = MusicCacheManager.getSoundId(track.id)
            OboeAudioEngine.setLooping(soundId, newMode == MusicRepeatMode.ONE)
        }
        
        Log.d(TAG, "Repeat mode: $newMode")
        ToastManager.info(
            when (newMode) {
                MusicRepeatMode.OFF -> "顺序播放"
                MusicRepeatMode.ALL -> "列表循环"
                MusicRepeatMode.ONE -> "单曲循环"
            }
        )
    }
    
    fun toggleShuffleMode() {
        val newMode = if (_state.value.shuffleMode == MusicShuffleMode.OFF) {
            MusicShuffleMode.ON
        } else {
            MusicShuffleMode.OFF
        }
        
        val currentTrack = _state.value.currentTrack
        val currentPlaylist = _state.value.playlist
        
        if (newMode == MusicShuffleMode.ON && currentTrack != null) {
            val shuffledPlaylist = currentPlaylist.shuffled()
            val newIndex = shuffledPlaylist.indexOf(currentTrack)
            
            _state.value = _state.value.copy(
                shuffleMode = newMode,
                playlist = shuffledPlaylist,
                playlistIndex = if (newIndex >= 0) newIndex else 0
            )
        } else {
            _state.value = _state.value.copy(shuffleMode = newMode)
        }
        
        Log.d(TAG, "Shuffle mode: $newMode")
        ToastManager.info(if (newMode == MusicShuffleMode.ON) "随机播放" else "顺序播放")
    }
    
    fun playTrack(track: MusicTrack) {
        setPlaylist(listOf(track), 0)
        play()
    }
    
    fun addToQueue(track: MusicTrack) {
        val currentPlaylist = _state.value.playlist.toMutableList()
        currentPlaylist.add(track)
        _state.value = _state.value.copy(playlist = currentPlaylist)
        ToastManager.success("「${track.title}」已添加到队列")
    }
    
    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                updateProgress()
                delay(PROGRESS_UPDATE_INTERVAL)
            }
        }
    }
    
    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }
    
    private fun updateProgress() {
        val track = _state.value.currentTrack ?: return
        val soundId = MusicCacheManager.getSoundId(track.id)
        
        val position = OboeAudioEngine.getPosition(soundId)
        _state.value = _state.value.copy(position = position)
        
        progressUpdateCount++
        if (progressUpdateCount >= SAVE_STATE_INTERVAL) {
            progressUpdateCount = 0
            saveCurrentPlaybackState()
        }
        
        if (!OboeAudioEngine.isPlaying(soundId) && _state.value.isPlaying) {
            onTrackCompleted()
        }
    }
    
    private fun onTrackCompleted() {
        when (_state.value.repeatMode) {
            MusicRepeatMode.ONE -> {
                seekTo(0)
                play()
            }
            MusicRepeatMode.ALL, MusicRepeatMode.OFF -> {
                next()
            }
        }
    }
    
    fun reloadCurrentTrackWithNewEffectOrder() {
        val track = _state.value.currentTrack ?: return
        val soundId = MusicCacheManager.getSoundId(track.id)
        
        if (!OboeAudioEngine.isLoaded(soundId)) {
            return
        }
        
        val wasPlaying = _state.value.isPlaying
        val currentPosition = _state.value.position
        
        Log.d(TAG, "重载当前音乐音轨以应用新的效果顺序")
        
        val effectOrder = ConfigStorage.getAudioEffectOrder()
        val orderIntArray = effectOrder.map { 
            when (it) {
                "spatial" -> 0
                "reverb" -> 1
                "equalizer" -> 2
                "quality" -> 3
                else -> 0
            }
        }.toIntArray()
        
        OboeAudioEngine.setEffectOrder(soundId, orderIntArray)
        
        Log.d(TAG, "已为音乐音轨应用新的效果顺序")
    }
    
    fun release() {
        stop()
        MusicCacheManager.clearAll()
        _state.value = MusicPlayerState()
        isInitialized = false
    }
}