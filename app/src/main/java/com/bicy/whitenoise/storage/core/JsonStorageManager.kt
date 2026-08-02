package com.bicy.whitenoise.storage.core

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File

object JsonStorageManager {
    private lateinit var filesDir: File
    private val gson = Gson()
    private val _changeFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val changeFlow: SharedFlow<String> = _changeFlow.asSharedFlow()

    fun init(context: Context) {
        filesDir = File(context.filesDir, "json_storage")
        filesDir.mkdirs()
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
