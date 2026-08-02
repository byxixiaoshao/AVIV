package com.bicy.whitenoise.storage.config

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.bicy.whitenoise.ui.components.glass.GlassMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 液态玻璃效果配置管理类
 * 支持三档模式：关闭(OFF)、兼容(COMPATIBLE)、完美(PERFECT)
 */
object LiquidGlassConfig {

    private const val PREFS_NAME = "liquid_glass_config"
    private const val KEY_MODE = "glass_mode"

    private val _modeFlow = MutableStateFlow(GlassMode.COMPATIBLE)
    val modeFlow: StateFlow<GlassMode> = _modeFlow.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 初始化状态，从 SharedPreferences 加载当前值
     */
    fun initialize(context: Context) {
        val modeOrdinal = getPrefs(context).getInt(KEY_MODE, GlassMode.COMPATIBLE.ordinal)
        val savedMode = GlassMode.entries.getOrElse(modeOrdinal) { GlassMode.COMPATIBLE }
        // 如果保存的是完美模式但不支持，自动降级为兼容模式
        _modeFlow.value = if (savedMode == GlassMode.PERFECT && !isPerfectModeSupported()) {
            GlassMode.COMPATIBLE
        } else {
            savedMode
        }
    }

    /**
     * 检查完美模式是否支持（需要 Android 13+，API 33）
     */
    fun isPerfectModeSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * 获取当前模式（同步方法）
     */
    fun getMode(context: Context): GlassMode {
        val modeOrdinal = getPrefs(context).getInt(KEY_MODE, GlassMode.COMPATIBLE.ordinal)
        val savedMode = GlassMode.entries.getOrElse(modeOrdinal) { GlassMode.COMPATIBLE }
        return if (savedMode == GlassMode.PERFECT && !isPerfectModeSupported()) {
            GlassMode.COMPATIBLE
        } else {
            savedMode
        }
    }

    /**
     * 设置模式
     * @param mode 目标模式
     * @return 是否设置成功（完美模式在不支持的设备上会失败）
     */
    fun setMode(mode: GlassMode): Boolean {
        // 完美模式在不支持的设备上不允许设置
        if (mode == GlassMode.PERFECT && !isPerfectModeSupported()) {
            return false
        }

        val context = com.bicy.whitenoise.utils.AppInitializer.getContext()
        getPrefs(context).edit().putInt(KEY_MODE, mode.ordinal).apply()
        _modeFlow.value = mode
        return true
    }

    /**
     * 获取可用的模式列表
     */
    fun getAvailableModes(): List<GlassMode> {
        return if (isPerfectModeSupported()) {
            GlassMode.entries
        } else {
            GlassMode.entries.filter { it != GlassMode.PERFECT }
        }
    }
}