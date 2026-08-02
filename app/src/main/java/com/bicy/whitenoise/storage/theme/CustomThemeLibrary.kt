package com.bicy.whitenoise.storage.theme

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.ui.theme.CustomTheme
import com.bicy.whitenoise.ui.theme.ThemeColorsPart.ThemeColorScheme
import com.bicy.whitenoise.utils.AppInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 自定义主题库管理器
 * 负责管理用户创建的自定义主题
 */
object CustomThemeLibrary {
    
    private const val TAG = "CustomThemeLibrary"
    private const val FILE_NAME = "custom_themes.json"
    
    private lateinit var context: Context
    private lateinit var file: File
    
    private val _themes = MutableStateFlow<List<CustomTheme>>(emptyList())
    val themes: StateFlow<List<CustomTheme>> = _themes.asStateFlow()
    
    /**
     * 初始化
     */
    fun init(context: Context) {
        this.context = context
        file = File(context.filesDir, FILE_NAME)
        loadThemes()
        Log.d(TAG, "CustomThemeLibrary initialized with ${_themes.value.size} themes")
    }
    
    /**
     * 异步初始化
     */
    suspend fun initAsync(context: Context) {
        withContext(Dispatchers.IO) {
            init(context)
        }
    }
    
    /**
     * 加载自定义主题
     */
    private fun loadThemes() {
        try {
            if (!file.exists()) {
                Log.d(TAG, "Custom themes file not exists, creating empty list")
                _themes.value = emptyList()
                saveThemes()
                return
            }
            
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val themeList = mutableListOf<CustomTheme>()
            
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                val theme = CustomTheme(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    accent = json.getInt("accent"),
                    primary = json.getInt("primary"),
                    background = json.getInt("background"),
                    text = json.getInt("text"),
                    createdAt = json.getLong("createdAt")
                )
                themeList.add(theme)
            }
            
            _themes.value = themeList
            Log.d(TAG, "Loaded ${themeList.size} custom themes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom themes", e)
            _themes.value = emptyList()
        }
    }
    
    /**
     * 保存自定义主题
     */
    private fun saveThemes() {
        try {
            val jsonArray = JSONArray()
            _themes.value.forEach { theme ->
                val json = JSONObject()
                json.put("id", theme.id)
                json.put("name", theme.name)
                json.put("accent", theme.accent)
                json.put("primary", theme.primary)
                json.put("background", theme.background)
                json.put("text", theme.text)
                json.put("createdAt", theme.createdAt)
                jsonArray.put(json)
            }
            
            file.writeText(jsonArray.toString())
            Log.d(TAG, "Saved ${_themes.value.size} custom themes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom themes", e)
        }
    }
    
    /**
     * 获取所有自定义主题
     */
    fun getAllThemes(): List<CustomTheme> = _themes.value
    
    /**
     * 通过 ID 获取自定义主题
     */
    fun getThemeById(id: String): CustomTheme? {
        return _themes.value.find { it.id == id }
    }
    
    /**
     * 添加自定义主题
     */
    fun addTheme(theme: CustomTheme) {
        val currentList = _themes.value.toMutableList()
        
        // 检查是否已存在同名主题
        if (currentList.any { it.name == theme.name }) {
            Log.w(TAG, "Theme with name '${theme.name}' already exists")
            return
        }
        
        currentList.add(theme)
        _themes.value = currentList
        saveThemes()
        Log.d(TAG, "Added custom theme: ${theme.name}")
    }
    
    /**
     * 更新自定义主题
     */
    fun updateTheme(theme: CustomTheme) {
        val currentList = _themes.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == theme.id }
        
        if (index != -1) {
            currentList[index] = theme
            _themes.value = currentList
            saveThemes()
            Log.d(TAG, "Updated custom theme: ${theme.name}")
        } else {
            Log.w(TAG, "Theme with id '${theme.id}' not found")
        }
    }
    
    /**
     * 删除自定义主题
     */
    fun deleteTheme(id: String) {
        val currentList = _themes.value.toMutableList()
        val removed = currentList.removeIf { it.id == id }
        
        if (removed) {
            _themes.value = currentList
            saveThemes()
            Log.d(TAG, "Deleted custom theme: $id")
        } else {
            Log.w(TAG, "Theme with id '$id' not found for deletion")
        }
    }
    
    /**
     * 检查主题名称是否已存在
     */
    fun isThemeNameExists(name: String, excludeId: String? = null): Boolean {
        return _themes.value.any { it.name == name && it.id != excludeId }
    }
    
    /**
     * 获取所有主题(预设 + 自定义)
     */
    fun getAllThemesIncludingPresets(): List<ThemeColorScheme> {
        val presets = com.bicy.whitenoise.ui.theme.ThemeColorsPart.ThemeColorPresets.allPresets
        val customs = _themes.value.map { it.toThemeColorScheme() }
        return presets + customs
    }
    
    /**
     * 通过 ID 获取主题(包括预设和自定义)
     */
    fun getThemeByIdIncludingPresets(id: String): ThemeColorScheme? {
        // 先查找预设
        val preset = com.bicy.whitenoise.ui.theme.ThemeColorsPart.ThemeColorPresets.getPresetById(id)
        if (preset.id == id) {
            return preset
        }
        
        // 再查找自定义
        val custom = getThemeById(id)
        return custom?.toThemeColorScheme()
    }
    
    /**
     * 获取主题名称
     */
    fun getThemeName(id: String): String {
        val theme = getThemeByIdIncludingPresets(id)
        return theme?.name ?: "未知主题"
    }
    
    /**
     * 检查是否为自定义主题
     */
    fun isCustomTheme(id: String): Boolean {
        return _themes.value.any { it.id == id }
    }
    
    /**
     * 清空所有自定义主题
     */
    fun clearAllThemes() {
        _themes.value = emptyList()
        saveThemes()
        Log.d(TAG, "Cleared all custom themes")
    }
}