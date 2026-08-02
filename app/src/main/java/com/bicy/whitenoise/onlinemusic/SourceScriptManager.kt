package com.bicy.whitenoise.onlinemusic

import android.content.Context
import android.util.Base64
import android.util.Log
import com.bicy.whitenoise.onlinemusic.model.*
import com.bicy.whitenoise.onlinemusic.model.SourceModelsPart.*
import com.bicy.whitenoise.ui.components.toast.ToastManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.Inflater

class SourceScriptManager private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "SourceScriptManager"
        private const val SCRIPT_DIR = "source_scripts"

        private val INFO_LIMITS = mapOf(
            "name" to 24,
            "description" to 36,
            "author" to 56,
            "homepage" to 1024,
            "version" to 36,
        )
        
        @Volatile
        private var INSTANCE: SourceScriptManager? = null
        
        fun getInstance(context: Context): SourceScriptManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SourceScriptManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val engines = mutableMapOf<String, SourceScriptEngine>()  // 每个脚本一个引擎
    private val scripts = mutableMapOf<String, ScriptInfo>()
    private var isLoaded = false
    private val scriptCapabilities = mutableMapOf<String, SourceCapabilities>()  // 缓存每个脚本的能力

    /** 异步加载磁盘脚本并全部激活 */
    fun initAsync(onReady: (() -> Unit)? = null) {
        if (isLoaded) {
            onReady?.invoke()
            return
        }
        scope.launch {
            loadAllScripts()
            // 激活所有脚本
            activateAllScripts()
            isLoaded = true
            withContext(Dispatchers.Main) {
                onReady?.invoke()
            }
        }
    }

    /** 激活所有已加载的脚本 */
    private suspend fun activateAllScripts() {
        for ((id, info) in scripts) {
            try {
                Log.d(TAG, "🚀 开始激活脚本: ${info.name}")
                
                val engine = SourceScriptEngine()
                val deferred = kotlinx.coroutines.CompletableDeferred<SourceCapabilities?>()
                
                engine.onInited = { sourcesJson ->
                    try {
                        Log.w(TAG, "✅ 脚本 ${info.name} 初始化返回: $sourcesJson")
                        val caps = parseCapabilities(sourcesJson)
                        scriptCapabilities[id] = caps
                        Log.w(TAG, "✅ 脚本 ${info.name} 支持平台: ${caps.sources.keys.joinToString()}")
                        deferred.complete(caps)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 解析脚本 ${info.name} 能力失败", e)
                        ToastManager.error("「${info.name}」解析失败")
                        deferred.complete(null)
                    }
                }
                
                engine.onError = { msg ->
                    Log.w(TAG, "❌ 脚本 ${info.name} 激活失败: $msg")
                    ToastManager.error("「${info.name}」激活失败")
                    deferred.complete(null)
                }
                
                engine.init(info)
                engines[id] = engine
                
                // 添加超时，避免脚本初始化卡住
                try {
                    withTimeout(10_000) {
                        deferred.await()
                    }
                    Log.d(TAG, "✅ 脚本 ${info.name} 已激活")
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    Log.w(TAG, "⚠️ 脚本 ${info.name} 初始化超时，跳过")
                    ToastManager.error("「${info.name}」初始化超时")
                    if (!deferred.isCompleted) {
                        deferred.complete(null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 激活脚本 ${info.name} 失败", e)
                ToastManager.error("「${info.name}」激活出错")
            }
        }
    }

    /** 当前激活的音源能力（兼容旧接口） */
    var capabilities: SourceCapabilities? = null
        private set

    /** 脚本状态回调 */
    var onStatusChanged: ((isActive: Boolean, message: String?) -> Unit)? = null

    // ======================== 脚本管理 ========================

    /** 从文件导入脚本 */
    fun importScript(file: File): ScriptInfo {
        val rawScript = file.readText(Charsets.UTF_8)
        return importScript(rawScript)
    }

    /** 从字符串导入脚本 */
    fun importScript(rawScript: String): ScriptInfo {
        val header = parseScriptHeader(rawScript)
        val id = "user_api_${UUID.randomUUID().toString().take(4)}_${System.currentTimeMillis()}"
        val info = ScriptInfo(
            id = id,
            name = header["name"] ?: "未命名音源",
            description = header["description"] ?: "",
            version = header["version"] ?: "",
            author = header["author"] ?: "",
            homepage = header["homepage"] ?: "",
            rawScript = rawScript,
        )

        // 去重检查
        for (existing in scripts.values) {
            if (compressScript(existing.rawScript) == compressScript(rawScript)) {
                throw IllegalStateException("导入失败，脚本内容与已有的源「${existing.name}」相同")
            }
        }

        scripts[info.id] = info
        saveScript(info.id, rawScript)
        Log.d(TAG, "Imported script: ${info.name} v${info.version}")
        ToastManager.success("「${info.name}」已导入")
        // 导入后立即激活
        scope.launch {
            try {
                activateScript(info.id)
                Log.d(TAG, "脚本 ${info.name} 导入后已自动激活")
            } catch (e: Exception) {
                Log.e(TAG, "导入后激活失败 ${info.name}: ${e.message}")
                ToastManager.error("「${info.name}」激活失败")
            }
        }
        return info
    }

    /** 移除脚本 */
    fun removeScript(id: String) {
        engines[id]?.close()
        engines.remove(id)
        scripts.remove(id)
        scriptCapabilities.remove(id)
        deleteScriptFile(id)
    }

    /** 获取所有已导入的脚本列表（不含 rawScript） */
    fun getScriptList(): List<ScriptInfo> {
        return scripts.values.map { it.copy(rawScript = "") }
    }

    /** 获取脚本数量 */
    fun getScriptCount(): Int = scripts.size

    /** 检查脚本是否已加载 */
    fun isLoaded(): Boolean = isLoaded

    /** 检查是否有已激活的脚本 */
    fun isScriptActive(): Boolean = engines.isNotEmpty()

    /** 获取已激活的脚本数量 */
    fun getActiveScriptCount(): Int = engines.size

    // ======================== 引擎管理 ========================

    /** 激活指定音源脚本（如果已激活则直接返回） */
    suspend fun activateScript(id: String): Result<SourceCapabilities> {
        // 如果已经激活，返回缓存的能力
        val existingCaps = scriptCapabilities[id]
        if (existingCaps != null && engines[id]?.isRunning == true) {
            return Result.success(existingCaps)
        }

        val info = scripts[id] ?: return Result.failure(IllegalArgumentException("Script not found: $id"))

        val newEngine = SourceScriptEngine()
        val deferred = CompletableDeferred<SourceCapabilities?>()

        newEngine.onInited = { sourcesJson ->
            try {
                val caps = parseCapabilities(sourcesJson)
                scriptCapabilities[id] = caps
                deferred.complete(caps)
                Log.d(TAG, "Script inited: ${info.name}")
            } catch (e: Exception) {
                deferred.complete(null)
            }
        }

        newEngine.onError = { msg ->
            Log.e(TAG, "Script error: $msg")
            deferred.complete(null)
            onStatusChanged?.invoke(false, msg)
        }

        newEngine.onUpdateAlert = { data ->
            Log.d(TAG, "Update alert: $data")
        }

        try {
            newEngine.init(info)
            engines[id] = newEngine
            val caps = withTimeout(10_000) { deferred.await() }
            if (caps != null) {
                onStatusChanged?.invoke(true, null)
                return Result.success(caps)
            } else {
                return Result.failure(RuntimeException("Script init failed"))
            }
        } catch (e: Exception) {
            newEngine.close()
            engines.remove(id)
            onStatusChanged?.invoke(false, e.message)
            return Result.failure(e)
        }
    }

    /** 单个引擎请求的最长等待时间（毫秒），超时即切换下一个引擎 */
    private val ENGINE_REQUEST_TIMEOUT_MS = 10_000L

    /**
     * 向支持指定平台的脚本发起请求。
     *
     * 引擎切换策略：
     *  - 先收集所有支持该平台的候选引擎
     *  - 依次尝试，每个引擎最多等待 [ENGINE_REQUEST_TIMEOUT_MS]
     *  - 单个引擎失败/超时/返回空 都记录原因后继续下一个
     *  - 某个引擎成功立即返回；全部失败才抛汇总异常
     *
     * @param onEngineSwitch 切换到新引擎时的回调：(current, total, scriptName) -> Unit
     *        current 从 1 开始计数；可用于更新 UI 提示
     */
    suspend fun request(
        source: String,
        action: String,
        info: Any,
        onEngineSwitch: ((current: Int, total: Int, scriptName: String) -> Unit)? = null,
    ): String? {
        val infoJson = when (info) {
            is String -> info
            else -> info.toString()
        }

        // 先收集所有候选引擎，便于回调中给出 total
        data class Candidate(val scriptId: String, val scriptName: String, val engine: SourceScriptEngine)
        val candidates = mutableListOf<Candidate>()
        for ((scriptId, engine) in engines) {
            val scriptName = scripts[scriptId]?.name ?: scriptId
            val caps = scriptCapabilities[scriptId]
            // 如果有能力缓存但明确不支持该平台，跳过
            if (caps != null && caps.sources.keys.none { it == source }) {
                continue
            }
            candidates.add(Candidate(scriptId, scriptName, engine))
        }

        val tried = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val total = candidates.size

        for ((index, candidate) in candidates.withIndex()) {
            val scriptName = candidate.scriptName
            val current = index + 1
            tried.add(scriptName)
            // 通知上层：正在尝试第 current/total 个引擎
            onEngineSwitch?.invoke(current, total, scriptName)

            try {
                // 每个引擎最多等待 10 秒，超时也算失败并切换下一个
                val result = withTimeout(ENGINE_REQUEST_TIMEOUT_MS) {
                    candidate.engine.handleRequest(source, action, infoJson)
                }
                if (!result.isNullOrBlank()) {
                    Log.d(TAG, "✅ 脚本 $scriptName 请求成功 ($source/$action)")
                    return result
                }
                failed.add("$scriptName: 返回空结果")
                Log.w(TAG, "脚本 $scriptName 返回空结果 ($source/$action)")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                failed.add("$scriptName: 请求超时(${ENGINE_REQUEST_TIMEOUT_MS}ms)")
                Log.w(TAG, "⏰ 脚本 $scriptName 请求超时 ($source/$action)")
            } catch (e: Exception) {
                failed.add("$scriptName: ${e.message}")
                Log.w(TAG, "脚本 $scriptName 请求失败 ($source/$action): ${e.message}")
            }
        }

        val detail = if (tried.isEmpty()) "未找到任何已激活脚本"
        else "已尝试: ${tried.joinToString("; ")}，失败原因: ${failed.joinToString("; ")}"
        throw IllegalStateException("$source 平台请求失败: $detail")
    }

    /** 获取音乐播放 URL */
    suspend fun getMusicUrl(
        source: String,
        info: Any,
        onEngineSwitch: ((current: Int, total: Int, scriptName: String) -> Unit)? = null,
    ): String? {
        return request(source, SourceAction.MUSIC_URL, info, onEngineSwitch)
    }

    /** 获取歌词 */
    suspend fun getLyric(source: String, info: Any): String? {
        return request(source, SourceAction.LYRIC, info)
    }

    /** 获取封面 */
    suspend fun getPic(source: String, info: Any): String? {
        return request(source, SourceAction.PIC, info)
    }

    /** 搜索在线音乐 */
    suspend fun searchMusic(source: String, keyword: String, page: Int = 1): List<MusicInfoOnline> {
        val searchInfo = JSONObject().apply {
            put("source", source)
            put("keyword", keyword)
            put("page", page)
            put("limit", 30)
        }
        val raw = request(source, SourceAction.SEARCH, searchInfo.toString()) ?: return emptyList()
        return parseSearchResults(raw)
    }

    /** 销毁 */
    fun destroy() {
        engines.values.forEach { it.close() }
        engines.clear()
        scriptCapabilities.clear()
        scope.cancel()
    }

    // ======================== 脚本头解析 ========================

    /**
     * 解析 JSDoc 头注释：
     *   /*
     *    * @name 音源名称
     *    * @description 描述
     *    * @author 作者
     *    * @homepage https://...
     *    * @version 1.0.0
     *    */
     */
    private fun parseScriptHeader(script: String): Map<String, String> {
        val headerRxp = Regex("""^/\*[\s\S]+?\*/""")
        val headerMatch = headerRxp.find(script)
            ?: throw IllegalArgumentException("无效的自定义源文件：缺少头注释")

        val infoRxp = Regex("""^\s?\*\s?@(\w+)\s(.+)$""")
        val result = mutableMapOf<String, String>()

        for (line in headerMatch.value.lines()) {
            val match = infoRxp.find(line) ?: continue
            val key = match.groupValues[1]
            val value = match.groupValues[2].trim()
            val limit = INFO_LIMITS[key] ?: continue
            result[key] = if (value.length > limit) value.take(limit) + "..." else value
        }

        if (!result.containsKey("name")) {
            result["name"] = "未命名音源"
        }
        return result
    }

    // ======================== 能力解析 ========================

    /**
     * 解析脚本 inited 时上报的 sources JSON，过滤并校验
     */
    private fun parseCapabilities(sourcesJson: String): SourceCapabilities {
        val data = JSONObject(sourcesJson)
        // sources 嵌套在 info 内（lx.send('inited', { info: { sources: {...} }, status: true })）
        val sourcesObj = data.optJSONObject("info")?.optJSONObject("sources")
            ?: data.optJSONObject("sources")
            ?: JSONObject()
        val filtered = mutableMapOf<String, SourceInfo>()

        for (source in Sources.ALL) {
            val srcObj = sourcesObj.optJSONObject(source) ?: continue
            if (srcObj.optString("type") != "music") continue

            val actionsRaw = srcObj.optJSONArray("actions") ?: continue
            val actions = (0 until actionsRaw.length()).map { actionsRaw.getString(it) }
            val supportActions = SUPPORT_ACTIONS[source] ?: emptyList()
            val filteredActions = actions.filter { it in supportActions }

            val qualitysRaw = srcObj.optJSONArray("qualitys") ?: continue
            val qualitys = (0 until qualitysRaw.length()).map { qualitysRaw.getString(it) }
            val supportQualities = SUPPORT_QUALITIES[source] ?: emptyList()
            val filteredQualitys = qualitys.filter { it in supportQualities }

            filtered[source] = SourceInfo(
                type = "music",
                actions = filteredActions,
                qualitys = filteredQualitys,
            )
        }

        return SourceCapabilities(sources = filtered)
    }

    // ======================== 搜索结果解析 ========================

    /**
     * 解析脚本搜索返回的 JSON，提取 MusicInfoOnline 列表
     * LX Music 脚本返回格式：{ "list": [{ ... }], "total": N, "limit": N, "source": "kw" }
     */
    private fun parseSearchResults(raw: String): List<MusicInfoOnline> {
        val data = JSONObject(raw)
        val list = data.optJSONArray("list") ?: return emptyList()
        val results = mutableListOf<MusicInfoOnline>()
        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            results.add(MusicInfoOnline(
                id = item.optString("id", ""),
                name = item.optString("name", item.optString("title", "")),
                singer = item.optString("singer", item.optString("artist", "")),
                source = item.optString("source", data.optString("source", "")),
                interval = item.optString("interval", ""),
                songmid = item.optString("songmid", ""),
                hash = item.optString("hash", ""),
                songId = item.optString("songId", item.optString("id", "")),
                albumName = item.optString("albumName", item.optString("album", "")),
                albumId = item.optString("albumId", ""),
                picUrl = item.optString("picUrl", item.optString("img", "")),
            ))
        }
        return results
    }

    // ======================== 脚本持久化 ========================

    private fun getScriptDir(): File {
        val dir = File(appContext.filesDir, SCRIPT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun compressScript(script: String): String {
        val deflater = Deflater()
        deflater.setInput(script.toByteArray(Charsets.UTF_8))
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!deflater.finished()) out.write(buf, 0, deflater.deflate(buf))
        deflater.end()
        return "gz_" + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun decompressScript(compressed: String): String {
        if (!compressed.startsWith("gz_")) return compressed
        val data = Base64.decode(compressed.substring(3), Base64.NO_WRAP)
        val inflater = Inflater()
        inflater.setInput(data)
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!inflater.finished()) out.write(buf, 0, inflater.inflate(buf))
        inflater.end()
        return String(out.toByteArray(), Charsets.UTF_8)
    }

    private fun saveScript(id: String, rawScript: String) {
        val file = File(getScriptDir(), "$id.lxs")
        file.writeText(compressScript(rawScript), Charsets.UTF_8)
    }

    private fun deleteScriptFile(id: String) {
        File(getScriptDir(), "$id.lxs").delete()
    }

    /** 从磁盘加载所有已保存的脚本 */
    fun loadAllScripts() {
        val dir = getScriptDir()
        val lxsFiles = dir.listFiles { f -> f.name.endsWith(".lxs") } ?: emptyArray()
        for (file in lxsFiles) {
            try {
                val id = file.name.removeSuffix(".lxs")
                val compressed = file.readText(Charsets.UTF_8)
                val rawScript = decompressScript(compressed)
                val header = parseScriptHeader(rawScript)
                scripts[id] = ScriptInfo(
                    id = id,
                    name = header["name"] ?: "未命名",
                    description = header["description"] ?: "",
                    version = header["version"] ?: "",
                    author = header["author"] ?: "",
                    homepage = header["homepage"] ?: "",
                    rawScript = rawScript,
                )
                Log.d(TAG, "Loaded script: $id -> ${header["name"]}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load script: ${file.name}", e)
            }
        }
        
        // 首次启动时，将内置 .js 脚本导入为 .lxs 格式
        val jsFiles = dir.listFiles { f -> f.name.endsWith(".js") } ?: emptyArray()
        for (file in jsFiles) {
            try {
                val rawScript = file.readText(Charsets.UTF_8)
                importScript(rawScript)  // 解析元数据并保存为 .lxs
                Log.d(TAG, "Migrated builtin script: ${file.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to migrate script: ${file.name}", e)
            } finally {
                file.delete()  // 无论成功与否，删除原始 .js 文件避免重复处理
            }
        }
    }
}
