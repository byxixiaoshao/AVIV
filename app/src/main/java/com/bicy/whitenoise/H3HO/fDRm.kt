package com.bicy.whitenoise.H3HO

import android.content.Context
import com.bicy.whitenoise.JwJY.sBYh.kcFp.CreativeEffectConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.ScatteredAudioClipData
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialScatterRangeData
import com.bicy.whitenoise.y10p.AppLog
import com.bicy.whitenoise.y10p.DownloadManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

data class ScatteredTrackState(
    val trackId: String,
    val audioClips: List<ScatteredAudioClipData>,
    val minIntervalMs: Long = 3000,
    val maxIntervalMs: Long = 10000,
    val volume: Float = 1.0f,
    val spatialRange: SpatialScatterRangeData = SpatialScatterRangeData(),
    val spatialEnabled: Boolean = false,
    val overlayMode: Boolean = false,
    val isPlaying: Boolean = false,
    val currentClipId: String? = null
)

object ScatteredPlayerManager {
    
    private const val TAG = "ScatteredPlayerManager"
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val trackStates = ConcurrentHashMap<String, ScatteredTrackState>()
    private val trackJobs = ConcurrentHashMap<String, Job>()
    private val loadedClips = ConcurrentHashMap<String, String>()
    
    private val _trackStatesFlow = MutableStateFlow<Map<String, ScatteredTrackState>>(emptyMap())
    val trackStatesFlow: StateFlow<Map<String, ScatteredTrackState>> = _trackStatesFlow.asStateFlow()
    
    private var contextRef: WeakReference<Context>? = null
    
    fun init(context: Context) {
        contextRef = WeakReference(context.applicationContext)
        AppLog.d(TAG, "ScatteredPlayerManager initialized")
    }
    
    fun registerTrack(
        trackId: String,
        audioClips: List<ScatteredAudioClipData>,
        minIntervalMs: Long = 3000,
        maxIntervalMs: Long = 10000,
        volume: Float = 1.0f,
        spatialRange: SpatialScatterRangeData = SpatialScatterRangeData(),
        spatialEnabled: Boolean = false,
        overlayMode: Boolean = false
    ) {
        val state = ScatteredTrackState(
            trackId = trackId,
            audioClips = audioClips,
            minIntervalMs = minIntervalMs,
            maxIntervalMs = maxIntervalMs,
            volume = volume,
            spatialRange = spatialRange,
            spatialEnabled = spatialEnabled,
            overlayMode = overlayMode,
            isPlaying = false
        )
        trackStates[trackId] = state
        updateStateFlow()
        AppLog.d(TAG, "Registered track: $trackId with ${audioClips.size} clips")
    }
    
    fun unregisterTrack(trackId: String) {
        stopTrack(trackId)
        trackStates.remove(trackId)
        updateStateFlow()
        AppLog.d(TAG, "Unregistered track: $trackId")
    }
    
    fun updateTrackClips(trackId: String, audioClips: List<ScatteredAudioClipData>) {
        val currentState = trackStates[trackId] ?: return
        trackStates[trackId] = currentState.copy(audioClips = audioClips)
        updateStateFlow()
        AppLog.d(TAG, "Updated clips for track: $trackId, count: ${audioClips.size}")
    }
    
    fun updateTrackConfig(
        trackId: String,
        minIntervalMs: Long? = null,
        maxIntervalMs: Long? = null,
        volume: Float? = null,
        spatialRange: SpatialScatterRangeData? = null,
        spatialEnabled: Boolean? = null,
        overlayMode: Boolean? = null
    ) {
        val currentState = trackStates[trackId] ?: return
        trackStates[trackId] = currentState.copy(
            minIntervalMs = minIntervalMs ?: currentState.minIntervalMs,
            maxIntervalMs = maxIntervalMs ?: currentState.maxIntervalMs,
            volume = volume ?: currentState.volume,
            spatialRange = spatialRange ?: currentState.spatialRange,
            spatialEnabled = spatialEnabled ?: currentState.spatialEnabled,
            overlayMode = overlayMode ?: currentState.overlayMode
        )
        updateStateFlow()
        
        if (volume != null) {
            val currentClipId = currentState.currentClipId
            if (currentClipId != null && OboeAudioEngine.isPlaying(currentClipId)) {
                OboeAudioEngine.setVolume(currentClipId, volume)
            }
        }
    }
    
    fun startTrack(trackId: String) {
        val state = trackStates[trackId] ?: return
        
        if (state.audioClips.isEmpty()) {
            AppLog.w(TAG, "No audio clips for track: $trackId")
            return
        }
        
        if (trackJobs[trackId]?.isActive == true) {
            AppLog.d(TAG, "Track already playing: $trackId")
            return
        }
        
        trackStates[trackId] = state.copy(isPlaying = true)
        updateStateFlow()
        
        val job = scope.launch {
            playScatteredLoop(trackId)
        }
        trackJobs[trackId] = job
        
        AppLog.d(TAG, "Started track: $trackId")
    }
    
    fun stopTrack(trackId: String) {
        val state = trackStates[trackId]
        
        state?.currentClipId?.let { clipId ->
            OboeAudioEngine.stopSound(clipId)
            OboeAudioEngine.unloadSound(clipId)
            loadedClips.remove(clipId)
        }
        
        trackJobs[trackId]?.cancel()
        trackJobs.remove(trackId)
        
        if (state != null) {
            trackStates[trackId] = state.copy(isPlaying = false, currentClipId = null)
            updateStateFlow()
        }
        
        AppLog.d(TAG, "Stopped track: $trackId")
    }
    
    fun pauseTrack(trackId: String) {
        val state = trackStates[trackId] ?: return
        
        state.currentClipId?.let { clipId ->
            OboeAudioEngine.pauseSound(clipId)
        }
        
        trackStates[trackId] = state.copy(isPlaying = false)
        updateStateFlow()
        
        AppLog.d(TAG, "Paused track: $trackId")
    }
    
    fun resumeTrack(trackId: String) {
        val state = trackStates[trackId] ?: return
        
        if (state.isPlaying) return
        
        state.currentClipId?.let { clipId ->
            if (OboeAudioEngine.isLoaded(clipId)) {
                OboeAudioEngine.resumeSound(clipId)
                trackStates[trackId] = state.copy(isPlaying = true)
                updateStateFlow()
                AppLog.d(TAG, "Resumed track: $trackId (resuming existing clip)")
                return
            }
        }
        
        startTrack(trackId)
        AppLog.d(TAG, "Resumed track: $trackId (starting new)")
    }
    
    fun pauseAll() {
        trackStates.keys.forEach { trackId ->
            pauseTrack(trackId)
        }
        AppLog.d(TAG, "Paused all tracks")
    }
    
    fun resumeAll() {
        trackStates.keys.forEach { trackId ->
            val state = trackStates[trackId] ?: return@forEach
            if (state.audioClips.isNotEmpty()) {
                startTrack(trackId)
            }
        }
        AppLog.d(TAG, "Resumed all tracks")
    }
    
    fun stopAll() {
        trackJobs.values.forEach { it.cancel() }
        trackJobs.clear()
        
        trackStates.keys.forEach { trackId ->
            val state = trackStates[trackId] ?: return@forEach
            trackStates[trackId] = state.copy(isPlaying = false, currentClipId = null)
        }
        updateStateFlow()
        
        AppLog.d(TAG, "Stopped all tracks")
    }
    
    fun isTrackPlaying(trackId: String): Boolean {
        return trackStates[trackId]?.isPlaying ?: false
    }
    
    fun getTrackState(trackId: String): ScatteredTrackState? {
        return trackStates[trackId]
    }
    
    private suspend fun playScatteredLoop(trackId: String) {
        val state = trackStates[trackId] ?: return
        
        if (state.audioClips.isEmpty()) {
            AppLog.w(TAG, "No clips to play for track: $trackId")
            return
        }
        
        AppLog.d(TAG, "playScatteredLoop: Track $trackId has ${state.audioClips.size} clips: ${state.audioClips.map { it.id }}")
        
        while (currentCoroutineContext().isActive) {
            val currentState = trackStates[trackId] ?: break
            if (!currentState.isPlaying) {
                while (currentCoroutineContext().isActive) {
                    delay(100)
                    val checkState = trackStates[trackId] ?: break
                    if (checkState.isPlaying) break
                }
                continue
            }
            
            val clip = currentState.audioClips[ThreadLocalRandom.current().nextInt(currentState.audioClips.size)]
            AppLog.d(TAG, "playScatteredLoop: Selected clip ${clip.id} from ${currentState.audioClips.size} clips for track $trackId")
            val clipId = "${trackId}_${clip.id}"
            
            try {
                playClip(clipId, clip, currentState)
                
                trackStates[trackId]?.let { 
                    trackStates[trackId] = it.copy(currentClipId = clipId)
                }
                updateStateFlow()
                
                if (currentState.overlayMode) {
                    val interval = ThreadLocalRandom.current().nextLong(currentState.minIntervalMs, currentState.maxIntervalMs + 1)
                    AppLog.d(TAG, "Overlay mode: Starting interval ${interval}ms while clip plays for track: $trackId")
                    delay(interval)
                } else {
                    val duration = OboeAudioEngine.getDuration(clipId)
                    if (duration > 0) {
                        delay(duration)
                    } else {
                        delay(1000)
                    }
                    
                    OboeAudioEngine.stopSound(clipId)
                    OboeAudioEngine.unloadSound(clipId)
                    loadedClips.remove(clipId)
                    
                    trackStates[trackId]?.let { 
                        trackStates[trackId] = it.copy(currentClipId = null)
                    }
                    updateStateFlow()
                    
                    if (!currentCoroutineContext().isActive) break
                    
                    val latestStateForInterval = trackStates[trackId] ?: currentState
                    val interval = ThreadLocalRandom.current().nextLong(latestStateForInterval.minIntervalMs, latestStateForInterval.maxIntervalMs + 1)
                    AppLog.d(TAG, "Waiting ${interval}ms before next clip for track: $trackId")
                    delay(interval)
                }
                
            } catch (e: CancellationException) {
                OboeAudioEngine.stopSound(clipId)
                OboeAudioEngine.unloadSound(clipId)
                loadedClips.remove(clipId)
                throw e
            } catch (e: Exception) {
                AppLog.e(TAG, "Error playing clip: ${clip.id}", e)
            }
        }
    }
    
    private suspend fun playClip(clipId: String, clip: ScatteredAudioClipData, state: ScatteredTrackState) {
        val ctx = contextRef?.get() ?: throw IllegalStateException("Context not initialized")
        
        val filePath = if (clip.filePath.startsWith("http")) {
            val cachedFile = getCachedFile(ctx, clip.id)
            if (cachedFile == null || !cachedFile.exists()) {
                AppLog.w(TAG, "Clip not cached: ${clip.id}, skipping")
                return
            }
            cachedFile.absolutePath
        } else {
            clip.filePath
        }
        
        val file = File(filePath)
        if (!file.exists()) {
            AppLog.w(TAG, "Clip file not found: $filePath")
            return
        }
        
        OboeAudioEngine.loadSound(clipId, file.absolutePath)
        loadedClips[clipId] = file.absolutePath
        
        var retryCount = 0
        while (!OboeAudioEngine.isLoaded(clipId) && retryCount < 50) {
            delay(100)
            retryCount++
        }
        
        if (!OboeAudioEngine.isLoaded(clipId)) {
            AppLog.e(TAG, "Failed to load clip: ${clip.id}")
            return
        }
        
        val latestState = trackStates[state.trackId] ?: state
        
        OboeAudioEngine.setVolume(clipId, latestState.volume)
        OboeAudioEngine.setLooping(clipId, false)
        
        OboeAudioEngine.setSpatialEnabled(clipId, latestState.spatialEnabled)
        if (latestState.spatialEnabled) {
            val random = ThreadLocalRandom.current()
            val x = random.nextFloat() * (latestState.spatialRange.xMax - latestState.spatialRange.xMin) + latestState.spatialRange.xMin
            val y = random.nextFloat() * (latestState.spatialRange.yMax - latestState.spatialRange.yMin) + latestState.spatialRange.yMin
            val z = random.nextFloat() * (latestState.spatialRange.zMax - latestState.spatialRange.zMin) + latestState.spatialRange.zMin
            OboeAudioEngine.setSpatialFixedOffset(clipId, x, y, z, 1.0f)
        }
        
        val reverbConfig = ReverbManager.getConfig(latestState.trackId)
        AppLog.d(TAG, "playClip: Getting reverb config for track ${latestState.trackId}, config=$reverbConfig")
        if (reverbConfig != null) {
            AppLog.d(TAG, "playClip: Applying reverb config for clip $clipId, roomSize=${reverbConfig.roomSize}, insulation=${reverbConfig.insulation}")
            OboeAudioEngine.setEffectEnabled(clipId, true)
            OboeAudioEngine.setReverbParams(clipId, reverbConfig.roomSize, reverbConfig.damping, reverbConfig.wetLevel)
            OboeAudioEngine.setInsulation(clipId, reverbConfig.insulation)
            OboeAudioEngine.setReverbDecayTime(clipId, reverbConfig.decayTime)
            OboeAudioEngine.setReverbPreDelay(clipId, reverbConfig.preDelay)
            OboeAudioEngine.setReverbDryLevel(clipId, reverbConfig.dryLevel)
        } else {
            AppLog.w(TAG, "playClip: No reverb config found for track ${latestState.trackId}")
        }
        
        val creativeConfig = CreativeEffectManager.getConfig(latestState.trackId)
        if (creativeConfig != null) {
            OboeAudioEngine.setCreativeEffectIntensity(clipId, CreativeEffectType.LoFi, creativeConfig.loFi)
            OboeAudioEngine.setCreativeEffectIntensity(clipId, CreativeEffectType.EightBit, creativeConfig.eightBit)
            OboeAudioEngine.setCreativeEffectIntensity(clipId, CreativeEffectType.Underwater, creativeConfig.underwater)
            OboeAudioEngine.setCreativeEffectIntensity(clipId, CreativeEffectType.AlienSignal, creativeConfig.alienSignal)
            OboeAudioEngine.setCreativeEffectIntensity(clipId, CreativeEffectType.Megaphone, creativeConfig.megaphone)
        }
        
        OboeAudioEngine.setFadeDuration(clipId, 0.2f)
        OboeAudioEngine.playSound(clipId)
        
        AppLog.d(TAG, "Playing clip: ${clip.id} for track: ${state.trackId}")
    }
    
    private fun getCachedFile(context: Context, soundId: String): File? {
        return DownloadManager.getScatteredCachedFile(context, soundId)
    }
    
    private fun updateStateFlow() {
        _trackStatesFlow.value = trackStates.toMap()
    }
    
    fun release() {
        stopAll()
        scope.cancel()
        trackStates.clear()
        loadedClips.clear()
        contextRef = null
        AppLog.d(TAG, "ScatteredPlayerManager released")
    }
}
