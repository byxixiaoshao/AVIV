package com.bicy.whitenoise.data.agent.AgentToolPart

data class ToolProperty(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)
