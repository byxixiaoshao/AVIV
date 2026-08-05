package com.bicy.whitenoise.ui.components.ExpandableTopBarPart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.bicy.whitenoise.ui.components.FocusableEditText
import com.bicy.whitenoise.ui.components.InteractiveSlider
import com.bicy.whitenoise.ui.components.glass.GlassAlertDialogSimple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bicy.whitenoise.R
import com.bicy.whitenoise.audio.OboeAudioEngine
import com.bicy.whitenoise.audio.CreativeEffectType
import com.bicy.whitenoise.audio.ReverbConfig
import com.bicy.whitenoise.storage.music.EffectIntensities
import com.bicy.whitenoise.storage.music.EqualizerConfig
import com.bicy.whitenoise.storage.music.MusicStorage
import com.bicy.whitenoise.music.MusicCacheManager
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.storage.config.SpeakerPresetCurves
import com.bicy.whitenoise.service.MemoryLockService
import com.bicy.whitenoise.service.AnomalyType
import com.bicy.whitenoise.ui.screens.ReverbConfigDialogPart.ReverbPreset
import com.bicy.whitenoise.equalizer.ControlPoint
import com.bicy.whitenoise.equalizer.EqualizerCurve
import com.bicy.whitenoise.equalizer.EqualizerCurve.Companion.defaultCurve
import com.bicy.whitenoise.equalizer.EqFilterType
import com.bicy.whitenoise.equalizer.FrequencyResponseGraph
import com.bicy.whitenoise.equalizer.PresetStorage
import com.bicy.whitenoise.equalizer.EqMode
import com.bicy.whitenoise.equalizer.EqualizerPreset
import com.bicy.whitenoise.equalizer.UndoRedoManager
import com.bicy.whitenoise.equalizer.AddPointCommand
import com.bicy.whitenoise.equalizer.DeletePointCommand
import com.bicy.whitenoise.equalizer.MovePointCommand
import com.bicy.whitenoise.equalizer.ChangeTypeCommand
import com.bicy.whitenoise.equalizer.ChangeQCommand
import com.bicy.whitenoise.equalizer.ChangeCurveInCommand
import com.bicy.whitenoise.equalizer.ChangeCurveOutCommand
import com.bicy.whitenoise.equalizer.CurveInterpolation
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow

// 对数频率标度：10Hz→24000Hz 映射到 0→1
private val FREQ_MIN_LOG = ln(10f)
private val FREQ_MAX_LOG = ln(24000f)
private fun freqToRatio(freq: Float): Float =
    ((ln(freq.coerceIn(10f, 24000f)) - FREQ_MIN_LOG) / (FREQ_MAX_LOG - FREQ_MIN_LOG)).coerceIn(0f, 1f)
private fun ratioToFreq(ratio: Float): Float =
    exp(FREQ_MIN_LOG + ratio.coerceIn(0f, 1f) * (FREQ_MAX_LOG - FREQ_MIN_LOG))

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
                icon = Icons.Filled.Equalizer,
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = stringResource(R.string.more_adjustments),
                icon = Icons.Filled.Tune,
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
    icon: ImageVector,
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
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
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
    config: com.bicy.whitenoise.storage.music.LimiterConfig
) {
    var limitEqualizer by remember { mutableStateOf(config.limitEqualizer) }
    var limitEffects by remember { mutableStateOf(config.limitEffects) }
    var limitReverb by remember { mutableStateOf(config.limitReverb) }
    var limitSpatial by remember { mutableStateOf(config.limitSpatial) }

    // 阈值/启动/释放：数据模型中阈值以线性振幅存储（如 0.9），UI 以 dB 操作
    var thresholdDb by remember {
        mutableFloatStateOf(20f * log10(config.threshold.coerceAtLeast(1e-4f)))
    }
    var attackMs by remember { mutableFloatStateOf(config.attack) }
    var releaseMs by remember { mutableFloatStateOf(config.release) }

    val updateLimiterConfig: () -> Unit = {
        val newConfig = com.bicy.whitenoise.storage.music.LimiterConfig(
            enabled = config.enabled,
            limitEqualizer = limitEqualizer,
            limitEffects = limitEffects,
            limitReverb = limitReverb,
            limitSpatial = limitSpatial,
            threshold = (10.0.pow((thresholdDb / 20f).toDouble())).toFloat().coerceIn(1e-4f, 1f),
            attack = attackMs,
            release = releaseMs
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

    // 展开动画：进入时淡入+上移，退出时反向；所有关闭路径统一走 closeWithAnimation，
    // 保证退出动画播放完毕后再真正 dismiss。
    if (expanded) {
        val scope = rememberCoroutineScope()
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        val progress by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "limiter_menu_anim"
        )
        val closeWithAnimation: () -> Unit = {
            scope.launch {
                visible = false
                delay(220)
                onDismissRequest()
            }
        }
        Popup(
            onDismissRequest = closeWithAnimation,
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
                    ) { closeWithAnimation() },
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .heightIn(max = 460.dp)
                        .padding(end = 8.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { }
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                        .graphicsLayer {
                            alpha = progress
                            translationY = (1f - progress) * 60f
                            scaleX = 0.92f + 0.08f * progress
                            scaleY = 0.92f + 0.08f * progress
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.limiter),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { closeWithAnimation() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 传递函数曲线 + 电平表 + 阈值/启动/释放滑块
                    LimiterVisualizer(
                        thresholdDb = thresholdDb,
                        attackMs = attackMs,
                        releaseMs = releaseMs,
                        enabled = config.enabled,
                        onThresholdChange = {
                            thresholdDb = it
                            updateLimiterConfig()
                        },
                        onAttackChange = {
                            attackMs = it
                            updateLimiterConfig()
                        },
                        onReleaseChange = {
                            releaseMs = it
                            updateLimiterConfig()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.limit_apply_to),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
    val track = MusicPlayerController.currentTrack
    val soundId = if (track != null) MusicCacheManager.getSoundId(track.id) else null
    
    val config by ConfigStorage.config.collectAsState()
    val autoEqEnabled = config.autoEqEnabled
    val autoEqIntensity = config.autoEqIntensity
    val speakerPreset = config.speakerPreset
    
    val eqMode by PresetStorage.eqMode.collectAsState()
    val globalCurve by PresetStorage.globalCurve.collectAsState()
    
    var currentCurve by remember { mutableStateOf(EqualizerCurve.defaultCurve()) }
    var selectedPresetName by remember { mutableStateOf("Flat") }
    var selectedIndex by remember { mutableStateOf(-1) }
    var eqBypassed by remember { mutableStateOf(ConfigStorage.isEqBypassEnabled()) }
    var eqVersion by remember { mutableStateOf(0) }
    val customPresetLabel = stringResource(R.string.eq_preset_custom)
    val flatPresetLabel = stringResource(R.string.eq_preset_flat)
    val undoManager = remember { UndoRedoManager() }
    
    // 预设弹窗状态
    var showPresetDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var savePresetName by remember { mutableStateOf("") }
    val customPresets by PresetStorage.presets.collectAsState()
    val pointCountTabs = remember { listOf(5, 7, 11, 13, 16) }
    var selectedPointTab by remember { mutableStateOf(5) }
    
    fun applyCurve(curve: EqualizerCurve) {
        val selPt = curve.points.getOrNull(selectedIndex)
        curve.points.sortBy { it.frequencyHz }
        selectedIndex = if (selPt != null) curve.points.indexOfFirst { it === selPt } else -1
        eqVersion++

        val sorted = curve.points
        val freqs = FloatArray(sorted.size) { sorted[it].frequencyHz }
        val gains = FloatArray(sorted.size) { sorted[it].gainDb }
        val types = IntArray(sorted.size) { sorted[it].filterType.nativeValue }
        val qs = FloatArray(sorted.size) { sorted[it].qOverride }
        val cIns = IntArray(sorted.size) { sorted[it].curveIn.nativeValue }
        val cOuts = IntArray(sorted.size) { sorted[it].curveOut.nativeValue }
        
        if (soundId != null) {
            OboeAudioEngine.setEqualizerCurve(soundId, freqs, gains, types, qs, cIns, cOuts)
            OboeAudioEngine.setEqEnabled(soundId, !eqBypassed)
            if (eqMode == EqMode.GLOBAL) {
                PresetStorage.saveGlobalCurve(currentCurve)
            } else {
                PresetStorage.saveTrackCurve(soundId, currentCurve)
            }
        }
    }
    
    LaunchedEffect(soundId, eqMode) {
        if (soundId != null) {
            currentCurve = if (eqMode == EqMode.GLOBAL) {
                PresetStorage.getGlobalCurve()
            } else {
                PresetStorage.getTrackCurve(soundId)
            }
            selectedPresetName = currentCurve.name
            applyCurve(currentCurve)
        }
    }
    
    val autoEqBands = floatArrayOf(25f, 50f, 100f, 200f, 400f, 800f, 1600f, 3200f, 6300f, 10000f, 14000f, 16000f)

    fun autoGainsToCurve(gains: FloatArray, freqs: FloatArray): EqualizerCurve {
        val pts = gains.mapIndexed { i, g ->
            val freq = if (i < freqs.size) freqs[i] else autoEqBands[i.coerceIn(autoEqBands.indices)]
            ControlPoint(freq, g.coerceIn(-24f, 24f))
        }
        return EqualizerCurve(points = pts.toMutableList(), name = "AutoEQ")
    }

    LaunchedEffect(autoEqEnabled) {
        if (soundId == null) return@LaunchedEffect
        if (autoEqEnabled) {
            OboeAudioEngine.setAutoEqIntensity(soundId, autoEqIntensity)
            OboeAudioEngine.setSpeakerPreset(soundId, speakerPreset)
            OboeAudioEngine.setAutoEqBassBias(soundId, ConfigStorage.getAutoEqBassBias())
            OboeAudioEngine.setAutoEqMidBias(soundId, ConfigStorage.getAutoEqMidBias())
            OboeAudioEngine.setAutoEqTrebleBias(soundId, ConfigStorage.getAutoEqTrebleBias())
            OboeAudioEngine.setAutoEqBrightnessTarget(soundId, ConfigStorage.getAutoEqProBrightnessTarget())
            OboeAudioEngine.setAutoEqLoudnessTarget(soundId, ConfigStorage.getAutoEqProLoudnessTarget())
            OboeAudioEngine.setAutoEqAttack(soundId, ConfigStorage.getAutoEqProAttack())
            OboeAudioEngine.setAutoEqRelease(soundId, ConfigStorage.getAutoEqProRelease())
            OboeAudioEngine.setAutoEqMaxSlope(soundId, ConfigStorage.getAutoEqProMaxSlope())
            OboeAudioEngine.setAutoEqMaxBoost(soundId, ConfigStorage.getAutoEqProMaxBoost())
            OboeAudioEngine.setAutoEqMaxCut(soundId, ConfigStorage.getAutoEqProMaxCut())
            OboeAudioEngine.setAutoEqCouplingCoeff(soundId, ConfigStorage.getAutoEqProCouplingCoeff())
            OboeAudioEngine.setAutoEqHysteresis(soundId, ConfigStorage.getAutoEqProHysteresisDb())
            OboeAudioEngine.setAutoEqDynamicQEnabled(soundId, ConfigStorage.getAutoEqProDynamicQEnabled())
            OboeAudioEngine.setAutoEqBandCount(soundId, ConfigStorage.getAutoEqBandCount())
            OboeAudioEngine.setAutoEqBandRatios(soundId, ConfigStorage.getAutoEqLowRatio(), ConfigStorage.getAutoEqMidRatio())
            OboeAudioEngine.setAutoEqModeEnabled(soundId, true)

            while (OboeAudioEngine.isHybridEqAnalyzing(soundId)) {
                delay(200)
            }
            val autoGains = OboeAudioEngine.getAutoEqGains(soundId)
            val autoFreqs = OboeAudioEngine.getAutoEqFrequencies(soundId)
            if (autoGains.any { it != 0f }) {
                val autoCurve = autoGainsToCurve(autoGains, autoFreqs)
                val sorted = autoCurve.points
                val freqs = FloatArray(sorted.size) { sorted[it].frequencyHz }
                val gains = FloatArray(sorted.size) { sorted[it].gainDb }
                val types = IntArray(sorted.size) { sorted[it].filterType.nativeValue }
                val qs = FloatArray(sorted.size) { sorted[it].qOverride }
                val cIns = IntArray(sorted.size) { sorted[it].curveIn.nativeValue }
                val cOuts = IntArray(sorted.size) { sorted[it].curveOut.nativeValue }
                OboeAudioEngine.setEqualizerCurve(soundId, freqs, gains, types, qs, cIns, cOuts)
                OboeAudioEngine.setEqEnabled(soundId, true)

                if (ConfigStorage.isAutoEqSyncToManual()) {
                    currentCurve.points.clear()
                    currentCurve.points.addAll(autoCurve.points)
                    currentCurve.name = autoCurve.name
                    selectedPresetName = autoCurve.name
                    if (eqMode == EqMode.GLOBAL) {
                        PresetStorage.saveGlobalCurve(currentCurve)
                    } else {
                        PresetStorage.saveTrackCurve(soundId, currentCurve)
                    }
                }
            }
        } else {
            OboeAudioEngine.setAutoEqModeEnabled(soundId, false)
            applyCurve(currentCurve)
        }
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!autoEqEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
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
                        fontWeight = if (!autoEqEnabled) FontWeight.Bold else FontWeight.Normal,
                        color = if (!autoEqEnabled) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (autoEqEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            ConfigStorage.setAutoEqEnabled(true)
                            if (soundId != null) OboeAudioEngine.setAutoEqEnabled(soundId, true)
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.eq_mode_auto),
                        fontSize = 12.sp,
                        fontWeight = if (autoEqEnabled) FontWeight.Bold else FontWeight.Normal,
                        color = if (autoEqEnabled) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (autoEqEnabled) {
            SpeakerCompensationPanel(soundId)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .clickable { 
                                selectedPointTab = currentCurve.pointCount.coerceIn(5..16)
                                // 如果当前点数不在预设tab中，自动选择最近的
                                if (selectedPointTab !in pointCountTabs) {
                                    selectedPointTab = pointCountTabs.minByOrNull { abs(it - currentCurve.pointCount) } ?: 5
                                }
                                showPresetDialog = true 
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.preset) + ": $selectedPresetName",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.expand),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                // 撤销
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .clickable(enabled = undoManager.canUndo) {
                            if (undoManager.undo(currentCurve)) {
                                selectedPresetName = customPresetLabel
                                applyCurve(currentCurve)
                            }
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u21A9",
                        fontSize = 16.sp,
                        color = if (undoManager.canUndo) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                
                // 重做
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .clickable(enabled = undoManager.canRedo) {
                            if (undoManager.redo(currentCurve)) {
                                selectedPresetName = customPresetLabel
                                applyCurve(currentCurve)
                            }
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u21AA",
                        fontSize = 16.sp,
                        color = if (undoManager.canRedo) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                
                // 保存预设图标
                IconButton(
                    onClick = {
                        savePresetName = selectedPresetName.takeIf { it != customPresetLabel && it != flatPresetLabel } ?: ""
                        showSaveDialog = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = stringResource(R.string.eq_save_preset),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FrequencyResponseGraph(
                curve = currentCurve,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { idx -> selectedIndex = idx },
                onPointMoved = { idx, freq, gain ->
                    val pt = currentCurve.points.getOrNull(idx) ?: return@FrequencyResponseGraph
                    val cmd = MovePointCommand(idx, pt.frequencyHz, pt.gainDb, freq, gain)
                    undoManager.execute(cmd, currentCurve)
                    selectedPresetName = customPresetLabel
                    applyCurve(currentCurve)
                },
                onPointAdded = { freq, gain ->
                    val maxPoints = if (ConfigStorage.isEqUnlimitedPoints()) Int.MAX_VALUE else 16
                    if (currentCurve.points.size >= maxPoints) return@FrequencyResponseGraph
                    if (currentCurve.points.any { abs(it.frequencyHz - freq) < 15f }) return@FrequencyResponseGraph
                    val pt = ControlPoint(freq, gain)
                    val idx = currentCurve.points.size
                    undoManager.execute(AddPointCommand(pt, idx), currentCurve)
                    selectedIndex = idx
                    selectedPresetName = customPresetLabel
                    applyCurve(currentCurve)
                    
                    // 超过32个点时向内存锁上报
                    if (currentCurve.points.size > 32) {
                        MemoryLockService.reportAnomaly(
                            AnomalyType.AUDIO_BUFFER_UNDERRUN,
                            "EQ点过多 (${currentCurve.points.size})",
                            "均衡器控制点超过32个，可能导致音频处理性能问题"
                        )
                    }
                },
                onPointDeleted = { idx ->
                    val pt = currentCurve.points.getOrNull(idx) ?: return@FrequencyResponseGraph
                    undoManager.execute(DeletePointCommand(pt, idx), currentCurve)
                    selectedIndex = -1
                    selectedPresetName = customPresetLabel
                    applyCurve(currentCurve)
                },
                modifier = Modifier.fillMaxWidth(),
                showActualResponse = true,
                getActualResponse = { freq -> OboeAudioEngine.getFilterResponse(soundId ?: "", freq) },
                refreshKey = eqVersion
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val selPoint = currentCurve.points.getOrNull(selectedIndex)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                // 控制点芯片栏——显示实际控制点
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val _force = eqVersion
                    items(currentCurve.points.size) { i ->
                        val pt = currentCurve.points[i]
                        val isSel = i == selectedIndex
                        val label = if (pt.frequencyHz >= 1000) String.format("%.1fK", pt.frequencyHz / 1000)
                                    else String.format("%.0f", pt.frequencyHz)
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface)
                                .clickable { selectedIndex = i }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                if (selPoint != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 频率滑块（对数刻度 + 实时状态）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("f:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        var freqRatio by remember(selectedIndex, eqVersion) { mutableFloatStateOf(freqToRatio(selPoint.frequencyHz)) }
                        var freqText by remember(selectedIndex, eqVersion) { mutableStateOf(String.format("%.0f", selPoint.frequencyHz)) }
                        InteractiveSlider(
                            value = freqRatio,
                            onValueChange = { ratio ->
                                freqRatio = ratio
                                val freq = ratioToFreq(ratio)
                                freqText = String.format("%.0f", freq)
                                val pt = currentCurve.points.getOrNull(selectedIndex) ?: return@InteractiveSlider
                                val cmd = MovePointCommand(selectedIndex, pt.frequencyHz, pt.gainDb, freq, pt.gainDb)
                                undoManager.execute(cmd, currentCurve)
                                selectedPresetName = customPresetLabel
                                applyCurve(currentCurve)
                            },
                            modifier = Modifier.weight(1f).height(28.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                        FocusableEditText(
                            value = freqText,
                            onValueChange = { freqText = it },
                            onSearch = {
                                val pt = currentCurve.points.getOrNull(selectedIndex)
                                val parsed = freqText.toFloatOrNull()
                                if (pt != null && parsed != null && parsed in 10f..24000f) {
                                    freqRatio = freqToRatio(parsed)
                                    undoManager.execute(MovePointCommand(selectedIndex, pt.frequencyHz, pt.gainDb, parsed, pt.gainDb), currentCurve)
                                    selectedPresetName = customPresetLabel
                                    applyCurve(currentCurve)
                                } else if (pt != null) {
                                    freqText = String.format("%.0f", pt.frequencyHz)
                                }
                            },
                            modifier = Modifier.width(56.dp),
                            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                            imeAction = EditorInfo.IME_ACTION_DONE
                        )
                        Text("Hz", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    
                    // 增益滑块
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("g:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        var gainRatio by remember(selectedIndex, eqVersion) { mutableFloatStateOf((selPoint.gainDb + 24f) / 48f) }
                        var gainText by remember(selectedIndex, eqVersion) { mutableStateOf(String.format("%+.1f", selPoint.gainDb)) }
                        InteractiveSlider(
                            value = gainRatio,
                            onValueChange = { ratio ->
                                gainRatio = ratio
                                val gain = -24f + ratio * 48f
                                gainText = String.format("%+.1f", gain)
                                val pt = currentCurve.points.getOrNull(selectedIndex) ?: return@InteractiveSlider
                                val cmd = MovePointCommand(selectedIndex, pt.frequencyHz, pt.gainDb, pt.frequencyHz, gain)
                                undoManager.execute(cmd, currentCurve)
                                selectedPresetName = customPresetLabel
                                applyCurve(currentCurve)
                            },
                            modifier = Modifier.weight(1f).height(28.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                        FocusableEditText(
                            value = gainText,
                            onValueChange = { gainText = it },
                            onSearch = {
                                val pt = currentCurve.points.getOrNull(selectedIndex)
                                val parsed = gainText.toFloatOrNull()
                                if (pt != null && parsed != null && parsed in -24f..24f) {
                                    gainRatio = (parsed + 24f) / 48f
                                    undoManager.execute(MovePointCommand(selectedIndex, pt.frequencyHz, pt.gainDb, pt.frequencyHz, parsed), currentCurve)
                                    selectedPresetName = customPresetLabel
                                    applyCurve(currentCurve)
                                } else if (pt != null) {
                                    gainText = String.format("%+.1f", pt.gainDb)
                                }
                            },
                            modifier = Modifier.width(56.dp),
                            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED,
                            imeAction = EditorInfo.IME_ACTION_DONE
                        )
                        Text("dB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    
                    // Q 值滑块
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Q:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        var qValue by remember(selectedIndex, eqVersion) { mutableFloatStateOf(selPoint.qOverride) }
                        var qText by remember(selectedIndex, eqVersion) { mutableStateOf(String.format("%.2f", selPoint.qOverride)) }
                        InteractiveSlider(
                            value = (qValue / 10f).coerceIn(0f, 1f),
                            onValueChange = { ratio ->
                                val q = 0.1f + ratio * 9.9f
                                qValue = q
                                qText = String.format("%.2f", q)
                                val oldQ = currentCurve.points.getOrNull(selectedIndex)?.qOverride ?: q
                                val cmd = ChangeQCommand(selectedIndex, oldQ, q)
                                undoManager.execute(cmd, currentCurve)
                                selectedPresetName = customPresetLabel
                                applyCurve(currentCurve)
                            },
                            modifier = Modifier.weight(1f).height(28.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                        FocusableEditText(
                            value = qText,
                            onValueChange = { qText = it },
                            onSearch = {
                                val pt = currentCurve.points.getOrNull(selectedIndex)
                                val parsed = qText.toFloatOrNull()
                                if (pt != null && parsed != null && parsed in 0.1f..10f) {
                                    qValue = parsed
                                    undoManager.execute(ChangeQCommand(selectedIndex, pt.qOverride, parsed), currentCurve)
                                    selectedPresetName = customPresetLabel
                                    applyCurve(currentCurve)
                                } else if (pt != null) {
                                    qText = String.format("%.2f", pt.qOverride)
                                }
                            },
                            modifier = Modifier.width(56.dp),
                            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                            imeAction = EditorInfo.IME_ACTION_DONE
                        )
                    }
                    
                    // 滤波器类型
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (type in EqFilterType.entries) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (selPoint.filterType == type) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable {
                                        val cmd = ChangeTypeCommand(selectedIndex, selPoint.filterType, type)
                                        undoManager.execute(cmd, currentCurve)
                                        selectedPresetName = customPresetLabel
                                        applyCurve(currentCurve)
                                    }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(type.name, fontSize = 10.sp,
                                    color = if (selPoint.filterType == type) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    
                    // 插值方式（入/出）
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("In:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        for (mode in CurveInterpolation.entries) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selPoint.curveIn == mode) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                else MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        val oldMode = currentCurve.points.getOrNull(selectedIndex)?.curveIn ?: mode
                                        val cmd = ChangeCurveInCommand(selectedIndex, oldMode, mode)
                                        undoManager.execute(cmd, currentCurve)
                                        selectedPresetName = customPresetLabel
                                        applyCurve(currentCurve)
                                    }
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(mode.name.take(3), fontSize = 8.sp,
                                    color = if (selPoint.curveIn == mode) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Out:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        for (mode in CurveInterpolation.entries) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selPoint.curveOut == mode) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                else MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        val oldMode = currentCurve.points.getOrNull(selectedIndex)?.curveOut ?: mode
                                        val cmd = ChangeCurveOutCommand(selectedIndex, oldMode, mode)
                                        undoManager.execute(cmd, currentCurve)
                                        selectedPresetName = customPresetLabel
                                        applyCurve(currentCurve)
                                    }
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(mode.name.take(3), fontSize = 8.sp,
                                    color = if (selPoint.curveOut == mode) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    
                    // 删除按钮
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFD32F2F).copy(alpha = 0.15f))
                            .clickable {
                                val pt = currentCurve.points.getOrNull(selectedIndex) ?: return@clickable
                                undoManager.execute(DeletePointCommand(pt, selectedIndex), currentCurve)
                                selectedIndex = -1
                                selectedPresetName = customPresetLabel
                                applyCurve(currentCurve)
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\u5220\u9664\u6B64\u70B9", fontSize = 12.sp, color = Color(0xFFD32F2F))
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "\u70B9\u51FB\u66F2\u7EBF\u6DFB\u52A0\u63A7\u5236\u70B9 | \u62D6\u62FD\u79FB\u52A8 | \u9009\u62E9\u70B9\u540E\u7F16\u8F91",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 重置图标
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .clickable {
                            currentCurve = EqualizerCurve.defaultCurve()
                            selectedPresetName = flatPresetLabel
                            selectedIndex = -1
                            undoManager.clear()
                            applyCurve(currentCurve)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u21BA",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // 单曲/全局 切换
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (eqMode == EqMode.PER_TRACK) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { PresetStorage.setEqMode(EqMode.PER_TRACK) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.eq_mode_per_track),
                            fontSize = 11.sp,
                            fontWeight = if (eqMode == EqMode.PER_TRACK) FontWeight.Bold else FontWeight.Normal,
                            color = if (eqMode == EqMode.PER_TRACK) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (eqMode == EqMode.GLOBAL) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { PresetStorage.setEqMode(EqMode.GLOBAL) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.eq_mode_global),
                            fontSize = 11.sp,
                            fontWeight = if (eqMode == EqMode.GLOBAL) FontWeight.Bold else FontWeight.Normal,
                            color = if (eqMode == EqMode.GLOBAL) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                
                // Bypass图标
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (eqBypassed) Color(0xFFD32F2F) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .clickable {
                            eqBypassed = !eqBypassed
                            ConfigStorage.setEqBypassEnabled(eqBypassed)
                            if (soundId != null) {
                                if (eqBypassed) {
                                    OboeAudioEngine.setEqEnabled(soundId, false)
                                } else {
                                    applyCurve(currentCurve)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u23FB",
                        fontSize = 18.sp,
                        color = if (eqBypassed) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
    
    // 保存预设对话框
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.eq_save_preset)) },
            text = {
                OutlinedTextField(
                    value = savePresetName,
                    onValueChange = { savePresetName = it },
                    placeholder = { Text(stringResource(R.string.eq_save_preset_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (savePresetName.isNotBlank()) {
                            PresetStorage.savePreset(savePresetName.trim(), currentCurve)
                            selectedPresetName = savePresetName.trim()
                            showSaveDialog = false
                        }
                    },
                    enabled = savePresetName.isNotBlank()
                ) {
                    Text(stringResource(R.string.save_preset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // 预设选择弹窗
    if (showPresetDialog) {
        val allPresets by PresetStorage.presets.collectAsState()
        val builtInPresets = remember { PresetStorage.builtInPresets }
        val tabCount = pointCountTabs.size
        val selectedTabIndex = if (selectedPointTab == -1) tabCount
            else pointCountTabs.indexOf(selectedPointTab).coerceAtLeast(0)
        
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.presets),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 点数Tab切换
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        pointCountTabs.forEach { count ->
                            Tab(
                                selected = selectedPointTab == count,
                                onClick = { selectedPointTab = count },
                                text = {
                                    Text(
                                        text = "${count}点",
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedPointTab == count) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedPointTab == count) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            )
                        }
                        // 自定义Tab
                        Tab(
                            selected = selectedPointTab == -1,
                            onClick = { selectedPointTab = -1 },
                            text = {
                                Text(
                                    text = stringResource(R.string.eq_custom_presets),
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedPointTab == -1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedPointTab == -1) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 预设列表
                    if (selectedPointTab == -1) {
                        // 自定义预设
                        val customList = allPresets
                        if (customList.isEmpty()) {
                            Text(
                                text = stringResource(R.string.eq_no_custom_presets),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.height(300.dp)
                            ) {
                                items(customList, key = { it.id }) { preset ->
                                    PresetListItem(
                                        preset = preset,
                                        isSelected = selectedPresetName == preset.name,
                                        showDelete = true,
                                        onSelect = {
                                            currentCurve = preset.curve.copy(points = preset.curve.points.toMutableList())
                                            selectedPresetName = preset.name
                                            undoManager.clear()
                                            applyCurve(currentCurve)
                                            showPresetDialog = false
                                        },
                                        onDelete = {
                                            PresetStorage.deletePreset(preset.id)
                                            if (selectedPresetName == preset.name) {
                                                selectedPresetName = customPresetLabel
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // 内置预设（按点数过滤）+ 同点数自定义预设
                        val filteredPresets = builtInPresets.filter { it.curve.pointCount == selectedPointTab } +
                                              allPresets.filter { it.curve.pointCount == selectedPointTab }
                        LazyColumn(
                            modifier = Modifier.height(300.dp)
                        ) {
                            items(filteredPresets, key = { it.id }) { preset ->
                                PresetListItem(
                                    preset = preset,
                                    isSelected = selectedPresetName == preset.name,
                                    showDelete = !preset.isBuiltIn,
                                    onSelect = {
                                        currentCurve = preset.curve.copy(points = preset.curve.points.toMutableList())
                                        selectedPresetName = preset.name
                                        undoManager.clear()
                                        applyCurve(currentCurve)
                                        showPresetDialog = false
                                    },
                                    onDelete = if (!preset.isBuiltIn) ({
                                        PresetStorage.deletePreset(preset.id)
                                        if (selectedPresetName == preset.name) {
                                            selectedPresetName = customPresetLabel
                                        }
                                    }) else null
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
            modifier = Modifier.fillMaxWidth(0.95f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeakerCompensationPanel(soundId: String?) {
    var selectedPreset by remember { mutableStateOf(ConfigStorage.getSpeakerPreset()) }
    var intensity by remember { mutableFloatStateOf(ConfigStorage.getAutoEqIntensity()) }
    var bassBias by remember { mutableFloatStateOf(ConfigStorage.getAutoEqBassBias()) }
    var midBias by remember { mutableFloatStateOf(ConfigStorage.getAutoEqMidBias()) }
    var trebleBias by remember { mutableFloatStateOf(ConfigStorage.getAutoEqTrebleBias()) }
    var brightnessTarget by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProBrightnessTarget()) }
    var loudnessTarget by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProLoudnessTarget()) }
    var maxBoost by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProMaxBoost()) }
    var maxCut by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProMaxCut()) }
    var dynamicQEnabled by remember { mutableStateOf(ConfigStorage.getAutoEqProDynamicQEnabled()) }
    var presetMenuExpanded by remember { mutableStateOf(false) }
    // Pro parameters
    var attack by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProAttack()) }
    var release by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProRelease()) }
    var maxSlope by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProMaxSlope()) }
    var couplingCoeff by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProCouplingCoeff()) }
    var hysteresis by remember { mutableFloatStateOf(ConfigStorage.getAutoEqProHysteresisDb()) }
    // Band configuration
    val eqUnlimited by ConfigStorage.config.collectAsState()
    var bandCount by remember { mutableIntStateOf(ConfigStorage.getAutoEqBandCount()) }
    var lowRatio by remember { mutableFloatStateOf(ConfigStorage.getAutoEqLowRatio()) }
    var midRatio by remember { mutableFloatStateOf(ConfigStorage.getAutoEqMidRatio()) }
    var eqAutoMode by remember { mutableStateOf(ConfigStorage.config.value.autoEqMode) }

    // 分类预设：设备类 + 场景类。新增预设由 SpeakerPresetCurves（Kotlin）提供补偿曲线，
    // 原有 5 个预设（phone/earphone/bluetooth/car/flat）仍由 C++ 引擎内部计算。
    val devicePresets = listOf(
        stringResource(R.string.speaker_preset_phone),
        stringResource(R.string.speaker_preset_earphone),
        stringResource(R.string.speaker_preset_bluetooth),
        stringResource(R.string.speaker_preset_car),
        stringResource(R.string.speaker_preset_tablet),
        stringResource(R.string.speaker_preset_headphone),
        stringResource(R.string.speaker_preset_desktop),
        stringResource(R.string.speaker_preset_tv)
    )
    val scenePresets = listOf(
        stringResource(R.string.speaker_preset_cinema),
        stringResource(R.string.speaker_preset_night),
        stringResource(R.string.speaker_preset_outdoor),
        stringResource(R.string.speaker_preset_studio)
    )
    val flatPresetName = stringResource(R.string.speaker_preset_flat)

    // --- Per-filter editor state ---
    // Each entry: Pair(actualBandIndex, Triple<gainDb, freqHz, q>).
    // The list index != actualBandIndex when bandCount > 48 (stride is applied).
    val presetEnglish = remember(selectedPreset) { presetToEnglish(selectedPreset) }
    val filterBands = remember { mutableStateOf<List<Pair<Int, Triple<Float, Float, Float>>>>(emptyList()) }
    var filterVersion by remember { mutableIntStateOf(0) }

    // Reload filter values from the engine (auto-computed gains/freqs merged with
    // any saved overrides for the current preset), then push overrides to C++.
    LaunchedEffect(presetEnglish, bandCount, soundId, filterVersion) {
        if (soundId == null) return@LaunchedEffect
        val gains = OboeAudioEngine.getAutoEqGains(soundId)
        val freqs = OboeAudioEngine.getAutoEqFrequencies(soundId)
        if (gains.isEmpty() || freqs.isEmpty() || gains.size != freqs.size) return@LaunchedEffect

        // Start fresh: clear any previously-pushed C++ overrides for this track,
        // then re-apply saved overrides from ConfigStorage for the current preset.
        OboeAudioEngine.clearAllAutoEqFilterOverrides(soundId)

        // Match the C++ applyAutoEqToEq stride: at most 48 bands are pushed to the
        // BiQuad pool. Only those bands are effective, so only display those here.
        val kMaxEqFilterPoints = 48
        val stride = if (gains.size > kMaxEqFilterPoints) gains.size / kMaxEqFilterPoints else 1

        // Kotlin 预设（tablet/headphone/desktop/tv/cinema/night/outdoor/studio）
        // 由 SpeakerPresetCurves 提供补偿曲线增益，替代 C++ 引擎的 auto gain。
        val isKotlinPreset = SpeakerPresetCurves.isKotlinPreset(presetEnglish)
        val saved = ConfigStorage.getAutoEqFilterOverridesFor(presetEnglish)
        val list = ArrayList<Pair<Int, Triple<Float, Float, Float>>>()
        var i = 0
        while (i < gains.size) {
            val o = saved[i]
            if (o != null) {
                list.add(i to Triple(o.gainDb, o.frequencyHz, o.q))
                OboeAudioEngine.setAutoEqFilterOverride(soundId, i, o.gainDb, o.frequencyHz, o.q)
            } else {
                val baseGain = if (isKotlinPreset) SpeakerPresetCurves.gainForFreq(presetEnglish, freqs[i]) else gains[i]
                list.add(i to Triple(baseGain, freqs[i], 1.0f))
                // Kotlin 预设的曲线增益需推送到 C++ BiQuad 池（C++ 不内置这些预设）
                if (isKotlinPreset) {
                    OboeAudioEngine.setAutoEqFilterOverride(soundId, i, baseGain, freqs[i], 1.0f)
                }
            }
            i += stride
        }
        filterBands.value = list
    }

    // Push AutoEQ gains to BiQuad pool + optionally sync to manual
    val syncToManualEq: () -> Unit = sync@{
        if (soundId == null) return@sync
        val autoGains = OboeAudioEngine.getAutoEqGains(soundId)
        val autoFreqs = OboeAudioEngine.getAutoEqFrequencies(soundId)
        if (autoGains.isEmpty() || autoFreqs.isEmpty()) return@sync
        if (autoGains.size != autoFreqs.size) return@sync

        // Kotlin 预设的增益来自 SpeakerPresetCurves 曲线；用户 override 优先；
        // 其余用 C++ auto gain。
        val isKotlinPreset = SpeakerPresetCurves.isKotlinPreset(presetEnglish)
        val savedOverrides = ConfigStorage.getAutoEqFilterOverridesFor(presetEnglish)
        val pts = autoGains.mapIndexed { i, g ->
            val baseGain = when {
                savedOverrides[i] != null -> savedOverrides[i]!!.gainDb
                isKotlinPreset -> SpeakerPresetCurves.gainForFreq(presetEnglish, autoFreqs[i])
                else -> g
            }
            ControlPoint(autoFreqs[i], baseGain.coerceIn(-24f, 24f))
        }

        // 存在用户 override 或 Kotlin 预设时，C++ BiQuad 池已通过 setAutoEqFilterOverride
        // 推送了正确曲线，调用 setEqualizerCurve 会覆盖这些值，因此跳过。
        val hasOverrides = savedOverrides.isNotEmpty()
        if (!hasOverrides && !isKotlinPreset) {
            val freqs = FloatArray(pts.size) { pts[it].frequencyHz }
            val gainsArr = FloatArray(pts.size) { pts[it].gainDb }
            val types = IntArray(pts.size) { pts[it].filterType.nativeValue }
            val qs = FloatArray(pts.size) { pts[it].qOverride }
            val cIns = IntArray(pts.size) { pts[it].curveIn.nativeValue }
            val cOuts = IntArray(pts.size) { pts[it].curveOut.nativeValue }
            OboeAudioEngine.setEqualizerCurve(soundId, freqs, gainsArr, types, qs, cIns, cOuts)
        }

        if (ConfigStorage.isAutoEqSyncToManual()) {
            val autoCurve = com.bicy.whitenoise.equalizer.EqualizerCurve(points = pts.toMutableList(), name = "AutoEQ")
            PresetStorage.saveTrackCurve(soundId, autoCurve)
        }

        // Refresh the per-filter editor so non-overridden bands display new auto values
        filterVersion++
    }

    // Actually sync to native when band config changes
    fun syncBandConfig() {
        if (soundId != null) {
            OboeAudioEngine.setAutoEqBandCount(soundId, bandCount)
            OboeAudioEngine.setAutoEqBandRatios(soundId, lowRatio, midRatio)
            syncToManualEq()
        }
        ConfigStorage.setAutoEqBandCount(bandCount)
        ConfigStorage.setAutoEqLowRatio(lowRatio)
        ConfigStorage.setAutoEqMidRatio(midRatio)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // --- 滤波器编辑入口（上移至面板顶部，扬声器预设并入弹窗） ---
        var showFilterEditor by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                .clickable { showFilterEditor = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.auto_eq_filter_editor_title),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = selectedPreset,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AutoEqSectionTitle(stringResource(R.string.speaker_compensation_intensity))
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
                    if (soundId != null) {
                        OboeAudioEngine.setAutoEqIntensity(soundId, it)
                        syncToManualEq()
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

        AutoEqSectionTitle(stringResource(R.string.auto_eq_freq_bias))
        BiasSlider(stringResource(R.string.auto_eq_bass_bias_freq), bassBias) {
            bassBias = it
            ConfigStorage.setAutoEqBassBias(it)
            if (soundId != null) {
                OboeAudioEngine.setAutoEqBassBias(soundId, it)
                syncToManualEq()
            }
        }
        BiasSlider(stringResource(R.string.auto_eq_mid_bias_freq), midBias) {
            midBias = it
            ConfigStorage.setAutoEqMidBias(it)
            if (soundId != null) {
                OboeAudioEngine.setAutoEqMidBias(soundId, it)
                syncToManualEq()
            }
        }
        BiasSlider(stringResource(R.string.auto_eq_treble_bias_freq), trebleBias) {
            trebleBias = it
            ConfigStorage.setAutoEqTrebleBias(it)
            if (soundId != null) {
                OboeAudioEngine.setAutoEqTrebleBias(soundId, it)
                syncToManualEq()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Band Configuration ---
        AutoEqSectionTitle(stringResource(R.string.auto_eq_band_config))
        val maxBandCount = if (eqUnlimited.eqUnlimitedPoints) 256 else 16
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$bandCount", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = bandCount.toFloat(),
                onValueChange = {
                    bandCount = it.toInt()
                    syncBandConfig()
                },
                valueRange = 0f..maxBandCount.toFloat(),
                steps = maxBandCount - 1,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 三段比例连动滑块（低频/中频/高频）
        val highRatio = (1.0f - lowRatio - midRatio).coerceIn(0.20f, 0.60f)
        
        // 连动调整：修改一项时，按比例重分配剩余两项
        fun adjustLow(newValue: Float) {
            val newLow = newValue.coerceIn(0.20f, 0.60f)
            val remaining = (1.0f - newLow).coerceAtLeast(0.40f)
            val oldMidPlusHigh = midRatio + highRatio
            if (oldMidPlusHigh > 0.001f) {
                val newMid = (remaining * midRatio / oldMidPlusHigh).coerceIn(0.20f, remaining - 0.20f)
                lowRatio = newLow
                midRatio = newMid
            }
            syncBandConfig()
        }
        fun adjustMid(newValue: Float) {
            val newMid = newValue.coerceIn(0.20f, 0.60f)
            val remaining = (1.0f - newMid).coerceAtLeast(0.40f)
            val oldLowPlusHigh = lowRatio + highRatio
            if (oldLowPlusHigh > 0.001f) {
                val newLow = (remaining * lowRatio / oldLowPlusHigh).coerceIn(0.20f, remaining - 0.20f)
                midRatio = newMid
                lowRatio = newLow
            }
            syncBandConfig()
        }
        fun adjustHigh(newValue: Float) {
            val newHigh = newValue.coerceIn(0.20f, 0.60f)
            val remaining = (1.0f - newHigh).coerceAtLeast(0.40f)
            val oldLowPlusMid = lowRatio + midRatio
            if (oldLowPlusMid > 0.001f) {
                val newLow = (remaining * lowRatio / oldLowPlusMid).coerceIn(0.20f, remaining - 0.20f)
                val newMid = remaining - newLow
                lowRatio = newLow
                midRatio = newMid
            }
            syncBandConfig()
        }
        
        BandRatioRow(
            label = stringResource(R.string.auto_eq_low_band),
            value = lowRatio,
            color = Color(0xFF4CAF50),
            onValueChange = { adjustLow(it) }
        )
        BandRatioRow(
            label = stringResource(R.string.auto_eq_mid_band),
            value = midRatio,
            color = Color(0xFFFFC107),
            onValueChange = { adjustMid(it) }
        )
        BandRatioRow(
            label = stringResource(R.string.auto_eq_high_band),
            value = highRatio,
            color = Color(0xFF2196F3),
            onValueChange = { adjustHigh(it) }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${(lowRatio * 100).toInt()}%", fontSize = 10.sp, color = Color(0xFF4CAF50))
            Text("${(midRatio * 100).toInt()}%", fontSize = 10.sp, color = Color(0xFFFFC107))
            Text("${((1f - lowRatio - midRatio) * 100).toInt()}%", fontSize = 10.sp, color = Color(0xFF2196F3))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Pro: Attack/Release (replaces Smoothing) ---
        AutoEqSectionTitle(stringResource(R.string.auto_eq_attack_release))
        ParaSlider(stringResource(R.string.auto_eq_attack), attack, "ms", 1f..500f) {
            attack = it
            ConfigStorage.setAutoEqProAttack(it)
            if (soundId != null) OboeAudioEngine.setAutoEqAttack(soundId, it)
        }
        ParaSlider(stringResource(R.string.auto_eq_release), release, "ms", 10f..2000f) {
            release = it
            ConfigStorage.setAutoEqProRelease(it)
            if (soundId != null) OboeAudioEngine.setAutoEqRelease(soundId, it)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Pro: MaxSlope ---
        AutoEqSectionTitle(stringResource(R.string.auto_eq_curve_control))
        ParaSlider(stringResource(R.string.auto_eq_max_slope), maxSlope, "dB/oct", 1f..48f) {
            maxSlope = it
            ConfigStorage.setAutoEqProMaxSlope(it)
            if (soundId != null) {
                OboeAudioEngine.setAutoEqMaxSlope(soundId, it)
                syncToManualEq()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Frequency Limit ---
        AutoEqSectionTitle(stringResource(R.string.auto_eq_freq_limit))
        ParaSlider(stringResource(R.string.auto_eq_max_boost), maxBoost, "dB", 0f..24f) {
            maxBoost = it
            ConfigStorage.setAutoEqProMaxBoost(it)
            if (soundId != null) {
                OboeAudioEngine.setAutoEqMaxBoost(soundId, it)
                syncToManualEq()
            }
        }
        ParaSlider(stringResource(R.string.auto_eq_max_cut), maxCut, "dB", 0f..24f) {
            maxCut = it
            ConfigStorage.setAutoEqProMaxCut(it)
            if (soundId != null) {
                OboeAudioEngine.setAutoEqMaxCut(soundId, it)
                syncToManualEq()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Curve Fine-tune ---
        AutoEqSectionTitle(stringResource(R.string.auto_eq_curve_fine_tune))
        ParaSlider(stringResource(R.string.auto_eq_brightness_target), brightnessTarget, "dB/oct", -4.5f..4.5f) {
            brightnessTarget = it
            ConfigStorage.setAutoEqProBrightnessTarget(it)
            if (soundId != null) {
                OboeAudioEngine.setAutoEqBrightnessTarget(soundId, it)
                syncToManualEq()
            }
        }
        ParaSlider(stringResource(R.string.auto_eq_loudness_target), loudnessTarget, "dB", -6f..6f) {
            loudnessTarget = it
            ConfigStorage.setAutoEqProLoudnessTarget(it)
            if (soundId != null) {
                OboeAudioEngine.setAutoEqLoudnessTarget(soundId, it)
                syncToManualEq()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Pro: Coupling & Hysteresis ---
        AutoEqSectionTitle(stringResource(R.string.auto_eq_stability))
        ParaSlider(stringResource(R.string.auto_eq_coupling), couplingCoeff, "", 0f..1f) {
            couplingCoeff = it
            ConfigStorage.setAutoEqProCouplingCoeff(it)
            if (soundId != null) {
                OboeAudioEngine.setAutoEqCouplingCoeff(soundId, it)
                syncToManualEq()
            }
        }
        ParaSlider(stringResource(R.string.auto_eq_hysteresis), hysteresis, "dB", 0f..6f) {
            hysteresis = it
            ConfigStorage.setAutoEqProHysteresisDb(it)
            if (soundId != null) {
                OboeAudioEngine.setAutoEqHysteresis(soundId, it)
                syncToManualEq()
            }
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
                    if (soundId != null) {
                        OboeAudioEngine.setAutoEqDynamicQEnabled(soundId, dynamicQEnabled)
                        syncToManualEq()
                    }
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
                    if (soundId != null) {
                        OboeAudioEngine.setAutoEqDynamicQEnabled(soundId, it)
                        syncToManualEq()
                    }
                }
            )
        }

        if (showFilterEditor) {
            GlassAlertDialogSimple(
                onDismissRequest = { showFilterEditor = false }
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 扬声器预设选择（分类：设备 / 场景 / 平坦）
                    AutoEqSectionTitle(stringResource(R.string.speaker_preset_title))
                    val onPresetSelected: (String) -> Unit = { preset ->
                        selectedPreset = preset
                        presetMenuExpanded = false
                        ConfigStorage.setSpeakerPreset(preset)
                        val englishPreset = presetToEnglish(preset)
                        if (soundId != null) {
                            OboeAudioEngine.clearAllAutoEqFilterOverrides(soundId)
                            OboeAudioEngine.setSpeakerPreset(soundId, englishPreset)
                            syncToManualEq()
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .clickable { presetMenuExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedPreset,
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
                            expanded = presetMenuExpanded,
                            onDismissRequest = { presetMenuExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = stringResource(R.string.speaker_preset_category_device),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            devicePresets.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p, fontSize = 14.sp, color = if (selectedPreset == p) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                                    onClick = { onPresetSelected(p) }
                                )
                            }
                            Text(
                                text = stringResource(R.string.speaker_preset_category_scene),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            scenePresets.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p, fontSize = 14.sp, color = if (selectedPreset == p) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                                    onClick = { onPresetSelected(p) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(flatPresetName, fontSize = 14.sp, color = if (selectedPreset == flatPresetName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                                onClick = { onPresetSelected(flatPresetName) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AutoEqFilterEditorSection(
                        soundId = soundId,
                        presetEnglish = presetEnglish,
                        bands = filterBands.value,
                        onBandChange = { bandIndex, gainDb, freqHz, q ->
                            if (soundId == null) return@AutoEqFilterEditorSection
                            // 1. Push to C++ immediately (triggers applyAutoEqToEq → BiQuad pool update)
                            OboeAudioEngine.setAutoEqFilterOverride(soundId, bandIndex, gainDb, freqHz, q)
                            // 2. Persist per preset
                            ConfigStorage.setAutoEqFilterOverride(
                                presetEnglish, bandIndex,
                                com.bicy.whitenoise.storage.config.ConfigStoragePart.AutoEqFilterOverride(gainDb, freqHz, q)
                            )
                            // 3. Update local display state (find the entry with matching bandIndex)
                            val updated = filterBands.value.toMutableList()
                            val pos = updated.indexOfFirst { it.first == bandIndex }
                            if (pos >= 0) {
                                updated[pos] = bandIndex to Triple(gainDb, freqHz, q)
                                filterBands.value = updated
                            }
                        },
                        onResetAll = {
                            if (soundId == null) return@AutoEqFilterEditorSection
                            OboeAudioEngine.clearAllAutoEqFilterOverrides(soundId)
                            ConfigStorage.clearAutoEqFilterOverridesFor(presetEnglish)
                            filterVersion++
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showFilterEditor = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.finish), color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.speaker_compensation_desc),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            lineHeight = 16.sp
        )
    }
}

/**
 * 任务6：暴露全部滤波器参数（增益 / 频率 / Q 值）供用户编辑。
 *
 * 每个滤波器占一行，三个 InteractiveSlider 横向排列：
 *   - 增益 Gain: -12dB .. +12dB, 步长 0.1dB
 *   - 频率 Freq: 20Hz .. 20000Hz, 对数刻度
 *   - Q 值:     0.1 .. 10, 步长 0.1
 *
 * 修改任意参数立即通过 setAutoEqFilterOverride 推送到 C++ EQ 滤波器池，
 * 同时持久化到 ConfigStorage（按预设隔离）。
 *
 * 注意：滤波器数量受 AutoEqEngine 的 bandCount 控制，且 applyAutoEqToEq 内部
 * 限制最多 48 个频段点推入 BiQuad 池（防止 256 频段爆音），此处不突破该限制。
 */
@Composable
private fun AutoEqFilterEditorSection(
    soundId: String?,
    presetEnglish: String,
    bands: List<Pair<Int, Triple<Float, Float, Float>>>,
    onBandChange: (bandIndex: Int, gainDb: Float, freqHz: Float, q: Float) -> Unit,
    onResetAll: () -> Unit
) {
    AutoEqSectionTitle(stringResource(R.string.auto_eq_filter_editor_title))

    Text(
        text = stringResource(R.string.auto_eq_filter_editor_desc),
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        lineHeight = 15.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    if (soundId == null || bands.isEmpty()) {
        Text(
            text = "—",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        return
    }

    // 每个滤波器一行
    bands.forEachIndexed { listIndex, (bandIndex, values) ->
        val (gainDb, freqHz, q) = values
        AutoEqFilterRow(
            displayNumber = listIndex + 1,
            gainDb = gainDb,
            freqHz = freqHz,
            q = q,
            onGainChange = { onBandChange(bandIndex, it, freqHz, q) },
            onFreqChange = { onBandChange(bandIndex, gainDb, it, q) },
            onQChange = { onBandChange(bandIndex, gainDb, freqHz, it) }
        )
        if (listIndex < bands.lastIndex) {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 重置全部滤波器（清除当前预设的所有覆盖）
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onResetAll,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_eq_filter_reset_all),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun AutoEqFilterRow(
    displayNumber: Int,
    gainDb: Float,
    freqHz: Float,
    q: Float,
    onGainChange: (Float) -> Unit,
    onFreqChange: (Float) -> Unit,
    onQChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.auto_eq_filter_band, displayNumber),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "${"%.1f".format(gainDb)} dB  •  ${formatFreq(freqHz)}  •  Q ${"%.1f".format(q)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 增益 Gain: -12 .. +12 dB, 步长 0.1
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.auto_eq_filter_gain),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.width(36.dp)
            )
            InteractiveSlider(
                value = gainDb,
                onValueChange = { v ->
                    // 步长 0.1dB
                    val stepped = (v * 10f).toInt() / 10f
                    onGainChange(stepped)
                },
                valueRange = -12f..12f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        // 频率 Freq: 20 .. 20000 Hz, 对数刻度
        // 用 0..1 线性 slider 映射到对数频率
        val freqRatio = freqToLogRatio(freqHz)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.auto_eq_filter_freq),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.width(36.dp)
            )
            InteractiveSlider(
                value = freqRatio,
                onValueChange = { ratio ->
                    onFreqChange(logRatioToFreq(ratio))
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFC107),
                    activeTrackColor = Color(0xFFFFC107),
                    inactiveTrackColor = Color(0xFFFFC107).copy(alpha = 0.15f)
                )
            )
        }

        // Q 值: 0.1 .. 10, 步长 0.1
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.auto_eq_filter_q),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.width(36.dp)
            )
            InteractiveSlider(
                value = q,
                onValueChange = { v ->
                    // 步长 0.1
                    val stepped = (v * 10f).toInt() / 10f
                    onQChange(stepped)
                },
                valueRange = 0.1f..10f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2196F3),
                    activeTrackColor = Color(0xFF2196F3),
                    inactiveTrackColor = Color(0xFF2196F3).copy(alpha = 0.15f)
                )
            )
        }
    }
}

// 20Hz .. 20000Hz 对数刻度辅助
private val FILTER_FREQ_MIN_HZ = 20f
private val FILTER_FREQ_MAX_HZ = 20000f
private val FILTER_FREQ_MIN_LOG = ln(FILTER_FREQ_MIN_HZ)
private val FILTER_FREQ_MAX_LOG = ln(FILTER_FREQ_MAX_HZ)

private fun freqToLogRatio(freq: Float): Float {
    val clamped = freq.coerceIn(FILTER_FREQ_MIN_HZ, FILTER_FREQ_MAX_HZ)
    return ((ln(clamped) - FILTER_FREQ_MIN_LOG) / (FILTER_FREQ_MAX_LOG - FILTER_FREQ_MIN_LOG))
        .coerceIn(0f, 1f)
}

private fun logRatioToFreq(ratio: Float): Float {
    return exp(FILTER_FREQ_MIN_LOG + ratio.coerceIn(0f, 1f) * (FILTER_FREQ_MAX_LOG - FILTER_FREQ_MIN_LOG))
}

private fun formatFreq(freq: Float): String {
    return if (freq >= 1000f) {
        "${"%.1f".format(freq / 1000f)}k Hz"
    } else {
        "${freq.toInt()} Hz"
    }
}

private fun presetToEnglish(preset: String): String {
    return when {
        // “头戴耳机”包含“耳机”，必须先于 earphone 判断
        preset.contains("头戴", ignoreCase = true) || preset.contains("headphone", ignoreCase = true) -> "headphone"
        preset.contains("earphone", ignoreCase = true) || preset.contains("耳机") -> "earphone"
        // “phone”含于“headphone/earphone”，放后面
        preset.contains("手机", ignoreCase = true) || preset.contains("phone", ignoreCase = true) -> "phone"
        preset.contains("bluetooth", ignoreCase = true) || preset.contains("蓝牙") -> "bluetooth"
        preset.contains("car", ignoreCase = true) || preset.contains("车载") -> "car"
        preset.contains("tablet", ignoreCase = true) || preset.contains("平板") -> "tablet"
        preset.contains("desktop", ignoreCase = true) || preset.contains("桌面") -> "desktop"
        preset.contains("tv", ignoreCase = true) || preset.contains("电视") -> "tv"
        preset.contains("cinema", ignoreCase = true) || preset.contains("影院") -> "cinema"
        preset.contains("night", ignoreCase = true) || preset.contains("夜间") -> "night"
        preset.contains("outdoor", ignoreCase = true) || preset.contains("户外") -> "outdoor"
        preset.contains("studio", ignoreCase = true) || preset.contains("录音棚") -> "studio"
        preset.contains("flat", ignoreCase = true) || preset.contains("平坦") -> "flat"
        else -> "phone"
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
private fun BandRatioRow(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = color,
            modifier = Modifier.width(56.dp)
        )
        InteractiveSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.20f..0.60f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.15f)
            )
        )
        Text(
            text = "${(value * 100).toInt()}%",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(36.dp),
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
    val speedAdjustExpanded = expandedSection == "speedAdjust"
    
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
    var stereoWidenerIntensity by remember { mutableFloatStateOf(savedEffects.stereoWidener) }
    var virtualBassIntensity by remember { mutableFloatStateOf(savedEffects.virtualBass) }
    var multibandCompressorIntensity by remember { mutableFloatStateOf(savedEffects.multibandCompressor) }
    
    var musicVolume by remember { mutableFloatStateOf(MusicStorage.getVolume()) }

    var pitchValue by remember { mutableFloatStateOf(MusicStorage.getPitch()) }
    var speedValue by remember { mutableFloatStateOf(MusicStorage.getSpeed()) }

    // 速度/音调持久化 debounce：拖动时只更新内存 state + 实时引擎，
    // 停止拖动 300ms 后才写磁盘，避免每次像素级拖动都触发磁盘 IO 导致卡顿
    LaunchedEffect(speedValue) {
        delay(300)
        MusicStorage.updateEffectIntensity("speed", speedValue)
    }
    LaunchedEffect(pitchValue) {
        delay(300)
        MusicStorage.updateEffectIntensity("pitch", pitchValue)
    }
    
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
            Spacer(modifier = Modifier.height(8.dp))
            EffectSliderItem(
                name = "立体声展宽",
                intensity = stereoWidenerIntensity,
                valueRange = 0f..3f,
                onIntensityChange = { 
                    stereoWidenerIntensity = it
                    MusicStorage.updateEffectIntensity("stereoWidener", it)
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.StereoWidener, it)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            EffectSliderItem(
                name = "虚拟低频",
                intensity = virtualBassIntensity,
                valueRange = 0f..3f,
                onIntensityChange = { 
                    virtualBassIntensity = it
                    MusicStorage.updateEffectIntensity("virtualBass", it)
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.VirtualBass, it)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            EffectSliderItem(
                name = "多段压缩",
                intensity = multibandCompressorIntensity,
                valueRange = 0f..3f,
                onIntensityChange = { 
                    multibandCompressorIntensity = it
                    MusicStorage.updateEffectIntensity("multibandCompressor", it)
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.MultibandCompressor, it)
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
                    valueRange = 0.25f..10f,
                    valueText = String.format("%.2f秒/圈", obrSurroundSpeed),
                    steps = 38,
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

        // ===== 速度与音调调整 =====
        // SoundTouch 实时 time-stretch + pitch-shift，速率与音调独立调节
        CollapsibleSection(
            title = stringResource(R.string.speed_adjust_title),
            subtitle = "SoundTouch",
            expanded = speedAdjustExpanded,
            onToggle = {
                expandedSection = if (speedAdjustExpanded) null else "speedAdjust"
            }
        ) {
            PitchSliderItem(
                value = pitchValue,
                onValueChange = {
                    pitchValue = it
                    // 实时引擎调用保留（C++ 层无锁 atomic，开销极小）；
                    // 磁盘持久化改为 LaunchedEffect debounce，避免拖动时频繁写磁盘导致卡顿
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setPitchShift(soundId, it)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SpeedSliderItem(
                value = speedValue,
                onValueChange = {
                    speedValue = it
                    val track = MusicPlayerController.currentTrack
                    if (track != null) {
                        val soundId = MusicCacheManager.getSoundId(track.id)
                        OboeAudioEngine.setPlaybackSpeed(soundId, it)
                    }
                }
            )
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
                    stereoWidenerIntensity = 0.5f
                    virtualBassIntensity = 0.2f
                    multibandCompressorIntensity = 0.5f
                    
                    musicVolume = 1f
                    pitchValue = 0f
                    speedValue = 1f
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
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.StereoWidener, 0.5f)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.VirtualBass, 0.2f)
                        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.MultibandCompressor, 0.5f)
                        OboeAudioEngine.setPitchShift(soundId, 0f)
                        OboeAudioEngine.setPlaybackSpeed(soundId, 1f)
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
    onIntensityChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
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
            
            val displayPercent = if (valueRange.endInclusive > 1.0f) {
                String.format("%.0f%%", intensity * 100f / valueRange.endInclusive)
            } else {
                String.format("%.0f%%", intensity * 100)
            }
            Text(
                text = displayPercent,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        InteractiveSlider(
            value = intensity,
            onValueChange = onIntensityChange,
            valueRange = valueRange,
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
            valueRange = 0.1f..5f,
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
        }
    }
}

/**
 * 预设列表项组件（用于弹窗内的纵向列表）
 */
@Composable
private fun PresetListItem(
    preset: EqualizerPreset,
    isSelected: Boolean,
    showDelete: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.name,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = preset.curve.pointCountLabel,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (showDelete && onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

