package com.bicy.whitenoise.onlinemusic

import android.content.Context
import android.os.Environment
import android.util.Log
import com.bicy.whitenoise.music.MusicLibraryPart.MusicLibrary
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.music.MusicLibraryPart.MusicTrack
import com.bicy.whitenoise.onlinemusic.model.SourceModelsPart.MusicInfoOnline
import com.bicy.whitenoise.onlinemusic.model.SourceModelsPart.ScriptInfo
import com.bicy.whitenoise.ui.components.toast.ToastManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File

/**
 * 在线音乐控制器：串联 音源脚本 → 下载 → 播放 全链路。
 *
 * 所有在线音频统一持久化到下载目录，不再区分缓存/下载。
 *
 * 使用方式：
 *   val controller = OnlineMusicController(context)
 *   controller.playOnline(musicInfo)  // 下载并播放
 */
class OnlineMusicController(private val appContext: Context) {

    companion object {
        private const val TAG = "OnlineMusicCtrl"

        /** 创建在线 MusicTrack */
        fun createOnlineTrack(
            musicInfo: MusicInfoOnline,
            filePath: String,
            streamUrl: String
        ): MusicTrack {
            val trackId = "online_${musicInfo.source}_${musicInfo.songId}"
            val durationMs = try {
                val interval = musicInfo.interval ?: ""
                val parts = interval.split(":")
                if (parts.size >= 2) {
                    val minutes = parts[0].toLongOrNull() ?: 0L
                    val seconds = parts[1].substringBefore(".").toLongOrNull() ?: 0L
                    (minutes * 60 + seconds) * 1000
                } else 0L
            } catch (_: Exception) { 0L }
            
            return MusicTrack(
                id = trackId,
                path = filePath,
                title = musicInfo.name,
                artist = musicInfo.singer,
                album = musicInfo.albumName,
                duration = durationMs,
                isOnline = true,
                streamUrl = streamUrl,
                source = musicInfo.source
            )
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** 统一下载目录（默认 Music/添空下载），所有在线音频均持久化到此 */
    val downloadDir: File by lazy {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "添空下载").also { it.mkdirs() }
    }
    private val scriptManager = SourceScriptManager.getInstance(appContext)

    /** 播放状态回调 */
    var onStatusChanged: ((OnlinePlayState) -> Unit)? = null

    sealed class OnlinePlayState {
        data object Idle : OnlinePlayState()
        data class Downloading(val url: String, val progress: Float = 0f) : OnlinePlayState()
        data class Buffering(val track: MusicTrack) : OnlinePlayState()
        data class Playing(val track: MusicTrack) : OnlinePlayState()
        data class Error(val message: String) : OnlinePlayState()
    }

    private var currentOnlineTrack: MusicTrack? = null

    /**
     * 播放在线歌曲。
     * 1. 获取播放 URL
     * 2. 创建 MusicTrack（带 streamUrl，路径指向下载目录）
     * 3. 通过 MusicPlayerController 播放（会自动下载到下载目录）
     */
    fun playOnline(
        musicInfo: MusicInfoOnline,
        scriptId: String? = null,
    ) {
        scope.launch {
            try {
                onStatusChanged?.invoke(OnlinePlayState.Downloading(musicInfo.name))
                ToastManager.loading("正在获取 ${musicInfo.name}...")

                if (!scriptManager.isLoaded()) {
                    Log.w(TAG, "脚本尚未加载，等待初始化...")
                    kotlinx.coroutines.delay(500)
                }

                val scripts = scriptManager.getScriptList()
                if (scripts.isEmpty()) {
                    onStatusChanged?.invoke(OnlinePlayState.Error("没有可用的音源脚本，请先导入"))
                    ToastManager.fail("没有可用的音源脚本，请先导入")
                    return@launch
                }

                val streamUrl = getMusicUrlSimple(musicInfo, scripts) { current, total, scriptName ->
                    // 切换音源时刷新 LOADING 文案，让用户看到当前进度
                    ToastManager.updateMessage("正在尝试音源 $current/$total: $scriptName")
                }
                if (streamUrl.isNullOrBlank()) {
                    onStatusChanged?.invoke(OnlinePlayState.Error("无法获取播放链接"))
                    ToastManager.fail("无法获取播放链接")
                    return@launch
                }

                Log.d(TAG, "Got streamUrl: $streamUrl")

                val baseName = sanitizeFilename("${musicInfo.name} - ${musicInfo.singer.takeIf { it.isNotBlank() } ?: "未知歌手"}")
                val filePath = File(downloadDir, "${baseName}.mp3").absolutePath

                // 文件已存在（之前播放/下载过）：直接播放本地文件，跳过下载
                if (File(filePath).exists()) {
                    Log.d(TAG, "文件已存在，直接播放: $filePath")
                    ToastManager.complete("${musicInfo.name}（本地已有）")
                    val localTrack = MusicTrack(
                        id = filePath,
                        path = filePath,
                        title = musicInfo.name,
                        artist = musicInfo.singer,
                        album = musicInfo.albumName,
                        duration = parseInterval(musicInfo.interval),
                        isOnline = false,
                        streamUrl = null,
                        source = null,
                        dateAdded = System.currentTimeMillis(),
                        albumArt = null,
                        mediaStoreId = 0
                    )
                    currentOnlineTrack = localTrack
                    MusicPlayerController.setPlaylist(listOf(localTrack), 0)
                    MusicPlayerController.play()
                    MusicLibrary.addOrUpdateTrack(localTrack)
                    // 持久化在线音乐元数据（兼容旧下载，重启后可从本地恢复）
                    scope.launch(Dispatchers.IO) {
                        OnlineMusicStorage.saveOnlineTrack(localTrack.copy(isOnline = true))
                    }
                    onStatusChanged?.invoke(OnlinePlayState.Playing(localTrack))
                    return@launch
                }

                val track = createOnlineTrack(musicInfo, filePath, streamUrl)
                currentOnlineTrack = track

                MusicPlayerController.setPlaylist(listOf(track), 0)
                MusicPlayerController.play()

                // 增量写入音乐库
                MusicLibrary.addOrUpdateTrack(MusicTrack(
                    id = filePath,
                    path = filePath,
                    title = musicInfo.name,
                    artist = musicInfo.singer,
                    album = musicInfo.albumName,
                    duration = parseInterval(musicInfo.interval),
                    isOnline = false,
                    streamUrl = null,
                    source = null,
                    dateAdded = System.currentTimeMillis(),
                    albumArt = null,
                    mediaStoreId = 0
                ))

                // 持久化在线音乐元数据（歌名/歌手/专辑/streamUrl 等）
                scope.launch(Dispatchers.IO) { OnlineMusicStorage.saveOnlineTrack(track) }

                onStatusChanged?.invoke(OnlinePlayState.Playing(track))
                ToastManager.complete("正在播放：${musicInfo.name}")
            } catch (e: CancellationException) {
                onStatusChanged?.invoke(OnlinePlayState.Error("操作被取消"))
                ToastManager.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "playOnline failed", e)
                onStatusChanged?.invoke(OnlinePlayState.Error(e.message ?: "未知错误"))
                // 用 fail 而非 error：确保 LOADING toast 原地转为 ERROR，
                // 避免出现 "正在获取" 一直卡住、错误提示又叠加上去的问题
                ToastManager.fail("播放在线音乐失败：${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 简化的获取音乐 URL 逻辑
     * 脚本已在启动时全部激活，直接请求即可
     *
     * 引擎切换时通过 [onEngineSwitch] 回调通知上层（用于更新 UI 提示）。
     */
    private suspend fun getMusicUrlSimple(
        musicInfo: MusicInfoOnline,
        scripts: List<ScriptInfo>,
        onEngineSwitch: ((current: Int, total: Int, scriptName: String) -> Unit)? = null,
    ): String? {
        val infoJson = buildMusicInfoJson(musicInfo)
        val source = musicInfo.source

        Log.d(TAG, "获取播放 URL: ${musicInfo.name} from ${source}")
        Log.d(TAG, "请求参数: $infoJson")
        val result = scriptManager.getMusicUrl(source, infoJson, onEngineSwitch)
        Log.d(TAG, "脚本返回原始结果: $result")

        if (!result.isNullOrBlank()) {
            val url = extractUrl(result)
            Log.d(TAG, "提取的 URL: $url")
            if (!url.isNullOrBlank()) {
                Log.i(TAG, "获取成功: $url")
                return url
            }
        }
        return null
    }

    /**
     * 从脚本返回结果中提取 URL
     * 支持两种格式：
     * 1. JSON 格式: { "data": { "url": "http://..." } }
     * 2. 直接返回 URL 字符串
     */
    private fun extractUrl(result: String): String? {
        val trimmed = result.trim()

        // 尝试解析 JSON 格式
        var url: String? = null
        try {
            val json = JSONObject(trimmed)
            // 格式1: { "data": { "url": "..." } }
            url = json.optJSONObject("data")?.optString("url")
                // 格式2: { "url": "..." }
                ?: json.optString("url", "")
        } catch (e: Exception) {
            // JSON 解析失败，可能就是普通字符串
            url = if (trimmed.isNotEmpty()) trimmed else null
        }

        // 验证 URL 格式
        if (url.isNullOrBlank()) return null
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        // 检查 URL 是否完整（至少包含主机名）
        val hostStart = url.indexOf("://") + 3
        if (hostStart >= url.length) return null
        val hostPart = url.substring(hostStart).split("/", "?", "#").first()
        if (hostPart.isBlank() || !hostPart.contains(".")) return null

        return url
    }
    
    private fun buildMusicInfoJson(musicInfo: MusicInfoOnline): String {
        // 构建符合 lx-music 协议的 meta 对象
        val metaJson = JSONObject().apply {
            put("songId", musicInfo.songId)
            put("albumName", musicInfo.albumName)
            put("albumId", musicInfo.albumId ?: "")
            put("picUrl", musicInfo.picUrl ?: "")
            // 平台特定字段
            if (musicInfo.hash.isNotEmpty()) put("hash", musicInfo.hash)
            if (musicInfo.strMediaMid.isNotEmpty()) put("strMediaMid", musicInfo.strMediaMid)
            if (musicInfo.albumMid.isNotEmpty()) put("albumMid", musicInfo.albumMid)
        }
        
        // 构建 musicInfo 对象（符合 LX.Music.MusicInfo 结构）
        val musicInfoJson = JSONObject().apply {
            put("id", musicInfo.id)
            put("name", musicInfo.name)
            put("singer", musicInfo.singer)
            put("source", musicInfo.source)
            put("interval", musicInfo.interval ?: "")
            put("meta", metaJson)
            // 平台特定 ID 字段（脚本通过 musicInfo.songmid/musicInfo.hash/musicInfo.songId 提取）
            if (musicInfo.songmid.isNotEmpty()) put("songmid", musicInfo.songmid)
            if (musicInfo.hash.isNotEmpty()) put("hash", musicInfo.hash)
            if (musicInfo.songId.isNotEmpty()) put("songId", musicInfo.songId)
        }
        
        // 最终请求参数：{ type, musicInfo }
        // 同时在顶层添加关键字段，兼容不同的脚本解析方式
        return JSONObject().apply {
            put("type", "320k")  // 默认请求 320k 音质
            put("musicInfo", musicInfoJson)
            // 兼容层：在顶层也放置关键字段
            put("id", musicInfo.id)
            put("songId", musicInfo.songId)
            put("songmid", musicInfo.songmid)
            put("hash", musicInfo.hash)
            put("name", musicInfo.name)
            put("singer", musicInfo.singer)
            put("source", musicInfo.source)
            put("albumName", musicInfo.albumName)
        }.toString()
    }

    /** 获取当前播放的在线歌曲 */
    fun getCurrentOnlineTrack(): MusicTrack? = currentOnlineTrack

    /** 清理 */
    fun destroy() {
        scope.cancel()
        scriptManager.destroy()
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|]"""), "_")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(100)
    }

    /** 解析时间字符串 ("mm:ss" 或 "mm:ss.xx") → 毫秒 */
    private fun parseInterval(interval: String?): Long {
        if (interval.isNullOrBlank()) return 0L
        try {
            val parts = interval.split(":")
            if (parts.size >= 2) {
                val minutes = parts[0].toLongOrNull() ?: 0L
                val seconds = parts[1].substringBefore(".").toLongOrNull() ?: 0L
                return (minutes * 60 + seconds) * 1000
            }
        } catch (_: Exception) {}
        return 0L
    }
}