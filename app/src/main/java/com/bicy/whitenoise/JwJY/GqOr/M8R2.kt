package com.bicy.whitenoise.JwJY.GqOr

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.xnef.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class UserPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("trackIds", JSONArray(trackIds))
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
        }
    }
    
    companion object {
        fun fromJson(json: JSONObject): UserPlaylist {
            val trackIds = mutableListOf<String>()
            val trackIdsArray = json.optJSONArray("trackIds")
            if (trackIdsArray != null) {
                for (i in 0 until trackIdsArray.length()) {
                    trackIds.add(trackIdsArray.getString(i))
                }
            }
            return UserPlaylist(
                id = json.getString("id"),
                name = json.getString("name"),
                trackIds = trackIds,
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }
}

object PlaylistManager {
    
    private const val TAG = "PlaylistManager"
    private const val PLAYLIST_FOLDER = "playlists"
    private const val FAVORITES_FILE = "favorites.json"
    
    private lateinit var appContext: Context
    private var playlistDir: File? = null
    
    private val _userPlaylists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val userPlaylists: StateFlow<List<UserPlaylist>> = _userPlaylists.asStateFlow()
    
    private val _favorites = MutableStateFlow<UserPlaylist?>(null)
    val favorites: StateFlow<UserPlaylist?> = _favorites.asStateFlow()
    
    fun init(context: Context) {
        appContext = context.applicationContext
        initPlaylistDir()
        loadFavorites()
        loadUserPlaylists()
    }
    
    private fun initPlaylistDir() {
        val musicDirs = File(appContext.filesDir, "music")
        if (!musicDirs.exists()) {
            musicDirs.mkdirs()
        }
        playlistDir = File(musicDirs, PLAYLIST_FOLDER)
        if (playlistDir?.exists() == false) {
            playlistDir?.mkdirs()
        }
        Log.d(TAG, "Playlist directory: ${playlistDir?.absolutePath}")
    }
    
    private fun loadFavorites() {
        val file = File(appContext.filesDir, FAVORITES_FILE)
        if (file.exists()) {
            try {
                val json = JSONObject(file.readText())
                _favorites.value = UserPlaylist.fromJson(json)
                Log.d(TAG, "Loaded favorites: ${_favorites.value?.trackIds?.size} tracks")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load favorites", e)
                createDefaultFavorites()
            }
        } else {
            createDefaultFavorites()
        }
    }
    
    private fun createDefaultFavorites() {
        val defaultFavorites = UserPlaylist(
            id = "favorites",
            name = "收藏",
            trackIds = emptyList()
        )
        _favorites.value = defaultFavorites
        saveFavorites()
    }
    
    private fun saveFavorites() {
        val file = File(appContext.filesDir, FAVORITES_FILE)
        try {
            file.writeText(_favorites.value?.toJson().toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save favorites", e)
        }
    }
    
    private fun loadUserPlaylists() {
        val dir = playlistDir ?: return
        val playlists = mutableListOf<UserPlaylist>()
        
        dir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            try {
                val json = JSONObject(file.readText())
                playlists.add(UserPlaylist.fromJson(json))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load playlist: ${file.name}", e)
            }
        }
        
        _userPlaylists.value = playlists.sortedByDescending { it.updatedAt }
        Log.d(TAG, "Loaded ${playlists.size} user playlists")
    }
    
    fun isFavorite(trackId: String): Boolean {
        return _favorites.value?.trackIds?.contains(trackId) == true
    }
    
    fun toggleFavorite(trackId: String) {
        val current = _favorites.value ?: return
        val newTrackIds = if (current.trackIds.contains(trackId)) {
            current.trackIds - trackId
        } else {
            current.trackIds + trackId
        }
        _favorites.value = current.copy(
            trackIds = newTrackIds,
            updatedAt = System.currentTimeMillis()
        )
        saveFavorites()
    }
    
    fun addToFavorites(trackIds: List<String>) {
        val current = _favorites.value ?: return
        val newTrackIds = (current.trackIds + trackIds).distinct()
        _favorites.value = current.copy(
            trackIds = newTrackIds,
            updatedAt = System.currentTimeMillis()
        )
        saveFavorites()
    }
    
    fun removeFromFavorites(trackIds: List<String>) {
        val current = _favorites.value ?: return
        val newTrackIds = current.trackIds - trackIds.toSet()
        _favorites.value = current.copy(
            trackIds = newTrackIds,
            updatedAt = System.currentTimeMillis()
        )
        saveFavorites()
    }
    
    fun createPlaylist(name: String): UserPlaylist {
        val id = "playlist_${System.currentTimeMillis()}"
        val playlist = UserPlaylist(
            id = id,
            name = name,
            trackIds = emptyList()
        )
        savePlaylist(playlist)
        _userPlaylists.value = (_userPlaylists.value + playlist).sortedByDescending { it.updatedAt }
        return playlist
    }
    
    fun deletePlaylist(playlistId: String) {
        val dir = playlistDir ?: return
        val file = File(dir, "$playlistId.json")
        if (file.exists()) {
            file.delete()
        }
        _userPlaylists.value = _userPlaylists.value.filter { it.id != playlistId }
    }
    
    fun renamePlaylist(playlistId: String, newName: String) {
        val playlist = _userPlaylists.value.find { it.id == playlistId } ?: return
        val updated = playlist.copy(
            name = newName,
            updatedAt = System.currentTimeMillis()
        )
        savePlaylist(updated)
        _userPlaylists.value = _userPlaylists.value.map {
            if (it.id == playlistId) updated else it
        }
    }
    
    fun addToPlaylist(playlistId: String, trackIds: List<String>) {
        val playlist = _userPlaylists.value.find { it.id == playlistId } ?: return
        val newTrackIds = (playlist.trackIds + trackIds).distinct()
        val updated = playlist.copy(
            trackIds = newTrackIds,
            updatedAt = System.currentTimeMillis()
        )
        savePlaylist(updated)
        _userPlaylists.value = _userPlaylists.value.map {
            if (it.id == playlistId) updated else it
        }
    }
    
    fun removeFromPlaylist(playlistId: String, trackIds: List<String>) {
        val playlist = _userPlaylists.value.find { it.id == playlistId } ?: return
        val newTrackIds = playlist.trackIds - trackIds.toSet()
        val updated = playlist.copy(
            trackIds = newTrackIds,
            updatedAt = System.currentTimeMillis()
        )
        savePlaylist(updated)
        _userPlaylists.value = _userPlaylists.value.map {
            if (it.id == playlistId) updated else it
        }
    }
    
    private fun savePlaylist(playlist: UserPlaylist) {
        val dir = playlistDir ?: return
        val file = File(dir, "${playlist.id}.json")
        try {
            file.writeText(playlist.toJson().toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save playlist: ${playlist.id}", e)
        }
    }
    
    fun getPlaylistById(id: String): UserPlaylist? {
        if (id == "favorites") return _favorites.value
        return _userPlaylists.value.find { it.id == id }
    }
    
    fun getTracksForPlaylist(playlist: UserPlaylist, allTracks: List<MusicTrack>): List<MusicTrack> {
        return playlist.trackIds.mapNotNull { id ->
            allTracks.find { it.id == id }
        }
    }
}
