package com.bicy.whitenoise.y10p

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.DzBD.IrBh.XsdL.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

object ScatteredStorageManager {
    
    private const val TAG = "ScatteredStorageManager"
    private const val SCATTERED_DIR = "white_noise/scattered"
    private const val CATEGORIES_FILE = "categories.json"
    private const val TYPES_SUFFIX = "_types_list.json"
    private const val SOUNDS_SUFFIX = "_sounds_list.json"
    private const val METADATA_FILE = "metadata.json"
    private const val AUDIO_FILE = "audio"
    
    private val gson = GsonBuilder().setPrettyPrinting().create()
    
    data class ScatteredCategory(
        val id: String,
        val name: String,
        val isCustom: Boolean = false,
        val translations: Map<String, String>? = null
    )
    
    data class ScatteredType(
        val id: String,
        val name: String,
        val categoryId: String,
        val translations: Map<String, String>? = null
    )
    
    data class ScatteredSound(
        val id: String,
        val name: String,
        val typeId: String,
        val categoryId: String,
        val remoteUrl: String? = null,
        val author: String? = null,
        val authorUrl: String? = null,
        val translations: Map<String, String>? = null,
        val downloadDate: String? = null,
        val fileSize: Long? = null
    )
    
    private fun getScatteredDir(context: Context): File {
        val dir = File(context.filesDir, SCATTERED_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    private fun getCategoriesFile(context: Context): File {
        return File(getScatteredDir(context), CATEGORIES_FILE)
    }
    
    private fun getCategoryDir(context: Context, categoryName: String): File {
        return File(getScatteredDir(context), categoryName)
    }
    
    private fun getTypesListFile(context: Context, categoryName: String): File {
        return File(getCategoryDir(context, categoryName), "${categoryName}$TYPES_SUFFIX")
    }
    
    private fun getTypeDir(context: Context, categoryName: String, typeName: String): File {
        return File(getCategoryDir(context, categoryName), typeName)
    }
    
    private fun getSoundsListFile(context: Context, categoryName: String, typeName: String): File {
        return File(getTypeDir(context, categoryName, typeName), "${typeName}$SOUNDS_SUFFIX")
    }
    
    private fun getSoundDir(context: Context, categoryName: String, typeName: String, soundName: String): File {
        return File(getTypeDir(context, categoryName, typeName), soundName)
    }
    
    private fun getMetadataFile(context: Context, categoryName: String, typeName: String, soundName: String): File {
        return File(getSoundDir(context, categoryName, typeName, soundName), METADATA_FILE)
    }
    
    fun init(context: Context) {
        val categoriesFile = getCategoriesFile(context)
        if (!categoriesFile.exists()) {
            initializeFromAsset(context)
        }
    }
    
    private fun initializeFromAsset(context: Context) {
        try {
            Log.d(TAG, "开始从scattered_sounds.json初始化...")
            
            val json = context.assets.open("scattered_sounds.json").use { 
                it.bufferedReader().use { reader -> reader.readText() }
            }
            
            val manifest = Gson().fromJson(json, ScatteredSoundsManifest::class.java)
            
            val categories = manifest.categories.map { category ->
                val categoryTranslations = mutableMapOf<String, String>()
                manifest.Language.forEach { (langCode, langTranslations) ->
                    langTranslations[category.name]?.let { categoryTranslations[langCode] = it }
                }
                
                ScatteredCategory(
                    id = category.id,
                    name = category.name,
                    isCustom = false,
                    translations = categoryTranslations.ifEmpty { null }
                )
            }
            
            saveCategories(context, categories)
            Log.d(TAG, "保存分类清单成功: ${categories.size}个分类")
            
            manifest.categories.forEach { category ->
                Log.d(TAG, "处理分类: ${category.id} -> ${category.name}")
                
                val categoryDir = getCategoryDir(context, category.name)
                if (!categoryDir.exists()) {
                    categoryDir.mkdirs()
                }
                
                val typesInCategory = manifest.soundTypes.filter { it.category == category.id }
                
                val scatteredTypes = typesInCategory.map { soundType ->
                    val typeTranslations = mutableMapOf<String, String>()
                    manifest.Language.forEach { (langCode, langTranslations) ->
                        langTranslations[soundType.name]?.let { typeTranslations[langCode] = it }
                    }
                    
                    ScatteredType(
                        id = soundType.id,
                        name = soundType.name,
                        categoryId = category.id,
                        translations = typeTranslations.ifEmpty { null }
                    )
                }
                
                saveTypesList(context, category.name, scatteredTypes)
                
                scatteredTypes.forEach { scatteredType ->
                    val typeDir = getTypeDir(context, category.name, scatteredType.name)
                    if (!typeDir.exists()) {
                        typeDir.mkdirs()
                    }
                    
                    val soundsInType = manifest.sounds.filter { it.type == scatteredType.id }
                    
                    val scatteredSounds = soundsInType.map { sound ->
                        val soundTranslations = mutableMapOf<String, String>()
                        manifest.Language.forEach { (langCode, langTranslations) ->
                            langTranslations[sound.name]?.let { soundTranslations[langCode] = it }
                        }
                        
                        ScatteredSound(
                            id = sound.id,
                            name = sound.name,
                            typeId = scatteredType.id,
                            categoryId = category.id,
                            remoteUrl = sound.remoteUrl,
                            author = sound.author,
                            authorUrl = sound.authorUrl,
                            translations = soundTranslations.ifEmpty { null }
                        )
                    }
                    
                    saveSoundsList(context, category.name, scatteredType.name, scatteredSounds)
                    
                    scatteredSounds.forEach { scatteredSound ->
                        val soundDir = getSoundDir(context, category.name, scatteredType.name, scatteredSound.name)
                        if (!soundDir.exists()) {
                            soundDir.mkdirs()
                        }
                        
                        saveSoundMetadata(context, category.name, scatteredType.name, scatteredSound.name, scatteredSound)
                    }
                }
            }
            
            Log.d(TAG, "从scattered_sounds.json初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "初始化散点白噪音清单失败", e)
            createDefaultCategories(context)
        }
    }
    
    private fun createDefaultCategories(context: Context) {
        val defaultCategories = listOf(
            ScatteredCategory(id = "objects", name = "objects", isCustom = false)
        )
        saveCategories(context, defaultCategories)
        
        val objectsDir = getCategoryDir(context, "objects")
        if (!objectsDir.exists()) {
            objectsDir.mkdirs()
        }
        
        saveTypesList(context, "objects", emptyList())
        
        Log.d(TAG, "创建默认分类清单")
    }
    
    private fun saveCategories(context: Context, categories: List<ScatteredCategory>) {
        val file = getCategoriesFile(context)
        file.writeText(gson.toJson(categories))
    }
    
    private fun saveTypesList(context: Context, categoryName: String, types: List<ScatteredType>) {
        val file = getTypesListFile(context, categoryName)
        file.writeText(gson.toJson(types))
    }
    
    private fun saveSoundsList(context: Context, categoryName: String, typeName: String, sounds: List<ScatteredSound>) {
        val file = getSoundsListFile(context, categoryName, typeName)
        file.writeText(gson.toJson(sounds))
    }
    
    private fun saveSoundMetadata(context: Context, categoryName: String, typeName: String, soundName: String, sound: ScatteredSound) {
        val file = getMetadataFile(context, categoryName, typeName, soundName)
        file.writeText(gson.toJson(sound))
    }
    
    private fun loadCategories(context: Context): List<ScatteredCategory> {
        val file = getCategoriesFile(context)
        if (!file.exists()) return emptyList()
        
        return try {
            gson.fromJson(file.readText(), object : TypeToken<List<ScatteredCategory>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun loadTypesList(context: Context, categoryName: String): List<ScatteredType> {
        val file = getTypesListFile(context, categoryName)
        if (!file.exists()) return emptyList()
        
        return try {
            gson.fromJson(file.readText(), object : TypeToken<List<ScatteredType>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun loadSoundsList(context: Context, categoryName: String, typeName: String): List<ScatteredSound> {
        val file = getSoundsListFile(context, categoryName, typeName)
        if (!file.exists()) return emptyList()
        
        return try {
            gson.fromJson(file.readText(), object : TypeToken<List<ScatteredSound>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun loadSoundMetadata(context: Context, categoryName: String, typeName: String, soundName: String): ScatteredSound? {
        val file = getMetadataFile(context, categoryName, typeName, soundName)
        if (!file.exists()) return null
        
        return try {
            gson.fromJson(file.readText(), ScatteredSound::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getCategoriesWithTypes(): List<ScatteredCategoryWithTypes> {
        val context = com.bicy.whitenoise.WhiteNoiseApp.context ?: return emptyList()
        val categories = loadCategories(context)
        
        return categories.map { category ->
            val types = loadTypesList(context, category.name)
            
            val typesWithSounds = types.map { type ->
                val sounds = loadSoundsList(context, category.name, type.name)
                
                val soundsWithType = sounds.map { sound ->
                    ScatteredSoundWithType(
                        id = sound.id,
                        name = sound.name,
                        typeId = sound.typeId,
                        typeName = type.name,
                        categoryId = sound.categoryId,
                        categoryName = category.name,
                        remoteUrl = sound.remoteUrl,
                        author = sound.author,
                        authorUrl = sound.authorUrl,
                        translations = sound.translations
                    )
                }
                
                ScatteredSoundTypeWithSounds(
                    typeId = type.id,
                    typeName = type.name,
                    categoryId = category.id,
                    categoryName = category.name,
                    translations = type.translations,
                    sounds = soundsWithType
                )
            }
            
            ScatteredCategoryWithTypes(
                categoryId = category.id,
                categoryName = category.name,
                translations = category.translations,
                soundTypes = typesWithSounds
            )
        }
    }
    
    fun getSoundTypesByCategory(categoryId: String): List<ScatteredSoundTypeWithSounds> {
        return getCategoriesWithTypes().find { it.categoryId == categoryId }?.soundTypes ?: emptyList()
    }
    
    fun getSoundsByType(typeId: String): List<ScatteredSoundWithType> {
        return getCategoriesWithTypes()
            .flatMap { it.soundTypes }
            .find { it.typeId == typeId }
            ?.sounds ?: emptyList()
    }
    
    fun getSoundById(soundId: String): ScatteredSoundWithType? {
        return getCategoriesWithTypes()
            .flatMap { it.soundTypes }
            .flatMap { it.sounds }
            .find { it.id == soundId }
    }
    
    fun updateSoundDownloadInfo(
        context: Context,
        categoryName: String,
        typeName: String,
        soundName: String,
        downloadDate: String,
        fileSize: Long
    ) {
        val metadata = loadSoundMetadata(context, categoryName, typeName, soundName) ?: return
        
        val updatedMetadata = metadata.copy(
            downloadDate = downloadDate,
            fileSize = fileSize
        )
        
        saveSoundMetadata(context, categoryName, typeName, soundName, updatedMetadata)
        
        val sounds = loadSoundsList(context, categoryName, typeName)
        val updatedSounds = sounds.map {
            if (it.id == metadata.id) updatedMetadata else it
        }
        saveSoundsList(context, categoryName, typeName, updatedSounds)
    }
    
    fun isSoundDownloaded(context: Context, categoryName: String, typeName: String, soundName: String): Boolean {
        val soundDir = getSoundDir(context, categoryName, typeName, soundName)
        if (!soundDir.exists() || !soundDir.isDirectory) return false
        
        val formats = listOf("mp3", "wav", "ogg", "m4a", "aac", "flac")
        return formats.any { format ->
            val audioFile = File(soundDir, "$AUDIO_FILE.$format")
            audioFile.exists() && audioFile.length() > 0
        }
    }
    
    fun getSoundAudioFile(context: Context, categoryName: String, typeName: String, soundName: String): File? {
        val soundDir = getSoundDir(context, categoryName, typeName, soundName)
        if (!soundDir.exists() || !soundDir.isDirectory) return null
        
        val formats = listOf("mp3", "wav", "ogg", "m4a", "aac", "flac")
        return formats.firstNotNullOfOrNull { format ->
            val audioFile = File(soundDir, "$AUDIO_FILE.$format")
            if (audioFile.exists() && audioFile.length() > 0) audioFile else null
        }
    }
}
