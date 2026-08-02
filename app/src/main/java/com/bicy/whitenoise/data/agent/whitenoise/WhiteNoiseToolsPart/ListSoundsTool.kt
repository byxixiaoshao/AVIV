package com.bicy.whitenoise.data.agent.whitenoise.WhiteNoiseToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject

class ListSoundsTool(private val vm: MainViewModel) : AgentTool {
    override val name = "list_sounds"; override val description = "列出所有可用的白噪音分类与声音。用户可以从中选择想播放的声音。"
    override val parameters = ToolParameters(properties = emptyMap())
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val categories = vm.categories.value
        if(categories.isEmpty()) return ToolResult.Success("暂无可用声音分类")
        val sb=StringBuilder("可用白噪音分类与声音：\n")
        categories.forEachIndexed{i,cat->sb.append("${i+1}. 分类：${cat.category.name}（共 ${cat.sounds.size} 个声音）\n");cat.sounds.take(8).forEach{s->sb.append("   - ID: ${s.id}, 名称: ${s.name}\n")};if(cat.sounds.size>8)sb.append("   ...（还有 ${cat.sounds.size-8} 个）\n")}
        return ToolResult.Success(sb.toString(), mapOf("categoryCount" to categories.size))
    }
}
