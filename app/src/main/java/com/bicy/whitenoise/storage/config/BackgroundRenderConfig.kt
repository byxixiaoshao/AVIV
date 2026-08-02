package com.bicy.whitenoise.storage.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bicy.whitenoise.WhiteNoiseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 背景渲染参数配置
 * 控制背景图片与背景层的图像处理效果
 */
object BackgroundRenderConfig {
    private const val KEY_ENABLED = "bg_render_enabled"
    private const val KEY_BRIGHTNESS = "bg_render_brightness"
    private const val KEY_CONTRAST = "bg_render_contrast"
    private const val KEY_SATURATION = "bg_render_saturation"
    private const val KEY_HIGHLIGHTS = "bg_render_highlights"
    private const val KEY_SHADOWS = "bg_render_shadows"
    private const val KEY_BLUR = "bg_render_blur"
    private const val KEY_VIGNETTE = "bg_render_vignette"

    // 默认值
    const val DEFAULT_ENABLED = false
    const val DEFAULT_BRIGHTNESS = 0f      // -1.0 ~ 1.0, 0 = 原始
    const val DEFAULT_CONTRAST = 0f        // -1.0 ~ 1.0, 0 = 原始
    const val DEFAULT_SATURATION = 0f      // -1.0 ~ 1.0, 0 = 原始
    const val DEFAULT_HIGHLIGHTS = 0f       // -1.0 ~ 1.0, 0 = 原始
    const val DEFAULT_SHADOWS = 0f          // -1.0 ~ 1.0, 0 = 原始
    const val DEFAULT_BLUR = 0f            // 0.0 ~ 1.0, 0 = 无模糊
    const val DEFAULT_VIGNETTE = 0f        // 0.0 ~ 1.0, 0 = 无暗角

    private val prefs: SharedPreferences by lazy {
        WhiteNoiseApp.context.getSharedPreferences("background_render_config", Context.MODE_PRIVATE)
    }

    private val _enabledFlow = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED))
    val enabledFlow: StateFlow<Boolean> = _enabledFlow

    private val _brightnessFlow = MutableStateFlow(prefs.getFloat(KEY_BRIGHTNESS, DEFAULT_BRIGHTNESS))
    val brightnessFlow: StateFlow<Float> = _brightnessFlow

    private val _contrastFlow = MutableStateFlow(prefs.getFloat(KEY_CONTRAST, DEFAULT_CONTRAST))
    val contrastFlow: StateFlow<Float> = _contrastFlow

    private val _saturationFlow = MutableStateFlow(prefs.getFloat(KEY_SATURATION, DEFAULT_SATURATION))
    val saturationFlow: StateFlow<Float> = _saturationFlow

    private val _highlightsFlow = MutableStateFlow(prefs.getFloat(KEY_HIGHLIGHTS, DEFAULT_HIGHLIGHTS))
    val highlightsFlow: StateFlow<Float> = _highlightsFlow

    private val _shadowsFlow = MutableStateFlow(prefs.getFloat(KEY_SHADOWS, DEFAULT_SHADOWS))
    val shadowsFlow: StateFlow<Float> = _shadowsFlow

    private val _blurFlow = MutableStateFlow(prefs.getFloat(KEY_BLUR, DEFAULT_BLUR))
    val blurFlow: StateFlow<Float> = _blurFlow

    private val _vignetteFlow = MutableStateFlow(prefs.getFloat(KEY_VIGNETTE, DEFAULT_VIGNETTE))
    val vignetteFlow: StateFlow<Float> = _vignetteFlow

    fun setEnabled(value: Boolean) { _enabledFlow.value = value; prefs.edit { putBoolean(KEY_ENABLED, value) } }
    fun setBrightness(value: Float) { _brightnessFlow.value = value; prefs.edit { putFloat(KEY_BRIGHTNESS, value) } }
    fun setContrast(value: Float) { _contrastFlow.value = value; prefs.edit { putFloat(KEY_CONTRAST, value) } }
    fun setSaturation(value: Float) { _saturationFlow.value = value; prefs.edit { putFloat(KEY_SATURATION, value) } }
    fun setHighlights(value: Float) { _highlightsFlow.value = value; prefs.edit { putFloat(KEY_HIGHLIGHTS, value) } }
    fun setShadows(value: Float) { _shadowsFlow.value = value; prefs.edit { putFloat(KEY_SHADOWS, value) } }
    fun setBlur(value: Float) { _blurFlow.value = value; prefs.edit { putFloat(KEY_BLUR, value) } }
    fun setVignette(value: Float) { _vignetteFlow.value = value; prefs.edit { putFloat(KEY_VIGNETTE, value) } }
}
