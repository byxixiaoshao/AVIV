package com.bicy.whitenoise.storage.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bicy.whitenoise.WhiteNoiseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 液态玻璃渲染参数配置
 * COMPATIBLE 和 PERFECT 模式各自独立存储
 */
object GlassRenderConfig {
    // ── COMPATIBLE 模式参数（Android <13 降级实现） ──
    private const val KEY_COMPAT_OPACITY = "glass_compat_opacity"
    private const val KEY_COMPAT_DARKNESS = "glass_compat_darkness"
    private const val KEY_COMPAT_SCALE = "glass_compat_scale"
    private const val KEY_COMPAT_SHADOW_ENABLED = "glass_compat_shadow_enabled"
    private const val KEY_COMPAT_SHADOW_STRENGTH = "glass_compat_shadow_strength"
    private const val KEY_COMPAT_SHADOW_HEIGHT = "glass_compat_shadow_height"

    // ── PERFECT 模式参数（Android >=13 Shader） ──
    private const val KEY_PERF_BLUR = "glass_perf_blur"
    private const val KEY_PERF_SCALE = "glass_perf_scale"
    private const val KEY_PERF_DISTORTION = "glass_perf_distortion"
    private const val KEY_PERF_DARKNESS = "glass_perf_darkness"
    private const val KEY_PERF_WARP = "glass_perf_warp"
    private const val KEY_PERF_ELEVATION = "glass_perf_elevation"
    private const val KEY_PERF_SHADOW_ENABLED = "glass_perf_shadow_enabled"
    private const val KEY_PERF_SHADOW_STRENGTH = "glass_perf_shadow_strength"

    // ── 硬编码默认值（基于中端性能参考） ──
    /** COMPATIBLE 默认值 */
    const val DEFAULT_COMPAT_OPACITY = 0.2f
    const val DEFAULT_COMPAT_DARKNESS = 0.05f
    const val DEFAULT_COMPAT_SCALE = 0.05f
    const val DEFAULT_COMPAT_SHADOW_ENABLED = false
    const val DEFAULT_COMPAT_SHADOW_STRENGTH = 0.3f
    const val DEFAULT_COMPAT_SHADOW_HEIGHT = 8f

    /** PERFECT 默认值 */
    const val DEFAULT_PERF_BLUR = 0.4f
    const val DEFAULT_PERF_SCALE = 0.1f
    const val DEFAULT_PERF_DISTORTION = 0.1f
    const val DEFAULT_PERF_DARKNESS = 0.08f
    const val DEFAULT_PERF_WARP = 0.05f
    const val DEFAULT_PERF_ELEVATION = 4f
    const val DEFAULT_PERF_SHADOW_ENABLED = false
    const val DEFAULT_PERF_SHADOW_STRENGTH = 0.3f

    private val prefs: SharedPreferences by lazy {
        WhiteNoiseApp.context.getSharedPreferences("glass_render_config", Context.MODE_PRIVATE)
    }

    // ── COMPATIBLE StateFlows ──
    private val _compatOpacityFlow = MutableStateFlow(prefs.getFloat(KEY_COMPAT_OPACITY, DEFAULT_COMPAT_OPACITY))
    val compatOpacityFlow: StateFlow<Float> = _compatOpacityFlow

    private val _compatDarknessFlow = MutableStateFlow(prefs.getFloat(KEY_COMPAT_DARKNESS, DEFAULT_COMPAT_DARKNESS))
    val compatDarknessFlow: StateFlow<Float> = _compatDarknessFlow

    private val _compatScaleFlow = MutableStateFlow(prefs.getFloat(KEY_COMPAT_SCALE, DEFAULT_COMPAT_SCALE))
    val compatScaleFlow: StateFlow<Float> = _compatScaleFlow

    private val _compatShadowEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_COMPAT_SHADOW_ENABLED, DEFAULT_COMPAT_SHADOW_ENABLED))
    val compatShadowEnabledFlow: StateFlow<Boolean> = _compatShadowEnabledFlow

    private val _compatShadowStrengthFlow = MutableStateFlow(prefs.getFloat(KEY_COMPAT_SHADOW_STRENGTH, DEFAULT_COMPAT_SHADOW_STRENGTH))
    val compatShadowStrengthFlow: StateFlow<Float> = _compatShadowStrengthFlow

    private val _compatShadowHeightFlow = MutableStateFlow(prefs.getFloat(KEY_COMPAT_SHADOW_HEIGHT, DEFAULT_COMPAT_SHADOW_HEIGHT))
    val compatShadowHeightFlow: StateFlow<Float> = _compatShadowHeightFlow

    // ── PERFECT StateFlows ──
    private val _perfBlurFlow = MutableStateFlow(prefs.getFloat(KEY_PERF_BLUR, DEFAULT_PERF_BLUR))
    val perfBlurFlow: StateFlow<Float> = _perfBlurFlow

    private val _perfScaleFlow = MutableStateFlow(prefs.getFloat(KEY_PERF_SCALE, DEFAULT_PERF_SCALE))
    val perfScaleFlow: StateFlow<Float> = _perfScaleFlow

    private val _perfDistortionFlow = MutableStateFlow(prefs.getFloat(KEY_PERF_DISTORTION, DEFAULT_PERF_DISTORTION))
    val perfDistortionFlow: StateFlow<Float> = _perfDistortionFlow

    private val _perfDarknessFlow = MutableStateFlow(prefs.getFloat(KEY_PERF_DARKNESS, DEFAULT_PERF_DARKNESS))
    val perfDarknessFlow: StateFlow<Float> = _perfDarknessFlow

    private val _perfWarpFlow = MutableStateFlow(prefs.getFloat(KEY_PERF_WARP, DEFAULT_PERF_WARP))
    val perfWarpFlow: StateFlow<Float> = _perfWarpFlow

    private val _perfElevationFlow = MutableStateFlow(prefs.getFloat(KEY_PERF_ELEVATION, DEFAULT_PERF_ELEVATION))
    val perfElevationFlow: StateFlow<Float> = _perfElevationFlow

    private val _perfShadowEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_PERF_SHADOW_ENABLED, DEFAULT_PERF_SHADOW_ENABLED))
    val perfShadowEnabledFlow: StateFlow<Boolean> = _perfShadowEnabledFlow

    private val _perfShadowStrengthFlow = MutableStateFlow(prefs.getFloat(KEY_PERF_SHADOW_STRENGTH, DEFAULT_PERF_SHADOW_STRENGTH))
    val perfShadowStrengthFlow: StateFlow<Float> = _perfShadowStrengthFlow

    // ── COMPATIBLE setters ──
    fun setCompatOpacity(value: Float) { _compatOpacityFlow.value = value; prefs.edit { putFloat(KEY_COMPAT_OPACITY, value) } }
    fun setCompatDarkness(value: Float) { _compatDarknessFlow.value = value; prefs.edit { putFloat(KEY_COMPAT_DARKNESS, value) } }
    fun setCompatScale(value: Float) { _compatScaleFlow.value = value; prefs.edit { putFloat(KEY_COMPAT_SCALE, value) } }
    fun setCompatShadowEnabled(value: Boolean) { _compatShadowEnabledFlow.value = value; prefs.edit { putBoolean(KEY_COMPAT_SHADOW_ENABLED, value) } }
    fun setCompatShadowStrength(value: Float) { _compatShadowStrengthFlow.value = value; prefs.edit { putFloat(KEY_COMPAT_SHADOW_STRENGTH, value) } }
    fun setCompatShadowHeight(value: Float) { _compatShadowHeightFlow.value = value; prefs.edit { putFloat(KEY_COMPAT_SHADOW_HEIGHT, value) } }

    // ── PERFECT setters ──
    fun setPerfBlur(value: Float) { _perfBlurFlow.value = value; prefs.edit { putFloat(KEY_PERF_BLUR, value) } }
    fun setPerfScale(value: Float) { _perfScaleFlow.value = value; prefs.edit { putFloat(KEY_PERF_SCALE, value) } }
    fun setPerfDistortion(value: Float) { _perfDistortionFlow.value = value; prefs.edit { putFloat(KEY_PERF_DISTORTION, value) } }
    fun setPerfDarkness(value: Float) { _perfDarknessFlow.value = value; prefs.edit { putFloat(KEY_PERF_DARKNESS, value) } }
    fun setPerfWarp(value: Float) { _perfWarpFlow.value = value; prefs.edit { putFloat(KEY_PERF_WARP, value) } }
    fun setPerfElevation(value: Float) { _perfElevationFlow.value = value; prefs.edit { putFloat(KEY_PERF_ELEVATION, value) } }
    fun setPerfShadowEnabled(value: Boolean) { _perfShadowEnabledFlow.value = value; prefs.edit { putBoolean(KEY_PERF_SHADOW_ENABLED, value) } }
    fun setPerfShadowStrength(value: Float) { _perfShadowStrengthFlow.value = value; prefs.edit { putFloat(KEY_PERF_SHADOW_STRENGTH, value) } }
}
