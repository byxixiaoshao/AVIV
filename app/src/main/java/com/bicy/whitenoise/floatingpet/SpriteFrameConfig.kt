package com.bicy.whitenoise.floatingpet

data class SpriteFrameConfig(
    val title: String,
    val width: Int,
    val height: Int,
    val animations: Map<String, AnimationConfig>
)

data class AnimationConfig(
    val image: String,
    val frameRate: Int,
    val speed: Float
) {
    fun getFrameFileName(index: Int): String = "${image}_${index}.png"
}