package com.bicy.whitenoise.storage.playlist.PlaylistManagerPart

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.music.MusicLibraryPart.MusicTrack
import com.bicy.whitenoise.storage.core.JsonStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object PlaylistManager {
    
    private const val TAG = "PlaylistManager"
    private const val FAVORITES_FILE = "favorites.json"
    private const val PLAYLISTS_FILE = "playlists.json"

    private data class FavoriteData(
        val trackIds: List<String>,
        val updatedAt: Long = System.currentTimeMillis()
    )
    
    private lateinit var appContext: Context
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _userPlaylists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val userPlaylists: StateFlow<List<UserPlaylist>> = _userPlaylists.asStateFlow()
    
    private val _favorites = MutableStateFlow<UserPlaylist?>(null)
    val favorites: StateFlow<UserPlaylist?> = _favorites.asStateFlow()
    
    fun init(context: Context) { appContext = context.applicationContext; loadFavorites(); loadUserPlaylists() }
    
    private fun loadFavorites() {
        try {
            val favData = runBlocking { JsonStorageManager.read(FAVORITES_FILE, FavoriteData::class.java) }
            if (favData != null) {
                _favorites.value = UserPlaylist(id = "favorites", name = "收藏", trackIds = favData.trackIds, updatedAt = favData.updatedAt)
                Log.d(TAG, "Loaded favorites: ${_favorites.value!!.trackIds.size} tracks")
            } else createDefaultFavorites()
        } catch (e: Exception) { Log.e(TAG, "Failed to load favorites", e); createDefaultFavorites() }
    }
    
    private fun createDefaultFavorites() { _favorites.value = UserPlaylist(id = "favorites", name = "收藏", trackIds = emptyList()); persistFavorites() }
    
    private fun persistFavorites() {
        scope.launch {
            try {
                val fav = _favorites.value ?: return@launch
                JsonStorageManager.write(FAVORITES_FILE, FavoriteData(trackIds = fav.trackIds, updatedAt = fav.updatedAt))
            } catch (e: Exception) { Log.e(TAG, "Failed to save favorites", e) }
        }
    }
    
    private fun loadUserPlaylists() {
        try {
            val array = runBlocking { JsonStorageManager.read(PLAYLISTS_FILE, Array<UserPlaylist>::class.java) }
            _userPlaylists.value = array?.toList() ?: emptyList()
            Log.d(TAG, "Loaded ${_userPlaylists.value.size} user playlists")
        } catch (e: Exception) { Log.e(TAG, "Failed to load playlists", e) }
    }
    
    private fun persistPlaylists() {
        scope.launch {
            try {
                JsonStorageManager.write(PLAYLISTS_FILE, _userPlaylists.value.toTypedArray())
            } catch (e: Exception) { Log.e(TAG, "Failed to save playlists", e) }
        }
    }
    
    fun isFavorite(trackId: String) = _favorites.value?.trackIds?.contains(trackId) == true
    
    fun toggleFavorite(trackId: String) { val c = _favorites.value ?: return; _favorites.value = c.copy(trackIds = if (c.trackIds.contains(trackId)) c.trackIds - trackId else c.trackIds + trackId, updatedAt = System.currentTimeMillis()); persistFavorites() }
    
    fun addToFavorites(trackIds: List<String>) { val c = _favorites.value ?: return; _favorites.value = c.copy(trackIds = (c.trackIds + trackIds).distinct(), updatedAt = System.currentTimeMillis()); persistFavorites() }
    
    fun removeFromFavorites(trackIds: List<String>) { val c = _favorites.value ?: return; _favorites.value = c.copy(trackIds = c.trackIds - trackIds.toSet(), updatedAt = System.currentTimeMillis()); persistFavorites() }
    
    fun createPlaylist(name: String): UserPlaylist { val p = UserPlaylist(id = "playlist_${System.currentTimeMillis()}", name = name, trackIds = emptyList()); _userPlaylists.value = (_userPlaylists.value + p).sortedByDescending { it.updatedAt }; persistPlaylists(); return p }
    
    fun deletePlaylist(playlistId: String) { _userPlaylists.value = _userPlaylists.value.filter { it.id != playlistId }; persistPlaylists() }
    
    fun renamePlaylist(playlistId: String, newName: String) { val p = _userPlaylists.value.find { it.id == playlistId } ?: return; val u = p.copy(name = newName, updatedAt = System.currentTimeMillis()); _userPlaylists.value = _userPlaylists.value.map { if (it.id == playlistId) u else it }; persistPlaylists() }
    
    fun addToPlaylist(playlistId: String, trackIds: List<String>) { val p = _userPlaylists.value.find { it.id == playlistId } ?: return; val u = p.copy(trackIds = (p.trackIds + trackIds).distinct(), updatedAt = System.currentTimeMillis()); _userPlaylists.value = _userPlaylists.value.map { if (it.id == playlistId) u else it }; persistPlaylists() }
    
    fun removeFromPlaylist(playlistId: String, trackIds: List<String>) { val p = _userPlaylists.value.find { it.id == playlistId } ?: return; val u = p.copy(trackIds = p.trackIds - trackIds.toSet(), updatedAt = System.currentTimeMillis()); _userPlaylists.value = _userPlaylists.value.map { if (it.id == playlistId) u else it }; persistPlaylists() }
    
    fun getPlaylistById(id: String) = if (id == "favorites") _favorites.value else _userPlaylists.value.find { it.id == id }
    
    fun getTracksForPlaylist(playlist: UserPlaylist, allTracks: List<MusicTrack>) = playlist.trackIds.mapNotNull { id -> allTracks.find { it.id == id } }
}
