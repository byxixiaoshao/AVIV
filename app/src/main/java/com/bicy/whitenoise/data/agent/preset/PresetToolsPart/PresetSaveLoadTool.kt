package com.bicy.whitenoise.data.agent.preset.PresetToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.storage.whitenoise.PresetManager
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject

class SavePresetTool(private val vm: MainViewModel) : AgentTool {
    override val name = "save_preset"
    override val description = "将当前播放列表保存为预设。"
    override val parameters = ToolParameters(
        properties = mapOf("name" to ToolProperty("string", "预设名称")),
        required = listOf("name")
    )
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val name = params.getString("name")
        val sounds = vm.getCurrentSoundConfigs()
        if (sounds.isEmpty()) return ToolResult.Error("当前播放列表为空，无法保存预设")
        val preset = PresetManager.save(name, sounds)
        return ToolResult.Success("已保存预设「$name」（包含 ${sounds.size} 个声音）",
            operationType = "CREATE", targetType = "preset", targetId = preset.id, targetName = name)
    }
}

class LoadPresetTool(private val vm: MainViewModel) : AgentTool {
    override val name = "load_preset"
    override val description = "加载指定的预设到播放列表。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "preset_id" to ToolProperty("string", "预设ID（可通过 list_presets 获取）"),
            "preset_name" to ToolProperty("string", "预设名称（用于日志显示）")
        ),
        required = listOf("preset_id")
    )
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val id = params.getString("preset_id"); PresetManager.load()
        val preset = PresetManager.get(id) ?: return ToolResult.Error("预设不存在：$id")
        vm.loadPresetSounds(preset.sounds) {}
        return ToolResult.Success("已加载预设「${preset.name}」（包含 ${preset.sounds.size} 个声音）",
            operationType = "LOAD", targetType = "preset", targetId = id, targetName = preset.name)
    }
}
