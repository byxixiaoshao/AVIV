package com.bicy.whitenoise.data.agent.appearance.AppearanceToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.storage.config.NavBackgroundConfig
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject

class SetBackgroundImageTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_background_image"
    override val description = "管理导航栏背景图片。可清除背景图片或调整背景透明度。注意：设置新背景图片需用户在设置页面手动选择，AI 无法直接指定图片文件。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "action" to ToolProperty("string", "操作类型", enum = listOf("clear", "set_alpha")),
            "alpha" to ToolProperty("number", "背景透明度 0.1-1.0（仅 action=set_alpha 时使用）")
        ),
        required = listOf("action")
    )

    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        when (params.getString("action")) {
            "clear" -> {
                NavBackgroundConfig.clearBackgroundUri()
                return ToolResult.Success("导航栏背景图片已清除", operationType = "UPDATE",
                    targetType = "background_image", targetId = "nav", targetName = "nav_background")
            }
            "set_alpha" -> {
                val alpha = (params.opt("alpha") as? Number)?.toFloat() ?: 0.85f
                NavBackgroundConfig.setBackgroundAlpha(alpha)
                return ToolResult.Success("导航栏背景透明度已设置为 ${(alpha * 100).toInt()}%",
                    operationType = "UPDATE", targetType = "background_alpha", targetId = "nav", targetName = "nav_background")
            }
            else -> return ToolResult.Error("未知操作：${params.getString("action")}")
        }
    }
}
