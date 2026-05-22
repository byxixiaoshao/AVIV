package com.bicy.whitenoise.JwJY.EY9i

import android.net.Uri
import android.util.Log
import com.bicy.whitenoise.H3HO.ReverbConfig
import com.bicy.whitenoise.JwJY.Jauc.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class MusicDirectory(
    val path: String,
    val uriString: String? = null,
    val name: String,
    val isEnabled: Boolean = true
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
    val playlistTrackIds: List<String> = emptyList()
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
    val distortion: Float = 0f
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
    val limiterEnabled: Boolean = true,
    val gains: FloatArray = FloatArray(12) { 0f }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EqualizerConfig
        if (enabled != other.enabled) return false
        if (limiterEnabled != other.limiterEnabled) return false
        if (!gains.contentEquals(other.gains)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + limiterEnabled.hashCode()
        result = 31 * result + gains.contentHashCode()
        return result
    }
}

data class MusicMixerConfig(
    val reverbConfig: ReverbConfig = ReverbConfig(),
    val volume: Float = 1f,
    val effectIntensities: EffectIntensities = EffectIntensities(),
    val spatialAudioConfig: MusicSpatialConfig = MusicSpatialConfig(),
    val equalizerConfig: EqualizerConfig = EqualizerConfig()
)

object MusicStorage {
    
    private const val TAG = "MusicStorage"
    
    private const val PLAYBACK_STATE_FILE = "playback_state.json"
    private const val MIXER_CONFIG_FILE = "mixer_config.json"
    private const val DIRECTORIES_FILE = "directories.json"
    private const val PLAYLIST_FILE = "playlist.json"
    
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
    
    private fun loadDirectories() {
        val file = StorageManager.getFile("music", DIRECTORIES_FILE) ?: return
        val jsonArray = StorageManager.loadJsonArray(file)
        
        if (jsonArray != null) {
            val dirList = mutableListOf<MusicDirectory>()
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                dirList.add(
                    MusicDirectory(
                        path = json.getString("path"),
                        uriString = json.optString("uri").takeIf { it.isNotEmpty() },
                        name = json.getString("name"),
                        isEnabled = json.optBoolean("isEnabled", true)
                    )
                )
            }
            _directories.value = dirList
        }
    }
    
    private fun saveDirectories() {
        val file = StorageManager.getFile("music", DIRECTORIES_FILE) ?: return
        val jsonArray = JSONArray()
        _directories.value.forEach { dir ->
            jsonArray.put(JSONObject().apply {
                put("path", dir.path)
                dir.uriString?.let { put("uri", it) }
                put("name", dir.name)
                put("isEnabled", dir.isEnabled)
            })
        }
        StorageManager.saveJsonSync(file, jsonArray)
    }
    
    private fun loadPlaybackState() {
        val file = StorageManager.getFile("music", PLAYBACK_STATE_FILE) ?: return
        val json = StorageManager.loadJson(file)
        
        if (json != null) {
            val trackIds = mutableListOf<String>()
            val trackIdsArray = json.optJSONArray("playlistTrackIds")
            if (trackIdsArray != null) {
                for (i in 0 until trackIdsArray.length()) {
                    trackIds.add(trackIdsArray.getString(i))
                }
            }
            
            _playbackState.value = MusicPlaybackState(
                trackId = json.optString("trackId").takeIf { it.isNotEmpty() },
                trackTitle = json.optString("trackTitle").takeIf { it.isNotEmpty() },
                trackArtist = json.optString("trackArtist").takeIf { it.isNotEmpty() },
                trackDuration = json.optLong("trackDuration", 0),
                position = json.optLong("position", 0),
                isPlaying = json.optBoolean("isPlaying", false),
                repeatMode = json.optString("repeatMode", "OFF"),
                shuffleMode = json.optString("shuffleMode", "OFF"),
                playlistIndex = json.optInt("playlistIndex", 0),
                playlistTrackIds = trackIds
            )
        }
    }
    
    private fun savePlaybackState() {
        val file = StorageManager.getFile("music", PLAYBACK_STATE_FILE) ?: return
        val state = _playbackState.value
        
        val json = JSONObject().apply {
            put("trackId", state.trackId ?: "")
            put("trackTitle", state.trackTitle ?: "")
            put("trackArtist", state.trackArtist ?: "")
            put("trackDuration", state.trackDuration)
            put("position", state.position)
            put("isPlaying", state.isPlaying)
            put("repeatMode", state.repeatMode)
            put("shuffleMode", state.shuffleMode)
            put("playlistIndex", state.playlistIndex)
            put("playlistTrackIds", JSONArray(state.playlistTrackIds))
        }
        
        StorageManager.saveJsonSync(file, json)
    }
    
    private fun loadMixerConfig() {
        val file = StorageManager.getFile("music", MIXER_CONFIG_FILE) ?: return
        val json = StorageManager.loadJson(file)
        
        if (json != null) {
            val reverbJson = json.optJSONObject("reverbConfig")
            val reverbConfig = if (reverbJson != null) {
                ReverbConfig(
                    enabled = reverbJson.optBoolean("enabled", false),
                    preset = reverbJson.optString("preset", "NONE"),
                    roomSize = reverbJson.optDouble("roomSize", 0.0).toFloat(),
                    decayTime = reverbJson.optDouble("decayTime", 1.5).toFloat(),
                    damping = reverbJson.optDouble("damping", 0.0).toFloat(),
                    wetLevel = reverbJson.optDouble("wetLevel", 0.0).toFloat(),
                    dryLevel = reverbJson.optDouble("dryLevel", 1.0).toFloat(),
                    preDelay = reverbJson.optDouble("preDelay", 0.025).toFloat(),
                    insulation = reverbJson.optDouble("insulation", 0.0).toFloat()
                )
            } else {
                ReverbConfig()
            }
            
            val effectsJson = json.optJSONObject("effectIntensities")
            val effectIntensities = if (effectsJson != null) {
                EffectIntensities(
                    loFi = effectsJson.optDouble("loFi", 0.0).toFloat(),
                    eightBit = effectsJson.optDouble("eightBit", 0.0).toFloat(),
                    underwater = effectsJson.optDouble("underwater", 0.0).toFloat(),
                    alienSignal = effectsJson.optDouble("alienSignal", 0.0).toFloat(),
                    megaphone = effectsJson.optDouble("megaphone", 0.0).toFloat(),
                    pitch = effectsJson.optDouble("pitch", 0.0).toFloat(),
                    speed = effectsJson.optDouble("speed", 1.0).toFloat(),
                    hifi = effectsJson.optDouble("hifi", 0.0).toFloat(),
                    distortion = effectsJson.optDouble("distortion", 0.0).toFloat()
                )
            } else {
                EffectIntensities()
            }
            
            val spatialJson = json.optJSONObject("spatialAudioConfig")
            val spatialConfig = if (spatialJson != null) {
                MusicSpatialConfig(
                    enabled = spatialJson.optBoolean("enabled", false),
                    offsetType = spatialJson.optInt("offsetType", 0),
                    fixedLeftRight = spatialJson.optDouble("fixedLeftRight", 0.0).toFloat(),
                    fixedUpDown = spatialJson.optDouble("fixedUpDown", 0.0).toFloat(),
                    fixedFrontBack = spatialJson.optDouble("fixedFrontBack", 0.0).toFloat(),
                    fixedMultiplier = spatialJson.optDouble("fixedMultiplier", 1.0).toFloat(),
                    surroundMode = spatialJson.optInt("surroundMode", 0),
                    surroundRadius = spatialJson.optDouble("surroundRadius", 1.0).toFloat(),
                    surroundSpeed = spatialJson.optDouble("surroundSpeed", 5.0).toFloat(),
                    randomMaxDistance = spatialJson.optDouble("randomMaxDistance", 5.0).toFloat(),
                    randomMinDistance = spatialJson.optDouble("randomMinDistance", 0.0).toFloat(),
                    randomValue = spatialJson.optDouble("randomValue", 0.5).toFloat(),
                    randomSpeed = spatialJson.optDouble("randomSpeed", 0.3).toFloat()
                )
            } else {
                MusicSpatialConfig()
            }
            
            val eqJson = json.optJSONObject("equalizerConfig")
            val equalizerConfig = if (eqJson != null) {
                val gainsArray = eqJson.optJSONArray("gains")
                val gains = if (gainsArray != null && gainsArray.length() == 12) {
                    FloatArray(12) { i -> gainsArray.getDouble(i).toFloat() }
                } else {
                    FloatArray(12) { 0f }
                }
                EqualizerConfig(
                    enabled = eqJson.optBoolean("enabled", false),
                    limiterEnabled = eqJson.optBoolean("limiterEnabled", true),
                    gains = gains
                )
            } else {
                EqualizerConfig()
            }
            
            _mixerConfig.value = MusicMixerConfig(
                reverbConfig = reverbConfig,
                volume = json.optDouble("volume", 1.0).toFloat(),
                effectIntensities = effectIntensities,
                spatialAudioConfig = spatialConfig,
                equalizerConfig = equalizerConfig
            )
        }
    }
    
    private fun saveMixerConfig() {
        val file = StorageManager.getFile("music", MIXER_CONFIG_FILE) ?: return
        val config = _mixerConfig.value
        
        val json = JSONObject().apply {
            put("volume", config.volume)
            
            put("reverbConfig", JSONObject().apply {
                put("enabled", config.reverbConfig.enabled)
                put("preset", config.reverbConfig.preset)
                put("roomSize", config.reverbConfig.roomSize)
                put("decayTime", config.reverbConfig.decayTime)
                put("damping", config.reverbConfig.damping)
                put("wetLevel", config.reverbConfig.wetLevel)
                put("dryLevel", config.reverbConfig.dryLevel)
                put("preDelay", config.reverbConfig.preDelay)
                put("insulation", config.reverbConfig.insulation)
            })
            
            put("effectIntensities", JSONObject().apply {
                put("loFi", config.effectIntensities.loFi)
                put("eightBit", config.effectIntensities.eightBit)
                put("underwater", config.effectIntensities.underwater)
                put("alienSignal", config.effectIntensities.alienSignal)
                put("megaphone", config.effectIntensities.megaphone)
                put("pitch", config.effectIntensities.pitch)
                put("speed", config.effectIntensities.speed)
                put("hifi", config.effectIntensities.hifi)
                put("distortion", config.effectIntensities.distortion)
            })
            
            put("spatialAudioConfig", JSONObject().apply {
                put("enabled", config.spatialAudioConfig.enabled)
                put("offsetType", config.spatialAudioConfig.offsetType)
                put("fixedLeftRight", config.spatialAudioConfig.fixedLeftRight)
                put("fixedUpDown", config.spatialAudioConfig.fixedUpDown)
                put("fixedFrontBack", config.spatialAudioConfig.fixedFrontBack)
                put("fixedMultiplier", config.spatialAudioConfig.fixedMultiplier)
                put("surroundMode", config.spatialAudioConfig.surroundMode)
                put("surroundRadius", config.spatialAudioConfig.surroundRadius)
                put("surroundSpeed", config.spatialAudioConfig.surroundSpeed)
                put("randomMaxDistance", config.spatialAudioConfig.randomMaxDistance)
                put("randomMinDistance", config.spatialAudioConfig.randomMinDistance)
                put("randomValue", config.spatialAudioConfig.randomValue)
                put("randomSpeed", config.spatialAudioConfig.randomSpeed)
            })
            
            put("equalizerConfig", JSONObject().apply {
                put("enabled", config.equalizerConfig.enabled)
                put("limiterEnabled", config.equalizerConfig.limiterEnabled)
                val gainsArray = JSONArray()
                config.equalizerConfig.gains.forEach { gainsArray.put(it) }
                put("gains", gainsArray)
            })
        }
        
        StorageManager.saveJsonSync(file, json)
    }
    
    fun getDirectories(): List<MusicDirectory> = _directories.value
    
    fun addDirectory(path: String, uri: Uri? = null) {
        if (_directories.value.any { it.path == path }) return
        
        val name = path.substringAfterLast('/')
        val newDir = MusicDirectory(
            path = path,
            uriString = uri?.toString(),
            name = name,
            isEnabled = true
        )
        
        _directories.value = _directories.value + newDir
        saveDirectories()
    }
    
    fun removeDirectory(path: String) {
        _directories.value = _directories.value.filter { it.path != path }
        saveDirectories()
    }
    
    fun setDirectoryEnabled(path: String, enabled: Boolean) {
        _directories.value = _directories.value.map { dir ->
            if (dir.path == path) dir.copy(isEnabled = enabled) else dir
        }
        saveDirectories()
    }
    
    fun clearDirectories() {
        _directories.value = emptyList()
        saveDirectories()
    }
    
    fun getEnabledDirectories(): List<MusicDirectory> = _directories.value.filter { it.isEnabled }
    
    fun hasDirectories(): Boolean = _directories.value.isNotEmpty()
    
    fun getPlaybackState(): MusicPlaybackState = _playbackState.value
    
    fun savePlaybackState(state: MusicPlaybackState) {
        _playbackState.value = state
        savePlaybackState()
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
        savePlaybackState()
    }
    
    fun clearPlaybackState() {
        _playbackState.value = MusicPlaybackState()
        savePlaybackState()
    }
    
    fun getMixerConfig(): MusicMixerConfig = _mixerConfig.value
    
    fun getReverbConfig(): ReverbConfig = _mixerConfig.value.reverbConfig
    
    fun updateReverbConfig(config: ReverbConfig) {
        _mixerConfig.value = _mixerConfig.value.copy(reverbConfig = config)
        saveMixerConfig()
    }
    
    fun setReverbEnabled(enabled: Boolean) {
        val current = _mixerConfig.value.reverbConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            reverbConfig = current.copy(enabled = enabled)
        )
        saveMixerConfig()
    }
    
    fun setReverbPreset(preset: String) {
        val current = _mixerConfig.value.reverbConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            reverbConfig = current.copy(preset = preset)
        )
        saveMixerConfig()
    }
    
    fun getVolume(): Float = _mixerConfig.value.volume
    
    fun updateVolume(volume: Float) {
        _mixerConfig.value = _mixerConfig.value.copy(volume = volume)
        saveMixerConfig()
    }
    
    fun getEffectIntensities(): EffectIntensities = _mixerConfig.value.effectIntensities
    
    fun updateEffectIntensities(intensities: EffectIntensities) {
        _mixerConfig.value = _mixerConfig.value.copy(effectIntensities = intensities)
        saveMixerConfig()
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
            else -> current
        }
        _mixerConfig.value = _mixerConfig.value.copy(effectIntensities = newIntensities)
        saveMixerConfig()
    }
    
    fun getSpatialAudioConfig(): MusicSpatialConfig = _mixerConfig.value.spatialAudioConfig
    
    fun updateSpatialAudioConfig(config: MusicSpatialConfig) {
        _mixerConfig.value = _mixerConfig.value.copy(spatialAudioConfig = config)
        saveMixerConfig()
    }
    
    fun updateSpatialAudioEnabled(enabled: Boolean) {
        val current = _mixerConfig.value.spatialAudioConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            spatialAudioConfig = current.copy(enabled = enabled)
        )
        saveMixerConfig()
    }
    
    fun updateSpatialAudioOffsetType(offsetType: Int) {
        val current = _mixerConfig.value.spatialAudioConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            spatialAudioConfig = current.copy(offsetType = offsetType)
        )
        saveMixerConfig()
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
        saveMixerConfig()
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
        saveMixerConfig()
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
        saveMixerConfig()
    }
    
    fun getEqualizerConfig(): EqualizerConfig = _mixerConfig.value.equalizerConfig
    
    fun updateEqualizerConfig(config: EqualizerConfig) {
        _mixerConfig.value = _mixerConfig.value.copy(equalizerConfig = config)
        saveMixerConfig()
    }
    
    fun updateEqualizerEnabled(enabled: Boolean) {
        val current = _mixerConfig.value.equalizerConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            equalizerConfig = current.copy(enabled = enabled)
        )
        saveMixerConfig()
    }
    
    fun updateEqualizerLimiterEnabled(enabled: Boolean) {
        val current = _mixerConfig.value.equalizerConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            equalizerConfig = current.copy(limiterEnabled = enabled)
        )
        saveMixerConfig()
    }
    
    fun updateEqualizerGains(gains: FloatArray) {
        val current = _mixerConfig.value.equalizerConfig
        _mixerConfig.value = _mixerConfig.value.copy(
            equalizerConfig = current.copy(gains = gains.copyOf())
        )
        saveMixerConfig()
    }
    
    private fun loadPlaylist() {
        val file = StorageManager.getFile("music", PLAYLIST_FILE) ?: return
        val jsonArray = StorageManager.loadJsonArray(file)
        
        if (jsonArray != null) {
            val items = mutableListOf<PlaylistItem>()
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                items.add(
                    PlaylistItem(
                        id = json.getString("id"),
                        uri = Uri.parse(json.getString("uri")),
                        title = json.getString("title"),
                        artist = json.optString("artist").takeIf { it.isNotEmpty() },
                        album = json.optString("album").takeIf { it.isNotEmpty() },
                        duration = json.optLong("duration", 0),
                        path = json.getString("path")
                    )
                )
            }
            _playlist.value = items
        }
    }
    
    private fun savePlaylist() {
        val file = StorageManager.getFile("music", PLAYLIST_FILE) ?: return
        val jsonArray = JSONArray()
        _playlist.value.forEach { item ->
            jsonArray.put(JSONObject().apply {
                put("id", item.id)
                put("uri", item.uri.toString())
                put("title", item.title)
                item.artist?.let { put("artist", it) }
                item.album?.let { put("album", it) }
                put("duration", item.duration)
                put("path", item.path)
            })
        }
        StorageManager.saveJsonSync(file, jsonArray)
    }
    
    fun getPlaylist(): List<PlaylistItem> = _playlist.value
    
    fun savePlaylist(items: List<PlaylistItem>) {
        _playlist.value = items
        savePlaylist()
    }
    
    fun clearPlaylist() {
        _playlist.value = emptyList()
        savePlaylist()
    }
    
    fun getPlaylistIndex(): Int = playlistIndex
    
    fun setPlaylistIndex(index: Int) {
        playlistIndex = index
    }
}
