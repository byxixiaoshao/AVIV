package com.bicy.whitenoise.H3HO

data class ReverbConfig(
    val enabled: Boolean = false,
    val preset: String = "NONE",
    val roomSize: Float = 0f,
    val decayTime: Float = 1.5f,
    val damping: Float = 0f,
    val wetLevel: Float = 0f,
    val dryLevel: Float = 1f,
    val preDelay: Float = 0.025f,
    val insulation: Float = 0f,
    val reflectionDensity: Float = 0.5f,
    val reflectionSpread: Float = 0.5f,
    val highpassCutoff: Float = 100f,
    val earlyReflectionLevel: Float = 0f
)
