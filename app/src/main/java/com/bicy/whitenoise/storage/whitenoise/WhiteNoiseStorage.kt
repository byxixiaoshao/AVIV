package com.bicy.whitenoise.storage.whitenoise

import android.util.Log
import com.bicy.whitenoise.audio.ReverbConfig
import com.bicy.whitenoise.storage.core.JsonStorageManager
import com.bicy.whitenoise.storage.core.StorageManager
import com.bicy.whitenoise.ui.components.toast.ToastManager
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.ConfigParser
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SoundCategory
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SoundMetadata
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SoundPlayConfig
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.PlaybackState
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SpatialAudioConfig
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.CreativeEffectConfig
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.ScatteredAudioClipData
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SpatialScatterRangeData
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SoundType
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
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

object WhiteNoiseStorage {
    
    private const val TAG = "WhiteNoiseStorage"

    private const val CATEGORIES_FILE = "white_noise_categories.json"
    private const val SOUNDS_FILE = "white_noise_sounds.json"
    private const val PLAYBACK_FILE = "white_noise_playback.json"
    private const val SCATTERED_SOUNDS_FILE = "sounds.json"
    
    private const val UNCATEGORIZED_ID = "uncategorized"
    const val UNCATEGORIZED_NAME = "category_uncategorized"

    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _categories = MutableStateFlow<List<SoundCategory>>(emptyList())
    val categories: StateFlow<List<SoundCategory>> = _categories.asStateFlow()
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _scatteredSounds = MutableStateFlow<List<SoundMetadata>>(emptyList())
    val scatteredSounds: StateFlow<List<SoundMetadata>> = _scatteredSounds.asStateFlow()
    
    /** In-memory cache of all sounds grouped by category */
    private val _soundsByCategory = mutableMapOf<String, List<SoundMetadata>>()
    
    private val listeners = CopyOnWriteArrayList<WeakReference<() -> Unit>>()
    
    // ===== Listener management =====
    
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
    
    // ===== Public accessors =====
    
    fun getCategories(): List<SoundCategory> = _categories.value
    
    fun getScatteredSounds(): List<SoundMetadata> = _scatteredSounds.value
    
    // ===== Initialization =====
    
    fun init() {
        loadCategories()
        loadAllSounds()
        loadPlaybackState()
        loadScatteredSounds()
        notifyListeners()
        Log.d(TAG, "WhiteNoiseStorage initialized")
    }
    
    // ===== Categories: JSON persistence =====
    
    private fun loadCategories() {
        try {
            val array = runBlocking {
                JsonStorageManager.read(CATEGORIES_FILE, Array<SoundCategory>::class.java)
            }
            if (array != null && array.isNotEmpty()) {
                _categories.value = array.toList()
            } else {
                createDefaultCategories()
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadCategories failed", e)
            createDefaultCategories()
        }
    }
    
    private fun createDefaultCategories() {
        val defaultCategories = listOf(
            SoundCategory(id = UNCATEGORIZED_ID, name = UNCATEGORIZED_NAME, isCustom = false)
        )
        _categories.value = defaultCategories
        persistCategories()
    }
    
    private fun persistCategories() {
        scope.launch {
            try {
                JsonStorageManager.write(CATEGORIES_FILE, _categories.value.toTypedArray())
            } catch (e: Exception) {
                Log.e(TAG, "saveCategories failed", e)
            }
        }
    }
    
    // ===== Load all sounds into memory =====
    
    private fun loadAllSounds() {
        try {
            val array = runBlocking {
                JsonStorageManager.read(SOUNDS_FILE, Array<SoundMetadata>::class.java)
            }
            if (array != null) {
                _soundsByCategory.clear()
                _soundsByCategory.putAll(array.groupBy { it.category })
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadAllSounds failed", e)
        }
    }
    
    private fun persistAllSounds() {
        scope.launch {
            try {
                val allSounds = _soundsByCategory.values.flatten().toTypedArray()
                JsonStorageManager.write(SOUNDS_FILE, allSounds)
            } catch (e: Exception) {
                Log.e(TAG, "persistAllSounds failed", e)
            }
        }
    }
    
    // ===== Category directory (file system for audio files) =====
    
    fun getCategoryDir(categoryName: String): File? {
        return try {
            File(AppInitializer.getContext().filesDir, "white_noise/library/$categoryName")
        } catch (e: Exception) {
            Log.e(TAG, "getCategoryDir failed", e)
            null
        }
    }
    
    // ===== Sounds: in-memory with JSON persistence =====
    
    fun getSounds(categoryName: String): List<SoundMetadata> {
        return _soundsByCategory[categoryName] ?: emptyList()
    }
    
    fun saveSounds(categoryName: String, sounds: List<SoundMetadata>) {
        _soundsByCategory[categoryName] = sounds
        persistAllSounds()
    }
    
    // ===== Sound metadata: merged into SoundMetadata (no separate entity needed) =====
    
    fun getSoundMetadata(categoryName: String, soundName: String): SoundMetadata? {
        return _soundsByCategory[categoryName]?.firstOrNull { it.name == soundName }
    }
    
    fun saveSoundMetadata(categoryName: String, soundName: String, metadata: SoundMetadata) {
        val sounds = _soundsByCategory[categoryName]?.toMutableList() ?: return
        val index = sounds.indexOfFirst { it.name == soundName }
        if (index >= 0) {
            sounds[index] = sounds[index].copy(
                type = metadata.type,
                downloadDate = metadata.downloadDate,
                fileSize = metadata.fileSize
            )
            _soundsByCategory[categoryName] = sounds
            persistAllSounds()
        }
    }
    
    // ===== Category management =====
    
    fun addCategory(name: String): SoundCategory {
        val newCategory = SoundCategory(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            isCustom = true
        )
        _categories.value = _categories.value + newCategory
        persistCategories()
        
        val categoryDir = getCategoryDir(name)
        categoryDir?.mkdirs()
        saveSounds(name, emptyList())
        
        return newCategory
    }
    
    fun deleteCategory(categoryName: String): Boolean {
        if (categoryName == UNCATEGORIZED_NAME) return false
        
        _categories.value = _categories.value.filter { it.name != categoryName }
        persistCategories()
        
        _soundsByCategory.remove(categoryName)
        persistAllSounds()
        
        // Delete category directory from file system (audio files)
        getCategoryDir(categoryName)?.deleteRecursively()
        ToastManager.success("已删除类别「$categoryName」")
        return true
    }
    
    // ===== Sound management =====
    
    fun addSound(categoryName: String, metadata: SoundMetadata) {
        val sounds = (_soundsByCategory[categoryName] ?: emptyList()).toMutableList()
        sounds.add(metadata)
        saveSounds(categoryName, sounds)
        
        val soundDir = getCategoryDir(categoryName)?.let { File(it, metadata.name) }
        soundDir?.mkdirs()
        ToastManager.success("添加成功：${metadata.name}")
    }
    
    fun deleteSound(categoryName: String, soundName: String): Boolean {
        val sounds = (_soundsByCategory[categoryName] ?: emptyList()).toMutableList()
        val updatedSounds = sounds.filter { it.name != soundName }
        saveSounds(categoryName, updatedSounds)
        
        getCategoryDir(categoryName)?.let { File(it, soundName) }?.deleteRecursively()
        ToastManager.success("已删除「$soundName」")
        return true
    }
    
    fun removeSound(categoryName: String, soundId: String) {
        val sounds = (_soundsByCategory[categoryName] ?: emptyList()).toMutableList()
        val updatedSounds = sounds.filter { it.id != soundId }
        saveSounds(categoryName, updatedSounds)
    }
    
    fun updateSound(categoryName: String, metadata: SoundMetadata) {
        val sounds = (_soundsByCategory[categoryName] ?: emptyList()).toMutableList()
        val index = sounds.indexOfFirst { it.id == metadata.id }
        if (index >= 0) {
            sounds[index] = metadata
            saveSounds(categoryName, sounds)
        }
    }
    
    fun toggleFavorite(soundId: String) {
        val categories = _categories.value
        for (category in categories) {
            val sounds = (_soundsByCategory[category.name] ?: emptyList()).toMutableList()
            val index = sounds.indexOfFirst { it.id == soundId }
            if (index >= 0) {
                val toggled = sounds[index].copy(isFavorite = !sounds[index].isFavorite)
                sounds[index] = toggled
                saveSounds(category.name, sounds)
                ToastManager.success(if (toggled.isFavorite) "已添加到收藏" else "已从收藏移除")
                break
            }
        }
    }
    
    // ===== Playback state: JSON persistence =====
    
    private fun loadPlaybackState() {
        try {
            val state = runBlocking {
                JsonStorageManager.read(PLAYBACK_FILE, PlaybackState::class.java)
            }
            if (state != null) {
                _playbackState.value = state
                Log.d(TAG, "loadPlaybackState: loaded ${state.sounds.size} sounds, isPaused = ${state.isPaused}")
            } else {
                Log.d(TAG, "loadPlaybackState: no saved config, using defaults")
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadPlaybackState failed", e)
        }
    }
    
    private fun persistPlaybackState() {
        scope.launch {
            try {
                JsonStorageManager.write(PLAYBACK_FILE, _playbackState.value)
            } catch (e: Exception) {
                Log.e(TAG, "persistPlaybackState failed", e)
            }
        }
    }
    
    // ===== Playback state public API =====
    
    fun getPlaybackState(): PlaybackState = _playbackState.value
    
    fun addPlayingSound(sound: SoundPlayConfig) {
        val currentSounds = _playbackState.value.sounds.toMutableList()
        if (currentSounds.none { it.id == sound.id }) {
            currentSounds.add(sound)
            _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
            persistPlaybackState()
            notifyListeners()
        }
    }
    
    fun removePlayingSound(soundId: String) {
        val currentSounds = _playbackState.value.sounds.filter { it.id != soundId }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        persistPlaybackState()
        notifyListeners()
    }
    
    fun updatePlayingSoundVolume(soundId: String, volume: Float) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == soundId) sound.copy(volume = volume) else sound
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        persistPlaybackState()
    }
    
    fun updatePlayingSoundReverb(soundId: String, config: ReverbConfig) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == soundId) sound.copy(reverbConfig = config) else sound
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        persistPlaybackState()
    }
    
    fun updatePlayingSoundSpatial(soundId: String, config: SpatialAudioConfig) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == soundId) sound.copy(spatialAudioConfig = config) else sound
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        persistPlaybackState()
    }
    
    fun updatePlayingSoundCreative(soundId: String, config: CreativeEffectConfig) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == soundId) sound.copy(creativeEffectConfig = config) else sound
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        persistPlaybackState()
    }

    fun updatePlayingSoundEqEnabled(soundId: String, enabled: Boolean) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == soundId) sound.copy(eqEnabled = enabled) else sound
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        persistPlaybackState()
    }

    // 更新播放速度/音调（按轨道独立持久化）
    fun updatePlayingSoundSpeed(soundId: String, speed: Float, pitch: Float) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == soundId) sound.copy(playbackSpeed = speed, pitchShift = pitch) else sound
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        persistPlaybackState()
    }

    fun countEqEnabledSounds(): Int =
        _playbackState.value.sounds.count { it.eqEnabled }

    
    fun setPlaybackPaused(paused: Boolean) {
        _playbackState.value = _playbackState.value.copy(isPaused = paused)
        persistPlaybackState()
        notifyListeners()
    }
    
    fun clearPlayback() {
        _playbackState.value = PlaybackState()
        persistPlaybackState()
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
        persistPlaybackState()
        notifyListeners()
    }
    
    fun removeAudioClipFromTrack(trackId: String, clipId: String) {
        val currentSounds = _playbackState.value.sounds.map { sound ->
            if (sound.id == trackId) {
                sound.copy(audioClips = sound.audioClips.filter { it.id != clipId })
            } else {
                sound
            }
        }
        _playbackState.value = _playbackState.value.copy(sounds = currentSounds)
        persistPlaybackState()
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
        persistPlaybackState()
        notifyListeners()
    }
    
    // ===== Scattered sounds: keep using JSON file persistence =====
    
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
    
    // ===== File system helpers for audio files =====
    
    fun getSoundFile(categoryName: String, soundName: String, format: String): File? {
        val soundDir = getCategoryDir(categoryName)?.let { File(it, soundName) } ?: return null
        if (!soundDir.exists()) return null
        
        val file = File(soundDir, "$soundName.$format")
        return if (file.exists() && file.length() > 0) file else null
    }
}
