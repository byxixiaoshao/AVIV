package com.bicy.whitenoise.DzBD.IrBh.XsdL

import com.google.gson.annotations.SerializedName

data class ScatteredCategory(
    val id: String,
    val name: String
)

data class ScatteredSoundType(
    val id: String,
    val name: String,
    val category: String
)

data class ScatteredSound(
    val id: String,
    val name: String,
    val type: String,
    val remoteUrl: String,
    val author: String,
    val authorUrl: String
)

data class ScatteredSoundsManifest(
    val version: String,
    val categories: List<ScatteredCategory>,
    val soundTypes: List<ScatteredSoundType>,
    val sounds: List<ScatteredSound>,
    @SerializedName("Language")
    val Language: Map<String, Map<String, String>>
)

data class ScatteredSoundWithType(
    val id: String,
    val name: String,
    val typeId: String,
    val typeName: String,
    val categoryId: String,
    val categoryName: String,
    val remoteUrl: String? = null,
    val author: String? = null,
    val authorUrl: String? = null,
    val translations: Map<String, String>? = null
)

data class ScatteredSoundTypeWithSounds(
    val typeId: String,
    val typeName: String,
    val categoryId: String,
    val categoryName: String,
    val translations: Map<String, String>? = null,
    val sounds: List<ScatteredSoundWithType> = emptyList()
)

data class ScatteredCategoryWithTypes(
    val categoryId: String,
    val categoryName: String,
    val translations: Map<String, String>? = null,
    val soundTypes: List<ScatteredSoundTypeWithSounds> = emptyList()
)
