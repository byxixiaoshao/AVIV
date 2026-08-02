package com.bicy.whitenoise.data.agent.appearance.AppearanceToolsPart

import android.graphics.Color
import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject

class SetCustomThemeColorTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_custom_theme_color"
    override val description = "设置自定义主题颜色。颜色使用十六进制格式（如 #FF5733 或 #88FF5733 带透明度）。设置后将自动切换到自定义主题。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "accent" to ToolProperty("string", "强调色（十六进制，如 #FF5733）"),
            "primary" to ToolProperty("string", "主色（十六进制）"),
            "background" to ToolProperty("string", "背景色（十六进制）"),
            "text" to ToolProperty("string", "文字色（十六进制）")
        ),
        required = listOf("accent")
    )

    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val current = ConfigStorage.getCustomColors()
        val accent = parseColor(params.optString("accent", ""), current.accent)
            ?: return ToolResult.Error("无效的强调色格式，请使用十六进制（如 #FF5733）")
        val primary = parseColor(params.optString("primary", ""), current.primary)
            ?: return ToolResult.Error("无效的主色格式")
        val background = parseColor(params.optString("background", ""), current.background)
            ?: return ToolResult.Error("无效的背景色格式")
        val text = parseColor(params.optString("text", ""), current.text)
            ?: return ToolResult.Error("无效的文字色格式")
        ConfigStorage.setCustomColors(accent, primary, background, text)
        return ToolResult.Success(
            message = "自定义主题色已设置并应用",
            operationType = "UPDATE", targetType = "theme_color", targetId = "custom", targetName = "custom_theme"
        )
    }

    private fun parseColor(hex: String, default: Int): Int? {
        if (hex.isBlank()) return default
        return try { Color.parseColor(hex) } catch (e: Exception) { null }
    }
}
