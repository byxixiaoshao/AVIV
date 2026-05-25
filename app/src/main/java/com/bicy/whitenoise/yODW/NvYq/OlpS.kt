package com.bicy.whitenoise.yODW.NvYq

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bicy.whitenoise.R
import com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundMetadata
import com.bicy.whitenoise.yODW.ZFNn.ShadowConfig
import com.bicy.whitenoise.yODW.ZFNn.dropShadow
import com.bicy.whitenoise.yODW.nU5N.CategoryWithSounds
import com.bicy.whitenoise.yODW.nU5N.MainViewModel
import com.bicy.whitenoise.y10p.LanguageManager
import com.bicy.whitenoise.y10p.SoundStorageManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ContentPaddingTop = 8.dp

@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playingStates by viewModel.playingStates.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    
    var expandedCategoryId by remember { mutableStateOf<String?>(null) }
    var showAddSoundDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = ContentPaddingTop)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.sound_library),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = stringResource(R.string.select_white_noise),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                
                IconButton(onClick = { showAddSoundDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                SoundCategoryList(
                    categories = categories,
                    viewModel = viewModel,
                    expandedCategoryId = expandedCategoryId,
                    playingStates = playingStates,
                    downloadProgress = downloadProgress,
                    onCategoryClick = { categoryId ->
                        viewModel.toggleCategory(categoryId)
                        expandedCategoryId = if (expandedCategoryId == categoryId) {
                            null
                        } else {
                            categoryId
                        }
                    },
                    onSoundClick = { sound -> viewModel.onSoundClick(sound) }
                )
            }
        }
    }
    
    if (showAddSoundDialog) {
        AddSoundDialog(
            viewModel = viewModel,
            onDismiss = { showAddSoundDialog = false },
            onCategoryAdded = { },
            onSoundAdded = { }
        )
    }
}

@Composable
private fun SoundCategoryList(
    categories: List<CategoryWithSounds>,
    viewModel: MainViewModel,
    expandedCategoryId: String?,
    playingStates: Set<String>,
    downloadProgress: Map<String, Float>,
    onCategoryClick: (String) -> Unit,
    onSoundClick: (SoundMetadata) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = categories,
            key = { it.category.id }
        ) { categoryWithSounds ->
            val isExpanded = remember(expandedCategoryId, categoryWithSounds.category.id) {
                expandedCategoryId == categoryWithSounds.category.id
            }
            
            CategoryItem(
                categoryWithSounds = categoryWithSounds,
                viewModel = viewModel,
                isExpanded = isExpanded,
                playingStates = playingStates,
                downloadProgress = downloadProgress,
                onCategoryClick = { onCategoryClick(categoryWithSounds.category.id) },
                onSoundClick = onSoundClick
            )
        }
    }
}

@Composable
private fun CategoryItem(
    categoryWithSounds: CategoryWithSounds,
    viewModel: MainViewModel,
    isExpanded: Boolean,
    playingStates: Set<String>,
    downloadProgress: Map<String, Float>,
    onCategoryClick: () -> Unit,
    onSoundClick: (SoundMetadata) -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        label = "arrowRotation"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .dropShadow(
                config = ShadowConfig.Light,
                shape = RoundedCornerShape(12.dp),
                clip = false
            )
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCategoryClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LanguageManager.translate(categoryWithSounds.category.name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                modifier = Modifier.rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SoundList(
                sounds = categoryWithSounds.sounds,
                categoryName = categoryWithSounds.category.name,
                viewModel = viewModel,
                playingStates = playingStates,
                downloadProgress = downloadProgress,
                onSoundClick = onSoundClick
            )
        }
    }
}

@Composable
private fun SoundList(
    sounds: List<SoundMetadata>,
    categoryName: String,
    viewModel: MainViewModel,
    playingStates: Set<String>,
    downloadProgress: Map<String, Float>,
    onSoundClick: (SoundMetadata) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(
            items = sounds,
            key = { it.id }
        ) { sound ->
            SoundChip(
                sound = sound,
                categoryName = categoryName,
                viewModel = viewModel,
                isPlaying = playingStates.contains(sound.id),
                downloadProgress = downloadProgress[sound.id] ?: 0f,
                onClick = { onSoundClick(sound) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SoundChip(
    sound: SoundMetadata,
    categoryName: String,
    viewModel: MainViewModel,
    isPlaying: Boolean,
    downloadProgress: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var showDetailDialog by remember { mutableStateOf(false) }
    
    val chipSize = 72.dp
    val cornerRadius = 12.dp
    
    Box(
        modifier = Modifier
            .size(chipSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = { showDetailDialog = true }
            )
    ) {
        if (downloadProgress in 0.01f..0.99f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(downloadProgress)
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
        }
        
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            )
        }
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = LanguageManager.translate(sound.name, sound.translations),
                style = MaterialTheme.typography.labelMedium,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
    
    if (showDetailDialog) {
        SoundDetailDialog(
            sound = sound,
            categoryName = categoryName,
            viewModel = viewModel,
            onDismiss = { showDetailDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
private fun SoundDetailDialog(
    sound: SoundMetadata,
    categoryName: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val soundType = remember(sound.id) {
        try {
            val categoryDir = File(context.filesDir, "white_noise/library/$categoryName")
            val soundDir = File(categoryDir, sound.name)
            val typeFile = File(soundDir, "metadata.json")
            if (typeFile.exists()) {
                val json = typeFile.readText()
                com.google.gson.Gson().fromJson(json, SoundStorageManager.SoundType::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    val isDownloaded = remember(sound.id) {
        com.bicy.whitenoise.y10p.DownloadManager.isCached(context, sound.id)
    }
    
    val downloadInfo = remember(sound.id) {
        val cachedFile = com.bicy.whitenoise.y10p.DownloadManager.getCachedFile(context, sound.id)
        if (cachedFile != null && cachedFile.exists()) {
            val fileSize = cachedFile.length()
            val lastModified = cachedFile.lastModified()
            
            DownloadInfo(
                fileSize = fileSize,
                downloadDate = lastModified,
                duration = soundType?.duration ?: 0
            )
        } else null
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_confirm)) },
            text = { Text(stringResource(R.string.delete_sound_hint)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSound(categoryName, sound.name, sound.id)
                        showDeleteConfirm = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = LanguageManager.translate(sound.name, sound.translations),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DetailRow(stringResource(R.string.download_address), sound.remoteUrl) {
                if (sound.remoteUrl.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sound.remoteUrl))
                    context.startActivity(intent)
                }
            }
            
            val displayAuthor = soundType?.author ?: sound.author
            val displayAuthorUrl = soundType?.authorUrl ?: sound.authorUrl
            
            if (displayAuthor.isNotEmpty()) {
                DetailRow(stringResource(R.string.author), displayAuthor, clickable = false)
            }
            
            if (displayAuthorUrl.isNotEmpty()) {
                DetailRow(stringResource(R.string.author_homepage), displayAuthorUrl) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(displayAuthorUrl))
                    context.startActivity(intent)
                }
            }
            
            if (downloadInfo != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.download_info),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(downloadInfo.downloadDate))
                val formattedSize = formatFileSize(downloadInfo.fileSize)
                
                Text(
                    text = "${stringResource(R.string.download_date)}: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Text(
                    text = "${stringResource(R.string.file_size)}: $formattedSize",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                downloadInfo.duration?.let { duration ->
                    val formattedDuration = formatDuration(duration)
                    Text(
                        text = "${stringResource(R.string.audio_duration)}: $formattedDuration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.donate_author_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showDeleteConfirm = true }
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    clickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = value.take(30) + if (value.length > 30) "..." else "",
            style = MaterialTheme.typography.bodyMedium,
            color = if (clickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (clickable) Modifier.clickable { onClick() } else Modifier
        )
    }
}

private data class DownloadInfo(
    val fileSize: Long,
    val downloadDate: Long,
    val duration: Long?
)

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024))
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}
