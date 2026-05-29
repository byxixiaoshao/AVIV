package com.bicy.whitenoise.yODW.SrEO.Xomm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import android.util.Log
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import kotlinx.coroutines.delay
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
    val flatPreset = stringResource(R.string.eq_preset_flat)
    var selectedPreset by remember { mutableStateOf(flatPreset) }
    var presetExpanded by remember { mutableStateOf(false) }
    
    val autoEqEnabled = ConfigStorage.isAutoEqEnabled()
    val config by ConfigStorage.config.collectAsState()
    val autoEqMode = config.autoEqMode
    val autoEqIntensity = config.autoEqIntensity
    val autoEqTargetCurve = config.autoEqTargetCurve
    
    LaunchedEffect(autoEqEnabled) {
        Log.d("EqualizerPanel", "autoEqEnabled changed: $autoEqEnabled, soundId=$soundId")
        if (soundId != null) {
            if (autoEqEnabled) {
                OboeAudioEngine.setEqEnabled(soundId, false)
                val intensity = autoEqIntensity
                val targetCurve = autoEqTargetCurve
                val englishCurve = targetCurveToEnglish(targetCurve)
                OboeAudioEngine.setAutoEqIntensity(soundId, intensity)
                OboeAudioEngine.setAutoEqTargetCurve(soundId, englishCurve)
                OboeAudioEngine.setAutoEqBassBias(soundId, ConfigStorage.getAutoEqBassBias())
                OboeAudioEngine.setAutoEqMidBias(soundId, ConfigStorage.getAutoEqMidBias())
                OboeAudioEngine.setAutoEqTrebleBias(soundId, ConfigStorage.getAutoEqTrebleBias())
                OboeAudioEngine.setAutoEqResponseSpeed(soundId, ConfigStorage.getAutoEqResponseSpeed())
                OboeAudioEngine.setAutoEqModeEnabled(soundId, true)
            } else {
                OboeAudioEngine.setAutoEqModeEnabled(soundId, false)
                val eqConfig = MusicStorage.getEqualizerConfig()
                OboeAudioEngine.setEqGains(soundId, eqConfig.gains)
                OboeAudioEngine.setEqEnabled(soundId, eqConfig.enabled)
                gains = eqConfig.gains.copyOf()
            }
        }
    }
    
    LaunchedEffect(soundId, autoEqEnabled) {
        if (soundId != null && autoEqEnabled) {
            while (true) {
                delay(500)
                if (OboeAudioEngine.hasHybridEqCurve(soundId)) {
                    val autoGains = OboeAudioEngine.getAutoEqGains(soundId)
                    gains = autoGains
                    
                    if (ConfigStorage.isAutoEqSyncToManual()) {
                        MusicStorage.updateEqualizerConfig(
                            EqualizerConfig(
                                enabled = true,
                                gains = autoGains.copyOf()
                            )
                        )
                    }
                    
                    delay(2000)
                }
            }
        }
    }
    
    val eqPresetFlat = stringResource(R.string.eq_preset_flat)
    val eqPresetCustom = stringResource(R.string.eq_preset_custom)
    val eqPresetBassBoost = stringResource(R.string.eq_preset_bass_boost)
    val eqPresetTrebleBoost = stringResource(R.string.eq_preset_treble_boost)
    val eqPresetVocal = stringResource(R.string.eq_preset_vocal)
    val eqPresetRock = stringResource(R.string.eq_preset_rock)
    val eqPresetPop = stringResource(R.string.eq_preset_pop)
    val eqPresetClassical = stringResource(R.string.eq_preset_classical)
    val eqPresetJazz = stringResource(R.string.eq_preset_jazz)
    val eqPresetElectronic = stringResource(R.string.eq_preset_electronic)
    val eqPresetVShape = stringResource(R.string.eq_preset_v_shape)
    
    val eqPresets = remember {
        listOf(
            EqPresetData(eqPresetFlat, FloatArray(12) { 0f }),
            EqPresetData(eqPresetBassBoost, floatArrayOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
            EqPresetData(eqPresetTrebleBoost, floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 2f, 4f, 5f, 6f, 6f)),
            EqPresetData(eqPresetVocal, floatArrayOf(-2f, -1f, 0f, 2f, 4f, 5f, 5f, 4f, 2f, 0f, -1f, -2f)),
            EqPresetData(eqPresetRock, floatArrayOf(4f, 3f, 1f, -1f, -2f, 0f, 2f, 3f, 4f, 4f, 3f, 2f)),
            EqPresetData(eqPresetPop, floatArrayOf(-1f, 0f, 2f, 4f, 5f, 4f, 2f, 0f, -1f, -1f, -1f, -1f)),
            EqPresetData(eqPresetClassical, floatArrayOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f, 4f, 4f)),
            EqPresetData(eqPresetJazz, floatArrayOf(3f, 2f, 0f, 1f, 2f, 2f, 2f, 3f, 4f, 4f, 3f, 3f)),
            EqPresetData(eqPresetElectronic, floatArrayOf(5f, 4f, 2f, 0f, -1f, -1f, 0f, -1f, 1f, 3f, 4f, 5f)),
            EqPresetData(eqPresetVShape, floatArrayOf(5f, 4f, 2f, 0f, -2f, -2f, -2f, 0f, 2f, 4f, 5f, 5f))
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.equalizer),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAuto = ConfigStorage.isAutoEqEnabled()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!isAuto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            ConfigStorage.setAutoEqEnabled(false)
                            if (soundId != null) OboeAudioEngine.setAutoEqEnabled(soundId, false)
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.eq_mode_manual),
                        fontSize = 12.sp,
                        fontWeight = if (!isAuto) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isAuto) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isAuto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            Log.d("EqualizerPanel", "Auto button clicked, soundId = $soundId")
                            ConfigStorage.setAutoEqEnabled(true)
                            if (soundId != null) {
                                Log.d("EqualizerPanel", "Calling OboeAudioEngine.setAutoEqEnabled($soundId, true)")
                                OboeAudioEngine.setAutoEqEnabled(soundId, true)
                            } else {
                                Log.w("EqualizerPanel", "soundId is null, cannot enable AutoEQ")
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.eq_mode_auto),
                        fontSize = 12.sp,
                        fontWeight = if (isAuto) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAuto) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (ConfigStorage.isAutoEqEnabled()) {
            val autoEqModeDisplay = config.autoEqMode
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (autoEqModeDisplay == "simple") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                        .clickable {
                            Log.d("EqualizerPanel", "Simple mode clicked, soundId = $soundId")
                            ConfigStorage.setAutoEqMode("simple")
                            if (soundId != null) {
                                val intensity = ConfigStorage.getAutoEqIntensity()
                                Log.d("EqualizerPanel", "Setting intensity to $intensity for simple mode")
                                OboeAudioEngine.setAutoEqIntensity(soundId, intensity)
                            }
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.eq_mode_simple),
                        fontSize = 12.sp,
                        fontWeight = if (autoEqModeDisplay == "simple") FontWeight.Bold else FontWeight.Normal,
                        color = if (autoEqModeDisplay == "simple") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (autoEqModeDisplay == "pro") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                        .clickable {
                            Log.d("EqualizerPanel", "Pro mode clicked, soundId = $soundId")
                            ConfigStorage.setAutoEqMode("pro")
                            if (soundId != null) {
                                Log.d("EqualizerPanel", "Setting intensity to 1.0 for pro mode")
                                OboeAudioEngine.setAutoEqIntensity(soundId, 1.0f)
                            } else {
                                Log.w("EqualizerPanel", "soundId is null, cannot set intensity")
                            }
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.eq_mode_advanced),
                        fontSize = 12.sp,
                        fontWeight = if (autoEqModeDisplay == "pro") FontWeight.Bold else FontWeight.Normal,
                        color = if (autoEqModeDisplay == "pro") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (soundId != null) {
                HybridEqStatusIndicator(soundId)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (autoEqMode == "simple") {
                AutoEqSimplePanel(soundId)
            } else {
                AutoEqProPanel(soundId)
            }
        } else {
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
                            selectedPreset = eqPresetCustom
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
                    selectedPreset = eqPresetFlat
                    applyGains(gains)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(stringResource(R.string.reset), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoEqSimplePanel(soundId: String?) {
    var intensity by remember { mutableFloatStateOf(ConfigStorage.getAutoEqIntensity()) }
    var bassBias by remember { mutableFloatStateOf(ConfigStorage.getAutoEqBassBias()) }
    var midBias by remember { mutableFloatStateOf(ConfigStorage.getAutoEqMidBias()) }
    var trebleBias by remember { mutableFloatStateOf(ConfigStorage.getAutoEqTrebleBias()) }
    var targetCurve by remember { mutableStateOf(ConfigStorage.getAutoEqTargetCurve()) }
    var responseSpeed by remember { mutableStateOf(ConfigStorage.getAutoEqResponseSpeed()) }
    var targetMenuExpanded by remember { mutableStateOf(false) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    
    val targetCurves = listOf(stringResource(R.string.auto_eq_curve_flat), stringResource(R.string.auto_eq_curve_warm), stringResource(R.string.auto_eq_curve_bright), stringResource(R.string.auto_eq_curve_vocal), stringResource(R.string.auto_eq_curve_loudness))
    val speedOptions = listOf(stringResource(R.string.auto_eq_speed_fast), stringResource(R.string.auto_eq_speed_medium), stringResource(R.string.auto_eq_speed_slow))
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        AutoEqSectionTitle(stringResource(R.string.auto_eq_global_intensity))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${(intensity * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = intensity,
                onValueChange = {
                    intensity = it
                    ConfigStorage.setAutoEqIntensity(it)
                    if (soundId != null) OboeAudioEngine.setAutoEqIntensity(soundId, it)
                    
                    val mappedMaxGain = it * 24f
                    ConfigStorage.setAutoEqProMaxBoost(mappedMaxGain)
                    ConfigStorage.setAutoEqProMaxCut(mappedMaxGain)
                    if (soundId != null) {
                        OboeAudioEngine.setAutoEqMaxBoost(soundId, mappedMaxGain)
                        OboeAudioEngine.setAutoEqMaxCut(soundId, mappedMaxGain)
                    }
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        AutoEqSectionTitle(stringResource(R.string.auto_eq_bias_offset))
        
        BiasSlider(stringResource(R.string.auto_eq_bass_bias), bassBias) {
            bassBias = it
            ConfigStorage.setAutoEqBassBias(it)
            if (soundId != null) OboeAudioEngine.setAutoEqBassBias(soundId, it)
        }
        BiasSlider(stringResource(R.string.auto_eq_mid_bias), midBias) {
            midBias = it
            ConfigStorage.setAutoEqMidBias(it)
            if (soundId != null) OboeAudioEngine.setAutoEqMidBias(soundId, it)
        }
        BiasSlider(stringResource(R.string.auto_eq_treble_bias), trebleBias) {
            trebleBias = it
            ConfigStorage.setAutoEqTrebleBias(it)
            if (soundId != null) OboeAudioEngine.setAutoEqTrebleBias(soundId, it)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        AutoEqSectionTitle(stringResource(R.string.auto_eq_target_curve))
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable { targetMenuExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = targetCurve,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            DropdownMenu(
                expanded = targetMenuExpanded,
                onDismissRequest = { targetMenuExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                targetCurves.forEach { curve ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = curve,
                                fontSize = 14.sp,
                                color = if (targetCurve == curve) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            targetCurve = curve
                            targetMenuExpanded = false
                            ConfigStorage.setAutoEqTargetCurve(curve)
                            val englishCurve = targetCurveToEnglish(curve)
                            if (soundId != null) OboeAudioEngine.setAutoEqTargetCurve(soundId, englishCurve)
                            
                            val (brightness, loudness) = when (curve) {
                                targetCurves[0] -> 0f to 0f
                                targetCurves[1] -> -1.5f to 0f
                                targetCurves[2] -> 1.5f to 0f
                                targetCurves[3] -> 0.5f to 2f
                                targetCurves[4] -> 0f to 4f
                                else -> 0f to 0f
                            }
                            ConfigStorage.setAutoEqProBrightnessTarget(brightness)
                            ConfigStorage.setAutoEqProLoudnessTarget(loudness)
                            if (soundId != null) {
                                OboeAudioEngine.setAutoEqBrightnessTarget(soundId, brightness)
                                OboeAudioEngine.setAutoEqLoudnessTarget(soundId, loudness)
                            }
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        AutoEqSectionTitle(stringResource(R.string.auto_eq_response_speed))
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable { speedMenuExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = responseSpeed,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            DropdownMenu(
                expanded = speedMenuExpanded,
                onDismissRequest = { speedMenuExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                speedOptions.forEach { speed ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = speed,
                                fontSize = 14.sp,
                                color = if (responseSpeed == speed) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            responseSpeed = speed
                            speedMenuExpanded = false
                            ConfigStorage.setAutoEqResponseSpeed(speed)
                            if (soundId != null) OboeAudioEngine.setAutoEqResponseSpeed(soundId, speed)
                            
                            val (attackMs, releaseMs) = when (speed) {
                                speedOptions[0] -> 50f to 100f
                                speedOptions[1] -> 150f to 300f
                                speedOptions[2] -> 300f to 600f
                                else -> 150f to 300f
                            }
                            ConfigStorage.setAutoEqProAttack(attackMs)
                            ConfigStorage.setAutoEqProRelease(releaseMs)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.auto_eq_simple_desc),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun AutoEqProPanel(soundId: String?) {
    var attack by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProAttack()) }
    var release by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProRelease()) }
    var maxSlope by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProMaxSlope()) }
    var maxBoost by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProMaxBoost()) }
    var maxCut by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProMaxCut()) }
    var smoothing by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProSmoothing()) }
    var brightnessTarget by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProBrightnessTarget()) }
    var loudnessTarget by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProLoudnessTarget()) }
    var couplingCoeff by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProCouplingCoeff()) }
    var hysteresisDb by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProHysteresisDb()) }
    var dynamicQEnabled by remember { mutableStateOf(ConfigStorage.getAutoEqProDynamicQEnabled()) }
    var bassBias by remember { mutableFloatStateOf(ConfigStorage.getAutoEqBassBias()) }
    var midBias by remember { mutableFloatStateOf(ConfigStorage.getAutoEqMidBias()) }
    var trebleBias by remember { mutableFloatStateOf(ConfigStorage.getAutoEqTrebleBias()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(930.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AutoEqSectionTitle(stringResource(R.string.auto_eq_freq_bias))
        BiasSlider(stringResource(R.string.auto_eq_bass_bias_freq), bassBias) {
            bassBias = it
            ConfigStorage.setAutoEqBassBias(it)
            if (soundId != null) OboeAudioEngine.setAutoEqBassBias(soundId, it)
        }
        BiasSlider(stringResource(R.string.auto_eq_mid_bias_freq), midBias) {
            midBias = it
            ConfigStorage.setAutoEqMidBias(it)
            if (soundId != null) OboeAudioEngine.setAutoEqMidBias(soundId, it)
        }
        BiasSlider(stringResource(R.string.auto_eq_treble_bias_freq), trebleBias) {
            trebleBias = it
            ConfigStorage.setAutoEqTrebleBias(it)
            if (soundId != null) OboeAudioEngine.setAutoEqTrebleBias(soundId, it)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        AutoEqSectionTitle(stringResource(R.string.auto_eq_dynamic_response))
        ParaSlider("Attack", attack, "ms", 10f..500f) {
            attack = it
            ConfigStorage.setAutoEqProAttack(it)
        }
        ParaSlider("Release", release, "ms", 20f..1000f) {
            release = it
            ConfigStorage.setAutoEqProRelease(it)
        }
        ParaSlider(stringResource(R.string.auto_eq_max_slope), maxSlope, "dB/s", 0.5f..50f) {
            maxSlope = it
            ConfigStorage.setAutoEqProMaxSlope(it)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        AutoEqSectionTitle(stringResource(R.string.auto_eq_freq_limit))
        ParaSlider(stringResource(R.string.auto_eq_max_boost), maxBoost, "dB", 0f..24f) {
            maxBoost = it
            ConfigStorage.setAutoEqProMaxBoost(it)
            if (soundId != null) OboeAudioEngine.setAutoEqMaxBoost(soundId, it)
        }
        ParaSlider(stringResource(R.string.auto_eq_max_cut), maxCut, "dB", 0f..24f) {
            maxCut = it
            ConfigStorage.setAutoEqProMaxCut(it)
            if (soundId != null) OboeAudioEngine.setAutoEqMaxCut(soundId, it)
        }
        ParaSlider(stringResource(R.string.auto_eq_hysteresis), hysteresisDb, "dB", 0.2f..3f) {
            hysteresisDb = it
            ConfigStorage.setAutoEqProHysteresisDb(it)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        AutoEqSectionTitle(stringResource(R.string.auto_eq_spectrum_analysis))
        ParaSlider(stringResource(R.string.auto_eq_smoothing), smoothing, "", 0f..0.99f) {
            smoothing = it
            ConfigStorage.setAutoEqProSmoothing(it)
            if (soundId != null) OboeAudioEngine.setAutoEqSmoothing(soundId, it)
        }
        ParaSlider(stringResource(R.string.auto_eq_coupling), couplingCoeff, "", 0f..1f) {
            couplingCoeff = it
            ConfigStorage.setAutoEqProCouplingCoeff(it)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        AutoEqSectionTitle(stringResource(R.string.auto_eq_curve_fine_tune))
        ParaSlider(stringResource(R.string.auto_eq_brightness_target), brightnessTarget, "dB/oct", -4.5f..4.5f) {
            brightnessTarget = it
            ConfigStorage.setAutoEqProBrightnessTarget(it)
            if (soundId != null) OboeAudioEngine.setAutoEqBrightnessTarget(soundId, it)
        }
        ParaSlider(stringResource(R.string.auto_eq_loudness_target), loudnessTarget, "dB", -6f..6f) {
            loudnessTarget = it
            ConfigStorage.setAutoEqProLoudnessTarget(it)
            if (soundId != null) OboeAudioEngine.setAutoEqLoudnessTarget(soundId, it)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable {
                    dynamicQEnabled = !dynamicQEnabled
                    ConfigStorage.setAutoEqProDynamicQEnabled(dynamicQEnabled)
                    if (soundId != null) OboeAudioEngine.setAutoEqDynamicQEnabled(soundId, dynamicQEnabled)
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.auto_eq_dynamic_q),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Checkbox(
                checked = dynamicQEnabled,
                onCheckedChange = { 
                    dynamicQEnabled = it
                    ConfigStorage.setAutoEqProDynamicQEnabled(it)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.auto_eq_advanced_desc),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun AutoEqSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

private fun targetCurveToEnglish(curve: String): String {
    return when {
        curve.contains("flat", ignoreCase = true) || curve.contains("平坦") || curve.contains("フラット") -> "flat"
        curve.contains("warm", ignoreCase = true) || curve.contains("温暖") || curve.contains("ウォーム") -> "warm"
        curve.contains("bright", ignoreCase = true) || curve.contains("明亮") || curve.contains("ブライト") -> "bright"
        curve.contains("vocal", ignoreCase = true) || curve.contains("人声") || curve.contains("ボーカル") -> "vocal"
        curve.contains("loud", ignoreCase = true) || curve.contains("响度") || curve.contains("ラウドネス") -> "loudness"
        else -> "flat"
    }
}

@Composable
private fun BiasSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(72.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -6f..6f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Text(
            text = "${if (value >= 0) "+" else ""}${"%.1f".format(value)} dB",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ParaSlider(
    label: String,
    value: Float,
    unit: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "${"%.1f".format(value)}$unit",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
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
    
    val noneString = stringResource(R.string.none)
    var selectedPreset by remember { mutableStateOf(noneString) }
    var reverbPresetExpanded by remember { mutableStateOf(false) }
    
    val savedEffects = MusicStorage.getEffectIntensities()
    var loFiIntensity by remember { mutableFloatStateOf(savedEffects.loFi) }
    var eightBitIntensity by remember { mutableFloatStateOf(savedEffects.eightBit) }
    var underwaterIntensity by remember { mutableFloatStateOf(savedEffects.underwater) }
    var alienSignalIntensity by remember { mutableFloatStateOf(savedEffects.alienSignal) }
    var megaphoneIntensity by remember { mutableFloatStateOf(savedEffects.megaphone) }
    
    var hifiIntensity by remember { mutableFloatStateOf(savedEffects.hifi) }
    
    var musicVolume by remember { mutableFloatStateOf(MusicStorage.getVolume()) }
    
    val reverbPresetStadium = stringResource(R.string.reverb_preset_stadium)
    val reverbPresetCar = stringResource(R.string.reverb_preset_car)
    val reverbPresetBathroom = stringResource(R.string.reverb_preset_bathroom)
    val reverbPresetChurch = stringResource(R.string.reverb_preset_church)
    val reverbPresetSmallClub = stringResource(R.string.reverb_preset_small_club)
    val reverbPresetForest = stringResource(R.string.reverb_preset_forest)
    val reverbPresetValley = stringResource(R.string.reverb_preset_valley)
    val reverbPresetSeaside = stringResource(R.string.reverb_preset_seaside)
    val reverbPresetDesert = stringResource(R.string.reverb_preset_desert)
    val reverbPresetCave = stringResource(R.string.reverb_preset_cave)
    val reverbPresetTunnel = stringResource(R.string.reverb_preset_tunnel)
    
    val reverbPresets = remember {
        listOf(
            ReverbPreset(reverbPresetStadium, 0.9f, 4.5f, 0.2f, 0.5f, 0.7f, 0.05f, 0.6f, 0.7f, 80f, 0.3f),
            ReverbPreset(reverbPresetCar, 0.1f, 0.3f, 0.8f, 0.3f, 0.9f, 0.005f, 0.8f, 0.6f, 150f, 0.1f),
            ReverbPreset(reverbPresetBathroom, 0.2f, 1.0f, 0.1f, 0.6f, 0.8f, 0.01f, 0.7f, 0.5f, 120f, 0.2f),
            ReverbPreset(reverbPresetChurch, 1.0f, 4.0f, 0.15f, 0.55f, 0.6f, 0.08f, 0.5f, 0.6f, 60f, 0.4f),
            ReverbPreset(reverbPresetSmallClub, 0.4f, 1.5f, 0.4f, 0.4f, 0.85f, 0.02f, 0.6f, 0.5f, 100f, 0.15f),
            ReverbPreset(reverbPresetForest, 0.2f, 0.5f, 0.85f, 0.1f, 0.92f, 0.01f, 0.4f, 0.3f, 300f, 0.15f),
            ReverbPreset(reverbPresetValley, 0.7f, 1.8f, 0.6f, 0.25f, 0.8f, 0.12f, 0.3f, 0.4f, 250f, 0.4f),
            ReverbPreset(reverbPresetSeaside, 0.1f, 0.15f, 0.95f, 0.05f, 0.96f, 0.0f, 0.8f, 0.7f, 350f, 0.08f),
            ReverbPreset(reverbPresetDesert, 0.05f, 0.08f, 0.98f, 0.02f, 0.98f, 0.0f, 0.9f, 0.8f, 400f, 0.0f),
            ReverbPreset(reverbPresetCave, 0.85f, 5.0f, 0.05f, 0.45f, 0.65f, 0.1f, 0.5f, 0.4f, 50f, 0.35f),
            ReverbPreset(reverbPresetTunnel, 0.6f, 2.5f, 0.2f, 0.4f, 0.75f, 0.05f, 0.4f, 0.3f, 70f, 0.2f)
        )
    }
    
    val customString = stringResource(R.string.custom)
    
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
            selectedPreset = noneString
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
            text = stringResource(R.string.audio_adjust),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CollapsibleSection(
            title = stringResource(R.string.spatial_reverb_title),
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
                        text = stringResource(R.string.preset_label, selectedPreset),
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
                    expanded = reverbPresetExpanded,
                    onDismissRequest = { reverbPresetExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = noneString,
                                fontSize = 14.sp,
                                color = if (selectedPreset == noneString)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = { applyPreset(null) },
                        modifier = Modifier.background(
                            if (selectedPreset == noneString)
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
                label = stringResource(R.string.reverb_room_size),
                value = roomSize,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", roomSize),
                onValueChange = { 
                    roomSize = it
                    selectedPreset =
                        customString
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = stringResource(R.string.reverb_decay_time),
                value = (decayTime - 0.1f) / 9.9f,
                valueRange = 0f..1f,
                valueText = stringResource(R.string.format_seconds, decayTime),
                onValueChange = { 
                    decayTime = 0.1f + it * 9.9f
                    selectedPreset = customString
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = stringResource(R.string.reverb_damping),
                value = damping,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", damping),
                onValueChange = { 
                    damping = it
                    selectedPreset = customString
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = stringResource(R.string.reverb_wet_level),
                value = wetLevel,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", wetLevel),
                onValueChange = { 
                    wetLevel = it
                    selectedPreset = customString
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = stringResource(R.string.reverb_dry_level),
                value = dryLevel,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", dryLevel),
                onValueChange = { 
                    dryLevel = it
                    selectedPreset = customString
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = stringResource(R.string.reverb_pre_delay),
                value = preDelay * 1000f,
                valueRange = 0f..100f,
                valueText = String.format("%.0fms", preDelay * 1000f),
                onValueChange = { 
                    preDelay = it / 1000f
                    selectedPreset = customString
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = stringResource(R.string.reverb_reflection_density),
                value = reflectionDensity,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", reflectionDensity),
                onValueChange = { 
                    reflectionDensity = it
                    selectedPreset = customString
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = stringResource(R.string.reverb_reflection_spread),
                value = reflectionSpread,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", reflectionSpread),
                onValueChange = { 
                    reflectionSpread = it
                    selectedPreset = customString
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = stringResource(R.string.reverb_highpass),
                value = highpassCutoff,
                valueRange = 20f..500f,
                valueText = String.format("%.0fHz", highpassCutoff),
                onValueChange = { 
                    highpassCutoff = it
                    selectedPreset = customString
                    applyAndSave()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReverbSliderComponent(
                label = stringResource(R.string.reverb_early_reflection),
                value = earlyReflectionLevel,
                valueRange = 0f..1f,
                valueText = String.format("%.2f", earlyReflectionLevel),
                onValueChange = { 
                    earlyReflectionLevel = it
                    selectedPreset = customString
                    applyAndSave()
                }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        CollapsibleSection(
            title = stringResource(R.string.sound_quality_effects),
            expanded = creativeExpanded,
            onToggle = { 
                expandedSection = if (creativeExpanded) null else "creative"
            }
        ) {
            ReverbSliderComponent(
                label = stringResource(R.string.insulation_coefficient),
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
                name = stringResource(R.string.effect_8bit_game),
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
                name = stringResource(R.string.effect_underwater),
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
                name = stringResource(R.string.effect_alien_signal),
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
                name = stringResource(R.string.effect_megaphone),
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
        
        val offsetTypeFixed = stringResource(R.string.offset_type_fixed)
        val offsetTypeSurround = stringResource(R.string.offset_type_surround)
        val offsetTypeRandom = stringResource(R.string.offset_type_random)
        val offsetTypes = listOf(offsetTypeFixed, offsetTypeSurround, offsetTypeRandom)
        
        CollapsibleSection(
            title = stringResource(R.string.sound_offset),
            expanded = obrExpanded,
            onToggle = { 
                expandedSection = if (obrExpanded) null else "obr"
            },
            subtitle = stringResource(R.string.experimental),
            warningText = stringResource(R.string.stutter_warning)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.enable),
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
                        text = stringResource(R.string.offset_type_label, offsetTypes[obrOffsetType]),
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
                    expanded = obrOffsetTypeExpanded,
                    onDismissRequest = { obrOffsetTypeExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    offsetTypes.forEachIndexed { index, name ->
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

@Composable
private fun HybridEqStatusIndicator(soundId: String) {
    var isAnalyzing by remember { mutableStateOf(false) }
    var hasCurve by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    
    LaunchedEffect(soundId) {
        var lastStatus = ""
        while (true) {
            isAnalyzing = OboeAudioEngine.isHybridEqAnalyzing(soundId)
            hasCurve = OboeAudioEngine.hasHybridEqCurve(soundId)
            progress = OboeAudioEngine.getHybridEqProgress(soundId)
            val currentStatus = "$isAnalyzing,$hasCurve,$progress"
            if (currentStatus != lastStatus) {
                Log.d("HybridEqStatus", "soundId=$soundId, isAnalyzing=$isAnalyzing, hasCurve=$hasCurve, progress=$progress")
                lastStatus = currentStatus
            }
            kotlinx.coroutines.delay(200)
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isAnalyzing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    hasCurve -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .then(
                if (hasCurve && !isAnalyzing) {
                    Modifier.clickable {
                        Log.d("HybridEqStatus", "Re-analyze clicked for soundId=$soundId")
                        val filePath = MusicCacheManager.getFilePath(soundId)
                        if (!filePath.isNullOrEmpty()) {
                            OboeAudioEngine.setAutoEqEnabled(soundId, true, filePath)
                        }
                    }
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            isAnalyzing -> MaterialTheme.colorScheme.primary
                            hasCurve -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = when {
                    isAnalyzing -> "正在分析音频..."
                    hasCurve -> "EQ 曲线已就绪"
                    else -> "等待分析"
                },
                fontSize = 13.sp,
                color = when {
                    isAnalyzing -> MaterialTheme.colorScheme.primary
                    hasCurve -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
        }
        
        if (isAnalyzing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$progress%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width((progress / 100f * 60).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        } else if (hasCurve) {
            Text(
                text = "混合模式",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
