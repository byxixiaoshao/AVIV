package com.bicy.whitenoise.subPage.home.model.SoundMetadataPart

import com.google.gson.annotations.SerializedName

data class SoundsManifest(
    val version: String,
    val categories: List<SoundCategory>,
    val sounds: List<SoundMetadata>,
    @SerializedName("Language")
    val Language: Map<String, Map<String, String>>
)
