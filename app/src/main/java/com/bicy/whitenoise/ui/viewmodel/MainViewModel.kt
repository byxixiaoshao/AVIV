package com.bicy.whitenoise.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.whitenoise.StMb.ScatteredTrackDataPart.TrackType
import com.bicy.whitenoise.servies.MusicService
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SoundPlayConfig
import com.bicy.whitenoise.ui.components.toast.ToastManager
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SpatialScatterRangeData
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.ScatteredAudioClipData
import com.bicy.whitenoise.subPage.home.Function
import com.bicy.whitenoise.subPage.home.model.SoundMetadataPart.SoundCategory
import com.bicy.whitenoise.subPage.home.model.SoundMetadataPart.*
import com.bicy.whitenoise.utils.DownloadManager
import com.bicy.whitenoise.utils.SoundStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Stable
data class PlayingSound(
    val id: String,
    val name: String,
    val volume: Float = 1.0f,
    val reverbConfig: com.bicy.whitenoise.audio.ReverbConfig = com.bicy.whitenoise.audio.ReverbConfig(),
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
    
    private val storageListener: () -> Unit = {
        viewModelScope.launch {
            syncPlayListState()
        }
    }
    
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
        
        WhiteNoiseStorage.addListener(storageListener)
    }
    
    private fun syncPlayListState() {
        val servicePlayingIds = MusicService.getInstance()?.getPlayingSounds() ?: emptySet()
        val playListSounds = WhiteNoiseStorage.getPlaybackState().sounds
        
        // 服务有播放状态时优先使用（最准确）
        // 服务无状态但存储有声音时保留当前状态（音频引擎可能还未启动）
        // 服务和存储都为空时才清空
        if (servicePlayingIds.isNotEmpty()) {
            _playingStates.value = servicePlayingIds
        } else if (playListSounds.isEmpty()) {
            _playingStates.value = emptySet()
        }
        
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
        } else if (Function.isCached(context, sound.id, sound.categoryName, sound.name)) {
            playSound(sound)
        } else {
            downloadAndPlaySound(sound)
        }
    }

    /**
     * Agent 工具专用：等待声音下载完成后再播放。
     * 如果已缓存则立即播放；如果正在下载则等待；否则启动下载并等待。
     */
    suspend fun ensureSoundPlayable(sound: SoundMetadata): Result<String> {
        // 已在播放中 → 直接返回
        val isCurrentlyPlaying = MusicService.getInstance()?.isSoundPlaying(sound.id) ?: false
        if (isCurrentlyPlaying) {
            return Result.success(sound.name)
        }

        // 已缓存 → 立即播放
        if (Function.isCached(context, sound.id, sound.categoryName, sound.name)) {
            playSound(sound)
            return Result.success(sound.name)
        }

        // 正在下载中 → 等待完成
        if (Function.isDownloading(sound.id)) {
            return waitForDownload(sound)
        }

        // 未下载 → 启动下载并等待
        return downloadAndWait(sound)
    }

    private suspend fun waitForDownload(sound: SoundMetadata): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            // 轮询等待下载完成
            val job = viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                val timeoutMs = 60_000L
                while (Function.isDownloading(sound.id)) {
                    if (System.currentTimeMillis() - startTime > timeoutMs) {
                        if (continuation.isActive) {
                            continuation.resumeWith(
                                Result.failure(Exception("下载超时：${sound.name}"))
                            )
                        }
                        return@launch
                    }
                    kotlinx.coroutines.delay(200)
                }
                // 下载完成
                if (continuation.isActive) {
                    if (Function.isCached(context, sound.id, sound.categoryName, sound.name)) {
                        playSound(sound)
                        continuation.resumeWith(Result.success(Result.success(sound.name)))
                    } else {
                        continuation.resumeWith(Result.failure(Exception("下载失败：${sound.name}")))
                    }
                }
            }
            continuation.invokeOnCancellation { job.cancel() }
        }
    }

    private suspend fun downloadAndWait(sound: SoundMetadata): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            var completed = false
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
                        if (completed) return@launch
                        completed = true
                        if (continuation.isActive) {
                            if (success) {
                                playSound(sound)
                                continuation.resumeWith(Result.success(Result.success(sound.name)))
                            } else {
                                continuation.resumeWith(Result.failure(Exception("下载失败：${sound.name}")))
                            }
                        }
                    }
                }
            )
            continuation.invokeOnCancellation {
                if (!completed) {
                    DownloadManager.cancelDownload(sound.id)
                }
            }
        }
    }
    
    private fun playSound(sound: SoundMetadata) {
        val savedConfig = WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == sound.id }
        val reverbConfig = savedConfig?.reverbConfig ?: com.bicy.whitenoise.audio.ReverbConfig()
        val spatialConfig = savedConfig?.spatialAudioConfig ?: com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SpatialAudioConfig()
        val creativeConfig = savedConfig?.creativeEffectConfig ?: com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.CreativeEffectConfig()
        val volume = savedConfig?.volume ?: 1f
        
        val soundConfig = SoundPlayConfig(
            id = sound.id,
            name = sound.name,
            categoryName = sound.categoryName,
            volume = volume,
            reverbConfig = reverbConfig,
            spatialAudioConfig = spatialConfig,
            creativeEffectConfig = creativeConfig,
            translations = sound.translations
        )
        WhiteNoiseStorage.addPlayingSound(soundConfig)
        Function.playSound(context, sound)
        ToastManager.success("已加入播放：${sound.name}")
        
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
        if (sound?.trackType == TrackType.SCATTERED) {
            MusicService.getInstance()?.unregisterScatteredTrack(soundId)
        } else {
            MusicService.getInstance()?.stopSound(soundId)
        }
        WhiteNoiseStorage.removePlayingSound(soundId)
        
        val currentStates = _playingStates.value.toMutableSet()
        currentStates.remove(soundId)
        _playingStates.value = currentStates
        
        updatePlayingSounds()
        ToastManager.info("已从播放中移除")
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
                // 暂停：清除播放状态
                _playingStates.value = emptySet()
                playingSounds.forEach { sound ->
                    if (sound.trackType == "scattered") {
                        com.bicy.whitenoise.audio.ScatteredPlayerManagerPart.ScatteredPlayerManager.pauseTrack(sound.id)
                    } else {
                        service.pauseSound(sound.id)
                    }
                }
            } else {
                // 恢复：逐声播放并记录状态
                val resumingIds = mutableSetOf<String>()
                playingSounds.forEach { sound ->
                    if (sound.trackType == "scattered") {
                        com.bicy.whitenoise.audio.ScatteredPlayerManagerPart.ScatteredPlayerManager.resumeTrack(sound.id)
                        resumingIds.add(sound.id)
                    } else {
                        val cachedFile = com.bicy.whitenoise.utils.DownloadManager.getCachedFile(
                            context, sound.id, sound.categoryName, sound.name)
                        if (cachedFile != null && cachedFile.exists()) {
                            if (service.isSoundPlaying(sound.id)) {
                                service.resumeSound(sound.id)
                            } else {
                                service.playSound(sound.id, cachedFile, sound.name)
                                service.setVolume(sound.id, sound.volume)
                            }
                            resumingIds.add(sound.id)
                        }
                    }
                }
                _playingStates.value = resumingIds
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
    
    fun setReverbConfig(soundId: String, config: com.bicy.whitenoise.audio.ReverbConfig) {
        com.bicy.whitenoise.audio.ReverbManager.setConfig(soundId, config)
        WhiteNoiseStorage.updatePlayingSoundReverb(soundId, config)
        MusicService.getInstance()?.setEffectEnabled(soundId, true)
        MusicService.getInstance()?.setReverbParams(soundId, config.roomSize, config.damping, config.wetLevel)
        //com.bicy.whitenoise.audio.ReverbManager.applyReverbConfig(soundId)
        
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
        WhiteNoiseStorage.removeListener(storageListener)
    }

    fun getCurrentSoundConfigs(): List<SoundPlayConfig> {
        return WhiteNoiseStorage.getPlaybackState().sounds
    }

    fun loadPresetSounds(sounds: List<SoundPlayConfig>, onComplete: () -> Unit) {
        // 停止所有正在播放的音频
        MusicService.getInstance()?.stopAllSounds()
        
        WhiteNoiseStorage.clearPlayback()
        sounds.forEach { sound ->
            WhiteNoiseStorage.addPlayingSound(sound)
        }
        syncPlayListState()
        onComplete()
    }

    fun restorePlaybackAfterLoad() {
        val sounds = WhiteNoiseStorage.getPlaybackState().sounds
        val service = MusicService.getInstance() ?: return
        sounds.forEach { sound ->
            val cachedFile = com.bicy.whitenoise.utils.DownloadManager.getCachedFile(
                context, sound.id, sound.categoryName, sound.name)
            if (cachedFile != null && cachedFile.exists() && !service.isSoundPlaying(sound.id)) {
                service.playSound(sound.id, cachedFile, sound.name)
                service.setVolume(sound.id, sound.volume)
            }
        }
    }
}
