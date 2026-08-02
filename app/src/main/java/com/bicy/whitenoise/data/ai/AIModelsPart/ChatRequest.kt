package com.bicy.whitenoise.data.ai.AIModelsPart

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val stream: Boolean = false,
    val tools: List<Map<String, Any>>? = null,
    val enableThinking: Boolean = false
)
