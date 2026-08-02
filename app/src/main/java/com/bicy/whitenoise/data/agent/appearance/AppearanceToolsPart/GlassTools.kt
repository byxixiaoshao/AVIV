package com.bicy.whitenoise.data.agent.appearance.AppearanceToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.storage.config.GlassRenderConfig
import com.bicy.whitenoise.storage.config.LiquidGlassConfig
import com.bicy.whitenoise.ui.components.glass.GlassMode
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject

class SetLiquidGlassModeTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_liquid_glass_mode"
    override val description = "设置液态玻璃效果模式。OFF=关闭（标准半透明），COMPATIBLE=兼容模式（所有设备），PERFECT=完美模式（需 Android 13+）。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "mode" to ToolProperty("string", "液态玻璃模式", enum = listOf("OFF", "COMPATIBLE", "PERFECT"))
        ),
        required = listOf("mode")
    )

    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val modeStr = params.getString("mode")
        val mode = when (modeStr) {
            "OFF" -> GlassMode.OFF; "COMPATIBLE" -> GlassMode.COMPATIBLE; "PERFECT" -> GlassMode.PERFECT
            else -> return ToolResult.Error("未知模式：$modeStr")
        }
        val success = LiquidGlassConfig.setMode(mode)
        if (!success) return ToolResult.Error("当前设备不支持 PERFECT 模式（需 Android 13+），已保持原模式")
        val modeName = when (mode) { GlassMode.OFF -> "关闭"; GlassMode.COMPATIBLE -> "兼容"; GlassMode.PERFECT -> "完美" }
        return ToolResult.Success(
            message = "液态玻璃效果已设置为「$modeName」模式",
            operationType = "UPDATE", targetType = "liquid_glass", targetId = "nav", targetName = "liquid_glass_mode"
        )
    }
}

class SetGlassRenderParamTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_glass_render_param"
    override val description = "调整液态玻璃渲染参数。target=compatible 调整兼容模式参数（opacity/darkness/scale/shadow_enabled/shadow_strength/shadow_height），target=perfect 调整完美模式参数（blur/scale/distortion/darkness/warp/elevation/shadow_enabled/shadow_strength）。所有数值参数范围 0.0-1.0（shadow_height 除外，单位 dp）。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "target" to ToolProperty("string", "参数目标模式", enum = listOf("compatible", "perfect")),
            "opacity" to ToolProperty("number", "[compatible] 模拟模糊强度 0.0-1.0"),
            "darkness" to ToolProperty("number", "[compatible/perfect] 边缘暗度 0.0-1.0"),
            "scale" to ToolProperty("number", "[compatible/perfect] 缩放效果 0.0-1.0"),
            "shadow_enabled" to ToolProperty("boolean", "[compatible/perfect] 是否启用阴影"),
            "shadow_strength" to ToolProperty("number", "[compatible/perfect] 阴影强度 0.0-1.0"),
            "shadow_height" to ToolProperty("number", "[compatible] 阴影高度（dp）"),
            "blur" to ToolProperty("number", "[perfect] 模糊强度 0.0-1.0"),
            "distortion" to ToolProperty("number", "[perfect] 中心畸变 0.0-1.0"),
            "warp" to ToolProperty("number", "[perfect] 边缘扭曲 0.0-1.0"),
            "elevation" to ToolProperty("number", "[perfect] 镜头效果（dp）")
        ),
        required = listOf("target")
    )

    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val target = params.getString("target"); val updates = mutableListOf<String>()
        fun num(key: String): Float? = (params.opt(key) as? Number)?.toFloat()?.coerceIn(0f, 1f)
        fun bool(key: String): Boolean? = if (params.has(key)) params.getBoolean(key) else null
        when (target) {
            "compatible" -> {
                num("opacity")?.let { GlassRenderConfig.setCompatOpacity(it); updates.add("模糊=${"%.2f".format(it)}") }
                num("darkness")?.let { GlassRenderConfig.setCompatDarkness(it); updates.add("边缘暗度=${"%.2f".format(it)}") }
                num("scale")?.let { GlassRenderConfig.setCompatScale(it); updates.add("缩放=${"%.2f".format(it)}") }
                bool("shadow_enabled")?.let { GlassRenderConfig.setCompatShadowEnabled(it); updates.add("阴影=${if (it) "开" else "关"}") }
                num("shadow_strength")?.let { GlassRenderConfig.setCompatShadowStrength(it); updates.add("阴影强度=${"%.2f".format(it)}") }
                (params.opt("shadow_height") as? Number)?.toFloat()?.let { GlassRenderConfig.setCompatShadowHeight(it); updates.add("阴影高度=${it}dp") }
            }
            "perfect" -> {
                num("blur")?.let { GlassRenderConfig.setPerfBlur(it); updates.add("模糊=${"%.2f".format(it)}") }
                num("scale")?.let { GlassRenderConfig.setPerfScale(it); updates.add("镜头效果=${"%.2f".format(it)}") }
                num("distortion")?.let { GlassRenderConfig.setPerfDistortion(it); updates.add("中心畸变=${"%.2f".format(it)}") }
                num("darkness")?.let { GlassRenderConfig.setPerfDarkness(it); updates.add("边缘暗度=${"%.2f".format(it)}") }
                num("warp")?.let { GlassRenderConfig.setPerfWarp(it); updates.add("边缘扭曲=${"%.2f".format(it)}") }
                (params.opt("elevation") as? Number)?.toFloat()?.let { GlassRenderConfig.setPerfElevation(it); updates.add("镜头效果=${it}dp") }
                bool("shadow_enabled")?.let { GlassRenderConfig.setPerfShadowEnabled(it); updates.add("阴影=${if (it) "开" else "关"}") }
                num("shadow_strength")?.let { GlassRenderConfig.setPerfShadowStrength(it); updates.add("阴影强度=${"%.2f".format(it)}") }
            }
            else -> return ToolResult.Error("未知目标模式：$target")
        }
        if (updates.isEmpty()) return ToolResult.Error("未提供任何可调整的参数")
        return ToolResult.Success(
            message = "液态玻璃[$target]参数已更新：${updates.joinToString("，")}",
            operationType = "UPDATE", targetType = "glass_render", targetId = target, targetName = "glass_render_$target"
        )
    }
}
