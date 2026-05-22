package com.bicy.whitenoise.y10p

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.bicy.whitenoise.R
import com.bicy.whitenoise.rgRE.MusicService
import com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundMetadata
import com.bicy.whitenoise.DzBD.IrBh.XsdL.ScatteredSoundWithType
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

enum class DownloadType {
    WHITE_NOISE,
    SCATTERED_NOISE,
    OTHER
}

object DownloadManager {
    
    private const val TAG = "DownloadManager"
    private const val CACHE_DIR = "audio_cache"
    private const val WHITE_NOISE_DIR = "white_noise/library"
    private const val SCATTERED_DIR = "white_noise/scattered"
    private const val OTHER_DIR = "other_audio"
    private const val AUDIO_FILE = "audio"
    private const val CONNECT_TIMEOUT_MS = 15_000L
    private const val READ_TIMEOUT_MS = 60_000L
    private const val MAX_RETRY_COUNT = 3
    
    private val downloadingSounds = ConcurrentHashMap<String, Job>()
    private val downloadProgress = ConcurrentHashMap<String, Float>()
    private val handler = Handler(Looper.getMainLooper())
    
    private var contextRef: WeakReference<Context>? = null
    private lateinit var okHttpClient: OkHttpClient
    
    fun init(context: Context) {
        contextRef = WeakReference(context.applicationContext)
        
        try {
            val builder = OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    val sslContext = SSLContext.getInstance("TLSv1.2")
                    sslContext.init(null, null, null)
                    
                    val sslSocketFactory = sslContext.socketFactory
                    
                    val trustManager = object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                    }
                    
                    builder.sslSocketFactory(sslSocketFactory, trustManager)
                    
                    Log.d(TAG, "Enabled TLS 1.2 for Android < 5.1")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to enable TLS 1.2: ${e.message}")
                }
            }
            
            okHttpClient = builder.build()
            
            Log.d(TAG, "DownloadManager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DownloadManager: ${e.message}")
            okHttpClient = OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
    
    fun isDownloading(soundId: String): Boolean {
        return downloadingSounds.containsKey(soundId)
    }
    
    fun getDownloadProgress(soundId: String): Float {
        return downloadProgress[soundId] ?: 0f
    }
    
    fun cancelDownload(soundId: String) {
        downloadingSounds[soundId]?.cancel()
        downloadingSounds.remove(soundId)
        downloadProgress.remove(soundId)
    }
    
    fun cancelAllDownloads() {
        downloadingSounds.values.forEach { it.cancel() }
        downloadingSounds.clear()
        downloadProgress.clear()
    }
    
    private fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_DIR)
        if (!dir.exists()) {
            val created = dir.mkdirs()
            Log.d(TAG, "Cache dir created: $created, path: ${dir.absolutePath}")
        }
        return dir
    }
    
    private fun getBaseDir(context: Context, type: DownloadType): File {
        val dirName = when (type) {
            DownloadType.WHITE_NOISE -> WHITE_NOISE_DIR
            DownloadType.SCATTERED_NOISE -> SCATTERED_DIR
            DownloadType.OTHER -> OTHER_DIR
        }
        val dir = File(context.filesDir, dirName)
        if (!dir.exists()) {
            val created = dir.mkdirs()
            Log.d(TAG, "Base dir created: $created, path: ${dir.absolutePath}")
        }
        return dir
    }
    
    private fun getSoundDir(
        context: Context,
        type: DownloadType,
        categoryName: String,
        soundName: String,
        typeName: String? = null
    ): File {
        val baseDir = getBaseDir(context, type)
        val path = if (type == DownloadType.SCATTERED_NOISE && typeName != null) {
            "$categoryName/$typeName/$soundName"
        } else {
            "$categoryName/$soundName"
        }
        val dir = File(baseDir, path)
        if (!dir.exists()) {
            val created = dir.mkdirs()
            Log.d(TAG, "Sound dir created: $created, path: ${dir.absolutePath}")
        }
        return dir
    }
    
    private fun getSoundAudioFile(
        context: Context,
        type: DownloadType,
        categoryName: String,
        soundName: String,
        format: String,
        typeName: String? = null
    ): File {
        return File(getSoundDir(context, type, categoryName, soundName, typeName), "$AUDIO_FILE.$format")
    }
    
    fun getCachedFile(context: Context, soundId: String): File? {
        val formats = listOf("mp3", "wav", "ogg", "m4a", "aac", "flac")
        
        val libraryDir = File(context.filesDir, WHITE_NOISE_DIR)
        if (libraryDir.exists() && libraryDir.isDirectory) {
            libraryDir.listFiles()?.forEach { categoryDir ->
                if (categoryDir.isDirectory) {
                    val soundsListFile = File(categoryDir, "${categoryDir.name}_sounds_list.json")
                    if (soundsListFile.exists()) {
                        try {
                            val json = soundsListFile.readText()
                            val soundsList = com.google.gson.Gson().fromJson(
                                json,
                                object : com.google.gson.reflect.TypeToken<List<SoundStorageManager.SoundItem>>() {}.type
                            ) as? List<SoundStorageManager.SoundItem>
                            
                            val matchedSound = soundsList?.find { it.id == soundId }
                            if (matchedSound != null) {
                                val soundDir = File(categoryDir, matchedSound.name)
                                if (soundDir.exists() && soundDir.isDirectory) {
                                    soundDir.listFiles()?.forEach { file ->
                                        if (file.isFile && file.name.startsWith("$AUDIO_FILE.")) {
                                            if (file.length() > 0) return file
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
        
        for (format in formats) {
            val cacheFile = File(getCacheDir(context), "$soundId.$format")
            if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile
        }
        
        return null
    }
    
    fun getCachedFileByPath(context: Context, categoryName: String, soundName: String): File? {
        return getCachedFileByPath(context, DownloadType.WHITE_NOISE, categoryName, soundName, null)
    }
    
    fun getCachedFileByPath(
        context: Context,
        type: DownloadType,
        categoryName: String,
        soundName: String,
        typeName: String?
    ): File? {
        val formats = listOf("mp3", "wav", "ogg", "m4a", "aac", "flac")
        val soundDir = getSoundDir(context, type, categoryName, soundName, typeName)
        
        for (format in formats) {
            val audioFile = File(soundDir, "$AUDIO_FILE.$format")
            if (audioFile.exists() && audioFile.length() > 0) return audioFile
        }
        
        return null
    }
    
    fun getScatteredCachedFile(context: Context, soundId: String): File? {
        val formats = listOf("mp3", "wav", "ogg", "m4a", "aac", "flac")
        
        val scatteredDir = File(context.filesDir, SCATTERED_DIR)
        if (scatteredDir.exists() && scatteredDir.isDirectory) {
            scatteredDir.listFiles()?.forEach { categoryDir ->
                if (categoryDir.isDirectory) {
                    categoryDir.listFiles()?.forEach { typeDir ->
                        if (typeDir.isDirectory) {
                            typeDir.listFiles()?.forEach { soundDir ->
                                if (soundDir.isDirectory && soundDir.name == "sound_$soundId") {
                                    soundDir.listFiles()?.forEach { file ->
                                        if (file.isFile && file.name.startsWith("$AUDIO_FILE.")) {
                                            if (file.length() > 0) {
                                                return file
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        for (format in formats) {
            val cacheFile = File(getCacheDir(context), "$soundId.$format")
            if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile
        }
        
        return null
    }
    
    private fun extractFormatFromUrl(url: String): String {
        val supportedFormats = listOf("mp3", "wav", "ogg", "m4a", "aac", "flac")
        val urlLower = url.lowercase()
        
        for (format in supportedFormats) {
            if (urlLower.contains(".$format") || urlLower.contains("filename=") && urlLower.contains(".$format")) {
                return format
            }
        }
        
        return "mp3"
    }
    
    fun isCached(context: Context, soundId: String): Boolean {
        return getCachedFile(context, soundId) != null
    }
    
    fun isScatteredCached(context: Context, soundId: String): Boolean {
        return getScatteredCachedFile(context, soundId) != null
    }
    
    fun downloadAudio(
        context: Context,
        sound: SoundMetadata,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        downloadAudio(
            context = context,
            downloadType = DownloadType.WHITE_NOISE,
            soundId = sound.id,
            soundName = sound.name,
            categoryName = if (sound.categoryName.isNotEmpty()) sound.categoryName else SoundStorageManager.UNCATEGORIZED_NAME,
            typeName = null,
            remoteUrl = sound.remoteUrl,
            onProgress = onProgress,
            onComplete = onComplete
        )
    }
    
    fun downloadScatteredAudio(
        context: Context,
        sound: ScatteredSoundWithType,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        downloadAudio(
            context = context,
            downloadType = DownloadType.SCATTERED_NOISE,
            soundId = sound.id,
            soundName = sound.name,
            categoryName = sound.categoryName,
            typeName = sound.typeName,
            remoteUrl = sound.remoteUrl,
            onProgress = onProgress,
            onComplete = onComplete
        )
    }
    
    fun downloadAudio(
        context: Context,
        downloadType: DownloadType,
        soundId: String,
        soundName: String,
        categoryName: String,
        typeName: String?,
        remoteUrl: String?,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        if (isDownloading(soundId)) {
            showToast(context, context.getString(R.string.download_already_downloading))
            return
        }
        
        val url = remoteUrl ?: run {
            showToast(context, context.getString(R.string.download_failed))
            onComplete(false)
            return
        }
        
        val job = CoroutineScope(Dispatchers.IO).launch {
            downloadProgress[soundId] = 0f
            
            withContext(Dispatchers.Main) {
                onProgress(0f)
            }
            
            var downloadSuccess = false
            
            try {
                Log.d(TAG, "开始下载: $soundName (类型: $downloadType)")
                
                downloadSuccess = downloadWithRetry(
                    context = context,
                    downloadType = downloadType,
                    soundId = soundId,
                    soundName = soundName,
                    categoryName = categoryName,
                    typeName = typeName,
                    url = url,
                    onProgress = onProgress
                )
                
                withContext(Dispatchers.Main) {
                    if (downloadSuccess) {
                        Log.d(TAG, "下载完成: $soundName")
                        
                        if (downloadType == DownloadType.WHITE_NOISE) {
                            val service = MusicService.getInstance()
                            if (service != null) {
                                val cachedFile = getCachedFile(context, soundId)
                                if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val preloadSuccess = service.preloadSound(soundId, cachedFile)
                                        if (preloadSuccess) {
                                            Log.d(TAG, "下载后预加载成功: $soundName")
                                        } else {
                                            Log.w(TAG, "下载后预加载失败: $soundName")
                                        }
                                    }
                                }
                            }
                        }
                        
                        onComplete(true)
                    } else {
                        Log.e(TAG, "下载失败: $soundName")
                        showToast(context, context.getString(R.string.download_failed))
                        onComplete(false)
                    }
                }
                
            } catch (e: CancellationException) {
                Log.d(TAG, "下载已取消: $soundName")
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    showToast(context, context.getString(R.string.download_failed))
                    onComplete(false)
                }
            } finally {
                downloadingSounds.remove(soundId)
                downloadProgress.remove(soundId)
            }
        }
        
        downloadingSounds[soundId] = job
    }
    
    private suspend fun downloadWithRetry(
        context: Context,
        downloadType: DownloadType,
        soundId: String,
        soundName: String,
        categoryName: String,
        typeName: String?,
        url: String,
        onProgress: (Float) -> Unit
    ): Boolean {
        var lastException: Exception? = null
        
        for (attempt in 1..MAX_RETRY_COUNT) {
            try {
                val success = downloadFromUrl(
                    context = context,
                    downloadType = downloadType,
                    soundId = soundId,
                    soundName = soundName,
                    categoryName = categoryName,
                    typeName = typeName,
                    url = url,
                    onProgress = { progress ->
                        downloadProgress[soundId] = progress
                        CoroutineScope(Dispatchers.Main).launch {
                            onProgress(progress)
                        }
                    }
                )
                
                if (success) {
                    return true
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "下载被取消")
                return false
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "下载失败 (尝试 $attempt/$MAX_RETRY_COUNT): ${e.javaClass.simpleName} - ${e.message}")
                
                if (attempt < MAX_RETRY_COUNT) {
                    val retryDelay = 500L * attempt
                    Log.d(TAG, "等待 ${retryDelay}ms 后重试...")
                    delay(retryDelay)
                }
            }
        }
        
        Log.e(TAG, "所有重试都失败")
        return false
    }
    
    private suspend fun downloadFromUrl(
        context: Context,
        downloadType: DownloadType,
        soundId: String,
        soundName: String,
        categoryName: String,
        typeName: String?,
        url: String,
        onProgress: (Float) -> Unit
    ): Boolean {
        return withContext(Dispatchers.IO) {
            var output: java.io.FileOutputStream? = null
            var input: java.io.InputStream? = null
            
            try {
                val format = extractFormatFromUrl(url)
                
                val audioFile = getSoundAudioFile(context, downloadType, categoryName, soundName, format, typeName)
                Log.d(TAG, "下载到文件: ${audioFile.absolutePath}")
                
                val soundDir = audioFile.parentFile
                if (soundDir != null && !soundDir.exists()) {
                    val created = soundDir.mkdirs()
                    Log.d(TAG, "创建目录: $created, 路径: ${soundDir.absolutePath}")
                }
                
                Log.d(TAG, "开始建立连接: $soundName")
                val startTime = System.currentTimeMillis()
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}) AppleWebKit/537.36")
                    .addHeader("Accept", "*/*")
                    .addHeader("Accept-Language", "en-US,en;q=0.9")
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                val connectTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "连接建立完成: $soundName, 耗时: ${connectTime}ms, 状态码: ${response.code}")
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP错误: ${response.code} - ${response.message}")
                    return@withContext false
                }
                
                val responseBody = response.body
                if (responseBody == null) {
                    Log.e(TAG, "响应体为空")
                    return@withContext false
                }
                
                val fileLength = responseBody.contentLength()
                Log.d(TAG, "文件大小: $fileLength bytes")
                
                if (fileLength <= 0) {
                    Log.e(TAG, "文件大小无效: $fileLength")
                    return@withContext false
                }
                
                input = responseBody.byteStream()
                output = java.io.FileOutputStream(audioFile)
                
                val buffer = ByteArray(8192)
                var total: Long = 0
                var count: Int
                
                while (input.read(buffer).also { count = it } != -1) {
                    if (!coroutineContext.isActive) {
                        Log.d(TAG, "下载被取消")
                        input.close()
                        output.close()
                        audioFile.delete()
                        return@withContext false
                    }
                    
                    total += count
                    val progress = total.toFloat() / fileLength.toFloat()
                    onProgress(progress)
                    output.write(buffer, 0, count)
                }
                
                output.flush()
                output.close()
                input.close()
                
                val downloadedSize = audioFile.length()
                Log.d(TAG, "下载完成, 文件大小: $downloadedSize bytes")
                
                if (downloadedSize > 0) {
                    if (downloadedSize != fileLength) {
                        Log.w(TAG, "文件大小不匹配: 期望 $fileLength, 实际 $downloadedSize")
                    }
                    return@withContext true
                } else {
                    Log.e(TAG, "文件大小为0, 删除文件")
                    audioFile.delete()
                    return@withContext false
                }
                
            } catch (e: IOException) {
                Log.e(TAG, "下载IO错误: ${e.javaClass.simpleName} - ${e.message}")
                e.printStackTrace()
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "下载错误: ${e.javaClass.simpleName} - ${e.message}")
                e.printStackTrace()
                throw e
            } finally {
                try {
                    input?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "关闭输入流失败: ${e.message}")
                }
                try {
                    output?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "关闭输出流失败: ${e.message}")
                }
            }
        }
    }
    
    private fun showToast(context: Context, message: String) {
        handler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    fun deleteCache(context: Context, soundId: String): Boolean {
        val file = getCachedFile(context, soundId) ?: return false
        return file.delete()
    }
    
    fun clearAllCache(context: Context) {
        val cacheDir = getCacheDir(context)
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
