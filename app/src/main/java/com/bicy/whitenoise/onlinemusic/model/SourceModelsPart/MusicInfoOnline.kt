package com.bicy.whitenoise.onlinemusic.model.SourceModelsPart

data class MusicInfoOnline(
    val id: String,
    val name: String,
    val singer: String,
    val source: Source,
    val interval: String? = null,
    val songmid: String = "",
    val hash: String = "",
    val strMediaMid: String = "",
    val albumMid: String = "",
    val songId: String = "",
    val albumName: String = "",
    val albumId: String? = null,
    val picUrl: String? = null,
    val qualitys: List<Quality> = emptyList(),
)
