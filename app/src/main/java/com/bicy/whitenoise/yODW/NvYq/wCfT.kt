package com.bicy.whitenoise.yODW.NvYq

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.bicy.whitenoise.yODW.SrEO.InteractiveSlider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bicy.whitenoise.R
import com.bicy.whitenoise.StMb.TrackType
import com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialScatterRangeData
import com.bicy.whitenoise.yODW.ZFNn.ShadowConfig
import com.bicy.whitenoise.yODW.ZFNn.dropShadow
import com.bicy.whitenoise.yODW.nU5N.MainViewModel
import com.bicy.whitenoise.yODW.nU5N.PlayingSound
import com.bicy.whitenoise.y10p.LanguageManager

private val ContentPaddingTop = 8.dp

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
    
    var configDialogState by remember { mutableStateOf<ConfigDialogState?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = ContentPaddingTop)
        ) {
            Text(
                text = stringResource(R.string.now_playing),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = stringResource(R.string.adjust_volume_effects),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (playingSounds.isEmpty()) {
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playingSounds, key = { it.id }) { sound ->
                        PlayingSoundItem(
                            sound = sound,
                            onVolumeChange = { volume -> viewModel.setVolume(sound.id, volume) },
                            onConfigClick = {
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
                            },
                            onRemove = { viewModel.removePlayingSound(sound.id) }
                        )
                    }
                }
            }
            
            if (playingSounds.isNotEmpty()) {
                PlayPauseButton(
                    isPaused = isPaused,
                    onClick = { viewModel.togglePauseResume() }
                )
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
            val trackConfig = com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == state.soundId }
            val currentSpatialRange = com.bicy.whitenoise.StMb.SpatialScatterRange(
                xMin = trackConfig?.spatialScatterRange?.xMin ?: -5f,
                xMax = trackConfig?.spatialScatterRange?.xMax ?: 5f,
                yMin = trackConfig?.spatialScatterRange?.yMin ?: 0f,
                yMax = trackConfig?.spatialScatterRange?.yMax ?: 3f,
                zMin = trackConfig?.spatialScatterRange?.zMin ?: -5f,
                zMax = trackConfig?.spatialScatterRange?.zMax ?: 5f
            )
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
                        xMin = spatialRange.xMin,
                        xMax = spatialRange.xMax,
                        yMin = spatialRange.yMin,
                        yMax = spatialRange.yMax,
                        zMin = spatialRange.zMin,
                        zMax = spatialRange.zMax
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
                    com.bicy.whitenoise.H3HO.ReverbManager.setConfig(state.soundId, reverbConfig)
                    com.bicy.whitenoise.H3HO.CreativeEffectManager.setConfig(state.soundId, creativeConfig)
                    
                    val currentClipId = com.bicy.whitenoise.H3HO.ScatteredPlayerManager.getTrackState(state.soundId)?.currentClipId
                    if (currentClipId != null) {
                        com.bicy.whitenoise.H3HO.OboeAudioEngine.setReverbParams(currentClipId, reverbConfig.roomSize, reverbConfig.damping, reverbConfig.wetLevel)
                        com.bicy.whitenoise.H3HO.OboeAudioEngine.setInsulation(currentClipId, reverbConfig.insulation)
                        com.bicy.whitenoise.H3HO.OboeAudioEngine.setReverbDecayTime(currentClipId, reverbConfig.decayTime)
                        com.bicy.whitenoise.H3HO.OboeAudioEngine.setReverbPreDelay(currentClipId, reverbConfig.preDelay)
                        com.bicy.whitenoise.H3HO.OboeAudioEngine.setReverbDryLevel(currentClipId, reverbConfig.dryLevel)
                    }
                    
                    com.bicy.whitenoise.H3HO.ScatteredPlayerManager.updateTrackConfig(
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
}

@Composable
private fun PlayingSoundItem(
    sound: PlayingSound,
    onVolumeChange: (Float) -> Unit,
    onConfigClick: () -> Unit,
    onRemove: () -> Unit
) {
    var localVolume by remember(sound.id) { mutableStateOf(sound.volume) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .dropShadow(
                config = ShadowConfig.Light,
                shape = RoundedCornerShape(12.dp),
                clip = false
            )
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
