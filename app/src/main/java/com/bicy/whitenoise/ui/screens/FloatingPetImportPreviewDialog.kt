package com.bicy.whitenoise.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bicy.whitenoise.floatingpet.FloatingPetImporter
import com.bicy.whitenoise.ui.components.glass.GlassAlertDialogSimple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 悬浮萌宠导入预览弹窗
 *
 * - Tab 切换预览各动作（按 manifast.json 实际含有的动作显示）
 * - 实时播放帧动画（与悬浮窗显示一致）
 * - 底部显示元信息：title、帧尺寸、帧数+文件大小
 * - 预检 title 重复，重复时禁用确认
 */
@Composable
fun FloatingPetImportPreviewDialog(
    tempFile: File,
    isTitleDuplicated: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current

    // 解析 manifest
    val manifest = remember { FloatingPetImporter.getParsedManifest(tempFile) }

    // 可用动作 Tab（按 manifast.json 实际含有的动作）
    val availableActions = remember(manifest) {
        manifest?.animations?.keys?.sortedBy { actionOrder(it) } ?: emptyList()
    }

    var selectedAction by remember { mutableStateOf(availableActions.firstOrNull() ?: "idle") }

    // 帧动画状态
    var frames by remember(selectedAction) { mutableStateOf<List<Bitmap>?>(null) }
    var loadFailed by remember(selectedAction) { mutableStateOf(false) }
    var frameIndex by remember(selectedAction) { mutableIntStateOf(0) }
    var currentFrame by remember(selectedAction) { mutableStateOf<Bitmap?>(null) }

    // 加载选中动作的帧
    LaunchedEffect(selectedAction) {
        frames = null
        loadFailed = false
        frameIndex = 0
        currentFrame = null
        withContext(Dispatchers.IO) {
            val loaded = FloatingPetImporter.loadPreviewFrames(tempFile, selectedAction)
            if (loaded.isNullOrEmpty()) {
                loadFailed = true
            } else {
                frames = loaded
                frameIndex = 0
                currentFrame = loaded.firstOrNull()
            }
        }
    }

    // 帧动画播放循环
    LaunchedEffect(frames) {
        val frameList = frames
        if (frameList != null && frameList.isNotEmpty()) {
            val animConfig = manifest?.animations?.get(selectedAction)
            val speed = animConfig?.speed ?: 1.5f
            while (true) {
                currentFrame = frameList[frameIndex]
                frameIndex = (frameIndex + 1) % frameList.size
                // 帧间隔 = speed * 1000ms / frameCount
                val interval = (speed * 1000L / frameList.size).coerceAtLeast(50L)
                delay(interval)
            }
        }
    }

    if (manifest == null) {
        GlassAlertDialogSimple(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "manifast.json 解析失败",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
        return
    }

    GlassAlertDialogSimple(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                text = "预览：${manifest.title}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 动作 Tab 切换
            if (availableActions.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableActions) { action ->
                        val isSelected = action == selectedAction
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedAction = action },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = actionLabel(action),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 帧动画预览区
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (loadFailed) {
                    Text(
                        text = "加载失败",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                } else if (currentFrame != null) {
                    Image(
                        bitmap = currentFrame!!.asImageBitmap(),
                        contentDescription = "Preview $selectedAction",
                        modifier = Modifier.size(140.dp)
                    )
                } else {
                    Text(
                        text = "加载中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 元信息
            val sizeText = formatFileSize(manifest.zipSizeBytes)
            val frameCountText = "${manifest.totalFrameCount} 帧"
            val dimensionText = "${manifest.width} × ${manifest.height}"
            Text(
                text = "$dimensionText  |  $frameCountText  |  $sizeText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            // 重名警告
            if (isTitleDuplicated) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "已存在相同动画「${manifest.title}」，无法导入",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 底部按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onConfirm,
                    enabled = !isTitleDuplicated
                ) {
                    Text(
                        text = "确认导入",
                        color = if (isTitleDuplicated)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun actionOrder(action: String): Int = when (action) {
    "idle" -> 0
    "move" -> 1
    "hide_bottom" -> 2
    "hide_top" -> 3
    "hide_left" -> 4
    "hide_right" -> 5
    else -> 99
}

private fun actionLabel(action: String): String = when (action) {
    "idle" -> "待机"
    "move" -> "移动"
    "hide_bottom" -> "下藏"
    "hide_top" -> "上藏"
    "hide_left" -> "左藏"
    "hide_right" -> "右藏"
    else -> action
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
