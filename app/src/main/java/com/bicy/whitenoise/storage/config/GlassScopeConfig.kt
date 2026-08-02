package com.bicy.whitenoise.storage.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bicy.whitenoise.WhiteNoiseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 液态玻璃作用范围配置（单选项）。
 * 底部导航栏 / 顶部栏 / 全部
 */
object GlassScopeConfig {

    /** 仅底部导航栏 */
    const val SCOPE_BOTTOM_NAV = "bottom_nav"
    /** 仅顶部栏 */
    const val SCOPE_TOP_BAR = "top_bar"
    /** 底部 + 顶部 */
    const val SCOPE_ALL = "all"

    val ALL_SCOPES = listOf(
        ScopeItem(SCOPE_BOTTOM_NAV, "glass_scope_bottom_nav"),
        ScopeItem(SCOPE_TOP_BAR, "glass_scope_top_bar"),
        ScopeItem(SCOPE_ALL, "glass_scope_all"),
    )

    data class ScopeItem(
        val key: String,
        val labelResId: String,
    )

    private val prefs: SharedPreferences by lazy {
        WhiteNoiseApp.context.getSharedPreferences("glass_scope_config", Context.MODE_PRIVATE)
    }

    private val _flow = MutableStateFlow(
        prefs.getString("scope", SCOPE_ALL) ?: SCOPE_ALL
    )

    val scopeFlow: StateFlow<String> = _flow

    fun setScope(scope: String) {
        _flow.value = scope
        prefs.edit { putString("scope", scope) }
    }

    /** 判断当前作用范围是否覆盖底部导航栏 */
    fun isBottomNavEnabled(): Boolean {
        return _flow.value == SCOPE_BOTTOM_NAV || _flow.value == SCOPE_ALL
    }

    /** 判断当前作用范围是否覆盖顶部栏 */
    fun isTopBarEnabled(): Boolean {
        return _flow.value == SCOPE_TOP_BAR || _flow.value == SCOPE_ALL
    }
}
