package com.bicy.whitenoise.utils

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.storage.core.JsonStorageManager
import com.bicy.whitenoise.subPage.scattered.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlinx.coroutines.runBlocking

data class ScatteredCategoryEntity(
    val id: String,
    val name: String,
    val isCustom: Boolean = false,
    val translationsJson: String? = null
)

data class ScatteredTypeEntity(
    val id: String,
    val name: String,
    val categoryId: String,
    val translationsJson: String? = null
)

data class ScatteredSoundEntity(
    val id: String,
    val name: String,
    val typeId: String,
    val categoryId: String,
    val remoteUrl: String? = null,
    val author: String? = null,
    val authorUrl: String? = null,
    val translationsJson: String? = null,
    val downloadDate: String? = null,
    val fileSize: Long? = null
)

object ScatteredStorageManager {
    
    private const val TAG = "ScatteredStorageManager"
    private const val SCATTERED_DIR = "white_noise/scattered"
    private const val AUDIO_FILE = "audio"
    private const val CATEGORIES_FILE = "scattered_categories.json"
    private const val TYPES_FILE = "scattered_types.json"
    private const val SOUNDS_FILE = "scattered_sounds.json"
    
    private val gson = Gson()
    
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
    
    // ===== JSON helpers =====
    
    private fun readCategories(): List<ScatteredCategoryEntity> = runBlocking {
        JsonStorageManager.read(CATEGORIES_FILE, Array<ScatteredCategoryEntity>::class.java)?.toList()
            ?: emptyList()
    }
    
    private fun writeCategories(entities: List<ScatteredCategoryEntity>) = runBlocking {
        JsonStorageManager.write(CATEGORIES_FILE, entities)
    }
    
    private fun readTypes(): List<ScatteredTypeEntity> = runBlocking {
        JsonStorageManager.read(TYPES_FILE, Array<ScatteredTypeEntity>::class.java)?.toList()
            ?: emptyList()
    }
    
    private fun writeTypes(entities: List<ScatteredTypeEntity>) = runBlocking {
        JsonStorageManager.write(TYPES_FILE, entities)
    }
    
    private fun readSounds(): List<ScatteredSoundEntity> = runBlocking {
        JsonStorageManager.read(SOUNDS_FILE, Array<ScatteredSoundEntity>::class.java)?.toList()
            ?: emptyList()
    }
    
    private fun writeSounds(entities: List<ScatteredSoundEntity>) = runBlocking {
        JsonStorageManager.write(SOUNDS_FILE, entities)
    }
    
    private fun getScatteredDir(context: Context): File {
        val dir = File(context.filesDir, SCATTERED_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    private fun getSoundDir(context: Context, categoryName: String, typeName: String, soundName: String): File {
        val categoryDir = File(getScatteredDir(context), categoryName)
        val typeDir = File(categoryDir, typeName)
        return File(typeDir, soundName)
    }
    
    fun init(context: Context) {
        JsonStorageManager.init(AppInitializer.getContext())
        val existingCategories = readCategories()
        if (existingCategories.isEmpty()) {
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
            
            val categoryEntities = manifest.categories.map { category ->
                val categoryTranslations = mutableMapOf<String, String>()
                manifest.Language.forEach { (langCode, langTranslations) ->
                    langTranslations[category.name]?.let { categoryTranslations[langCode] = it }
                }
                
                ScatteredCategoryEntity(
                    id = category.id,
                    name = category.name,
                    isCustom = false,
                    translationsJson = if (categoryTranslations.isNotEmpty()) gson.toJson(categoryTranslations) else null
                )
            }
            
            val typeEntities = manifest.soundTypes.map { soundType ->
                val typeTranslations = mutableMapOf<String, String>()
                manifest.Language.forEach { (langCode, langTranslations) ->
                    langTranslations[soundType.name]?.let { typeTranslations[langCode] = it }
                }
                
                ScatteredTypeEntity(
                    id = soundType.id,
                    name = soundType.name,
                    categoryId = soundType.category,
                    translationsJson = if (typeTranslations.isNotEmpty()) gson.toJson(typeTranslations) else null
                )
            }
            
            val soundEntities = manifest.sounds.map { sound ->
                val soundType = manifest.soundTypes.find { it.id == sound.type }
                val categoryId = soundType?.category ?: ""
                
                val soundTranslations = mutableMapOf<String, String>()
                manifest.Language.forEach { (langCode, langTranslations) ->
                    langTranslations[sound.name]?.let { soundTranslations[langCode] = it }
                }
                
                ScatteredSoundEntity(
                    id = sound.id,
                    name = sound.name,
                    typeId = sound.type,
                    categoryId = categoryId,
                    remoteUrl = sound.remoteUrl,
                    author = sound.author,
                    authorUrl = sound.authorUrl,
                    translationsJson = if (soundTranslations.isNotEmpty()) gson.toJson(soundTranslations) else null
                )
            }
            
            writeCategories(categoryEntities)
            writeTypes(typeEntities)
            writeSounds(soundEntities)
            
            Log.d(TAG, "从scattered_sounds.json初始化完成: categories=${categoryEntities.size}, types=${typeEntities.size}, sounds=${soundEntities.size}")
        } catch (e: Exception) {
            Log.e(TAG, "初始化散点白噪音清单失败", e)
            createDefaultCategories()
        }
    }
    
    private fun createDefaultCategories() {
        writeCategories(listOf(
            ScatteredCategoryEntity(
                id = "objects",
                name = "objects",
                isCustom = false
            )
        ))
        Log.d(TAG, "创建默认分类清单")
    }
    
    fun getCategoriesWithTypes(): List<ScatteredCategoryWithTypes> {
        val categoryEntities = readCategories()
        val typeEntities = readTypes()
        val soundEntities = readSounds()
        
        return categoryEntities.map { catEntity ->
            val typesInCategory = typeEntities.filter { it.categoryId == catEntity.id }
            
            val typesWithSounds = typesInCategory.map { typeEntity ->
                val soundsInType = soundEntities.filter { it.typeId == typeEntity.id }
                
                val soundsWithType = soundsInType.map { soundEntity ->
                    ScatteredSoundWithType(
                        id = soundEntity.id,
                        name = soundEntity.name,
                        typeId = soundEntity.typeId,
                        typeName = typeEntity.name,
                        categoryId = soundEntity.categoryId,
                        categoryName = catEntity.name,
                        remoteUrl = soundEntity.remoteUrl,
                        author = soundEntity.author,
                        authorUrl = soundEntity.authorUrl,
                        translations = deserializeTranslations(soundEntity.translationsJson)
                    )
                }
                
                ScatteredSoundTypeWithSounds(
                    typeId = typeEntity.id,
                    typeName = typeEntity.name,
                    categoryId = catEntity.id,
                    categoryName = catEntity.name,
                    translations = deserializeTranslations(typeEntity.translationsJson),
                    sounds = soundsWithType
                )
            }
            
            ScatteredCategoryWithTypes(
                categoryId = catEntity.id,
                categoryName = catEntity.name,
                translations = deserializeTranslations(catEntity.translationsJson),
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
        val allSounds = readSounds()
        val allTypes = readTypes()
        
        val targetType = allTypes.find { it.name == typeName } ?: return
        val targetSound = allSounds.find { it.name == soundName && it.typeId == targetType.id } ?: return
        
        val updatedSound = targetSound.copy(
            downloadDate = downloadDate,
            fileSize = fileSize
        )
        
        val updatedAllSounds = allSounds.map {
            if (it.id == updatedSound.id) updatedSound else it
        }
        writeSounds(updatedAllSounds)
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
    
    private fun deserializeTranslations(translationsJson: String?): Map<String, String>? {
        if (translationsJson.isNullOrEmpty()) return null
        return try {
            gson.fromJson(translationsJson, object : TypeToken<Map<String, String>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }
}
