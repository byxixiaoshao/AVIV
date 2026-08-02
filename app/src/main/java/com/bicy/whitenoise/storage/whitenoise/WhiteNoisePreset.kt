package com.bicy.whitenoise.storage.whitenoise

import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SoundPlayConfig
import java.util.UUID

data class WhiteNoisePreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sounds: List<SoundPlayConfig>,
    val createdAt: Long = System.currentTimeMillis()
)
