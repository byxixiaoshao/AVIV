package com.bicy.whitenoise.StMb

import kotlin.random.Random

data class SpatialScatterRange(
    val xMin: Float = -5f,
    val xMax: Float = 5f,
    val yMin: Float = 0f,
    val yMax: Float = 3f,
    val zMin: Float = -5f,
    val zMax: Float = 5f
) {
    private fun ClosedFloatingPointRange<Float>.randomValue(): Float {
        return Random.nextDouble(start.toDouble(), endInclusive.toDouble()).toFloat()
    }
    
    fun randomPosition(): FloatArray {
        return floatArrayOf(
            (xMin..xMax).randomValue(),
            (yMin..yMax).randomValue(),
            (zMin..zMax).randomValue()
        )
    }
    
    fun toMap(): Map<String, Float> {
        return mapOf(
            "xMin" to xMin, "xMax" to xMax,
            "yMin" to yMin, "yMax" to yMax,
            "zMin" to zMin, "zMax" to zMax
        )
    }
    
    companion object {
        fun fromMap(map: Map<String, Float>): SpatialScatterRange {
            return SpatialScatterRange(
                xMin = map["xMin"] ?: -5f,
                xMax = map["xMax"] ?: 5f,
                yMin = map["yMin"] ?: 0f,
                yMax = map["yMax"] ?: 3f,
                zMin = map["zMin"] ?: -5f,
                zMax = map["zMax"] ?: 5f
            )
        }
    }
}

data class ScatteredAudioClip(
    val id: String,
    val name: String,
    val filePath: String,
    val durationMs: Long = 0
)

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

enum class TrackType {
    LOOP,
    SCATTERED
}
