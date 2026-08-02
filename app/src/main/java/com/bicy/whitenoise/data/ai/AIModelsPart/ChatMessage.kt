package com.bicy.whitenoise.data.ai.AIModelsPart

data class ChatMessage(
    val role: String,
    val content: String,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val isThinking: Boolean = false,
    val toolCallsJson: String? = null
)
