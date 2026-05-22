package com.bicy.whitenoise.yODW.NvYq.EF5M

object AdditionalParamType {
    const val Pitch = 500
    const val Speed = 501
}

data class ReverbPreset(
    val name: String,
    val roomSize: Float,
    val decayTime: Float,
    val damping: Float,
    val wetLevel: Float,
    val dryLevel: Float,
    val preDelay: Float,
    val reflectionDensity: Float = 0.5f,
    val reflectionSpread: Float = 0.5f,
    val highpassCutoff: Float = 100f,
    val earlyReflectionLevel: Float = 0f
)

val reverbPresets = listOf(
    ReverbPreset("体育场", 0.9f, 4.5f, 0.2f, 0.5f, 0.7f, 0.05f, 0.6f, 0.7f, 80f, 0.3f),
    ReverbPreset("汽车内", 0.1f, 0.3f, 0.8f, 0.3f, 0.9f, 0.005f, 0.8f, 0.6f, 150f, 0.1f),
    ReverbPreset("浴室", 0.2f, 1.0f, 0.1f, 0.6f, 0.8f, 0.01f, 0.7f, 0.5f, 120f, 0.2f),
    ReverbPreset("教堂", 1.0f, 4.0f, 0.15f, 0.55f, 0.6f, 0.08f, 0.5f, 0.6f, 60f, 0.4f),
    ReverbPreset("小俱乐部", 0.4f, 1.5f, 0.4f, 0.4f, 0.85f, 0.02f, 0.6f, 0.5f, 100f, 0.15f),
    ReverbPreset("森林", 0.2f, 0.5f, 0.85f, 0.1f, 0.92f, 0.01f, 0.4f, 0.3f, 300f, 0.15f),
    ReverbPreset("山谷", 0.7f, 1.8f, 0.6f, 0.25f, 0.8f, 0.12f, 0.3f, 0.4f, 250f, 0.4f),
    ReverbPreset("海边", 0.1f, 0.15f, 0.95f, 0.05f, 0.96f, 0.0f, 0.8f, 0.7f, 350f, 0.08f),
    ReverbPreset("沙漠", 0.05f, 0.08f, 0.98f, 0.02f, 0.98f, 0.0f, 0.9f, 0.8f, 400f, 0.0f),
    ReverbPreset("洞穴", 0.85f, 5.0f, 0.05f, 0.45f, 0.65f, 0.1f, 0.5f, 0.4f, 50f, 0.35f),
    ReverbPreset("隧道", 0.6f, 2.5f, 0.2f, 0.4f, 0.75f, 0.05f, 0.4f, 0.3f, 70f, 0.2f)
)
