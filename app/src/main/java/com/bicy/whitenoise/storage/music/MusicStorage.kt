package com.bicy.whitenoise.storage.music

import android.net.Uri
import android.util.Log
import com.bicy.whitenoise.audio.ReverbConfig
import com.bicy.whitenoise.storage.core.JsonStorageManager
import com.bicy.whitenoise.utils.AppInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

data class MusicDirectory(
    val path: String,
    val uriString: String? = null,
    val name: String,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false
) {
    val uri: Uri?
        get() = uriString?.let { Uri.parse(it) }
}

data class PlaylistItem(
    val id: String,
    val uri: Uri,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val duration: Long = 0,
    val path: String
)

data class MusicPlaybackState(
    val trackId: String? = null,
    val trackTitle: String? = null,
    val trackArtist: String? = null,
    val trackDuration: Long = 0,
    val position: Long = 0,
    val isPlaying: Boolean = false,
    val repeatMode: String = "OFF",
    val shuffleMode: String = "OFF",
    val playlistIndex: Int = 0,
    val playlistTrackIds: List<String> = emptyList(),
    val onlineTracksJson: String = "{}"
)

data class EffectIntensities(
    val loFi: Float = 0f,
    val eightBit: Float = 0f,
    val underwater: Float = 0f,
    val alienSignal: Float = 0f,
    val megaphone: Float = 0f,
    val pitch: Float = 0f,
    val speed: Float = 1f,
    val hifi: Float = 0f,
    val distortion: Float = 0f,
    val stereoWidener: Float = 0.5f,
    val virtualBass: Float = 0.2f,
    val multibandCompressor: Float = 0.5f
)

data class MusicSpatialConfig(
    val enabled: Boolean = false,
    val offsetType: Int = 0,
    val fixedLeftRight: Float = 0f,
    val fixedUpDown: Float = 0f,
    val fixedFrontBack: Float = 0f,
    val fixedMultiplier: Float = 1f,
    val surroundMode: Int = 0,
    val surroundRadius: Float = 1f,
    val surroundSpeed: Float = 5f,
    val randomMaxDistance: Float = 5f,
    val randomMinDistance: Float = 0f,
    val randomValue: Float = 0.5f,
    val randomSpeed: Float = 0.3f
)

data class EqualizerConfig(
    val enabled: Boolean = false,
    val gains: FloatArray = FloatArray(12) { 0f }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EqualizerConfig
        if (enabled != other.enabled) return false
        if (!gains.contentEquals(other.gains)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + gains.contentHashCode()
        return result
    }
}

data class LimiterConfig(
    val enabled: Boolean = true,
    val limitEqualizer: Boolean = true,
    val limitEffects: Boolean = true,
    val limitReverb: Boolean = true,
    val limitSpatial: Boolean = true,
    val threshold: Float = 0.9f,
    val attack: Float = 5.0f,
    val release: Float = 50.0f
)

data class MusicMixerConfig(
    val reverbConfig: ReverbConfig = ReverbConfig(),
    val volume: Float = 1f,
    val effectIntensities: EffectIntensities = EffectIntensities(),
    val spatialAudioConfig: MusicSpatialConfig = MusicSpatialConfig(),
    val equalizerConfig: EqualizerConfig = EqualizerConfig(),
    val limiterConfig: LimiterConfig = LimiterConfig()
)

object MusicStorage {
    
    private const val TAG = "MusicStorage"

    /** 默认音乐目录路径 */
    val DEFAULT_MUSIC_DIR = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC).absolutePath

    private const val DIRECTORIES_FILE = "music_directories.json"
    private const val PLAYBACK_FILE = "music_playback.json"
    private const val MIXER_FILE = "music_mixer.json"
    private const val PLAYLIST_FILE = "music_playlist.json"
    private const val PLAYLIST_TRACK_KEY = "__music_queue__"

    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _directories = MutableStateFlow<List<MusicDirectory>>(emptyList())
    val directories: StateFlow<List<MusicDirectory>> = _directories.asStateFlow()
    
    private val _playbackState = MutableStateFlow(MusicPlaybackState())
    val playbackState: StateFlow<MusicPlaybackState> = _playbackState.asStateFlow()
    
    private val _mixerConfig = MutableStateFlow(MusicMixerConfig())
    val mixerConfig: StateFlow<MusicMixerConfig> = _mixerConfig.asStateFlow()
    
    private val _playlist = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val playlist: StateFlow<List<PlaylistItem>> = _playlist.asStateFlow()
    
    private var playlistIndex = 0
    
    private val listeners = mutableListOf<() -> Unit>()
    
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }
    
    private fun notifyListeners() {
        listeners.forEach { it() }
    }
    
    fun init() {
        loadDirectories()
        loadPlaybackState()
        loadMixerConfig()
        loadPlaylist()
        Log.d(TAG, "MusicStorage initialized")
    }
    
    // ==================== Directory operations ====================
    
    private fun loadDirectories() {
        try {
            val array = runBlocking {
                JsonStorageManager.read(DIRECTORIES_FILE, Array<MusicDirectory>::class.java)
            }
            _directories.value = array?.toList() ?: emptyList()
            // 确保默认目录始终存在
            ensureDefaultDirectory()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load directories from JSON", e)
        }
    }

    /** 确保默认音乐目录存在 */
    private fun ensureDefaultDirectory() {
        val dirs = _directories.value.toMutableList()
        val hasDefault = dirs.any { it.path == DEFAULT_MUSIC_DIR }
        if (!hasDefault) {
            dirs.add(MusicDirectory(
                path = DEFAULT_MUSIC_DIR,
                name = "音乐目录",
                isEnabled = true,
                isDefault = true
            ))
            _directories.value = dirs
        }
    }
    
    private fun persistDirectories() {
        scope.launch {
            try {
                val dirs = _directories.value
                    .filter { !it.isDefault }
                    .toTypedArray()
                if (dirs.isNotEmpty()) {
                    JsonStorageManager.write(DIRECTORIES_FILE, dirs)
                } else {
                    JsonStorageManager.write(DIRECTORIES_FILE, emptyArray<MusicDirectory>())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save directories to JSON", e)
            }
        }
    }
    
    fun getDirectories(): List<MusicDirectory> = _directories.value
    
    fun addDirectory(path: String, uri: Uri? = null): Boolean {
        // 如果目录已存在但需要更新 URI（如用户通过 SAF 重新授权默认目录），则更新 URI
        val existing = _directories.value.find { it.path == path }
        if (existing != null) {
            if (uri != null && existing.uriString != uri.toString()) {
                _directories.value = _directories.value.map {
                    if (it.path == path) it.copy(uriString = uri.toString()) else it
                }
                persistDirectories()
                return true
            }
            return false
        }
        
        val name = path.substringAfterLast('/')
        val newDir = MusicDirectory(
            path = path,
            uriString = uri?.toString(),
            name = name,
            isEnabled = true,
            isDefault = path == DEFAULT_MUSIC_DIR
        )
        
        _directories.value = _directories.value + newDir
        persistDirectories()
        return true
    }
    
    fun removeDirectory(path: String) {
        val dir = _directories.value.find { it.path == path }
        if (dir?.isDefault == true) return
        _directories.value = _directories.value.filter { it.path != path }
        persistDirectories()
    }
    
    fun setDirectoryEnabled(path: String, enabled: Boolean) {
        _directories.value = _directories.value.map { dir ->
            if (dir.path == path) dir.copy(isEnabled = enabled) else dir
        }
        persistDirectories()
    }
    
    fun clearDirectories() {
        _directories.value = _directories.value.filter { it.isDefault }
        persistDirectories()
    }
    
    fun getEnabledDirectories(): List<MusicDirectory> = _directories.value.filter { it.isEnabled }
    
    fun hasDirectories(): Boolean = _directories.value.isNotEmpty()
    
    // ==================== Playback State operations ====================
    
    private fun loadPlaybackState() {
        try {
            val state = runBlocking {
                JsonStorageManager.read(PLAYBACK_FILE, MusicPlaybackState::class.java)
            }
            if (state != null) {
                _playbackState.value = state
                Log.d(TAG, "Loaded playback state: trackId=${state.trackId}, position=${state.position}, playlistIndex=${state.playlistIndex}, playlistSize=${state.playlistTrackIds.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load playback state from JSON", e)
        }
    }
    
    private fun persistPlaybackState() {
        scope.launch {
            try {
                JsonStorageManager.write(PLAYBACK_FILE, _playbackState.value)
                Log.d(TAG, "Saved playback state: trackId=${_playbackState.value.trackId}, position=${_playbackState.value.position}, playlistIndex=${_playbackState.value.playlistIndex}, playlistSize=${_playbackState.value.playlistTrackIds.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save playback state to JSON", e)
            }
        }
    }
    
    fun getPlaybackState(): MusicPlaybackState = _playbackState.value
    
    fun savePlaybackState(state: MusicPlaybackState) {
        _playbackState.value = state
        persistPlaybackState()
    }
    
    fun updatePlaybackState(
        trackId: String? = null,
        trackTitle: String? = null,
        trackArtist: String? = null,
        trackDuration: Long? = null,
        position: Long? = null,
        isPlaying: Boolean? = null,
        repeatMode: String? = null,
        shuffleMode: String? = null,
        playlistIndex: Int? = null
    ) {
        val current = _playbackState.value
        _playbackState.value = current.copy(
            trackId = trackId ?: current.trackId,
            trackTitle = trackTitle ?: current.trackTitle,
            trackArtist = trackArtist ?: current.trackArtist,
            trackDuration = trackDuration ?: current.trackDuration,
            position = position ?: current.position,
            isPlaying = isPlaying ?: current.isPlaying,
            repeatMode = repeatMode ?: current.repeatMode,
            shuffleMode = shuffleMode ?: current.shuffleMode,
            playlistIndex = playlistIndex ?: current.playlistIndex
        )
        persistPlaybackState()
    }
    
    fun clearPlaybackState() {
        _playbackState.value = MusicPlaybackState()
        persistPlaybackState()
    }
    
    // ==================== Mixer Config operations ====================
    
    private fun loadMixerConfig() {
        try {
            val config = runBlocking {
                JsonStorageManager.read(MIXER_FILE, MusicMixerConfig::class.java)
            }
            if (config != null) {
                _mixerConfig.value = config
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load mixer config from JSON", e)
        }
    }
    
    private fun persistMixerConfig() {
        scope.launch {
            try {
                JsonStorageManager.write(MIXER_FILE, _mixerConfig.value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save mixer config to JSON", e)
            }
        }
    }
    
    fun getMixerConfig(): MusicMixerConfig = _mixerConfig.value
    
    fun getReverbConfig(): ReverbConfig = _mixerConfig.value.reverbConfig
    
    fun updateReverbConfig(config: ReverbConfig) {
        _mixerConfig.value = _mixerConfig.value.copy(reverbConfig = config)
        persistMixerConfig()
    }
    
    fun setReverbEnabled(enabled: Boolean) {
        val current = _mixerConfig.value.reverbConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            reverbConfig = current.copy(enabled = enabled)
        )
        persistMixerConfig()
    }
    
    fun setReverbPreset(preset: String) {
        val current = _mixerConfig.value.reverbConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            reverbConfig = current.copy(preset = preset)
        )
        persistMixerConfig()
    }
    
    fun getVolume(): Float = _mixerConfig.value.volume
    
    fun updateVolume(volume: Float) {
        _mixerConfig.value = _mixerConfig.value.copy(volume = volume)
        persistMixerConfig()
    }
    
    fun getPitch(): Float = _mixerConfig.value.effectIntensities.pitch
    
    fun getSpeed(): Float = _mixerConfig.value.effectIntensities.speed
    
    fun getEffectIntensities(): EffectIntensities = _mixerConfig.value.effectIntensities
    
    fun updateEffectIntensities(intensities: EffectIntensities) {
        _mixerConfig.value = _mixerConfig.value.copy(effectIntensities = intensities)
        persistMixerConfig()
    }
    
    fun updateEffectIntensity(effectName: String, intensity: Float) {
        val current = _mixerConfig.value.effectIntensities
        val newIntensities = when (effectName) {
            "loFi" -> current.copy(loFi = intensity)
            "eightBit" -> current.copy(eightBit = intensity)
            "underwater" -> current.copy(underwater = intensity)
            "alienSignal" -> current.copy(alienSignal = intensity)
            "megaphone" -> current.copy(megaphone = intensity)
            "pitch" -> current.copy(pitch = intensity)
            "speed" -> current.copy(speed = intensity)
            "hifi" -> current.copy(hifi = intensity)
            "distortion" -> current.copy(distortion = intensity)
            "stereoWidener" -> current.copy(stereoWidener = intensity)
            "virtualBass" -> current.copy(virtualBass = intensity)
            "multibandCompressor" -> current.copy(multibandCompressor = intensity)
            else -> current
        }
        _mixerConfig.value = _mixerConfig.value.copy(effectIntensities = newIntensities)
        persistMixerConfig()
    }
    
    fun getSpatialAudioConfig(): MusicSpatialConfig = _mixerConfig.value.spatialAudioConfig
    
    fun updateSpatialAudioConfig(config: MusicSpatialConfig) {
        _mixerConfig.value = _mixerConfig.value.copy(spatialAudioConfig = config)
        persistMixerConfig()
    }
    
    fun updateSpatialAudioEnabled(enabled: Boolean) {
        val current = _mixerConfig.value.spatialAudioConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            spatialAudioConfig = current.copy(enabled = enabled)
        )
        persistMixerConfig()
    }
    
    fun updateSpatialAudioOffsetType(offsetType: Int) {
        val current = _mixerConfig.value.spatialAudioConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            spatialAudioConfig = current.copy(offsetType = offsetType)
        )
        persistMixerConfig()
    }
    
    fun updateSpatialAudioFixedOffset(leftRight: Float, upDown: Float, frontBack: Float, multiplier: Float) {
        val current = _mixerConfig.value.spatialAudioConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            spatialAudioConfig = current.copy(
                fixedLeftRight = leftRight,
                fixedUpDown = upDown,
                fixedFrontBack = frontBack,
                fixedMultiplier = multiplier
            )
        )
        persistMixerConfig()
    }
    
    fun updateSpatialAudioSurroundParams(mode: Int, radius: Float, speed: Float) {
        val current = _mixerConfig.value.spatialAudioConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            spatialAudioConfig = current.copy(
                surroundMode = mode,
                surroundRadius = radius,
                surroundSpeed = speed
            )
        )
        persistMixerConfig()
    }
    
    fun updateSpatialAudioRandomParams(maxDistance: Float, minDistance: Float, randomValue: Float, speed: Float) {
        val current = _mixerConfig.value.spatialAudioConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            spatialAudioConfig = current.copy(
                randomMaxDistance = maxDistance,
                randomMinDistance = minDistance,
                randomValue = randomValue,
                randomSpeed = speed
            )
        )
        persistMixerConfig()
    }
    
    fun getEqualizerConfig(): EqualizerConfig = _mixerConfig.value.equalizerConfig
    
    fun updateEqualizerConfig(config: EqualizerConfig) {
        _mixerConfig.value = _mixerConfig.value.copy(equalizerConfig = config)
        persistMixerConfig()
    }
    
    fun updateEqualizerEnabled(enabled: Boolean) {
        val current = _mixerConfig.value.equalizerConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            equalizerConfig = current.copy(enabled = enabled)
        )
        persistMixerConfig()
    }
    
    fun updateEqualizerGains(gains: FloatArray) {
        val current = _mixerConfig.value.equalizerConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            equalizerConfig = current.copy(gains = gains.copyOf())
        )
        persistMixerConfig()
    }
    
    fun getLimiterConfig(): LimiterConfig = _mixerConfig.value.limiterConfig
    
    fun updateLimiterConfig(config: LimiterConfig) {
        _mixerConfig.value = _mixerConfig.value.copy(limiterConfig = config)
        persistMixerConfig()
    }
    
    fun updateLimiterEnabled(enabled: Boolean) {
        val current = _mixerConfig.value.limiterConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            limiterConfig = current.copy(enabled = enabled)
        )
        persistMixerConfig()
    }
    
    fun updateLimiterTargets(
        limitEqualizer: Boolean? = null,
        limitEffects: Boolean? = null,
        limitReverb: Boolean? = null,
        limitSpatial: Boolean? = null
    ) {
        val current = _mixerConfig.value.limiterConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            limiterConfig = current.copy(
                limitEqualizer = limitEqualizer ?: current.limitEqualizer,
                limitEffects = limitEffects ?: current.limitEffects,
                limitReverb = limitReverb ?: current.limitReverb,
                limitSpatial = limitSpatial ?: current.limitSpatial
            )
        )
        persistMixerConfig()
    }
    
    // ==================== Playlist operations ====================
    
    private fun loadPlaylist() {
        try {
            // Read the stored serialized version and parse
            val stored = runBlocking {
                JsonStorageManager.read(PLAYLIST_FILE, PlaylistStore::class.java)
            }
            if (stored != null) {
                _playlist.value = parsePlaylistItemsJson(stored.itemsJson)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load playlist from JSON", e)
        }
    }
    
    /** Simple data class for persisting playlist items as JSON */
    private data class PlaylistStore(
        val itemsJson: String = "[]"
    )
    
    private fun persistPlaylist() {
        scope.launch {
            try {
                val json = serializePlaylistItems(_playlist.value)
                JsonStorageManager.write(PLAYLIST_FILE, PlaylistStore(itemsJson = json))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save playlist to JSON", e)
            }
        }
    }

    private fun serializePlaylistItems(items: List<PlaylistItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("uri", item.uri.toString())
                put("title", item.title)
                item.artist?.let { put("artist", it) }
                item.album?.let { put("album", it) }
                put("duration", item.duration)
                put("path", item.path)
            })
        }
        return arr.toString()
    }

    private fun parsePlaylistItemsJson(json: String): List<PlaylistItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val items = mutableListOf<PlaylistItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                items.add(
                    PlaylistItem(
                        id = obj.getString("id"),
                        uri = Uri.parse(obj.getString("uri")),
                        title = obj.getString("title"),
                        artist = obj.optString("artist").takeIf { it.isNotEmpty() },
                        album = obj.optString("album").takeIf { it.isNotEmpty() },
                        duration = obj.optLong("duration", 0),
                        path = obj.getString("path")
                    )
                )
            }
            items
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse playlist JSON", e)
            emptyList()
        }
    }
    
    fun getPlaylist(): List<PlaylistItem> = _playlist.value
    
    fun savePlaylist(items: List<PlaylistItem>) {
        _playlist.value = items
        persistPlaylist()
    }
    
    fun clearPlaylist() {
        _playlist.value = emptyList()
        persistPlaylist()
    }
    
    fun getPlaylistIndex(): Int = playlistIndex
    
    fun setPlaylistIndex(index: Int) {
        playlistIndex = index
    }
}
