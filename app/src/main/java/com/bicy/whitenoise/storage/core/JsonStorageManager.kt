package com.bicy.whitenoise.storage.core

import android.content.Context
import com.bicy.whitenoise.WhiteNoiseApp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File

object JsonStorageManager {
    @Volatile
    private var appContext: Context? = null
    private val gson = Gson()
    private val _changeFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val changeFlow: SharedFlow<String> = _changeFlow.asSharedFlow()

    /**
     * 懒初始化存储目录，修复 error_2.txt 的 `filesDir has not been initialized` 崩溃。
     *
     * 根因：WhiteNoiseApp.onCreate() 中 MemoryLockService.start() / startService(LogCaptureService)
     * 在 AppInitializer.init()（调用 JsonStorageManager.init()）之前执行；存储读取协程可能在
     * init() 完成前就被派发到 IO 线程，触发 lateinit 访问崩溃。主进程与 :log 进程均受影响。
     *
     * 修复：filesDir 改为懒加载——优先用 init() 传入的 appContext，兜底用 WhiteNoiseApp.context
     * （后者在 Application.onCreate 首行即设置，所有进程在任何 Service/Activity 之前就绪）。
     * 这样即使 read()/write() 在 init() 之前被调用也能安全解析目录，且 init() 幂等可重复调用。
     */
    private val filesDir: File by lazy {
        val ctx = appContext ?: WhiteNoiseApp.context
        File(ctx.filesDir, "json_storage").apply { mkdirs() }
    }

    /** 显式初始化（幂等，预热懒加载；多进程均安全）。 */
    fun init(context: Context) {
        appContext = context.applicationContext
        filesDir  // 触发 lazy 初始化
    }

    suspend fun <T : Any> read(fileName: String, type: Class<T>): T? = withContext(Dispatchers.IO) {
        val file = File(filesDir, fileName)
        if (!file.exists()) return@withContext null
        try {
            gson.fromJson(file.readText(), type)
        } catch (_: Exception) { null }
    }

    suspend fun write(fileName: String, data: Any) = withContext(Dispatchers.IO) {
        val file = File(filesDir, fileName)
        try {
            file.writeText(gson.toJson(data))
            _changeFlow.tryEmit(fileName)
        } catch (_: Exception) {}
    }

    suspend fun delete(fileName: String) = withContext(Dispatchers.IO) {
        val file = File(filesDir, fileName)
        if (file.exists()) {
            file.delete()
            _changeFlow.tryEmit(fileName)
        }
    }

    fun getFile(fileName: String): File = File(filesDir, fileName)

    fun exists(fileName: String): Boolean = File(filesDir, fileName).exists()
}
