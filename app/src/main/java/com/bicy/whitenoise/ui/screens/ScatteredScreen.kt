package com.bicy.whitenoise.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import com.bicy.whitenoise.ui.components.glass.GlassAlertDialogSimple
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.BottomNavTotalHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bicy.whitenoise.R
import com.bicy.whitenoise.ui.PageTopPadding
import com.bicy.whitenoise.StMb.ScatteredTrackDataPart.*
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.ScatteredAudioClipData
import com.bicy.whitenoise.subPage.scattered.model.ScatteredCategoryWithTypes
import com.bicy.whitenoise.subPage.scattered.model.ScatteredSoundTypeWithSounds
import com.bicy.whitenoise.subPage.scattered.model.ScatteredSoundWithType
import com.bicy.whitenoise.ui.PageBottomPadding
import com.bicy.whitenoise.ui.theme.ShadowConfig
import com.bicy.whitenoise.ui.theme.dropShadow
import com.bicy.whitenoise.ui.components.glass.GlassCategorySection
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import com.bicy.whitenoise.utils.DownloadManager
import com.bicy.whitenoise.utils.LanguageManager
import com.bicy.whitenoise.utils.ScatteredStorageManager

@Composable
fun ScatteredScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val categories = ScatteredStorageManager.getCategoriesWithTypes()
    
    // 大分类展开状态
    var expandedCategoryId by remember { mutableStateOf<String?>(null) }
    // 小分类展开状态（独立于大分类）
    var expandedTypeId by remember { mutableStateOf<String?>(null) }
    
    // 声音添加对话框状态
    var selectedSound by remember { mutableStateOf<ScatteredSoundWithType?>(null) }
    var showAddToTrackDialog by remember { mutableStateOf(false) }
    
    // 创建散点组对话框状态
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    
    // 下载状态
    val downloadProgress = remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    val downloadingSounds = remember { mutableStateOf<Set<String>>(emptySet()) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(PageTopPadding))

            // 标题区域
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
                
                IconButton(onClick = { showCreateGroupDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.create_player),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    expandedTypeId = expandedTypeId,
                    onCategoryClick = { categoryId ->
                        expandedCategoryId = if (expandedCategoryId == categoryId) null else categoryId
                    },
                    onTypeClick = { typeId ->
                        expandedTypeId = if (expandedTypeId == typeId) null else typeId
                    },
                    onSoundClick = { sound ->
                        handleSoundClick(
                            context = context,
                            sound = sound,
                            downloadingSounds = downloadingSounds,
                            downloadProgress = downloadProgress,
                            onShowDialog = {
                                selectedSound = sound
                                showAddToTrackDialog = true
                            }
                        )
                    },
                    downloadProgress = downloadProgress.value,
                    downloadingSounds = downloadingSounds.value
                )
            }
        }
    }
    
    // 添加到轨道对话框
    if (showAddToTrackDialog && selectedSound != null) {
        AddToTrackDialog(
            sound = selectedSound!!,
            scatteredTracks = viewModel.playingSounds.value.filter { 
                it.trackType == TrackType.SCATTERED 
            },
            onDismiss = {
                showAddToTrackDialog = false
                selectedSound = null
            },
            onAddToTrack = { trackId ->
                addSoundToTrack(selectedSound!!, trackId)
                showAddToTrackDialog = false
                selectedSound = null
            },
            onCreateNewTrack = { name ->
                viewModel.createEmptyScatteredGroup(name)
                // 创建后自动添加
                val newTrackId = "scattered_${System.currentTimeMillis()}"
                addSoundToTrack(selectedSound!!, newTrackId)
                showAddToTrackDialog = false
                selectedSound = null
            }
        )
    }
    
    // 创建散点组对话框
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

private fun handleSoundClick(
    context: android.content.Context,
    sound: ScatteredSoundWithType,
    downloadingSounds: androidx.compose.runtime.MutableState<Set<String>>,
    downloadProgress: androidx.compose.runtime.MutableState<Map<String, Float>>,
    onShowDialog: () -> Unit
) {
    val isDownloaded = ScatteredStorageManager.getSoundAudioFile(
        context, sound.categoryName, sound.typeName, sound.name
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
                
                if (success) onShowDialog()
            }
        )
    } else if (isDownloaded) {
        onShowDialog()
    }
}

private fun addSoundToTrack(sound: ScatteredSoundWithType, trackId: String) {
    val clip = ScatteredAudioClipData(
        id = sound.id,
        name = LanguageManager.translate(sound.name, sound.translations),
        filePath = sound.remoteUrl ?: "",
        durationMs = 0
    )
    com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage.addAudioClipToTrack(trackId, clip)
    
    val track = com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == trackId }
    if (track != null) {
        com.bicy.whitenoise.servies.MusicService.getInstance()?.updateScatteredTrackClips(trackId, track.audioClips)
    }
}

@Composable
private fun ScatteredCategoryList(
    categories: List<ScatteredCategoryWithTypes>,
    expandedCategoryId: String?,
    expandedTypeId: String?,
    onCategoryClick: (String) -> Unit,
    onTypeClick: (String) -> Unit,
    onSoundClick: (ScatteredSoundWithType) -> Unit,
    downloadProgress: Map<String, Float>,
    downloadingSounds: Set<String>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            bottom = PageBottomPadding
        )
    ) {
        items(categories, key = { it.categoryId }) { category ->
            val isCategoryExpanded = remember(expandedCategoryId, category.categoryId) {
                expandedCategoryId == category.categoryId
            }
            
            ScatteredCategoryItem(
                category = category,
                isCategoryExpanded = isCategoryExpanded,
                expandedTypeId = expandedTypeId,
                onCategoryClick = { onCategoryClick(category.categoryId) },
                onTypeClick = onTypeClick,
                onSoundClick = onSoundClick,
                downloadProgress = downloadProgress,
                downloadingSounds = downloadingSounds
            )
        }
    }
}

@Composable
private fun ScatteredCategoryItem(
    category: ScatteredCategoryWithTypes,
    isCategoryExpanded: Boolean,
    expandedTypeId: String?,
    onCategoryClick: () -> Unit,
    onTypeClick: (String) -> Unit,
    onSoundClick: (ScatteredSoundWithType) -> Unit,
    downloadProgress: Map<String, Float>,
    downloadingSounds: Set<String>
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isCategoryExpanded) 0f else -90f,
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
    ) {
        // 使用GlassCategorySection包装内容（液态玻璃效果）
        GlassCategorySection(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 大分类标题
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
                        contentDescription = if (isCategoryExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                        modifier = Modifier.rotate(rotationAngle),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                
                // 小分类列表
                AnimatedVisibility(
                    visible = isCategoryExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        category.soundTypes.forEach { type ->
                            val isTypeExpanded = remember(expandedTypeId, type.typeId) {
                                expandedTypeId == type.typeId
                            }
                            
                            ScatteredTypeItem(
                                type = type,
                                isExpanded = isTypeExpanded,
                                onClick = { onTypeClick(type.typeId) },
                                onSoundClick = onSoundClick,
                                downloadProgress = downloadProgress,
                                downloadingSounds = downloadingSounds
                            )
                            
                            if (type != category.soundTypes.last()) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScatteredTypeItem(
    type: ScatteredSoundTypeWithSounds,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onSoundClick: (ScatteredSoundWithType) -> Unit,
    downloadProgress: Map<String, Float>,
    downloadingSounds: Set<String>
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        label = "typeArrowRotation"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
    ) {
        // 小分类标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LanguageManager.translate(type.typeName, type.translations),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "(${type.sounds.size})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        
        // 声音列表（直接显示）
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                type.sounds.forEach { sound ->
                    SoundListItem(
                        sound = sound,
                        isDownloading = downloadingSounds.contains(sound.id),
                        downloadProgress = downloadProgress[sound.id] ?: 0f,
                        onClick = { onSoundClick(sound) }
                    )
                    
                    if (sound != type.sounds.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundListItem(
    sound: ScatteredSoundWithType,
    isDownloading: Boolean,
    downloadProgress: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    val isDownloaded = remember(sound.id) {
        ScatteredStorageManager.getSoundAudioFile(
            context, sound.categoryName, sound.typeName, sound.name
        ) != null
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 声音名称
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = LanguageManager.translate(sound.name, sound.translations),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (!sound.author.isNullOrEmpty()) {
                Text(
                    text = sound.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // 下载状态/添加按钮
        if (isDownloading) {
            CircularProgressIndicator(
                progress = { downloadProgress.coerceIn(0f, 1f) },
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        } else if (!isDownloaded) {
            Text(
                text = stringResource(R.string.click_to_download),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AddToTrackDialog(
    sound: ScatteredSoundWithType,
    scatteredTracks: List<com.bicy.whitenoise.ui.viewmodel.PlayingSound>,
    onDismiss: () -> Unit,
    onAddToTrack: (String) -> Unit,
    onCreateNewTrack: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newTrackName by remember { mutableStateOf("") }

    // 使用GlassAlertDialogSimple包装（液态玻璃效果）
    GlassAlertDialogSimple(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (showCreateDialog) {
                // 创建新track对话框
                Text(
                    text = stringResource(R.string.create_scattered_group),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newTrackName,
                    onValueChange = { newTrackName = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            if (newTrackName.isNotBlank()) {
                                onCreateNewTrack(newTrackName)
                                showCreateDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            } else {
                // 主对话框内容
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
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (scatteredTracks.isEmpty()) {
                    Column {
                        Text(
                            text = stringResource(R.string.no_scattered_tracks_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 快速创建按钮
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable { showCreateDialog = true }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.create_scattered_group),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.select_scattered_track_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 轨道列表
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(scatteredTracks) { track ->
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

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = LanguageManager.translate(track.name, track.translations),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = stringResource(R.string.added_audio_count, track.audioClipCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (track != scatteredTracks.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                        }

                        // 创建新组按钮
                        item {
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .clickable { showCreateDialog = true }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.create_scattered_group),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
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
                text = stringResource(R.string.create_scattered_group),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.enter_scattered_group_name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.name)) },
                    placeholder = { Text(stringResource(R.string.enter_group_name)) },
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
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}