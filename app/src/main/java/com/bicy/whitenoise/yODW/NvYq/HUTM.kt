package com.bicy.whitenoise.yODW.NvYq

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.bicy.whitenoise.yODW.SrEO.InteractiveSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.bicy.whitenoise.R
import com.bicy.whitenoise.H3HO.OboeAudioEngine
import com.bicy.whitenoise.H3HO.ReverbConfig
import com.bicy.whitenoise.H3HO.ReverbManager
import com.bicy.whitenoise.H3HO.CreativeEffectManager
import com.bicy.whitenoise.H3HO.CreativeEffectType
import com.bicy.whitenoise.StMb.ScatteredAudioClip
import com.bicy.whitenoise.StMb.SpatialScatterRange
import com.bicy.whitenoise.JwJY.NATg.ConfigStorage
import com.bicy.whitenoise.JwJY.sBYh.kcFp.CreativeEffectConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialAudioConfig
import com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage
import com.bicy.whitenoise.yODW.NvYq.EF5M.CollapsibleSection
import com.bicy.whitenoise.yODW.NvYq.EF5M.EffectSliderItem
import com.bicy.whitenoise.yODW.NvYq.EF5M.PresetChip
import com.bicy.whitenoise.yODW.NvYq.EF5M.ReverbSlider
import com.bicy.whitenoise.yODW.NvYq.EF5M.reverbPresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScatteredConfigDialog(
    trackId: String,
    trackName: String,
    audioClipCount: Int = 0,
    audioClips: List<ScatteredAudioClip> = emptyList(),
    currentRange: SpatialScatterRange = SpatialScatterRange(),
    currentMinInterval: Long = 3000,
    currentMaxInterval: Long = 10000,
    currentSpatialScatterEnabled: Boolean = false,
    currentOverlayMode: Boolean = false,
    onDismiss: () -> Unit,
    onApply: (
        spatialRange: SpatialScatterRange, 
        minInterval: Long, 
        maxInterval: Long,
        spatialScatterEnabled: Boolean,
        overlayMode: Boolean,
        reverbConfig: ReverbConfig,
        creativeConfig: CreativeEffectConfig
    ) -> Unit,
    onAddAudioClip: () -> Unit = {},
    onRemoveAudioClip: (String) -> Unit = {}
) {
    val globalState by ConfigStorage.config.collectAsState()
    val isPremiumUser = globalState.isPremium
    
    var spatialRange by remember { mutableStateOf(currentRange) }
    var minInterval by remember { mutableStateOf(currentMinInterval.toFloat()) }
    var maxInterval by remember { mutableStateOf(currentMaxInterval.toFloat()) }
    var spatialScatterEnabled by remember { mutableStateOf(currentSpatialScatterEnabled) }
    var overlayMode by remember { mutableStateOf(currentOverlayMode) }
    
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val audioGroupExpanded = expandedSection == "audioGroup"
    val spatialReverbExpanded = expandedSection == "spatialReverb"
    val creativeExpanded = expandedSection == "creative"
    val spatialScatterExpanded = expandedSection == "spatialScatter"
    
    var selectedPreset by remember { mutableStateOf("") }
    
    val savedConfig = remember { 
        WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == trackId }?.reverbConfig
            ?: ReverbManager.getConfig(trackId) 
            ?: ReverbConfig() 
    }
    val originalConfig = remember { savedConfig.copy() }
    
    var roomSize by remember { mutableStateOf(savedConfig.roomSize) }
    var decayTime by remember { mutableStateOf(savedConfig.decayTime) }
    var damping by remember { mutableStateOf(savedConfig.damping) }
    var wetLevel by remember { mutableStateOf(savedConfig.wetLevel) }
    var dryLevel by remember { mutableStateOf(savedConfig.dryLevel) }
    var preDelay by remember { mutableStateOf(savedConfig.preDelay) }
    var insulation by remember { mutableStateOf(savedConfig.insulation) }
    var reflectionDensity by remember { mutableStateOf(savedConfig.reflectionDensity) }
    var reflectionSpread by remember { mutableStateOf(savedConfig.reflectionSpread) }
    var highpassCutoff by remember { mutableStateOf(savedConfig.highpassCutoff) }
    var earlyReflectionLevel by remember { mutableStateOf(savedConfig.earlyReflectionLevel) }
    
    val savedCreativeConfig = remember { WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == trackId }?.creativeEffectConfig ?: CreativeEffectConfig() }
    var loFiIntensity by remember { mutableFloatStateOf(savedCreativeConfig.loFi) }
    var eightBitIntensity by remember { mutableFloatStateOf(savedCreativeConfig.eightBit) }
    var underwaterIntensity by remember { mutableFloatStateOf(savedCreativeConfig.underwater) }
    var alienSignalIntensity by remember { mutableFloatStateOf(savedCreativeConfig.alienSignal) }
    var megaphoneIntensity by remember { mutableFloatStateOf(savedCreativeConfig.megaphone) }
    var hifiIntensity by remember { mutableFloatStateOf(savedCreativeConfig.hifi) }
    
    val saveCreativeConfig: () -> Unit = {
        val config = CreativeEffectConfig(
            loFi = loFiIntensity,
            eightBit = eightBitIntensity,
            underwater = underwaterIntensity,
            alienSignal = alienSignalIntensity,
            megaphone = megaphoneIntensity,
            hifi = hifiIntensity
        )
        WhiteNoiseStorage.updatePlayingSoundCreative(trackId, config)
        com.bicy.whitenoise.H3HO.CreativeEffectManager.setConfig(trackId, config)
    }
    
    val applyCreativeEffect: (Int, Float) -> Unit = { effectType, intensity ->
        saveCreativeConfig()
        val currentClipId = com.bicy.whitenoise.H3HO.ScatteredPlayerManager.getTrackState(trackId)?.currentClipId
        if (currentClipId != null) {
            OboeAudioEngine.setCreativeEffectIntensity(currentClipId, effectType, intensity)
        }
    }
    
    val applyPreview: () -> Unit = {
        val config = ReverbConfig(
            enabled = true,
            preset = selectedPreset,
            roomSize = roomSize,
            decayTime = decayTime,
            damping = damping,
            wetLevel = wetLevel,
            dryLevel = dryLevel,
            preDelay = preDelay,
            insulation = insulation,
            reflectionDensity = reflectionDensity,
            reflectionSpread = reflectionSpread,
            highpassCutoff = highpassCutoff,
            earlyReflectionLevel = earlyReflectionLevel
        )
        WhiteNoiseStorage.updatePlayingSoundReverb(trackId, config)
        com.bicy.whitenoise.H3HO.ReverbManager.setConfig(trackId, config)
        
        val currentClipId = com.bicy.whitenoise.H3HO.ScatteredPlayerManager.getTrackState(trackId)?.currentClipId
        if (currentClipId != null) {
            OboeAudioEngine.setReverbParams(currentClipId, roomSize, damping, wetLevel)
            OboeAudioEngine.setInsulation(currentClipId, insulation)
            OboeAudioEngine.setReverbDecayTime(currentClipId, decayTime)
            OboeAudioEngine.setReverbPreDelay(currentClipId, preDelay)
            OboeAudioEngine.setReverbDryLevel(currentClipId, dryLevel)
            OboeAudioEngine.setReflectionDensity(currentClipId, reflectionDensity)
            OboeAudioEngine.setReflectionSpread(currentClipId, reflectionSpread)
            OboeAudioEngine.setHighpassCutoff(currentClipId, highpassCutoff)
            OboeAudioEngine.setEarlyReflectionLevel(currentClipId, earlyReflectionLevel)
            OboeAudioEngine.setEffectEnabled(currentClipId, true)
        }
    }
    
    val applyPreset: (com.bicy.whitenoise.yODW.NvYq.EF5M.ReverbPreset) -> Unit = { preset ->
        roomSize = preset.roomSize
        decayTime = preset.decayTime
        damping = preset.damping
        wetLevel = preset.wetLevel
        dryLevel = preset.dryLevel
        preDelay = preset.preDelay
        reflectionDensity = preset.reflectionDensity
        reflectionSpread = preset.reflectionSpread
        highpassCutoff = preset.highpassCutoff
        earlyReflectionLevel = preset.earlyReflectionLevel
        applyPreview()
    }
    
    var isApplied by remember { mutableStateOf(false) }
    
    val restoreOriginal: () -> Unit = {
        WhiteNoiseStorage.updatePlayingSoundReverb(trackId, originalConfig)
        
        val currentClipId = com.bicy.whitenoise.H3HO.ScatteredPlayerManager.getTrackState(trackId)?.currentClipId
        if (currentClipId != null) {
            OboeAudioEngine.setReverbParams(currentClipId, originalConfig.roomSize, originalConfig.damping, originalConfig.wetLevel)
            OboeAudioEngine.setInsulation(currentClipId, originalConfig.insulation)
            OboeAudioEngine.setReverbDecayTime(currentClipId, originalConfig.decayTime)
            OboeAudioEngine.setReverbPreDelay(currentClipId, originalConfig.preDelay)
            OboeAudioEngine.setReverbDryLevel(currentClipId, originalConfig.dryLevel)
            OboeAudioEngine.setReflectionDensity(currentClipId, originalConfig.reflectionDensity)
            OboeAudioEngine.setReflectionSpread(currentClipId, originalConfig.reflectionSpread)
            OboeAudioEngine.setHighpassCutoff(currentClipId, originalConfig.highpassCutoff)
            OboeAudioEngine.setEarlyReflectionLevel(currentClipId, originalConfig.earlyReflectionLevel)
        }
    }
    
    DisposableEffect(trackId) {
        onDispose {
            if (!isApplied) {
                restoreOriginal()
            }
        }
    }
    
    @Suppress("DEPRECATION")
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.audio_group_config),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = trackName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            CollapsibleSection(
                title = stringResource(R.string.audio_group_config),
                expanded = audioGroupExpanded,
                onToggle = { expandedSection = if (audioGroupExpanded) null else "audioGroup" }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable { onAddAudioClip() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_audio),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tap_to_add_audio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (audioClips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = stringResource(R.string.added_audio, audioClips.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(audioClips, key = { it.id }) { clip ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = clip.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                IconButton(
                                    onClick = { onRemoveAudioClip(clip.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "移除",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.playback_interval),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.min_interval)}：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(80.dp)
                    )
                    InteractiveSlider(
                        value = minInterval,
                        onValueChange = { minInterval = it },
                        modifier = Modifier.weight(1f),
                        valueRange = 0f..30000f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "${(minInterval / 1000).toInt()}${stringResource(R.string.seconds)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.width(48.dp)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.max_interval)}：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(80.dp)
                    )
                    InteractiveSlider(
                        value = maxInterval,
                        onValueChange = { maxInterval = it },
                        modifier = Modifier.weight(1f),
                        valueRange = 0f..60000f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "${(maxInterval / 1000).toInt()}${stringResource(R.string.seconds)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.width(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = overlayMode,
                        onCheckedChange = { overlayMode = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.overlay_mode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.overlay_mode_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CollapsibleSection(
                title = stringResource(R.string.spatial_reverb),
                expanded = spatialReverbExpanded,
                onToggle = { expandedSection = if (spatialReverbExpanded) null else "spatialReverb" }
            ) {
                Text(
                    text = stringResource(R.string.preset),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        PresetChip(
                            label = stringResource(R.string.none),
                            selected = selectedPreset == "",
                            onClick = {
                                selectedPreset = ""
                            }
                        )
                    }
                    items(reverbPresets, key = { it.name }) { preset ->
                        PresetChip(
                            label = preset.name,
                            selected = selectedPreset == preset.name,
                            onClick = {
                                selectedPreset = preset.name
                                applyPreset(preset)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ReverbSlider(
                    label = stringResource(R.string.room_size),
                    value = roomSize,
                    valueRange = 0f..1f,
                    valueText = String.format("%.2f", roomSize),
                    onValueChange = { 
                        roomSize = it
                        selectedPreset = ""
                        applyPreview()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ReverbSlider(
                    label = stringResource(R.string.decay_time),
                    value = (decayTime - 0.1f) / 9.9f,
                    valueRange = 0f..1f,
                    valueText = String.format("%.2f${stringResource(R.string.seconds)}", decayTime),
                    onValueChange = { 
                        decayTime = 0.1f + it * 9.9f
                        selectedPreset = ""
                        applyPreview()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ReverbSlider(
                    label = stringResource(R.string.damping),
                    value = damping,
                    valueRange = 0f..1f,
                    valueText = String.format("%.2f", damping),
                    onValueChange = { 
                        damping = it
                        selectedPreset = ""
                        applyPreview()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ReverbSlider(
                    label = stringResource(R.string.wet_level),
                    value = wetLevel,
                    valueRange = 0f..1f,
                    valueText = String.format("%.2f", wetLevel),
                    onValueChange = { 
                        wetLevel = it
                        selectedPreset = ""
                        applyPreview()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ReverbSlider(
                    label = stringResource(R.string.dry_level),
                    value = dryLevel,
                    valueRange = 0f..1f,
                    valueText = String.format("%.2f", dryLevel),
                    onValueChange = { 
                        dryLevel = it
                        selectedPreset = ""
                        applyPreview()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ReverbSlider(
                    label = stringResource(R.string.pre_delay),
                    value = preDelay * 1000f,
                    valueRange = 0f..100f,
                    valueText = String.format("%.0fms", preDelay * 1000f),
                    onValueChange = { 
                        preDelay = it / 1000f
                        selectedPreset = ""
                        applyPreview()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ReverbSlider(
                    label = stringResource(R.string.reflection_density),
                    value = reflectionDensity,
                    valueRange = 0f..1f,
                    valueText = String.format("%.2f", reflectionDensity),
                    onValueChange = { 
                        reflectionDensity = it
                        selectedPreset = ""
                        applyPreview()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ReverbSlider(
                    label = stringResource(R.string.reflection_spread),
                    value = reflectionSpread,
                    valueRange = 0f..1f,
                    valueText = String.format("%.2f", reflectionSpread),
                    onValueChange = { 
                        reflectionSpread = it
                        selectedPreset = ""
                        applyPreview()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ReverbSlider(
                    label = stringResource(R.string.highpass_filter),
                    value = highpassCutoff,
                    valueRange = 20f..500f,
                    valueText = String.format("%.0fHz", highpassCutoff),
                    onValueChange = { 
                        highpassCutoff = it
                        selectedPreset = ""
                        applyPreview()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ReverbSlider(
                    label = stringResource(R.string.early_reflection),
                    value = earlyReflectionLevel,
                    valueRange = 0f..1f,
                    valueText = String.format("%.2f", earlyReflectionLevel),
                    onValueChange = { 
                        earlyReflectionLevel = it
                        selectedPreset = ""
                        applyPreview()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CollapsibleSection(
                title = stringResource(R.string.quality_effect),
                expanded = creativeExpanded,
                onToggle = { expandedSection = if (creativeExpanded) null else "creative" }
            ) {
                ReverbSlider(
                    label = stringResource(R.string.insulation),
                    value = insulation,
                    valueRange = 0f..1f,
                    valueText = String.format("%.2f", insulation),
                    onValueChange = { 
                        insulation = it
                        applyPreview()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                EffectSliderItem(
                    name = "Lo-Fi",
                    intensity = loFiIntensity,
                    onIntensityChange = { 
                        loFiIntensity = it
                        applyCreativeEffect(CreativeEffectType.LoFi, it)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                EffectSliderItem(
                    name = "8-bit游戏",
                    intensity = eightBitIntensity,
                    onIntensityChange = { 
                        eightBitIntensity = it
                        applyCreativeEffect(CreativeEffectType.EightBit, it)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                EffectSliderItem(
                    name = "水下",
                    intensity = underwaterIntensity,
                    onIntensityChange = { 
                        underwaterIntensity = it
                        applyCreativeEffect(CreativeEffectType.Underwater, it)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                EffectSliderItem(
                    name = "外星信号",
                    intensity = alienSignalIntensity,
                    onIntensityChange = { 
                        alienSignalIntensity = it
                        applyCreativeEffect(CreativeEffectType.AlienSignal, it)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                EffectSliderItem(
                    name = "扩音器",
                    intensity = megaphoneIntensity,
                    onIntensityChange = { 
                        megaphoneIntensity = it
                        applyCreativeEffect(CreativeEffectType.Megaphone, it)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                EffectSliderItem(
                    name = stringResource(R.string.pseudo_restoration_processing),
                    intensity = hifiIntensity,
                    onIntensityChange = { 
                        hifiIntensity = it
                        applyCreativeEffect(com.bicy.whitenoise.yODW.SrEO.Xomm.AdditionalParamType.HiFi, it)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CollapsibleSection(
                title = "空间散点范围",
                expanded = spatialScatterExpanded,
                onToggle = { expandedSection = if (spatialScatterExpanded) null else "spatialScatter" },
                subtitle = "(实验性)",
                warningText = "作为声向偏移处理，使用途中可能会出现卡顿情况"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "启用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = spatialScatterEnabled,
                        onCheckedChange = { spatialScatterEnabled = it }
                    )
                }
                
                if (spatialScatterEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    RangeSlider(
                        label = "X轴 (左右)",
                        minValue = spatialRange.xMin,
                        maxValue = spatialRange.xMax,
                        onValueChange = { min, max ->
                            spatialRange = spatialRange.copy(xMin = min, xMax = max)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    RangeSlider(
                        label = "Y轴 (高度)",
                        minValue = spatialRange.yMin,
                        maxValue = spatialRange.yMax,
                        onValueChange = { min, max ->
                            spatialRange = spatialRange.copy(yMin = min, yMax = max)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    RangeSlider(
                        label = "Z轴 (前后)",
                        minValue = spatialRange.zMin,
                        maxValue = spatialRange.zMax,
                        onValueChange = { min, max ->
                            spatialRange = spatialRange.copy(zMin = min, zMax = max)
                        }
                    )
                }
            }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    val reverbConfig = ReverbConfig(
                        enabled = true,
                        preset = selectedPreset,
                        roomSize = roomSize,
                        decayTime = decayTime,
                        damping = damping,
                        wetLevel = wetLevel,
                        dryLevel = dryLevel,
                        preDelay = preDelay,
                        insulation = insulation,
                        reflectionDensity = reflectionDensity,
                        reflectionSpread = reflectionSpread,
                        highpassCutoff = highpassCutoff,
                        earlyReflectionLevel = earlyReflectionLevel
                    )
                    val creativeConfig = CreativeEffectConfig(
                        loFi = loFiIntensity,
                        eightBit = eightBitIntensity,
                        underwater = underwaterIntensity,
                        alienSignal = alienSignalIntensity,
                        megaphone = megaphoneIntensity,
                        hifi = hifiIntensity
                    )
                    onApply(spatialRange, minInterval.toLong(), maxInterval.toLong(), spatialScatterEnabled, overlayMode, reverbConfig, creativeConfig)
                    isApplied = true
                }) {
                    Text("确定")
                }
            }
        }
    }
}

@Composable
private fun RangeSlider(
    label: String,
    minValue: Float,
    maxValue: Float,
    onValueChange: (Float, Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: [${minValue.toInt()}m ~ ${maxValue.toInt()}m]",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InteractiveSlider(
                value = minValue,
                onValueChange = { onValueChange(it, maxValue) },
                modifier = Modifier.weight(1f),
                valueRange = -10f..10f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            InteractiveSlider(
                value = maxValue,
                onValueChange = { onValueChange(minValue, it) },
                modifier = Modifier.weight(1f),
                valueRange = -10f..10f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
