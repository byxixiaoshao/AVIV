package com.bicy.whitenoise.JwJY.sBYh

import android.util.Log
import com.bicy.whitenoise.H3HO.ReverbConfig
import com.bicy.whitenoise.JwJY.Jauc.StorageManager
import com.bicy.whitenoise.JwJY.sBYh.kcFp.ConfigParser
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SoundCategory
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SoundMetadata
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SoundPlayConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.PlaybackState
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialAudioConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.CreativeEffectConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.ScatteredAudioClipData
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialScatterRangeData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

object WhiteNoiseStorage {
    
    private const val TAG = "WhiteNoiseStorage"
    
    private const val CATEGORIES_FILE = "categories.json"
    private const val SOUNDS_FILE = "sounds.json"
    private const val METADATA_FILE = "metadata.json"
    private const val PLAYBACK_CONFIG_FILE = "playback_config.json"
    private const val SCATTERED_SOUNDS_FILE = "sounds.json"
    
    private const val UNCATEGORIZED_ID = "uncategorized"
    const val UNCATEGORIZED_NAME = "category_uncategorized"
    
    private val _categories = MutableStateFlow<List<SoundCategory>>(emptyList())
    val categories: StateFlow<List<SoundCategory>> = _categories.asStateFlow()
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _scatteredSounds = MutableStateFlow<List<SoundMetadata>>(emptyList())
    val scatteredSounds: StateFlow<List<SoundMetadata>> = _scatteredSounds.asStateFlow()
    
    private val listeners = CopyOnWriteArrayList<WeakReference<() -> Unit>>()
    
    fun addListener(listener: () -> Unit) {
        listeners.add(WeakReference(listener))
    }
    
    fun removeListener(listener: () -> Unit) {
        listeners.removeAll { it.get() == listener }
    }
    
    private fun notifyListeners() {
        listeners.removeAll { it.get() == null }
        listeners.forEach { it.get()?.invoke() }
    }
    
    fun getCategories(): List<SoundCategory> = _categories.value
    
    fun getScatteredSounds(): List<SoundMetadata> = _scatteredSounds.value
    
    fun init() {
        loadCategories()
        loadPlaybackState()
        loadScatteredSounds()
        notifyListeners()
        Log.d(TAG, "WhiteNoiseStorage initialized")
    }
    
    private fun loadCategories() {
        val file = StorageManager.getFile("white_noise", "library", CATEGORIES_FILE) ?: return
        val jsonArray = StorageManager.loadJsonArray(file)
        
        if (jsonArray != null) {
            val categoryList = mutableListOf<SoundCategory>()
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                categoryList.add(
                    SoundCategory(
                        id = json.getString("id"),
                        name = json.getString("name"),
                        isCustom = json.optBoolean("isCustom", false)
                    )
                )
            }
            _categories.value = categoryList
        } else {
            createDefaultCategories()
        }
    }
    
    private fun createDefaultCategories() {
        val defaultCategories = listOf(
            SoundCategory(id = UNCATEGORIZED_ID, name = UNCATEGORIZED_NAME, isCustom = false)
        )
        _categories.value = defaultCategories
        saveCategories()
    }
    
    private fun saveCategories() {
        val file = StorageManager.getFile("white_noise", "library", CATEGORIES_FILE) ?: return
        val jsonArray = JSONArray()
        _categories.value.forEach { category ->
            jsonArray.put(JSONObject().apply {
                put("id", category.id)
                put("name", category.name)
                put("isCustom", category.isCustom)
            })
        }
        StorageManager.saveJsonSync(file, jsonArray)
    }
    
    fun getCategoryDir(categoryName: String): File? {
        return StorageManager.getFile("white_noise", "library", categoryName)
    }
    
    fun getSounds(categoryName: String): List<SoundMetadata> {
        val file = StorageManager.getFile("white_noise", "library", categoryName, SOUNDS_FILE) ?: return emptyList()
        val jsonArray = StorageManager.loadJsonArray(file) ?: return emptyList()
        
        val soundList = mutableListOf<SoundMetadata>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            soundList.add(ConfigParser.parseSoundMetadata(json))
        }
        return soundList
    }
    
    fun saveSounds(categoryName: String, sounds: List<SoundMetadata>) {
        val file = StorageManager.getFile("white_noise", "library", categoryName, SOUNDS_FILE) ?: return
        val jsonArray = JSONArray()
        sounds.forEach { sound ->
            jsonArray.put(ConfigParser.toJson(sound))
        }
        StorageManager.saveJsonSync(file, jsonArray)
    }
    
    fun getSoundMetadata(categoryName: String, soundName: String): SoundMetadata? {
        val file = StorageManager.getFile("white_noise", "library", categoryName, soundName, METADATA_FILE) ?: return null
        val json = StorageManager.loadJson(file) ?: return null
        return ConfigParser.parseSoundMetadata(json)
    }
    
    fun saveSoundMetadata(categoryName: String, soundName: String, metadata: SoundMetadata) {
        val file = StorageManager.getFile("white_noise", "library", categoryName, soundName, METADATA_FILE) ?: return
        StorageManager.saveJsonSync(file, ConfigParser.toJson(metadata))
    }
    
    fun addCategory(name: String): SoundCategory {
        val newCategory = SoundCategory(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            isCustom = true
        )
        _categories.value = _categories.value + newCategory
        saveCategories()
        
        val categoryDir = getCategoryDir(name)
        categoryDir?.mkdirs()
        saveSounds(name, emptyList())
        
        return newCategory
    }
    
    fun deleteCategory(categoryName: String): Boolean {
        if (categoryName == UNCATEGORIZED_NAME) return false
        
        _categories.value = _categories.value.filter { it.name != categoryName }
        saveCategories()
        
        StorageManager.deleteFile("white_noise", "library", categoryName)
        return true
    }
    
    fun addSound(categoryName: String, metadata: SoundMetadata) {
        val sounds = getSounds(categoryName).toMutableList()
        sounds.add(metadata)
        saveSounds(categoryName, sounds)
        
        val soundDir = StorageManager.getFile("white_noise", "library", categoryName, metadata.name)
        soundDir?.mkdirs()
        saveSoundMetadata(categoryName, metadata.name, metadata)
    }
    
    fun deleteSound(categoryName: String, soundName: String): Boolean {
        val sounds = getSounds(categoryName).toMutableList()
        val updatedSounds = sounds.filter { it.name != soundName }
        saveSounds(categoryName, updatedSounds)
        
        StorageManager.deleteFile("white_noise", "library", categoryName, soundName)
        return true
    }
    
    fun removeSound(categoryName: String, soundId: String) {
        val sounds = getSounds(categoryName).toMutableList()
        val updatedSounds = sounds.filter { it.id != soundId }
        saveSounds(categoryName, updatedSounds)
    }
    
    fun updateSound(categoryName: String, metadata: SoundMetadata) {
        val sounds = getSounds(categoryName).toMutableList()
        val index = sounds.indexOfFirst { it.id == metadata.id }
        if (index >= 0) {
            sounds[index] = metadata
            saveSounds(categoryName, sounds)
            saveSoundMetadata(categoryName, metadata.name, metadata)
        }
    }
    
    fun toggleFavorite(soundId: String) {
        val categories = _categories.value
        for (category in categories) {
            val sounds = getSounds(category.name).toMutableList()
            val index = sounds.indexOfFirst { it.id == soundId }
            if (index >= 0) {
                sounds[index] = sounds[index].copy(isFavorite = !sounds[index].isFavorite)
                saveSounds(category.name, sounds)
                break
            }
        }
    }
    
    private fun loadPlaybackState() {
        val file = StorageManager.getFile("white_noise", PLAYBACK_CONFIG_FILE)
        if (file == null) {
            Log.e(TAG, "loadPlaybackState: file is null")
            return
        }
        
        Log.d(TAG, "loadPlaybackState: file path = ${file.absolutePath}, exists = ${file.exists()}")
        
        val json = StorageManager.loadJson(file)
        if (json == null) {
            Log.e(TAG, "loadPlaybackState: json is null")
            notifyListeners()
            return
        }
        
        val soundsArray = json.optJSONArray("sounds")
        val sounds = mutableListOf<SoundPlayConfig>()
        
        if (soundsArray != null) {
            for (i in 0 until soundsArray.length()) {
                val soundJson = soundsArray.getJSONObject(i)
                sounds.add(ConfigParser.parseSoundPlayConfig(soundJson))
            }
        }
        
        Log.d(TAG, "loadPlaybackState: loaded ${sounds.size} sounds, isPaused = ${json.optBoolean("isPaused", false)}")
        
        _playbackState.value = PlaybackState(
            isPaused = json.optBoolean("isPaused", false),
            sounds = sounds
        )
        notifyListeners()
    }
    
    private fun savePlaybackState() {
        val file = StorageManager.getFile("white_noise", PLAYBACK_CONFIG_FILE) ?: return
        val state = _playbackState.value
        
        val json = JSONObject().apply {
            put("isPaused", state.isPaused)
            
            val soundsArray = JSONArray()
            state.sounds.forEach { sound ->
                soundsArray.put(ConfigParser.toJson(sound))
            }
            put("sounds", soundsArray)
        }
        
        StorageManager.saveJsonSync(file, json)
    }
    
    fun getPlaybackState(): PlaybackState = _playbackState.value
    
    fun addPlayingSound(sound: SoundPlayConfig) {
        val currentSounds = _playbackState.value.sounds.toMutableList()
        if (currentSounds.none { it.id == sound.id }) {
            currentSounds.add(sound)
            _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
            savePlaybackState()
            notifyListeners()
        }
    }
    
    fun removePlayingSound(soundId: String) {
        val currentSounds = _playbackState.value.sounds.filter { it.id != soundId }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        savePlaybackState()
        notifyListeners()
    }
    
    fun updatePlayingSoundVolume(soundId: String, volume: Float) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == soundId) sound.copy(volume = volume) else sound
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        savePlaybackState()
    }
    
    fun updatePlayingSoundReverb(soundId: String, config: ReverbConfig) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == soundId) sound.copy(reverbConfig = config) else sound
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        savePlaybackState()
    }
    
    fun updatePlayingSoundSpatial(soundId: String, config: SpatialAudioConfig) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == soundId) sound.copy(spatialAudioConfig = config) else sound
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        savePlaybackState()
    }
    
    fun updatePlayingSoundCreative(soundId: String, config: CreativeEffectConfig) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == soundId) sound.copy(creativeEffectConfig = config) else sound
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        savePlaybackState()
    }
    
    fun setPlaybackPaused(paused: Boolean) {
        _playbackState.value = _playbackState.value.copy(isPaused = paused)
        savePlaybackState()
        notifyListeners()
    }
    
    fun clearPlayback() {
        _playbackState.value = PlaybackState()
        savePlaybackState()
        notifyListeners()
    }
    
    fun addAudioClipToTrack(trackId: String, clip: ScatteredAudioClipData) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == trackId) {
                sound.copy(audioClips = sound.audioClips + clip)
            } else {
                sound
            }
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        savePlaybackState()
        notifyListeners()
    }
    
    fun updateScatteredTrackConfig(
        trackId: String,
        minIntervalMs: Long? = null,
        maxIntervalMs: Long? = null,
        spatialScatterRange: SpatialScatterRangeData? = null,
        spatialScatterEnabled: Boolean? = null,
        overlayMode: Boolean? = null
    ) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == trackId) {
                sound.copy(
                    minIntervalMs = minIntervalMs ?: sound.minIntervalMs,
                    maxIntervalMs = maxIntervalMs ?: sound.maxIntervalMs,
                    spatialScatterRange = spatialScatterRange ?: sound.spatialScatterRange,
                    spatialScatterEnabled = spatialScatterEnabled ?: sound.spatialScatterEnabled,
                    overlayMode = overlayMode ?: sound.overlayMode
                )
            } else {
                sound
            }
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        savePlaybackState()
        notifyListeners()
    }
    
    private fun loadScatteredSounds() {
        val file = StorageManager.getFile("white_noise", "scattered", SCATTERED_SOUNDS_FILE) ?: return
        val jsonArray = StorageManager.loadJsonArray(file)
        
        if (jsonArray != null) {
            val soundList = mutableListOf<SoundMetadata>()
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                soundList.add(ConfigParser.parseSoundMetadata(json))
            }
            _scatteredSounds.value = soundList
        }
    }
    
    private fun saveScatteredSounds() {
        val file = StorageManager.getFile("white_noise", "scattered", SCATTERED_SOUNDS_FILE) ?: return
        val jsonArray = JSONArray()
        _scatteredSounds.value.forEach { sound ->
            jsonArray.put(ConfigParser.toJson(sound))
        }
        StorageManager.saveJsonSync(file, jsonArray)
    }
    
    fun addScatteredSound(sound: SoundMetadata) {
        val currentSounds = _scatteredSounds.value.toMutableList()
        if (currentSounds.none { it.id == sound.id }) {
            currentSounds.add(sound)
            _scatteredSounds.value = currentSounds
            saveScatteredSounds()
        }
    }
    
    fun removeScatteredSound(soundId: String) {
        _scatteredSounds.value = _scatteredSounds.value.filter { it.id != soundId }
        saveScatteredSounds()
    }
    
    fun updateScatteredSound(sound: SoundMetadata) {
        val currentSounds = _scatteredSounds.value.map { 
            if (it.id == sound.id) sound else it 
        }
        _scatteredSounds.value = currentSounds
        saveScatteredSounds()
    }
    
    fun clearScatteredSounds() {
        _scatteredSounds.value = emptyList()
        saveScatteredSounds()
    }
    
    fun getSoundFile(categoryName: String, soundName: String, format: String): File? {
        val soundDir = StorageManager.getFile("white_noise", "library", categoryName, soundName) ?: return null
        if (!soundDir.exists()) return null
        
        val file = File(soundDir, "$soundName.$format")
        return if (file.exists() && file.length() > 0) file else null
    }
}
