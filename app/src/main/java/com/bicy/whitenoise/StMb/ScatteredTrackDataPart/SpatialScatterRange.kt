package com.bicy.whitenoise.StMb.ScatteredTrackDataPart

data class SpatialScatterRange(
    val minRadius: Float = 0.5f,
    val maxRadius: Float = 5.0f,
    val xEnabled: Boolean = true,
    val yEnabled: Boolean = true,
    val zEnabled: Boolean = true,
    val moveEnabled: Boolean = false,
    val moveRandomValue: Float = 0.5f,
    val moveSpeed: Float = 1.0f,
    val directionRandomValue: Float = 0.3f
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "minRadius" to minRadius,
            "maxRadius" to maxRadius,
            "xEnabled" to xEnabled,
            "yEnabled" to yEnabled,
            "zEnabled" to zEnabled,
            "moveEnabled" to moveEnabled,
            "moveRandomValue" to moveRandomValue,
            "moveSpeed" to moveSpeed,
            "directionRandomValue" to directionRandomValue
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): SpatialScatterRange {
            return SpatialScatterRange(
                minRadius = (map["minRadius"] as? Float) ?: 0.5f,
                maxRadius = (map["maxRadius"] as? Float) ?: 5.0f,
                xEnabled = (map["xEnabled"] as? Boolean) ?: true,
                yEnabled = (map["yEnabled"] as? Boolean) ?: true,
                zEnabled = (map["zEnabled"] as? Boolean) ?: true,
                moveEnabled = (map["moveEnabled"] as? Boolean) ?: false,
                moveRandomValue = (map["moveRandomValue"] as? Float) ?: 0.5f,
                moveSpeed = (map["moveSpeed"] as? Float) ?: 1.0f,
                directionRandomValue = (map["directionRandomValue"] as? Float) ?: 0.3f
            )
        }
    }
}
