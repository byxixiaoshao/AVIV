package com.bicy.whitenoise.data.ai.AIModelsPart

data class ChatResponse(
    val id: String,
    val choices: List<ChatChoice>,
    val usage: ChatUsage? = null
)
