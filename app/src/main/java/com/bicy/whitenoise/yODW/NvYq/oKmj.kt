package com.bicy.whitenoise.yODW.NvYq

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bicy.whitenoise.R
import com.bicy.whitenoise.JwJY.sBYh.kcFp.ScatteredAudioClipData
import com.bicy.whitenoise.DzBD.IrBh.XsdL.ScatteredCategoryWithTypes
import com.bicy.whitenoise.DzBD.IrBh.XsdL.ScatteredSoundTypeWithSounds
import com.bicy.whitenoise.DzBD.IrBh.XsdL.ScatteredSoundWithType
import com.bicy.whitenoise.yODW.ZFNn.ShadowConfig
import com.bicy.whitenoise.yODW.ZFNn.dropShadow
import com.bicy.whitenoise.yODW.nU5N.MainViewModel
import com.bicy.whitenoise.y10p.DownloadManager
import com.bicy.whitenoise.y10p.LanguageManager
import com.bicy.whitenoise.y10p.ScatteredStorageManager

private val ContentPaddingTop = 8.dp

@Composable
fun ScatteredScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val categories = ScatteredStorageManager.getCategoriesWithTypes()
    
    var expandedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<ScatteredSoundTypeWithSounds?>(null) }
    var showSoundListDialog by remember { mutableStateOf(false) }
    
    var selectedSound by remember { mutableStateOf<ScatteredSoundWithType?>(null) }
    var showSoundDetailDialog by remember { mutableStateOf(false) }
    
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    
    val downloadProgress = remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    val downloadingSounds = remember { mutableStateOf<Set<String>>(emptySet()) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                        text = stringResource(R.string.scattered_sound_library),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = stringResource(R.string.select_random_sound_group),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                
                Row {
                    IconButton(onClick = {
                        // TODO: 添加功能
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    IconButton(onClick = {
                        showCreateGroupDialog = true
                    }) {
                        Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.create_player),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.still_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_scattered_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                ScatteredCategoryList(
                    categories = categories,
                    expandedCategoryId = expandedCategoryId,
                    onCategoryClick = { categoryId ->
                        expandedCategoryId = if (expandedCategoryId == categoryId) {
                            null
                        } else {
                            categoryId
                        }
                    },
                    onTypeClick = { type ->
                        selectedType = type
                        showSoundListDialog = true
                    }
                )
            }
        }
    }
    
    if (showSoundListDialog && selectedType != null) {
        SoundListDialog(
            soundType = selectedType!!,
            downloadProgress = downloadProgress.value,
            downloadingSounds = downloadingSounds.value,
            onDismiss = {
                showSoundListDialog = false
                selectedType = null
            },
            onSoundClick = { sound ->
                val isDownloaded = ScatteredStorageManager.getSoundAudioFile(
                    context,
                    sound.categoryName,
                    sound.typeName,
                    sound.name
                ) != null
                
                if (!isDownloaded && !downloadingSounds.value.contains(sound.id)) {
                    val currentDownloading = downloadingSounds.value.toMutableSet()
                    currentDownloading.add(sound.id)
                    downloadingSounds.value = currentDownloading
                    
                    DownloadManager.downloadScatteredAudio(
                        context = context,
                        sound = sound,
                        onProgress = { progress ->
                            val currentProgress = downloadProgress.value.toMutableMap()
                            currentProgress[sound.id] = progress
                            downloadProgress.value = currentProgress
                        },
                        onComplete = { success ->
                            val currentDownloading = downloadingSounds.value.toMutableSet()
                            currentDownloading.remove(sound.id)
                            downloadingSounds.value = currentDownloading
                            
                            val currentProgress = downloadProgress.value.toMutableMap()
                            currentProgress.remove(sound.id)
                            downloadProgress.value = currentProgress
                            
                            if (success) {
                                selectedSound = sound
                                showSoundDetailDialog = true
                            }
                        }
                    )
                } else if (isDownloaded) {
                    selectedSound = sound
                    showSoundDetailDialog = true
                }
            }
        )
    }
    
    if (showSoundDetailDialog && selectedSound != null) {
        SoundDetailDialog(
            sound = selectedSound!!,
            scatteredTracks = viewModel.playingSounds.value.filter { it.trackType == com.bicy.whitenoise.StMb.TrackType.SCATTERED },
            onDismiss = {
                showSoundDetailDialog = false
                selectedSound = null
            },
            onAddToTrack = { trackId ->
                val sound = selectedSound!!
                val clip = ScatteredAudioClipData(
                    id = sound.id,
                    name = com.bicy.whitenoise.y10p.LanguageManager.translate(sound.name, sound.translations),
                    filePath = sound.remoteUrl ?: "",
                    durationMs = 0
                )
                com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage.addAudioClipToTrack(trackId, clip)
                
                val track = com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == trackId }
                if (track != null) {
                    com.bicy.whitenoise.rgRE.MusicService.getInstance()?.updateScatteredTrackClips(trackId, track.audioClips)
                }
                
                showSoundDetailDialog = false
                selectedSound = null
            }
        )
    }
    
    if (showCreateGroupDialog) {
        CreateScatteredGroupDialog(
            name = newGroupName,
            onNameChange = { newGroupName = it },
            onDismiss = {
                showCreateGroupDialog = false
                newGroupName = ""
            },
            onConfirm = {
                if (newGroupName.isNotBlank()) {
                    viewModel.createEmptyScatteredGroup(newGroupName)
                    showCreateGroupDialog = false
                    newGroupName = ""
                }
            }
        )
    }
}

@Composable
private fun ScatteredCategoryList(
    categories: List<ScatteredCategoryWithTypes>,
    expandedCategoryId: String?,
    onCategoryClick: (String) -> Unit,
    onTypeClick: (ScatteredSoundTypeWithSounds) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = categories,
            key = { it.categoryId }
        ) { category ->
            val isExpanded = remember(expandedCategoryId, category.categoryId) {
                expandedCategoryId == category.categoryId
            }
            
            ScatteredCategoryItem(
                category = category,
                isExpanded = isExpanded,
                onCategoryClick = { onCategoryClick(category.categoryId) },
                onTypeClick = onTypeClick
            )
        }
    }
}

@Composable
private fun ScatteredCategoryItem(
    category: ScatteredCategoryWithTypes,
    isExpanded: Boolean,
    onCategoryClick: () -> Unit,
    onTypeClick: (ScatteredSoundTypeWithSounds) -> Unit
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
                text = LanguageManager.translate(category.categoryName, category.translations),
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
            TypeList(
                types = category.soundTypes,
                onTypeClick = onTypeClick
            )
        }
    }
}

@Composable
private fun TypeList(
    types: List<ScatteredSoundTypeWithSounds>,
    onTypeClick: (ScatteredSoundTypeWithSounds) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(
            items = types,
            key = { it.typeId }
        ) { type ->
            TypeChip(
                type = type,
                onClick = { onTypeClick(type) }
            )
        }
    }
}

@Composable
private fun TypeChip(
    type: ScatteredSoundTypeWithSounds,
    onClick: () -> Unit
) {
    val chipSize = 72.dp
    val cornerRadius = 12.dp
    
    Box(
        modifier = Modifier
            .size(chipSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = LanguageManager.translate(type.typeName, type.translations),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun SoundListDialog(
    soundType: ScatteredSoundTypeWithSounds,
    downloadProgress: Map<String, Float>,
    downloadingSounds: Set<String>,
    onDismiss: () -> Unit,
    onSoundClick: (ScatteredSoundWithType) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageManager.translate(soundType.typeName, soundType.translations),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                ) {
                    items(soundType.sounds, key = { it.id }) { sound ->
                        SoundItem(
                            sound = sound,
                            isDownloading = downloadingSounds.contains(sound.id),
                            downloadProgress = downloadProgress[sound.id] ?: 0f,
                            onClick = { onSoundClick(sound) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundItem(
    sound: ScatteredSoundWithType,
    isDownloading: Boolean,
    downloadProgress: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    val isDownloaded = remember(sound.id) {
        ScatteredStorageManager.getSoundAudioFile(
            context,
            sound.categoryName,
            sound.typeName,
            sound.name
        ) != null
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = LanguageManager.translate(sound.name, sound.translations),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = sound.author ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (isDownloading) {
                if (downloadProgress in 0.01f..0.99f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(downloadProgress)
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                }
                
                CircularProgressIndicator(
                    progress = { downloadProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterEnd),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else if (!isDownloaded) {
                Text(
                    text = "点击下载",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Composable
private fun SoundDetailDialog(
    sound: ScatteredSoundWithType,
    scatteredTracks: List<com.bicy.whitenoise.yODW.nU5N.PlayingSound>,
    onDismiss: () -> Unit,
    onAddToTrack: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageManager.translate(sound.name, sound.translations),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (scatteredTracks.isEmpty()) {
                    Text(
                        text = "暂无散点播放音轨\n请先在播放页创建散点音轨",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        text = "选择要添加到的散点音轨：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    scatteredTracks.forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddToTrack(track.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = LanguageManager.translate(track.name, track.translations),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Text(
                                    text = "已添加 ${track.audioClipCount} 个音频",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        
                        if (track != scatteredTracks.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateScatteredGroupDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "创建散点播放组",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "请输入散点播放组名称",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("名称") },
                    placeholder = { Text("输入播放组名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = name.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
