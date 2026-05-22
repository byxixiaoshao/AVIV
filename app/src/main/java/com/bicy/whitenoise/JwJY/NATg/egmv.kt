package com.bicy.whitenoise.JwJY.NATg

import android.util.Log
import com.bicy.whitenoise.JwJY.Jauc.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

data class AppConfig(
    val isPremium: Boolean = false,
    val premiumPurchaseDate: Long? = null,
    val premiumOrderId: String? = null,
    val autoPlay: Boolean = true,
    val themeMode: String = "follow_system",
    val themeColorId: String = "default",
    val customAccentColor: Int = -1,
    val customPrimaryColor: Int = -1,
    val customBackgroundColor: Int = -1,
    val customTextColor: Int = -1,
    val language: String = "zh_CN",
    val logEnabled: Boolean = true,
    val audioEffectOrder: List<String> = listOf("spatial", "reverb", "equalizer", "quality")
)

data class CustomColors(
    val accent: Int,
    val primary: Int,
    val background: Int,
    val text: Int
)

object ConfigStorage {
    
    private const val TAG = "ConfigStorage"
    private const val CONFIG_FILE = "config.json"
    
    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()
    
    private val listeners = CopyOnWriteArrayList<WeakReference<() -> Unit>>()
    
    fun init() {
        loadConfig()
        Log.d(TAG, "ConfigStorage initialized")
    }
    
    private fun loadConfig() {
        val file = StorageManager.getFile("config", CONFIG_FILE) ?: return
        val json = StorageManager.loadJson(file)
        
        if (json != null) {
            val orderArray = json.optJSONArray("audioEffectOrder")
            val audioEffectOrder = if (orderArray != null) {
                (0 until orderArray.length()).map { orderArray.getString(it) }
            } else {
                listOf("spatial", "reverb", "equalizer", "quality")
            }
            
            _config.value = AppConfig(
                isPremium = json.optBoolean("isPremium", false),
                premiumPurchaseDate = if (json.has("premiumPurchaseDate")) json.optLong("premiumPurchaseDate") else null,
                premiumOrderId = json.optString("premiumOrderId").takeIf { it.isNotEmpty() },
                autoPlay = json.optBoolean("autoPlay", true),
                themeMode = json.optString("themeMode", "follow_system"),
                themeColorId = json.optString("themeColorId", "default"),
                customAccentColor = json.optInt("customAccentColor", -1),
                customPrimaryColor = json.optInt("customPrimaryColor", -1),
                customBackgroundColor = json.optInt("customBackgroundColor", -1),
                customTextColor = json.optInt("customTextColor", -1),
                language = json.optString("language", "zh_CN"),
                logEnabled = json.optBoolean("logEnabled", false),
                audioEffectOrder = audioEffectOrder
            )
            Log.d(TAG, "Config loaded: isPremium=${_config.value.isPremium}")
        }
    }
    
    private fun saveConfig() {
        val file = StorageManager.getFile("config", CONFIG_FILE) ?: return
        val config = _config.value
        
        val json = JSONObject().apply {
            put("isPremium", config.isPremium)
            config.premiumPurchaseDate?.let { put("premiumPurchaseDate", it) }
            config.premiumOrderId?.let { put("premiumOrderId", it) }
            put("autoPlay", config.autoPlay)
            put("themeMode", config.themeMode)
            put("themeColorId", config.themeColorId)
            put("customAccentColor", config.customAccentColor)
            put("customPrimaryColor", config.customPrimaryColor)
            put("customBackgroundColor", config.customBackgroundColor)
            put("customTextColor", config.customTextColor)
            put("language", config.language)
            put("logEnabled", config.logEnabled)
            put("audioEffectOrder", JSONArray(config.audioEffectOrder))
        }
        
        StorageManager.saveJsonSync(file, json)
    }
    
    private fun updateConfig(update: (AppConfig) -> AppConfig) {
        _config.value = update(_config.value)
        saveConfig()
        notifyListeners()
    }
    
    fun addListener(listener: () -> Unit) {
        listeners.add(WeakReference(listener))
    }
    
    fun removeListener(listener: () -> Unit) {
        listeners.removeAll { it.get() == listener }
    }
    
    private fun notifyListeners() {
        listeners.removeAll { it.get() == null }
        listeners.forEach { it.get()?.invoke() }
    }
    
    fun getConfig(): AppConfig = _config.value
    
    fun isPremiumUser(): Boolean = _config.value.isPremium
    
    fun setPremiumUser(isPremium: Boolean, orderId: String? = null) {
        updateConfig { it.copy(
            isPremium = isPremium,
            premiumPurchaseDate = if (isPremium) System.currentTimeMillis() else null,
            premiumOrderId = orderId
        )}
        Log.d(TAG, "Premium user set to: $isPremium")
    }
    
    fun getAutoPlay(): Boolean = _config.value.autoPlay
    
    fun setAutoPlay(enabled: Boolean) {
        updateConfig { it.copy(autoPlay = enabled) }
    }
    
    fun getThemeMode(): String = _config.value.themeMode
    
    fun setThemeMode(mode: String) {
        updateConfig { it.copy(themeMode = mode) }
    }
    
    fun getThemeColorId(): String = _config.value.themeColorId
    
    fun setThemeColor(colorId: String) {
        updateConfig { it.copy(themeColorId = colorId) }
    }
    
    fun getCustomColors(): CustomColors {
        val config = _config.value
        return CustomColors(
            accent = config.customAccentColor,
            primary = config.customPrimaryColor,
            background = config.customBackgroundColor,
            text = config.customTextColor
        )
    }
    
    fun setCustomColors(accent: Int, primary: Int, background: Int, text: Int) {
        updateConfig { it.copy(
            themeColorId = "custom",
            customAccentColor = accent,
            customPrimaryColor = primary,
            customBackgroundColor = background,
            customTextColor = text
        )}
    }
    
    fun getLanguage(): String = _config.value.language
    
    fun setLanguage(language: String) {
        updateConfig { it.copy(language = language) }
    }
    
    fun isLogEnabled(): Boolean = _config.value.logEnabled
    
    fun setLogEnabled(enabled: Boolean) {
        updateConfig { it.copy(logEnabled = enabled) }
    }
    
    fun getAudioEffectOrder(): List<String> = _config.value.audioEffectOrder
    
    fun setAudioEffectOrder(order: List<String>) {
        updateConfig { it.copy(audioEffectOrder = order) }
    }
    
    fun clearAllData() {
        _config.value = AppConfig()
        saveConfig()
        notifyListeners()
        Log.d(TAG, "All config data cleared")
    }
}
