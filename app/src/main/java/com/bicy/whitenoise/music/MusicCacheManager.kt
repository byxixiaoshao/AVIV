package com.bicy.whitenoise.music

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.bicy.whitenoise.audio.OboeAudioEngine
import com.bicy.whitenoise.service.AnomalyType
import com.bicy.whitenoise.service.MemoryLockService
import com.bicy.whitenoise.storage.core.JsonStorageManager
import com.bicy.whitenoise.utils.AppInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.LinkedHashMap
import com.bicy.whitenoise.music.MusicLibraryPart.*

data class MusicCacheEntity(
    val id: String = "library",
    val tracksJson: String = "[]",
    val directoriesJson: String = "[]",
    val lastScanTime: Long = 0
)

object MusicCacheManager {
    
    private const val TAG = "MusicCacheManager"
    private const val MAX_LOADED_TRACKS = 5
    private const val CACHE_FILE = "music_cache.json"
    
    private var contextRef: WeakReference<Context>? = null
    
    private val loadedTracks = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(MAX_LOADED_TRACKS, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
                if (size > MAX_LOADED_TRACKS) {
                    eldest?.key?.let { soundId ->
                        if (soundId != currentPlayingSoundId) {
                            OboeAudioEngine.unloadSound(soundId)
                            Log.d(TAG, "Unloaded cached track: $soundId")
                        }
                    }
                    return true
                }
                return false
            }
        }
    )
    
    private var currentPlayingSoundId: String? = null
    
    fun init(context: Context) {
        contextRef = WeakReference(context.applicationContext)
        JsonStorageManager.init(context.applicationContext)
    }
    
    fun getSoundId(trackId: String): String {
        return "music_$trackId"
    }
    
    fun getFilePath(soundId: String): String? {
        return loadedTracks[soundId]
    }
    
    fun isTrackLoaded(trackId: String): Boolean {
        val soundId = getSoundId(trackId)
        return OboeAudioEngine.isLoaded(soundId)
    }
    
    private fun loadFromUri(track: MusicTrack): Boolean {
        val ctx = contextRef?.get() ?: return false
        val soundId = getSoundId(track.id)
        val contentUri = track.contentUri ?: return false
        
        return try {
            val pfd = ctx.contentResolver.openFileDescriptor(contentUri, "r")
            if (pfd == null) {
                Log.e(TAG, "Failed to open file descriptor for: ${track.title}")
                return false
            }
            
            val length = pfd.statSize
            val fd = pfd.detachFd()
            
            val result = OboeAudioEngine.loadSoundFromFd(soundId, fd, 0, length, track.path)
            
            if (result != 0) {
                Log.e(TAG, "Failed to load track from fd: ${track.title}, result=$result")
                return false
            }
            
            loadedTracks[soundId] = track.path
            Log.d(TAG, "Loaded track from fd: ${track.title}, fd=$fd, length=$length")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load track from uri: ${track.title}", e)
            MemoryLockService.reportAnomaly(AnomalyType.AUDIO_DECODE_ERROR, "\u4eceURI\u52a0\u8f7d\u66f2\u76ee\u5931\u8d25: ${track.title}", e.stackTraceToString())
            false
        }
    }
    
    private fun loadFromPath(track: MusicTrack): Boolean {
        val soundId = getSoundId(track.id)
        val result = OboeAudioEngine.loadSound(soundId, track.path)
        
        if (result != 0) {
            Log.e(TAG, "Failed to load track from path: ${track.title}, result=$result")
            return false
        }
        
        loadedTracks[soundId] = track.path
        Log.d(TAG, "Loaded track from path: ${track.title}")
        return true
    }
    
    fun loadTrack(track: MusicTrack, callback: ((Boolean) -> Unit)? = null) {
        val soundId = getSoundId(track.id)
        
        Log.w(TAG, "loadTrack: ${track.title}, soundId=$soundId, isLoaded=${OboeAudioEngine.isLoaded(soundId)}, isLoading=${OboeAudioEngine.isLoading(soundId)}, isOnline=${track.isOnline}")
        
        if (OboeAudioEngine.isLoaded(soundId)) {
            loadedTracks[soundId] = track.path
            callback?.invoke(true)
            return
        }
        
        if (OboeAudioEngine.isLoading(soundId)) {
            callback?.invoke(false)
            return
        }
        
        // 在线音乐：检查缓存文件是否存在
        if (track.isOnline) {
            val cacheFile = java.io.File(track.path)
            if (cacheFile.exists()) {
                // 缓存文件存在，直接加载
                Log.d(TAG, "Loading online track from cache: ${track.title}")
                val success = loadFromPath(track)
                callback?.invoke(success)
            } else {
                // 缓存文件不存在，需要先下载
                Log.d(TAG, "Cache file not found for online track: ${track.title}, need download")
                downloadAndLoadOnlineTrack(track, callback)
            }
            return
        }
        
        // 本地音乐
        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && track.contentUri != null) {
            loadFromUri(track)
        } else {
            loadFromPath(track)
        }
        
        callback?.invoke(success)
        Log.w(TAG, "Loading track: ${track.title}, success=$success")
    }
    
    private fun downloadAndLoadOnlineTrack(track: MusicTrack, callback: ((Boolean) -> Unit)? = null) {
        val ctx = contextRef?.get() ?: run {
            callback?.invoke(false)
            return
        }
        
        val streamUrl = track.streamUrl ?: run {
            Log.e(TAG, "No streamUrl for online track: ${track.title}")
            callback?.invoke(false)
            return
        }
        
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Downloading online track: ${track.title} from $streamUrl")
                
                // 下载文件
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder().url(streamUrl).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "Download failed: ${response.code}")
                    withContext(Dispatchers.Main) { callback?.invoke(false) }
                    return@launch
                }
                
                val cacheFile = java.io.File(track.path)
                cacheFile.parentFile?.mkdirs()
                
                response.body?.byteStream()?.use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                Log.d(TAG, "Download complete: ${cacheFile.absolutePath}")
                
                // 下载完成后加载
                val success = loadFromPath(track)
                withContext(Dispatchers.Main) { callback?.invoke(success) }
                
            } catch (e: Exception) {
                Log.e(TAG, "Download error: ${track.title}", e)
                MemoryLockService.reportAnomaly(AnomalyType.NETWORK_ERROR, "\u5728\u7ebf\u66f2\u76ee\u4e0b\u8f7d\u5931\u8d25: ${track.title}", e.stackTraceToString())
                withContext(Dispatchers.Main) { callback?.invoke(false) }
            }
        }
    }
    
    fun setCurrentPlaying(trackId: String) {
        currentPlayingSoundId = getSoundId(trackId)
    }
    
    fun clearCurrentPlaying() {
        currentPlayingSoundId = null
    }
    
    fun preloadNextTrack(track: MusicTrack) {
        val soundId = getSoundId(track.id)
        
        if (OboeAudioEngine.isLoaded(soundId) || OboeAudioEngine.isLoading(soundId)) {
            return
        }
        
        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && track.contentUri != null) {
            loadFromUri(track)
        } else {
            loadFromPath(track)
        }
        
        Log.d(TAG, "Preloaded next track: ${track.title}, success=$success")
    }
    
    fun unloadTrack(trackId: String) {
        val soundId = getSoundId(trackId)
        if (soundId == currentPlayingSoundId) return
        
        if (OboeAudioEngine.isLoaded(soundId)) {
            OboeAudioEngine.unloadSound(soundId)
            loadedTracks.remove(soundId)
            Log.d(TAG, "Unloaded track: $trackId")
        }
    }
    
    fun clearAll() {
        loadedTracks.keys.toList().forEach { soundId ->
            if (soundId != currentPlayingSoundId) {
                OboeAudioEngine.unloadSound(soundId)
            }
        }
        loadedTracks.clear()
        currentPlayingSoundId = null
    }
    
    suspend fun saveLibraryCache(
        tracks: List<MusicTrack>, 
        directories: List<com.bicy.whitenoise.storage.music.MusicDirectory>,
        lastScanTime: Long = System.currentTimeMillis()
    ) {
        withContext(Dispatchers.IO) {
            try {
                val tracksJson = JSONArray().apply {
                    tracks.forEach { track ->
                        put(JSONObject().apply {
                            put("id", track.id)
                            put("path", track.path)
                            put("title", track.title)
                            put("artist", track.artist ?: "")
                            put("album", track.album ?: "")
                            put("duration", track.duration)
                            track.uriString?.let { put("uriString", it) }
                            put("dateAdded", track.dateAdded)
                        })
                    }
                }.toString()
                
                val dirsJson = JSONArray().apply {
                    directories.forEach { dir ->
                        put(JSONObject().apply {
                            put("path", dir.path)
                            put("uri", dir.uriString ?: "")
                            put("name", dir.name)
                            put("isEnabled", dir.isEnabled)
                        })
                    }
                }.toString()
                
                val entity = MusicCacheEntity(
                    id = "library",
                    tracksJson = tracksJson,
                    directoriesJson = dirsJson,
                    lastScanTime = lastScanTime
                )
                JsonStorageManager.write(CACHE_FILE, entity)
                
                Log.d(TAG, "Saved ${tracks.size} tracks and ${directories.size} directories to cache")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save library cache", e)
                MemoryLockService.reportAnomaly(AnomalyType.IO_ERROR, "\u4fdd\u5b58\u66f2\u5e93\u7f13\u5b58\u5931\u8d25", e.stackTraceToString())
            }
        }
    }
    
    data class LibraryCache(
        val tracks: List<MusicTrack>,
        val directories: List<com.bicy.whitenoise.storage.music.MusicDirectory>,
        val lastScanTime: Long = 0L
    )
    
    suspend fun loadLibraryCache(): LibraryCache? {
        return withContext(Dispatchers.IO) {
            try {
                val entity = JsonStorageManager.read(CACHE_FILE, MusicCacheEntity::class.java) ?: return@withContext null
                
                val dirList = mutableListOf<com.bicy.whitenoise.storage.music.MusicDirectory>()
                val dirArray = JSONArray(entity.directoriesJson)
                for (i in 0 until dirArray.length()) {
                    val json = dirArray.getJSONObject(i)
                    dirList.add(
                        com.bicy.whitenoise.storage.music.MusicDirectory(
                            path = json.getString("path"),
                            uriString = json.optString("uri").takeIf { it.isNotEmpty() },
                            name = json.getString("name"),
                            isEnabled = json.optBoolean("isEnabled", true)
                        )
                    )
                }
                
                val tracks = mutableListOf<MusicTrack>()
                val tracksArray = JSONArray(entity.tracksJson)
                for (i in 0 until tracksArray.length()) {
                    val json = tracksArray.getJSONObject(i)
                    val track = MusicTrack(
                        id = json.getString("id"),
                        path = json.getString("path"),
                        title = json.getString("title"),
                        artist = json.optString("artist").takeIf { it.isNotEmpty() },
                        album = json.optString("album").takeIf { it.isNotEmpty() },
                        duration = json.optLong("duration", 0),
                        albumArt = null,
                        uriString = json.optString("uriString").takeIf { it.isNotEmpty() },
                        dateAdded = json.optLong("dateAdded", System.currentTimeMillis())
                    )
                    tracks.add(track)
                }
                
                Log.d(TAG, "Loaded ${tracks.size} tracks and ${dirList.size} directories from cache")
                LibraryCache(tracks, dirList, entity.lastScanTime)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load library cache", e)
                MemoryLockService.reportAnomaly(AnomalyType.IO_ERROR, "\u52a0\u8f7d\u66f2\u5e93\u7f13\u5b58\u5931\u8d25", e.stackTraceToString())
                null
            }
        }
    }
    
    fun directoriesMatch(
        cached: List<com.bicy.whitenoise.storage.music.MusicDirectory>,
        current: List<com.bicy.whitenoise.storage.music.MusicDirectory>
    ): Boolean {
        if (cached.size != current.size) return false
        
        val cachedSet = cached.filter { it.isEnabled }.map { it.path to it.uriString }.toSet()
        val currentSet = current.filter { it.isEnabled }.map { it.path to it.uriString }.toSet()
        
        return cachedSet == currentSet
    }
    
    fun clearLibraryCache() {
        try {
            runBlocking {
                JsonStorageManager.delete(CACHE_FILE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear library cache", e)
            MemoryLockService.reportAnomaly(AnomalyType.IO_ERROR, "\u6e05\u9664\u66f2\u5e93\u7f13\u5b58\u5931\u8d25", e.stackTraceToString())
        }
    }
}
