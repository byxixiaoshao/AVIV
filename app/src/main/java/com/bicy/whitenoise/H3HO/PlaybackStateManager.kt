package com.bicy.whitenoise.H3HO

import android.util.Log
import com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SoundPlayConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialAudioConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.CreativeEffectConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.ScatteredAudioClipData
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialScatterRangeData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object PlaybackStateManager {
    
    private const val TAG = "PlaybackStateManager"
    
    private val soundConfigs = ConcurrentHashMap<String, SoundPlayConfig>()
    private val playingStates = ConcurrentHashMap<String, Boolean>()
    private val loadedSounds = ConcurrentHashMap<String, String>()
    private val volumeSettings = ConcurrentHashMap<String, Float>()
    
    private val _state = MutableStateFlow(ManagerState())
    val state: StateFlow<ManagerState> = _state.asStateFlow()
    
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    
    data class ManagerState(
        val isPaused: Boolean = false,
        val playingSoundIds: Set<String> = emptySet(),
        val loadedSoundIds: Set<String> = emptySet()
    )
    
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: () -> Unit) {
        listeners.removeAll { it == listener }
    }
    
    private fun notifyListeners() {
        updateState()
        listeners.forEach { it() }
    }
    
    private fun updateState() {
        _state.value = ManagerState(
            isPaused = playingStates.isNotEmpty() && playingStates.values.all { !it },
            playingSoundIds = playingStates.filter { it.value }.keys,
            loadedSoundIds = loadedSounds.keys.toSet()
        )
    }
    
    fun init() {
        loadState()
        Log.i(TAG, "PlaybackStateManager initialized")
    }
    
    fun loadState() {
        val playbackState = WhiteNoiseStorage.getPlaybackState()
        soundConfigs.clear()
        playingStates.clear()
        
        Log.i(TAG, "loadState: Loading ${playbackState.sounds.size} sounds from WhiteNoiseStorage")
        playbackState.sounds.forEach { config ->
            Log.i(TAG, "loadState: Loading config for ${config.id}, volume=${config.volume}, reverbEnabled=${config.reverbConfig.enabled}")
            soundConfigs[config.id] = config
            playingStates[config.id] = true
            volumeSettings[config.id] = config.volume
        }
        
        updateState()
        Log.i(TAG, "Loaded state: ${soundConfigs.size} sounds")
    }
    
    fun playSound(soundId: String, filePath: String, config: SoundPlayConfig) {
        soundConfigs[soundId] = config
        playingStates[soundId] = true
        loadedSounds[soundId] = filePath
        volumeSettings[soundId] = config.volume
        WhiteNoiseStorage.addPlayingSound(config)
        notifyListeners()
        Log.i(TAG, "Playing sound: $soundId")
    }
    
    fun stopSound(soundId: String) {
        soundConfigs.remove(soundId)
        playingStates.remove(soundId)
        loadedSounds.remove(soundId)
        volumeSettings.remove(soundId)
        WhiteNoiseStorage.removePlayingSound(soundId)
        notifyListeners()
        Log.i(TAG, "Stopped sound: $soundId")
    }
    
    fun pauseSound(soundId: String) {
        playingStates[soundId] = false
        WhiteNoiseStorage.setPlaybackPaused(true)
        notifyListeners()
        Log.i(TAG, "Paused sound: $soundId")
    }
    
    fun resumeSound(soundId: String) {
        playingStates[soundId] = true
        if (playingStates.values.any { it }) {
            WhiteNoiseStorage.setPlaybackPaused(false)
        }
        notifyListeners()
        Log.i(TAG, "Resumed sound: $soundId")
    }
    
    fun pauseAll() {
        playingStates.keys.forEach { playingStates[it] = false }
        WhiteNoiseStorage.setPlaybackPaused(true)
        notifyListeners()
        Log.i(TAG, "Paused all sounds")
    }
    
    fun resumeAll() {
        playingStates.keys.forEach { playingStates[it] = true }
        WhiteNoiseStorage.setPlaybackPaused(false)
        notifyListeners()
        Log.i(TAG, "Resumed all sounds")
    }
    
    fun updateVolume(soundId: String, volume: Float) {
        volumeSettings[soundId] = volume
        val currentConfig = soundConfigs[soundId]
        if (currentConfig != null) {
            soundConfigs[soundId] = currentConfig.copy(volume = volume)
            WhiteNoiseStorage.updatePlayingSoundVolume(soundId, volume)
        }
        notifyListeners()
        Log.i(TAG, "Updated volume for $soundId: $volume")
    }
    
    fun updateReverbConfig(soundId: String, reverbConfig: ReverbConfig) {
        val currentConfig = soundConfigs[soundId]
        if (currentConfig != null) {
            soundConfigs[soundId] = currentConfig.copy(reverbConfig = reverbConfig)
            WhiteNoiseStorage.updatePlayingSoundReverb(soundId, reverbConfig)
            notifyListeners()
            Log.i(TAG, "Updated reverb config for $soundId")
        }
    }
    
    fun updateSpatialConfig(soundId: String, spatialConfig: SpatialAudioConfig) {
        val currentConfig = soundConfigs[soundId]
        if (currentConfig != null) {
            soundConfigs[soundId] = currentConfig.copy(spatialAudioConfig = spatialConfig)
            WhiteNoiseStorage.updatePlayingSoundSpatial(soundId, spatialConfig)
            notifyListeners()
            Log.i(TAG, "Updated spatial config for $soundId")
        }
    }
    
    fun updateCreativeConfig(soundId: String, creativeConfig: CreativeEffectConfig) {
        val currentConfig = soundConfigs[soundId]
        if (currentConfig != null) {
            soundConfigs[soundId] = currentConfig.copy(creativeEffectConfig = creativeConfig)
            WhiteNoiseStorage.updatePlayingSoundCreative(soundId, creativeConfig)
            notifyListeners()
            Log.i(TAG, "Updated creative config for $soundId")
        }
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
        val config = SoundPlayConfig(
            id = trackId,
            name = trackId,
            volume = volume,
            trackType = "scattered",
            audioClips = audioClips,
            minIntervalMs = minIntervalMs,
            maxIntervalMs = maxIntervalMs,
            spatialScatterRange = spatialRange,
            spatialScatterEnabled = spatialEnabled,
            overlayMode = overlayMode
        )
        soundConfigs[trackId] = config
        playingStates[trackId] = false
        volumeSettings[trackId] = volume
        WhiteNoiseStorage.addPlayingSound(config)
        notifyListeners()
        Log.i(TAG, "Registered scattered track: $trackId")
    }
    
    fun updateScatteredTrackConfig(
        trackId: String,
        minIntervalMs: Long? = null,
        maxIntervalMs: Long? = null,
        spatialScatterRange: SpatialScatterRangeData? = null,
        spatialScatterEnabled: Boolean? = null,
        overlayMode: Boolean? = null,
        volume: Float? = null
    ) {
        val currentConfig = soundConfigs[trackId] ?: return
        val updatedConfig = currentConfig.copy(
            minIntervalMs = minIntervalMs ?: currentConfig.minIntervalMs,
            maxIntervalMs = maxIntervalMs ?: currentConfig.maxIntervalMs,
            spatialScatterRange = spatialScatterRange ?: currentConfig.spatialScatterRange,
            spatialScatterEnabled = spatialScatterEnabled ?: currentConfig.spatialScatterEnabled,
            overlayMode = overlayMode ?: currentConfig.overlayMode,
            volume = volume ?: currentConfig.volume
        )
        soundConfigs[trackId] = updatedConfig
        if (volume != null) {
            volumeSettings[trackId] = volume
        }
        WhiteNoiseStorage.updateScatteredTrackConfig(
            trackId, minIntervalMs, maxIntervalMs, spatialScatterRange, spatialScatterEnabled, overlayMode
        )
        notifyListeners()
        Log.i(TAG, "Updated scattered track config: $trackId")
    }
    
    fun addAudioClipToTrack(trackId: String, clip: ScatteredAudioClipData) {
        val currentConfig = soundConfigs[trackId] ?: return
        val updatedConfig = currentConfig.copy(
            audioClips = currentConfig.audioClips + clip
        )
        soundConfigs[trackId] = updatedConfig
        WhiteNoiseStorage.addAudioClipToTrack(trackId, clip)
        notifyListeners()
        Log.i(TAG, "Added audio clip to track: $trackId")
    }
    
    fun getSoundConfig(soundId: String): SoundPlayConfig? {
        return soundConfigs[soundId]
    }
    
    fun getPlayingSounds(): List<String> {
        return playingStates.filter { it.value }.keys.toList()
    }
    
    fun getAllSoundIds(): List<String> {
        return soundConfigs.keys.toList()
    }
    
    fun isPlaying(soundId: String): Boolean {
        return playingStates[soundId] == true
    }
    
    fun isLoaded(soundId: String): Boolean {
        return loadedSounds.containsKey(soundId)
    }
    
    fun getLoadedSoundPath(soundId: String): String? {
        return loadedSounds[soundId]
    }
    
    fun setLoadedSoundPath(soundId: String, filePath: String) {
        loadedSounds[soundId] = filePath
    }
    
    fun getVolume(soundId: String): Float {
        return volumeSettings[soundId] ?: 1f
    }
    
    fun clearAll() {
        soundConfigs.clear()
        playingStates.clear()
        loadedSounds.clear()
        volumeSettings.clear()
        WhiteNoiseStorage.clearPlayback()
        notifyListeners()
        Log.i(TAG, "Cleared all state")
    }
    
    fun getPlayingStates(): Map<String, Boolean> {
        return playingStates.toMap()
    }
    
    fun getSoundConfigs(): Map<String, SoundPlayConfig> {
        return soundConfigs.toMap()
    }
    
    fun getLoadedSounds(): Map<String, String> {
        return loadedSounds.toMap()
    }
    
    fun getVolumeSettings(): Map<String, Float> {
        return volumeSettings.toMap()
    }
}
