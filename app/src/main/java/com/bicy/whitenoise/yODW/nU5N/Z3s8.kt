package com.bicy.whitenoise.yODW.nU5N

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.whitenoise.StMb.TrackType
import com.bicy.whitenoise.rgRE.MusicService
import com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SoundPlayConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialScatterRangeData
import com.bicy.whitenoise.JwJY.sBYh.kcFp.ScatteredAudioClipData
import com.bicy.whitenoise.DzBD.PeeU.Function
import com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundCategory
import com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundMetadata
import com.bicy.whitenoise.y10p.DownloadManager
import com.bicy.whitenoise.y10p.SoundStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
data class PlayingSound(
    val id: String,
    val name: String,
    val volume: Float = 1.0f,
    val reverbConfig: com.bicy.whitenoise.H3HO.ReverbConfig = com.bicy.whitenoise.H3HO.ReverbConfig(),
    val trackType: TrackType = TrackType.LOOP,
    val audioClipCount: Int = 0,
    val translations: Map<String, String>? = null,
    val minIntervalMs: Long = 3000,
    val maxIntervalMs: Long = 10000,
    val spatialScatterEnabled: Boolean = false
)

@Stable
data class CategoryWithSounds(
    val category: SoundCategory,
    val sounds: List<SoundMetadata>,
    val isExpanded: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    
    private val _categories = MutableStateFlow<List<CategoryWithSounds>>(emptyList())
    val categories: StateFlow<List<CategoryWithSounds>> = _categories.asStateFlow()
    
    private val _playingSounds = MutableStateFlow<List<PlayingSound>>(emptyList())
    val playingSounds: StateFlow<List<PlayingSound>> = _playingSounds.asStateFlow()
    
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()
    
    private val _playingStates = MutableStateFlow<Set<String>>(emptySet())
    val playingStates: StateFlow<Set<String>> = _playingStates.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        setupListeners()
        updatePlayingSounds()
        loadSoundsFromCache()
    }
    
    private fun setupListeners() {
        Function.setDownloadProgressListener { soundId, progress ->
            viewModelScope.launch {
                _downloadProgress.update { it + (soundId to progress) }
            }
        }
        
        MusicService.onPlaybackStateChangeListener = { soundId, isPlaying ->
            viewModelScope.launch {
                _playingStates.update { currentStates ->
                    val mutableStates = currentStates.toMutableSet()
                    if (isPlaying) {
                        mutableStates.add(soundId)
                    } else {
                        mutableStates.remove(soundId)
                    }
                    mutableStates.toSet()
                }
                
                val currentPlayingIds = MusicService.getInstance()?.getPlayingSounds() ?: emptySet()
                val hasPlayingSounds = _playingSounds.value.isNotEmpty()
                
                if (isPlaying && currentPlayingIds.isNotEmpty()) {
                    if (_isPaused.value) {
                        WhiteNoiseStorage.setPlaybackPaused(false)
                        _isPaused.value = false
                    }
                } else if (!isPlaying && currentPlayingIds.isEmpty() && hasPlayingSounds) {
                    if (!_isPaused.value) {
                        WhiteNoiseStorage.setPlaybackPaused(true)
                        _isPaused.value = true
                    }
                }
            }
        }
        
        WhiteNoiseStorage.addListener {
            viewModelScope.launch {
                syncPlayListState()
            }
        }
    }
    
    private fun syncPlayListState() {
        val servicePlayingIds = MusicService.getInstance()?.getPlayingSounds() ?: emptySet()
        _playingStates.value = servicePlayingIds
        
        val playListSounds = WhiteNoiseStorage.getPlaybackState().sounds
        if (playListSounds.isEmpty()) {
            if (_isPaused.value) {
                WhiteNoiseStorage.setPlaybackPaused(false)
            }
            _isPaused.value = false
        } else {
            _isPaused.value = WhiteNoiseStorage.getPlaybackState().isPaused
        }
        
        updatePlayingSounds()
    }
    
    private fun loadSoundsFromCache() {
        viewModelScope.launch {
            _isLoading.value = true
            loadSoundsInternal()
            syncPlayListState()
        }
    }
    
    private suspend fun loadSoundsInternal() {
        val result = withContext(Dispatchers.IO) {
            val customClasses = SoundStorageManager.loadSoundsClass(context)
            val categoriesWithSounds = mutableListOf<CategoryWithSounds>()
            
            customClasses.forEach { soundClass ->
                val soundItems = SoundStorageManager.loadSoundsList(context, soundClass.name)
                
                val sounds = soundItems.map { soundItem ->
                    val soundType = SoundStorageManager.loadSoundType(context, soundClass.name, soundItem.name)
                    
                    SoundMetadata(
                        id = soundItem.id,
                        name = soundType?.nameKey ?: soundItem.name,
                        category = soundClass.id,
                        categoryName = soundClass.name,
                        remoteUrl = soundType?.downloadUrl ?: soundItem.remoteUrl ?: "",
                        author = soundType?.author ?: soundItem.author ?: "",
                        authorUrl = soundType?.authorUrl ?: soundItem.authorUrl ?: "",
                        translations = soundType?.translations
                    )
                }
                
                categoriesWithSounds.add(
                    CategoryWithSounds(
                        category = SoundCategory(
                            id = soundClass.id,
                            name = soundClass.name
                        ),
                        sounds = sounds
                    )
                )
            }
            
            categoriesWithSounds
        }
        
        _categories.value = result
        _isLoading.value = false
    }
    
    fun loadSounds() {
        viewModelScope.launch {
            _isLoading.value = true
            loadSoundsInternal()
        }
    }
    
    fun deleteSound(categoryName: String, soundName: String, soundId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                SoundStorageManager.deleteSound(context, categoryName, soundName)
                DownloadManager.deleteCache(context, soundId)
            }
            loadSoundsInternal()
        }
    }
    
    fun toggleCategory(categoryId: String) {
        val currentList = _categories.value.toMutableList()
        val index = currentList.indexOfFirst { it.category.id == categoryId }
        if (index >= 0) {
            val item = currentList[index]
            currentList[index] = item.copy(isExpanded = !item.isExpanded)
            _categories.value = currentList
        }
    }
    
    fun onSoundClick(sound: SoundMetadata) {
        if (Function.isDownloading(sound.id)) {
            return
        }
        
        val isCurrentlyPlaying = MusicService.getInstance()?.isSoundPlaying(sound.id) ?: false
        
        if (isCurrentlyPlaying) {
            stopSound(sound.id)
        } else if (Function.isCached(context, sound.id)) {
            playSound(sound)
        } else {
            downloadAndPlaySound(sound)
        }
    }
    
    private fun playSound(sound: SoundMetadata) {
        val reverbConfig = com.bicy.whitenoise.H3HO.ReverbManager.getConfig(sound.id) ?: com.bicy.whitenoise.H3HO.ReverbConfig()
        val soundConfig = SoundPlayConfig(
            id = sound.id,
            name = sound.name,
            volume = 1f,
            reverbConfig = reverbConfig,
            translations = sound.translations
        )
        WhiteNoiseStorage.addPlayingSound(soundConfig)
        Function.playSound(context, sound)
        
        val currentStates = _playingStates.value.toMutableSet()
        currentStates.add(sound.id)
        _playingStates.value = currentStates
        
        updatePlayingSounds()
    }
    
    private fun stopSound(soundId: String) {
        WhiteNoiseStorage.removePlayingSound(soundId)
        Function.stopSound(context, soundId)
        
        val currentStates = _playingStates.value.toMutableSet()
        currentStates.remove(soundId)
        _playingStates.value = currentStates
        
        updatePlayingSounds()
    }
    
    private fun downloadAndPlaySound(sound: SoundMetadata) {
        Function.downloadAudio(
            context = context,
            sound = sound,
            onProgress = { progress ->
                viewModelScope.launch {
                    _downloadProgress.value = _downloadProgress.value + (sound.id to progress)
                }
            },
            onComplete = { success ->
                viewModelScope.launch {
                    val currentProgress = _downloadProgress.value.toMutableMap()
                    currentProgress.remove(sound.id)
                    _downloadProgress.value = currentProgress
                    
                    if (success) {
                        playSound(sound)
                    }
                }
            }
        )
    }
    
    fun updatePlayingSounds() {
        val sounds = WhiteNoiseStorage.getPlaybackState().sounds
        Log.d("MainViewModel", "updatePlayingSounds: ${sounds.size} sounds loaded")
        sounds.forEach { sound ->
            Log.d("MainViewModel", "  - ${sound.name}, type=${sound.trackType}, volume=${sound.volume}")
        }
        
        val playingSoundsList = sounds.map { sound ->
            PlayingSound(
                id = sound.id,
                name = sound.name,
                volume = sound.volume,
                reverbConfig = sound.reverbConfig,
                translations = sound.translations,
                trackType = if (sound.trackType == "scattered") TrackType.SCATTERED else TrackType.LOOP,
                audioClipCount = sound.audioClips.size,
                minIntervalMs = sound.minIntervalMs,
                maxIntervalMs = sound.maxIntervalMs,
                spatialScatterEnabled = sound.spatialScatterEnabled
            )
        }
        _playingSounds.value = playingSoundsList
        _isPaused.value = WhiteNoiseStorage.getPlaybackState().isPaused
        Log.d("MainViewModel", "updatePlayingSounds: _playingSounds updated to ${playingSoundsList.size}")
    }
    
    fun removePlayingSound(soundId: String) {
        val sound = _playingSounds.value.find { it.id == soundId }
        if (sound?.trackType == com.bicy.whitenoise.StMb.TrackType.SCATTERED) {
            MusicService.getInstance()?.unregisterScatteredTrack(soundId)
        } else {
            MusicService.getInstance()?.stopSound(soundId)
        }
        WhiteNoiseStorage.removePlayingSound(soundId)
        
        val currentStates = _playingStates.value.toMutableSet()
        currentStates.remove(soundId)
        _playingStates.value = currentStates
        
        updatePlayingSounds()
    }
    
    fun setVolume(soundId: String, volume: Float) {
        MusicService.getInstance()?.setVolume(soundId, volume)
        WhiteNoiseStorage.updatePlayingSoundVolume(soundId, volume)
        
        val currentList = _playingSounds.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == soundId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(volume = volume)
            _playingSounds.value = currentList
        }
    }
    
    fun togglePauseResume() {
        val playingSounds = WhiteNoiseStorage.getPlaybackState().sounds
        if (playingSounds.isEmpty()) {
            return
        }
        
        val newPausedState = !_isPaused.value
        WhiteNoiseStorage.setPlaybackPaused(newPausedState)
        
        val service = MusicService.getInstance()
        if (service != null) {
            if (newPausedState) {
                playingSounds.forEach { sound ->
                    if (sound.trackType == "scattered") {
                        com.bicy.whitenoise.H3HO.ScatteredPlayerManager.pauseTrack(sound.id)
                    } else {
                        service.pauseSound(sound.id)
                    }
                }
            } else {
                playingSounds.forEach { sound ->
                    if (sound.trackType == "scattered") {
                        com.bicy.whitenoise.H3HO.ScatteredPlayerManager.resumeTrack(sound.id)
                    } else {
                        val cachedFile = com.bicy.whitenoise.y10p.DownloadManager.getCachedFile(context, sound.id)
                        if (cachedFile != null && cachedFile.exists()) {
                            if (service.isSoundPlaying(sound.id)) {
                                service.resumeSound(sound.id)
                            } else {
                                service.playSound(sound.id, cachedFile, sound.name)
                                service.setVolume(sound.id, sound.volume)
                            }
                        }
                    }
                }
            }
        }
        
        _isPaused.value = newPausedState
    }
    
    fun startMusicService() {
        val serviceIntent = Intent(context, MusicService::class.java)
        context.startForegroundService(serviceIntent)
    }
    
    fun isSoundPlaying(soundId: String): Boolean {
        return _playingStates.value.contains(soundId)
    }
    
    fun getDownloadProgress(soundId: String): Float {
        return _downloadProgress.value[soundId] ?: 0f
    }
    
    fun createEmptyScatteredGroup(name: String) {
        val groupId = "scattered_${System.currentTimeMillis()}"
        val soundConfig = SoundPlayConfig(
            id = groupId,
            name = name,
            volume = 1.0f,
            trackType = "scattered",
            audioClips = emptyList(),
            minIntervalMs = 3000,
            maxIntervalMs = 10000,
            spatialScatterEnabled = false
        )
        
        WhiteNoiseStorage.addPlayingSound(soundConfig)
        
        MusicService.getInstance()?.registerScatteredTrack(
            trackId = groupId,
            audioClips = emptyList<ScatteredAudioClipData>(),
            minIntervalMs = 3000,
            maxIntervalMs = 10000,
            volume = 1.0f,
            spatialRange = SpatialScatterRangeData(),
            spatialEnabled = false,
            overlayMode = false
        )
        
        updatePlayingSounds()
        startMusicService()
    }
    
    fun addCategory(name: String) {
        viewModelScope.launch {
            SoundStorageManager.addCategory(context, name)
            loadSounds()
        }
    }
    
    fun addNetworkSound(
        categoryName: String, 
        soundName: String, 
        downloadUrl: String,
        author: String? = null,
        authorUrl: String? = null
    ) {
        viewModelScope.launch {
            val soundType = SoundStorageManager.SoundType(
                type = SoundStorageManager.SoundSourceType.NETWORK_DOWNLOAD,
                nameKey = soundName,
                downloadUrl = downloadUrl,
                author = author,
                authorUrl = authorUrl,
                synthesisParams = null
            )
            
            SoundStorageManager.addSound(
                context = context,
                categoryName = categoryName,
                name = soundName,
                soundType = soundType
            )
            
            loadSounds()
        }
    }
    
    fun addLocalSound(categoryName: String, soundName: String, fileUri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                if (inputStream != null) {
                    val fileName = fileUri.lastPathSegment ?: "audio"
                    val format = when {
                        fileName.endsWith(".mp3", ignoreCase = true) -> "mp3"
                        fileName.endsWith(".ogg", ignoreCase = true) -> "ogg"
                        fileName.endsWith(".wav", ignoreCase = true) -> "wav"
                        else -> "mp3"
                    }
                    
                    val soundType = SoundStorageManager.SoundType(
                        type = SoundStorageManager.SoundSourceType.LOCAL_IMPORT,
                        nameKey = soundName,
                        downloadUrl = null,
                        synthesisParams = null
                    )
                    
                    val soundItem = SoundStorageManager.addSound(
                        context = context,
                        categoryName = categoryName,
                        name = soundName,
                        soundType = soundType
                    )
                    
                    val soundFile = SoundStorageManager.getSoundFile(context, categoryName, soundName, format)
                    soundFile.parentFile?.mkdirs()
                    inputStream.use { input ->
                        soundFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    loadSounds()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "添加本地音频失败: ${e.message}")
            }
        }
    }
    
    fun setReverbConfig(soundId: String, config: com.bicy.whitenoise.H3HO.ReverbConfig) {
        com.bicy.whitenoise.H3HO.ReverbManager.setConfig(soundId, config)
        WhiteNoiseStorage.updatePlayingSoundReverb(soundId, config)
        MusicService.getInstance()?.setEffectEnabled(soundId, true)
        MusicService.getInstance()?.setReverbParams(soundId, config.roomSize, config.damping, config.wetLevel)
        //com.bicy.whitenoise.H3HO.ReverbManager.applyReverbConfig(soundId)
        
        val currentList = _playingSounds.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == soundId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(reverbConfig = config)
            _playingSounds.value = currentList
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Function.clearDownloadProgressListener()
        MusicService.onPlaybackStateChangeListener = null
    }
}
