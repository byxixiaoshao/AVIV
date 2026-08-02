package com.bicy.whitenoise.storage.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bicy.whitenoise.WhiteNoiseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 背景玻璃模糊效果配置
 * 5种玻璃类型：磨砂、毛玻璃、格栅、云雾、丝绸
 */
object BackgroundGlassConfig {

    enum class GlassType(val key: String) {
        SANDBLASTED("sandblasted"),  // 磨砂玻璃
        FROSTED("frosted"),          // 毛玻璃（默认）
        GRID("grid"),                // 格栅玻璃
        MISTY("misty"),              // 云雾玻璃
        SILK("silk");               // 丝绸玻璃

        companion object {
            fun fromKey(key: String?): GlassType =
                entries.find { it.key == key } ?: FROSTED
        }
    }

    private const val KEY_ENABLED = "bg_glass_enabled"
    private const val KEY_TYPE = "bg_glass_type"
    // 通用参数
    private const val KEY_BLUR = "bg_glass_blur"
    private const val KEY_OPACITY = "bg_glass_opacity"
    private const val KEY_DARKNESS = "bg_glass_darkness"
    // 磨砂玻璃专用
    private const val KEY_NOISE = "bg_glass_noise"
    // 格栅玻璃专用
    private const val KEY_GRID_SIZE = "bg_glass_grid_size"
    // 云雾玻璃专用
    private const val KEY_GRADIENT = "bg_glass_gradient"
    // 丝绸玻璃专用
    private const val KEY_SHEEN = "bg_glass_sheen"

    // 默认值
    const val DEFAULT_ENABLED = false
    val DEFAULT_TYPE = GlassType.FROSTED
    const val DEFAULT_BLUR = 0.5f
    const val DEFAULT_OPACITY = 0.5f
    const val DEFAULT_DARKNESS = 0.3f
    const val DEFAULT_NOISE = 0.3f
    const val DEFAULT_GRID_SIZE = 0.3f
    const val DEFAULT_GRADIENT = 0.5f
    const val DEFAULT_SHEEN = 0.4f

    private val prefs: SharedPreferences by lazy {
        WhiteNoiseApp.context.getSharedPreferences("background_glass_config", Context.MODE_PRIVATE)
    }

    private val _enabledFlow = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED))
    val enabledFlow: StateFlow<Boolean> = _enabledFlow

    private val _typeFlow = MutableStateFlow(GlassType.fromKey(prefs.getString(KEY_TYPE, null)))
    val typeFlow: StateFlow<GlassType> = _typeFlow

    private val _blurFlow = MutableStateFlow(prefs.getFloat(KEY_BLUR, DEFAULT_BLUR))
    val blurFlow: StateFlow<Float> = _blurFlow

    private val _opacityFlow = MutableStateFlow(prefs.getFloat(KEY_OPACITY, DEFAULT_OPACITY))
    val opacityFlow: StateFlow<Float> = _opacityFlow

    private val _darknessFlow = MutableStateFlow(prefs.getFloat(KEY_DARKNESS, DEFAULT_DARKNESS))
    val darknessFlow: StateFlow<Float> = _darknessFlow

    private val _noiseFlow = MutableStateFlow(prefs.getFloat(KEY_NOISE, DEFAULT_NOISE))
    val noiseFlow: StateFlow<Float> = _noiseFlow

    private val _gridSizeFlow = MutableStateFlow(prefs.getFloat(KEY_GRID_SIZE, DEFAULT_GRID_SIZE))
    val gridSizeFlow: StateFlow<Float> = _gridSizeFlow

    private val _gradientFlow = MutableStateFlow(prefs.getFloat(KEY_GRADIENT, DEFAULT_GRADIENT))
    val gradientFlow: StateFlow<Float> = _gradientFlow

    private val _sheenFlow = MutableStateFlow(prefs.getFloat(KEY_SHEEN, DEFAULT_SHEEN))
    val sheenFlow: StateFlow<Float> = _sheenFlow

    fun setEnabled(value: Boolean) { _enabledFlow.value = value; prefs.edit { putBoolean(KEY_ENABLED, value) } }
    fun setType(value: GlassType) { _typeFlow.value = value; prefs.edit { putString(KEY_TYPE, value.key) } }
    fun setBlur(value: Float) { _blurFlow.value = value; prefs.edit { putFloat(KEY_BLUR, value) } }
    fun setOpacity(value: Float) { _opacityFlow.value = value; prefs.edit { putFloat(KEY_OPACITY, value) } }
    fun setDarkness(value: Float) { _darknessFlow.value = value; prefs.edit { putFloat(KEY_DARKNESS, value) } }
    fun setNoise(value: Float) { _noiseFlow.value = value; prefs.edit { putFloat(KEY_NOISE, value) } }
    fun setGridSize(value: Float) { _gridSizeFlow.value = value; prefs.edit { putFloat(KEY_GRID_SIZE, value) } }
    fun setGradient(value: Float) { _gradientFlow.value = value; prefs.edit { putFloat(KEY_GRADIENT, value) } }
    fun setSheen(value: Float) { _sheenFlow.value = value; prefs.edit { putFloat(KEY_SHEEN, value) } }
}
