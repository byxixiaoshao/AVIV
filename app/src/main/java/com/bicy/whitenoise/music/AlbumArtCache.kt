package com.bicy.whitenoise.music

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.LruCache
import com.bicy.whitenoise.music.MusicLibraryPart.MusicTrack
import com.bicy.whitenoise.utils.AudioMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * 专辑封面本地缓存。
 *
 * 加载优先级：内存缓存 → 磁盘缓存 → 内嵌封面（从音频文件提取）→ 在线下载（暂未实现）。
 * 在线获取失败不触发应用内提醒（按需求）。
 *
 * UI 通过 [artFlow] 观察当前已加载的封面图，按 trackId 查询。
 */
object AlbumArtCache {

    private const val TAG = "AlbumArtCache"
    private const val DIR_NAME = "album_art"
    private const val MAX_MEMORY_BYTES = 16 * 1024 * 1024 // 16MB

    private lateinit var appContext: Context
    private lateinit var cacheDir: File

    /** 内存缓存：trackId -> ByteArray */
    private val memoryCache = object : LruCache<String, ByteArray>(MAX_MEMORY_BYTES) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    /** 正在加载的 trackId，避免重复请求 */
    private val loading = mutableSetOf<String>()

    /** 已处理完成的封面：trackId -> ByteArray?（null 表示确认无封面） */
    private val _artFlow = MutableStateFlow<Map<String, ByteArray?>>(emptyMap())
    val artFlow: StateFlow<Map<String, ByteArray?>> = _artFlow.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        cacheDir = File(appContext.filesDir, DIR_NAME).apply { mkdirs() }
    }

    /**
     * 异步请求封面：内存 → 磁盘 → 内嵌 → 在线（未实现）。
     * 结果会更新 [artFlow]，UI 可观察。
     * 同一 trackId 重复调用会被去重，避免抖动。
     */
    suspend fun requestAlbumArt(track: MusicTrack?) {
        if (track == null) return
        val trackId = track.id

        memoryCache.get(trackId)?.let { updateFlow(trackId, it); return }
        if (_artFlow.value.containsKey(trackId)) return // 已处理（含确认无封面）

        synchronized(loading) {
            if (trackId in loading) return
            loading.add(trackId)
        }

        try {
            val data = loadWithFallback(track)
            if (data != null) memoryCache.put(trackId, data)
            updateFlow(trackId, data)
        } finally {
            synchronized(loading) { loading.remove(trackId) }
        }
    }

    /** 同步获取：仅返回内存/磁盘已存在的封面，不触发任何 IO 读取（不含内嵌/在线） */
    fun getCached(trackId: String): ByteArray? {
        memoryCache.get(trackId)?.let { return it }
        val file = File(cacheDir, fileNameFor(trackId))
        return if (file.exists()) try { file.readBytes() } catch (_: Exception) { null } else null
    }

    /** 主动预加载到磁盘缓存（不更新 Flow，用于扫描后台预热） */
    suspend fun preloadToDisk(track: MusicTrack) = withContext(Dispatchers.IO) {
        val file = File(cacheDir, fileNameFor(track.id))
        if (file.exists()) return@withContext
        val embedded = extractEmbedded(track) ?: return@withContext
        try { file.writeBytes(embedded) } catch (e: Exception) {
            Log.e(TAG, "preloadToDisk failed: ${track.title}", e)
        }
    }

    private suspend fun loadWithFallback(track: MusicTrack): ByteArray? = withContext(Dispatchers.IO) {
        readFromDisk(track.id)?.let { return@withContext it }
        val embedded = extractEmbedded(track)
        if (embedded != null) writeToDisk(track.id, embedded)
        embedded
    }

    private fun extractEmbedded(track: MusicTrack): ByteArray? = try {
        val file = File(track.path)
        when {
            file.exists() -> AudioMetadataReader.getAlbumArt(file)
            track.uriString != null -> AudioMetadataReader.getAlbumArt(appContext, Uri.parse(track.uriString))
            else -> null
        }
    } catch (e: Exception) {
        Log.e(TAG, "extractEmbedded failed: ${track.title}", e); null
    }

    private fun readFromDisk(trackId: String): ByteArray? {
        val file = File(cacheDir, fileNameFor(trackId))
        return if (file.exists()) try { file.readBytes() } catch (_: Exception) { null } else null
    }

    private fun writeToDisk(trackId: String, data: ByteArray) {
        try { File(cacheDir, fileNameFor(trackId)).writeBytes(data) } catch (e: Exception) {
            Log.e(TAG, "writeToDisk failed: $trackId", e)
        }
    }

    private fun updateFlow(trackId: String, data: ByteArray?) {
        _artFlow.value = _artFlow.value + (trackId to data)
    }

    /** 生成安全的文件名：trackId 可能含特殊字符 */
    private fun fileNameFor(trackId: String): String {
        val safe = trackId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        return if (safe.length == trackId.length) "$safe.jpg" else {
            val md = MessageDigest.getInstance("MD5").digest(trackId.toByteArray())
            md.joinToString("") { "%02x".format(it) } + ".jpg"
        }
    }

    /** 清除指定曲目的缓存 */
    fun evict(trackId: String) {
        memoryCache.remove(trackId)
        File(cacheDir, fileNameFor(trackId)).delete()
        _artFlow.value = _artFlow.value - trackId
    }
}
