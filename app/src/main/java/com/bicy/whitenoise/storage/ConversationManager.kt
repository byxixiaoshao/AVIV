package com.bicy.whitenoise.storage

import com.bicy.whitenoise.WhiteNoiseApp
import com.bicy.whitenoise.storage.core.JsonStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID
import com.bicy.whitenoise.data.ai.AIModelsPart.*
import com.google.gson.Gson

data class ConversationSessionEntity(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ConversationMessageEntity(
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val toolCallsJson: String? = null,
    val toolCallId: String? = null,
    val isThinking: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

object ConversationManager {

    private val gson = Gson()
    private const val SESSIONS_FILE = "conversation_sessions.json"
    private const val MESSAGES_FILE = "conversation_messages.json"

    init {
        JsonStorageManager.init(WhiteNoiseApp.context)
    }

    private fun readSessions(): List<ConversationSessionEntity> = runBlocking {
        JsonStorageManager.read(SESSIONS_FILE, Array<ConversationSessionEntity>::class.java)?.toList()
            ?: emptyList()
    }

    private suspend fun writeSessions(sessions: List<ConversationSessionEntity>) {
        JsonStorageManager.write(SESSIONS_FILE, sessions)
    }

    private fun readMessages(): List<ConversationMessageEntity> = runBlocking {
        JsonStorageManager.read(MESSAGES_FILE, Array<ConversationMessageEntity>::class.java)?.toList()
            ?: emptyList()
    }

    private suspend fun writeMessages(messages: List<ConversationMessageEntity>) {
        JsonStorageManager.write(MESSAGES_FILE, messages)
    }

    fun observeSessions(): Flow<List<ConversationSessionEntity>> {
        return flow {
            emit(readSessions())
        }
    }

    suspend fun getSessions(): List<ConversationSessionEntity> = withContext(Dispatchers.IO) {
        readSessions()
    }

    suspend fun getSession(id: String): ConversationSessionEntity? = withContext(Dispatchers.IO) {
        readSessions().find { it.id == id }
    }

    suspend fun createSession(title: String): ConversationSessionEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val session = ConversationSessionEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = now,
            updatedAt = now
        )
        val sessions = readSessions().toMutableList()
        sessions.add(session)
        writeSessions(sessions)
        session
    }

    suspend fun renameSession(id: String, title: String) = withContext(Dispatchers.IO) {
        val sessions = readSessions().map {
            if (it.id == id) it.copy(title = title, updatedAt = System.currentTimeMillis()) else it
        }
        writeSessions(sessions)
    }

    suspend fun deleteSession(id: String) = withContext(Dispatchers.IO) {
        val sessions = readSessions().filter { it.id != id }
        writeSessions(sessions)
        // Also delete messages for this session
        val messages = readMessages().filter { it.sessionId != id }
        writeMessages(messages)
    }

    suspend fun getMessages(sessionId: String): List<ConversationMessageEntity> = withContext(Dispatchers.IO) {
        readMessages().filter { it.sessionId == sessionId }
    }

    suspend fun saveMessage(
        sessionId: String,
        role: String,
        content: String,
        toolCallsJson: String? = null,
        toolCallId: String? = null,
        isThinking: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val msg = ConversationMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = role,
            content = content,
            toolCallsJson = toolCallsJson,
            toolCallId = toolCallId,
            isThinking = isThinking,
            timestamp = System.currentTimeMillis()
        )
        val messages = readMessages().toMutableList()
        messages.add(msg)
        writeMessages(messages)

        // Update session timestamp
        val sessions = readSessions().map {
            if (it.id == sessionId) it.copy(updatedAt = System.currentTimeMillis()) else it
        }
        writeSessions(sessions)
    }

    suspend fun loadMessagesToHistory(sessionId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        val entities = readMessages().filter { it.sessionId == sessionId }
        entities.map { entity ->
            ChatMessage(
                role = entity.role,
                content = entity.content,
                toolCallsJson = entity.toolCallsJson,
                toolCallId = entity.toolCallId,
                isThinking = entity.isThinking
            )
        }
    }

    suspend fun saveHistory(
        sessionId: String,
        messages: List<ChatMessage>,
        lastMessageText: String
    ) = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext
        val entities = messages.map { msg ->
            ConversationMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = msg.role,
                content = msg.content,
                toolCallsJson = msg.toolCallsJson,
                toolCallId = msg.toolCallId,
                isThinking = msg.isThinking,
                timestamp = System.currentTimeMillis()
            )
        }
        // Replace all messages for this session atomically
        val allMessages = readMessages().filter { it.sessionId != sessionId }.toMutableList()
        allMessages.addAll(entities)
        writeMessages(allMessages)

        val title = lastMessageText.take(30).replace("\n", " ").trim().ifBlank { "\u65b0\u5bf9\u8bdd" }
        val sessions = readSessions().map {
            if (it.id == sessionId) it.copy(title = title, updatedAt = System.currentTimeMillis()) else it
        }
        writeSessions(sessions)
    }
}
