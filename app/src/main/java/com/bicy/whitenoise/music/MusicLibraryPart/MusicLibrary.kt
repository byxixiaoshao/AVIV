package com.bicy.whitenoise.music.MusicLibraryPart

import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.bicy.whitenoise.storage.music.MusicStorage
import com.bicy.whitenoise.utils.AudioMetadata
import com.bicy.whitenoise.utils.AudioMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import com.bicy.whitenoise.music.MusicCacheManager
import com.bicy.whitenoise.music.MusicScannerPart.*

object MusicLibrary {
    
    private const val TAG = "MusicLibrary"
    private val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "wma")
    
    private val _tracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val tracks: StateFlow<List<MusicTrack>> = _tracks.asStateFlow()
    
    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private var cachedTracks: MutableMap<String, MusicTrack> = mutableMapOf()
    private var isInitialized = false
    
    fun init(context: Context) { MusicCacheManager.init(context) }
    
    suspend fun loadFromCacheOnly(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cache = MusicCacheManager.loadLibraryCache()
            if (cache != null && cache.tracks.isNotEmpty()) { _tracks.value = cache.tracks.sortedBy { it.title.lowercase() }; isInitialized = true; Log.d(TAG, "Loaded ${cache.tracks.size} tracks from cache"); true }
            else { Log.d(TAG, "No cache found"); false }
        } catch (e: Exception) { Log.e(TAG, "Failed to load cache", e); false }
    }
    
    suspend fun performIncrementalScan() = withContext(Dispatchers.IO) {
        if (_isScanning.value) { Log.d(TAG, "Scan already in progress, skipping"); return@withContext }
        _isScanning.value = true
        try {
            val currentDirectories = MusicStorage.getEnabledDirectories()
            if (currentDirectories.isEmpty()) { Log.w(TAG, "No directories to scan"); _isScanning.value = false; return@withContext }
            val hasValidPermissions = currentDirectories.all { dir -> val dirUri = dir.uri; dirUri != null && MusicScanner.hasPersistedUriPermission(dirUri) }
            if (!hasValidPermissions) { Log.w(TAG, "Missing permissions"); _isScanning.value = false; return@withContext }
            val cache = MusicCacheManager.loadLibraryCache()
            val lastScanTime = cache?.lastScanTime ?: 0L
            val cachedTracksMap = if (cache != null && cache.tracks.isNotEmpty()) cache.tracks.associateBy { it.uriString ?: it.path } else emptyMap()
            val basicFiles = MusicScanner.scanAudioFilesBasic()
            if (basicFiles.isNotEmpty()) { Log.d(TAG, "File times - max: ${basicFiles.maxOf{it.lastModified}}, min: ${basicFiles.minOf{it.lastModified}}, lastScanTime: $lastScanTime") }
            val resultTracks = mutableListOf<MusicTrack>()
            val scannedUris = mutableSetOf<String>()
            var newCount = 0; var updatedCount = 0; var unchangedCount = 0; var metadataReadCount = 0
            basicFiles.forEach { audioFile ->
                val uriString = audioFile.uri.toString()
                if (scannedUris.contains(uriString)) return@forEach; scannedUris.add(uriString)
                val cachedTrack = cachedTracksMap[uriString]
                val needsMetadataRead = cachedTrack == null || audioFile.lastModified > lastScanTime
                if (needsMetadataRead) {
                    metadataReadCount++; val metadata = MusicScanner.readAudioMetadata(audioFile.uri)
                    val title = metadata?.title?.takeIf { it.isNotBlank() } ?: audioFile.name; val artist = metadata?.artist?.takeIf { it.isNotBlank() }; val album = metadata?.album?.takeIf { it.isNotBlank() }; val duration = metadata?.duration ?: 0L
                    if (cachedTrack != null) {
                        val metadataChanged = cachedTrack.title != title || cachedTrack.artist != artist || cachedTrack.album != album || (duration > 0 && cachedTrack.duration != duration)
                        if (metadataChanged) { resultTracks.add(cachedTrack.copy(title = title, artist = artist, album = album, duration = if (duration > 0) duration else cachedTrack.duration)); updatedCount++ }
                        else { resultTracks.add(cachedTrack); unchangedCount++ }
                    } else { resultTracks.add(MusicTrack(id = uriString.hashCode().toString(), path = audioFile.path, title = title, artist = artist, album = album, duration = duration, albumArt = null, mediaStoreId = -1, uriString = uriString, dateAdded = audioFile.lastModified)); newCount++ }
                } else { resultTracks.add(cachedTrack); unchangedCount++ }
            }
            val removedCount = cachedTracksMap.size - unchangedCount - updatedCount
            if (newCount > 0 || updatedCount > 0 || removedCount > 0) {
                val maxLastModified = basicFiles.maxOfOrNull { it.lastModified } ?: System.currentTimeMillis()
                _tracks.value = resultTracks.sortedBy { it.title.lowercase() }; isInitialized = true
                MusicCacheManager.saveLibraryCache(resultTracks, currentDirectories, maxLastModified)
            }
        } catch (e: Exception) { Log.e(TAG, "Error during incremental scan", e) }
        finally { _isScanning.value = false }
    }
    
    suspend fun scanLibrary(context: Context, forceRescan: Boolean = false) = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext
        _isScanning.value = true; _scanProgress.value = ScanProgress(isScanning = true)
        try {
            val currentDirectories = MusicStorage.getEnabledDirectories()
            if (currentDirectories.isEmpty()) { _tracks.value = emptyList(); _scanProgress.value = ScanProgress(isScanning = false); MusicCacheManager.clearLibraryCache(); return@withContext }
            val documentFiles = MusicScanner.scanAudioFiles()
            val resultTracks = mutableListOf<MusicTrack>(); val scannedKeys = mutableSetOf<String>()
            documentFiles.forEach { audioFile ->
                val key = audioFile.uri.toString(); if (scannedKeys.contains(key)) return@forEach; scannedKeys.add(key)
                resultTracks.add(MusicTrack(id = key.hashCode().toString(), path = audioFile.path,
                    title = audioFile.title?.takeIf{it.isNotBlank()} ?: audioFile.name,
                    artist = audioFile.artist?.takeIf{it.isNotBlank()}, album = audioFile.album?.takeIf{it.isNotBlank()},
                    duration = audioFile.duration, albumArt = null, mediaStoreId = -1, uriString = key, dateAdded = audioFile.lastModified))
            }
            _tracks.value = resultTracks.sortedBy { it.title.lowercase() }
            _scanProgress.value = ScanProgress(isScanning = false, filesFound = resultTracks.size, totalFiles = resultTracks.size)
            isInitialized = true
            val maxLastModified = documentFiles.maxOfOrNull { it.lastModified } ?: System.currentTimeMillis()
            MusicCacheManager.saveLibraryCache(resultTracks, currentDirectories, maxLastModified)
        } catch (e: Exception) { Log.e(TAG, "Error scanning library", e); _scanProgress.value = ScanProgress(isScanning = false) }
        finally { _isScanning.value = false }
    }
    
    fun getTrackById(id: String) = _tracks.value.find { it.id == id }
    
    fun searchTracks(query: String): List<MusicTrack> { if (query.isBlank()) return _tracks.value; val lq = query.lowercase(); return _tracks.value.filter { it.title.lowercase().contains(lq) || it.artist?.lowercase()?.contains(lq) == true || it.album?.lowercase()?.contains(lq) == true } }
    
    fun getTracksByArtist(artist: String) = _tracks.value.filter { it.artist == artist }
    fun getTracksByAlbum(album: String) = _tracks.value.filter { it.album == album }
    fun getAllArtists() = _tracks.value.mapNotNull { it.artist }.distinct().sorted()
    fun getAllAlbums() = _tracks.value.mapNotNull { it.album }.distinct().sorted()
    fun clearLibrary() { _tracks.value = emptyList(); cachedTracks.clear(); MusicCacheManager.clearLibraryCache(); isInitialized = false }
    fun hasTracks() = _tracks.value.isNotEmpty()
    fun addOrUpdateTrack(track: MusicTrack) { val current = _tracks.value.toMutableList(); val index = current.indexOfFirst { it.id == track.id }; if (index >= 0) current[index] = track else current.add(track); _tracks.value = current.sortedBy { it.title.lowercase() } }
}

