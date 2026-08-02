package com.bicy.whitenoise.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.bicy.whitenoise.ui.components.InteractiveSlider
import com.bicy.whitenoise.ui.components.glass.GlassAlertDialogSimple
import com.bicy.whitenoise.ui.components.glass.GlassCard
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.BottomNavTotalHeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bicy.whitenoise.R
import com.bicy.whitenoise.ui.PageTopPadding
import com.bicy.whitenoise.StMb.ScatteredTrackDataPart.*
import com.bicy.whitenoise.storage.whitenoise.PresetManager
import com.bicy.whitenoise.storage.whitenoise.WhiteNoisePreset
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SpatialScatterRangeData
import com.bicy.whitenoise.ui.PageBottomPadding
import com.bicy.whitenoise.ui.theme.ShadowConfig
import com.bicy.whitenoise.ui.theme.dropShadow
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import com.bicy.whitenoise.ui.viewmodel.PlayingSound
import com.bicy.whitenoise.utils.LanguageManager
import com.bicy.whitenoise.ui.tutorial.TutorialManager
import com.bicy.whitenoise.ui.tutorial.tutorialTarget

sealed class ConfigDialogState {
    data class Reverb(val soundId: String, val soundName: String) : ConfigDialogState()
    data class Scattered(
        val soundId: String,
        val soundName: String,
        val audioClipCount: Int
    ) : ConfigDialogState()
}

@Composable
fun PlayScreen(
    viewModel: MainViewModel = viewModel()
) {
    val playingSounds by viewModel.playingSounds.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val showTutorialSample by TutorialManager.showTutorialSample.collectAsState()
    val tutorialActive by TutorialManager.currentTutorial.collectAsState()

    // 教程示例声音（视觉展示，无实际音频）
    val tutorialSampleName = stringResource(R.string.white_noise)
    val displaySounds = if (showTutorialSample) {
        listOf(PlayingSound(id = "tutorial_sample", name = tutorialSampleName)) + playingSounds
    } else playingSounds
    
    var configDialogState by remember { mutableStateOf<ConfigDialogState?>(null) }
    var showPresetsDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showDeletePresetConfirm by remember { mutableStateOf(false) }
    var showLoadPresetConfirm by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf<WhiteNoisePreset?>(null) }
    var presets by remember { mutableStateOf<List<WhiteNoisePreset>>(emptyList()) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(PageTopPadding))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = stringResource(R.string.adjust_volume_effects),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Row(
                    modifier = Modifier.tutorialTarget(
                        TutorialManager.KEY_SAVE_LOAD,
                        enabled = tutorialActive != null
                    )
                ) {
                    IconButton(onClick = { showPresetsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.presets),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { showSavePresetDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.save_preset),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (displaySounds.isEmpty()) {
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
                            text = stringResource(R.string.select_from_library),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        bottom = PageBottomPadding
                    )
                ) {
                    items(displaySounds, key = { it.id }) { sound ->
                        val isTutorialSample = sound.id == "tutorial_sample"
                        PlayingSoundItem(
                            sound = sound,
                            onVolumeChange = { volume ->
                                if (!isTutorialSample) viewModel.setVolume(sound.id, volume)
                            },
                            onConfigClick = {
                                if (!isTutorialSample) {
                                    configDialogState = when (sound.trackType) {
                                        TrackType.SCATTERED -> ConfigDialogState.Scattered(
                                            soundId = sound.id,
                                            soundName = sound.name,
                                            audioClipCount = sound.audioClipCount
                                        )
                                        TrackType.LOOP -> ConfigDialogState.Reverb(
                                            soundId = sound.id,
                                            soundName = sound.name
                                        )
                                    }
                                }
                            },
                            onRemove = {
                                if (!isTutorialSample) viewModel.removePlayingSound(sound.id)
                            },
                            modifier = if (isTutorialSample) Modifier.tutorialTarget(
                                TutorialManager.KEY_SAMPLE_SOUND,
                                enabled = tutorialActive != null
                            ) else Modifier
                        )
                    }
                }
            }
        }
    }

    when (val state = configDialogState) {
        is ConfigDialogState.Reverb -> {
            ReverbConfigDialog(
                soundId = state.soundId,
                soundName = state.soundName,
                onDismiss = { configDialogState = null },
                onApply = { config ->
                    viewModel.setReverbConfig(state.soundId, config)
                    configDialogState = null
                }
            )
        }
        is ConfigDialogState.Scattered -> {
            val trackConfig = WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == state.soundId }
            val data = trackConfig?.spatialScatterRange
            val currentSpatialRange = if (data != null) {
                SpatialScatterRange(
                    minRadius = data.minRadius,
                    maxRadius = data.maxRadius,
                    xEnabled = data.xEnabled,
                    yEnabled = data.yEnabled,
                    zEnabled = data.zEnabled,
                    moveEnabled = data.moveEnabled,
                    moveRandomValue = data.moveRandomValue,
                    moveSpeed = data.moveSpeed,
                    directionRandomValue = data.directionRandomValue
                )
            } else SpatialScatterRange()
            val currentMinInterval = trackConfig?.minIntervalMs ?: 3000
            val currentMaxInterval = trackConfig?.maxIntervalMs ?: 10000
            val currentSpatialScatterEnabled = trackConfig?.spatialScatterEnabled ?: false
            val currentOverlayMode = trackConfig?.overlayMode ?: false
            
            ScatteredConfigDialog(
                trackId = state.soundId,
                trackName = state.soundName,
                audioClipCount = state.audioClipCount,
                audioClips = emptyList(),
                currentRange = currentSpatialRange,
                currentMinInterval = currentMinInterval,
                currentMaxInterval = currentMaxInterval,
                currentSpatialScatterEnabled = currentSpatialScatterEnabled,
                currentOverlayMode = currentOverlayMode,
                onDismiss = { configDialogState = null },
                onApply = { spatialRange, minInterval, maxInterval, spatialScatterEnabled, overlayMode, reverbConfig, creativeConfig ->
                    val spatialRangeData = SpatialScatterRangeData(
                        minRadius = spatialRange.minRadius,
                        maxRadius = spatialRange.maxRadius,
                        xEnabled = spatialRange.xEnabled,
                        yEnabled = spatialRange.yEnabled,
                        zEnabled = spatialRange.zEnabled,
                        moveEnabled = spatialRange.moveEnabled,
                        moveRandomValue = spatialRange.moveRandomValue,
                        moveSpeed = spatialRange.moveSpeed,
                        directionRandomValue = spatialRange.directionRandomValue
                    )
                    WhiteNoiseStorage.updateScatteredTrackConfig(
                        trackId = state.soundId,
                        minIntervalMs = minInterval,
                        maxIntervalMs = maxInterval,
                        spatialScatterRange = spatialRangeData,
                        spatialScatterEnabled = spatialScatterEnabled,
                        overlayMode = overlayMode
                    )
                    
                    WhiteNoiseStorage.updatePlayingSoundReverb(state.soundId, reverbConfig)
                    WhiteNoiseStorage.updatePlayingSoundCreative(state.soundId, creativeConfig)
                    com.bicy.whitenoise.audio.ReverbManager.setConfig(state.soundId, reverbConfig)
                    com.bicy.whitenoise.audio.CreativeEffectManager.setConfig(state.soundId, creativeConfig)
                    
                    val currentClipId = com.bicy.whitenoise.audio.ScatteredPlayerManagerPart.ScatteredPlayerManager.getTrackState(state.soundId)?.currentClipId
                    if (currentClipId != null) {
                        com.bicy.whitenoise.audio.OboeAudioEngine.setReverbParams(currentClipId, reverbConfig.roomSize, reverbConfig.damping, reverbConfig.wetLevel)
                        com.bicy.whitenoise.audio.OboeAudioEngine.setInsulation(currentClipId, reverbConfig.insulation)
                        com.bicy.whitenoise.audio.OboeAudioEngine.setReverbDecayTime(currentClipId, reverbConfig.decayTime)
                        com.bicy.whitenoise.audio.OboeAudioEngine.setReverbPreDelay(currentClipId, reverbConfig.preDelay)
                        com.bicy.whitenoise.audio.OboeAudioEngine.setReverbDryLevel(currentClipId, reverbConfig.dryLevel)
                    }
                    
                    com.bicy.whitenoise.audio.ScatteredPlayerManagerPart.ScatteredPlayerManager.updateTrackConfig(
                        trackId = state.soundId,
                        minIntervalMs = minInterval,
                        maxIntervalMs = maxInterval,
                        spatialRange = spatialRangeData,
                        spatialEnabled = spatialScatterEnabled,
                        overlayMode = overlayMode
                    )
                    
                    configDialogState = null
                }
            )
        }
        null -> {}
    }

    // ── 保存预设命名窗口 ──
    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf("") }
        GlassAlertDialogSimple(onDismissRequest = { showSavePresetDialog = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.save_preset),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text(stringResource(R.string.preset_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { showSavePresetDialog = false },
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            if (presetName.isNotBlank()) {
                                val configs = viewModel.getCurrentSoundConfigs()
                                PresetManager.save(presetName.trim(), configs)
                                presets = PresetManager.presets.value
                                showSavePresetDialog = false
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd),
                        enabled = presetName.isNotBlank()
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }

    // ── 预设列表弹窗 ──
    if (showPresetsDialog) {
        LaunchedEffect(Unit) { presets = PresetManager.presets.value }
        GlassAlertDialogSimple(onDismissRequest = { showPresetsDialog = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.presets),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (presets.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_presets),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp)
                    ) {
                        items(presets, key = { it.id }) { preset ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPreset = preset
                                        showLoadPresetConfirm = true
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "${preset.sounds.size} ${stringResource(R.string.sounds)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                                IconButton(onClick = {
                                    selectedPreset = preset
                                    showDeletePresetConfirm = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { showPresetsDialog = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }

    // ── 删除预设确认 ──
    if (showDeletePresetConfirm && selectedPreset != null) {
        GlassAlertDialogSimple(onDismissRequest = { showDeletePresetConfirm = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.delete_preset_confirm),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.delete_preset_hint, selectedPreset!!.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { showDeletePresetConfirm = false },
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            PresetManager.delete(selectedPreset!!.id)
                            presets = PresetManager.presets.value
                            showDeletePresetConfirm = false
                        },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            stringResource(R.string.confirm),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // ── 加载预设确认 ──
    if (showLoadPresetConfirm && selectedPreset != null) {
        GlassAlertDialogSimple(onDismissRequest = { showLoadPresetConfirm = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.load_preset_confirm),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.load_preset_hint, selectedPreset!!.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { showLoadPresetConfirm = false },
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            val wasPlaying = viewModel.playingSounds.value.isNotEmpty()
                            viewModel.loadPresetSounds(selectedPreset!!.sounds) {
                                showLoadPresetConfirm = false
                                showPresetsDialog = false
                                if (wasPlaying) {
                                    viewModel.startMusicService()
                                    viewModel.restorePlaybackAfterLoad()
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayingSoundItem(
    sound: PlayingSound,
    onVolumeChange: (Float) -> Unit,
    onConfigClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var localVolume by remember(sound.id) { mutableStateOf(sound.volume) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .dropShadow(
                config = ShadowConfig.Light,
                shape = RoundedCornerShape(12.dp),
                clip = false
            )
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconRes = when (sound.trackType) {
                    TrackType.LOOP -> R.drawable.ic_play
                    TrackType.SCATTERED -> R.drawable.ic_scattered
                }
                
                Icon(
                    imageVector = ImageVector.vectorResource(id = iconRes),
                    contentDescription = if (sound.trackType == TrackType.LOOP) stringResource(R.string.white_noise) else stringResource(R.string.scattered),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column(modifier = Modifier.weight(0.3f)) {
                    Text(
                        text = LanguageManager.translate(sound.name, sound.translations),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (sound.trackType == TrackType.SCATTERED && sound.audioClipCount > 0) {
                        Text(
                            text = "${stringResource(R.string.scattered)} · ${sound.audioClipCount}${stringResource(R.string.audio_clips_count)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                InteractiveSlider(
                    value = localVolume,
                    onValueChange = { newVolume ->
                        localVolume = newVolume
                        onVolumeChange(newVolume)
                    },
                    modifier = Modifier.weight(0.5f),
                    valueRange = 0f..3f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "${(localVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.width(40.dp)
                )
                
                IconButton(
                    onClick = onConfigClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.config),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayPauseButton(
    isPaused: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) stringResource(R.string.play) else stringResource(R.string.pause),
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
