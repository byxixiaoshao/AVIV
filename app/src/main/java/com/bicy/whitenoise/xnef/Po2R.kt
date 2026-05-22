package com.bicy.whitenoise.xnef

import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.bicy.whitenoise.JwJY.EY9i.MusicStorage
import com.bicy.whitenoise.y10p.AudioMetadata
import com.bicy.whitenoise.y10p.AudioMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class MusicTrack(
    val id: String,
    val path: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Long,
    val albumArt: ByteArray? = null,
    val mediaStoreId: Long = -1,
    val uriString: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
) {
    val contentUri: Uri?
        get() = uriString?.let { Uri.parse(it) }
            ?: if (mediaStoreId > 0) {
                Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId.toString())
            } else null
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MusicTrack
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class ScanProgress(
    val isScanning: Boolean = false,
    val currentPath: String = "",
    val filesFound: Int = 0,
    val totalFiles: Int = 0
)

object MusicLibrary {
    
    private const val TAG = "MusicLibrary"
    
    private val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "wma")
    private val SUPPORTED_MIME_TYPES = setOf(
        "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav",
        "audio/flac", "audio/x-flac", "audio/aac", "audio/mp4",
        "audio/x-m4a", "audio/ogg", "audio/x-ogg", "audio/wma",
        "audio/x-ms-wma"
    )
    
    private val _tracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val tracks: StateFlow<List<MusicTrack>> = _tracks.asStateFlow()
    
    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private var cachedTracks: MutableMap<String, MusicTrack> = mutableMapOf()
    private var isInitialized = false
    
    fun init(context: Context) {
        MusicCacheManager.init(context)
    }
    
    suspend fun loadFromCacheOnly(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cache = MusicCacheManager.loadLibraryCache()
                if (cache != null && cache.tracks.isNotEmpty()) {
                    _tracks.value = cache.tracks.sortedBy { it.title.lowercase() }
                    isInitialized = true
                    Log.d(TAG, "Loaded ${cache.tracks.size} tracks from cache")
                    true
                } else {
                    Log.d(TAG, "No cache found")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load cache", e)
                false
            }
        }
    }
    
    suspend fun performIncrementalScan() {
        if (_isScanning.value) {
            Log.d(TAG, "Scan already in progress, skipping")
            return
        }
        
        _isScanning.value = true
        
        withContext(Dispatchers.IO) {
            try {
                val currentDirectories = MusicStorage.getEnabledDirectories()
                
                if (currentDirectories.isEmpty()) {
                    Log.w(TAG, "No directories to scan")
                    _isScanning.value = false
                    return@withContext
                }
                
                val hasValidPermissions = currentDirectories.all { dir ->
                    val dirUri = dir.uri
                    dirUri != null && MusicScanner.hasPersistedUriPermission(dirUri)
                }
                
                if (!hasValidPermissions) {
                    Log.w(TAG, "Missing permissions, skipping incremental scan")
                    _isScanning.value = false
                    return@withContext
                }
                
                val cache = MusicCacheManager.loadLibraryCache()
                val lastScanTime = cache?.lastScanTime ?: 0L
                val cachedTracksMap = if (cache != null && cache.tracks.isNotEmpty()) {
                    cache.tracks.associateBy { it.uriString ?: it.path }
                } else {
                    emptyMap()
                }
                
                Log.d(TAG, "Starting incremental scan, cached: ${cachedTracksMap.size}, lastScanTime: $lastScanTime")
                
                val basicFiles = MusicScanner.scanAudioFilesBasic()
                
                if (basicFiles.isNotEmpty()) {
                    val maxFileTime = basicFiles.maxOf { it.lastModified }
                    val minFileTime = basicFiles.minOf { it.lastModified }
                    Log.d(TAG, "File times - max: $maxFileTime, min: $minFileTime, lastScanTime: $lastScanTime")
                }
                
                val resultTracks = mutableListOf<MusicTrack>()
                val scannedUris = mutableSetOf<String>()
                var newCount = 0
                var updatedCount = 0
                var unchangedCount = 0
                var metadataReadCount = 0
                val metadataStartTime = System.currentTimeMillis()
                
                basicFiles.forEach { audioFile ->
                    val uriString = audioFile.uri.toString()
                    if (scannedUris.contains(uriString)) return@forEach
                    scannedUris.add(uriString)
                    
                    val cachedTrack = cachedTracksMap[uriString]
                    
                    val needsMetadataRead = cachedTrack == null || audioFile.lastModified > lastScanTime
                    
                    if (needsMetadataRead) {
                        metadataReadCount++
                        val metadata = MusicScanner.readAudioMetadata(audioFile.uri)
                        
                        val title = metadata?.title?.takeIf { it.isNotBlank() } ?: audioFile.name
                        val artist = metadata?.artist?.takeIf { it.isNotBlank() }
                        val album = metadata?.album?.takeIf { it.isNotBlank() }
                        val duration = metadata?.duration ?: 0L
                        
                        if (cachedTrack != null) {
                            val metadataChanged = cachedTrack.title != title ||
                                cachedTrack.artist != artist ||
                                cachedTrack.album != album ||
                                (duration > 0 && cachedTrack.duration != duration)
                            
                            if (metadataChanged) {
                                resultTracks.add(cachedTrack.copy(
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    duration = if (duration > 0) duration else cachedTrack.duration
                                ))
                                updatedCount++
                            } else {
                                resultTracks.add(cachedTrack)
                                unchangedCount++
                            }
                        } else {
                            resultTracks.add(MusicTrack(
                                id = uriString.hashCode().toString(),
                                path = audioFile.path,
                                title = title,
                                artist = artist,
                                album = album,
                                duration = duration,
                                albumArt = null,
                                mediaStoreId = -1,
                                uriString = uriString,
                                dateAdded = audioFile.lastModified
                            ))
                            newCount++
                        }
                    } else {
                        resultTracks.add(cachedTrack!!)
                        unchangedCount++
                    }
                }
                
                val metadataElapsed = System.currentTimeMillis() - metadataStartTime
                val removedCount = cachedTracksMap.size - unchangedCount - updatedCount
                
                if (newCount > 0 || updatedCount > 0 || removedCount > 0) {
                    Log.d(TAG, "Changes detected - new: $newCount, updated: $updatedCount, removed: $removedCount, metadata reads: $metadataReadCount in ${metadataElapsed}ms")
                    
                    val maxLastModified = basicFiles.maxOfOrNull { it.lastModified } ?: System.currentTimeMillis()
                    
                    _tracks.value = resultTracks.sortedBy { it.title.lowercase() }
                    isInitialized = true
                    MusicCacheManager.saveLibraryCache(resultTracks, currentDirectories, maxLastModified)
                } else {
                    Log.d(TAG, "No changes detected, metadata reads: $metadataReadCount in ${metadataElapsed}ms")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during incremental scan", e)
            } finally {
                _isScanning.value = false
            }
        }
    }
    
    suspend fun scanLibrary(context: Context, forceRescan: Boolean = false) {
        if (_isScanning.value) return
        
        _isScanning.value = true
        _scanProgress.value = ScanProgress(isScanning = true)
        
        withContext(Dispatchers.IO) {
            try {
                val currentDirectories = MusicStorage.getEnabledDirectories()
                
                Log.d(TAG, "Scanning directories: ${currentDirectories.map { it.path }}")
                
                if (currentDirectories.isEmpty()) {
                    Log.w(TAG, "No directories to scan, clearing library")
                    _tracks.value = emptyList()
                    _scanProgress.value = ScanProgress(isScanning = false)
                    MusicCacheManager.clearLibraryCache()
                    return@withContext
                }
                
                val documentFiles = MusicScanner.scanAudioFiles()
                Log.d(TAG, "DocumentFile scan found: ${documentFiles.size} files")
                
                val resultTracks = mutableListOf<MusicTrack>()
                val scannedKeys = mutableSetOf<String>()
                
                documentFiles.forEach { audioFile ->
                    val key = audioFile.uri.toString()
                    if (scannedKeys.contains(key)) return@forEach
                    scannedKeys.add(key)
                    
                    val newTitle = audioFile.title?.takeIf { it.isNotBlank() } ?: audioFile.name
                    val newArtist = audioFile.artist?.takeIf { it.isNotBlank() }
                    val newAlbum = audioFile.album?.takeIf { it.isNotBlank() }
                    
                    resultTracks.add(MusicTrack(
                        id = audioFile.uri.toString().hashCode().toString(),
                        path = audioFile.path,
                        title = newTitle,
                        artist = newArtist,
                        album = newAlbum,
                        duration = audioFile.duration,
                        albumArt = null,
                        mediaStoreId = -1,
                        uriString = audioFile.uri.toString(),
                        dateAdded = audioFile.lastModified
                    ))
                }
                
                _tracks.value = resultTracks.sortedBy { it.title.lowercase() }
                _scanProgress.value = ScanProgress(
                    isScanning = false,
                    filesFound = resultTracks.size,
                    totalFiles = resultTracks.size
                )
                
                isInitialized = true
                
                val maxLastModified = documentFiles.maxOfOrNull { it.lastModified } ?: System.currentTimeMillis()
                MusicCacheManager.saveLibraryCache(resultTracks, currentDirectories, maxLastModified)
                
                Log.d(TAG, "Scan complete: ${resultTracks.size} tracks found")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning library", e)
                _scanProgress.value = ScanProgress(isScanning = false)
            } finally {
                _isScanning.value = false
            }
        }
    }
    
    private fun scanWithMediaStore(context: Context, directories: List<File>): List<MusicTrack> {
        val tracks = mutableListOf<MusicTrack>()
        
        Log.d(TAG, "scanWithMediaStore: directories = ${directories.map { it.absolutePath }}")
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE
        )
        
        val cursor: Cursor? = context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )
        
        var totalCount = 0
        var matchedCount = 0
        var notMatchedCount = 0
        
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            
            while (it.moveToNext()) {
                val path = it.getString(dataColumn)
                val duration = it.getLong(durationColumn)
                totalCount++
                
                val matchedDir = directories.find { dir ->
                    path.startsWith(dir.absolutePath)
                }
                
                if (matchedDir == null) {
                    notMatchedCount++
                    if (notMatchedCount <= 5) {
                        Log.v(TAG, "Path not matched: $path")
                    }
                    continue
                }
                
                matchedCount++
                
                val mediaStoreId = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: "Unknown"
                val artist = it.getString(artistColumn)
                val album = it.getString(albumColumn)
                
                val track = MusicTrack(
                    id = path.hashCode().toString(),
                    path = path,
                    title = title,
                    artist = if (artist == "<unknown>") null else artist,
                    album = if (album == "<unknown>") null else album,
                    duration = duration,
                    albumArt = null,
                    mediaStoreId = mediaStoreId,
                    dateAdded = System.currentTimeMillis()
                )
                
                tracks.add(track)
                cachedTracks[track.id] = track
            }
        }
        
        Log.d(TAG, "MediaStore scan: total=$totalCount, matched=$matchedCount, notMatched=$notMatchedCount")
        
        return tracks
    }
    
    private fun collectAudioFiles(directory: File, fileList: MutableList<File>) {
        try {
            val files = directory.listFiles()
            if (files == null) {
                Log.w(TAG, "Cannot list files in: ${directory.absolutePath} - listFiles() returned null")
                return
            }
            
            if (files.isEmpty()) {
                Log.d(TAG, "Dir: ${directory.name} -> EMPTY (listFiles returned empty array)")
                return
            }
            
            var dirCount = 0
            var fileCount = 0
            var audioCount = 0
            var skippedCount = 0
            
            files.forEach { file ->
                if (file.isDirectory) {
                    dirCount++
                    collectAudioFiles(file, fileList)
                } else if (file.isFile) {
                    fileCount++
                    val extension = file.extension.lowercase()
                    if (extension in SUPPORTED_EXTENSIONS) {
                        audioCount++
                        fileList.add(file)
                    } else {
                        skippedCount++
                        if (skippedCount <= 10) {
                            Log.i(TAG, "SKIPPED: ${file.name} (ext: $extension)")
                        }
                    }
                }
            }
            
            Log.d(TAG, "Dir: ${directory.name} -> $dirCount dirs, $fileCount files ($audioCount audio, $skippedCount skipped)")
            
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException accessing directory: ${directory.absolutePath}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing directory: ${directory.absolutePath}", e)
        }
    }
    
    private fun triggerMediaScan(context: Context, directory: File) {
        if (!directory.exists() || !directory.isDirectory) return
        
        val audioFiles = mutableListOf<File>()
        collectAudioFiles(directory, audioFiles)
        
        if (audioFiles.isEmpty()) return
        
        val paths = audioFiles.map { it.absolutePath }.toTypedArray()
        val mimeTypes = paths.map { "audio/*" }.toTypedArray()
        
        try {
            MediaScannerConnection.scanFile(
                context,
                paths,
                mimeTypes
            ) { path, uri ->
                if (uri != null) {
                    Log.v(TAG, "MediaStore indexed: $path")
                }
            }
            Log.d(TAG, "Triggered media scan for ${paths.size} files in ${directory.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger media scan", e)
        }
    }
    
    private fun scanFileSystem(directories: List<File>, existingIds: Set<String>): List<MusicTrack> {
        val newTracks = mutableListOf<MusicTrack>()
        
        directories.forEach { dir ->
            if (!dir.exists() || !dir.isDirectory) return@forEach
            
            val allFiles = mutableListOf<File>()
            collectAudioFiles(dir, allFiles)
            
            allFiles.forEach { file ->
                val id = file.absolutePath.hashCode().toString()
                if (!existingIds.contains(id)) {
                    val track = processAudioFile(file)
                    if (track != null) {
                        newTracks.add(track)
                        Log.d(TAG, "Found new file not in MediaStore: ${file.name}")
                    }
                }
            }
        }
        
        return newTracks
    }
    
    private fun processAudioFile(file: File): MusicTrack? {
        return try {
            val metadata: AudioMetadata? = AudioMetadataReader.readFromFile(file)
            
            if (metadata == null) {
                Log.w(TAG, "Failed to read metadata for: ${file.name}, using defaults")
            }
            
            val id = file.absolutePath.hashCode().toString()
            val title = metadata?.title ?: file.nameWithoutExtension
            val artist = metadata?.artist
            val album = metadata?.album
            val duration = metadata?.duration ?: 0L
            
            MusicTrack(
                id = id,
                path = file.absolutePath,
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                albumArt = metadata?.albumArt,
                dateAdded = file.lastModified()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing file: ${file.absolutePath}", e)
            null
        }
    }
    
    fun getTrackById(id: String): MusicTrack? {
        return _tracks.value.find { it.id == id }
    }
    
    fun searchTracks(query: String): List<MusicTrack> {
        if (query.isBlank()) return _tracks.value
        
        val lowerQuery = query.lowercase()
        return _tracks.value.filter { track ->
            track.title.lowercase().contains(lowerQuery) ||
            track.artist?.lowercase()?.contains(lowerQuery) == true ||
            track.album?.lowercase()?.contains(lowerQuery) == true
        }
    }
    
    fun getTracksByArtist(artist: String): List<MusicTrack> {
        return _tracks.value.filter { it.artist == artist }
    }
    
    fun getTracksByAlbum(album: String): List<MusicTrack> {
        return _tracks.value.filter { it.album == album }
    }
    
    fun getAllArtists(): List<String> {
        return _tracks.value
            .mapNotNull { it.artist }
            .distinct()
            .sorted()
    }
    
    fun getAllAlbums(): List<String> {
        return _tracks.value
            .mapNotNull { it.album }
            .distinct()
            .sorted()
    }
    
    fun clearLibrary() {
        _tracks.value = emptyList()
        cachedTracks.clear()
        MusicCacheManager.clearLibraryCache()
        isInitialized = false
    }
    
    fun hasTracks(): Boolean {
        return _tracks.value.isNotEmpty()
    }
}
