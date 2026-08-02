package com.bicy.whitenoise.onlinemusic.model.SourceModelsPart

data class ScriptInfo(
    val id: String,
    val name: String,
    val description: String = "",
    val version: String = "",
    val author: String = "",
    val homepage: String = "",
    val rawScript: String = "",
    val allowShowUpdateAlert: Boolean = false,
)
