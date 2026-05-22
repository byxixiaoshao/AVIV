package com.bicy.whitenoise.yODW.nU5N

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _autoPlayEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_PLAY, true))
    val autoPlayEnabled: StateFlow<Boolean> = _autoPlayEnabled.asStateFlow()
    
    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_AUTO_PLAY = "auto_play"
    }
    
    fun setAutoPlay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY, enabled).apply()
        _autoPlayEnabled.value = enabled
    }
}
