package com.bicy.whitenoise.onlinemusic

import android.util.Base64
import android.util.Log
import com.bicy.whitenoise.BuildConfig
import com.bicy.whitenoise.onlinemusic.model.SourceModelsPart.ScriptInfo
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.QuickJsException
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.evaluate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 脚本引擎（严格参照 lx-music-mobile 架构）
 * 
 * 消息流程：
 * 1. JS 调用 nativeCall(action, data) -> 原生处理 -> callJS(action, data)
 * 2. HTTP 请求由原生层直接执行（替代 React Native 的 fetch）
 */
class SourceScriptEngine {

    companion object {
        private const val TAG = "SourceScriptEngine"
        private const val BROWSER_UA = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36"
        private const val REFERER_MUSIC_163 = "https://music.163.com"
        /** 单个 HTTP 请求的默认超时（连接+读取各 8s），避免单个请求卡死引擎切换流程 */
        private const val DEFAULT_HTTP_TIMEOUT = 8_000L
        /** 脚本端单次请求最长等待（含可能的多轮 HTTP），外层 SourceScriptManager 还有 10s 引擎级超时 */
        private const val REQUEST_TIMEOUT = 15_000L
    }

    private var quickJs: QuickJs? = null
    private var scriptKey: String = ""
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    @Volatile private var isClosed = false
    private val okHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))  // 强制 HTTP/1.1，避免 HTTP/2 被 WAF 识别
        .addNetworkInterceptor { chain ->
            val request = chain.request()
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "🔍 [请求] ${request.method} ${request.url}")
                Log.w(TAG, "🔍 [请求头] ${request.headers}")
            }
            val response = chain.proceed(request)
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "🔍 [响应] ${response.code} ${response.message}")
                Log.w(TAG, "🔍 [响应头] ${response.headers}")
                Log.w(TAG, "🔍 [TLS] ${response.handshake?.cipherSuite}, ${response.handshake?.tlsVersion}")
            }
            response
        }
        .build()
    private val timeoutCallbacks = ConcurrentHashMap<Int, Boolean>()
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<String?>>()

    var onInited: ((String) -> Unit)? = null
    var onUpdateAlert: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    val isRunning: Boolean get() = quickJs?.isClosed == false

    /**
     * 初始化引擎（参照 lx-music-mobile QuickJS.createJSEnv）
     */
    suspend fun init(info: ScriptInfo) {
        val qjs = QuickJs.create(Dispatchers.Default).also { quickJs = it }
        scriptKey = info.id

        try {
            if (BuildConfig.DEBUG) Log.d(TAG, "📍 步骤1: 注册原生桥接 - ${info.name}")
            // 注册原生桥接（参照 lx-music-mobile QuickJS.createEnvObj）
            defineNativeBridge(qjs)
            if (BuildConfig.DEBUG) Log.d(TAG, "✅ 步骤1完成: 原生桥接已注册")

            if (BuildConfig.DEBUG) Log.d(TAG, "📍 步骤2: 注入预加载脚本 - ${info.name}")
            // 注入预加载脚本（参照 lx-music-mobile user-api-preload.js）
            evaluatePreloadScript(qjs, info)
            if (BuildConfig.DEBUG) Log.d(TAG, "✅ 步骤2完成: 预加载脚本已注入")

            if (BuildConfig.DEBUG) Log.d(TAG, "📍 步骤3: 执行用户脚本 - ${info.name}")
            // 执行脚本
            qjs.evaluate<Any?>(info.rawScript)
            if (BuildConfig.DEBUG) Log.d(TAG, "✅ 步骤3完成: 脚本已执行")

            if (BuildConfig.DEBUG) Log.d(TAG, "🎉 脚本初始化成功: ${info.name}")
        } catch (e: QuickJsException) {
            Log.e(TAG, "❌ 脚本初始化失败 [${info.name}]: ${e.message}")
            onError?.invoke(e.message ?: "Unknown error")
            close()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "❌ 脚本初始化异常 [${info.name}]: ${e.message}", e)
            onError?.invoke(e.message ?: "Unknown error")
            close()
            throw e
        }
    }

    /**
     * 发起音源请求（事件驱动，参照 lx-music-mobile 架构）
     * 
     * 流程：callJS('request', {requestKey, data}) → 预加载脚本 handleRequest 
     * → 用户脚本 handler → lx.request → HTTP → callJS('response') 
     * → 预加载脚本 .then → nativeCall('response', {requestKey, status, result}) 
     * → handleScriptResponse → CompletableDeferred
     */
    suspend fun handleRequest(source: String, action: String, infoJson: String): String? {
        if (isClosed) return null
        
        val qjs = quickJs ?: throw IllegalStateException("引擎未初始化")
        
        val requestKey = Math.random().toString()
        val deferred = CompletableDeferred<String?>()
        pendingRequests[requestKey] = deferred

        return try {
            // 解析 info 为 JSON 对象（预加载脚本期望 data.info 为对象）
            val infoObj = try {
                JSONObject(infoJson)
            } catch (_: Exception) {
                infoJson
            }

            val requestData = JSONObject().apply {
                put("requestKey", requestKey)
                put("data", JSONObject().apply {
                    put("source", source)
                    put("action", action)
                    put("info", if (infoObj is String) infoObj else infoObj)
                })
            }

            if (BuildConfig.DEBUG) Log.d(TAG, "📤 发起脚本请求: source=$source, action=$action, requestKey=$requestKey")

            // 通过 callJS 触发预加载脚本的 handleRequest 事件链
            callJS("request", requestData.toString())

            // 等待脚本通过 nativeCall('response', ...) 返回结果
            val result = withTimeout(REQUEST_TIMEOUT) {
                deferred.await()
            }

            if (BuildConfig.DEBUG) Log.d(TAG, "📥 脚本请求完成: $source/$action, result=${result?.take(200)}")
            result
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            if (BuildConfig.DEBUG) Log.w(TAG, "⏰ 请求超时: $source/$action")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ 请求执行失败: ${e.message}")
            null
        } finally {
            pendingRequests.remove(requestKey)
        }
    }

    fun close() {
        isClosed = true
        quickJs?.let {
            if (!it.isClosed) it.close()
        }
        quickJs = null
        timeoutCallbacks.clear()
        pendingRequests.values.forEach { it.cancel() }
        pendingRequests.clear()
        engineScope.cancel()
        if (BuildConfig.DEBUG) Log.d(TAG, "引擎已关闭")
    }

    // ======================== 原生桥接层（参照 lx-music-mobile QuickJS.java）========================

    private suspend fun defineNativeBridge(qjs: QuickJs) {
        // 参照 lx-music-mobile QuickJS.java：所有原生方法注册在 globalThis 上

        if (BuildConfig.DEBUG) Log.d(TAG, "🔧 注册原生桥接对象...")

        qjs.define("__lx_native__") {
            // 接收 JS 消息
            function("__lx_native_call__") { args ->
                val key = args[0] as? String ?: return@function null
                val action = args[1] as? String ?: return@function null
                val dataJson = args[2] as? String?

                if (BuildConfig.DEBUG) Log.w(TAG, "🔔 收到 JS 调用: action=$action, data=$dataJson")

                if (key != scriptKey) {
                    if (BuildConfig.DEBUG) Log.w(TAG, "❌ key 不匹配: $key != $scriptKey")
                    return@function null
                }

                when (action) {
                    "request" -> handleNativeRequest(dataJson)
                    "response" -> handleScriptResponse(dataJson)
                    "init" -> handleNativeInit(dataJson)
                    "showUpdateAlert" -> handleNativeUpdateAlert(dataJson)
                    "__lx_log__" -> {
                        val msg = dataJson?.let { 
                            try { JSONObject(it).optString("msg", it) } catch (_: Exception) { it }
                        } ?: ""
                        if (BuildConfig.DEBUG) Log.w(TAG, "[JS] $msg")
                    }
                }
                null
            }

            // setTimeout
            function("__lx_native_call__set_timeout") { args ->
                val id = (args[0] as? Number)?.toInt() ?: 0
                val timeout = (args[1] as? Number)?.toLong() ?: 0L

                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                timeoutCallbacks[id] = true

                handler.postDelayed({
                    if (timeoutCallbacks.remove(id) != null) {
                        callJS("__set_timeout__", id)
                    }
                }, timeout)

                null
            }

            // 工具方法
            function("__lx_native_call__utils_str2b64") { args ->
                val str = args[0] as? String ?: ""
                String(Base64.encode(str.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP))
            }

            function("__lx_native_call__utils_b642buf") { args ->
                val b64 = args[0] as? String ?: return@function "[]"
                try {
                    val bytes = Base64.decode(b64.toByteArray(), Base64.NO_WRAP)
                    bytes.joinToString(",", "[", "]") { it.toString() }
                } catch (e: Exception) {
                    "[]"
                }
            }

            function("__lx_native_call__utils_str2md5") { args ->
                val str = args[0] as? String ?: ""
                try {
                    val decoded = URLDecoder.decode(str, "UTF-8")
                    val md = MessageDigest.getInstance("MD5")
                    val digest = md.digest(decoded.toByteArray(StandardCharsets.UTF_8))
                    digest.joinToString("") { "%02x".format(it) }
                } catch (e: Exception) {
                    ""
                }
            }

            function("__lx_native_call__utils_aes_encrypt") { args ->
                val dataB64 = args[0] as? String ?: ""
                val keyB64 = args[1] as? String ?: ""
                val ivB64 = args[2] as? String ?: ""
                val mode = args[3] as? String ?: "AES/CBC/PKCS7Padding"
                aesEncrypt(dataB64, keyB64, ivB64, mode)
            }

            function("__lx_native_call__utils_rsa_encrypt") { args ->
                val dataB64 = args[0] as? String ?: ""
                val keyB64 = args[1] as? String ?: ""
                val padding = args[2] as? String ?: "RSA/ECB/NoPadding"
                rsaEncrypt(dataB64, keyB64, padding)
            }
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "🔧 将方法暴露到 globalThis...")

        // 将所有方法暴露到 globalThis（直接在协程中调用，不使用 runBlocking）
        qjs.evaluate<Any?>("""
            globalThis.__lx_native_call__ = globalThis.__lx_native__.__lx_native_call__;
            globalThis.__lx_native_call__set_timeout = globalThis.__lx_native__.__lx_native_call__set_timeout;
            globalThis.__lx_native_call__utils_str2b64 = globalThis.__lx_native__.__lx_native_call__utils_str2b64;
            globalThis.__lx_native_call__utils_b642buf = globalThis.__lx_native__.__lx_native_call__utils_b642buf;
            globalThis.__lx_native_call__utils_str2md5 = globalThis.__lx_native__.__lx_native_call__utils_str2md5;
            globalThis.__lx_native_call__utils_aes_encrypt = globalThis.__lx_native__.__lx_native_call__utils_aes_encrypt;
            globalThis.__lx_native_call__utils_rsa_encrypt = globalThis.__lx_native__.__lx_native_call__utils_rsa_encrypt;
        """.trimIndent())

        if (BuildConfig.DEBUG) Log.d(TAG, "✅ 原生桥接注册完成")
    }

    // 处理 HTTP 请求（参照 lx-music-mobile UserApiModule）
    private fun handleNativeRequest(dataJson: String?) {
        if (BuildConfig.DEBUG) Log.w(TAG, "🚨 handleNativeRequest 被调用！dataJson=$dataJson")

        if (dataJson == null) {
            Log.e(TAG, "❌ handleNativeRequest: dataJson is null")
            return
        }

        try {
            val data = JSONObject(dataJson)
            val requestKey = data.getString("requestKey")
            val url = data.getString("url")
            val options = data.optJSONObject("options") ?: JSONObject()

            if (BuildConfig.DEBUG) Log.w(TAG, "✅ 收到 HTTP 请求: $url, requestKey=$requestKey")

            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    if (BuildConfig.DEBUG) Log.w(TAG, "🌐 开始执行 HTTP 请求...")
                    val response = executeHttpRequest(url, options)
                    if (BuildConfig.DEBUG) Log.w(TAG, "✅ HTTP 请求完成，准备回调 JS")
                    try { callJS("response", mapOf(
                        "requestKey" to requestKey,
                        "error" to null,
                        "response" to response
                    )) } catch (e: Exception) { Log.e(TAG, "callJS 回调失败: ${e.message}") }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ HTTP 请求失败: ${e.message}")
                    try { callJS("response", mapOf(
                        "requestKey" to requestKey,
                        "error" to (e.message ?: "Unknown error"),
                        "response" to null
                    )) } catch (e: Exception) { Log.e(TAG, "callJS 回调失败: ${e.message}") }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析请求参数失败: ${e.message}")
        }
    }

    // 处理脚本返回的 response（预加载脚本 .then 回调中 nativeCall('response', ...) 的结果）
    private fun handleScriptResponse(dataJson: String?) {
        if (dataJson == null) {
            if (BuildConfig.DEBUG) Log.w(TAG, "⚠️ handleScriptResponse: dataJson is null")
            return
        }
        try {
            val data = JSONObject(dataJson)
            val requestKey = data.getString("requestKey")
            val status = data.optBoolean("status", false)
            val deferred = pendingRequests.remove(requestKey)

            if (deferred != null) {
                if (status) {
                    val result = data.opt("result")
                    val resultStr = when (result) {
                        is JSONObject -> result.toString()
                        is String -> result
                        else -> result?.toString()
                    }
                    if (BuildConfig.DEBUG) Log.d(TAG, "✅ 脚本响应成功: requestKey=$requestKey")
                    deferred.complete(resultStr)
                } else {
                    val errorMessage = data.optString("errorMessage", "Unknown error")
                    if (BuildConfig.DEBUG) Log.w(TAG, "❌ 脚本响应失败: requestKey=$requestKey, error=$errorMessage")
                    deferred.complete(null)
                }
            } else {
                if (BuildConfig.DEBUG) Log.w(TAG, "⚠️ 未找到对应的 pendingRequests: requestKey=$requestKey")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ handleScriptResponse 解析失败: ${e.message}")
        }
    }

    // 执行 HTTP 请求（参照 lx-music-mobile userApi/request.js）
    private fun executeHttpRequest(url: String, options: JSONObject): Map<String, Any?> {
        val method = options.optString("method", "GET")
        val scriptHeaders = options.optJSONObject("headers")
        val body = options.opt("body")
        val form = options.opt("form")
        val formData = options.opt("formData")
        val timeout = options.optLong("timeout", DEFAULT_HTTP_TIMEOUT)

        // 构建默认请求头（参照 lx-music-mobile request.js 第 8-9 行）
        // 强制使用 Chrome UA，防止脚本自定义 UA 被服务器拒绝
        // 加入浏览器特征头，防止被 WAF/CDN 识别为非浏览器流量静默拒绝
        val defaultHeaders = JSONObject().apply {
            put("User-Agent", BROWSER_UA)
            put("Accept", "application/json, text/plain, */*")
            put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            put("Referer", REFERER_MUSIC_163)
        }

        // 合并请求头：默认 headers 为基础，脚本 headers 覆盖
        val mergedHeaders = JSONObject()
        defaultHeaders.keys().forEach { key -> mergedHeaders.put(key, defaultHeaders.getString(key)) }
        scriptHeaders?.keys()?.forEach { key ->
            mergedHeaders.put(key, scriptHeaders.getString(key))
        }

        if (BuildConfig.DEBUG) {
            Log.w(TAG, "🌐 HTTP 请求: $method $url")
            Log.w(TAG, "📋 合并请求头: $mergedHeaders")
        }

        val client = okHttpClient.newBuilder()
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .readTimeout(timeout, TimeUnit.MILLISECONDS)
            .build()

        val builder = Request.Builder().url(url)

        mergedHeaders.keys().forEach { key ->
            val value = mergedHeaders.getString(key)
            if (value != null && value !in listOf("null", "undefined")) {
                builder.addHeader(key, value)
            }
        }

        var requestBody: RequestBody? = null
        // 处理 form 数据（x-www-form-urlencoded）
        if (form != null && form != JSONObject.NULL && form is JSONObject) {
            val formBuilder = StringBuilder()
            form.keys().forEach { key ->
                if (formBuilder.isNotEmpty()) formBuilder.append("&")
                formBuilder.append(URLEncoder.encode(key, "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(form.optString(key), "UTF-8"))
            }
            requestBody = formBuilder.toString().toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
            if (BuildConfig.DEBUG) Log.w(TAG, "📝 Form 数据: ${formBuilder.toString().take(200)}")
        }
        // 处理 body 数据（JSON）
        else if (body != null && body != JSONObject.NULL) {
            requestBody = body.toString().toRequestBody("application/json".toMediaTypeOrNull())
        }

        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody ?: ByteArray(0).toRequestBody(null))
            "PUT" -> builder.put(requestBody ?: ByteArray(0).toRequestBody(null))
            "DELETE" -> builder.delete(requestBody)
        }

        val response = client.newCall(builder.build()).execute()
        val responseBody = response.body?.string() ?: ""

        if (BuildConfig.DEBUG) {
            Log.w(TAG, "📊 HTTP 响应: ${response.code} ${response.message}")
            Log.w(TAG, "📄 响应体预览: ${responseBody.take(300)}")
        }

        // 参照 lx-music-mobile：解析 body 为 JSON，解析失败则保留原始字符串
        val parsedBody: Any = try {
            JSONObject(responseBody)
        } catch (_: Exception) {
            try {
                JSONArray(responseBody)
            } catch (_: Exception) {
                responseBody
            }
        }

        return mapOf(
            "statusCode" to response.code,
            "statusMessage" to response.message,
            "headers" to response.headers.toMultimap().mapValues { it.value.joinToString(", ") },
            "body" to parsedBody
        )
    }

    // 调用 JS 函数（参照 lx-music-mobile QuickJS.callJS）
    private fun callJS(action: String, data: Any?) {
        val qjs = quickJs ?: return
        try {
            // 将数据转换为 JSON 字符串，并转义为 JS 字符串字面量
            val dataStr = when (data) {
                is Map<*, *> -> JSONObject(data).toString()
                is Number -> data.toString()
                is String -> data
                null -> "null"
                else -> data.toString()
            }

            // 转义 JSON 字符串中的特殊字符
            val escapedData = dataStr.escapeJs()

            if (BuildConfig.DEBUG) Log.w(TAG, "📞 调用 JS: action=$action, data=$dataStr")

            runBlocking {
                qjs.evaluate<Any?>(
                    """
                    (function() {
                        if (!globalThis.__lx_native__) return;
                        globalThis.__lx_native__('$scriptKey', '$action', '$escapedData');
                    })()
                    """.trimIndent()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "调用 JS 失败: ${e.message}")
        }
    }

    private fun handleNativeInit(dataJson: String?) {
        if (dataJson == null) {
            if (BuildConfig.DEBUG) Log.w(TAG, "⚠️ handleNativeInit: dataJson is null")
            return
        }
        try {
            val data = JSONObject(dataJson)
            if (BuildConfig.DEBUG) Log.w(TAG, "✅ 脚本初始化成功: $dataJson")
            onInited?.invoke(data.toString())
        } catch (e: Exception) {
            Log.e(TAG, "❌ 处理初始化失败: ${e.message}")
        }
    }

    private fun handleNativeUpdateAlert(dataJson: String?) {
        if (dataJson == null) return
        onUpdateAlert?.invoke(dataJson)
    }

    // ======================== 加密方法（参照 lx-music-mobile AES.java / RSA.java）========================

    private fun aesEncrypt(dataB64: String, keyB64: String, ivB64: String, mode: String): String {
        return try {
            val data = Base64.decode(dataB64, Base64.DEFAULT)
            val key = Base64.decode(keyB64, Base64.DEFAULT)

            val cipher = Cipher.getInstance(mode)
            val secretKeySpec = SecretKeySpec(key, "AES")

            if (ivB64.isNotEmpty()) {
                val iv = Base64.decode(ivB64, Base64.DEFAULT)
                val finalIv = ByteArray(16)
                System.arraycopy(iv, 0, finalIv, 0, Math.min(iv.size, 16))
                val ivSpec = IvParameterSpec(finalIv)
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
            }

            val encrypted = cipher.doFinal(data)
            String(Base64.encode(encrypted, Base64.NO_WRAP), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "AES 加密失败: ${e.message}")
            ""
        }
    }

    private fun rsaEncrypt(dataB64: String, keyB64: String, padding: String): String {
        return try {
            val keyBytes = Base64.decode(keyB64.trim().toByteArray(), Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec)

            val cipher = Cipher.getInstance(padding)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)

            val data = Base64.decode(dataB64, Base64.DEFAULT)
            val encrypted = cipher.doFinal(data)
            String(Base64.encode(encrypted, Base64.NO_WRAP))
        } catch (e: Exception) {
            Log.e(TAG, "RSA 加密失败: ${e.message}")
            ""
        }
    }

    // ======================== 预加载脚本（参照 lx-music-mobile user-api-preload.js）========================

    private suspend fun evaluatePreloadScript(qjs: QuickJs, info: ScriptInfo) {
        val escapedName = JSONObject.quote(info.name)
        val escapedDesc = JSONObject.quote(info.description)
        val escapedVer = JSONObject.quote(info.version)
        val escapedAuthor = JSONObject.quote(info.author)
        val escapedHomepage = JSONObject.quote(info.homepage)
        val escapedKey = JSONObject.quote(info.id)
        // 规范化 rawScript：去除BOM、CRLF→LF、trim，与服务器端存储格式保持一致
        val normalizedScript = info.rawScript
            .removePrefix("\uFEFF")  // 去除 UTF-8 BOM
            .replace("\r\n", "\n")   // CRLF → LF
            .trim()                  // 去除首尾空白
        val rawScriptLiteral = JSONObject.quote(normalizedScript)

        qjs.evaluate<Any?>(getPreloadScript(escapedKey, escapedName, escapedDesc, escapedVer, escapedAuthor, escapedHomepage, rawScriptLiteral))
    }

    private fun getPreloadScript(key: String, name: String, desc: String, ver: String, author: String, homepage: String, rawScript: String): String {
        return """
'use strict'

// 定义 console 对象
globalThis.console = {
  log: function() {},
  warn: function() {},
  error: function() {},
  info: function() {},
  debug: function() {}
};

globalThis.lx_setup = (key, id, name, description, version, author, homepage, rawScript) => {
  delete globalThis.lx_setup
  const _nativeCall = globalThis.__lx_native_call__
  delete globalThis.__lx_native_call__
  const checkLength = (str, length = 1048576) => {
    if (typeof str == 'string' && str.length > length) throw new Error('Input too long')
    return str
  }
  const nativeFuncNames = [
    '__lx_native_call__set_timeout',
    '__lx_native_call__utils_str2b64',
    '__lx_native_call__utils_b642buf',
    '__lx_native_call__utils_str2md5',
    '__lx_native_call__utils_aes_encrypt',
    '__lx_native_call__utils_rsa_encrypt',
  ]
  const nativeFuncs = {}
  for (const name of nativeFuncNames) {
    const nativeFunc = globalThis[name]
    delete globalThis[name]
    nativeFuncs[name.replace('__lx_native_call__', '')] = (...args) => {
      for (const arg of args) checkLength(arg)
      return nativeFunc(...args)
    }
  }
  const KEY_PREFIX = {
    publicKeyStart: '-----BEGIN PUBLIC KEY-----',
    publicKeyEnd: '-----END PUBLIC KEY-----',
    privateKeyStart: '-----BEGIN PRIVATE KEY-----',
    privateKeyEnd: '-----END PRIVATE KEY-----',
  }
  const RSA_PADDING = {
    OAEPWithSHA1AndMGF1Padding: 'RSA/ECB/OAEPWithSHA1AndMGF1Padding',
    NoPadding: 'RSA/ECB/NoPadding',
  }
  const AES_MODE = {
    CBC_128_PKCS7Padding: 'AES/CBC/PKCS7Padding',
    ECB_128_NoPadding: 'AES',
  }
  const nativeCall = (action, data) => {
    data = JSON.stringify(data)
    checkLength(data, 2097152)
    _nativeCall(key, action, data)
  }

  const callbacks = new Map()
  let timeoutId = 0
  const _setTimeout = (callback, timeout = 0, ...params) => {
    if (typeof callback !== 'function') throw new Error('callback required a function')
    if (typeof timeout !== 'number' || timeout < 0) throw new Error('timeout required a number')
    if (timeoutId > 90000000000) throw new Error('max timeout')
    const id = timeoutId++
    callbacks.set(id, {
      callback(...args) {
        callback(...args)
      },
      params,
    })
    nativeFuncs.set_timeout(id, parseInt(timeout))
    return id
  }
  const _clearTimeout = (id) => {
    const tagret = callbacks.get(id)
    if (!tagret) return
    callbacks.delete(id)
  }
  const handleSetTimeout = (id) => {
    const tagret = callbacks.get(id)
    if (!tagret) return
    callbacks.delete(id)
    tagret.callback(...tagret.params)
  }

  function bytesToString(bytes) {
    let result = ''
    let i = 0
    while (i < bytes.length) {
      const byte = bytes[i]
      if (byte < 128) {
        result += String.fromCharCode(byte)
        i++
      } else if (byte >= 192 && byte < 224) {
        result += String.fromCharCode(((byte & 31) << 6) | (bytes[i + 1] & 63))
        i += 2
      } else {
        result += String.fromCharCode(((byte & 15) << 12) | ((bytes[i + 1] & 63) << 6) | (bytes[i + 2] & 63))
        i += 3
      }
    }
    return result
  }
  function stringToBytes(inputString) {
    const bytes = []
    for (let i = 0; i < inputString.length; i++) {
      const charCode = inputString.charCodeAt(i)
      if (charCode < 128) {
        bytes.push(charCode)
      } else if (charCode < 2048) {
        bytes.push((charCode >> 6) | 192)
        bytes.push((charCode & 63) | 128)
      } else {
        bytes.push((charCode >> 12) | 224)
        bytes.push(((charCode >> 6) & 63) | 128)
        bytes.push((charCode & 63) | 128)
      }
    }
    return bytes
  }

  const NATIVE_EVENTS_NAMES = {
    init: 'init',
    showUpdateAlert: 'showUpdateAlert',
    request: 'request',
    cancelRequest: 'cancelRequest',
    response: 'response',
  }
  const EVENT_NAMES = {
    request: 'request',
    inited: 'inited',
    updateAlert: 'updateAlert',
  }
  const eventNames = Object.values(EVENT_NAMES)
  const events = {
    request: null,
  }
  const allSources = ['kw', 'kg', 'tx', 'wy', 'mg', 'local']
  const supportQualitys = {
    kw: ['128k', '320k', 'flac', 'flac24bit'],
    kg: ['128k', '320k', 'flac', 'flac24bit'],
    tx: ['128k', '320k', 'flac', 'flac24bit'],
    wy: ['128k', '320k', 'flac', 'flac24bit'],
    mg: ['128k', '320k', 'flac', 'flac24bit'],
    local: [],
  }
  const supportActions = {
    kw: ['musicUrl'],
    kg: ['musicUrl'],
    tx: ['musicUrl'],
    wy: ['musicUrl'],
    mg: ['musicUrl'],
    xm: ['musicUrl'],
    local: ['musicUrl', 'lyric', 'pic'],
  }

  const verifyLyricInfo = (info) => {
    if (typeof info != 'object' || typeof info.lyric != 'string') throw new Error('failed')
    if (info.lyric.length > 51200) throw new Error('failed')
    return {
      lyric: info.lyric,
      tlyric: (typeof info.tlyric == 'string' && info.tlyric.length < 5120) ? info.tlyric : null,
      rlyric: (typeof info.rlyric == 'string' && info.rlyric.length < 5120) ? info.rlyric : null,
      lxlyric: (typeof info.lxlyric == 'string' && info.lxlyric.length < 8192) ? info.lxlyric : null,
    }
  }

  const requestQueue = new Map()
  let isInitedApi = false
  let isShowedUpdateAlert = false

  const sendNativeRequest = (url, options, callback) => {
    const requestKey = Math.random().toString()
    const requestInfo = {
      aborted: false,
      abort: () => {
        nativeCall(NATIVE_EVENTS_NAMES.cancelRequest, requestKey)
      },
    }
    requestQueue.set(requestKey, {
      callback,
      requestInfo,
    })

    nativeCall(NATIVE_EVENTS_NAMES.request, { requestKey, url, options })
    return requestInfo
  }
  const handleNativeResponse = ({ requestKey, error, response }) => {
    const targetRequest = requestQueue.get(requestKey)
    if (!targetRequest) return
    requestQueue.delete(requestKey)
    targetRequest.requestInfo.aborted = true
    if (error == null) {
      nativeCall('__lx_log__', { msg: 'handleNativeResponse: error=null, body.type=' + (typeof response.body) + ', body.code=' + JSON.stringify(response.body?.code) + ', statusCode=' + response.statusCode })
      targetRequest.callback(null, response)
    }
    else targetRequest.callback(new Error(error), null)
  }

  const handleRequest = ({ requestKey, data }) => {
    if (!events.request) return nativeCall(NATIVE_EVENTS_NAMES.response, { requestKey, status: false, errorMessage: 'Request event is not defined' })
    try {
      events.request.call(globalThis.lx, { source: data.source, action: data.action, info: data.info }).then(response => {
        let result
        switch (data.action) {
          case 'musicUrl':
            if (typeof response != 'string' || response.length > 2048 || !/^https?:/.test(response)) throw new Error('failed')
            result = {
              source: data.source,
              action: data.action,
              data: {
                type: data.info.type,
                url: response,
              },
            }
            break
          case 'lyric':
            result = {
              source: data.source,
              action: data.action,
              data: verifyLyricInfo(response),
            }
            break
          case 'pic':
            if (typeof response != 'string' || response.length > 2048 || !/^https?:/.test(response)) throw new Error('failed')
            result = {
              source: data.source,
              action: data.action,
              data: response,
            }
            break
        }
        nativeCall(NATIVE_EVENTS_NAMES.response, { requestKey, status: true, result })
      }).catch(err => {
        nativeCall(NATIVE_EVENTS_NAMES.response, { requestKey, status: false, errorMessage: err.message })
      })
    } catch (err) {
      nativeCall(NATIVE_EVENTS_NAMES.response, { requestKey, status: false, errorMessage: err.message })
    }
  }

  const jsCall = (action, data) => {
    switch (action) {
      case '__run_error__':
        if (!isInitedApi) isInitedApi = true
        return
      case '__set_timeout__':
        handleSetTimeout(data)
        return
      case 'request':
        handleRequest(data)
        return
      case 'response':
        handleNativeResponse(data)
        return
    }
    return 'Unknown action: ' + action
  }

  Object.defineProperty(globalThis, '__lx_native__', {
    enumerable: false,
    configurable: false,
    writable: false,
    value: (_key, action, data) => {
      if (key != _key) return 'Invalid key'
      return data == null ? jsCall(action) : jsCall(action, JSON.parse(data))
    },
  })


  const handleInit = (info) => {
    if (!info) {
      nativeCall(NATIVE_EVENTS_NAMES.init, { info: null, status: false, errorMessage: 'Missing required parameter init info' })
      return
    }
    const sourceInfo = {
      sources: {},
    }
    try {
      for (const source of allSources) {
        const userSource = info.sources[source]
        if (!userSource || userSource.type !== 'music') continue
        const qualitys = supportQualitys[source]
        const actions = supportActions[source]
        sourceInfo.sources[source] = {
          type: 'music',
          actions: actions.filter(a => userSource.actions.includes(a)),
          qualitys: qualitys.filter(q => userSource.qualitys.includes(q)),
        }
      }
    } catch (error) {
      nativeCall(NATIVE_EVENTS_NAMES.init, { info: null, status: false, errorMessage: error.message })
      return
    }
    nativeCall(NATIVE_EVENTS_NAMES.init, { info: sourceInfo, status: true })
  }
  const handleShowUpdateAlert = (data, resolve, reject) => {
    if (!data || typeof data != 'object') return reject(new Error('parameter format error.'))
    if (!data.log || typeof data.log != 'string') return reject(new Error('log is required.'))
    if (data.updateUrl && !/^https?:\/\/[^\s$.?#].[^\s]*$/.test(data.updateUrl) && data.updateUrl.length > 1024) delete data.updateUrl
    if (data.log.length > 1024) data.log = data.log.substring(0, 1024) + '...'
    nativeCall(NATIVE_EVENTS_NAMES.showUpdateAlert, { log: data.log, updateUrl: data.updateUrl, name })
    resolve()
  }

  const dataToB64 = (data) => {
    if (typeof data === 'string') return nativeFuncs.utils_str2b64(data)
    else if (Array.isArray(data) || ArrayBuffer.isView(data)) return utils.buffer.bufToString(data, 'base64')
    throw new Error('data type error: ' + typeof data + ' raw data: ' + data)
  }
  const utils = {
    crypto: {
      aesEncrypt(buffer, mode, key, iv) {
        switch (mode) {
          case 'aes-128-cbc':
            return utils.buffer.from(nativeFuncs.utils_aes_encrypt(dataToB64(buffer), dataToB64(key), dataToB64(iv), AES_MODE.CBC_128_PKCS7Padding), 'base64')
          case 'aes-128-ecb':
            return utils.buffer.from(nativeFuncs.utils_aes_encrypt(dataToB64(buffer), dataToB64(key), '', AES_MODE.ECB_128_NoPadding), 'base64')
          default:
            throw new Error('Binary encoding is not supported for input strings')
        }
      },
      rsaEncrypt(buffer, key) {
        if (typeof key !== 'string') throw new Error('Invalid RSA key')
        key = key.replace(KEY_PREFIX.publicKeyStart, '')
          .replace(KEY_PREFIX.publicKeyEnd, '')
        return utils.buffer.from(nativeFuncs.utils_rsa_encrypt(dataToB64(buffer), key, RSA_PADDING.NoPadding), 'base64')
      },
      randomBytes(size) {
        const byteArray = new Uint8Array(size)
        for (let i = 0; i < size; i++) {
          byteArray[i] = Math.floor(Math.random() * 256)
        }
        return byteArray
      },
      md5(str) {
        if (typeof str !== 'string') throw new Error('param required a string')
        const encoded = encodeURIComponent(str)
        const md5 = nativeFuncs.utils_str2md5(encoded)
        nativeCall('__lx_log__', { msg: 'crypto.md5: input=' + str.substring(0, 80) + ', encoded=' + encoded.substring(0, 80) + ', result=' + md5 })
        return md5
      },
      encrypt(data, format) {
        nativeCall('__lx_log__', { msg: 'crypto.encrypt called: data.type=' + typeof data + ', data=' + JSON.stringify(typeof data === 'string' ? data.substring(0, 80) : data) + ', format=' + format })
        let buf = data
        if (typeof data === 'string') {
          buf = utils.buffer.from(data, 'hex')
        }
        const result = utils.buffer.bufToString(buf, format)
        nativeCall('__lx_log__', { msg: 'crypto.encrypt result: ' + result.substring(0, 80) })
        return result
      },
    },
    buffer: {
      from(input, encoding) {
        if (typeof input === 'string') {
          switch (encoding) {
            case 'binary':
              throw new Error('Binary encoding is not supported for input strings')
            case 'base64':
              return new Uint8Array(JSON.parse(nativeFuncs.utils_b642buf(input)))
            case 'hex':
              return new Uint8Array(input.match(/.{1,2}/g).map(byte => parseInt(byte, 16)))
            default:
              return new Uint8Array(stringToBytes(input))
          }
        } else if (Array.isArray(input)) {
          return new Uint8Array(input)
        } else {
          throw new Error('Unsupported input type: ' + input + ' encoding: ' + encoding)
        }
      },
      bufToString(buf, format) {
        if (Array.isArray(buf) || ArrayBuffer.isView(buf)) {
          switch (format) {
            case 'binary':
              return buf
            case 'hex':
              return new Uint8Array(buf).reduce((str, byte) => str + byte.toString(16).padStart(2, '0'), '')
            case 'base64':
              return nativeFuncs.utils_str2b64(bytesToString(Array.from(buf)))
            case 'utf8':
            case 'utf-8':
            default:
              return bytesToString(Array.from(buf))
          }
        } else {
          throw new Error('Input is not a valid buffer: ' + buf + ' format: ' + format)
        }
      },
    },
  }

  globalThis.lx = {
    EVENT_NAMES,
    request(url, { method = 'get', timeout, headers, body, form, formData, binary }, callback) {
      let options = { headers, binary: binary === true }
      if (timeout && typeof timeout == 'number' && timeout > 0) options.timeout = Math.min(timeout, 60_000)

      let request = sendNativeRequest(url, { method, body, form, formData, ...options }, (err, resp) => {
        nativeCall('__lx_log__', { msg: 'lx.request wrapper: err=' + JSON.stringify(err) + ', resp.statusCode=' + (resp?.statusCode) + ', resp.body.type=' + (typeof resp?.body) + ', resp.body=' + JSON.stringify(resp?.body) })
        if (err) {
          callback(err, null, null)
        } else {
          callback(err, {
            statusCode: resp.statusCode,
            statusMessage: resp.statusMessage,
            headers: resp.headers,
            body: resp.body,
          }, resp.body)
        }
      })

      return () => {
        if (!request.aborted) request.abort()
        request = null
      }
    },
    send(eventName, data) {
      return new Promise((resolve, reject) => {
        if (!eventNames.includes(eventName)) return reject(new Error('The event is not supported: ' + eventName))
        switch (eventName) {
          case EVENT_NAMES.inited:
            if (isInitedApi) return reject(new Error('Script is inited'))
            isInitedApi = true
            handleInit(data)
            resolve()
            break
          case EVENT_NAMES.updateAlert:
            if (isShowedUpdateAlert) return reject(new Error('The update alert can only be called once.'))
            isShowedUpdateAlert = true
            handleShowUpdateAlert(data, resolve, reject)
            break
          default:
            reject(new Error('Unknown event name: ' + eventName))
        }
      })
    },
    on(eventName, handler) {
      if (!eventNames.includes(eventName)) return Promise.reject(new Error('The event is not supported: ' + eventName))
      switch (eventName) {
        case EVENT_NAMES.request:
          events.request = handler
          break
        default: return Promise.reject(new Error('The event is not supported: ' + eventName))
      }
      return Promise.resolve()
    },
    utils,
    currentScriptInfo: {
      key: key,
      id: id,
      name: name,
      description: description,
      version: version,
      author: author,
      homepage: homepage,
      rawScript: rawScript,
    },
    version: version,
    env: 'mobile',
  }

  globalThis.setTimeout = _setTimeout
  globalThis.clearTimeout = _clearTimeout

  const freezeObject = (obj) => {
    if (typeof obj != 'object') return
    Object.freeze(obj)
    for (const subObj of Object.values(obj)) freezeObject(subObj)
  }
  freezeObject(globalThis.lx)

  const _toString = Function.prototype.toString
  Function.prototype.toString = function() {
    return Object.getOwnPropertyDescriptors(this).name.configurable
      ? _toString.apply(this)
      : 'function ' + this.name + '() { [native code] }'
  }
  globalThis.eval = function() {
    throw new Error('eval is not available')
  }
  const proxyFunctionConstructor = new Proxy(Function.prototype.constructor, {
    apply() {
      throw new Error('Dynamic code execution is not allowed.')
    },
    construct() {
      throw new Error('Dynamic code execution is not allowed.')
    },
  })
  Object.defineProperty(Function.prototype, 'constructor', {
    value: proxyFunctionConstructor,
    writable: false,
    configurable: false,
    enumerable: false,
  })
  globalThis.Function = proxyFunctionConstructor

  const excludes = [
    Function.prototype.toString,
    Function.prototype.toLocaleString,
    Object.prototype.toString,
  ]
  const freezeObjectProperty = (obj, freezedObj = new Set()) => {
    if (obj == null) return
    switch (typeof obj) {
      case 'object':
      case 'function':
        if (freezedObj.has(obj)) return
        freezedObj.add(obj)
        for (const [name, { ...config }] of Object.entries(Object.getOwnPropertyDescriptors(obj))) {
          if (!excludes.includes(config.value)) {
            if (config.writable) config.writable = false
            if (config.configurable) config.configurable = false
            Object.defineProperty(obj, name, config)
          }
          freezeObjectProperty(config.value, freezedObj)
        }
    }
  }
  freezeObjectProperty(globalThis)

  console.log('Preload finished.')
}

globalThis.lx_setup($key, $key, $name, $desc, $ver, $author, $homepage, $rawScript)
        """.trimIndent()
    }

    private fun String.escapeJs(): String {
        return this
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\"", "\\\"")
    }
}
