package com.bicy.whitenoise.data.ai.AIModelsPart

data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)
