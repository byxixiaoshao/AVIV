package com.bicy.whitenoise.data.ai.AIModelsPart

data class ChatUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)
