package com.bicy.whitenoise.ui.screens.ChatStatePart

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
