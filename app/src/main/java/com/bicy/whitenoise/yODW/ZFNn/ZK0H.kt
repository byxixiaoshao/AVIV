package com.bicy.whitenoise.yODW.ZFNn

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Deprecated("Use ConfigStorage instead")
object ThemeColorManager {
    
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
        val config = com.bicy.whitenoise.JwJY.NATg.ConfigStorage.getConfig()
        
        if (config.themeColorId == "custom" && config.customAccentColor != -1) {
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
        } else {
            _currentThemeColor.value = ThemeColorPresets.getPresetById(config.themeColorId)
        }
    }
    
    fun setThemeColor(colorId: String) {
        if (colorId == "custom") {
            val custom = _customColors.value
            if (custom != null) {
                com.bicy.whitenoise.JwJY.NATg.ConfigStorage.setThemeColor(colorId)
                _currentThemeColor.value = ThemeColorPresets.createCustomColorScheme(
                    accent = androidx.compose.ui.graphics.Color(custom.accent),
                    primary = androidx.compose.ui.graphics.Color(custom.primary),
                    background = androidx.compose.ui.graphics.Color(custom.background),
                    text = androidx.compose.ui.graphics.Color(custom.text)
                )
            } else {
                val defaultAccent = 0xFFB8A07A.toInt()
                val defaultPrimary = 0xFFB8A07A.toInt()
                val defaultBackground = 0xFFFAF6F0.toInt()
                val defaultText = 0xFF3D3A35.toInt()
                
                setCustomColors(defaultAccent, defaultPrimary, defaultBackground, defaultText)
            }
        } else {
            com.bicy.whitenoise.JwJY.NATg.ConfigStorage.setThemeColor(colorId)
            _currentThemeColor.value = ThemeColorPresets.getPresetById(colorId)
        }
    }
    
    fun setCustomColors(
        accent: Int,
        primary: Int,
        background: Int,
        text: Int
    ) {
        com.bicy.whitenoise.JwJY.NATg.ConfigStorage.setCustomColors(accent, primary, background, text)
        
        _customColors.value = CustomColors(accent, primary, background, text)
        _currentThemeColor.value = ThemeColorPresets.createCustomColorScheme(
            accent = androidx.compose.ui.graphics.Color(accent),
            primary = androidx.compose.ui.graphics.Color(primary),
            background = androidx.compose.ui.graphics.Color(background),
            text = androidx.compose.ui.graphics.Color(text)
        )
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
        return com.bicy.whitenoise.JwJY.NATg.ConfigStorage.getThemeColorId()
    }
    
    fun getCustomColors(): CustomColors? = _customColors.value
}
