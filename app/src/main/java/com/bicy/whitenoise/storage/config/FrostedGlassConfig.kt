package com.bicy.whitenoise.storage.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bicy.whitenoise.WhiteNoiseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 毛玻璃效果配置。
 * 纯装饰方案（渐变 + 模糊 + 暗度 + 边缘高光），不依赖 AGSL Shader。
 */
object FrostedGlassConfig {

    private const val KEY_ENABLED = "frosted_enabled"
    private const val KEY_BLUR = "frosted_blur"
    private const val KEY_OPACITY = "frosted_opacity"
    private const val KEY_EDGE_HIGHLIGHT = "frosted_edge_highlight"
    private const val KEY_DARKNESS = "frosted_darkness"

    private const val DEFAULT_ENABLED = false
    private const val DEFAULT_BLUR = 0.5f
    private const val DEFAULT_OPACITY = 0.5f
    private const val DEFAULT_EDGE_HIGHLIGHT = 0.3f
    private const val DEFAULT_DARKNESS = 0.3f

    private val prefs: SharedPreferences by lazy {
        WhiteNoiseApp.context.getSharedPreferences("frosted_glass_config", Context.MODE_PRIVATE)
    }

    private val _enabledFlow = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED))
    val enabledFlow: StateFlow<Boolean> = _enabledFlow

    private val _blurFlow = MutableStateFlow(prefs.getFloat(KEY_BLUR, DEFAULT_BLUR))
    val blurFlow: StateFlow<Float> = _blurFlow

    private val _opacityFlow = MutableStateFlow(prefs.getFloat(KEY_OPACITY, DEFAULT_OPACITY))
    val opacityFlow: StateFlow<Float> = _opacityFlow

    private val _edgeHighlightFlow = MutableStateFlow(prefs.getFloat(KEY_EDGE_HIGHLIGHT, DEFAULT_EDGE_HIGHLIGHT))
    val edgeHighlightFlow: StateFlow<Float> = _edgeHighlightFlow

    private val _darknessFlow = MutableStateFlow(prefs.getFloat(KEY_DARKNESS, DEFAULT_DARKNESS))
    val darknessFlow: StateFlow<Float> = _darknessFlow

    fun setEnabled(value: Boolean) {
        _enabledFlow.value = value
        prefs.edit { putBoolean(KEY_ENABLED, value) }
    }

    fun setBlur(value: Float) {
        _blurFlow.value = value
        prefs.edit { putFloat(KEY_BLUR, value) }
    }

    fun setOpacity(value: Float) {
        _opacityFlow.value = value
        prefs.edit { putFloat(KEY_OPACITY, value) }
    }

    fun setEdgeHighlight(value: Float) {
        _edgeHighlightFlow.value = value
        prefs.edit { putFloat(KEY_EDGE_HIGHLIGHT, value) }
    }

    fun setDarkness(value: Float) {
        _darknessFlow.value = value
        prefs.edit { putFloat(KEY_DARKNESS, value) }
    }
}
