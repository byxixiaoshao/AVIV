package com.bicy.whitenoise.data.ai.AIModelsPart

data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finishReason: String? = null
)
