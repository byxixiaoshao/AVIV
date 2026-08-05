package com.bicy.whitenoise.data.agent.AgentToolPart

import org.json.JSONObject

interface AgentTool {
    val name: String
    val description: String
    val parameters: ToolParameters

    // 任务A: 是否为读取类工具（仅查询，不修改配置）
    // true = 免确认弹窗直接执行；false = 修改类，confirmMode 开启时需弹窗确认
    // 默认 false（修改类），读取类工具覆盖为 true 或在 AgentService.readOnlyToolNames 中声明
    val isReadOnly: Boolean get() = false

    suspend fun execute(params: JSONObject, context: ToolContext): ToolResult
}
