package com.bicy.whitenoise.storage.config

import android.util.Log
import com.bicy.whitenoise.utils.AppInitializer
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.lang.ref.WeakReference
import com.bicy.whitenoise.storage.config.ConfigStoragePart.*
import java.util.concurrent.CopyOnWriteArrayList

object ConfigStorage {
    
    private const val TAG = "ConfigStorage"
    private const val CONFIG_FILE = "app_config.json"
    private val gson = Gson()
    
    private lateinit var configFile: File
    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()
    
    private val listeners = CopyOnWriteArrayList<WeakReference<() -> Unit>>()
    
    fun init() {
        configFile = File(AppInitializer.getContext().filesDir, "json_storage/$CONFIG_FILE")
        configFile.parentFile?.mkdirs()
        loadConfig()
        Log.d(TAG, "ConfigStorage initialized")
    }
    
    private fun loadConfig() {
        try {
            if (configFile.exists()) {
                val config = gson.fromJson(configFile.readText(), AppConfig::class.java)
                if (config != null) {
                    _config.value = config
                    Log.d(TAG, "Config loaded from JSON")
                    return
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load config", e)
        }
        Log.d(TAG, "No saved config, using defaults")
    }
    
    private fun saveConfig() {
        try {
            configFile.writeText(gson.toJson(_config.value))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save config", e)
        }
        Log.d(TAG, "Config saved")
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
    
    fun getDayThemeId(): String = _config.value.dayThemeId
    
    fun setDayThemeId(themeId: String) {
        updateConfig { it.copy(dayThemeId = themeId) }
    }
    
    fun getNightThemeId(): String = _config.value.nightThemeId
    
    fun setNightThemeId(themeId: String) {
        updateConfig { it.copy(nightThemeId = themeId) }
    }
    
    fun getScheduledDefaultThemeId(): String = _config.value.scheduledDefaultThemeId
    
    fun setScheduledDefaultThemeId(themeId: String) {
        updateConfig { it.copy(scheduledDefaultThemeId = themeId) }
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

    fun clearOldCustomColors() {
        updateConfig { it.copy(
            customAccentColor = -1,
            customPrimaryColor = -1,
            customBackgroundColor = -1,
            customTextColor = -1
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

    // === 灵敏度 0..1 连续 ===
    fun getVizWnSensitivity(): Float = _config.value.vizWnSensitivity
    fun setVizWnSensitivity(value: Float) {
        updateConfig { it.copy(vizWnSensitivity = value.coerceIn(0f, 1f)) }
    }
    fun getVizMusicSensitivity(): Float = _config.value.vizMusicSensitivity
    fun setVizMusicSensitivity(value: Float) {
        updateConfig { it.copy(vizMusicSensitivity = value.coerceIn(0f, 1f)) }
    }
    fun getVizFlashSensitivity(): Float = _config.value.vizFlashSensitivity
    fun setVizFlashSensitivity(value: Float) {
        updateConfig { it.copy(vizFlashSensitivity = value.coerceIn(0f, 1f)) }
    }

    // === 降落速度 0..1（闪烁为暗淡速度） ===
    fun getVizWnFallSpeed(): Float = _config.value.vizWnFallSpeed
    fun setVizWnFallSpeed(value: Float) {
        updateConfig { it.copy(vizWnFallSpeed = value.coerceIn(0f, 1f)) }
    }
    fun getVizMusicFallSpeed(): Float = _config.value.vizMusicFallSpeed
    fun setVizMusicFallSpeed(value: Float) {
        updateConfig { it.copy(vizMusicFallSpeed = value.coerceIn(0f, 1f)) }
    }
    fun getVizFlashFallSpeed(): Float = _config.value.vizFlashFallSpeed
    fun setVizFlashFallSpeed(value: Float) {
        updateConfig { it.copy(vizFlashFallSpeed = value.coerceIn(0f, 1f)) }
    }

    // === 响应频段范围（per-viz） ===
    fun getVizWnMinBand(): Int = _config.value.vizWnMinBand
    fun setVizWnMinBand(value: Int) {
        updateConfig { it.copy(vizWnMinBand = value.coerceIn(0, 15)) }
    }
    fun getVizWnMaxBand(): Int = _config.value.vizWnMaxBand
    fun setVizWnMaxBand(value: Int) {
        updateConfig { it.copy(vizWnMaxBand = value.coerceIn(0, 15)) }
    }
    fun getVizMusicMinFreq(): Float = _config.value.vizMusicMinFreq
    fun setVizMusicMinFreq(value: Float) {
        updateConfig { it.copy(vizMusicMinFreq = value.coerceIn(20f, 20000f)) }
    }
    fun getVizMusicMaxFreq(): Float = _config.value.vizMusicMaxFreq
    fun setVizMusicMaxFreq(value: Float) {
        updateConfig { it.copy(vizMusicMaxFreq = value.coerceIn(20f, 20000f)) }
    }
    fun getVizFlashMinBand(): Int = _config.value.vizFlashMinBand
    fun setVizFlashMinBand(value: Int) {
        updateConfig { it.copy(vizFlashMinBand = value.coerceIn(0, 15)) }
    }
    fun getVizFlashMaxBand(): Int = _config.value.vizFlashMaxBand
    fun setVizFlashMaxBand(value: Int) {
        updateConfig { it.copy(vizFlashMaxBand = value.coerceIn(0, 15)) }
    }

    // === 柱形数量 8..64 ===
    fun getVizWnBarCount(): Int = _config.value.vizWnBarCount
    fun setVizWnBarCount(value: Int) {
        updateConfig { it.copy(vizWnBarCount = value.coerceIn(8, 64)) }
    }
    fun getVizMusicBarCount(): Int = _config.value.vizMusicBarCount
    fun setVizMusicBarCount(value: Int) {
        updateConfig { it.copy(vizMusicBarCount = value.coerceIn(8, 64)) }
    }
    fun getVizFlashBarCount(): Int = _config.value.vizFlashBarCount
    fun setVizFlashBarCount(value: Int) {
        updateConfig { it.copy(vizFlashBarCount = value.coerceIn(8, 64)) }
    }

    // === 闪烁最低暗度 / 最高明度 0..1 ===
    fun getVizFlashMinDarkness(): Float = _config.value.vizFlashMinDarkness
    fun setVizFlashMinDarkness(value: Float) {
        updateConfig { it.copy(vizFlashMinDarkness = value.coerceIn(0f, 1f)) }
    }
    fun getVizFlashMaxBrightness(): Float = _config.value.vizFlashMaxBrightness
    fun setVizFlashMaxBrightness(value: Float) {
        updateConfig { it.copy(vizFlashMaxBrightness = value.coerceIn(0f, 1f)) }
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
    
    fun getAutoEqBandCount(): Int = _config.value.autoEqBandCount
    fun setAutoEqBandCount(value: Int) {
        updateConfig { it.copy(autoEqBandCount = value) }
    }
    
    fun getAutoEqLowRatio(): Float = _config.value.autoEqLowRatio
    fun setAutoEqLowRatio(value: Float) {
        updateConfig { it.copy(autoEqLowRatio = value) }
    }
    
    fun getAutoEqMidRatio(): Float = _config.value.autoEqMidRatio
    fun setAutoEqMidRatio(value: Float) {
        updateConfig { it.copy(autoEqMidRatio = value) }
    }
    
    fun isAutoEqSyncToManual(): Boolean = _config.value.autoEqSyncToManual
    fun setAutoEqSyncToManual(enabled: Boolean) {
        updateConfig { it.copy(autoEqSyncToManual = enabled) }
    }

    // --- Per-filter overrides for AutoEQ ---
    // Key convention: "<preset>:<bandIndex>" (e.g. "phone:3")
    fun getAutoEqFilterOverrides(): Map<String, AutoEqFilterOverride> =
        _config.value.autoEqFilterOverrides

    fun getAutoEqFilterOverridesFor(preset: String): Map<Int, AutoEqFilterOverride> {
        val prefix = "$preset:"
        return _config.value.autoEqFilterOverrides
            .filterKeys { it.startsWith(prefix) }
            .mapKeys { it.key.removePrefix(prefix).toIntOrNull() ?: -1 }
            .filterKeys { it >= 0 }
    }

    fun setAutoEqFilterOverride(preset: String, bandIndex: Int, override: AutoEqFilterOverride) {
        updateConfig {
            val key = "$preset:$bandIndex"
            it.copy(autoEqFilterOverrides = it.autoEqFilterOverrides + (key to override))
        }
    }

    fun clearAutoEqFilterOverride(preset: String, bandIndex: Int) {
        updateConfig {
            val key = "$preset:$bandIndex"
            it.copy(autoEqFilterOverrides = it.autoEqFilterOverrides - key)
        }
    }

    fun clearAutoEqFilterOverridesFor(preset: String) {
        updateConfig {
            val prefix = "$preset:"
            it.copy(autoEqFilterOverrides = it.autoEqFilterOverrides.filterKeys { !it.startsWith(prefix) })
        }
    }

    fun isEqUnlimitedPoints(): Boolean = _config.value.eqUnlimitedPoints
    fun setEqUnlimitedPoints(enabled: Boolean) {
        updateConfig { it.copy(eqUnlimitedPoints = enabled) }
    }

    fun isEqBypassEnabled(): Boolean = _config.value.eqBypassEnabled
    fun setEqBypassEnabled(enabled: Boolean) {
        updateConfig { it.copy(eqBypassEnabled = enabled) }
    }

    fun getSpeakerPreset(): String = _config.value.speakerPreset
    fun setSpeakerPreset(preset: String) {
        updateConfig { it.copy(speakerPreset = preset) }
    }
    
    fun isFloatingPetEnabled(): Boolean = _config.value.floatingPetEnabled
    fun setFloatingPetEnabled(enabled: Boolean) {
        updateConfig { it.copy(floatingPetEnabled = enabled) }
    }
    
    fun getFloatingPetId(): String = _config.value.floatingPetId
    fun setFloatingPetId(id: String) {
        updateConfig { it.copy(floatingPetId = id) }
    }
    
    fun getFloatingPetScale(): Float = _config.value.floatingPetScale
    fun setFloatingPetScale(scale: Float) {
        updateConfig { it.copy(floatingPetScale = scale) }
    }
    
    fun isFloatingPetAntiAlias(): Boolean = _config.value.floatingPetAntiAlias
    fun setFloatingPetAntiAlias(enabled: Boolean) {
        updateConfig { it.copy(floatingPetAntiAlias = enabled) }
    }
    
    fun getFloatingPetHideDelay(): Int = _config.value.floatingPetHideDelay
    fun setFloatingPetHideDelay(delay: Int) {
        updateConfig { it.copy(floatingPetHideDelay = delay.coerceIn(3, 10)) }
    }
    
    fun getFloatingPetAlpha(): Float = _config.value.floatingPetAlpha
    fun setFloatingPetAlpha(alpha: Float) {
        updateConfig { it.copy(floatingPetAlpha = alpha.coerceIn(0.1f, 1.0f)) }
    }
    
    fun getFloatingPetHiddenAlpha(): Float = _config.value.floatingPetHiddenAlpha
    fun setFloatingPetHiddenAlpha(alpha: Float) {
        updateConfig { it.copy(floatingPetHiddenAlpha = alpha.coerceIn(0.1f, 1.0f)) }
    }
    
    fun isFloatingPetWindowMode(): Boolean = _config.value.floatingPetWindowMode
    fun setFloatingPetWindowMode(enabled: Boolean) {
        updateConfig { it.copy(floatingPetWindowMode = enabled) }
    }
    
    fun getAiBaseUrl(): String = _config.value.aiBaseUrl
    fun setAiBaseUrl(url: String) {
        updateConfig { it.copy(aiBaseUrl = url) }
    }
    
    fun getAiApiKey(): String = _config.value.aiApiKey
    fun setAiApiKey(key: String) {
        updateConfig { it.copy(aiApiKey = key) }
    }
    
    fun getAiModel(): String = _config.value.aiModel
    fun setAiModel(model: String) {
        updateConfig { it.copy(aiModel = model) }
    }
    
    fun getAiMaxTokens(): Int = _config.value.aiMaxTokens
    fun setAiMaxTokens(tokens: Int) {
        updateConfig { it.copy(aiMaxTokens = tokens.coerceIn(1024, 65536)) }
    }
    
    fun getAiMaxToolCalls(): Int = _config.value.aiMaxToolCalls
    fun setAiMaxToolCalls(calls: Int) {
        updateConfig { it.copy(aiMaxToolCalls = calls.coerceIn(10, 50)) }
    }
    
    fun getAiSystemPrompt(): String = _config.value.aiSystemPrompt
    fun setAiSystemPrompt(prompt: String) {
        updateConfig { it.copy(aiSystemPrompt = prompt) }
    }

    fun isAiEnabled(): Boolean = _config.value.aiEnabled
    fun setAiEnabled(enabled: Boolean) {
        updateConfig { it.copy(aiEnabled = enabled) }
    }

    fun getAiTemperature(): Float = _config.value.aiTemperature
    fun setAiTemperature(value: Float) {
        updateConfig { it.copy(aiTemperature = value.coerceIn(0f, 2f)) }
    }

    fun isAiEnableThinking(): Boolean = _config.value.aiEnableThinking
    fun setAiEnableThinking(enabled: Boolean) {
        updateConfig { it.copy(aiEnableThinking = enabled) }
    }
    
    fun getCompletedTutorials(): List<String> = _config.value.completedTutorials
    fun setCompletedTutorials(tutorials: List<String>) {
        updateConfig { it.copy(completedTutorials = tutorials) }
    }
    fun addCompletedTutorial(tutorialId: String) {
        updateConfig { it.copy(completedTutorials = it.completedTutorials + tutorialId) }
    }
    fun clearCompletedTutorials() {
        updateConfig { it.copy(completedTutorials = emptyList()) }
    }

    fun getToastDurationSeconds(): Int = _config.value.toastDurationSeconds

    fun setToastDurationSeconds(seconds: Int) {
        updateConfig { it.copy(toastDurationSeconds = seconds.coerceIn(3, 10)) }
    }
    
    fun clearAllData() {
        _config.value = AppConfig()
        saveConfig()
        notifyListeners()
        Log.d(TAG, "All config data cleared")
    }
}
