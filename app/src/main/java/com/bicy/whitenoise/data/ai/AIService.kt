package com.bicy.whitenoise.data.ai

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.data.agent.AgentService
import com.bicy.whitenoise.data.agent.AgentToolPart.ToolContext
import com.bicy.whitenoise.data.agent.AgentToolPart.ToolResult
import com.bicy.whitenoise.data.ai.AIModelsPart.*
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

class AIService(private val context: Context) {

    private var provider: OpenAIProvider? = null

    companion object {
        private const val TAG = "AIService"
        const val DEFAULT_SYSTEM_PROMPT =
            "你是 AVIV 白噪音与音乐播放器应用的 AI 助手。你可以帮助用户搭建白噪音播放列表、" +
            "调整音质效果、控制音乐播放、配置调音台、设置外观主题等。\n\n" +
            "请根据用户需求调用相应工具完成任务。每次工具调用后请简要说明执行结果。" +
            "如果用户需求模糊，请询问澄清。所有工具返回的金额/时间/ID 等参数以工具响应为准。"
    }

    fun initialize() {
        val baseUrl = ConfigStorage.getAiBaseUrl()
        val apiKey = ConfigStorage.getAiApiKey()
        if (ConfigStorage.isAiEnabled() && apiKey.isNotBlank() && baseUrl.isNotBlank()) {
            provider = OpenAIProvider(
                apiKey = apiKey,
                baseUrl = baseUrl,
                maxRetries = 5,
                connectTimeoutSec = 30,
                readTimeoutSec = 120
            )
        } else {
            provider = null
        }
    }

    fun isInitialized(): Boolean = provider != null && ConfigStorage.isAiEnabled()

    data class ToolCallInfo(
        val toolName: String,
        val arguments: String,
        val result: String? = null,
        val isError: Boolean = false,
        val isComplete: Boolean = false
    )

    data class RoundInfo(
        val round: Int,
        val thinking: String?,
        val hasToolCalls: Boolean
    )

    data class OperationRecord(
        val operationType: String,
        val targetType: String,
        val targetId: String,
        val targetName: String
    )

    /**
     * 工具调用确认结果
     * - Confirm: 用户确认执行（可携带修改后的参数）
     * - Reject: 用户拒绝执行（携带拒绝原因，AI 可据此重试）
     */
    sealed class ConfirmResult {
        data class Confirm(val modifiedArgs: JSONObject? = null) : ConfirmResult()
        data class Reject(val reason: String) : ConfirmResult()
    }

    /** 工具调用确认回调：返回 null 表示无需确认（直接执行） */
    typealias ToolConfirmCallback = suspend (toolName: String, args: JSONObject) -> ConfirmResult?

    suspend fun chatWithTools(
        messages: MutableList<ChatMessage>,
        temperature: Float? = null,
        maxTokens: Int? = null,
        onRound: ((RoundInfo) -> Unit)? = null,
        onToolCall: ((ToolCallInfo) -> Unit)? = null,
        onOperation: ((OperationRecord) -> Unit)? = null,
        onToolConfirm: ToolConfirmCallback? = null
    ): Result<ChatResponse> {
        if (!ConfigStorage.isAiEnabled()) {
            return Result.failure(Exception("AI 功能未启用：请在设置中启用 AI"))
        }
        val apiKey = ConfigStorage.getAiApiKey()
        val baseUrl = ConfigStorage.getAiBaseUrl()
        val model = ConfigStorage.getAiModel()
        when {
            apiKey.isBlank() -> return Result.failure(Exception("API Key 未配置"))
            baseUrl.isBlank() -> return Result.failure(Exception("API 端点未配置"))
            model.isBlank() -> return Result.failure(Exception("AI 模型未配置"))
        }
        val p = provider ?: return Result.failure(Exception("AI 未初始化"))
        val tools = AgentService.getToolsDefinition()
        val maxIterations = ConfigStorage.getAiMaxToolCalls().coerceIn(1, 50)

        var iteration = 0
        while (iteration < maxIterations) {
            iteration++
            Log.d(TAG, "Agent 迭代 $iteration/$maxIterations")

            var networkWait = 0
            while (!NetworkUtils.isNetworkAvailable(context) && networkWait < 10) {
                networkWait++
                onToolCall?.invoke(ToolCallInfo(
                    toolName = "网络检测",
                    arguments = "",
                    result = "等待网络恢复... ($networkWait/10)",
                    isComplete = true
                ))
                delay(2000)
            }
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.failure(Exception("网络不可用"))
            }

            val request = ChatRequest(
                model = model,
                messages = messages,
                temperature = temperature ?: ConfigStorage.getAiTemperature(),
                maxTokens = maxTokens ?: ConfigStorage.getAiMaxTokens(),
                tools = tools,
                enableThinking = ConfigStorage.isAiEnableThinking()
            )

            val result = p.chat(request)
            if (result.isFailure) return result

            val response = result.getOrNull()
                ?: return Result.failure(Exception("API 返回空响应"))
            val message = response.choices.firstOrNull()?.message
                ?: return Result.failure(Exception("无响应消息"))

            val thinking = extractThinking(message.content)
            val hasToolCalls = !message.toolCalls.isNullOrEmpty()

            onRound?.invoke(RoundInfo(iteration, thinking, hasToolCalls))

            if (!hasToolCalls) return Result.success(response)

            messages.add(message)

            for (toolCall in message.toolCalls) {
                val toolName = toolCall.function.name
                val toolArgs = toolCall.function.arguments
                Log.d(TAG, "工具调用: $toolName($toolArgs)")

                onToolCall?.invoke(ToolCallInfo(toolName, toolArgs, isComplete = false))

                val argsJson = try {
                    JSONObject(toolArgs)
                } catch (e: Exception) {
                    Log.e(TAG, "工具参数解析失败: ${toolArgs.take(200)}", e)
                    return Result.failure(Exception("工具参数被截断或格式错误"))
                }

                // 用户确认流程（confirmMode 开启时）
                val effectiveArgs = confirmIfNeeded(onToolConfirm, toolName, argsJson, toolArgs, toolCall, messages, onToolCall)
                if (effectiveArgs == null) continue  // 用户拒绝或确认失败，跳过执行

                val ctx = AgentService.getCurrentContext()
                val toolResult = AgentService.executeTool(toolName, effectiveArgs, ctx)

                val (content, isError) = when (toolResult) {
                    is ToolResult.Success -> {
                        if (toolResult.hasOperation()) {
                            onOperation?.invoke(OperationRecord(
                                operationType = toolResult.operationType!!,
                                targetType = toolResult.targetType!!,
                                targetId = toolResult.targetId!!,
                                targetName = toolResult.targetName ?: ""
                            ))
                        }
                        toolResult.message to false
                    }
                    is ToolResult.Error -> "错误: ${toolResult.message}" to true
                }

                Log.d(TAG, "工具结果: $content")
                onToolCall?.invoke(ToolCallInfo(toolName, toolArgs, content, isError, isComplete = true))

                messages.add(ChatMessage(
                    role = "tool",
                    content = content,
                    toolCallId = toolCall.id
                ))
            }
        }
        return Result.failure(Exception("超过最大工具调用次数 ($maxIterations 次)"))
    }

    /**
     * 工具调用确认：若 onToolConfirm 返回 Reject，向 messages 注入拒绝消息并返回 null（跳过执行）；
     * 若返回 Confirm(modifiedArgs)，返回修改后的参数；若返回 null，返回原始参数（直接执行）。
     */
    private suspend fun confirmIfNeeded(
        onToolConfirm: ToolConfirmCallback?,
        toolName: String,
        argsJson: JSONObject,
        toolArgs: String,
        toolCall: com.bicy.whitenoise.data.ai.AIModelsPart.ToolCall,
        messages: MutableList<ChatMessage>,
        onToolCall: ((ToolCallInfo) -> Unit)?
    ): JSONObject? {
        if (onToolConfirm == null) return argsJson
        return try {
            when (val result = onToolConfirm(toolName, argsJson)) {
                null -> argsJson
                is ConfirmResult.Confirm -> result.modifiedArgs ?: argsJson
                is ConfirmResult.Reject -> {
                    val rejectMsg = "用户拒绝执行: ${result.reason}"
                    onToolCall?.invoke(ToolCallInfo(toolName, toolArgs, rejectMsg, isError = true, isComplete = true))
                    messages.add(ChatMessage(role = "tool", content = rejectMsg, toolCallId = toolCall.id))
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "工具确认流程异常", e)
            argsJson  // 异常时直接执行
        }
    }

    /**
     * 带流式输出的 chatWithTools：
     * - 工具调用轮次使用非流式（速度快）
     * - 最终回复轮次使用流式输出
     */
    suspend fun chatWithToolsAndStream(
        messages: MutableList<ChatMessage>,
        temperature: Float? = null,
        maxTokens: Int? = null,
        onRound: ((RoundInfo) -> Unit)? = null,
        onToolCall: ((ToolCallInfo) -> Unit)? = null,
        onOperation: ((OperationRecord) -> Unit)? = null,
        onStreamChunk: ((String) -> Unit)? = null,
        onToolConfirm: ToolConfirmCallback? = null
    ): Result<ChatResponse> {
        if (!ConfigStorage.isAiEnabled()) {
            return Result.failure(Exception("AI 功能未启用：请在设置中启用 AI"))
        }
        val apiKey = ConfigStorage.getAiApiKey()
        val baseUrl = ConfigStorage.getAiBaseUrl()
        val model = ConfigStorage.getAiModel()
        when {
            apiKey.isBlank() -> return Result.failure(Exception("API Key 未配置"))
            baseUrl.isBlank() -> return Result.failure(Exception("API 端点未配置"))
            model.isBlank() -> return Result.failure(Exception("AI 模型未配置"))
        }
        val p = provider ?: return Result.failure(Exception("AI 未初始化"))
        val tools = AgentService.getToolsDefinition()
        val maxIterations = ConfigStorage.getAiMaxToolCalls().coerceIn(1, 50)

        var iteration = 0
        while (iteration < maxIterations) {
            iteration++
            Log.d(TAG, "Agent 迭代 $iteration/$maxIterations")

            var networkWait = 0
            while (!NetworkUtils.isNetworkAvailable(context) && networkWait < 10) {
                networkWait++
                onToolCall?.invoke(ToolCallInfo(
                    toolName = "网络检测", arguments = "",
                    result = "等待网络恢复... ($networkWait/10)", isComplete = true
                ))
                delay(2000)
            }
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.failure(Exception("网络不可用"))
            }

            val request = ChatRequest(
                model = model,
                messages = messages,
                temperature = temperature ?: ConfigStorage.getAiTemperature(),
                maxTokens = maxTokens ?: ConfigStorage.getAiMaxTokens(),
                tools = tools,
                enableThinking = ConfigStorage.isAiEnableThinking()
            )

            val result2 = p.chat(request)
            if (result2.isFailure) return result2

            val response2 = result2.getOrNull()
                ?: return Result.failure(Exception("API 返回空响应"))
            val message2 = response2.choices.firstOrNull()?.message
                ?: return Result.failure(Exception("无响应消息"))

            val thinking = extractThinking(message2.content)
            val hasToolCalls = !message2.toolCalls.isNullOrEmpty()

            onRound?.invoke(RoundInfo(iteration, thinking, hasToolCalls))

            if (!hasToolCalls) {
                // 最终回复轮次：用流式重新获取
                val fullContent = message2.content.trim().ifBlank { null }
                val cb = onStreamChunk
                if (cb != null) {
                    // 先推送已有的 thinking（如果有）
                    // 然后流式拉取剩余内容
                    val streamRequest = ChatRequest(
                        model = model,
                        messages = messages,
                        temperature = request.temperature,
                        maxTokens = request.maxTokens,
                        stream = true,
                        tools = null,
                        enableThinking = request.enableThinking
                    )
                    var streamedFull = StringBuilder()
                    var inputTokens = 0
                    var outputTokens = 0
                    p.chatStream(streamRequest).collect { chunkResult ->
                        if (chunkResult.isSuccess) {
                            val chunk = chunkResult.getOrNull()
                            if (chunk != null) {
                                val delta = chunk.delta.content
                                if (delta.isNotBlank()) {
                                    streamedFull.append(delta)
                                    cb(delta)
                                }
                                inputTokens = chunk.delta.toolCalls?.size ?: 0
                            }
                        }
                    }
                    val finalContent = streamedFull.toString()
                    val assistantMsg = ChatMessage(
                        role = "assistant",
                        content = finalContent.ifBlank { fullContent ?: "" }
                    )
                    return Result.success(ChatResponse(
                        id = response2.id,
                        choices = listOf(ChatChoice(index = 0, message = assistantMsg, finishReason = "stop")),
                        usage = ChatUsage(promptTokens = 0, completionTokens = finalContent.length, totalTokens = finalContent.length)
                    ))
                }
                return Result.success(response2)
            }

            messages.add(message2)

            for (toolCall in message2.toolCalls) {
                val toolName = toolCall.function.name
                val toolArgs = toolCall.function.arguments
                Log.d(TAG, "工具调用: $toolName($toolArgs)")

                onToolCall?.invoke(ToolCallInfo(toolName, toolArgs, isComplete = false))

                val argsJson = try {
                    JSONObject(toolArgs)
                } catch (e: Exception) {
                    Log.e(TAG, "工具参数解析失败: ${toolArgs.take(200)}", e)
                    return Result.failure(Exception("工具参数被截断或格式错误"))
                }

                // 用户确认流程（confirmMode 开启时）
                val effectiveArgs = confirmIfNeeded(onToolConfirm, toolName, argsJson, toolArgs, toolCall, messages, onToolCall)
                if (effectiveArgs == null) continue  // 用户拒绝，跳过执行

                val ctx = AgentService.getCurrentContext()
                val toolResult = AgentService.executeTool(toolName, effectiveArgs, ctx)

                val (content, isError) = when (toolResult) {
                    is ToolResult.Success -> {
                        if (toolResult.hasOperation()) {
                            onOperation?.invoke(OperationRecord(
                                operationType = toolResult.operationType!!,
                                targetType = toolResult.targetType!!,
                                targetId = toolResult.targetId!!,
                                targetName = toolResult.targetName ?: ""
                            ))
                        }
                        toolResult.message to false
                    }
                    is ToolResult.Error -> "错误: ${toolResult.message}" to true
                }

                Log.d(TAG, "工具结果: $content")
                onToolCall?.invoke(ToolCallInfo(toolName, toolArgs, content, isError, isComplete = true))

                messages.add(ChatMessage(
                    role = "tool",
                    content = content,
                    toolCallId = toolCall.id
                ))
            }
        }
        return Result.failure(Exception("超过最大工具调用次数 ($maxIterations 次)"))
    }

    fun chatStream(
        messages: List<ChatMessage>,
        temperature: Float? = null,
        maxTokens: Int? = null
    ): Flow<Result<StreamChunk>> {
        if (!ConfigStorage.isAiEnabled()) {
            return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("AI 未启用")))
        }
        val p = provider ?: return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("AI 未初始化")))
        val model = ConfigStorage.getAiModel()
        if (model.isBlank()) return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("模型未配置")))

        val request = ChatRequest(
            model = model,
            messages = messages,
            temperature = temperature ?: ConfigStorage.getAiTemperature(),
            maxTokens = maxTokens ?: ConfigStorage.getAiMaxTokens(),
            stream = true,
            enableThinking = ConfigStorage.isAiEnableThinking()
        )
        return p.chatStream(request)
    }

    suspend fun testConnection(): Result<Boolean> {
        val p = provider ?: return Result.failure(Exception("AI 未初始化"))
        return p.testConnection()
    }

    fun buildSystemPrompt(): String {
        val sb = StringBuilder()
        val custom = ConfigStorage.getAiSystemPrompt()
        sb.append(if (custom.isNotBlank()) custom else DEFAULT_SYSTEM_PROMPT)
        sb.append("\n\n")
        sb.append("【可用工具】\n")
        AgentService.listAvailableTools().forEach { name ->
            val desc = AgentService.getToolDescription(name) ?: ""
            sb.append("- $name: $desc\n")
        }
        sb.append("\n请根据用户需求选择合适的工具。")
        return sb.toString()
    }

    private fun extractThinking(content: String): String? {
        val start = content.indexOf("\u2354")
        val end = content.indexOf("\u2355")
        return when {
            start != -1 && end != -1 && end > start ->
                content.substring(start + 1, end).trim().ifEmpty { null }
            start != -1 ->
                content.substring(start + 1).trim().ifEmpty { null }
            else -> null
        }
    }
}
