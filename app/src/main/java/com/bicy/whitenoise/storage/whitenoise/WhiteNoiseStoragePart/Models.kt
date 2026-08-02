package com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart

import com.bicy.whitenoise.audio.ReverbConfig

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
    val categoryName: String = "",
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
    val overlayMode: Boolean = false,
    val eqEnabled: Boolean = false,
    val filePath: String? = null,
    // 播放速度/音调（按轨道独立持久化，与音乐速度完全独立）
    // 速度 0.1x-5.0x（1.0=原速），音调 ±12 半音（0=原调），通过 SoundTouch 实时变速变调
    val playbackSpeed: Float = 1f,
    val pitchShift: Float = 0f
)

data class ScatteredAudioClipData(
    val id: String,
    val name: String,
    val filePath: String,
    val durationMs: Long = 0
)

data class SpatialScatterRangeData(
    val minRadius: Float = 0.5f,
    val maxRadius: Float = 5.0f,
    val xEnabled: Boolean = true,
    val yEnabled: Boolean = true,
    val zEnabled: Boolean = true,
    val moveEnabled: Boolean = false,
    val moveRandomValue: Float = 0.5f,
    val moveSpeed: Float = 1.0f,
    val directionRandomValue: Float = 0.3f
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
    val hifi: Float = 0f,
    val stereoWidener: Float = 0.5f,
    val virtualBass: Float = 0.2f,
    val multibandCompressor: Float = 0.5f
)

data class PlaybackState(
    val isPaused: Boolean = false,
    val sounds: List<SoundPlayConfig> = emptyList()
)
