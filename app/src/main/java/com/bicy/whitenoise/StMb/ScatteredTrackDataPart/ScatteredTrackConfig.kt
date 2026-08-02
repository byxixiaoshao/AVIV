package com.bicy.whitenoise.StMb.ScatteredTrackDataPart

data class ScatteredTrackConfig(
    val id: String,
    val name: String,
    val audioClips: List<ScatteredAudioClip> = emptyList(),
    val minIntervalMs: Long = 3000,
    val maxIntervalMs: Long = 10000,
    val spatialRange: SpatialScatterRange = SpatialScatterRange(),
    val volume: Float = 1.0f,
    val maxConcurrent: Int = 3,
    val fadeInMs: Long = 200,
    val fadeOutMs: Long = 300,
    val enabled: Boolean = true
)
