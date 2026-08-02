package com.bicy.whitenoise.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bicy.whitenoise.ui.components.glass.GlassAlertDialogSimple
import com.bicy.whitenoise.R
import com.bicy.whitenoise.floatingpet.FloatingPetImporter
import com.bicy.whitenoise.floatingpet.SpriteFrameLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FloatingPetSelectorDialog(
    currentPetId: String,
    onPetSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val availablePets = remember { mutableStateListOf<String>() }
    val selectedPetId = remember { mutableStateOf(currentPetId) }

    val previewBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val previewFrameIndex = remember { mutableIntStateOf(0) }
    val previewFrames = remember { mutableStateOf<List<Bitmap>?>(null) }

    // 导入相关状态
    var showPreviewDialog by remember { mutableStateOf(false) }
    var tempZipFile by remember { mutableStateOf<File?>(null) }
    var isTitleDuplicated by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }

    // 长按删除状态
    var petToDelete by remember { mutableStateOf<String?>(null) }

    // SAF 文件选择器
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val tempFile = FloatingPetImporter.getTempFile(context, uri)
                if (tempFile == null) {
                    importError = "无法读取文件"
                    return@launch
                }
                // 预检
                val (manifest, error) = FloatingPetImporter.precheckFull(context, uri)
                if (error != null) {
                    importError = error.reason + (error.detail?.let { "：$it" } ?: "")
                    tempFile.delete()
                    return@launch
                }
                if (manifest == null) {
                    importError = "manifast.json 解析失败"
                    tempFile.delete()
                    return@launch
                }
                // 检查重名
                val existingPets = SpriteFrameLoader.getAvailablePets(context)
                isTitleDuplicated = existingPets.contains(manifest.title)
                tempZipFile = tempFile
                showPreviewDialog = true
            }
        }
    }

    // 刷新宠物列表
    val refreshPets: () -> Unit = {
        scope.launch(Dispatchers.IO) {
            val pets = SpriteFrameLoader.getAvailablePets(context)
            availablePets.clear()
            availablePets.addAll(pets)
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pets = SpriteFrameLoader.getAvailablePets(context)
            availablePets.addAll(pets)

            if (pets.contains(currentPetId)) {
                val frames = SpriteFrameLoader.loadFrames(context, currentPetId, "idle")
                previewFrames.value = frames
            }
        }
    }

    LaunchedEffect(previewFrames.value) {
        val frames = previewFrames.value
        if (frames != null && frames.isNotEmpty()) {
            while (true) {
                previewBitmap.value = frames[previewFrameIndex.intValue]
                previewFrameIndex.intValue = (previewFrameIndex.intValue + 1) % frames.size
                delay(500)
            }
        }
    }

    GlassAlertDialogSimple(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.select_floating_pet),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                previewBitmap.value?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier.size(128.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availablePets) { petId ->
                    PetItem(
                        petId = petId,
                        isSelected = petId == selectedPetId.value,
                        isCustom = FloatingPetImporter.isCustomPet(context, petId),
                        onClick = {
                            selectedPetId.value = petId
                            scope.launch(Dispatchers.IO) {
                                val frames = SpriteFrameLoader.loadFrames(context, petId, "idle")
                                previewFrames.value = frames
                                previewFrameIndex.intValue = 0
                            }
                        },
                        onLongClick = {
                            if (FloatingPetImporter.isCustomPet(context, petId)) {
                                petToDelete = petId
                            }
                        }
                    )
                }

                // 导入按钮
                item {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                zipPickerLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "导入萌宠",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(onClick = {
                    onPetSelected(selectedPetId.value)
                    onDismiss()
                }) {
                    Text(text = stringResource(R.string.confirm))
                }
            }
        }
    }

    // 导入错误提示
    importError?.let { error ->
        AlertDialog(
            onDismissRequest = { importError = null },
            confirmButton = {
                TextButton(onClick = { importError = null }) { Text("确定") }
            },
            title = { Text("导入失败") },
            text = { Text(error) }
        )
    }

    // 预览弹窗
    if (showPreviewDialog && tempZipFile != null) {
        FloatingPetImportPreviewDialog(
            tempFile = tempZipFile!!,
            isTitleDuplicated = isTitleDuplicated,
            onDismiss = {
                showPreviewDialog = false
                tempZipFile?.delete()
                tempZipFile = null
            },
            onConfirm = {
                if (isTitleDuplicated || importing) return@FloatingPetImportPreviewDialog
                importing = true
                val fileToImport = tempZipFile!!
                scope.launch(Dispatchers.IO) {
                    // 重新从 URI 导入（确保完整性）
                    // 此处直接用 tempFile 解压
                    val result = importFromTempFile(context, fileToImport)
                    importing = false
                    withContext(Dispatchers.Main) {
                        showPreviewDialog = false
                        tempZipFile?.delete()
                        tempZipFile = null
                        when (result) {
                            is FloatingPetImporter.ImportResult.Success -> {
                                // 不 Toast，直接刷新列表
                                refreshPets()
                            }
                            is FloatingPetImporter.ImportResult.Error -> {
                                importError = result.reason + (result.detail?.let { "：$it" } ?: "")
                            }
                            else -> {}
                        }
                    }
                }
            }
        )
    }

    // 长按删除确认
    petToDelete?.let { petId ->
        AlertDialog(
            onDismissRequest = { petToDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        FloatingPetImporter.deletePet(context, petId)
                        withContext(Dispatchers.Main) {
                            // 如果删除的是当前选中的，切回第一个
                            if (selectedPetId.value == petId) {
                                selectedPetId.value = availablePets.firstOrNull { it != petId } ?: "Bicy"
                            }
                            refreshPets()
                        }
                    }
                    petToDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { petToDelete = null }) { Text("取消") }
            },
            title = { Text("删除萌宠") },
            text = { Text("确定要删除「$petId」吗？此操作不可撤销。") }
        )
    }
}

/** 从临时文件执行导入 */
private suspend fun importFromTempFile(
    context: android.content.Context,
    tempFile: File
): FloatingPetImporter.ImportResult {
    return withContext(Dispatchers.IO) {
        // 直接从 tempFile 解压（已通过预检）
        val manifest = FloatingPetImporter.getParsedManifest(tempFile)
        if (manifest == null) {
            return@withContext FloatingPetImporter.ImportResult.Error("manifast.json 解析失败")
        }

        val petsDir = com.bicy.whitenoise.storage.core.StorageManager.getFloatingPetsDir()
            ?: return@withContext FloatingPetImporter.ImportResult.Error("存储目录不可用")

        val destDir = File(petsDir, manifest.title)
        if (destDir.exists()) destDir.deleteRecursively()
        destDir.mkdirs()

        try {
            java.util.zip.ZipInputStream(java.io.FileInputStream(tempFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val normalizedPath = normalizePath(entry.name)
                        if (normalizedPath != null) {
                            val destFile = File(destDir, normalizedPath)
                            if (destFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                                destFile.parentFile?.mkdirs()
                                java.io.FileOutputStream(destFile).use { fos -> zis.copyTo(fos) }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // 验证完整性
            val manifestFile = File(destDir, "manifast.json")
            if (!manifestFile.exists()) {
                destDir.deleteRecursively()
                return@withContext FloatingPetImporter.ImportResult.Error("解压后 manifast.json 缺失")
            }

            // 验证 idle 帧存在
            val idleAnim = manifest.animations["idle"]
            if (idleAnim != null) {
                for (i in 0 until idleAnim.frameRate) {
                    val frameFile = File(destDir, "${idleAnim.image}_$i.png")
                    if (!frameFile.exists()) {
                        destDir.deleteRecursively()
                        return@withContext FloatingPetImporter.ImportResult.Error(
                            "idle 动作帧缺失",
                            "缺少帧：${idleAnim.image}_$i.png"
                        )
                    }
                }
            }

            SpriteFrameLoader.invalidatePet(manifest.title)
            FloatingPetImporter.ImportResult.Success(manifest.title, manifest.toConfig())
        } catch (e: Exception) {
            destDir.deleteRecursively()
            FloatingPetImporter.ImportResult.Error("导入失败", e.message)
        }
    }
}

private fun normalizePath(rawPath: String): String? {
    var path = rawPath.replace("\\", "/").trim()
    if (path.contains("__MACOSX") || path.startsWith(".")) return null
    while (path.startsWith("./")) path = path.removePrefix("./")
    val parts = path.split("/")
    if (parts.size <= 1) return path
    val firstSeg = parts[0]
    if (firstSeg == "manifast.json" || firstSeg in listOf("idle", "move", "hide_bottom", "hide_top", "hide_left", "hide_right")) {
        return path
    }
    return parts.drop(1).joinToString("/")
}

private fun com.bicy.whitenoise.floatingpet.FloatingPetImporter.ParsedManifest.toConfig():
        com.bicy.whitenoise.floatingpet.SpriteFrameConfig {
    val animMap = animations.mapValues { (_, spec) ->
        com.bicy.whitenoise.floatingpet.AnimationConfig(spec.image, spec.frameRate, spec.speed)
    }
    return com.bicy.whitenoise.floatingpet.SpriteFrameConfig(title, width, height, animMap)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PetItem(
    petId: String,
    isSelected: Boolean,
    isCustom: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(petId) {
        withContext(Dispatchers.IO) {
            thumbnail = SpriteFrameLoader.getBitmap(context, petId, "idle", 0)
        }
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        thumbnail?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = petId,
                modifier = Modifier.size(48.dp)
            )
        }

        // 自定义萌宠标记（右下角小圆点）
        if (isCustom) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
