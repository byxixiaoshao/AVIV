package com.bicy.whitenoise.JwJY.Jauc

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference

object StorageManager {
    
    private const val TAG = "StorageManager"
    
    private const val WHITE_NOISE_DIR = "white_noise"
    private const val MUSIC_DIR = "music"
    private const val CONFIG_DIR = "config"
    
    private const val LIBRARY_DIR = "library"
    private const val SCATTERED_DIR = "scattered"
    
    private var contextRef: WeakReference<Context>? = null
    private var isInitialized = false
    
    fun init(context: Context) {
        contextRef = WeakReference(context.applicationContext)
        createDirectoryStructure()
        isInitialized = true
        Log.d(TAG, "StorageManager initialized")
    }
    
    fun isInitialized(): Boolean = isInitialized
    
    private fun createDirectoryStructure() {
        val filesDir = contextRef?.get()?.filesDir ?: return
        
        File(filesDir, WHITE_NOISE_DIR).apply { mkdirs() }
        File(filesDir, MUSIC_DIR).apply { mkdirs() }
        File(filesDir, CONFIG_DIR).apply { mkdirs() }
        
        File(filesDir, "$WHITE_NOISE_DIR/$LIBRARY_DIR").apply { mkdirs() }
        File(filesDir, "$WHITE_NOISE_DIR/$SCATTERED_DIR").apply { mkdirs() }
        
        Log.d(TAG, "Directory structure created")
    }
    
    fun getWhiteNoiseDir(): File? {
        return contextRef?.get()?.filesDir?.let { File(it, WHITE_NOISE_DIR) }
    }
    
    fun getLibraryDir(): File? {
        return contextRef?.get()?.filesDir?.let { File(it, "$WHITE_NOISE_DIR/$LIBRARY_DIR") }
    }
    
    fun getScatteredDir(): File? {
        return contextRef?.get()?.filesDir?.let { File(it, "$WHITE_NOISE_DIR/$SCATTERED_DIR") }
    }
    
    fun getMusicDir(): File? {
        return contextRef?.get()?.filesDir?.let { File(it, MUSIC_DIR) }
    }
    
    fun getConfigDir(): File? {
        return contextRef?.get()?.filesDir?.let { File(it, CONFIG_DIR) }
    }
    
    fun getFile(vararg pathParts: String): File? {
        val ctx = contextRef?.get() ?: return null
        return File(ctx.filesDir, pathParts.joinToString(File.separator))
    }
    
    fun saveJson(file: File, json: JSONObject) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                file.parentFile?.mkdirs()
                file.writeText(json.toString(2))
                Log.d(TAG, "JSON saved: ${file.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save JSON: ${file.name}", e)
            }
        }
    }
    
    fun saveJsonSync(file: File, json: JSONObject): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.writeText(json.toString(2))
            Log.d(TAG, "JSON saved sync: ${file.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save JSON sync: ${file.name}", e)
            false
        }
    }
    
    fun saveJsonSync(file: File, jsonArray: JSONArray): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.writeText(jsonArray.toString(2))
            Log.d(TAG, "JSONArray saved sync: ${file.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save JSONArray sync: ${file.name}", e)
            false
        }
    }
    
    fun loadJson(file: File): JSONObject? {
        return try {
            if (file.exists()) {
                JSONObject(file.readText())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load JSON: ${file.name}", e)
            null
        }
    }
    
    fun saveJsonArray(file: File, jsonArray: JSONArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                file.parentFile?.mkdirs()
                file.writeText(jsonArray.toString(2))
                Log.d(TAG, "JSONArray saved: ${file.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save JSONArray: ${file.name}", e)
            }
        }
    }
    
    fun loadJsonArray(file: File): JSONArray? {
        return try {
            if (file.exists()) {
                JSONArray(file.readText())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load JSONArray: ${file.name}", e)
            null
        }
    }
    
    fun fileExists(vararg pathParts: String): Boolean {
        val file = getFile(*pathParts) ?: return false
        return file.exists()
    }
    
    fun deleteFile(vararg pathParts: String): Boolean {
        val file = getFile(*pathParts) ?: return false
        return if (file.exists()) {
            file.deleteRecursively()
        } else {
            true
        }
    }
    
    fun deleteDirectory(dir: File): Boolean {
        return if (dir.exists() && dir.isDirectory) {
            dir.deleteRecursively()
        } else {
            true
        }
    }
}
