package com.bicy.whitenoise.subPage.home.model.SoundMetadataPart

data class SoundMetadata(
    val id: String,
    val name: String,
    val category: String,
    val categoryName: String = "",
    val remoteUrl: String,
    val author: String,
    val authorUrl: String,
    val translations: Map<String, String>? = null
)
