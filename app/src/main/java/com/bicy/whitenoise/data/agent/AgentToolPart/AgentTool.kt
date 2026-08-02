package com.bicy.whitenoise.data.agent.AgentToolPart

import org.json.JSONObject

interface AgentTool {
    val name: String
    val description: String
    val parameters: ToolParameters

    suspend fun execute(params: JSONObject, context: ToolContext): ToolResult
}
