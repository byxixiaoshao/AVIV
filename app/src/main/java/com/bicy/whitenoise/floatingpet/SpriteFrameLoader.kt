package com.bicy.whitenoise.floatingpet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.bicy.whitenoise.service.AnomalyType
import com.bicy.whitenoise.service.MemoryLockService
import com.bicy.whitenoise.storage.core.StorageManager
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

object SpriteFrameLoader {
    
    private const val TAG = "SpriteFrameLoader"
    private const val ASSETS_PATH = "Floating_Sprite_Frame"
    
    private val loadedConfigs = mutableMapOf<String, SpriteFrameConfig>()
    private val loadedFrames = mutableMapOf<String, MutableList<Bitmap>>()
    
    fun loadConfig(context: Context, petId: String): SpriteFrameConfig? {
        if (loadedConfigs.containsKey(petId)) {
            return loadedConfigs[petId]
        }
        
        val petsDir = StorageManager.getFloatingPetsDir()
        
        val inputStream: InputStream? = if (petsDir != null) {
            val extractedFile = File(petsDir, "$petId/manifast.json")
            if (extractedFile.exists()) {
                Log.d(TAG, "Loading config from extracted directory: $petId")
                FileInputStream(extractedFile)
            } else {
                Log.d(TAG, "Loading config from assets: $petId")
                try {
                    context.assets.open("$ASSETS_PATH/$petId/manifast.json")
                } catch (e: Exception) {
                    MemoryLockService.reportAnomaly(AnomalyType.IO_ERROR, "加载精灵配置失败: $petId", e.stackTraceToString())
                    null
                }
            }
        } else {
            try {
                context.assets.open("$ASSETS_PATH/$petId/manifast.json")
            } catch (e: Exception) {
                MemoryLockService.reportAnomaly(AnomalyType.IO_ERROR, "加载精灵配置失败: $petId", e.stackTraceToString())
                null
            }
        }
        
        if (inputStream == null) {
            Log.e(TAG, "Manifest not found for pet: $petId")
            return null
        }
        
        try {
            val jsonStr = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()
            val json = JSONObject(jsonStr)
            
            val animations = mutableMapOf<String, AnimationConfig>()
            val animationKeys = listOf("idle", "move", "hide_bottom", "hide_top", "hide_left", "hide_right")
            
            for (key in animationKeys) {
                if (json.has(key)) {
                    val animJson = json.getJSONObject(key)
                    animations[key] = AnimationConfig(
                        image = animJson.getString("image"),
                        frameRate = animJson.getInt("frame_rate"),
                        speed = animJson.getDouble("speed").toFloat()
                    )
                }
            }
            
            val config = SpriteFrameConfig(
                title = json.getString("title"),
                width = json.getInt("width"),
                height = json.getInt("height"),
                animations = animations
            )
            
            loadedConfigs[petId] = config
            Log.d(TAG, "Config loaded for $petId: $config")
            return config
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config for $petId", e)
            return null
        }
    }
    
    fun loadFrames(context: Context, petId: String, animationKey: String): List<Bitmap>? {
        val cacheKey = "$petId/$animationKey"
        if (loadedFrames.containsKey(cacheKey)) {
            return loadedFrames[cacheKey]
        }
        
        val config = loadConfig(context, petId)
        if (config == null) return null
        
        val animConfig = config.animations[animationKey]
        if (animConfig == null) {
            Log.e(TAG, "Animation not found: $animationKey for $petId")
            return null
        }
        
        val petsDir = StorageManager.getFloatingPetsDir()
        
        try {
            val frames = mutableListOf<Bitmap>()
            for (i in 0 until animConfig.frameRate) {
                val frameFileName = animConfig.getFrameFileName(i)
                
                val inputStream: InputStream? = if (petsDir != null) {
                    val extractedFile = File(petsDir, "$petId/$frameFileName")
                    if (extractedFile.exists()) {
                        FileInputStream(extractedFile)
                    } else {
                        try {
                            context.assets.open("$ASSETS_PATH/$petId/$frameFileName")
                        } catch (e: Exception) {
                            MemoryLockService.reportAnomaly(AnomalyType.IO_ERROR, "加载帧图片失败: $petId/$frameFileName", e.stackTraceToString())
                            null
                        }
                    }
                } else {
                    try {
                        context.assets.open("$ASSETS_PATH/$petId/$frameFileName")
                    } catch (e: Exception) {
                        MemoryLockService.reportAnomaly(AnomalyType.IO_ERROR, "加载帧图片失败: $petId/$frameFileName", e.stackTraceToString())
                        null
                    }
                }
                
                if (inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (bitmap != null) {
                        frames.add(bitmap)
                    }
                }
            }
            
            if (frames.isNotEmpty()) {
                loadedFrames[cacheKey] = frames
                Log.d(TAG, "Loaded ${frames.size} frames for $cacheKey")
                return frames
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load frames for $cacheKey", e)
        }
        
        return null
    }
    
    fun getAvailablePets(context: Context): List<String> {
        val pets = mutableSetOf<String>()
        
        val petsDir = StorageManager.getFloatingPetsDir()
        if (petsDir != null && petsDir.exists()) {
            petsDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    val manifestFile = File(file, "manifast.json")
                    if (manifestFile.exists()) {
                        pets.add(file.name)
                    }
                }
            }
        }
        
        try {
            val assetPetDirs = context.assets.list(ASSETS_PATH) ?: emptyArray()
            for (dir in assetPetDirs) {
                try {
                    context.assets.open("$ASSETS_PATH/$dir/manifast.json").close()
                    pets.add(dir)
                } catch (e: java.io.FileNotFoundException) {
                    // 预期行为：config.json 等非目录条目拼接 /manifast.json 后必抛 FNF，
                    // 静默跳过即可，不应作为 IO_ERROR 上报（否则每次打开萌宠选择器都触发异常日志）
                } catch (e: Exception) {
                    MemoryLockService.reportAnomaly(AnomalyType.IO_ERROR, "检查精灵资源失败: $ASSETS_PATH/$dir", e.stackTraceToString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list pets from assets", e)
            MemoryLockService.reportAnomaly(AnomalyType.IO_ERROR, "列出精灵列表失败", e.stackTraceToString())
        }
        
        return pets.toList()
    }
    
    fun clearCache() {
        loadedFrames.values.flatten().forEach { it.recycle() }
        loadedFrames.clear()
        loadedConfigs.clear()
    }
    
    fun getBitmap(context: Context, petId: String, animationKey: String, frameIndex: Int): Bitmap? {
        val frames = loadFrames(context, petId, animationKey)
        return frames?.getOrNull(frameIndex)
    }
    
    fun invalidatePet(petId: String) {
        loadedConfigs.remove(petId)
        loadedFrames.keys.filter { it.startsWith("$petId/") }.forEach { loadedFrames.remove(it) }
    }
}