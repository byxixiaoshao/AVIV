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
    val audioEffectOrder: List<String> = listOf("spatial", "reverb", "equalizer", "quality"),
    val vizWnEnabled: Boolean = true,
    val vizMusicEnabled: Boolean = true,
    val vizFlashEnabled: Boolean = true,
    val vizWnSensitivity: Int = 1,
    val vizMusicSensitivity: Int = 1,
    val vizFlashSensitivity: Int = 1,
    val vizRefreshRate: Int = 1,
    val mediaControlPriority: String = "smart",
    val autoEqEnabled: Boolean = false,
    val autoEqMode: String = "simple",
    val autoEqIntensity: Float = 0.5f,
    val autoEqBassBias: Float = 0f,
    val autoEqMidBias: Float = 0f,
    val autoEqTrebleBias: Float = 0f,
    val autoEqTargetCurve: String = "flat",
    val autoEqResponseSpeed: String = "medium",
    val autoEqProAttack: Float = 100f,
    val autoEqProRelease: Float = 200f,
    val autoEqProMaxSlope: Float = 10f,
    val autoEqProMaxBoost: Float = 12f,
    val autoEqProMaxCut: Float = 12f,
    val autoEqProSmoothing: Float = 0.7f,
    val autoEqProBrightnessTarget: Float = 0f,
    val autoEqProLoudnessTarget: Float = 0f,
    val autoEqProCouplingCoeff: Float = 0.3f,
    val autoEqProHysteresisDb: Float = 1f,
    val autoEqProDynamicQEnabled: Boolean = true,
    val autoEqSyncToManual: Boolean = false
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
                audioEffectOrder = audioEffectOrder,
                vizWnEnabled = json.optBoolean("vizWnEnabled", true),
                vizMusicEnabled = json.optBoolean("vizMusicEnabled", true),
                vizFlashEnabled = json.optBoolean("vizFlashEnabled", true),
                vizWnSensitivity = json.optInt("vizWnSensitivity", 1),
                vizMusicSensitivity = json.optInt("vizMusicSensitivity", 1),
                vizFlashSensitivity = json.optInt("vizFlashSensitivity", 1),
                vizRefreshRate = json.optInt("vizRefreshRate", 1),
                mediaControlPriority = json.optString("mediaControlPriority", "smart"),
                autoEqEnabled = json.optBoolean("autoEqEnabled", false),
                autoEqMode = json.optString("autoEqMode", "simple"),
                autoEqIntensity = json.optDouble("autoEqIntensity", 0.5).toFloat(),
                autoEqBassBias = json.optDouble("autoEqBassBias", 0.0).toFloat(),
                autoEqMidBias = json.optDouble("autoEqMidBias", 0.0).toFloat(),
                autoEqTrebleBias = json.optDouble("autoEqTrebleBias", 0.0).toFloat(),
                autoEqTargetCurve = json.optString("autoEqTargetCurve", "flat"),
                autoEqResponseSpeed = json.optString("autoEqResponseSpeed", "medium"),
                autoEqSyncToManual = json.optBoolean("autoEqSyncToManual", false)
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
            put("vizWnEnabled", config.vizWnEnabled)
            put("vizMusicEnabled", config.vizMusicEnabled)
            put("vizFlashEnabled", config.vizFlashEnabled)
            put("vizWnSensitivity", config.vizWnSensitivity)
            put("vizMusicSensitivity", config.vizMusicSensitivity)
            put("vizFlashSensitivity", config.vizFlashSensitivity)
            put("vizRefreshRate", config.vizRefreshRate)
            put("mediaControlPriority", config.mediaControlPriority)
            put("autoEqEnabled", config.autoEqEnabled)
            put("autoEqMode", config.autoEqMode)
            put("autoEqIntensity", config.autoEqIntensity.toDouble())
            put("autoEqBassBias", config.autoEqBassBias.toDouble())
            put("autoEqMidBias", config.autoEqMidBias.toDouble())
            put("autoEqTrebleBias", config.autoEqTrebleBias.toDouble())
            put("autoEqTargetCurve", config.autoEqTargetCurve)
            put("autoEqResponseSpeed", config.autoEqResponseSpeed)
            put("autoEqSyncToManual", config.autoEqSyncToManual)
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
    
    fun getVizWnEnabled(): Boolean = _config.value.vizWnEnabled
    
    fun setVizWnEnabled(enabled: Boolean) {
        updateConfig { it.copy(vizWnEnabled = enabled) }
    }
    
    fun getVizMusicEnabled(): Boolean = _config.value.vizMusicEnabled
    
    fun setVizMusicEnabled(enabled: Boolean) {
        updateConfig { it.copy(vizMusicEnabled = enabled) }
    }
    
    fun getVizFlashEnabled(): Boolean = _config.value.vizFlashEnabled
    
    fun setVizFlashEnabled(enabled: Boolean) {
        updateConfig { it.copy(vizFlashEnabled = enabled) }
    }
    
    fun getVizWnSensitivity(): Int = _config.value.vizWnSensitivity
    
    fun setVizWnSensitivity(value: Int) {
        updateConfig { it.copy(vizWnSensitivity = value) }
    }
    
    fun getVizMusicSensitivity(): Int = _config.value.vizMusicSensitivity
    
    fun setVizMusicSensitivity(value: Int) {
        updateConfig { it.copy(vizMusicSensitivity = value) }
    }
    
    fun getVizFlashSensitivity(): Int = _config.value.vizFlashSensitivity
    
    fun setVizFlashSensitivity(value: Int) {
        updateConfig { it.copy(vizFlashSensitivity = value) }
    }
    
    fun getVizRefreshRate(): Int = _config.value.vizRefreshRate
    
    fun setVizRefreshRate(value: Int) {
        updateConfig { it.copy(vizRefreshRate = value) }
    }
    
    fun getMediaControlPriority(): String = _config.value.mediaControlPriority
    
    fun setMediaControlPriority(priority: String) {
        updateConfig { it.copy(mediaControlPriority = priority) }
    }
    
    fun isAutoEqEnabled(): Boolean = _config.value.autoEqEnabled
    
    fun setAutoEqEnabled(enabled: Boolean) {
        updateConfig { it.copy(autoEqEnabled = enabled) }
    }
    
    fun getAutoEqMode(): String = _config.value.autoEqMode
    
    fun setAutoEqMode(mode: String) {
        updateConfig { it.copy(autoEqMode = mode) }
    }
    
    fun getAutoEqIntensity(): Float = _config.value.autoEqIntensity
    
    fun setAutoEqIntensity(intensity: Float) {
        updateConfig { it.copy(autoEqIntensity = intensity) }
    }
    
    fun getAutoEqBassBias(): Float = _config.value.autoEqBassBias
    
    fun setAutoEqBassBias(bias: Float) {
        updateConfig { it.copy(autoEqBassBias = bias) }
    }
    
    fun getAutoEqMidBias(): Float = _config.value.autoEqMidBias
    
    fun setAutoEqMidBias(bias: Float) {
        updateConfig { it.copy(autoEqMidBias = bias) }
    }
    
    fun getAutoEqTrebleBias(): Float = _config.value.autoEqTrebleBias
    
    fun setAutoEqTrebleBias(bias: Float) {
        updateConfig { it.copy(autoEqTrebleBias = bias) }
    }
    
    fun getAutoEqTargetCurve(): String = _config.value.autoEqTargetCurve
    
    fun setAutoEqTargetCurve(curve: String) {
        updateConfig { it.copy(autoEqTargetCurve = curve) }
    }
    
    fun getAutoEqResponseSpeed(): String = _config.value.autoEqResponseSpeed
    
    fun setAutoEqResponseSpeed(speed: String) {
        updateConfig { it.copy(autoEqResponseSpeed = speed) }
    }
    
    fun getAutoEqProAttack(): Float = _config.value.autoEqProAttack
    fun setAutoEqProAttack(value: Float) {
        updateConfig { it.copy(autoEqProAttack = value) }
    }
    
    fun getAutoEqProRelease(): Float = _config.value.autoEqProRelease
    fun setAutoEqProRelease(value: Float) {
        updateConfig { it.copy(autoEqProRelease = value) }
    }
    
    fun getAutoEqProMaxSlope(): Float = _config.value.autoEqProMaxSlope
    fun setAutoEqProMaxSlope(value: Float) {
        updateConfig { it.copy(autoEqProMaxSlope = value) }
    }
    
    fun getAutoEqProMaxBoost(): Float = _config.value.autoEqProMaxBoost
    fun setAutoEqProMaxBoost(value: Float) {
        updateConfig { it.copy(autoEqProMaxBoost = value) }
    }
    
    fun getAutoEqProMaxCut(): Float = _config.value.autoEqProMaxCut
    fun setAutoEqProMaxCut(value: Float) {
        updateConfig { it.copy(autoEqProMaxCut = value) }
    }
    
    fun getAutoEqProSmoothing(): Float = _config.value.autoEqProSmoothing
    fun setAutoEqProSmoothing(value: Float) {
        updateConfig { it.copy(autoEqProSmoothing = value) }
    }
    
    fun getAutoEqProBrightnessTarget(): Float = _config.value.autoEqProBrightnessTarget
    fun setAutoEqProBrightnessTarget(value: Float) {
        updateConfig { it.copy(autoEqProBrightnessTarget = value) }
    }
    
    fun getAutoEqProLoudnessTarget(): Float = _config.value.autoEqProLoudnessTarget
    fun setAutoEqProLoudnessTarget(value: Float) {
        updateConfig { it.copy(autoEqProLoudnessTarget = value) }
    }
    
    fun getAutoEqProCouplingCoeff(): Float = _config.value.autoEqProCouplingCoeff
    fun setAutoEqProCouplingCoeff(value: Float) {
        updateConfig { it.copy(autoEqProCouplingCoeff = value) }
    }
    
    fun getAutoEqProHysteresisDb(): Float = _config.value.autoEqProHysteresisDb
    fun setAutoEqProHysteresisDb(value: Float) {
        updateConfig { it.copy(autoEqProHysteresisDb = value) }
    }
    
    fun getAutoEqProDynamicQEnabled(): Boolean = _config.value.autoEqProDynamicQEnabled
    fun setAutoEqProDynamicQEnabled(enabled: Boolean) {
        updateConfig { it.copy(autoEqProDynamicQEnabled = enabled) }
    }
    
    fun isAutoEqSyncToManual(): Boolean = _config.value.autoEqSyncToManual
    fun setAutoEqSyncToManual(enabled: Boolean) {
        updateConfig { it.copy(autoEqSyncToManual = enabled) }
    }
    
    fun clearAllData() {
        _config.value = AppConfig()
        saveConfig()
        notifyListeners()
        Log.d(TAG, "All config data cleared")
    }
}
