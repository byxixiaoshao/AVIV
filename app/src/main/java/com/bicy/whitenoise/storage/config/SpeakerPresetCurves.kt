package com.bicy.whitenoise.storage.config

import kotlin.math.ln

/**
 * Kotlin 层定义的扬声器补偿预设曲线。
 *
 * C++ 已知的预设（phone/earphone/bluetooth/car/flat）由引擎内部计算，
 * 新增的设备/场景预设在此定义基础补偿曲线（12 频段增益）。
 * 非标准频段数时按对数频率插值。
 */
object SpeakerPresetCurves {

    /** 标准 12 频段中心频率 */
    val BAND_FREQS = floatArrayOf(25f, 50f, 100f, 200f, 400f, 800f, 1600f, 3200f, 6300f, 10000f, 14000f, 16000f)

    /** C++ 引擎已知的预设（不需要 Kotlin 曲线） */
    val NATIVE_PRESETS = setOf("phone", "earphone", "bluetooth", "car", "flat")

    enum class PresetCategory { DEVICE, SCENE }

    data class PresetInfo(
        val englishName: String,
        val category: PresetCategory,
        /** 与 BAND_FREQS 等长的增益数组（dB） */
        val gains: FloatArray
    )

    /**
     * 新增预设列表。增益值参考典型听感补偿曲线：
     * - 设备类：针对不同回放设备的频响缺陷进行补偿
     * - 场景类：针对不同听音环境的听感偏好进行优化
     */
    val presets: List<PresetInfo> = listOf(
        // --- 设备类 ---
        PresetInfo("tablet", PresetCategory.DEVICE,
            floatArrayOf(2f, 2f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 0f)),
        PresetInfo("headphone", PresetCategory.DEVICE,
            floatArrayOf(0f, 0f, -1f, -1f, -1f, 0f, 0f, 0f, 1f, 1f, 2f, 2f)),
        PresetInfo("desktop", PresetCategory.DEVICE,
            floatArrayOf(1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f)),
        PresetInfo("tv", PresetCategory.DEVICE,
            floatArrayOf(3f, 3f, 2f, 1f, 0f, 0f, 0f, 0f, 1f, 2f, 2f, 1f)),
        // --- 场景类 ---
        PresetInfo("cinema", PresetCategory.SCENE,
            floatArrayOf(5f, 5f, 4f, 2f, -1f, -1f, -1f, 0f, 2f, 3f, 3f, 2f)),
        PresetInfo("night", PresetCategory.SCENE,
            floatArrayOf(-4f, -4f, -3f, -2f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 0f)),
        PresetInfo("outdoor", PresetCategory.SCENE,
            floatArrayOf(0f, 0f, 0f, 1f, 2f, 3f, 3f, 2f, 1f, 0f, 0f, 0f)),
        PresetInfo("studio", PresetCategory.SCENE,
            floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
    )

    private val byName = presets.associateBy { it.englishName }

    fun isKotlinPreset(englishName: String): Boolean = englishName in byName

    /** 对数频率空间插值，返回该频率处的增益 */
    fun gainForFreq(englishName: String, freq: Float): Float {
        val info = byName[englishName] ?: return 0f
        val gains = info.gains
        val freqs = BAND_FREQS
        if (freq <= freqs[0]) return gains[0]
        if (freq >= freqs.last()) return gains.last()
        for (i in 0 until freqs.size - 1) {
            if (freq in freqs[i]..freqs[i + 1]) {
                val lo = ln(freqs[i])
                val hi = ln(freqs[i + 1])
                val ratio = ((ln(freq) - lo) / (hi - lo)).coerceIn(0f, 1f)
                return gains[i] * (1 - ratio) + gains[i + 1] * ratio
            }
        }
        return 0f
    }

    fun getCategory(englishName: String): PresetCategory? = byName[englishName]?.category
}
