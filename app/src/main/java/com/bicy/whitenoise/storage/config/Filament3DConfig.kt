package com.bicy.whitenoise.storage.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bicy.whitenoise.WhiteNoiseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * 3D 粒子音频可视化背景配置
 * 参数完全对齐 3d-particle-preview/index.html 的 params
 */
object Filament3DConfig {

    private const val KEY_3D_ENABLED = "viz_3d_background_enabled"
    private const val KEY_GLASS_FADE_EXEMPT = "viz_3d_glass_fade_exempt"
    private const val KEY_GYRO_ENABLED = "viz_3d_gyro_enabled"
    private const val KEY_TRANS_DURATION = "viz_3d_trans_duration"
    // 对齐 web params
    private const val KEY_PARTICLE_SIZE = "viz_3d_particle_size"
    private const val KEY_MOVE_SPEED = "viz_3d_move_speed"
    private const val KEY_PULSE_AMT = "viz_3d_pulse_amt"
    private const val KEY_GLOW_STRENGTH = "viz_3d_glow_strength"
    private const val KEY_GLOW_PULSE = "viz_3d_glow_pulse"
    private const val KEY_GLOW_DARK = "viz_3d_glow_dark"
    private const val KEY_GLOW_BRIGHT = "viz_3d_glow_bright"
    private const val KEY_BLOOM_STRENGTH = "viz_3d_bloom_strength"
    private const val KEY_BLOOM_RADIUS = "viz_3d_bloom_radius"
    private const val KEY_BLOOM_THRESHOLD = "viz_3d_bloom_threshold"
    private const val KEY_BG_FOLLOW_COVER = "viz_3d_bg_follow_cover"
    private const val KEY_BG_BRIGHTNESS = "viz_3d_bg_brightness"
    private const val KEY_CUSTOM_BG = "viz_3d_custom_bg"
    private const val KEY_BASS_SENS = "viz_3d_bass_sens"
    private const val KEY_MID_SENS = "viz_3d_mid_sens"
    private const val KEY_TREBLE_SENS = "viz_3d_treble_sens"
    private const val KEY_PARTICLE_COUNT = "viz_3d_particle_count" // 旧版粒子总数, 仅用于迁移
    private const val KEY_PARTICLE_EDGE = "viz_3d_particle_edge"   // 边长粒子数量(edge×edge 网格)
    private const val KEY_GLOW_TIER = "viz_3d_glow_tier"          // 粒子光晕档位: 0关闭 1柔和 2明显 3强霓虹
    private const val KEY_BLOOM_STYLE = "viz_3d_bloom_style"      // 中心光晕样式: 0经典柔光 1十字光芒 2星芒 3环形光波
    private const val KEY_GYRO_SENSITIVITY = "viz_3d_gyro_sensitivity"
    private const val KEY_GYRO_AMOUNT = "viz_3d_gyro_amount"
    private const val KEY_GYRO_RETURN = "viz_3d_gyro_return"
    private const val KEY_GYRO_SMOOTHING = "viz_3d_gyro_smoothing"
    private const val KEY_FPS_LIMIT = "viz_3d_fps_limit"
    private const val KEY_SCATTER_MODE = "viz_3d_scatter_mode"
    private const val KEY_SCATTER_RADIUS = "viz_3d_scatter_radius"

    // 默认值对齐 web params
    const val DEFAULT_PARTICLE_SIZE = 1.0f
    const val DEFAULT_MOVE_SPEED = 1.0f
    const val DEFAULT_PULSE_AMT = 0.3f
    const val DEFAULT_GLOW_STRENGTH = 1.0f
    const val DEFAULT_GLOW_PULSE = false
    const val DEFAULT_GLOW_DARK = 0.3f
    const val DEFAULT_GLOW_BRIGHT = 2.0f
    const val DEFAULT_BLOOM_STRENGTH = 1.5f
    const val DEFAULT_BLOOM_RADIUS = 0.6f
    const val DEFAULT_BLOOM_THRESHOLD = 0.2f
    const val DEFAULT_BG_FOLLOW_COVER = true
    const val DEFAULT_BG_BRIGHTNESS = 0.05f
    const val DEFAULT_CUSTOM_BG = 0xFF101018.toInt()
    const val DEFAULT_BASS_SENS = 1.5f
    const val DEFAULT_MID_SENS = 1.0f
    const val DEFAULT_TREBLE_SENS = 2.0f
    const val DEFAULT_PARTICLE_EDGE = 55
    const val MIN_PARTICLE_EDGE = 10
    const val MAX_PARTICLE_EDGE = 2000
    const val DEFAULT_GLOW_TIER = 2           // 默认"明显可见"
    const val DEFAULT_BLOOM_STYLE = 0         // 默认"经典柔光"
    // 粒子光晕档位
    const val GLOW_TIER_OFF = 0
    const val GLOW_TIER_SOFT = 1
    const val GLOW_TIER_VISIBLE = 2
    const val GLOW_TIER_NEON = 3
    // 中心光晕样式
    const val BLOOM_STYLE_CLASSIC = 0
    const val BLOOM_STYLE_CROSS = 1
    const val BLOOM_STYLE_STAR = 2
    const val BLOOM_STYLE_RING = 3
    const val DEFAULT_GYRO_SENSITIVITY = 1.0f
    const val DEFAULT_GYRO_AMOUNT = 1.0f
    const val DEFAULT_GYRO_RETURN = 0.988f
    const val DEFAULT_GYRO_SMOOTHING = 0.25f
    const val DEFAULT_FPS_LIMIT = 60
    /** 帧率限制挡位: 0 表示无限制 */
    val FPS_TIERS = intArrayOf(30, 60, 90, 120, 144, 165, 0)
    const val DEFAULT_GYRO_ENABLED = true
    const val DEFAULT_TRANS_DURATION = 1.2f

    // ── 无专辑散开形态 ──
    const val SCATTER_MODE_CLOUD = 0   // 球形/椭圆云团
    const val SCATTER_MODE_SHELL = 1   // 球面壳层
    const val SCATTER_MODE_RING = 2    // 扁平星环
    const val SCATTER_MODE_WAVE = 3    // 上下声波罩
    const val DEFAULT_SCATTER_MODE = SCATTER_MODE_CLOUD
    const val DEFAULT_SCATTER_RADIUS = 3.5f
    const val MIN_SCATTER_RADIUS = 1.5f
    const val MAX_SCATTER_RADIUS = 6.0f

    private val prefs: SharedPreferences by lazy {
        WhiteNoiseApp.context.getSharedPreferences("filament_3d_config", Context.MODE_PRIVATE)
    }

    // ── 开关 ──
    private val _threeDEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_3D_ENABLED, false))
    val threeDEnabledFlow: StateFlow<Boolean> = _threeDEnabledFlow
    private val _glassFadeExemptFlow = MutableStateFlow(prefs.getBoolean(KEY_GLASS_FADE_EXEMPT, false))
    val glassFadeExemptFlow: StateFlow<Boolean> = _glassFadeExemptFlow
    private val _glowPulseFlow = MutableStateFlow(prefs.getBoolean(KEY_GLOW_PULSE, DEFAULT_GLOW_PULSE))
    val glowPulseFlow: StateFlow<Boolean> = _glowPulseFlow
    private val _bgFollowCoverFlow = MutableStateFlow(prefs.getBoolean(KEY_BG_FOLLOW_COVER, DEFAULT_BG_FOLLOW_COVER))
    val bgFollowCoverFlow: StateFlow<Boolean> = _bgFollowCoverFlow
    private val _gyroEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_GYRO_ENABLED, DEFAULT_GYRO_ENABLED))
    val gyroEnabledFlow: StateFlow<Boolean> = _gyroEnabledFlow
    private val _transDurationFlow = MutableStateFlow(prefs.getFloat(KEY_TRANS_DURATION, DEFAULT_TRANS_DURATION))
    val transDurationFlow: StateFlow<Float> = _transDurationFlow

    // ── Float 参数 ──
    private val _particleSizeFlow = MutableStateFlow(prefs.getFloat(KEY_PARTICLE_SIZE, DEFAULT_PARTICLE_SIZE))
    val particleSizeFlow: StateFlow<Float> = _particleSizeFlow
    private val _moveSpeedFlow = MutableStateFlow(prefs.getFloat(KEY_MOVE_SPEED, DEFAULT_MOVE_SPEED))
    val moveSpeedFlow: StateFlow<Float> = _moveSpeedFlow
    private val _pulseAmtFlow = MutableStateFlow(prefs.getFloat(KEY_PULSE_AMT, DEFAULT_PULSE_AMT))
    val pulseAmtFlow: StateFlow<Float> = _pulseAmtFlow
    private val _glowStrengthFlow = MutableStateFlow(prefs.getFloat(KEY_GLOW_STRENGTH, DEFAULT_GLOW_STRENGTH))
    val glowStrengthFlow: StateFlow<Float> = _glowStrengthFlow
    private val _glowDarkFlow = MutableStateFlow(prefs.getFloat(KEY_GLOW_DARK, DEFAULT_GLOW_DARK))
    val glowDarkFlow: StateFlow<Float> = _glowDarkFlow
    private val _glowBrightFlow = MutableStateFlow(prefs.getFloat(KEY_GLOW_BRIGHT, DEFAULT_GLOW_BRIGHT))
    val glowBrightFlow: StateFlow<Float> = _glowBrightFlow
    private val _bloomStrengthFlow = MutableStateFlow(prefs.getFloat(KEY_BLOOM_STRENGTH, DEFAULT_BLOOM_STRENGTH))
    val bloomStrengthFlow: StateFlow<Float> = _bloomStrengthFlow
    private val _bloomRadiusFlow = MutableStateFlow(prefs.getFloat(KEY_BLOOM_RADIUS, DEFAULT_BLOOM_RADIUS))
    val bloomRadiusFlow: StateFlow<Float> = _bloomRadiusFlow
    private val _bloomThresholdFlow = MutableStateFlow(prefs.getFloat(KEY_BLOOM_THRESHOLD, DEFAULT_BLOOM_THRESHOLD))
    val bloomThresholdFlow: StateFlow<Float> = _bloomThresholdFlow
    private val _bassSensFlow = MutableStateFlow(prefs.getFloat(KEY_BASS_SENS, DEFAULT_BASS_SENS))
    val bassSensFlow: StateFlow<Float> = _bassSensFlow
    private val _midSensFlow = MutableStateFlow(prefs.getFloat(KEY_MID_SENS, DEFAULT_MID_SENS))
    val midSensFlow: StateFlow<Float> = _midSensFlow
    private val _trebleSensFlow = MutableStateFlow(prefs.getFloat(KEY_TREBLE_SENS, DEFAULT_TREBLE_SENS))
    val trebleSensFlow: StateFlow<Float> = _trebleSensFlow
    private val _customBgFlow = MutableStateFlow(prefs.getInt(KEY_CUSTOM_BG, DEFAULT_CUSTOM_BG))
    val customBgFlow: StateFlow<Int> = _customBgFlow
    private val _bgBrightnessFlow = MutableStateFlow(prefs.getFloat(KEY_BG_BRIGHTNESS, DEFAULT_BG_BRIGHTNESS))
    val bgBrightnessFlow: StateFlow<Float> = _bgBrightnessFlow
    private val _particleEdgeFlow = MutableStateFlow(loadParticleEdge())
    val particleEdgeFlow: StateFlow<Int> = _particleEdgeFlow
    private val _glowTierFlow = MutableStateFlow(prefs.getInt(KEY_GLOW_TIER, DEFAULT_GLOW_TIER))
    val glowTierFlow: StateFlow<Int> = _glowTierFlow
    private val _bloomStyleFlow = MutableStateFlow(prefs.getInt(KEY_BLOOM_STYLE, DEFAULT_BLOOM_STYLE))
    val bloomStyleFlow: StateFlow<Int> = _bloomStyleFlow
    private val _gyroSensitivityFlow = MutableStateFlow(prefs.getFloat(KEY_GYRO_SENSITIVITY, DEFAULT_GYRO_SENSITIVITY))
    val gyroSensitivityFlow: StateFlow<Float> = _gyroSensitivityFlow
    private val _gyroAmountFlow = MutableStateFlow(prefs.getFloat(KEY_GYRO_AMOUNT, DEFAULT_GYRO_AMOUNT))
    val gyroAmountFlow: StateFlow<Float> = _gyroAmountFlow
    private val _gyroReturnFlow = MutableStateFlow(prefs.getFloat(KEY_GYRO_RETURN, DEFAULT_GYRO_RETURN))
    val gyroReturnFlow: StateFlow<Float> = _gyroReturnFlow
    private val _gyroSmoothingFlow = MutableStateFlow(prefs.getFloat(KEY_GYRO_SMOOTHING, DEFAULT_GYRO_SMOOTHING))
    val gyroSmoothingFlow: StateFlow<Float> = _gyroSmoothingFlow
    private val _fpsLimitFlow = MutableStateFlow(prefs.getInt(KEY_FPS_LIMIT, DEFAULT_FPS_LIMIT))
    val fpsLimitFlow: StateFlow<Int> = _fpsLimitFlow
    private val _scatterModeFlow = MutableStateFlow(prefs.getInt(KEY_SCATTER_MODE, DEFAULT_SCATTER_MODE))
    val scatterModeFlow: StateFlow<Int> = _scatterModeFlow
    private val _scatterRadiusFlow = MutableStateFlow(prefs.getFloat(KEY_SCATTER_RADIUS, DEFAULT_SCATTER_RADIUS))
    val scatterRadiusFlow: StateFlow<Float> = _scatterRadiusFlow

    /** 旧版本迁移: 粒子总数 → 边长 (旧 3000 → 边长 55) */
    private fun loadParticleEdge(): Int {
        if (prefs.contains(KEY_PARTICLE_EDGE)) {
            return prefs.getInt(KEY_PARTICLE_EDGE, DEFAULT_PARTICLE_EDGE)
        }
        val old = prefs.getInt(KEY_PARTICLE_COUNT, -1)
        val edge = if (old > 0) kotlin.math.sqrt(old.toDouble()).roundToInt()
            .coerceIn(MIN_PARTICLE_EDGE, MAX_PARTICLE_EDGE)
        else DEFAULT_PARTICLE_EDGE
        prefs.edit { putInt(KEY_PARTICLE_EDGE, edge) }
        return edge
    }

    // ── Getters ──
    fun isThreeDEnabled() = _threeDEnabledFlow.value
    fun isGlassFadeExempt() = _glassFadeExemptFlow.value
    fun isGlowPulse() = _glowPulseFlow.value
    fun isBgFollowCover() = _bgFollowCoverFlow.value
    fun isGyroEnabled() = _gyroEnabledFlow.value
    fun getTransDuration() = _transDurationFlow.value
    fun getParticleSize() = _particleSizeFlow.value
    fun getMoveSpeed() = _moveSpeedFlow.value
    fun getPulseAmt() = _pulseAmtFlow.value
    fun getGlowStrength() = _glowStrengthFlow.value
    fun getGlowDark() = _glowDarkFlow.value
    fun getGlowBright() = _glowBrightFlow.value
    fun getBloomStrength() = _bloomStrengthFlow.value
    fun getBloomRadius() = _bloomRadiusFlow.value
    fun getBloomThreshold() = _bloomThresholdFlow.value
    fun getBassSens() = _bassSensFlow.value
    fun getMidSens() = _midSensFlow.value
    fun getTrebleSens() = _trebleSensFlow.value
    fun getCustomBg() = _customBgFlow.value
    fun getBgBrightness() = _bgBrightnessFlow.value
    fun getParticleEdge() = _particleEdgeFlow.value
    fun getGlowTier() = _glowTierFlow.value
    fun getBloomStyle() = _bloomStyleFlow.value
    fun getGyroSensitivity() = _gyroSensitivityFlow.value
    fun getGyroAmount() = _gyroAmountFlow.value
    fun getGyroReturn() = _gyroReturnFlow.value
    fun getGyroSmoothing() = _gyroSmoothingFlow.value
    fun getFpsLimit() = _fpsLimitFlow.value
    fun getScatterMode() = _scatterModeFlow.value
    fun getScatterRadius() = _scatterRadiusFlow.value

    // ── Setters ──
    fun setThreeDEnabled(v: Boolean) { _threeDEnabledFlow.value = v; prefs.edit { putBoolean(KEY_3D_ENABLED, v) } }
    fun setGlassFadeExempt(v: Boolean) { _glassFadeExemptFlow.value = v; prefs.edit { putBoolean(KEY_GLASS_FADE_EXEMPT, v) } }
    fun setGlowPulse(v: Boolean) { _glowPulseFlow.value = v; prefs.edit { putBoolean(KEY_GLOW_PULSE, v) } }
    fun setBgFollowCover(v: Boolean) { _bgFollowCoverFlow.value = v; prefs.edit { putBoolean(KEY_BG_FOLLOW_COVER, v) } }
    fun setGyroEnabled(v: Boolean) { _gyroEnabledFlow.value = v; prefs.edit { putBoolean(KEY_GYRO_ENABLED, v) } }
    fun setTransDuration(v: Float) { _transDurationFlow.value = v; prefs.edit { putFloat(KEY_TRANS_DURATION, v) } }
    fun setParticleSize(v: Float) { _particleSizeFlow.value = v; prefs.edit { putFloat(KEY_PARTICLE_SIZE, v) } }
    fun setMoveSpeed(v: Float) { _moveSpeedFlow.value = v; prefs.edit { putFloat(KEY_MOVE_SPEED, v) } }
    fun setPulseAmt(v: Float) { _pulseAmtFlow.value = v; prefs.edit { putFloat(KEY_PULSE_AMT, v) } }
    fun setGlowStrength(v: Float) { _glowStrengthFlow.value = v; prefs.edit { putFloat(KEY_GLOW_STRENGTH, v) } }
    fun setGlowDark(v: Float) { _glowDarkFlow.value = v; prefs.edit { putFloat(KEY_GLOW_DARK, v) } }
    fun setGlowBright(v: Float) { _glowBrightFlow.value = v; prefs.edit { putFloat(KEY_GLOW_BRIGHT, v) } }
    fun setBloomStrength(v: Float) { _bloomStrengthFlow.value = v; prefs.edit { putFloat(KEY_BLOOM_STRENGTH, v) } }
    fun setBloomRadius(v: Float) { _bloomRadiusFlow.value = v; prefs.edit { putFloat(KEY_BLOOM_RADIUS, v) } }
    fun setBloomThreshold(v: Float) { _bloomThresholdFlow.value = v; prefs.edit { putFloat(KEY_BLOOM_THRESHOLD, v) } }
    fun setBassSens(v: Float) { _bassSensFlow.value = v; prefs.edit { putFloat(KEY_BASS_SENS, v) } }
    fun setMidSens(v: Float) { _midSensFlow.value = v; prefs.edit { putFloat(KEY_MID_SENS, v) } }
    fun setTrebleSens(v: Float) { _trebleSensFlow.value = v; prefs.edit { putFloat(KEY_TREBLE_SENS, v) } }
    fun setCustomBg(v: Int) { _customBgFlow.value = v; prefs.edit { putInt(KEY_CUSTOM_BG, v) } }
    fun setBgBrightness(v: Float) { _bgBrightnessFlow.value = v.coerceIn(0.02f, 0.5f); prefs.edit { putFloat(KEY_BG_BRIGHTNESS, v) } }
    fun setParticleEdge(v: Int) { val e = v.coerceIn(MIN_PARTICLE_EDGE, MAX_PARTICLE_EDGE); _particleEdgeFlow.value = e; prefs.edit { putInt(KEY_PARTICLE_EDGE, e) } }
    fun setGlowTier(v: Int) { val t = v.coerceIn(0, 3); _glowTierFlow.value = t; prefs.edit { putInt(KEY_GLOW_TIER, t) } }
    fun setBloomStyle(v: Int) { val s = v.coerceIn(0, 3); _bloomStyleFlow.value = s; prefs.edit { putInt(KEY_BLOOM_STYLE, s) } }
    fun setGyroSensitivity(v: Float) { _gyroSensitivityFlow.value = v; prefs.edit { putFloat(KEY_GYRO_SENSITIVITY, v) } }
    fun setGyroAmount(v: Float) { _gyroAmountFlow.value = v; prefs.edit { putFloat(KEY_GYRO_AMOUNT, v) } }
    fun setGyroReturn(v: Float) { _gyroReturnFlow.value = v.coerceIn(0.9f, 0.999f); prefs.edit { putFloat(KEY_GYRO_RETURN, v) } }
    fun setGyroSmoothing(v: Float) { _gyroSmoothingFlow.value = v.coerceIn(0.02f, 0.8f); prefs.edit { putFloat(KEY_GYRO_SMOOTHING, v) } }
    fun setFpsLimit(v: Int) { _fpsLimitFlow.value = v; prefs.edit { putInt(KEY_FPS_LIMIT, v) } }
    fun setScatterMode(v: Int) { val m = v.coerceIn(0, 3); _scatterModeFlow.value = m; prefs.edit { putInt(KEY_SCATTER_MODE, m) } }
    fun setScatterRadius(v: Float) { _scatterRadiusFlow.value = v.coerceIn(MIN_SCATTER_RADIUS, MAX_SCATTER_RADIUS); prefs.edit { putFloat(KEY_SCATTER_RADIUS, v) } }
}
