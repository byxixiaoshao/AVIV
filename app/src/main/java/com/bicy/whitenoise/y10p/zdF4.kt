package com.bicy.whitenoise.y10p

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object SoundStorageManager {
    
    private const val TAG = "SoundStorageManager"
    private const val SOUNDS_CLASS_FILE = "categories.json"
    private const val SOUNDS_LIST_SUFFIX = "_sounds_list.json"
    private const val TYPE_FILE = "metadata.json"
    const val UNCATEGORIZED_NAME = "category_uncategorized"
    
    private val gson = GsonBuilder().setPrettyPrinting().create()
    
    private const val LIBRARY_DIR = "white_noise/library"
    
    data class SoundClass(
        val id: String,
        val name: String,
        val isCustom: Boolean = false
    )
    
    data class SoundType(
        val type: SoundSourceType,
        val nameKey: String = "",
        val translations: Map<String, String>? = null,
        val downloadUrl: String? = null,
        val author: String? = null,
        val authorUrl: String? = null,
        val synthesisParams: SynthesisParams? = null,
        val downloadDate: String? = null,
        val fileSize: Long? = null,
        val duration: Long? = null
    )
    
    enum class SoundSourceType {
        NETWORK_DOWNLOAD,
        LOCAL_SYNTHESIS,
        LOCAL_IMPORT
    }
    
    data class SynthesisParams(
        val noiseType: String,
        val frequency: Float,
        val duration: Int,
        val volume: Float,
        val additionalParams: Map<String, Any>? = null
    )
    
    data class SoundItem(
        val id: String,
        val name: String,
        val remoteUrl: String? = null,
        val author: String? = null,
        val authorUrl: String? = null
    )
    
    private fun getLibraryDir(context: Context): File {
        val dir = File(context.filesDir, LIBRARY_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    private fun getSoundsClassFile(context: Context): File {
        return File(getLibraryDir(context), SOUNDS_CLASS_FILE)
    }
    
    private fun getCategoryDir(context: Context, categoryName: String): File {
        return File(getLibraryDir(context), categoryName)
    }
    
    private fun getSoundsListFile(context: Context, categoryName: String): File {
        return File(getCategoryDir(context, categoryName), "${categoryName}$SOUNDS_LIST_SUFFIX")
    }
    
    private fun getSoundDir(context: Context, categoryName: String, soundName: String): File {
        return File(getCategoryDir(context, categoryName), soundName)
    }
    
    private fun getTypeFile(context: Context, categoryName: String, soundName: String): File {
        return File(getSoundDir(context, categoryName, soundName), TYPE_FILE)
    }
    
    fun init(context: Context) {
        val classFile = getSoundsClassFile(context)
        if (!classFile.exists()) {
            initializeFromRemoteManifest(context)
        } else {
            val existingClasses = loadSoundsClass(context)
            val hasOnlyUncategorized = existingClasses.size == 1 && existingClasses[0].id == "uncategorized"
            if (hasOnlyUncategorized) {
                Log.d(TAG, "检测到只有未分类，重新初始化")
                initializeFromRemoteManifest(context)
            }
        }
        
        val classList = loadSoundsClass(context)
        classList.forEach { soundClass ->
            val categoryDir = getCategoryDir(context, soundClass.name)
            if (!categoryDir.exists()) {
                categoryDir.mkdirs()
            }
            
            val listFile = getSoundsListFile(context, soundClass.name)
            if (!listFile.exists()) {
                createDefaultSoundsList(context, soundClass.name)
            }
        }
    }
    
    private fun initializeFromRemoteManifest(context: Context) {
        try {
            Log.d(TAG, "开始从sounds_remote.json初始化...")
            
            val json = context.assets.open("sounds_remote.json").use { 
                it.bufferedReader().use { reader -> reader.readText() }
            }
            Log.d(TAG, "读取sounds_remote.json成功，长度: ${json.length}")
            
            val manifest = com.google.gson.Gson().fromJson(
                json,
                com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundsManifest::class.java
            )
            Log.d(TAG, "解析JSON成功: ${manifest.categories.size}个分类, ${manifest.sounds.size}个音频")
            
            val soundClasses = manifest.categories.map { category ->
                SoundClass(
                    id = category.id,
                    name = category.name,
                    isCustom = false
                )
            }.toMutableList()
            
            soundClasses.add(
                0,
                SoundClass(
                    id = "uncategorized",
                    name = UNCATEGORIZED_NAME,
                    isCustom = false
                )
            )
            
            saveSoundsClass(context, soundClasses)
            Log.d(TAG, "保存分类清单成功")
            
            manifest.categories.forEach { category ->
                Log.d(TAG, "处理分类: ${category.id} -> ${category.name}")
                
                val categoryDir = getCategoryDir(context, category.name)
                if (!categoryDir.exists()) {
                    val created = categoryDir.mkdirs()
                    Log.d(TAG, "创建分类目录: ${categoryDir.absolutePath}, 结果: $created")
                }
                
                val soundsInCategory = manifest.sounds.filter { it.category == category.id }
                Log.d(TAG, "分类 ${category.name} 中有 ${soundsInCategory.size} 个音频")
                
                val soundItems = soundsInCategory.map { sound ->
                    SoundItem(
                        id = sound.id,
                        name = sound.name,
                        remoteUrl = sound.remoteUrl,
                        author = sound.author,
                        authorUrl = sound.authorUrl
                    )
                }
                
                saveSoundsList(context, category.name, soundItems)
                
                soundItems.forEach { soundItem ->
                    val soundDir = getSoundDir(context, category.name, soundItem.name)
                    if (!soundDir.exists()) {
                        soundDir.mkdirs()
                    }
                    
                    val translations = mutableMapOf<String, String>()
                    manifest.Language.forEach { (langCode, langTranslations) ->
                        val translation = langTranslations[soundItem.name]
                        if (translation != null) {
                            translations[langCode] = translation
                        }
                    }
                    
                    val soundType = SoundType(
                        type = SoundSourceType.NETWORK_DOWNLOAD,
                        nameKey = soundItem.name,
                        translations = translations.ifEmpty { null },
                        downloadUrl = soundItem.remoteUrl,
                        author = soundItem.author,
                        authorUrl = soundItem.authorUrl,
                        synthesisParams = null
                    )
                    
                    saveSoundType(context, category.name, soundItem.name, soundType)
                }
            }
            
            Log.d(TAG, "从sounds_remote.json初始化完成: ${soundClasses.size}个分类")
        } catch (e: Exception) {
            Log.e(TAG, "从sounds_remote.json初始化失败: ${e.message}", e)
            e.printStackTrace()
            createDefaultSoundsClass(context)
        }
    }
    
    private fun createDefaultSoundsClass(context: Context) {
        val defaultClasses = listOf(
            SoundClass(
                id = "uncategorized",
                name = UNCATEGORIZED_NAME,
                isCustom = false
            )
        )
        
        val classFile = getSoundsClassFile(context)
        val writer = FileWriter(classFile)
        gson.toJson(defaultClasses, writer)
        writer.close()
        
        val uncategorizedDir = getCategoryDir(context, UNCATEGORIZED_NAME)
        if (!uncategorizedDir.exists()) {
            uncategorizedDir.mkdirs()
        }
        
        val listFile = getSoundsListFile(context, UNCATEGORIZED_NAME)
        if (!listFile.exists()) {
            createDefaultSoundsList(context, UNCATEGORIZED_NAME)
        }
        
        Log.d(TAG, "创建默认分类清单: ${defaultClasses.size}个分类")
    }
    
    private fun createDefaultSoundsList(context: Context, categoryName: String) {
        val defaultSounds = emptyList<SoundItem>()
        
        val listFile = getSoundsListFile(context, categoryName)
        val categoryDir = listFile.parentFile
        if (categoryDir != null && !categoryDir.exists()) {
            categoryDir.mkdirs()
        }
        
        val writer = FileWriter(listFile)
        gson.toJson(defaultSounds, writer)
        writer.close()
        
        Log.d(TAG, "创建默认音频清单: $categoryName")
    }
    
    fun loadSoundsClass(context: Context): List<SoundClass> {
        val classFile = getSoundsClassFile(context)
        if (!classFile.exists()) {
            createDefaultSoundsClass(context)
        }
        
        try {
            val reader = FileReader(classFile)
            val type = object : TypeToken<List<SoundClass>>() {}.type
            val classList = gson.fromJson<List<SoundClass>>(reader, type)
            reader.close()
            return classList
        } catch (e: Exception) {
            Log.e(TAG, "加载分类清单失败: ${e.message}", e)
            createDefaultSoundsClass(context)
            return loadSoundsClass(context)
        }
    }
    
    fun saveSoundsClass(context: Context, classList: List<SoundClass>) {
        val classFile = getSoundsClassFile(context)
        try {
            val writer = FileWriter(classFile)
            gson.toJson(classList, writer)
            writer.close()
            Log.d(TAG, "保存分类清单成功: ${classList.size}个分类")
        } catch (e: Exception) {
            Log.e(TAG, "保存分类清单失败: ${e.message}", e)
        }
    }
    
    fun loadSoundsList(context: Context, categoryName: String): List<SoundItem> {
        val listFile = getSoundsListFile(context, categoryName)
        if (!listFile.exists()) {
            createDefaultSoundsList(context, categoryName)
        }
        
        try {
            val reader = FileReader(listFile)
            val type = object : TypeToken<List<SoundItem>>() {}.type
            val soundList = gson.fromJson<List<SoundItem>>(reader, type)
            reader.close()
            return soundList
        } catch (e: Exception) {
            Log.e(TAG, "加载音频清单失败 ($categoryName): ${e.message}", e)
            createDefaultSoundsList(context, categoryName)
            return emptyList()
        }
    }
    
    fun saveSoundsList(context: Context, categoryName: String, soundList: List<SoundItem>) {
        val listFile = getSoundsListFile(context, categoryName)
        try {
            val writer = FileWriter(listFile)
            gson.toJson(soundList, writer)
            writer.close()
            Log.d(TAG, "保存音频清单成功 ($categoryName): ${soundList.size}个音频")
        } catch (e: Exception) {
            Log.e(TAG, "保存音频清单失败 ($categoryName): ${e.message}", e)
        }
    }
    
    fun loadSoundType(context: Context, categoryName: String, soundName: String): SoundType? {
        val typeFile = getTypeFile(context, categoryName, soundName)
        if (!typeFile.exists()) {
            return null
        }
        
        try {
            val reader = FileReader(typeFile)
            val soundType = gson.fromJson(reader, SoundType::class.java)
            reader.close()
            return soundType
        } catch (e: Exception) {
            Log.e(TAG, "加载音频类型失败 ($categoryName/$soundName): ${e.message}", e)
            return null
        }
    }
    
    fun saveSoundType(context: Context, categoryName: String, soundName: String, soundType: SoundType) {
        val soundDir = getSoundDir(context, categoryName, soundName)
        if (!soundDir.exists()) {
            soundDir.mkdirs()
        }
        
        val typeFile = getTypeFile(context, categoryName, soundName)
        try {
            val writer = FileWriter(typeFile)
            gson.toJson(soundType, writer)
            writer.close()
            Log.d(TAG, "保存音频类型成功 ($categoryName/$soundName): ${soundType.type}")
        } catch (e: Exception) {
            Log.e(TAG, "保存音频类型失败 ($categoryName/$soundName): ${e.message}", e)
        }
    }
    
    fun addCategory(context: Context, name: String): SoundClass {
        val classList = loadSoundsClass(context)
        
        val newClass = SoundClass(
            id = "custom_class_${System.currentTimeMillis()}",
            name = name,
            isCustom = true
        )
        
        val updatedList = classList + newClass
        saveSoundsClass(context, updatedList)
        
        val categoryDir = getCategoryDir(context, name)
        if (!categoryDir.exists()) {
            categoryDir.mkdirs()
        }
        
        createDefaultSoundsList(context, name)
        
        return newClass
    }
    
    fun addSound(
        context: Context,
        categoryName: String,
        name: String,
        soundType: SoundType,
        author: String? = null,
        authorUrl: String? = null
    ): SoundItem {
        val soundList = loadSoundsList(context, categoryName)
        
        val newSound = SoundItem(
            id = "sound_${System.currentTimeMillis()}",
            name = name,
            remoteUrl = soundType.downloadUrl,
            author = author,
            authorUrl = authorUrl
        )
        
        val updatedList = soundList + newSound
        saveSoundsList(context, categoryName, updatedList)
        
        val soundDir = getSoundDir(context, categoryName, name)
        if (!soundDir.exists()) {
            soundDir.mkdirs()
        }
        
        saveSoundType(context, categoryName, name, soundType)
        
        return newSound
    }
    
    fun deleteCategory(context: Context, categoryName: String): Boolean {
        if (categoryName == UNCATEGORIZED_NAME) {
            Log.w(TAG, "不能删除未分类")
            return false
        }
        
        val classList = loadSoundsClass(context)
        val updatedList = classList.filter { it.name != categoryName }
        saveSoundsClass(context, updatedList)
        
        val categoryDir = getCategoryDir(context, categoryName)
        if (categoryDir.exists()) {
            categoryDir.deleteRecursively()
        }
        
        return true
    }
    
    fun deleteSound(context: Context, categoryName: String, soundName: String): Boolean {
        val soundList = loadSoundsList(context, categoryName)
        val updatedList = soundList.filter { it.name != soundName }
        saveSoundsList(context, categoryName, updatedList)
        
        val soundDir = getSoundDir(context, categoryName, soundName)
        if (soundDir.exists()) {
            soundDir.deleteRecursively()
        }
        
        return true
    }
    
    fun getSoundFile(context: Context, categoryName: String, soundName: String, format: String): File {
        val soundDir = getSoundDir(context, categoryName, soundName)
        return File(soundDir, "audio.$format")
    }
    
    fun getExistingSoundFile(context: Context, categoryName: String, soundName: String, format: String): File? {
        val file = getSoundFile(context, categoryName, soundName, format)
        return if (file.exists() && file.length() > 0) file else null
    }
    
    fun getAllSounds(context: Context): Map<String, List<SoundItem>> {
        val classList = loadSoundsClass(context)
        val result = mutableMapOf<String, List<SoundItem>>()
        
        classList.forEach { soundClass ->
            val soundList = loadSoundsList(context, soundClass.name)
            result[soundClass.name] = soundList
        }
        
        return result
    }
}
