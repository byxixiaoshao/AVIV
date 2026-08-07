package com.bicy.whitenoise.data.agent.system.SystemToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject

class ToggleVisualizationTool(private val vm: MainViewModel) : AgentTool {
    override val name = "toggle_visualization"
    override val description = "开关音频可视化效果。可控制白噪音可视化、音乐可视化、闪光可视化三组开关。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "viz_type" to ToolProperty("string", "可视化类型", enum = listOf("white_noise", "music", "flash")),
            "enabled" to ToolProperty("boolean", "是否启用")
        ),
        required = listOf("viz_type", "enabled")
    )

    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val vizType = params.getString("viz_type")
        val enabled = params.getBoolean("enabled")
        val name = when (vizType) {
            "white_noise" -> { ConfigStorage.setVizWnEnabled(enabled); "白噪音可视化" }
            "music" -> { ConfigStorage.setVizMusicEnabled(enabled); "音乐可视化" }
            "flash" -> { ConfigStorage.setVizFlashEnabled(enabled); "闪光可视化" }
            else -> return ToolResult.Error("未知可视化类型：$vizType")
        }
        return ToolResult.Success(
            message = "$name 已${if (enabled) "启用" else "关闭"}",
            operationType = "UPDATE", targetType = "visualization", targetId = vizType, targetName = name
        )
    }
}

class SetVizSensitivityTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_viz_sensitivity"
    override val description = "调整可视化灵敏度。level 为 0(低)/1(中)/2(高)。可同时设置白噪音、音乐、闪光三组灵敏度。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "white_noise" to ToolProperty("number", "白噪音灵敏度 0-2"),
            "music" to ToolProperty("number", "音乐灵敏度 0-2"),
            "flash" to ToolProperty("number", "闪光灵敏度 0-2")
        )
    )

    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val updates = mutableListOf<String>()
        if (params.has("white_noise")) {
            val v = params.getInt("white_noise").coerceIn(0, 2)
            ConfigStorage.setVizWnSensitivity(v.toFloat()); updates.add("白噪音=$v")
        }
        if (params.has("music")) {
            val v = params.getInt("music").coerceIn(0, 2)
            ConfigStorage.setVizMusicSensitivity(v.toFloat()); updates.add("音乐=$v")
        }
        if (params.has("flash")) {
            val v = params.getInt("flash").coerceIn(0, 2)
            ConfigStorage.setVizFlashSensitivity(v.toFloat()); updates.add("闪光=$v")
        }
        if (updates.isEmpty()) return ToolResult.Error("未提供任何灵敏度参数")
        return ToolResult.Success(
            message = "可视化灵敏度已更新：${updates.joinToString("，")}",
            operationType = "UPDATE", targetType = "viz_sensitivity", targetId = "sensitivity", targetName = "viz_sensitivity"
        )
    }
}
