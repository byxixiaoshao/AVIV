package com.bicy.whitenoise.onlinemusic.model.SourceModelsPart

data class SourceRequest(
    val requestKey: String,
    val source: Source,
    val action: String,
    val info: Any,
)
