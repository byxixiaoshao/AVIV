package com.bicy.whitenoise.yODW.SrEO.Xomm

data class ElementTransition(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val alpha: Float = 1f
)

data class TransitionProgress(
    val album: ElementTransition,
    val title: ElementTransition,
    val artist: ElementTransition,
    val prevButton: ElementTransition,
    val playButton: ElementTransition,
    val nextButton: ElementTransition,
    val shuffleButton: ElementTransition,
    val repeatButton: ElementTransition,
    val horizontalProgress: ElementTransition,
    val verticalProgress: ElementTransition,
    val featureButtons: ElementTransition,
    val panelSlide: Float
)

data class EqPresetData(
    val name: String,
    val gains: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EqPresetData
        if (name != other.name) return false
        if (!gains.contentEquals(other.gains)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + gains.contentHashCode()
        return result
    }
}

data class ReverbPreset(
    val name: String,
    val roomSize: Float,
    val decayTime: Float,
    val damping: Float,
    val wetLevel: Float,
    val dryLevel: Float,
    val preDelay: Float
)
