package com.bicy.whitenoise.audio.ScatteredPlayerManagerPart

import android.content.Context
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.CreativeEffectConfig
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.ScatteredAudioClipData
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SpatialScatterRangeData
import com.bicy.whitenoise.service.AnomalyType
import com.bicy.whitenoise.service.MemoryLockService
import com.bicy.whitenoise.utils.AppLog
import com.bicy.whitenoise.utils.DownloadManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import com.bicy.whitenoise.audio.*

object ScatteredPlayerManager {
    private const val TAG = "ScatteredPlayerManager"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val trackStates = ConcurrentHashMap<String, ScatteredTrackState>()
    private val trackJobs = ConcurrentHashMap<String, Job>()
    private val loadedClips = ConcurrentHashMap<String, String>()
    private val _trackStatesFlow = MutableStateFlow<Map<String, ScatteredTrackState>>(emptyMap())
    val trackStatesFlow: StateFlow<Map<String, ScatteredTrackState>> = _trackStatesFlow.asStateFlow()
    private var contextRef: WeakReference<Context>? = null

    fun init(context: Context) { contextRef = WeakReference(context.applicationContext); AppLog.d(TAG, "ScatteredPlayerManager initialized") }

    fun registerTrack(trackId: String, audioClips: List<ScatteredAudioClipData>, minIntervalMs: Long = 3000, maxIntervalMs: Long = 10000, volume: Float = 1.0f, spatialRange: SpatialScatterRangeData = SpatialScatterRangeData(), spatialEnabled: Boolean = false, overlayMode: Boolean = false) {
        trackStates[trackId] = ScatteredTrackState(trackId = trackId, audioClips = audioClips, minIntervalMs = minIntervalMs, maxIntervalMs = maxIntervalMs, volume = volume, spatialRange = spatialRange, spatialEnabled = spatialEnabled, overlayMode = overlayMode, isPlaying = false); updateStateFlow()
    }

    fun unregisterTrack(trackId: String) { stopTrack(trackId); trackStates.remove(trackId); updateStateFlow() }
    fun updateTrackClips(trackId: String, audioClips: List<ScatteredAudioClipData>) { val cs = trackStates[trackId] ?: return; trackStates[trackId] = cs.copy(audioClips = audioClips); updateStateFlow() }

    fun updateTrackConfig(trackId: String, minIntervalMs: Long? = null, maxIntervalMs: Long? = null, volume: Float? = null, spatialRange: SpatialScatterRangeData? = null, spatialEnabled: Boolean? = null, overlayMode: Boolean? = null) {
        val cs = trackStates[trackId] ?: return
        trackStates[trackId] = cs.copy(minIntervalMs = minIntervalMs ?: cs.minIntervalMs, maxIntervalMs = maxIntervalMs ?: cs.maxIntervalMs, volume = volume ?: cs.volume, spatialRange = spatialRange ?: cs.spatialRange, spatialEnabled = spatialEnabled ?: cs.spatialEnabled, overlayMode = overlayMode ?: cs.overlayMode)
        updateStateFlow()
        if (volume != null) { cs.currentClipId?.let { if (OboeAudioEngine.isPlaying(it)) OboeAudioEngine.setVolume(it, volume) } }
    }

    fun startTrack(trackId: String) { val s = trackStates[trackId] ?: return; if (s.audioClips.isEmpty()) return; if (trackJobs[trackId]?.isActive == true) return; trackStates[trackId] = s.copy(isPlaying = true); updateStateFlow(); trackJobs[trackId] = scope.launch { playScatteredLoop(trackId) } }

    fun stopTrack(trackId: String) { val s = trackStates[trackId]; s?.currentClipId?.let { OboeAudioEngine.stopSound(it); OboeAudioEngine.unloadSound(it); loadedClips.remove(it) }; trackJobs[trackId]?.cancel(); trackJobs.remove(trackId); if(s != null){ trackStates[trackId] = s.copy(isPlaying = false, currentClipId = null); updateStateFlow() } }

    fun pauseTrack(trackId: String) { val s = trackStates[trackId] ?: return; s.currentClipId?.let { OboeAudioEngine.pauseSound(it) }; trackStates[trackId] = s.copy(isPlaying = false); updateStateFlow() }

    fun resumeTrack(trackId: String) { val s = trackStates[trackId] ?: return; if(s.isPlaying) return; s.currentClipId?.let { if(OboeAudioEngine.isLoaded(it)){ OboeAudioEngine.resumeSound(it); trackStates[trackId] = s.copy(isPlaying = true); updateStateFlow(); return } }; startTrack(trackId) }

    fun pauseAll() { trackStates.keys.forEach { pauseTrack(it) } }
    fun resumeAll() { trackStates.keys.forEach { val s = trackStates[it] ?: return@forEach; if(s.audioClips.isNotEmpty()) startTrack(it) } }

    private val duckedVolumes = mutableMapOf<String, Float>()
    private var isDucking = false

    fun duckAll() { if(isDucking) return; isDucking = true; trackStates.forEach{(id, s)->duckedVolumes[id] = s.volume; trackStates[id] = s.copy(volume = s.volume * 0.2f); s.currentClipId?.let{if(OboeAudioEngine.isPlaying(it))OboeAudioEngine.setVolume(it, s.volume*0.2f)}}; updateStateFlow() }

    fun unduckAll() { if(!isDucking) return; isDucking = false; duckedVolumes.forEach{(id, ov)->trackStates[id]?.let{s->trackStates[id]=s.copy(volume=ov); s.currentClipId?.let{if(OboeAudioEngine.isPlaying(it))OboeAudioEngine.setVolume(it,ov)}}}; duckedVolumes.clear(); updateStateFlow() }

    fun stopAll() { trackJobs.values.forEach { it.cancel() }; trackJobs.clear(); trackStates.keys.forEach { key -> trackStates[key]?.let { trackStates[key] = it.copy(isPlaying = false, currentClipId = null) } }; updateStateFlow() }

    fun isTrackPlaying(trackId: String) = trackStates[trackId]?.isPlaying ?: false
    fun getTrackState(trackId: String) = trackStates[trackId]

    private suspend fun playScatteredLoop(trackId: String) {
        val state = trackStates[trackId] ?: return
        if (state.audioClips.isEmpty()) { AppLog.w(TAG, "No clips to play for track: $trackId"); return }
        AppLog.d(TAG, "playScatteredLoop: Track $trackId has ${state.audioClips.size} clips: ${state.audioClips.map { it.id }}")
        while (currentCoroutineContext().isActive) {
            val currentState = trackStates[trackId] ?: break
            if (!currentState.isPlaying) { while (currentCoroutineContext().isActive) { delay(100); val cs = trackStates[trackId] ?: break; if (cs.isPlaying) break }; continue }
            val clip = currentState.audioClips[ThreadLocalRandom.current().nextInt(currentState.audioClips.size)]
            val clipId = "${trackId}_${clip.id}"
            try {
                playClip(clipId, clip, currentState); trackStates[trackId]?.let { trackStates[trackId] = it.copy(currentClipId = clipId) }; updateStateFlow()
                if (currentState.overlayMode) { delay(ThreadLocalRandom.current().nextLong(currentState.minIntervalMs, currentState.maxIntervalMs + 1)) }
                else {
                    val duration = OboeAudioEngine.getDuration(clipId); if (duration > 0) delay(duration) else delay(1000)
                    OboeAudioEngine.stopSound(clipId); OboeAudioEngine.unloadSound(clipId); loadedClips.remove(clipId)
                    trackStates[trackId]?.let { trackStates[trackId] = it.copy(currentClipId = null) }; updateStateFlow()
                    if (!currentCoroutineContext().isActive) break
                    delay(ThreadLocalRandom.current().nextLong((trackStates[trackId] ?: currentState).minIntervalMs, (trackStates[trackId] ?: currentState).maxIntervalMs + 1))
                }
            } catch (e: CancellationException) { OboeAudioEngine.stopSound(clipId); OboeAudioEngine.unloadSound(clipId); loadedClips.remove(clipId); throw e
            } catch (e: Exception) { AppLog.e(TAG, "Error playing clip: ${clip.id}", e); MemoryLockService.reportAnomaly(AnomalyType.AUDIO_PLAYER_ERROR, "散点音效播放异常: ${clip.id}", e.stackTraceToString()) }
        }
    }

    private suspend fun playClip(clipId: String, clip: ScatteredAudioClipData, state: ScatteredTrackState) {
        val ctx = contextRef?.get() ?: throw IllegalStateException("Context not initialized")
        val filePath = if (clip.filePath.startsWith("http")) { val cf = getCachedFile(ctx, clip.id); if (cf == null || !cf.exists()) { AppLog.w(TAG, "Clip not cached: ${clip.id}, skipping"); return }; cf.absolutePath } else clip.filePath
        val file = File(filePath); if (!file.exists()) { AppLog.w(TAG, "Clip file not found: $filePath"); return }
        OboeAudioEngine.loadSound(clipId, file.absolutePath); loadedClips[clipId] = file.absolutePath
        var retryCount = 0; while (!OboeAudioEngine.isLoaded(clipId) && retryCount < 50) { delay(100); retryCount++ }
        if (!OboeAudioEngine.isLoaded(clipId)) { AppLog.e(TAG, "Failed to load clip: ${clip.id}"); return }
        val latestState = trackStates[state.trackId] ?: state
        OboeAudioEngine.setVolume(clipId, latestState.volume); OboeAudioEngine.setLooping(clipId, false)
        OboeAudioEngine.setSpatialEnabled(clipId, latestState.spatialEnabled)
        if (latestState.spatialEnabled) { OboeAudioEngine.setSpatialOffsetType(clipId, 3); OboeAudioEngine.setSpatialScatterParams(clipId, latestState.spatialRange.minRadius, latestState.spatialRange.maxRadius, latestState.spatialRange.xEnabled, latestState.spatialRange.yEnabled, latestState.spatialRange.zEnabled, latestState.spatialRange.moveEnabled, latestState.spatialRange.moveRandomValue, latestState.spatialRange.moveSpeed, latestState.spatialRange.directionRandomValue) }
        val reverbConfig = ReverbManager.getConfig(latestState.trackId)
        if (reverbConfig != null) { OboeAudioEngine.setEffectEnabled(clipId, true); OboeAudioEngine.setReverbParams(clipId, reverbConfig.roomSize, reverbConfig.damping, reverbConfig.wetLevel); OboeAudioEngine.setInsulation(clipId, reverbConfig.insulation); OboeAudioEngine.setReverbDecayTime(clipId, reverbConfig.decayTime); OboeAudioEngine.setReverbPreDelay(clipId, reverbConfig.preDelay); OboeAudioEngine.setReverbDryLevel(clipId, reverbConfig.dryLevel) }
        val creativeConfig = CreativeEffectManager.getConfig(latestState.trackId)
        if (creativeConfig != null) { OboeAudioEngine.setCreativeEffectIntensity(clipId, CreativeEffectType.LoFi, creativeConfig.loFi); OboeAudioEngine.setCreativeEffectIntensity(clipId, CreativeEffectType.EightBit, creativeConfig.eightBit); OboeAudioEngine.setCreativeEffectIntensity(clipId, CreativeEffectType.Underwater, creativeConfig.underwater); OboeAudioEngine.setCreativeEffectIntensity(clipId, CreativeEffectType.AlienSignal, creativeConfig.alienSignal); OboeAudioEngine.setCreativeEffectIntensity(clipId, CreativeEffectType.Megaphone, creativeConfig.megaphone) }
        // 应用播放速度/音调（按轨道独立，从 SoundPlayConfig 读取）
        // speed=1/pitch=0 时不调用，避免启用 SoundTouch 的不必要开销
        val speedConfig = PlaybackStateManager.getSoundConfig(latestState.trackId)
        val wnSpeed = speedConfig?.playbackSpeed ?: 1f
        val wnPitch = speedConfig?.pitchShift ?: 0f
        if (wnSpeed != 1f) OboeAudioEngine.setPlaybackSpeed(clipId, wnSpeed)
        if (wnPitch != 0f) OboeAudioEngine.setPitchShift(clipId, wnPitch)
        OboeAudioEngine.setFadeDuration(clipId, 0.2f); OboeAudioEngine.playSound(clipId)
    }

    private fun getCachedFile(context: Context, soundId: String) = DownloadManager.getScatteredCachedFile(context, soundId)
    private fun updateStateFlow() { _trackStatesFlow.value = trackStates.toMap() }
    fun release() { stopAll(); scope.cancel(); trackStates.clear(); loadedClips.clear(); contextRef = null }
}

