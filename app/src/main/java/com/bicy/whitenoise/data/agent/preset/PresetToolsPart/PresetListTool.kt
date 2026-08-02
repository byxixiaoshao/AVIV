package com.bicy.whitenoise.data.agent.preset.PresetToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.storage.whitenoise.PresetManager
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject

class ListPresetsTool(private val vm: MainViewModel) : AgentTool {
    override val name = "list_presets"; override val description = "列出所有已保存的白噪音预设。"
    override val parameters = ToolParameters(properties = emptyMap())
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        PresetManager.load(); val presets = PresetManager.presets.value
        if (presets.isEmpty()) return ToolResult.Success("暂无已保存的预设")
        val sb = StringBuilder("已保存的预设列表：\n")
        presets.forEachIndexed { i, p -> sb.append("${i + 1}. 「${p.name}」（ID: ${p.id}）包含 ${p.sounds.size} 个声音\n") }
        return ToolResult.Success(sb.toString(), mapOf("count" to presets.size))
    }
}

class DeletePresetTool(private val vm: MainViewModel) : AgentTool {
    override val name = "delete_preset"; override val description = "删除指定的预设。"
    override val parameters = ToolParameters(
        properties = mapOf("preset_id" to ToolProperty("string", "要删除的预设ID")),
        required = listOf("preset_id")
    )
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val id = params.getString("preset_id"); PresetManager.load()
        val preset = PresetManager.get(id) ?: return ToolResult.Error("预设不存在：$id")
        PresetManager.delete(id)
        return ToolResult.Success("已删除预设「${preset.name}」", operationType = "DELETE",
            targetType = "preset", targetId = id, targetName = preset.name)
    }
}
