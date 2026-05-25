package com.bicy.whitenoise.yODW.NvYq

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.bicy.whitenoise.H3HO.CreativeEffectType
import com.bicy.whitenoise.H3HO.ReverbConfig
import com.bicy.whitenoise.H3HO.ReverbManager
import com.bicy.whitenoise.JwJY.NATg.ConfigStorage
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialAudioConfig
import com.bicy.whitenoise.JwJY.sBYh.kcFp.CreativeEffectConfig
import com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage
import com.bicy.whitenoise.yODW.NvYq.EF5M.CollapsibleSection
import com.bicy.whitenoise.yODW.NvYq.EF5M.EffectSliderItem
import com.bicy.whitenoise.yODW.NvYq.EF5M.PremiumRequiredReverbDialog
import com.bicy.whitenoise.yODW.NvYq.EF5M.PresetChip
import com.bicy.whitenoise.yODW.NvYq.EF5M.ReverbSlider
import com.bicy.whitenoise.yODW.NvYq.EF5M.reverbPresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReverbConfigDialog(
    soundId: String,
    soundName: String,
    onDismiss: () -> Unit,
    onApply: (ReverbConfig) -> Unit
) {
    val globalState by ConfigStorage.config.collectAsState()
    val isPremiumUser = globalState.isPremium
    
    if (!isPremiumUser) {
        PremiumRequiredReverbDialog(onDismiss = onDismiss)
        return
    }
    
    val savedConfig = remember { 
        WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == soundId }?.reverbConfig
            ?: ReverbManager.getConfig(soundId) 
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
    
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val spatialReverbExpanded = expandedSection == "spatialReverb"
    val creativeExpanded = expandedSection == "creative"
    
    var selectedPreset by remember { mutableStateOf("") }
    
    val savedCreativeConfig = remember { WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == soundId }?.creativeEffectConfig ?: CreativeEffectConfig() }
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
        WhiteNoiseStorage.updatePlayingSoundCreative(soundId, config)
        com.bicy.whitenoise.H3HO.CreativeEffectManager.setConfig(soundId, config)
    }
    
    val savedSpatialConfig = remember { WhiteNoiseStorage.getPlaybackState().sounds.find { it.id == soundId }?.spatialAudioConfig ?: SpatialAudioConfig() }
    var obrEnabled by remember { mutableStateOf(savedSpatialConfig.enabled) }
    var obrOffsetType by remember { mutableStateOf(savedSpatialConfig.offsetType) }
    var obrFixedLeftRight by remember { mutableFloatStateOf(savedSpatialConfig.fixedLeftRight) }
    var obrFixedUpDown by remember { mutableFloatStateOf(savedSpatialConfig.fixedUpDown) }
    var obrFixedFrontBack by remember { mutableFloatStateOf(savedSpatialConfig.fixedFrontBack) }
    var obrFixedMultiplier by remember { mutableFloatStateOf(savedSpatialConfig.fixedMultiplier) }
    var obrSurroundMode by remember { mutableStateOf(savedSpatialConfig.surroundMode) }
    var obrSurroundRadius by remember { mutableFloatStateOf(savedSpatialConfig.surroundRadius) }
    var obrSurroundSpeed by remember { mutableFloatStateOf(savedSpatialConfig.surroundSpeed) }
    var obrRandomMaxDistance by remember { mutableFloatStateOf(savedSpatialConfig.randomMaxDistance) }
    var obrRandomMinDistance by remember { mutableFloatStateOf(savedSpatialConfig.randomMinDistance) }
    var obrRandomValue by remember { mutableFloatStateOf(savedSpatialConfig.randomValue) }
    var obrRandomSpeed by remember { mutableFloatStateOf(savedSpatialConfig.randomSpeed) }
    var obrOffsetTypeExpanded by remember { mutableStateOf(false) }
    var obrSurroundModeExpanded by remember { mutableStateOf(false) }
    
    val saveSpatialConfig: () -> Unit = {
        val config = SpatialAudioConfig(
            enabled = obrEnabled,
            offsetType = obrOffsetType,
            fixedLeftRight = obrFixedLeftRight,
            fixedUpDown = obrFixedUpDown,
            fixedFrontBack = obrFixedFrontBack,
            fixedMultiplier = obrFixedMultiplier,
            surroundMode = obrSurroundMode,
            surroundRadius = obrSurroundRadius,
            surroundSpeed = obrSurroundSpeed,
            randomMaxDistance = obrRandomMaxDistance,
            randomMinDistance = obrRandomMinDistance,
            randomValue = obrRandomValue,
            randomSpeed = obrRandomSpeed
        )
        WhiteNoiseStorage.updatePlayingSoundSpatial(soundId, config)
        com.bicy.whitenoise.H3HO.SpatialAudioManager.setConfig(soundId, config)
        
        OboeAudioEngine.setSpatialEnabled(soundId, obrEnabled)
        OboeAudioEngine.setSpatialOffsetType(soundId, obrOffsetType)
        OboeAudioEngine.setSpatialFixedOffset(soundId, obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, obrFixedMultiplier)
        OboeAudioEngine.setSpatialSurroundParams(soundId, obrSurroundMode, obrSurroundRadius, obrSurroundSpeed)
        OboeAudioEngine.setSpatialRandomParams(soundId, obrRandomMaxDistance, obrRandomMinDistance, obrRandomValue, obrRandomSpeed)
    }
    
    val applyPreview: () -> Unit = {
        OboeAudioEngine.setReverbParams(soundId, roomSize, damping, wetLevel)
        OboeAudioEngine.setInsulation(soundId, insulation)
        OboeAudioEngine.setReverbDecayTime(soundId, decayTime)
        OboeAudioEngine.setReverbPreDelay(soundId, preDelay)
        OboeAudioEngine.setReverbDryLevel(soundId, dryLevel)
        OboeAudioEngine.setReflectionDensity(soundId, reflectionDensity)
        OboeAudioEngine.setReflectionSpread(soundId, reflectionSpread)
        OboeAudioEngine.setHighpassCutoff(soundId, highpassCutoff)
        OboeAudioEngine.setEarlyReflectionLevel(soundId, earlyReflectionLevel)
        OboeAudioEngine.setEffectEnabled(soundId, true)
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
        OboeAudioEngine.setReverbParams(soundId, originalConfig.roomSize, originalConfig.damping, originalConfig.wetLevel)
        OboeAudioEngine.setInsulation(soundId, originalConfig.insulation)
        OboeAudioEngine.setReverbDecayTime(soundId, originalConfig.decayTime)
        OboeAudioEngine.setReverbPreDelay(soundId, originalConfig.preDelay)
        OboeAudioEngine.setReverbDryLevel(soundId, originalConfig.dryLevel)
        OboeAudioEngine.setReflectionDensity(soundId, originalConfig.reflectionDensity)
        OboeAudioEngine.setReflectionSpread(soundId, originalConfig.reflectionSpread)
        OboeAudioEngine.setHighpassCutoff(soundId, originalConfig.highpassCutoff)
        OboeAudioEngine.setEarlyReflectionLevel(soundId, originalConfig.earlyReflectionLevel)
    }
    
    DisposableEffect(soundId) {
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
                text = "音频效果配置",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = soundName,
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
                title = "空间混响",
                expanded = spatialReverbExpanded,
                onToggle = { expandedSection = if (spatialReverbExpanded) null else "spatialReverb" }
            ) {
                Text(
                    text = "预设",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        PresetChip(
                            label = "无",
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
                    label = "房间大小",
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
                    label = "衰减时间",
                    value = (decayTime - 0.1f) / 9.9f,
                    valueRange = 0f..1f,
                    valueText = String.format("%.2f秒", decayTime),
                    onValueChange = { 
                        decayTime = 0.1f + it * 9.9f
                        selectedPreset = ""
                        applyPreview()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ReverbSlider(
                    label = "阻尼",
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
                    label = "湿声电平",
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
                    label = "干声电平",
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
                    label = "预延迟",
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
                title = "音质效果",
                expanded = creativeExpanded,
                onToggle = { expandedSection = if (creativeExpanded) null else "creative" }
            ) {
                ReverbSlider(
                    label = "隔音系数",
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
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.LoFi, it)
                        saveCreativeConfig()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                EffectSliderItem(
                    name = "8-bit游戏",
                    intensity = eightBitIntensity,
                    onIntensityChange = { 
                        eightBitIntensity = it
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.EightBit, it)
                        saveCreativeConfig()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                EffectSliderItem(
                    name = "水下",
                    intensity = underwaterIntensity,
                    onIntensityChange = { 
                        underwaterIntensity = it
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.Underwater, it)
                        saveCreativeConfig()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                EffectSliderItem(
                    name = "外星信号",
                    intensity = alienSignalIntensity,
                    onIntensityChange = { 
                        alienSignalIntensity = it
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.AlienSignal, it)
                        saveCreativeConfig()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                EffectSliderItem(
                    name = "扩音器",
                    intensity = megaphoneIntensity,
                    onIntensityChange = { 
                        megaphoneIntensity = it
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.Megaphone, it)
                        saveCreativeConfig()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                EffectSliderItem(
                    name = stringResource(R.string.pseudo_restoration_processing),
                    intensity = hifiIntensity,
                    onIntensityChange = { 
                        hifiIntensity = it
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, com.bicy.whitenoise.yODW.SrEO.Xomm.AdditionalParamType.HiFi, it)
                        saveCreativeConfig()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val obrExpanded = expandedSection == "obr"
            CollapsibleSection(
                title = "声向偏移",
                expanded = obrExpanded,
                onToggle = { expandedSection = if (obrExpanded) null else "obr" },
                subtitle = "(实验性)",
                warningText = "使用途中可能会出现卡顿情况"
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
                        checked = obrEnabled,
                        onCheckedChange = { 
                            obrEnabled = it
                            saveSpatialConfig()
                            OboeAudioEngine.setSpatialEnabled(soundId, it)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .clickable { obrOffsetTypeExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "偏移类型: ${when(obrOffsetType) { 0 -> "固定偏移"; 1 -> "3D环绕"; else -> "随机游动" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "展开",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = obrOffsetTypeExpanded,
                        onDismissRequest = { obrOffsetTypeExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("固定偏移") },
                            onClick = {
                                obrOffsetType = 0
                                obrOffsetTypeExpanded = false
                                saveSpatialConfig()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("3D环绕") },
                            onClick = {
                                obrOffsetType = 1
                                obrOffsetTypeExpanded = false
                                saveSpatialConfig()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("随机游动") },
                            onClick = {
                                obrOffsetType = 2
                                obrOffsetTypeExpanded = false
                                saveSpatialConfig()
                            }
                        )
                    }
                }
                
                if (obrOffsetType == 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ReverbSlider(
                        label = "X轴旋转",
                        value = obrFixedLeftRight,
                        valueRange = -180f..180f,
                        valueText = String.format("%.0f°", obrFixedLeftRight),
                        onValueChange = { 
                            val snapped = if (kotlin.math.abs(it) < 5f) 0f else kotlin.math.round(it)
                            obrFixedLeftRight = snapped
                            saveSpatialConfig()
                            OboeAudioEngine.setSpatialFixedOffset(soundId, obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, obrFixedMultiplier)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReverbSlider(
                        label = "Y轴旋转",
                        value = obrFixedUpDown,
                        valueRange = -180f..180f,
                        valueText = String.format("%.0f°", obrFixedUpDown),
                        onValueChange = { 
                            val snapped = if (kotlin.math.abs(it) < 5f) 0f else kotlin.math.round(it)
                            obrFixedUpDown = snapped
                            saveSpatialConfig()
                            OboeAudioEngine.setSpatialFixedOffset(soundId, obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, obrFixedMultiplier)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReverbSlider(
                        label = "Z轴旋转",
                        value = obrFixedFrontBack,
                        valueRange = -180f..180f,
                        valueText = String.format("%.0f°", obrFixedFrontBack),
                        onValueChange = { 
                            val snapped = if (kotlin.math.abs(it) < 5f) 0f else kotlin.math.round(it)
                            obrFixedFrontBack = snapped
                            saveSpatialConfig()
                            OboeAudioEngine.setSpatialFixedOffset(soundId, obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, obrFixedMultiplier)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReverbSlider(
                        label = "偏移距离",
                        value = obrFixedMultiplier,
                        valueRange = 0f..5f,
                        valueText = String.format("%.1fm", obrFixedMultiplier),
                        steps = 50,
                        onValueChange = { 
                            obrFixedMultiplier = it
                            saveSpatialConfig()
                            OboeAudioEngine.setSpatialFixedOffset(soundId, obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, it)
                        }
                    )
                }
                
                if (obrOffsetType == 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .clickable { obrSurroundModeExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "环绕方式: ${when(obrSurroundMode) { 0 -> "水平环绕"; 1 -> "纵切环绕"; else -> "横切环绕" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "展开",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = obrSurroundModeExpanded,
                            onDismissRequest = { obrSurroundModeExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            val surroundModes = listOf("水平环绕", "纵切环绕", "横切环绕")
                            surroundModes.forEachIndexed { index, label ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = label,
                                            color = if (obrSurroundMode == index)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        obrSurroundMode = index
                                        obrSurroundModeExpanded = false
                                        saveSpatialConfig()
                                        OboeAudioEngine.setSpatialSurroundParams(soundId, index, obrSurroundRadius, obrSurroundSpeed)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ReverbSlider(
                        label = "环绕半径",
                        value = obrSurroundRadius,
                        valueRange = 0.1f..5f,
                        valueText = String.format("%.1fm", obrSurroundRadius),
                        steps = 49,
                        onValueChange = { 
                            obrSurroundRadius = it
                            saveSpatialConfig()
                            OboeAudioEngine.setSpatialSurroundParams(soundId, obrSurroundMode, it, obrSurroundSpeed)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReverbSlider(
                        label = "环绕速度",
                        value = obrSurroundSpeed,
                        valueRange = 1f..60f,
                        valueText = String.format("%.0f秒/圈", obrSurroundSpeed),
                        steps = 59,
                        onValueChange = { 
                            obrSurroundSpeed = it
                            saveSpatialConfig()
                            OboeAudioEngine.setSpatialSurroundParams(soundId, obrSurroundMode, obrSurroundRadius, it)
                        }
                    )
                }
                
                if (obrOffsetType == 2) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ReverbSlider(
                        label = "最大距离",
                        value = obrRandomMaxDistance,
                        valueRange = 0f..10f,
                        valueText = String.format("%.1fm", obrRandomMaxDistance),
                        steps = 100,
                        onValueChange = { 
                            obrRandomMaxDistance = it
                            saveSpatialConfig()
                            OboeAudioEngine.setSpatialRandomParams(soundId, it, obrRandomMinDistance, obrRandomValue, obrRandomSpeed)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReverbSlider(
                        label = "最小距离",
                        value = obrRandomMinDistance,
                        valueRange = 0f..obrRandomMaxDistance,
                        valueText = String.format("%.1fm", obrRandomMinDistance),
                        steps = (obrRandomMaxDistance * 10).toInt(),
                        onValueChange = { 
                            obrRandomMinDistance = it
                            saveSpatialConfig()
                            OboeAudioEngine.setSpatialRandomParams(soundId, obrRandomMaxDistance, it, obrRandomValue, obrRandomSpeed)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReverbSlider(
                        label = "游动随机值",
                        value = obrRandomValue,
                        valueRange = 0f..1f,
                        valueText = String.format("%.1f", obrRandomValue),
                        steps = 10,
                        onValueChange = { 
                            obrRandomValue = it
                            saveSpatialConfig()
                            OboeAudioEngine.setSpatialRandomParams(soundId, obrRandomMaxDistance, obrRandomMinDistance, it, obrRandomSpeed)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReverbSlider(
                        label = "游动速度",
                        value = obrRandomSpeed,
                        valueRange = 0.1f..2f,
                        valueText = String.format("%.1f", obrRandomSpeed),
                        steps = 19,
                        onValueChange = { 
                            obrRandomSpeed = it
                            saveSpatialConfig()
                            OboeAudioEngine.setSpatialRandomParams(soundId, obrRandomMaxDistance, obrRandomMinDistance, obrRandomValue, it)
                        }
                    )
                }
            }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = {
                        roomSize = 0f
                        decayTime = 1.5f
                        damping = 0f
                        wetLevel = 0f
                        dryLevel = 1f
                        preDelay = 0.025f
                        insulation = 0f
                        reflectionDensity = 0.5f
                        reflectionSpread = 0.5f
                        highpassCutoff = 100f
                        earlyReflectionLevel = 0f
                        applyPreview()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("重置")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                        restoreOriginal()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("取消")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                        isApplied = true
                        saveSpatialConfig()
                        val config = ReverbConfig(
                            enabled = true,
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
                        onApply(config)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("应用", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
