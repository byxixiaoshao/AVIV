package com.bicy.whitenoise.onlinemusic.model.SourceModelsPart

data class SourceInfo(
    val type: String = "music",
    val actions: List<String>,
    val qualitys: List<Quality>,
)
