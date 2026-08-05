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
     * - Confirm: 用户确认执行（modifiedArgs 仅单工具时有效，多工具时为 null 直接执行原参数）
     * - Reject: 用户拒绝执行（携带拒绝原因，AI 可据此重试），拒绝所有待确认工具
     */
    sealed class ConfirmResult {
        data class Confirm(val modifiedArgs: JSONObject? = null) : ConfirmResult()
        data class Reject(val reason: String) : ConfirmResult()
    }

    /**
     * 任务A: 待确认的工具调用项
     * 包含工具名、参数、toolCallId（用于关联 tool message）和原始参数字符串
     */
    data class ToolConfirmItem(
        val toolName: String,
        val args: JSONObject,
        val toolCallId: String,
        val rawArgs: String
    )

    /**
     * 任务A: 工具调用确认回调
     * 接收待确认的修改类工具列表，返回确认/拒绝结果
     * - 返回 null 表示无需确认（confirmMode 关闭），直接执行
     * - 返回 Confirm 执行所有工具（modifiedArgs 仅单工具时有效）
     * - 返回 Reject 拒绝所有工具
     */
    typealias ToolConfirmCallback = suspend (tools: List<ToolConfirmItem>) -> ConfirmResult?

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

            // 任务A: 读取类工具直接执行，修改类工具收集后合并确认（只弹一个弹窗）
            processToolCalls(message.toolCalls!!, messages, onToolCall, onOperation, onToolConfirm)
        }
        return Result.failure(Exception("超过最大工具调用次数 ($maxIterations 次)"))
    }

    /**
     * 任务A: 统一处理一轮中的所有工具调用
     * - 读取类工具（isReadOnly=true）：免确认直接执行
     * - 修改类工具：收集到列表，合并为单个确认弹窗（若 onToolConfirm 非 null）
     *   - 用户确认 → 执行所有（单工具支持参数修改，多工具用原参数）
     *   - 用户拒绝 → 注入拒绝消息到所有 tool message
     *   - 返回 null → 无需确认，直接执行
     */
    private suspend fun processToolCalls(
        toolCalls: List<com.bicy.whitenoise.data.ai.AIModelsPart.ToolCall>,
        messages: MutableList<ChatMessage>,
        onToolCall: ((ToolCallInfo) -> Unit)?,
        onOperation: ((OperationRecord) -> Unit)?,
        onToolConfirm: ToolConfirmCallback?
    ) {
        val ctx = AgentService.getCurrentContext()
        val pendingConfirmTools = mutableListOf<ToolConfirmItem>()

        for (toolCall in toolCalls) {
            val toolName = toolCall.function.name
            val toolArgs = toolCall.function.arguments
            Log.d(TAG, "工具调用: $toolName($toolArgs)")

            onToolCall?.invoke(ToolCallInfo(toolName, toolArgs, isComplete = false))

            val argsJson = try {
                JSONObject(toolArgs)
            } catch (e: Exception) {
                Log.e(TAG, "工具参数解析失败: ${toolArgs.take(200)}", e)
                messages.add(ChatMessage(role = "tool", content = "错误: 工具参数格式错误", toolCallId = toolCall.id))
                onToolCall?.invoke(ToolCallInfo(toolName, toolArgs, "错误: 工具参数格式错误", isError = true, isComplete = true))
                continue
            }

            if (AgentService.isToolReadOnly(toolName)) {
                // 读取类工具——免确认直接执行
                executeToolAndAppend(ToolConfirmItem(toolName, argsJson, toolCall.id, toolArgs), argsJson, messages, ctx, onToolCall, onOperation)
            } else {
                // 修改类工具——收集待确认
                pendingConfirmTools.add(ToolConfirmItem(toolName, argsJson, toolCall.id, toolArgs))
            }
        }

        // 任务A: 合并确认修改类工具（只弹一个弹窗）
        if (pendingConfirmTools.isEmpty()) return

        val confirmResult = onToolConfirm?.invoke(pendingConfirmTools)
        when (confirmResult) {
            null -> {
                // 无需确认（confirmMode 关闭）——直接执行所有
                for (item in pendingConfirmTools) {
                    executeToolAndAppend(item, item.args, messages, ctx, onToolCall, onOperation)
                }
            }
            is ConfirmResult.Confirm -> {
                // 用户确认——单工具支持参数修改，多工具用原参数
                for (item in pendingConfirmTools) {
                    val effectiveArgs = if (pendingConfirmTools.size == 1) confirmResult.modifiedArgs ?: item.args else item.args
                    executeToolAndAppend(item, effectiveArgs, messages, ctx, onToolCall, onOperation)
                }
            }
            is ConfirmResult.Reject -> {
                // 用户拒绝——所有修改类工具注入拒绝消息
                val rejectMsg = "用户拒绝执行: ${confirmResult.reason}"
                for (item in pendingConfirmTools) {
                    Log.d(TAG, "工具被拒绝: ${item.toolName}, 原因: ${confirmResult.reason}")
                    onToolCall?.invoke(ToolCallInfo(item.toolName, item.rawArgs, rejectMsg, isError = true, isComplete = true))
                    messages.add(ChatMessage(role = "tool", content = rejectMsg, toolCallId = item.toolCallId))
                }
            }
        }
    }

    /**
     * 执行单个工具调用并追加结果到 messages
     */
    private suspend fun executeToolAndAppend(
        item: ToolConfirmItem,
        effectiveArgs: JSONObject,
        messages: MutableList<ChatMessage>,
        ctx: com.bicy.whitenoise.data.agent.AgentToolPart.ToolContext,
        onToolCall: ((ToolCallInfo) -> Unit)?,
        onOperation: ((OperationRecord) -> Unit)?
    ) {
        val toolResult = AgentService.executeTool(item.toolName, effectiveArgs, ctx)
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
        onToolCall?.invoke(ToolCallInfo(item.toolName, item.rawArgs, content, isError, isComplete = true))
        messages.add(ChatMessage(role = "tool", content = content, toolCallId = item.toolCallId))
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

            // 任务A: 读取类工具直接执行，修改类工具收集后合并确认（只弹一个弹窗）
            processToolCalls(message2.toolCalls!!, messages, onToolCall, onOperation, onToolConfirm)
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
