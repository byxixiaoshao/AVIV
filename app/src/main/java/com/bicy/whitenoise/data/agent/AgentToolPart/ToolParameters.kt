package com.bicy.whitenoise.data.agent.AgentToolPart

data class ToolParameters(
    val type: String = "object",
    val properties: Map<String, ToolProperty>,
    val required: List<String> = emptyList()
)
