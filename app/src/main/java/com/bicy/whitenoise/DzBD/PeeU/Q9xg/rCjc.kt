package com.bicy.whitenoise.DzBD.PeeU.Q9xg

import com.google.gson.annotations.SerializedName

data class SoundCategory(
    val id: String,
    val name: String
)

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

data class SoundsManifest(
    val version: String,
    val categories: List<SoundCategory>,
    val sounds: List<SoundMetadata>,
    @SerializedName("Language")
    val Language: Map<String, Map<String, String>>
)
