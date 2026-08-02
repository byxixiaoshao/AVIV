package com.bicy.whitenoise.onlinemusic

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.music.MusicLibraryPart.MusicTrack
import com.bicy.whitenoise.storage.core.JsonStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 在线音乐存储管理
 * 负责将在线音乐元数据持久化到数据库
 */
data class MusicTrackEntity(
    val id: String,
    val path: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Long,
    val mediaStoreId: Long = -1,
    val uriString: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val streamUrl: String? = null,
    val source: String? = null
)

object OnlineMusicStorage {
    
    private const val TAG = "OnlineMusicStorage"
    
    private lateinit var appContext: Context
    
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    /**
     * 保存在线音乐到数据库
     */
    suspend fun saveOnlineTrack(track: MusicTrack) {
        if (!track.isOnline) return
        
        withContext(Dispatchers.IO) {
            try {
                val entity = MusicTrackEntity(
                    id = track.id,
                    path = track.path,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    duration = track.duration,
                    mediaStoreId = track.mediaStoreId,
                    uriString = track.uriString,
                    dateAdded = track.dateAdded,
                    isOnline = true,
                    streamUrl = track.streamUrl,
                    source = track.source
                )
                
                val existing = JsonStorageManager.read("online_music_tracks.json", Array<MusicTrackEntity>::class.java)?.toMutableList() ?: mutableListOf()
                existing.removeAll { it.id == entity.id }
                existing.add(entity)
                JsonStorageManager.write("online_music_tracks.json", existing.toTypedArray())
                
                Log.d(TAG, "Saved online track: ${track.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save online track: ${track.title}", e)
            }
        }
    }
    
    /**
     * 从数据库加载在线音乐
     */
    suspend fun loadOnlineTracks(): List<MusicTrack> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = JsonStorageManager.read("online_music_tracks.json", Array<MusicTrackEntity>::class.java)?.toList() ?: emptyList()
                
                entities.map { entity ->
                    MusicTrack(
                        id = entity.id,
                        path = entity.path,
                        title = entity.title,
                        artist = entity.artist,
                        album = entity.album,
                        duration = entity.duration,
                        mediaStoreId = entity.mediaStoreId,
                        uriString = entity.uriString,
                        dateAdded = entity.dateAdded,
                        isOnline = entity.isOnline,
                        streamUrl = entity.streamUrl,
                        source = entity.source
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load online tracks", e)
                emptyList()
            }
        }
    }
    
    /**
     * 根据 ID 获取在线音乐
     */
    suspend fun getTrackById(id: String): MusicTrack? {
        return withContext(Dispatchers.IO) {
            try {
                val entities = JsonStorageManager.read("online_music_tracks.json", Array<MusicTrackEntity>::class.java)?.toList() ?: emptyList()
                val entity = entities.firstOrNull { it.id == id } ?: return@withContext null
                
                if (!entity.isOnline) return@withContext null
                
                MusicTrack(
                    id = entity.id,
                    path = entity.path,
                    title = entity.title,
                    artist = entity.artist,
                    album = entity.album,
                    duration = entity.duration,
                    mediaStoreId = entity.mediaStoreId,
                    uriString = entity.uriString,
                    dateAdded = entity.dateAdded,
                    isOnline = true,
                    streamUrl = entity.streamUrl,
                    source = entity.source
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get track by id: $id", e)
                null
            }
        }
    }
    
    /**
     * 删除在线音乐
     */
    suspend fun deleteOnlineTrack(id: String) {
        withContext(Dispatchers.IO) {
            try {
                val entities = JsonStorageManager.read("online_music_tracks.json", Array<MusicTrackEntity>::class.java)?.toList() ?: emptyList()
                val filtered = entities.filter { it.id != id }
                JsonStorageManager.write("online_music_tracks.json", filtered.toTypedArray())
                Log.d(TAG, "Deleted online track: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete online track: $id", e)
            }
        }
    }
}