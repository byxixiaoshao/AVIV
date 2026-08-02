package com.bicy.whitenoise.storage.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bicy.whitenoise.WhiteNoiseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 毛玻璃作用范围配置（多选项）。
 */
object FrostedGlassScopeConfig {

    const val SCOPE_SECTIONS = "sections"
    const val SCOPE_CARDS = "cards"
    const val SCOPE_DIALOGS = "dialogs"
    const val SCOPE_NOTIFICATIONS = "notifications"

    val ALL_SCOPES = listOf(
        ScopeItem(SCOPE_SECTIONS, "frosted_scope_sections", false),
        ScopeItem(SCOPE_CARDS, "frosted_scope_cards", false),
        ScopeItem(SCOPE_DIALOGS, "frosted_scope_dialogs", false),
        ScopeItem(SCOPE_NOTIFICATIONS, "frosted_scope_notifications", false),
    )

    data class ScopeItem(
        val key: String,
        val labelResId: String,
        val enabled: Boolean,
    )

    private val prefs: SharedPreferences by lazy {
        WhiteNoiseApp.context.getSharedPreferences("frosted_scope_config", Context.MODE_PRIVATE)
    }

    private val _flows = mutableMapOf<String, MutableStateFlow<Boolean>>()

    fun flow(key: String): StateFlow<Boolean> {
        return _flows.getOrPut(key) {
            MutableStateFlow(prefs.getBoolean("scope_$key", ALL_SCOPES.find { it.key == key }?.enabled ?: false))
        }
    }

    fun setEnabled(key: String, enabled: Boolean) {
        _flows[key]?.value = enabled
        prefs.edit { putBoolean("scope_$key", enabled) }
    }
}
