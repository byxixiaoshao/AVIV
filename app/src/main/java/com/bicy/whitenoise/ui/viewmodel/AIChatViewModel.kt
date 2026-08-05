package com.bicy.whitenoise.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.whitenoise.data.ai.AIService
import com.bicy.whitenoise.data.ai.AIModelsPart.ChatMessage as AiChatMessage
import com.bicy.whitenoise.data.agent.AgentService
import com.bicy.whitenoise.storage.ConversationManager
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.utils.LogManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bicy.whitenoise.data.ai.AIModelsPart.*
import com.bicy.whitenoise.ui.screens.ChatStatePart.*
import org.json.JSONObject

data class ConversationSessionEntity(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ChatMessageType {
    USER, ASSISTANT, TOOL_CALL, THINKING, ERROR, SYSTEM, STREAMING
}

data class ChatUiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val type: ChatMessageType,
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String? = null,
    val toolArgs: String? = null,
    val toolResult: String? = null,
    val isToolError: Boolean = false,
    val isToolComplete: Boolean = false,
    val isExpanded: Boolean = false
)

data class ChatUiState(
    val messages: List<ChatUiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentThinking: String? = null,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val currentSessionId: String? = null,
    val sessions: List<ConversationSessionEntity> = emptyList(),
    val showSessionList: Boolean = false,
    val inputText: String = "",
    /** 变色按钮开关：true=弹窗确认模式，false=直接执行模式 */
    val confirmMode: Boolean = false,
    /** 等待用户确认的工具调用（非 null 时显示确认弹窗） */
    val pendingConfirmation: PendingConfirmation? = null
)

/** 任务A: 待确认弹窗中的单个工具项 */
data class PendingToolItem(
    val toolName: String,
    val arguments: String,
    val description: String
)

/** 等待确认的工具调用信息（支持多工具合并弹窗） */
data class PendingConfirmation(
    val tools: List<PendingToolItem>,
    val isSingle: Boolean = tools.size == 1
)

class AIChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AIChatViewModel"
    }

    private val aiService = AIService(application)
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var conversationHistory: MutableList<AiChatMessage> = mutableListOf()
    private var isInitialized = false

    // 工具调用确认：等待用户操作的 Deferred
    private var confirmDeferred: CompletableDeferred<AIService.ConfirmResult>? = null

    fun ensureInitialized(mainViewModel: MainViewModel) {
        if (isInitialized) return
        AgentService.init(mainViewModel)
        aiService.initialize()
        isInitialized = true
        Log.d(TAG, "AIChatViewModel initialized, AgentService tools: ${AgentService.listAvailableTools().size}")
    }

    fun refreshConfig() {
        aiService.initialize()
    }

    fun isAiReady(): Boolean = aiService.isInitialized()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        if (!aiService.isInitialized()) {
            _uiState.update { it.copy(error = "AI 未启用或未配置，请在设置中配置 API 信息") }
            return
        }

        viewModelScope.launch {
            // 自动创建会话
            if (_uiState.value.currentSessionId == null) {
                val session = ConversationManager.createSession("新对话")
                _uiState.update { it.copy(currentSessionId = session.id) }
            }

            val userMsg = ChatUiMessage(content = text, type = ChatMessageType.USER)
            _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true, error = null) }
            conversationHistory.add(AiChatMessage(role = "user", content = text))

            // 始终在首位置注入最新 system prompt（替换旧的）
            val systemPrompt = AiChatMessage(role = "system", content = aiService.buildSystemPrompt())
            val existingSystemIdx = conversationHistory.indexOfFirst { it.role == "system" }
            if (existingSystemIdx >= 0) {
                conversationHistory[existingSystemIdx] = systemPrompt
            } else {
                conversationHistory.add(0, systemPrompt)
            }

            var streamingMsgId: String? = null
            try {
                val result = aiService.chatWithToolsAndStream(
                    messages = conversationHistory,
                    onRound = { roundInfo ->
                        if (roundInfo.thinking != null) {
                            _uiState.update { it.copy(currentThinking = roundInfo.thinking) }
                            val thinkMsg = ChatUiMessage(
                                content = roundInfo.thinking,
                                type = ChatMessageType.THINKING
                            )
                            _uiState.update { it.copy(messages = it.messages + thinkMsg) }
                        }
                    },
                    onToolCall = { toolCallInfo ->
                        if (toolCallInfo.isComplete) {
                            // 更新已有的工具调用消息
                            _uiState.update { state ->
                                val msgs = state.messages.toMutableList()
                                val idx = msgs.indexOfLast {
                                    it.type == ChatMessageType.TOOL_CALL &&
                                    it.toolName == toolCallInfo.toolName &&
                                    !it.isToolComplete
                                }
                                if (idx >= 0) {
                                    msgs[idx] = msgs[idx].copy(
                                        toolResult = toolCallInfo.result,
                                        isToolError = toolCallInfo.isError,
                                        isToolComplete = true
                                    )
                                } else {
                                    msgs.add(ChatUiMessage(
                                        content = toolCallInfo.result ?: "",
                                        type = ChatMessageType.TOOL_CALL,
                                        toolName = toolCallInfo.toolName,
                                        toolArgs = toolCallInfo.arguments,
                                        toolResult = toolCallInfo.result,
                                        isToolError = toolCallInfo.isError,
                                        isToolComplete = true
                                    ))
                                }
                                state.copy(messages = msgs)
                            }
                        } else {
                            // 工具开始调用 → 直接追加到消息列表
                            val toolMsg = ChatUiMessage(
                                content = "",
                                type = ChatMessageType.TOOL_CALL,
                                toolName = toolCallInfo.toolName,
                                toolArgs = toolCallInfo.arguments,
                                isToolComplete = false
                            )
                            _uiState.update { it.copy(messages = it.messages + toolMsg) }
                        }
                    },
                    onStreamChunk = { delta ->
                        _uiState.update { state ->
                            if (streamingMsgId == null) {
                                val newMsg = ChatUiMessage(
                                    content = delta,
                                    type = ChatMessageType.STREAMING
                                )
                                streamingMsgId = newMsg.id
                                state.copy(messages = state.messages + newMsg)
                            } else {
                                val msgs = state.messages.toMutableList()
                                val idx = msgs.indexOfLast { it.id == streamingMsgId }
                                if (idx >= 0) {
                                    msgs[idx] = msgs[idx].copy(content = msgs[idx].content + delta)
                                }
                                state.copy(messages = msgs)
                            }
                        }
                    },
                    onOperation = null,
                    onToolConfirm = { tools -> requestConfirmation(tools) }
                )

                if (result.isFailure) {
                    val errorMsg = result.exceptionOrNull()?.message ?: "未知错误"
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatUiMessage(content = errorMsg, type = ChatMessageType.ERROR),
                            isLoading = false,
                            error = errorMsg
                        )
                    }
                    return@launch
                }

                val response = result.getOrNull()!!
                val assistantMsg = response.choices.firstOrNull()?.message
                val replyContent = assistantMsg?.content?.trim()
                val usage = response.usage
                val inputTk = usage?.promptTokens ?: 0
                val outputTk = usage?.completionTokens ?: 0

                _uiState.update { state ->
                    val msgs = state.messages.toMutableList()
                    // 将流式消息转为最终助手消息
                    if (streamingMsgId != null) {
                        val idx = msgs.indexOfLast { it.id == streamingMsgId }
                        if (idx >= 0 && !replyContent.isNullOrEmpty()) {
                            msgs[idx] = msgs[idx].copy(
                                content = replyContent,
                                type = ChatMessageType.ASSISTANT
                            )
                        } else if (idx >= 0) {
                            msgs.removeAt(idx)
                        }
                    }
                    if (streamingMsgId == null && !replyContent.isNullOrEmpty()) {
                        msgs.add(ChatUiMessage(content = replyContent, type = ChatMessageType.ASSISTANT))
                        conversationHistory.add(AiChatMessage(role = "assistant", content = replyContent))
                    } else if (!replyContent.isNullOrEmpty()) {
                        conversationHistory.add(AiChatMessage(role = "assistant", content = replyContent))
                    }
                    state.copy(
                        messages = msgs,
                        isLoading = false,
                        currentThinking = null,
                        inputTokens = inputTk,
                        outputTokens = outputTk
                    )
                }
                // 自动保存会话
                saveCurrentSession()
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage failed", e)
                // 即使请求失败也保存已有的对话内容
                saveCurrentSession()
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatUiMessage(
                            content = "请求失败：${e.message}",
                            type = ChatMessageType.ERROR
                        ),
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun toggleThinkingExpansion(messageId: String) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map {
                if (it.id == messageId) it.copy(isExpanded = !it.isExpanded) else it
            })
        }
    }

    fun toggleToolExpansion(messageId: String) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map {
                if (it.id == messageId) it.copy(isExpanded = !it.isExpanded) else it
            })
        }
    }

    fun clearMessages() {
        conversationHistory.clear()
        _uiState.value = ChatUiState()
    }

    // ── 会话管理 ──

    fun loadSessions() {
        viewModelScope.launch {
            try {
                val sessions = ConversationManager.getSessions().map {
                    ConversationSessionEntity(it.id, it.title, it.createdAt, it.updatedAt)
                }
                _uiState.update { it.copy(sessions = sessions) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sessions", e)
                LogManager.e(TAG, "Failed to load sessions", e)
            }
        }
    }

    /**
     * 恢复最近一次会话（屏幕打开时调用）
     */
    fun restoreLastSession() {
        viewModelScope.launch {
            try {
                val sessions = ConversationManager.getSessions().map {
                    ConversationSessionEntity(it.id, it.title, it.createdAt, it.updatedAt)
                }
                _uiState.update { it.copy(sessions = sessions) }
                if (sessions.isNotEmpty()) {
                    switchSession(sessions.first().id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore last session", e)
                LogManager.e(TAG, "Failed to restore last session", e)
            }
        }
    }

    fun newSession() {
        conversationHistory.clear()
        viewModelScope.launch {
            val session = ConversationManager.createSession("新对话")
            val sessions = ConversationManager.getSessions().map {
                ConversationSessionEntity(it.id, it.title, it.createdAt, it.updatedAt)
            }
            _uiState.value = ChatUiState(
                currentSessionId = session.id,
                sessions = sessions,
                showSessionList = false
            )
        }
    }

    fun switchSession(sessionId: String) {
        saveCurrentSession()
        viewModelScope.launch {
            try {
                val messages = ConversationManager.loadMessagesToHistory(sessionId)
                conversationHistory.clear()
                conversationHistory.addAll(messages.filter { !it.isThinking })
                _uiState.update {
                    it.copy(
                        currentSessionId = sessionId,
                        messages = messages.filter { !it.isThinking }.map { msg ->
                            when (msg.role) {
                                "tool" -> {
                                    val isError = msg.content.startsWith("错误:")
                                    ChatUiMessage(
                                        content = msg.content,
                                        type = ChatMessageType.TOOL_CALL,
                                        toolName = msg.toolCallId,
                                        toolResult = msg.content,
                                        isToolError = isError,
                                        isToolComplete = true
                                    )
                                }
                                else -> ChatUiMessage(
                                    content = msg.content,
                                    type = when (msg.role) {
                                        "user" -> ChatMessageType.USER
                                        "assistant" -> ChatMessageType.ASSISTANT
                                        else -> ChatMessageType.SYSTEM
                                    }
                                )
                            }
                        },
                        showSessionList = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch session", e)
                LogManager.e(TAG, "Failed to switch session", e)
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            ConversationManager.deleteSession(sessionId)
            if (_uiState.value.currentSessionId == sessionId) {
                conversationHistory.clear()
                val sessions = ConversationManager.getSessions().map {
                    ConversationSessionEntity(it.id, it.title, it.createdAt, it.updatedAt)
                }
                if (sessions.isNotEmpty()) {
                    switchSession(sessions.first().id)
                } else {
                    _uiState.value = ChatUiState(sessions = emptyList())
                }
            } else {
                val sessions = ConversationManager.getSessions().map {
                    ConversationSessionEntity(it.id, it.title, it.createdAt, it.updatedAt)
                }
                _uiState.update { it.copy(sessions = sessions) }
            }
        }
    }

    fun toggleSessionList() {
        _uiState.update { it.copy(showSessionList = !it.showSessionList) }
        if (_uiState.value.showSessionList) loadSessions()
    }

    private fun saveCurrentSession() {
        val sessionId = _uiState.value.currentSessionId ?: return
        // 过滤掉 system prompt（每次注入最新版本）
        val messagesToSave = conversationHistory.filter { it.role != "system" }
        if (messagesToSave.isEmpty()) return
        val lastUserMsg = messagesToSave.lastOrNull { it.role == "user" }?.content ?: "新对话"
        viewModelScope.launch {
            ConversationManager.saveHistory(sessionId, messagesToSave.toList(), lastUserMsg)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    // ── 工具调用确认（变色按钮）──

    /** 切换确认模式：true=每次工具调用前弹窗确认，false=直接执行 */
    fun toggleConfirmMode() {
        _uiState.update { it.copy(confirmMode = !it.confirmMode) }
    }

    /**
     * 任务A: 请求用户确认工具调用（confirmMode 开启时由 AIService 调用）
     * 接收待确认的修改类工具列表，合并为单个弹窗。
     * 返回 null 表示无需确认（直接执行），否则挂起等待用户操作。
     */
    private suspend fun requestConfirmation(tools: List<AIService.ToolConfirmItem>): AIService.ConfirmResult? {
        if (!_uiState.value.confirmMode) return null
        val deferred = CompletableDeferred<AIService.ConfirmResult>()
        confirmDeferred = deferred
        val pendingTools = tools.map {
            PendingToolItem(
                toolName = it.toolName,
                arguments = it.args.toString(2),
                description = AgentService.getToolDescription(it.toolName) ?: ""
            )
        }
        _uiState.update {
            it.copy(pendingConfirmation = PendingConfirmation(
                tools = pendingTools,
                isSingle = pendingTools.size == 1
            ))
        }
        return deferred.await()
    }

    /**
     * 确认执行工具调用（可选传入修改后的参数 JSON 字符串）
     * 任务A: 仅单个工具时支持参数修改，多工具时忽略 modifiedArgs 直接执行原参数
     */
    fun confirmToolCall(modifiedArgs: String? = null) {
        val isSingle = _uiState.value.pendingConfirmation?.isSingle == true
        val modified = if (isSingle && modifiedArgs != null) {
            runCatching { JSONObject(modifiedArgs) }.getOrNull()
        } else null
        confirmDeferred?.complete(AIService.ConfirmResult.Confirm(modified))
        confirmDeferred = null
        _uiState.update { it.copy(pendingConfirmation = null) }
    }

    /** 拒绝执行工具调用（AI 可根据拒绝原因重试） */
    fun rejectToolCall(reason: String) {
        confirmDeferred?.complete(AIService.ConfirmResult.Reject(reason))
        confirmDeferred = null
        _uiState.update { it.copy(pendingConfirmation = null) }
    }
}
