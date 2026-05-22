package com.bicy.whitenoise.DzBD.XP5y

import com.bicy.whitenoise.H3HO.ReverbConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialAudioConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.CreativeEffectConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.ScatteredAudioClipData
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialScatterRangeData

data class PlayListData(
    val isPaused: Boolean = false,
    val sounds: List<SoundPlayData> = emptyList()
)

data class SoundPlayData(
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
    val spatialScatterEnabled: Boolean = false
)
