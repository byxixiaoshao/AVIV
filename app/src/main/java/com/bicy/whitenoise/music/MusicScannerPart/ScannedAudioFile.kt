package com.bicy.whitenoise.music.MusicScannerPart

import android.net.Uri

data class ScannedAudioFile(
    val uri: Uri,
    val path: String,
    val name: String,
    val extension: String,
    val length: Long,
    val lastModified: Long,
    val duration: Long = 0,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null
)
