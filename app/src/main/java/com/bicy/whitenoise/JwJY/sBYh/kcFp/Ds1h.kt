package com.bicy.whitenoise.JwJY.sBYh.kcFp

import com.bicy.whitenoise.H3HO.ReverbConfig

data class SoundCategory(
    val id: String,
    val name: String,
    val isCustom: Boolean = false
)

data class SoundMetadata(
    val id: String,
    val name: String,
    val displayName: String = name,
    val category: String = "",
    val assetPath: String? = null,
    val customPath: String? = null,
    val duration: Long = 0,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val remoteUrl: String? = null,
    val author: String? = null,
    val authorUrl: String? = null,
    val type: SoundType = SoundType.NETWORK_DOWNLOAD,
    val downloadDate: Long? = null,
    val fileSize: Long? = null,
    val uri: android.net.Uri? = null,
    val addedAt: Long = System.currentTimeMillis()
)

enum class SoundType {
    NETWORK_DOWNLOAD,
    LOCAL_SYNTHESIS,
    LOCAL_IMPORT
}

data class SoundPlayConfig(
    val id: String,
    val name: String,
    val volume: Float = 1f,
    val reverbConfig: ReverbConfig = ReverbConfig(),
    val spatialAudioConfig: SpatialAudioConfig = SpatialAudioConfig(),
    val creativeEffectConfig: CreativeEffectConfig = CreativeEffectConfig(),
    val translations: Map<String, String>? = null,
    val trackType: String = "loop",
    val audioClips: List<ScatteredAudioClipData> = emptyList(),
    val minIntervalMs: Long = 3000,
    val maxIntervalMs: Long = 10000,
    val spatialScatterRange: SpatialScatterRangeData = SpatialScatterRangeData(),
    val spatialScatterEnabled: Boolean = false,
    val overlayMode: Boolean = false
)

data class ScatteredAudioClipData(
    val id: String,
    val name: String,
    val filePath: String,
    val durationMs: Long = 0
)

data class SpatialScatterRangeData(
    val xMin: Float = -5f,
    val xMax: Float = 5f,
    val yMin: Float = 0f,
    val yMax: Float = 3f,
    val zMin: Float = -5f,
    val zMax: Float = 5f
)

data class SpatialAudioConfig(
    val enabled: Boolean = false,
    val offsetType: Int = 0,
    val fixedLeftRight: Float = 0f,
    val fixedUpDown: Float = 0f,
    val fixedFrontBack: Float = 0f,
    val fixedMultiplier: Float = 1f,
    val surroundMode: Int = 0,
    val surroundRadius: Float = 1f,
    val surroundSpeed: Float = 5f,
    val randomMaxDistance: Float = 5f,
    val randomMinDistance: Float = 0f,
    val randomValue: Float = 0.5f,
    val randomSpeed: Float = 0.3f
)

data class CreativeEffectConfig(
    val loFi: Float = 0f,
    val eightBit: Float = 0f,
    val underwater: Float = 0f,
    val alienSignal: Float = 0f,
    val megaphone: Float = 0f,
    val hifi: Float = 0f
)

data class PlaybackState(
    val isPaused: Boolean = false,
    val sounds: List<SoundPlayConfig> = emptyList()
)
