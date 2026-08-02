package com.bicy.whitenoise.utils

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.storage.core.JsonStorageManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlinx.coroutines.runBlocking
import com.bicy.whitenoise.subPage.home.model.SoundMetadataPart.*

data class WhiteNoiseCategoryEntity(
    val id: String,
    val name: String,
    val translationsJson: String? = null,
    val isCustom: Boolean = false,
    val sortOrder: Int = 0
)

data class WhiteNoiseEntity(
    val id: String,
    val name: String,
    val displayName: String,
    val category: String,
    val assetPath: String?,
    val customPath: String?,
    val duration: Long = 0,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val remoteUrl: String?,
    val author: String?,
    val authorUrl: String?,
    val type: String = "NETWORK_DOWNLOAD",
    val downloadDate: Long? = null,
    val fileSize: Long? = null,
    val uriString: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

data class SoundMetadataEntity(
    val soundId: String,
    val type: String = "NETWORK_DOWNLOAD",
    val description: String = "",
    val tagsJson: String = "[]",
    val downloadDate: Long? = null,
    val fileSize: Long? = null
)

object SoundStorageManager {

    private const val TAG = "SoundStorageManager"
    const val UNCATEGORIZED_NAME = "category_uncategorized"

    private val gson = Gson()

    private const val LIBRARY_DIR = "white_noise/library"
    private const val CATEGORIES_FILE = "white_noise_categories.json"
    private const val WHITE_NOISE_FILE = "white_noise.json"
    private const val SOUND_METADATA_FILE = "sound_metadata.json"

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

    // ===== JSON helpers =====

    private fun readCategories(): List<WhiteNoiseCategoryEntity> = runBlocking {
        JsonStorageManager.read(CATEGORIES_FILE, Array<WhiteNoiseCategoryEntity>::class.java)?.toList()
            ?: emptyList()
    }

    private fun writeCategories(categories: List<WhiteNoiseCategoryEntity>) = runBlocking {
        JsonStorageManager.write(CATEGORIES_FILE, categories)
    }

    private fun readWhiteNoise(): List<WhiteNoiseEntity> = runBlocking {
        JsonStorageManager.read(WHITE_NOISE_FILE, Array<WhiteNoiseEntity>::class.java)?.toList()
            ?: emptyList()
    }

    private fun writeWhiteNoise(entities: List<WhiteNoiseEntity>) = runBlocking {
        JsonStorageManager.write(WHITE_NOISE_FILE, entities)
    }

    private fun readSoundMetadata(): List<SoundMetadataEntity> = runBlocking {
        JsonStorageManager.read(SOUND_METADATA_FILE, Array<SoundMetadataEntity>::class.java)?.toList()
            ?: emptyList()
    }

    private fun writeSoundMetadata(entities: List<SoundMetadataEntity>) = runBlocking {
        JsonStorageManager.write(SOUND_METADATA_FILE, entities)
    }

    // ===== Entity ↔ Model conversions =====

    private fun WhiteNoiseCategoryEntity.toSoundClass(): SoundClass {
        return SoundClass(
            id = id,
            name = name,
            isCustom = isCustom
        )
    }

    private fun SoundClass.toCategoryEntity(sortOrder: Int): WhiteNoiseCategoryEntity {
        return WhiteNoiseCategoryEntity(
            id = id,
            name = name,
            isCustom = isCustom,
            sortOrder = sortOrder
        )
    }

    private fun WhiteNoiseEntity.toSoundItem(): SoundItem {
        return SoundItem(
            id = id,
            name = name,
            remoteUrl = remoteUrl,
            author = author,
            authorUrl = authorUrl
        )
    }

    private fun SoundItem.toWhiteNoiseEntity(categoryName: String): WhiteNoiseEntity {
        return WhiteNoiseEntity(
            id = id,
            name = name,
            displayName = name,
            category = categoryName,
            assetPath = null,
            customPath = null,
            duration = 0,
            isCustom = false,
            isFavorite = false,
            remoteUrl = remoteUrl,
            author = author,
            authorUrl = authorUrl,
            type = SoundSourceType.NETWORK_DOWNLOAD.name,
            downloadDate = null,
            fileSize = null,
            uriString = null,
            addedAt = System.currentTimeMillis()
        )
    }

    private fun SoundMetadataEntity.toSoundType(): SoundType {
        val translationsJson = try {
            if (tagsJson.isNotBlank()) {
                val mapType = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson<Map<String, String>>(tagsJson, mapType)
            } else null
        } catch (e: Exception) {
            null
        }
        return SoundType(
            type = try { SoundSourceType.valueOf(type) } catch (e: IllegalArgumentException) { SoundSourceType.NETWORK_DOWNLOAD },
            nameKey = description,
            translations = translationsJson,
            downloadDate = downloadDate?.toString(),
            fileSize = fileSize
        )
    }

    private fun createSoundMetadataEntity(
        soundId: String,
        soundType: SoundType
    ): SoundMetadataEntity {
        val translationsJson = soundType.translations?.let { gson.toJson(it) } ?: "{}"
        val downloadDateLong = soundType.downloadDate?.toLongOrNull()
        return SoundMetadataEntity(
            soundId = soundId,
            type = soundType.type.name,
            description = soundType.nameKey,
            tagsJson = translationsJson,
            downloadDate = downloadDateLong,
            fileSize = soundType.fileSize
        )
    }

    // ===== File system helpers (for audio files, NOT JSON) =====

    private fun getLibraryDir(context: Context): File {
        val dir = File(context.filesDir, LIBRARY_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getCategoryDir(context: Context, categoryName: String): File {
        return File(getLibraryDir(context), categoryName)
    }

    private fun getSoundDir(context: Context, categoryName: String, soundName: String): File {
        return File(getCategoryDir(context, categoryName), soundName)
    }

    fun getSoundFile(context: Context, categoryName: String, soundName: String, format: String): File {
        val soundDir = getSoundDir(context, categoryName, soundName)
        return File(soundDir, "audio.$format")
    }

    fun getExistingSoundFile(context: Context, categoryName: String, soundName: String, format: String): File? {
        val file = getSoundFile(context, categoryName, soundName, format)
        return if (file.exists() && file.length() > 0) file else null
    }

    // ===== Initialization =====

    fun init(context: Context) {
        JsonStorageManager.init(AppInitializer.getContext())
        val categories = readCategories()
        if (categories.isEmpty() || (categories.size == 1 && categories[0].id == "uncategorized" && categories[0].name == UNCATEGORIZED_NAME)) {
            val hasOnlyUncategorized = categories.size == 1 && categories[0].id == "uncategorized"
            if (hasOnlyUncategorized && categories.size == 1) {
                Log.d(TAG, "检测到只有未分类，重新初始化")
            }
            initializeFromRemoteManifest(context)
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
                SoundsManifest::class.java
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

            val allWhiteNoiseEntities = mutableListOf<WhiteNoiseEntity>()
            val allSoundMetadataEntities = mutableListOf<SoundMetadataEntity>()

            manifest.categories.forEach { category ->
                Log.d(TAG, "处理分类: ${category.id} -> ${category.name}")

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

                allWhiteNoiseEntities.addAll(soundItems.map { it.toWhiteNoiseEntity(category.name) })

                soundItems.forEach { soundItem ->
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

                    allSoundMetadataEntities.add(createSoundMetadataEntity(soundItem.id, soundType))
                }
            }

            writeWhiteNoise(allWhiteNoiseEntities)
            Log.d(TAG, "保存白噪音实体成功: ${allWhiteNoiseEntities.size}个")

            writeSoundMetadata(allSoundMetadataEntities)
            Log.d(TAG, "保存音频元数据成功: ${allSoundMetadataEntities.size}个")

            Log.d(TAG, "从sounds_remote.json初始化完成: ${soundClasses.size}个分类")
        } catch (e: Exception) {
            Log.e(TAG, "从sounds_remote.json初始化失败: ${e.message}", e)
            e.printStackTrace()
            createDefaultSoundsClass(context)
        }
    }

    fun reinitializeFromRemoteManifest(context: Context) {
        initializeFromRemoteManifest(context)
        Log.d(TAG, "重新初始化白噪音列表完成")
    }

    private fun createDefaultSoundsClass(context: Context) {
        val defaultClasses = listOf(
            SoundClass(
                id = "uncategorized",
                name = UNCATEGORIZED_NAME,
                isCustom = false
            )
        )

        try {
            val entities = defaultClasses.mapIndexed { index, sc ->
                sc.toCategoryEntity(sortOrder = index)
            }
            writeCategories(entities)
            Log.d(TAG, "创建默认分类清单: ${defaultClasses.size}个分类")
        } catch (e: Exception) {
            Log.e(TAG, "创建默认分类失败: ${e.message}", e)
        }
    }

    // ===== SoundClass (categories) operations =====

    fun loadSoundsClass(context: Context): List<SoundClass> {
        return try {
            val entities = readCategories()
            if (entities.isEmpty()) {
                createDefaultSoundsClass(context)
                readCategories().map { it.toSoundClass() }
            } else {
                entities.map { it.toSoundClass() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载分类清单失败: ${e.message}", e)
            createDefaultSoundsClass(context)
            return try {
                readCategories().map { it.toSoundClass() }
            } catch (ex: Exception) {
                emptyList()
            }
        }
    }

    fun saveSoundsClass(context: Context, classList: List<SoundClass>) {
        try {
            val entities = classList.mapIndexed { index, sc ->
                sc.toCategoryEntity(sortOrder = index)
            }
            writeCategories(entities)
            Log.d(TAG, "保存分类清单成功: ${classList.size}个分类")
        } catch (e: Exception) {
            Log.e(TAG, "保存分类清单失败: ${e.message}", e)
        }
    }

    // ===== SoundItem (sounds list) operations =====

    fun loadSoundsList(context: Context, categoryName: String): List<SoundItem> {
        return try {
            readWhiteNoise().filter { it.category == categoryName }.map { it.toSoundItem() }
        } catch (e: Exception) {
            Log.e(TAG, "加载音频清单失败 ($categoryName): ${e.message}", e)
            emptyList()
        }
    }

    fun saveSoundsList(context: Context, categoryName: String, soundList: List<SoundItem>) {
        try {
            val allSounds = readWhiteNoise()
            val otherSounds = allSounds.filter { it.category != categoryName }
            val newSounds = soundList.map { it.toWhiteNoiseEntity(categoryName) }
            writeWhiteNoise(otherSounds + newSounds)
            Log.d(TAG, "保存音频清单成功 ($categoryName): ${soundList.size}个音频")
        } catch (e: Exception) {
            Log.e(TAG, "保存音频清单失败 ($categoryName): ${e.message}", e)
        }
    }

    // ===== SoundType operations =====

    fun loadSoundType(context: Context, categoryName: String, soundName: String): SoundType? {
        return try {
            val wnEntity = readWhiteNoise()
                .firstOrNull { it.category == categoryName && it.name == soundName }
                ?: return null
            val smEntity = readSoundMetadata().find { it.soundId == wnEntity.id }
            smEntity?.toSoundType()
        } catch (e: Exception) {
            Log.e(TAG, "加载音频类型失败 ($categoryName/$soundName): ${e.message}", e)
            null
        }
    }

    fun saveSoundType(context: Context, categoryName: String, soundName: String, soundType: SoundType) {
        try {
            val allWhiteNoise = readWhiteNoise()
            val wnEntity = allWhiteNoise
                .firstOrNull { it.category == categoryName && it.name == soundName }

            if (wnEntity != null) {
                val soundId = wnEntity.id
                val metadataEntity = createSoundMetadataEntity(soundId, soundType)
                val allMetadata = readSoundMetadata().toMutableList()
                allMetadata.removeAll { it.soundId == soundId }
                allMetadata.add(metadataEntity)
                writeSoundMetadata(allMetadata)

                // Also update the WhiteNoiseEntity with type-related fields
                val updatedWn = wnEntity.copy(
                    type = soundType.type.name,
                    downloadDate = soundType.downloadDate?.toLongOrNull(),
                    fileSize = soundType.fileSize,
                    duration = soundType.duration ?: wnEntity.duration,
                    author = soundType.author ?: wnEntity.author,
                    authorUrl = soundType.authorUrl ?: wnEntity.authorUrl,
                    remoteUrl = soundType.downloadUrl ?: wnEntity.remoteUrl
                )
                val updatedAll = allWhiteNoise.map {
                    if (it.id == soundId) updatedWn else it
                }
                writeWhiteNoise(updatedAll)

                Log.d(TAG, "保存音频类型成功 ($categoryName/$soundName): ${soundType.type}")
            } else {
                Log.w(TAG, "保存音频类型失败：找不到对应白噪音实体 ($categoryName/$soundName)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存音频类型失败 ($categoryName/$soundName): ${e.message}", e)
        }
    }

    // ===== Category management =====

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

        // Save empty sounds list for the new category
        saveSoundsList(context, name, emptyList())

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

        val soundId = "sound_${System.currentTimeMillis()}"
        val newSound = SoundItem(
            id = soundId,
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

        // Delete category sounds from WhiteNoise
        try {
            val allSounds = readWhiteNoise()
            val remainingSounds = allSounds.filter { it.category != categoryName }
            writeWhiteNoise(remainingSounds)
        } catch (e: Exception) {
            Log.e(TAG, "删除分类音频失败: ${e.message}", e)
        }

        // Delete audio files directory
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

        // Delete audio files directory
        val soundDir = getSoundDir(context, categoryName, soundName)
        if (soundDir.exists()) {
            soundDir.deleteRecursively()
        }

        return true
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
