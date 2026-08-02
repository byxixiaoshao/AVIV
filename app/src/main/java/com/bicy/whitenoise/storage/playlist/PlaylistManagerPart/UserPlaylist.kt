package com.bicy.whitenoise.storage.playlist.PlaylistManagerPart

import org.json.JSONArray
import org.json.JSONObject

data class UserPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("trackIds", JSONArray(trackIds))
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }
    
    companion object {
        fun fromJson(json: JSONObject): UserPlaylist {
            val trackIds = mutableListOf<String>()
            val arr = json.optJSONArray("trackIds")
            if (arr != null) for (i in 0 until arr.length()) trackIds.add(arr.getString(i))
            return UserPlaylist(id = json.getString("id"), name = json.getString("name"),
                trackIds = trackIds, createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()))
        }
    }
}
