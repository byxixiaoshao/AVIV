package com.bicy.whitenoise.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.bicy.whitenoise.data.ai.AIModelsPart.*

class OpenAIProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val maxRetries: Int = 5,
    private val connectTimeoutSec: Long = 30,
    private val readTimeoutSec: Long = 120
) : AIProvider {

    companion object {
        private const val TAG = "OpenAIProvider"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
            .writeTimeout(readTimeoutSec, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    override suspend fun chat(request: ChatRequest): Result<ChatResponse> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            if (attempt > 1) {
                val delayMs = when (attempt) {
                    2 -> 2000L; 3 -> 3000L; 4 -> 5000L; else -> 8000L
                }
                Log.d(TAG, "等待 ${delayMs}ms 后重试 (第${attempt}次)...")
                delay(delayMs)
            }
            val result = executeChat(request)
            if (result.isSuccess) return@withContext result
            val err = result.exceptionOrNull() as? Exception ?: Exception("未知错误")
            lastException = err
            if (!isRetryable(err)) return@withContext result
            Log.w(TAG, "可重试错误 (第${attempt}次): ${err.message}")
        }
        Result.failure(Exception("请求失败(已重试 $maxRetries 次): ${lastException?.message}"))
    }

    private fun executeChat(request: ChatRequest): Result<ChatResponse> = runCatching {
        val body = buildRequestBody(request, stream = false)
        val req = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody(JSON))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val errBody = resp.body?.string() ?: "无错误详情"
                val msg = parseErrorMessage(errBody) ?: "HTTP ${resp.code}"
                throw Exception("API 错误 (${resp.code}): $msg")
            }
            val text = resp.body?.string() ?: throw Exception("空响应")
            parseResponse(text)
        }
    }.onFailure { Log.e(TAG, "请求异常: ${it.javaClass.simpleName}: ${it.message}", it) }

    override fun chatStream(request: ChatRequest): Flow<Result<StreamChunk>> = flow {
        var retryCount = 0
        var lastError: Exception? = null
        while (retryCount < maxRetries) {
            if (retryCount > 0) {
                delay(retryCount * 1000L)
                Log.d(TAG, "流式请求重试 (第${retryCount + 1}次)...")
            }
            try {
                val body = buildRequestBody(request, stream = true)
                val req = Request.Builder()
                    .url("$baseUrl/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(body.toRequestBody(JSON))
                    .build()
                val resp: Response = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string() ?: ""
                    val msg = parseErrorMessage(errBody) ?: "HTTP ${resp.code}"
                    lastError = Exception("API 错误 (${resp.code}): $msg")
                    if (msg.contains("rate limit") || resp.code == 401 || resp.code == 403) {
                        emit(Result.failure(lastError))
                        return@flow
                    }
                    retryCount++
                    continue
                }
                var success = false
                resp.body?.byteStream()?.bufferedReader()?.use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        if (line.startsWith("data: ")) {
                            val data = line.substring(6)
                            if (data != "[DONE]") {
                                parseStreamChunk(data)?.let { chunk ->
                                    emit(Result.success(chunk))
                                    success = true
                                }
                            }
                        }
                        line = reader.readLine()
                    }
                }
                if (success) return@flow
            } catch (e: java.io.IOException) {
                Log.w(TAG, "流式IO错误: ${e.message}")
                lastError = e
                retryCount++
                continue
            } catch (e: Exception) {
                Log.e(TAG, "流式请求异常", e)
                emit(Result.failure(Exception("流式请求失败: ${e.message}")))
                return@flow
            }
            break
        }
        if (retryCount >= maxRetries) {
            emit(Result.failure(Exception("流式请求失败(已重试 $maxRetries 次): ${lastError?.message}")))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun testConnection(): Result<Boolean> = runCatching {
        val req = ChatRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(ChatMessage("user", "Hello")),
            maxTokens = 10
        )
        chat(req).isSuccess
    }

    private fun isRetryable(error: Exception): Boolean {
        val m = error.message ?: return false
        return m.contains("超时") || m.contains("IO") || m.contains("连接被拒绝") ||
               m.contains("SocketTimeoutException") || m.contains("UnknownHostException") ||
               m.contains("reset") || m.contains("broken pipe")
    }

    private fun parseErrorMessage(errorBody: String): String? = runCatching {
        JSONObject(errorBody).optJSONObject("error")?.optString("message")
    }.getOrNull()

    private fun buildRequestBody(request: ChatRequest, stream: Boolean): String {
        val messagesArray = JSONArray()
        request.messages.forEach { msg ->
            val msgObj = JSONObject()
            msgObj.put("role", msg.role)
            if (msg.role == "tool") {
                msgObj.put("content", msg.content)
                msgObj.put("tool_call_id", msg.toolCallId)
            } else {
                msgObj.put("content", msg.content)
                msg.toolCalls?.takeIf { it.isNotEmpty() }?.let { tcs ->
                    val arr = JSONArray()
                    tcs.forEach { tc ->
                        val tcObj = JSONObject()
                        tcObj.put("id", tc.id)
                        tcObj.put("type", tc.type)
                        val func = JSONObject()
                        func.put("name", tc.function.name)
                        func.put("arguments", tc.function.arguments)
                        tcObj.put("function", func)
                        arr.put(tcObj)
                    }
                    msgObj.put("tool_calls", arr)
                }
            }
            messagesArray.put(msgObj)
        }
        val body = JSONObject()
        body.put("model", request.model)
        body.put("messages", messagesArray)
        body.put("temperature", request.temperature)
        body.put("max_tokens", request.maxTokens)
        body.put("stream", stream)
        request.tools?.takeIf { it.isNotEmpty() }?.let { tools ->
            val arr = JSONArray()
            tools.forEach { arr.put(JSONObject(it)) }
            body.put("tools", arr)
        }
        if (request.enableThinking) {
            val lower = request.model.lowercase()
            if (lower.contains("deepseek") || lower.contains("r1")) {
                body.put("reasoning_effort", "medium")
            }
        }
        return body.toString()
    }

    private fun parseResponse(text: String): ChatResponse {
        val json = JSONObject(text)
        val choices = mutableListOf<ChatChoice>()
        val choicesArray = json.getJSONArray("choices")
        for (i in 0 until choicesArray.length()) {
            val choiceObj = choicesArray.getJSONObject(i)
            val messageObj = choiceObj.getJSONObject("message")
            val toolCalls = if (messageObj.has("tool_calls")) {
                val tcArr = messageObj.getJSONArray("tool_calls")
                val list = mutableListOf<ToolCall>()
                for (j in 0 until tcArr.length()) {
                    val tc = tcArr.getJSONObject(j)
                    val func = tc.getJSONObject("function")
                    list.add(ToolCall(
                        id = tc.getString("id"),
                        type = tc.optString("type", "function"),
                        function = ToolCallFunction(
                            name = func.getString("name"),
                            arguments = func.getString("arguments")
                        )
                    ))
                }
                list
            } else null
            val content = messageObj.optString("content", "")
            val reasoning = messageObj.optString("reasoning_content", "")
            val finalContent = if (reasoning.isNotEmpty()) {
                "\u2354$reasoning\u2355$content"
            } else content
            choices.add(ChatChoice(
                index = choiceObj.getInt("index"),
                message = ChatMessage(
                    role = messageObj.getString("role"),
                    content = finalContent,
                    toolCalls = toolCalls
                ),
                finishReason = choiceObj.optString("finish_reason")
            ))
        }
        var usage: ChatUsage? = null
        if (json.has("usage")) {
            val u = json.getJSONObject("usage")
            usage = ChatUsage(
                promptTokens = u.getInt("prompt_tokens"),
                completionTokens = u.getInt("completion_tokens"),
                totalTokens = u.getInt("total_tokens")
            )
        }
        return ChatResponse(id = json.getString("id"), choices = choices, usage = usage)
    }

    private fun parseStreamChunk(data: String): StreamChunk? {
        val json = JSONObject(data)
        val choicesArray = json.getJSONArray("choices")
        if (choicesArray.length() == 0) return null
        val choiceObj = choicesArray.getJSONObject(0)
        if (!choiceObj.has("delta")) return null
        val delta = choiceObj.getJSONObject("delta")
        val content = delta.optString("content", "")
        val reasoning = delta.optString("reasoning_content", "")
        val role = delta.optString("role", "assistant")
        val finalContent = when {
            reasoning.isNotEmpty() && content.isNotEmpty() -> "\u2354$reasoning\u2355$content"
            reasoning.isNotEmpty() -> "\u2354$reasoning"
            content.isNotEmpty() -> content
            else -> ""
        }
        return StreamChunk(
            delta = ChatMessage(role = role, content = finalContent),
            finishReason = choiceObj.optString("finish_reason")
        )
    }
}
