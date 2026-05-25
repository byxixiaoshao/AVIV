package com.bicy.whitenoise.yODW.SrEO.Xomm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.bicy.whitenoise.yODW.SrEO.InteractiveSlider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bicy.whitenoise.R
import com.bicy.whitenoise.H3HO.OboeAudioEngine
import com.bicy.whitenoise.H3HO.CreativeEffectType
import com.bicy.whitenoise.H3HO.ReverbConfig
import com.bicy.whitenoise.JwJY.EY9i.EffectIntensities
import com.bicy.whitenoise.JwJY.EY9i.EqualizerConfig
import com.bicy.whitenoise.JwJY.EY9i.MusicStorage
import com.bicy.whitenoise.xnef.MusicCacheManager
import com.bicy.whitenoise.xnef.MusicPlayerController
import com.bicy.whitenoise.JwJY.NATg.ConfigStorage
import com.bicy.whitenoise.yODW.NvYq.EF5M.ReverbPreset

@Composable
fun VerticalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val safeProgress = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    val backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val progressColor = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = modifier) {
        if (size.width.isNaN() || size.height.isNaN() || size.width <= 0f || size.height <= 0f) {
            return@Canvas
        }
        
        drawRect(
            color = backgroundColor,
            topLeft = Offset.Zero,
            size = size
        )
        
        val progressHeight = size.height * safeProgress
        val progressTop = size.height - progressHeight
        
        drawRect(
            color = progressColor,
            topLeft = Offset(0f, progressTop),
            size = Size(size.width, progressHeight)
        )
    }
}

@Composable
fun MixerPanel(
    panelProgress: Float,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(1) }
    var limiterMenuExpanded by remember { mutableStateOf(false) }
    val limiterConfig = MusicStorage.getLimiterConfig()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .alpha(panelProgress)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> EqualizerPanel()
                1 -> ReverbPanel()
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabButton(
                text = stringResource(R.string.equalizer),
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = stringResource(R.string.more_adjustments),
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
            
            Box {
                LimiterButton(
                    enabled = limiterConfig.enabled,
                    onClick = { limiterMenuExpanded = true }
                )
                
                LimiterDropdownMenu(
                    expanded = limiterMenuExpanded,
                    onDismissRequest = { limiterMenuExpanded = false },
                    config = limiterConfig
                )
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun LimiterButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.limiter),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer { rotationZ = 180f }
        )
    }
}

@Composable
fun LimiterDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    config: com.bicy.whitenoise.JwJY.EY9i.LimiterConfig
) {
    var limitEqualizer by remember { mutableStateOf(config.limitEqualizer) }
    var limitEffects by remember { mutableStateOf(config.limitEffects) }
    var limitReverb by remember { mutableStateOf(config.limitReverb) }
    var limitSpatial by remember { mutableStateOf(config.limitSpatial) }
    
    val updateLimiterConfig: () -> Unit = {
        val newConfig = com.bicy.whitenoise.JwJY.EY9i.LimiterConfig(
            enabled = config.enabled,
            limitEqualizer = limitEqualizer,
            limitEffects = limitEffects,
            limitReverb = limitReverb,
            limitSpatial = limitSpatial,
            threshold = config.threshold,
            attack = config.attack,
            release = config.release
        )
        MusicStorage.updateLimiterConfig(newConfig)
        OboeAudioEngine.setGlobalLimiterConfig(
            enabled = newConfig.enabled,
            limitEqualizer = newConfig.limitEqualizer,
            limitEffects = newConfig.limitEffects,
            limitReverb = newConfig.limitReverb,
            limitSpatial = newConfig.limitSpatial,
            threshold = newConfig.threshold,
            attack = newConfig.attack,
            release = newConfig.release
        )
        
        val track = MusicPlayerController.currentTrack
        val soundId = track?.let { MusicCacheManager.getSoundId(it.id) }
        if (soundId != null) {
            OboeAudioEngine.setEqLimiterEnabled(soundId, newConfig.limitEqualizer)
            OboeAudioEngine.setLimitEffectsEnabled(soundId, newConfig.limitEffects)
            OboeAudioEngine.setLimitReverbEnabled(soundId, newConfig.limitReverb)
            OboeAudioEngine.setLimitSpatialEnabled(soundId, newConfig.limitSpatial)
        }
    }
    
    if (expanded) {
        Popup(
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismissRequest() },
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .padding(end = 8.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.limiter),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    
                    LimiterMenuItem(
                        text = stringResource(R.string.limit_equalizer),
                        checked = limitEqualizer,
                        onCheckedChange = {
                            limitEqualizer = it
                            updateLimiterConfig()
                        }
                    )
                    
                    LimiterMenuItem(
                        text = stringResource(R.string.limit_effects),
                        checked = limitEffects,
                        onCheckedChange = {
                            limitEffects = it
                            updateLimiterConfig()
                        }
                    )
                    
                    LimiterMenuItem(
                        text = stringResource(R.string.limit_reverb),
                        checked = limitReverb,
                        onCheckedChange = {
                            limitReverb = it
                            updateLimiterConfig()
                        }
                    )
                    
                    LimiterMenuItem(
                        text = stringResource(R.string.limit_spatial),
                        checked = limitSpatial,
                        onCheckedChange = {
                            limitSpatial = it
                            updateLimiterConfig()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LimiterMenuItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EqualizerPanel() {
    val globalState by ConfigStorage.config.collectAsState()
    val isPremiumUser = globalState.isPremium
    
    if (!isPremiumUser) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.equalizer),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.premium_exclusive_feature),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }
    
    val track = MusicPlayerController.currentTrack
    val soundId = if (track != null) MusicCacheManager.getSoundId(track.id) else null
    
    val savedEqConfig = MusicStorage.getEqualizerConfig()
    var gains by remember { mutableStateOf(savedEqConfig.gains.copyOf()) }
    var selectedPreset by remember { mutableStateOf("平坦") }
    var presetExpanded by remember { mutableStateOf(false) }
    
    LaunchedEffect(soundId) {
        if (soundId != null) {
            val eqConfig = MusicStorage.getEqualizerConfig()
            OboeAudioEngine.setEqGains(soundId, eqConfig.gains)
            OboeAudioEngine.setEqEnabled(soundId, eqConfig.enabled)
            gains = eqConfig.gains.copyOf()
        }
    }
    
    val eqPresets = remember {
        listOf(
            EqPresetData("平坦", FloatArray(12) { 0f }),
            EqPresetData("低音增强", floatArrayOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
            EqPresetData("高音增强", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 2f, 4f, 5f, 6f, 6f)),
            EqPresetData("人声增强", floatArrayOf(-2f, -1f, 0f, 2f, 4f, 5f, 5f, 4f, 2f, 0f, -1f, -2f)),
            EqPresetData("摇滚", floatArrayOf(4f, 3f, 1f, -1f, -2f, 0f, 2f, 3f, 4f, 4f, 3f, 2f)),
            EqPresetData("流行", floatArrayOf(-1f, 0f, 2f, 4f, 5f, 4f, 2f, 0f, -1f, -1f, -1f, -1f)),
            EqPresetData("古典", floatArrayOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f, 4f, 4f)),
            EqPresetData("爵士", floatArrayOf(3f, 2f, 0f, 1f, 2f, 2f, 2f, 3f, 4f, 4f, 3f, 3f)),
            EqPresetData("电子", floatArrayOf(5f, 4f, 2f, 0f, -1f, -1f, 0f, -1f, 1f, 3f, 4f, 5f)),
            EqPresetData("V型", floatArrayOf(5f, 4f, 2f, 0f, -2f, -2f, -2f, 0f, 2f, 4f, 5f, 5f))
        )
    }
    
    val applyGains: (FloatArray) -> Unit = { newGains ->
        if (soundId != null) {
            OboeAudioEngine.setEqGains(soundId, newGains)
            OboeAudioEngine.setEqEnabled(soundId, true)
            MusicStorage.updateEqualizerConfig(
                EqualizerConfig(
                    enabled = true,
                    gains = newGains.copyOf()
                )
            )
        }
    }
    
    val applyPreset: (EqPresetData) -> Unit = { preset ->
        gains = preset.gains.copyOf()
        selectedPreset = preset.name
        presetExpanded = false
        applyGains(preset.gains)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.equalizer),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable { presetExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.preset)}: $selectedPreset",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.expand),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            DropdownMenu(
                expanded = presetExpanded,
                onDismissRequest = { presetExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                eqPresets.forEach { preset ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = preset.name,
                                fontSize = 14.sp,
                                color = if (selectedPreset == preset.name)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = { applyPreset(preset) },
                        modifier = Modifier.background(
                            if (selectedPreset == preset.name)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val frequencies = listOf("31", "63", "125", "250", "500", "1K", "2K", "4K", "8K", "12K", "16K", "20K")
            
            frequencies.forEachIndexed { index, freq ->
                EqBandSliderHorizontal(
                    frequency = freq,
                    gain = gains[index],
                    onGainChange = { newGain ->
                        val newGains = gains.copyOf()
                        newGains[index] = newGain
                        gains = newGains
                        selectedPreset = "自定义"
                        applyGains(newGains)
                    }
                )
                
                if (index < frequencies.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = {
                gains = FloatArray(12) { 0f }
                selectedPreset = "平坦"
                applyGains(gains)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("重置", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqBandSliderHorizontal(
    frequency: String,
    gain: Float,
    onGainChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = frequency,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(36.dp)
        )
        
        InteractiveSlider(
            value = gain,
            onValueChange = onGainChange,
            valueRange = -12f..12f,
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
        
        Text(
            text = String.format("%+.0f", gain),
            fontSize = 11.sp,
            color = when {
                gain > 0 -> MaterialTheme.colorScheme.primary
                gain < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReverbPanel() {
    val reverbConfig by remember { mutableStateOf(MusicStorage.getReverbConfig()) }
    
    var roomSize by remember { mutableFloatStateOf(reverbConfig.roomSize) }
    var decayTime by remember { mutableFloatStateOf(reverbConfig.decayTime) }
    var damping by remember { mutableFloatStateOf(reverbConfig.damping) }
    var wetLevel by remember { mutableFloatStateOf(reverbConfig.wetLevel) }
    var dryLevel by remember { mutableFloatStateOf(reverbConfig.dryLevel) }
    var preDelay by remember { mutableFloatStateOf(reverbConfig.preDelay) }
    var insulation by remember { mutableFloatStateOf(reverbConfig.insulation) }
    var reflectionDensity by remember { mutableFloatStateOf(reverbConfig.reflectionDensity) }
    var reflectionSpread by remember { mutableFloatStateOf(reverbConfig.reflectionSpread) }
    var highpassCutoff by remember { mutableFloatStateOf(reverbConfig.highpassCutoff) }
    var earlyReflectionLevel by remember { mutableFloatStateOf(reverbConfig.earlyReflectionLevel) }
    
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val spatialReverbExpanded = expandedSection == "spatialReverb"
    val creativeExpanded = expandedSection == "creative"
    
    var selectedPreset by remember { mutableStateOf("无") }
    var reverbPresetExpanded by remember { mutableStateOf(false) }
    
    val savedEffects = MusicStorage.getEffectIntensities()
    var loFiIntensity by remember { mutableFloatStateOf(savedEffects.loFi) }
    var eightBitIntensity by remember { mutableFloatStateOf(savedEffects.eightBit) }
    var underwaterIntensity by remember { mutableFloatStateOf(savedEffects.underwater) }
    var alienSignalIntensity by remember { mutableFloatStateOf(savedEffects.alienSignal) }
    var megaphoneIntensity by remember { mutableFloatStateOf(savedEffects.megaphone) }
    
    var hifiIntensity by remember { mutableFloatStateOf(savedEffects.hifi) }
    
    var musicVolume by remember { mutableFloatStateOf(MusicStorage.getVolume()) }
    
    val reverbPresets = remember {
        listOf(
            ReverbPreset("体育场", 0.9f, 4.5f, 0.2f, 0.5f, 0.7f, 0.05f, 0.6f, 0.7f, 80f, 0.3f),
            ReverbPreset("汽车内", 0.1f, 0.3f, 0.8f, 0.3f, 0.9f, 0.005f, 0.8f, 0.6f, 150f, 0.1f),
            ReverbPreset("浴室", 0.2f, 1.0f, 0.1f, 0.6f, 0.8f, 0.01f, 0.7f, 0.5f, 120f, 0.2f),
            ReverbPreset("教堂", 1.0f, 4.0f, 0.15f, 0.55f, 0.6f, 0.08f, 0.5f, 0.6f, 60f, 0.4f),
            ReverbPreset("小俱乐部", 0.4f, 1.5f, 0.4f, 0.4f, 0.85f, 0.02f, 0.6f, 0.5f, 100f, 0.15f),
            ReverbPreset("森林", 0.2f, 0.5f, 0.85f, 0.1f, 0.92f, 0.01f, 0.4f, 0.3f, 300f, 0.15f),
            ReverbPreset("山谷", 0.7f, 1.8f, 0.6f, 0.25f, 0.8f, 0.12f, 0.3f, 0.4f, 250f, 0.4f),
            ReverbPreset("海边", 0.1f, 0.15f, 0.95f, 0.05f, 0.96f, 0.0f, 0.8f, 0.7f, 350f, 0.08f),
            ReverbPreset("沙漠", 0.05f, 0.08f, 0.98f, 0.02f, 0.98f, 0.0f, 0.9f, 0.8f, 400f, 0.0f),
            ReverbPreset("洞穴", 0.85f, 5.0f, 0.05f, 0.45f, 0.65f, 0.1f, 0.5f, 0.4f, 50f, 0.35f),
            ReverbPreset("隧道", 0.6f, 2.5f, 0.2f, 0.4f, 0.75f, 0.05f, 0.4f, 0.3f, 70f, 0.2f)
        )
    }
    
    val applyAndSave: () -> Unit = {
        val config = ReverbConfig(
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
        MusicStorage.updateReverbConfig(config)
        
        val track = MusicPlayerController.currentTrack
        if (track != null) {
            val soundId = MusicCacheManager.getSoundId(track.id)
            OboeAudioEngine.setReverbParams(soundId, roomSize, damping, wetLevel)
            OboeAudioEngine.setInsulation(soundId, insulation)
            OboeAudioEngine.setReverbDecayTime(soundId, decayTime)
            OboeAudioEngine.setReverbPreDelay(soundId, preDelay * 1000f)
            OboeAudioEngine.setReverbDryLevel(soundId, dryLevel)
            OboeAudioEngine.setReflectionDensity(soundId, reflectionDensity)
            OboeAudioEngine.setReflectionSpread(soundId, reflectionSpread)
            OboeAudioEngine.setHighpassCutoff(soundId, highpassCutoff)
            OboeAudioEngine.setEarlyReflectionLevel(soundId, earlyReflectionLevel)
            OboeAudioEngine.setEffectEnabled(soundId, true)
        }
    }
    
    val applyPreset: (ReverbPreset?) -> Unit = { preset ->
        if (preset != null) {
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
            selectedPreset = preset.name
        } else {
            selectedPreset = "无"
        }
        reverbPresetExpanded = false
        applyAndSave()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "音频调整",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CollapsibleSection(
            title = "空间混响",
            expanded = spatialReverbExpanded,
            onToggle = { 
                expandedSection = if (spatialReverbExpanded) null else "spatialReverb"
            }
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .clickable { reverbPresetExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "预设: $selectedPreset",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "展开",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = reverbPresetExpanded,
                    onDismissRequest = { reverbPresetExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = "无",
                                fontSize = 14.sp,
                                color = if (selectedPreset == "无")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = { applyPreset(null) },
                        modifier = Modifier.background(
                            if (selectedPreset == "无")
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    )
                    reverbPresets.forEach { preset ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = preset.name,
                                    fontSize = 14.sp,
                                    color = if (selectedPreset == preset.name)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = { applyPreset(preset) },
                            modifier = Modifier.background(
                                if (selectedPreset == preset.name)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ReverbSliderComponent(
                label = "房间大小",
                value = roomSize,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", roomSize),
                onValueChange = { 
                    roomSize = it
                    selectedPreset = "自定义"
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = "衰减时间",
                value = (decayTime - 0.1f) / 9.9f,
                valueRange = 0f..1f,
                valueText = String.format("%.2f秒", decayTime),
                onValueChange = { 
                    decayTime = 0.1f + it * 9.9f
                    selectedPreset = "自定义"
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = "阻尼",
                value = damping,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", damping),
                onValueChange = { 
                    damping = it
                    selectedPreset = "自定义"
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = "湿声电平",
                value = wetLevel,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", wetLevel),
                onValueChange = { 
                    wetLevel = it
                    selectedPreset = "自定义"
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = "干声电平",
                value = dryLevel,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", dryLevel),
                onValueChange = { 
                    dryLevel = it
                    selectedPreset = "自定义"
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = "预延迟",
                value = preDelay * 1000f,
                valueRange = 0f..100f,
                valueText = String.format("%.0fms", preDelay * 1000f),
                onValueChange = { 
                    preDelay = it / 1000f
                    selectedPreset = "自定义"
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = "反射密度",
                value = reflectionDensity,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", reflectionDensity),
                onValueChange = { 
                    reflectionDensity = it
                    selectedPreset = "自定义"
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = "反射扩散",
                value = reflectionSpread,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", reflectionSpread),
                onValueChange = { 
                    reflectionSpread = it
                    selectedPreset = "自定义"
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = "高通滤波",
                value = highpassCutoff,
                valueRange = 20f..500f,
                valueText = String.format("%.0fHz", highpassCutoff),
                onValueChange = { 
                    highpassCutoff = it
                    selectedPreset = "自定义"
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = "早期反射",
                value = earlyReflectionLevel,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", earlyReflectionLevel),
                onValueChange = { 
                    earlyReflectionLevel = it
                    selectedPreset = "自定义"
                    applyAndSave()
                }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        CollapsibleSection(
            title = "音质效果",
            expanded = creativeExpanded,
            onToggle = { 
                expandedSection = if (creativeExpanded) null else "creative"
            }
        ) {
            ReverbSliderComponent(
                label = "隔音系数",
                value = insulation,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", insulation),
                onValueChange = { 
                    insulation = it
                    applyAndSave()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            EffectSliderItem(
                name = "Lo-Fi",
                intensity = loFiIntensity,
                onIntensityChange = { 
                    loFiIntensity = it
                    MusicStorage.updateEffectIntensity("loFi", it)
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.LoFi, it)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            EffectSliderItem(
                name = "8-bit游戏",
                intensity = eightBitIntensity,
                onIntensityChange = { 
                    eightBitIntensity = it
                    MusicStorage.updateEffectIntensity("eightBit", it)
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.EightBit, it)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            EffectSliderItem(
                name = "水下",
                intensity = underwaterIntensity,
                onIntensityChange = { 
                    underwaterIntensity = it
                    MusicStorage.updateEffectIntensity("underwater", it)
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.Underwater, it)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            EffectSliderItem(
                name = "外星信号",
                intensity = alienSignalIntensity,
                onIntensityChange = { 
                    alienSignalIntensity = it
                    MusicStorage.updateEffectIntensity("alienSignal", it)
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.AlienSignal, it)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            EffectSliderItem(
                name = "扩音器",
                intensity = megaphoneIntensity,
                onIntensityChange = { 
                    megaphoneIntensity = it
                    MusicStorage.updateEffectIntensity("megaphone", it)
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.Megaphone, it)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            EffectSliderItem(
                name = stringResource(R.string.pseudo_restoration_processing),
                intensity = hifiIntensity,
                onIntensityChange = { 
                    hifiIntensity = it
                    MusicStorage.updateEffectIntensity("hifi", it)
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, AdditionalParamType.HiFi, it)
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        val savedSpatialConfig = MusicStorage.getSpatialAudioConfig()
        val obrExpanded = expandedSection == "obr"
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
        
        CollapsibleSection(
            title = "声向偏移",
            expanded = obrExpanded,
            onToggle = { 
                expandedSection = if (obrExpanded) null else "obr"
            },
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
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Checkbox(
                    checked = obrEnabled,
                    onCheckedChange = { 
                        obrEnabled = it
                        MusicStorage.updateSpatialAudioEnabled(it)
                        val track = MusicPlayerController.currentTrack
                        if (track != null) {
                            val soundId = MusicCacheManager.getSoundId(track.id)
                            OboeAudioEngine.setSpatialEnabled(soundId, it)
                        }
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
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "展开",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = obrOffsetTypeExpanded,
                    onDismissRequest = { obrOffsetTypeExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    listOf("固定偏移", "3D环绕", "随机游动").forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = name,
                                    fontSize = 14.sp,
                                    color = if (obrOffsetType == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = { 
                                obrOffsetType = index
                                obrOffsetTypeExpanded = false
                                MusicStorage.updateSpatialAudioOffsetType(index)
                                val track = MusicPlayerController.currentTrack
                                if (track != null) {
                                    val soundId = MusicCacheManager.getSoundId(track.id)
                                    OboeAudioEngine.setSpatialOffsetType(soundId, index)
                                }
                            },
                            modifier = Modifier.background(
                                if (obrOffsetType == index)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
            
            if (obrOffsetType == 0) {
                Spacer(modifier = Modifier.height(12.dp))
                ReverbSliderComponent(
                    label = "X轴旋转",
                    value = obrFixedLeftRight,
                    valueRange = -180f..180f,
                    valueText = String.format("%.0f°", obrFixedLeftRight),
                    onValueChange = { 
                        val snapped = if (kotlin.math.abs(it) < 5f) 0f else kotlin.math.round(it)
                        obrFixedLeftRight = snapped
                        MusicStorage.updateSpatialAudioFixedOffset(obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, obrFixedMultiplier)
                        val track = MusicPlayerController.currentTrack
                        if (track != null) {
                            val soundId = MusicCacheManager.getSoundId(track.id)
                            OboeAudioEngine.setSpatialFixedOffset(soundId, obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, obrFixedMultiplier)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                ReverbSliderComponent(
                    label = "Y轴旋转",
                    value = obrFixedUpDown,
                    valueRange = -180f..180f,
                    valueText = String.format("%.0f°", obrFixedUpDown),
                    onValueChange = { 
                        val snapped = if (kotlin.math.abs(it) < 5f) 0f else kotlin.math.round(it)
                        obrFixedUpDown = snapped
                        MusicStorage.updateSpatialAudioFixedOffset(obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, obrFixedMultiplier)
                        val track = MusicPlayerController.currentTrack
                        if (track != null) {
                            val soundId = MusicCacheManager.getSoundId(track.id)
                            OboeAudioEngine.setSpatialFixedOffset(soundId, obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, obrFixedMultiplier)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                ReverbSliderComponent(
                    label = "Z轴旋转",
                    value = obrFixedFrontBack,
                    valueRange = -180f..180f,
                    valueText = String.format("%.0f°", obrFixedFrontBack),
                    onValueChange = { 
                        val snapped = if (kotlin.math.abs(it) < 5f) 0f else kotlin.math.round(it)
                        obrFixedFrontBack = snapped
                        MusicStorage.updateSpatialAudioFixedOffset(obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, obrFixedMultiplier)
                        val track = MusicPlayerController.currentTrack
                        if (track != null) {
                            val soundId = MusicCacheManager.getSoundId(track.id)
                            OboeAudioEngine.setSpatialFixedOffset(soundId, obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, obrFixedMultiplier)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                ReverbSliderComponent(
                    label = "偏移距离",
                    value = obrFixedMultiplier,
                    valueRange = 0f..5f,
                    valueText = String.format("%.1fm", obrFixedMultiplier),
                    steps = 50,
                    onValueChange = { 
                        obrFixedMultiplier = it
                        MusicStorage.updateSpatialAudioFixedOffset(obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, it)
                        val track = MusicPlayerController.currentTrack
                        if (track != null) {
                            val soundId = MusicCacheManager.getSoundId(track.id)
                            OboeAudioEngine.setSpatialFixedOffset(soundId, obrFixedLeftRight, obrFixedUpDown, obrFixedFrontBack, it)
                        }
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
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "展开",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = obrSurroundModeExpanded,
                        onDismissRequest = { obrSurroundModeExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        val surroundModes = listOf("水平环绕", "纵切环绕", "横切环绕")
                        surroundModes.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = name,
                                        fontSize = 14.sp,
                                        color = if (obrSurroundMode == index)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { 
                                    obrSurroundMode = index
                                    obrSurroundModeExpanded = false
                                    MusicStorage.updateSpatialAudioSurroundParams(index, obrSurroundRadius, obrSurroundSpeed)
                                    val track = MusicPlayerController.currentTrack
                                    if (track != null) {
                                        val soundId = MusicCacheManager.getSoundId(track.id)
                                        OboeAudioEngine.setSpatialSurroundParams(soundId, index, obrSurroundRadius, obrSurroundSpeed)
                                    }
                                },
                                modifier = Modifier.background(
                                    if (obrSurroundMode == index)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                ReverbSliderComponent(
                    label = "环绕半径",
                    value = obrSurroundRadius,
                    valueRange = 0.1f..5f,
                    valueText = String.format("%.1fm", obrSurroundRadius),
                    steps = 49,
                    onValueChange = { 
                        obrSurroundRadius = it
                        MusicStorage.updateSpatialAudioSurroundParams(obrSurroundMode, it, obrSurroundSpeed)
                        val track = MusicPlayerController.currentTrack
                        if (track != null) {
                            val soundId = MusicCacheManager.getSoundId(track.id)
                            OboeAudioEngine.setSpatialSurroundParams(soundId, obrSurroundMode, it, obrSurroundSpeed)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                ReverbSliderComponent(
                    label = "环绕速度",
                    value = obrSurroundSpeed,
                    valueRange = 1f..60f,
                    valueText = String.format("%.0f秒/圈", obrSurroundSpeed),
                    steps = 59,
                    onValueChange = { 
                        obrSurroundSpeed = it
                        MusicStorage.updateSpatialAudioSurroundParams(obrSurroundMode, obrSurroundRadius, it)
                        val track = MusicPlayerController.currentTrack
                        if (track != null) {
                            val soundId = MusicCacheManager.getSoundId(track.id)
                            OboeAudioEngine.setSpatialSurroundParams(soundId, obrSurroundMode, obrSurroundRadius, it)
                        }
                    }
                )
            }
            
            if (obrOffsetType == 2) {
                Spacer(modifier = Modifier.height(12.dp))
                ReverbSliderComponent(
                    label = "最大距离",
                    value = obrRandomMaxDistance,
                    valueRange = 0f..10f,
                    valueText = String.format("%.1fm", obrRandomMaxDistance),
                    steps = 100,
                    onValueChange = { 
                        obrRandomMaxDistance = it
                        if (obrRandomMinDistance > it) {
                            obrRandomMinDistance = it
                        }
                        MusicStorage.updateSpatialAudioRandomParams(it, obrRandomMinDistance, obrRandomValue, obrRandomSpeed)
                        val track = MusicPlayerController.currentTrack
                        if (track != null) {
                            val soundId = MusicCacheManager.getSoundId(track.id)
                            OboeAudioEngine.setSpatialRandomParams(soundId, it, obrRandomMinDistance, obrRandomValue, obrRandomSpeed)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                ReverbSliderComponent(
                    label = "最小距离",
                    value = obrRandomMinDistance,
                    valueRange = 0f..obrRandomMaxDistance,
                    valueText = String.format("%.1fm", obrRandomMinDistance),
                    steps = (obrRandomMaxDistance * 10).toInt(),
                    onValueChange = { 
                        obrRandomMinDistance = it
                        MusicStorage.updateSpatialAudioRandomParams(obrRandomMaxDistance, it, obrRandomValue, obrRandomSpeed)
                        val track = MusicPlayerController.currentTrack
                        if (track != null) {
                            val soundId = MusicCacheManager.getSoundId(track.id)
                            OboeAudioEngine.setSpatialRandomParams(soundId, obrRandomMaxDistance, it, obrRandomValue, obrRandomSpeed)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                ReverbSliderComponent(
                    label = "游动随机值",
                    value = obrRandomValue,
                    valueRange = 0f..1f,
                    valueText = String.format("%.1f", obrRandomValue),
                    steps = 10,
                    onValueChange = { 
                        obrRandomValue = it
                        MusicStorage.updateSpatialAudioRandomParams(obrRandomMaxDistance, obrRandomMinDistance, it, obrRandomSpeed)
                        val track = MusicPlayerController.currentTrack
                        if (track != null) {
                            val soundId = MusicCacheManager.getSoundId(track.id)
                            OboeAudioEngine.setSpatialRandomParams(soundId, obrRandomMaxDistance, obrRandomMinDistance, it, obrRandomSpeed)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                ReverbSliderComponent(
                    label = "游动速度",
                    value = obrRandomSpeed,
                    valueRange = 0.1f..2f,
                    valueText = String.format("%.1f", obrRandomSpeed),
                    steps = 19,
                    onValueChange = { 
                        obrRandomSpeed = it
                        MusicStorage.updateSpatialAudioRandomParams(obrRandomMaxDistance, obrRandomMinDistance, obrRandomValue, it)
                        val track = MusicPlayerController.currentTrack
                        if (track != null) {
                            val soundId = MusicCacheManager.getSoundId(track.id)
                            OboeAudioEngine.setSpatialRandomParams(soundId, obrRandomMaxDistance, obrRandomMinDistance, obrRandomValue, it)
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Text(
                text = "音量",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            ReverbSliderComponent(
                label = "音量",
                value = musicVolume,
                valueRange = 0f..1f,
                valueText = String.format("%.0f%%", musicVolume * 100),
                onValueChange = { 
                    musicVolume = it
                    MusicStorage.updateVolume(it)
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setVolume(soundId, it)
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    roomSize = 0f
                    decayTime = 1.5f
                    damping = 0f
                    wetLevel = 0f
                    dryLevel = 1f
                    preDelay = 0.025f
                    insulation = 0f
                    applyAndSave()
                    
                    loFiIntensity = 0f
                    eightBitIntensity = 0f
                    underwaterIntensity = 0f
                    alienSignalIntensity = 0f
                    megaphoneIntensity = 0f
                    hifiIntensity = 0f
                    
                    musicVolume = 1f
                    MusicStorage.updateVolume(1f)
                    
                    MusicStorage.updateEffectIntensities(
                        EffectIntensities()
                    )
                    
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.LoFi, 0f)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.EightBit, 0f)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.Underwater, 0f)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.AlienSignal, 0f)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.Megaphone, 0f)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, AdditionalParamType.HiFi, 0f)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, AdditionalParamType.Distortion, 0f)
                        OboeAudioEngine.setVolume(soundId, 1f)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "重置",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    subtitle: String? = null,
    warningText: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 4.dp, bottom = 1.dp)
                        )
                    }
                }
            }
            
            Text(
                text = if (expanded) "收起" else "展开",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (warningText != null) {
                    Text(
                        text = warningText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                content()
            }
        }
    }
}

@Composable
fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                   else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EffectPlaceholderItem(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = "开发中",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectSliderItem(
    name: String,
    intensity: Float,
    onIntensityChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = String.format("%.0f%%", intensity * 100),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        InteractiveSlider(
            value = intensity,
            onValueChange = onIntensityChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReverbSliderComponent(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
    steps: Int = 0
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = valueText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        InteractiveSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PitchSliderItem(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    var internalValue by remember { mutableFloatStateOf(value) }
    
    LaunchedEffect(value) {
        internalValue = value
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "声调",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = if (internalValue >= 0) "+${internalValue.toInt()}" else "${internalValue.toInt()}",
                fontSize = 11.sp,
                color = when {
                    internalValue > 0 -> MaterialTheme.colorScheme.primary
                    internalValue < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        InteractiveSlider(
            value = internalValue,
            onValueChange = { 
                val snapped = if (kotlin.math.abs(it) < 0.5f) 0f else kotlin.math.round(it)
                internalValue = snapped
                onValueChange(snapped)
            },
            valueRange = -12f..12f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSliderItem(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    var internalValue by remember { mutableFloatStateOf(value) }
    
    LaunchedEffect(value) {
        internalValue = value
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "速度",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = String.format("%.1fx", internalValue),
                fontSize = 11.sp,
                color = when {
                    internalValue > 1f -> MaterialTheme.colorScheme.primary
                    internalValue < 1f -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        InteractiveSlider(
            value = internalValue,
            onValueChange = { 
                val snapped = if (kotlin.math.abs(it - 1f) < 0.05f) 1f else it
                internalValue = snapped
                onValueChange(snapped)
            },
            valueRange = 0.3f..3f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}
