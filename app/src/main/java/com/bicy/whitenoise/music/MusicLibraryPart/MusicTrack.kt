package com.bicy.whitenoise.music.MusicLibraryPart

import android.net.Uri
import android.provider.MediaStore

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
    val dateAdded: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val streamUrl: String? = null,
    val source: String? = null
) {
    val contentUri: Uri? get() = uriString?.let { Uri.parse(it) }
        ?: if (mediaStoreId > 0) Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId.toString()) else null
    val playUri: String get() = when { isOnline && streamUrl != null -> streamUrl; uriString != null -> uriString; else -> "file://$path" }
    val cachePath: String? get() = if (isOnline) path else null
    override fun equals(other: Any?): Boolean = this === other || (other is MusicTrack && id == other.id)
    override fun hashCode(): Int = id.hashCode()
}

data class ScanProgress(
    val isScanning: Boolean = false,
    val currentPath: String = "",
    val filesFound: Int = 0,
    val totalFiles: Int = 0
)
