package com.bicy.whitenoise.storage.config.ConfigStoragePart

/**
 * 用户对 AutoEQ 单个滤波器的覆盖编辑（增益 / 频率 / Q 值）。
 * Key 形如 "<preset>:<bandIndex>"（例如 "phone:3"），存于 AppConfig.autoEqFilterOverrides。
 * 当切换预设时，仅匹配当前预设的覆盖会生效；切换到其他预设不会带入旧覆盖。
 */
data class AutoEqFilterOverride(
    val gainDb: Float = 0.0f,
    val frequencyHz: Float = 1000.0f,
    val q: Float = 1.0f
)

data class AppConfig(
    val autoPlay: Boolean = true,
    val themeMode: String = "off",
    val themeColorId: String = "default",
    val dayThemeId: String = "default",
    val nightThemeId: String = "default_dark",
    val scheduledDefaultThemeId: String = "default",
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
    val autoEqProBrightnessTarget: Float = 0f,
    val autoEqProLoudnessTarget: Float = 0f,
    val autoEqProCouplingCoeff: Float = 0.3f,
    val autoEqProHysteresisDb: Float = 1f,
    val autoEqProDynamicQEnabled: Boolean = true,
    val autoEqBandCount: Int = 12,
    val autoEqLowRatio: Float = 0.33f,
    val autoEqMidRatio: Float = 0.34f,
    val autoEqSyncToManual: Boolean = false,
    val autoEqFilterOverrides: Map<String, AutoEqFilterOverride> = emptyMap(),
    val eqUnlimitedPoints: Boolean = false,
    val eqBypassEnabled: Boolean = false,
    val speakerPreset: String = "phone",
    val floatingPetEnabled: Boolean = false,
    val floatingPetId: String = "Bicy",
    val floatingPetScale: Float = 1.0f,
    val floatingPetAntiAlias: Boolean = false,
    val floatingPetHideDelay: Int = 5,
    val floatingPetAlpha: Float = 1.0f,
    val floatingPetHiddenAlpha: Float = 0.6f,
    val floatingPetWindowMode: Boolean = false,
    val aiBaseUrl: String = "",
    val aiApiKey: String = "",
    val aiModel: String = "",
    val aiMaxTokens: Int = 4096,
    val aiMaxToolCalls: Int = 15,
    val aiSystemPrompt: String = "",
    val aiEnabled: Boolean = false,
    val aiTemperature: Float = 0.7f,
    val aiEnableThinking: Boolean = false,
    val dataMigrated: Boolean = false,
    val completedTutorials: List<String> = emptyList(),
    val toastDurationSeconds: Int = 3
)

data class CustomColors(
    val accent: Int,
    val primary: Int,
    val background: Int,
    val text: Int
)
