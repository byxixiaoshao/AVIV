package com.bicy.whitenoise.ui.theme

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.storage.theme.CustomThemeLibrary
import com.bicy.whitenoise.storage.theme.ThemeScheduleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.bicy.whitenoise.ui.theme.ThemeColorsPart.*

object ThemeColorManager {
    
    private const val TAG = "ThemeColorManager"
    
    private lateinit var context: Context
    
    private val _currentThemeColor = MutableStateFlow<ThemeColorScheme>(ThemeColorPresets.Default)
    val currentThemeColor: StateFlow<ThemeColorScheme> = _currentThemeColor.asStateFlow()
    
    private val _customColors = MutableStateFlow<CustomColors?>(null)
    val customColors: StateFlow<CustomColors?> = _customColors.asStateFlow()
    
    private val _currentThemeMode = MutableStateFlow<ThemeMode>(ThemeMode.OFF)
    val currentThemeMode: StateFlow<ThemeMode> = _currentThemeMode.asStateFlow()
    
    data class CustomColors(
        val accent: Int,
        val primary: Int,
        val background: Int,
        val text: Int
    )
    
    fun init(context: Context) {
        this.context = context
        
        // 初始化自定义主题库
        CustomThemeLibrary.init(context)
        
        // 迁移旧自定义主题色
        migrateOldCustomColors()
        
        // 初始化定时任务管理器
        ThemeScheduleManager.init(context)
        
        // 加载当前主题
        loadThemeColor()
        
        Log.d(TAG, "ThemeColorManager initialized")
    }
    
    suspend fun initAsync(context: Context) {
        withContext(Dispatchers.IO) {
            init(context)
        }
    }
    
    /**
     * 加载主题色
     */
    private fun loadThemeColor() {
        val config = ConfigStorage.getConfig()
        
        Log.d(TAG, "loadThemeColor: themeMode=${config.themeMode}")
        
        // 加载自定义颜色
        if (config.customAccentColor != -1 && config.customPrimaryColor != -1 && 
            config.customBackgroundColor != -1 && config.customTextColor != -1) {
            _customColors.value = CustomColors(
                config.customAccentColor,
                config.customPrimaryColor,
                config.customBackgroundColor,
                config.customTextColor
            )
        }
        
        // 根据主题模式加载主题
        val themeMode = ThemeMode.fromValue(config.themeMode)
        _currentThemeMode.value = themeMode
        
        val themeScheme = getThemeSchemeByMode(themeMode)
        _currentThemeColor.value = themeScheme
        
        Log.d(TAG, "Theme loaded: mode=${themeMode.displayName}, theme=${themeScheme.name}")
    }
    
    /**
     * 根据主题模式获取主题方案
     */
    private fun getThemeSchemeByMode(mode: ThemeMode): ThemeColorScheme {
        return when (mode) {
            ThemeMode.OFF -> {
                // 关闭模式:使用 themeColorId
                getThemeById(ConfigStorage.getThemeColorId())
            }
            ThemeMode.FOLLOW_SYSTEM -> {
                // 跟随系统模式:根据系统暗色模式选择日间或夜间主题
                val isSystemDark = isSystemDarkMode()
                val themeId = if (isSystemDark) {
                    ConfigStorage.getNightThemeId()
                } else {
                    ConfigStorage.getDayThemeId()
                }
                getThemeById(themeId)
            }
            ThemeMode.SCHEDULED -> {
                // 定时模式:根据当前时间匹配任务,如果没有匹配则使用默认主题
                val scheduledThemeId = ThemeScheduleManager.getCurrentThemeId()
                if (scheduledThemeId != null) {
                    getThemeById(scheduledThemeId)
                } else {
                    getThemeById(ConfigStorage.getScheduledDefaultThemeId())
                }
            }
        }
    }
    
    /**
     * 通过 ID 获取主题方案
     */
    private fun getThemeById(themeId: String): ThemeColorScheme {
        // 先检查是否为自定义主题
        if (themeId == "custom" && _customColors.value != null) {
            val c = _customColors.value!!
            return ThemeColorPresets.createCustomColorScheme(
                accent = androidx.compose.ui.graphics.Color(c.accent),
                primary = androidx.compose.ui.graphics.Color(c.primary),
                background = androidx.compose.ui.graphics.Color(c.background),
                text = androidx.compose.ui.graphics.Color(c.text)
            )
        }
        
        // 查找预设主题
        val preset = ThemeColorPresets.getPresetById(themeId)
        if (preset.id == themeId) {
            return preset
        }
        
        // 查找自定义主题库中的主题
        val customTheme = CustomThemeLibrary.getThemeById(themeId)
        if (customTheme != null) {
            return customTheme.toThemeColorScheme()
        }
        
        // 返回默认主题
        return ThemeColorPresets.Default
    }
    
    /**
     * 检查系统是否为暗色模式
     */
    private fun isSystemDarkMode(): Boolean {
        val currentMode = context.resources.configuration.uiMode and 
                         android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
    
    /**
     * 设置主题模式
     */
    fun setThemeMode(mode: ThemeMode) {
        Log.d(TAG, "setThemeMode: mode=${mode.displayName}")
        ConfigStorage.setThemeMode(mode.value)
        _currentThemeMode.value = mode
        
        // 根据新模式更新主题
        val themeScheme = getThemeSchemeByMode(mode)
        _currentThemeColor.value = themeScheme
        
        Log.d(TAG, "Theme mode updated: ${mode.displayName}")
    }
    
    /**
     * 设置关闭模式下的主题
     */
    fun setThemeColor(colorId: String) {
        Log.d(TAG, "setThemeColor: colorId=$colorId")
        ConfigStorage.setThemeColor(colorId)
        
        // 如果当前是关闭模式,立即更新主题
        if (_currentThemeMode.value == ThemeMode.OFF) {
            val themeScheme = getThemeById(colorId)
            _currentThemeColor.value = themeScheme
        }
    }
    
    /**
     * 设置日间主题 ID
     */
    fun setDayThemeId(themeId: String) {
        Log.d(TAG, "setDayThemeId: themeId=$themeId")
        ConfigStorage.setDayThemeId(themeId)
        
        // 如果当前是跟随系统模式且系统不是暗色模式,立即更新主题
        if (_currentThemeMode.value == ThemeMode.FOLLOW_SYSTEM && !isSystemDarkMode()) {
            val themeScheme = getThemeById(themeId)
            _currentThemeColor.value = themeScheme
        }
    }
    
    /**
     * 设置夜间主题 ID
     */
    fun setNightThemeId(themeId: String) {
        Log.d(TAG, "setNightThemeId: themeId=$themeId")
        ConfigStorage.setNightThemeId(themeId)
        
        // 如果当前是跟随系统模式且系统是暗色模式,立即更新主题
        if (_currentThemeMode.value == ThemeMode.FOLLOW_SYSTEM && isSystemDarkMode()) {
            val themeScheme = getThemeById(themeId)
            _currentThemeColor.value = themeScheme
        }
    }
    
    /**
     * 设置定时模式默认主题 ID
     */
    fun setScheduledDefaultThemeId(themeId: String) {
        Log.d(TAG, "setScheduledDefaultThemeId: themeId=$themeId")
        ConfigStorage.setScheduledDefaultThemeId(themeId)
        
        // 如果当前是定时模式且没有匹配的任务,立即更新主题
        if (_currentThemeMode.value == ThemeMode.SCHEDULED && 
            ThemeScheduleManager.getCurrentThemeId() == null) {
            val themeScheme = getThemeById(themeId)
            _currentThemeColor.value = themeScheme
        }
    }
    
    /**
     * 设置自定义颜色
     */
    fun setCustomColors(
        accent: Int,
        primary: Int,
        background: Int,
        text: Int
    ) {
        Log.d(TAG, "setCustomColors: accent=$accent, primary=$primary, background=$background, text=$text")
        ConfigStorage.setCustomColors(accent, primary, background, text)
        
        _customColors.value = CustomColors(accent, primary, background, text)
        
        // 如果当前主题是自定义主题,立即更新
        val currentId = getCurrentColorId()
        if (currentId == "custom") {
            _currentThemeColor.value = ThemeColorPresets.createCustomColorScheme(
                accent = androidx.compose.ui.graphics.Color(accent),
                primary = androidx.compose.ui.graphics.Color(primary),
                background = androidx.compose.ui.graphics.Color(background),
                text = androidx.compose.ui.graphics.Color(text)
            )
        }
        
        Log.d(TAG, "Custom colors saved and applied")
    }
    
    /**
     * 刷新当前主题(用于定时模式或跟随系统模式)
     */
    fun refreshCurrentTheme() {
        val themeScheme = getThemeSchemeByMode(_currentThemeMode.value)
        if (themeScheme.id != _currentThemeColor.value.id) {
            _currentThemeColor.value = themeScheme
            Log.d(TAG, "Theme refreshed: ${themeScheme.name}")
        }
    }
    
    /**
     * 获取当前主题色
     */
    fun getCurrentThemeColor(): ThemeColorScheme = _currentThemeColor.value
    
    /**
     * 获取当前主题模式
     */
    fun getCurrentThemeMode(): ThemeMode = _currentThemeMode.value
    
    /**
     * 获取主题显示名称
     */
    fun getThemeDisplayName(colorId: String): String {
        return if (colorId == "custom") {
            "自定义"
        } else {
            CustomThemeLibrary.getThemeName(colorId)
        }
    }
    
    /**
     * 获取当前主题 ID
     */
    fun getCurrentColorId(): String {
        return when (_currentThemeMode.value) {
            ThemeMode.OFF -> ConfigStorage.getThemeColorId()
            ThemeMode.FOLLOW_SYSTEM -> {
                if (isSystemDarkMode()) ConfigStorage.getNightThemeId()
                else ConfigStorage.getDayThemeId()
            }
            ThemeMode.SCHEDULED -> {
                ThemeScheduleManager.getCurrentThemeId() ?: ConfigStorage.getScheduledDefaultThemeId()
            }
        }
    }
    
    /**
     * 获取自定义颜色
     */
    fun getCustomColors(): CustomColors? = _customColors.value
    
    /**
     * 获取所有主题(预设 + 自定义)
     */
    fun getAllThemes(): List<ThemeColorScheme> {
        return CustomThemeLibrary.getAllThemesIncludingPresets()
    }
    
    /**
     * 迁移旧自定义主题色到主题库
     * 检查 ConfigStorage 中的旧字段，如果有值就创建一个自定义主题并保存到 CustomThemeLibrary
     * 然后清除这些旧字段
     */
    private fun migrateOldCustomColors() {
        val config = ConfigStorage.getConfig()
        
        // 检查是否有旧的自定义颜色
        if (config.customAccentColor != -1 && config.customPrimaryColor != -1 &&
            config.customBackgroundColor != -1 && config.customTextColor != -1) {
            
            Log.d(TAG, "Migrating old custom colors: accent=${config.customAccentColor}, primary=${config.customPrimaryColor}")
            
            // 创建自定义主题
            val customTheme = com.bicy.whitenoise.ui.theme.CustomTheme(
                id = "migrated_custom_${System.currentTimeMillis()}",
                name = "我的主题",
                accent = config.customAccentColor,
                primary = config.customPrimaryColor,
                background = config.customBackgroundColor,
                text = config.customTextColor,
                createdAt = System.currentTimeMillis()
            )
            
            // 保存到自定义主题库
            CustomThemeLibrary.addTheme(customTheme)
            
            // 如果当前主题是旧的自定义主题，切换到迁移后的主题
            if (ConfigStorage.getThemeColorId() == "custom") {
                ConfigStorage.setThemeColor(customTheme.id)
            }
            
            // 清除旧的自定义颜色字段
            ConfigStorage.clearOldCustomColors()
            
            Log.d(TAG, "Old custom colors migrated successfully")
        } else {
            Log.d(TAG, "No old custom colors to migrate")
        }
    }
}
