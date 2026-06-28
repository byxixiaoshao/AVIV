package com.bicy.whitenoise.ui.theme

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object ThemeColorManager {
    
    private const val TAG = "ThemeColorManager"
    
    private lateinit var prefs: android.content.SharedPreferences
    
    private val _currentThemeColor = MutableStateFlow<ThemeColorScheme>(ThemeColorPresets.Default)
    val currentThemeColor: StateFlow<ThemeColorScheme> = _currentThemeColor.asStateFlow()
    
    private val _customColors = MutableStateFlow<CustomColors?>(null)
    val customColors: StateFlow<CustomColors?> = _customColors.asStateFlow()
    
    data class CustomColors(
        val accent: Int,
        val primary: Int,
        val background: Int,
        val text: Int
    )
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences("theme_settings", Context.MODE_PRIVATE)
        loadThemeColor()
    }
    
    suspend fun initAsync(context: Context) {
        withContext(Dispatchers.IO) {
            init(context)
        }
    }
    
    private fun loadThemeColor() {
        val config = com.bicy.whitenoise.storage.config.ConfigStorage.getConfig()
        
        Log.d(TAG, "loadThemeColor: themeColorId=${config.themeColorId}, customAccentColor=${config.customAccentColor}, customPrimaryColor=${config.customPrimaryColor}, customBackgroundColor=${config.customBackgroundColor}, customTextColor=${config.customTextColor}")
        
        if (config.themeColorId == "custom" && config.customAccentColor != -1 && config.customPrimaryColor != -1 && config.customBackgroundColor != -1 && config.customTextColor != -1) {
            Log.d(TAG, "Loading custom colors from config")
            _customColors.value = CustomColors(
                config.customAccentColor,
                config.customPrimaryColor,
                config.customBackgroundColor,
                config.customTextColor
            )
            _currentThemeColor.value = ThemeColorPresets.createCustomColorScheme(
                accent = androidx.compose.ui.graphics.Color(config.customAccentColor),
                primary = androidx.compose.ui.graphics.Color(config.customPrimaryColor),
                background = androidx.compose.ui.graphics.Color(config.customBackgroundColor),
                text = androidx.compose.ui.graphics.Color(config.customTextColor)
            )
            Log.d(TAG, "Custom theme loaded successfully")
        } else {
            val preset = ThemeColorPresets.getPresetById(config.themeColorId)
            Log.d(TAG, "Loading preset: ${preset.id} - ${preset.name}")
            _currentThemeColor.value = preset
        }
    }
    
    fun setThemeColor(colorId: String) {
        Log.d(TAG, "setThemeColor: colorId=$colorId")
        if (colorId == "custom") {
            val custom = _customColors.value
            if (custom != null) {
                com.bicy.whitenoise.storage.config.ConfigStorage.setThemeColor(colorId)
                _currentThemeColor.value = ThemeColorPresets.createCustomColorScheme(
                    accent = androidx.compose.ui.graphics.Color(custom.accent),
                    primary = androidx.compose.ui.graphics.Color(custom.primary),
                    background = androidx.compose.ui.graphics.Color(custom.background),
                    text = androidx.compose.ui.graphics.Color(custom.text)
                )
                Log.d(TAG, "Custom theme set: accent=${custom.accent}, primary=${custom.primary}")
            } else {
                Log.d(TAG, "No custom colors saved, using default custom colors")
                val defaultAccent = 0xFFB8A07A.toInt()
                val defaultPrimary = 0xFFB8A07A.toInt()
                val defaultBackground = 0xFFFAF6F0.toInt()
                val defaultText = 0xFF3D3A35.toInt()
                
                setCustomColors(defaultAccent, defaultPrimary, defaultBackground, defaultText)
            }
        } else {
            com.bicy.whitenoise.storage.config.ConfigStorage.setThemeColor(colorId)
            _currentThemeColor.value = ThemeColorPresets.getPresetById(colorId)
            Log.d(TAG, "Preset theme set: ${colorId}")
        }
    }
    
    fun setCustomColors(
        accent: Int,
        primary: Int,
        background: Int,
        text: Int
    ) {
        Log.d(TAG, "setCustomColors: accent=$accent, primary=$primary, background=$background, text=$text")
        com.bicy.whitenoise.storage.config.ConfigStorage.setCustomColors(accent, primary, background, text)
        
        _customColors.value = CustomColors(accent, primary, background, text)
        _currentThemeColor.value = ThemeColorPresets.createCustomColorScheme(
            accent = androidx.compose.ui.graphics.Color(accent),
            primary = androidx.compose.ui.graphics.Color(primary),
            background = androidx.compose.ui.graphics.Color(background),
            text = androidx.compose.ui.graphics.Color(text)
        )
        Log.d(TAG, "Custom colors saved and applied")
    }
    
    fun getCurrentThemeColor(): ThemeColorScheme = _currentThemeColor.value
    
    fun getThemeColorDisplayName(colorId: String): String {
        return if (colorId == "custom") {
            "自定义"
        } else {
            ThemeColorPresets.getPresetById(colorId).name
        }
    }
    
    fun getCurrentColorId(): String {
        return com.bicy.whitenoise.storage.config.ConfigStorage.getThemeColorId()
    }
    
    fun getCustomColors(): CustomColors? = _customColors.value
}
