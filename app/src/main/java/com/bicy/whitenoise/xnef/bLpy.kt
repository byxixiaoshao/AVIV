package com.bicy.whitenoise.xnef

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.bicy.whitenoise.H3HO.OboeAudioEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.LinkedHashMap

object MusicCacheManager {
    
    private const val TAG = "MusicCacheManager"
    private const val CACHE_FILE_NAME = "music_library_cache.json"
    private const val MAX_LOADED_TRACKS = 5
    
    private var contextRef: WeakReference<Context>? = null
    private var libraryCacheDir: File? = null
    
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
        libraryCacheDir = File(context.filesDir, "music")
        if (!libraryCacheDir!!.exists()) {
            libraryCacheDir!!.mkdirs()
        }
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
        
        Log.w(TAG, "loadTrack: ${track.title}, soundId=$soundId, isLoaded=${OboeAudioEngine.isLoaded(soundId)}, isLoading=${OboeAudioEngine.isLoading(soundId)}")
        
        if (OboeAudioEngine.isLoaded(soundId)) {
            loadedTracks[soundId] = track.path
            callback?.invoke(true)
            return
        }
        
        if (OboeAudioEngine.isLoading(soundId)) {
            callback?.invoke(false)
            return
        }
        
        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && track.contentUri != null) {
            loadFromUri(track)
        } else {
            loadFromPath(track)
        }
        
        callback?.invoke(success)
        Log.w(TAG, "Loading track: ${track.title}, success=$success")
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
        directories: List<com.bicy.whitenoise.JwJY.EY9i.MusicDirectory>,
        lastScanTime: Long = System.currentTimeMillis()
    ) {
        val ctx = contextRef?.get() ?: return
        
        withContext(Dispatchers.IO) {
            try {
                val rootJson = JSONObject()
                rootJson.put("lastScanTime", lastScanTime)
                
                val dirArray = JSONArray()
                directories.forEach { dir ->
                    dirArray.put(JSONObject().apply {
                        put("path", dir.path)
                        put("uri", dir.uriString ?: "")
                        put("name", dir.name)
                        put("isEnabled", dir.isEnabled)
                    })
                }
                rootJson.put("directories", dirArray)
                
                val tracksArray = JSONArray()
                tracks.forEach { track ->
                    tracksArray.put(JSONObject().apply {
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
                rootJson.put("tracks", tracksArray)
                
                val dir = libraryCacheDir ?: return@withContext
                val cacheFile = File(dir, CACHE_FILE_NAME)
                cacheFile.writeText(rootJson.toString())
                
                Log.d(TAG, "Saved ${tracks.size} tracks and ${directories.size} directories to cache")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save library cache", e)
            }
        }
    }
    
    data class LibraryCache(
        val tracks: List<MusicTrack>,
        val directories: List<com.bicy.whitenoise.JwJY.EY9i.MusicDirectory>,
        val lastScanTime: Long = 0L
    )
    
    suspend fun loadLibraryCache(): LibraryCache? {
        val ctx = contextRef?.get() ?: return null
        
        return withContext(Dispatchers.IO) {
            try {
                val dir = libraryCacheDir ?: return@withContext null
                val cacheFile = File(dir, CACHE_FILE_NAME)
                if (!cacheFile.exists()) return@withContext null
                
                val jsonString = cacheFile.readText()
                val rootJson = JSONObject(jsonString)
                
                val dirList = mutableListOf<com.bicy.whitenoise.JwJY.EY9i.MusicDirectory>()
                val dirArray = rootJson.optJSONArray("directories")
                if (dirArray != null) {
                    for (i in 0 until dirArray.length()) {
                        val json = dirArray.getJSONObject(i)
                        dirList.add(
                            com.bicy.whitenoise.JwJY.EY9i.MusicDirectory(
                                path = json.getString("path"),
                                uriString = json.optString("uri").takeIf { it.isNotEmpty() },
                                name = json.getString("name"),
                                isEnabled = json.optBoolean("isEnabled", true)
                            )
                        )
                    }
                }
                
                val tracks = mutableListOf<MusicTrack>()
                val tracksArray = rootJson.optJSONArray("tracks")
                if (tracksArray != null) {
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
                }
                
                Log.d(TAG, "Loaded ${tracks.size} tracks and ${dirList.size} directories from cache")
                val lastScanTime = rootJson.optLong("lastScanTime", 0L)
                LibraryCache(tracks, dirList, lastScanTime)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load library cache", e)
                null
            }
        }
    }
    
    fun directoriesMatch(
        cached: List<com.bicy.whitenoise.JwJY.EY9i.MusicDirectory>,
        current: List<com.bicy.whitenoise.JwJY.EY9i.MusicDirectory>
    ): Boolean {
        if (cached.size != current.size) return false
        
        val cachedSet = cached.filter { it.isEnabled }.map { it.path to it.uriString }.toSet()
        val currentSet = current.filter { it.isEnabled }.map { it.path to it.uriString }.toSet()
        
        return cachedSet == currentSet
    }
    
    fun clearLibraryCache() {
        val dir = libraryCacheDir ?: return
        val cacheFile = File(dir, CACHE_FILE_NAME)
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }
}
