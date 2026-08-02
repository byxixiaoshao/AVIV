package com.bicy.whitenoise.data.ai.AIModelsPart

import kotlinx.coroutines.flow.Flow

interface AIProvider {
    suspend fun chat(request: ChatRequest): Result<ChatResponse>
    fun chatStream(request: ChatRequest): Flow<Result<StreamChunk>>
    suspend fun testConnection(): Result<Boolean>
}
