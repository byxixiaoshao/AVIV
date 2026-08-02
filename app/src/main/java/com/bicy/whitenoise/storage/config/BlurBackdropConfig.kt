package com.bicy.whitenoise.storage.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bicy.whitenoise.WhiteNoiseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BlurBackdropConfig {
    private const val KEY_ENABLED = "blur_backdrop_enabled"
    private const val KEY_BLUR = "blur_backdrop_blur"
    private const val KEY_DARKNESS = "blur_backdrop_darkness"

    private const val DEFAULT_ENABLED = false
    private const val DEFAULT_BLUR = 0.5f
    private const val DEFAULT_DARKNESS = 0.3f

    private val prefs: SharedPreferences by lazy {
        WhiteNoiseApp.context.getSharedPreferences("blur_backdrop_config", Context.MODE_PRIVATE)
    }

    private val _enabledFlow = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED))
    val enabledFlow: StateFlow<Boolean> = _enabledFlow

    private val _blurFlow = MutableStateFlow(prefs.getFloat(KEY_BLUR, DEFAULT_BLUR))
    val blurFlow: StateFlow<Float> = _blurFlow

    private val _darknessFlow = MutableStateFlow(prefs.getFloat(KEY_DARKNESS, DEFAULT_DARKNESS))
    val darknessFlow: StateFlow<Float> = _darknessFlow

    fun setEnabled(value: Boolean) { _enabledFlow.value = value; prefs.edit { putBoolean(KEY_ENABLED, value) } }
    fun setBlur(value: Float) { _blurFlow.value = value; prefs.edit { putFloat(KEY_BLUR, value) } }
    fun setDarkness(value: Float) { _darknessFlow.value = value; prefs.edit { putFloat(KEY_DARKNESS, value) } }
}
