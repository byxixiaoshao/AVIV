package com.bicy.whitenoise.audio.ScatteredPlayerManagerPart

data class ScatteredTrackState(
    val trackId: String,
    val audioClips: List<com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.ScatteredAudioClipData>,
    val minIntervalMs: Long = 3000,
    val maxIntervalMs: Long = 10000,
    val volume: Float = 1.0f,
    val spatialRange: com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SpatialScatterRangeData = com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SpatialScatterRangeData(),
    val spatialEnabled: Boolean = false,
    val overlayMode: Boolean = false,
    val isPlaying: Boolean = false,
    val currentClipId: String? = null
)
