package com.bicy.whitenoise.floatingpet

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bicy.whitenoise.service.AnomalyType
import com.bicy.whitenoise.service.MemoryLockService
import com.bicy.whitenoise.storage.core.StorageManager
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * 悬浮萌宠 ZIP 导入器
 *
 * ZIP 结构（参照 Bicy）：根目录即萌宠目录，含 manifast.json + 各动作子目录
 * - manifast.json：title/width/height 必填，至少含 idle 动作
 * - 帧图片仅支持 PNG
 * - petId = manifast.json 的 title
 */
object FloatingPetImporter {

    private const val TAG = "FloatingPetImporter"
    private const val MAX_PACKAGE_SIZE = 50L * 1024 * 1024 // 50MB 警告阈值
    private const val MANIFEST_NAME = "manifast.json"
    private val VALID_ACTIONS = listOf("idle", "move", "hide_bottom", "hide_top", "hide_left", "hide_right")

    /** 导入结果 */
    sealed class ImportResult {
        data class Success(val petId: String, val config: SpriteFrameConfig) : ImportResult()
        data class Error(val reason: String, val detail: String? = null) : ImportResult()
        data class SizeWarning(val sizeBytes: Long, val manifest: ParsedManifest) : ImportResult()
    }

    /** 预检结果：用于预览阶段（未真正解压） */
    data class ParsedManifest(
        val title: String,
        val width: Int,
        val height: Int,
        val animations: Map<String, AnimationSpec>,
        val totalFrameCount: Int,
        val zipSizeBytes: Long
    )

    data class AnimationSpec(
        val image: String,
        val frameRate: Int,
        val speed: Float
    )

    /**
     * 预检：从 URI 读取 ZIP，解析 manifast.json，不真正解压
     * 用于预览弹窗展示和重名检查
     */
    fun precheck(context: Context, uri: Uri): ImportResult {
        return try {
            val tempFile = copyToTemp(context, uri)
            val size = tempFile.length()
            val manifest = parseManifestFromZip(tempFile)
                ?: return ImportResult.Error("manifast.json 缺失或格式错误", "ZIP 内未找到有效的 manifast.json")

            // 检查 title 重复
            val existingPets = SpriteFrameLoader.getAvailablePets(context)
            if (existingPets.contains(manifest.title)) {
                return ImportResult.Error("已存在相同动画", "名为「${manifest.title}」的萌宠已存在")
            }

            if (size > MAX_PACKAGE_SIZE) {
                return ImportResult.SizeWarning(size, manifest)
            }

            ImportResult.Success(manifest.title, manifest.toConfig())
        } catch (e: Exception) {
            Log.e(TAG, "Precheck failed", e)
            MemoryLockService.reportAnomaly(AnomalyType.IO_ERROR, "萌宠导入预检失败", e.stackTraceToString())
            ImportResult.Error("预检失败", e.message)
        }
    }

    /**
     * 完整预检（含大包警告后用户确认时调用）
     * 返回 ParsedManifest 供预览使用
     */
    fun precheckFull(context: Context, uri: Uri): Pair<ParsedManifest?, ImportResult.Error?> {
        return try {
            val tempFile = copyToTemp(context, uri)
            val manifest = parseManifestFromZip(tempFile)
            if (manifest == null) {
                return null to ImportResult.Error("manifast.json 缺失或格式错误")
            }
            val existingPets = SpriteFrameLoader.getAvailablePets(context)
            if (existingPets.contains(manifest.title)) {
                return null to ImportResult.Error("已存在相同动画", "名为「${manifest.title}」的萌宠已存在")
            }
            manifest to null
        } catch (e: Exception) {
            null to ImportResult.Error("预检失败", e.message)
        }
    }

    /**
     * 执行导入：解压 ZIP 到 getFloatingPetsDir()/{title}/
     * 调用前应已通过 precheck 确认无重名
     */
    fun import(context: Context, uri: Uri, onProgress: ((Float) -> Unit)? = null): ImportResult {
        return try {
            val tempFile = copyToTemp(context, uri)
            val manifest = parseManifestFromZip(tempFile)
                ?: return ImportResult.Error("manifast.json 缺失或格式错误")

            val existingPets = SpriteFrameLoader.getAvailablePets(context)
            if (existingPets.contains(manifest.title)) {
                return ImportResult.Error("已存在相同动画", "名为「${manifest.title}」的萌宠已存在")
            }

            val petsDir = StorageManager.getFloatingPetsDir()
                ?: return ImportResult.Error("存储目录不可用")

            val destDir = File(petsDir, manifest.title)
            if (destDir.exists()) destDir.deleteRecursively()
            destDir.mkdirs()

            // 统计总条目数用于进度
            val totalEntries = countZipEntries(tempFile)
            var extracted = 0

            ZipInputStream(FileInputStream(tempFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    // 跳过目录条目
                    if (!entry.isDirectory) {
                        // 规范化路径：去除可能的前导目录
                        val normalizedPath = normalizeEntryPath(entryName)
                        if (normalizedPath != null) {
                            val destFile = File(destDir, normalizedPath)
                            // 防止 Zip Slip 攻击
                            if (destFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                                destFile.parentFile?.mkdirs()
                                FileOutputStream(destFile).use { fos -> zis.copyTo(fos) }

                                // 校验 PNG 格式（仅对图片文件）
                                if (normalizedPath.endsWith(".png", ignoreCase = true)) {
                                    if (!isPngFile(destFile)) {
                                        destFile.deleteRecursively()
                                        zis.closeEntry()
                                        destDir.deleteRecursively()
                                        return ImportResult.Error("帧图片格式错误", "仅支持 PNG 格式：$normalizedPath")
                                    }
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    extracted++
                    onProgress?.invoke(extracted.toFloat() / totalEntries.coerceAtLeast(1))
                    entry = zis.nextEntry
                }
            }

            // 验证解压后的 manifast.json 和帧文件完整性
            val manifestFile = File(destDir, MANIFEST_NAME)
            if (!manifestFile.exists()) {
                destDir.deleteRecursively()
                return ImportResult.Error("解压后 manifast.json 缺失")
            }

            // 验证 idle 动作帧存在
            val idleAnim = manifest.animations["idle"]
            if (idleAnim != null) {
                for (i in 0 until idleAnim.frameRate) {
                    val frameFile = File(destDir, "${idleAnim.image}_$i.png")
                    if (!frameFile.exists()) {
                        destDir.deleteRecursively()
                        return ImportResult.Error("idle 动作帧缺失", "缺少帧：${idleAnim.image}_$i.png")
                    }
                }
            }

            // 清除 SpriteFrameLoader 缓存，确保下次加载从磁盘读取
            SpriteFrameLoader.invalidatePet(manifest.title)

            Log.d(TAG, "Imported pet: ${manifest.title} (${manifest.totalFrameCount} frames)")
            ImportResult.Success(manifest.title, manifest.toConfig())
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            MemoryLockService.reportAnomaly(AnomalyType.IO_ERROR, "萌宠导入失败", e.stackTraceToString())
            ImportResult.Error("导入失败", e.message)
        }
    }

    /**
     * 从临时文件加载预览帧（用于预览弹窗，不依赖解压目录）
     * 按 index 排序返回
     */
    fun loadPreviewFrames(tempFile: File, animationKey: String): List<android.graphics.Bitmap>? {
        return try {
            val manifest = parseManifestFromZip(tempFile) ?: return null
            val anim = manifest.animations[animationKey] ?: return null
            val frameMap = mutableMapOf<Int, android.graphics.Bitmap>()

            ZipInputStream(FileInputStream(tempFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val normalizedPath = normalizeEntryPath(entry.name)
                    if (normalizedPath != null && !entry.isDirectory) {
                        val prefix = "${anim.image}_"
                        if (normalizedPath.startsWith(prefix) && normalizedPath.endsWith(".png", ignoreCase = true)) {
                            val indexPart = normalizedPath.removePrefix(prefix).removeSuffix(".png")
                            val index = indexPart.toIntOrNull()
                            if (index != null && index in 0 until anim.frameRate && !frameMap.containsKey(index)) {
                                val bitmap = android.graphics.BitmapFactory.decodeStream(zis)
                                if (bitmap != null) frameMap[index] = bitmap
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (frameMap.isEmpty()) null
            else frameMap.toSortedMap().values.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Load preview frames failed: $animationKey", e)
            null
        }
    }

    /** 获取已解析的 manifest（从临时文件） */
    fun getParsedManifest(tempFile: File): ParsedManifest? = parseManifestFromZip(tempFile)

    /**
     * 获取临时文件（供预览弹窗复用，避免重复复制）
     */
    fun getTempFile(context: Context, uri: Uri): File? {
        return try {
            copyToTemp(context, uri)
        } catch (e: Exception) {
            Log.e(TAG, "Copy to temp failed", e)
            null
        }
    }

    /**
     * 删除自定义萌宠
     */
    fun deletePet(context: Context, petId: String): Boolean {
        return try {
            // 不允许删除 assets 内的预设萌宠，仅删除内部存储中的
            val petsDir = StorageManager.getFloatingPetsDir() ?: return false
            val petDir = File(petsDir, petId)
            if (petDir.exists() && petDir.isDirectory) {
                SpriteFrameLoader.invalidatePet(petId)
                petDir.deleteRecursively()
                Log.d(TAG, "Deleted pet: $petId")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete pet failed: $petId", e)
            false
        }
    }

    /**
     * 判断萌宠是否为自定义（可删除）
     * assets 内的为预设（不可删），内部存储的为自定义（可删）
     */
    fun isCustomPet(context: Context, petId: String): Boolean {
        val petsDir = StorageManager.getFloatingPetsDir() ?: return false
        val petDir = File(petsDir, petId)
        // 存在于内部存储且不在 assets 中 → 自定义
        // 存在于内部存储但在 assets 中 → 预设（FloatingPetInitializer 解压的）
        val inStorage = petDir.exists() && File(petDir, MANIFEST_NAME).exists()
        if (!inStorage) return false
        // 检查是否也在 assets 中
        return try {
            context.assets.open("Floating_Sprite_Frame/$petId/$MANIFEST_NAME").close()
            false // 在 assets 中 → 预设
        } catch (e: Exception) {
            true // 不在 assets 中 → 自定义
        }
    }

    // ==================== 内部方法 ====================

    private fun copyToTemp(context: Context, uri: Uri): File {
        val tempFile = File(context.cacheDir, "pet_import_${System.currentTimeMillis()}.zip")
        context.contentResolver.openInputStream(uri).use { input ->
            if (input == null) throw IllegalStateException("无法读取文件 URI")
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    private fun parseManifestFromZip(zipFile: File): ParsedManifest? {
        return try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val normalizedPath = normalizeEntryPath(entry.name)
                    if (normalizedPath == MANIFEST_NAME && !entry.isDirectory) {
                        val jsonStr = zis.bufferedReader().use { it.readText() }
                        return parseManifestJson(jsonStr, zipFile.length())
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Parse manifest failed", e)
            null
        }
    }

    private fun parseManifestJson(jsonStr: String, zipSize: Long): ParsedManifest? {
        return try {
            val json = JSONObject(jsonStr)
            // 严格校验必填字段
            if (!json.has("title")) return null
            if (!json.has("width")) return null
            if (!json.has("height")) return null

            val title = json.getString("title").trim()
            if (title.isEmpty()) return null

            val width = json.getInt("width")
            val height = json.getInt("height")
            if (width <= 0 || height <= 0) return null

            // 至少含 idle
            if (!json.has("idle")) return null

            val animations = mutableMapOf<String, AnimationSpec>()
            var totalFrames = 0
            for (key in VALID_ACTIONS) {
                if (json.has(key)) {
                    val animJson = json.getJSONObject(key)
                    val image = animJson.getString("image")
                    val frameRate = animJson.getInt("frame_rate")
                    val speed = animJson.getDouble("speed").toFloat()
                    if (frameRate <= 0 || image.isEmpty()) return null
                    animations[key] = AnimationSpec(image, frameRate, speed)
                    totalFrames += frameRate
                }
            }

            // idle 必须存在且帧数 > 0
            val idle = animations["idle"] ?: return null
            if (idle.frameRate <= 0) return null

            ParsedManifest(title, width, height, animations, totalFrames, zipSize)
        } catch (e: Exception) {
            Log.e(TAG, "Parse manifest JSON failed", e)
            null
        }
    }

    /**
     * 规范化 ZIP 条目路径：
     * - 去除前导目录（如 MyPet/manifast.json → manifast.json）
     * - 去除 ./ 前缀
     * - 返回 null 表示应跳过（如 __MACOSX 等）
     */
    private fun normalizeEntryPath(rawPath: String): String? {
        var path = rawPath.replace("\\", "/").trim()
        // 跳过 macOS 系统目录
        if (path.contains("__MACOSX") || path.startsWith(".")) return null
        // 去除 ./ 前缀
        while (path.startsWith("./")) path = path.removePrefix("./")
        // 如果有多级目录，检测是否需要去除前导目录
        // manifast.json 可能在根目录或一级子目录下
        val parts = path.split("/")
        if (parts.size <= 1) return path
        // 如果直接是 manifast.json 或已知动作目录结构，保留
        // 判断：如果第一段是 manifast.json/idle/move/hide_*，则路径已经是根级
        val firstSeg = parts[0]
        if (firstSeg == MANIFEST_NAME || firstSeg in VALID_ACTIONS) return path
        // 否则去除第一级目录（ZIP 根目录即萌宠目录的兼容处理）
        return parts.drop(1).joinToString("/")
    }

    private fun countZipEntries(zipFile: File): Int {
        return try {
            var count = 0
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) count++
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            count
        } catch (e: Exception) {
            1
        }
    }

    /** 校验 PNG 文件头 */
    private fun isPngFile(file: File): Boolean {
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(8)
                val read = fis.read(header)
                read == 8 &&
                    header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
                    header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun ParsedManifest.toConfig(): SpriteFrameConfig {
        val animMap = animations.mapValues { (_, spec) ->
            AnimationConfig(spec.image, spec.frameRate, spec.speed)
        }
        return SpriteFrameConfig(title, width, height, animMap)
    }
}
