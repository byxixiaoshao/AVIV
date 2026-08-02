package com.bicy.whitenoise.data.ai.AIModelsPart

data class StreamChunk(
    val delta: ChatMessage,
    val finishReason: String? = null
)
