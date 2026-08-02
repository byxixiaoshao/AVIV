package com.bicy.whitenoise.ui.components.ExpandableNavBarPart

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.bicy.whitenoise.ui.components.InteractiveSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bicy.whitenoise.R
import com.bicy.whitenoise.timer.PauseType
import com.bicy.whitenoise.timer.TimerManager
import com.bicy.whitenoise.ui.theme.InsetShadowConfig
import com.bicy.whitenoise.ui.theme.ShadowConfig
import com.bicy.whitenoise.ui.theme.advancedInsetShadow
import com.bicy.whitenoise.ui.theme.dropShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PresetButtonsContent(
    hours: Int,
    minutes: Int,
    textAlpha: Float,
    isEnabled: Boolean = true
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val scope = rememberCoroutineScope()
    
    var animatingToTarget by remember { mutableIntStateOf(-1) }
    
    val minutePresets = listOf(
        15 to stringResource(R.string.timer_15_min),
        30 to stringResource(R.string.timer_30_min),
        45 to stringResource(R.string.timer_45_min)
    )
    
    val hourPresets = listOf(
        1 to stringResource(R.string.timer_1_hour),
        2 to stringResource(R.string.timer_2_hour),
        3 to stringResource(R.string.timer_3_hour)
    )
    
    fun easeOutCubic(t: Float): Float {
        val xPos = 1f - t
        return 1f - xPos * xPos * xPos
    }
    
    fun performVibrate() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(12, 110)
                it.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(12)
            }
        }
    }

    // 到达预设目标时的确认震动（强）
    fun performConfirmVibrate() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(45, 230)
                it.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(45)
            }
        }
    }
    
    fun animateToPreset(targetMinutes: Int) {
        val targetHours = targetMinutes / 60
        val targetMins = targetMinutes % 60
        
        if (hours == targetHours && minutes == targetMins) return
        
        animatingToTarget = targetMinutes
        
        scope.launch {
            val totalAnimDuration = 400L
            val frameDelay = 16L
            val startTime = System.currentTimeMillis()
            var lastVibrateTime = 0L
            
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= totalAnimDuration) {
                    TimerManager.setTime(targetHours, targetMins)
                    animatingToTarget = -1
                    performConfirmVibrate()
                    break
                }
                
                val rawProgress = elapsed.toFloat() / totalAnimDuration
                val easedProgress = easeOutCubic(rawProgress)
                
                val currentH = (hours + (targetHours - hours) * easedProgress).toInt()
                val currentM = (minutes + (targetMins - minutes) * easedProgress).toInt()
                TimerManager.setTime(currentH, currentM)
                
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastVibrateTime > 30) {
                    performVibrate()
                    lastVibrateTime = currentTime
                }
                
                delay(frameDelay)
            }
        }
    }
    
    fun animateToHourPreset(targetHours: Int) {
        if (hours == targetHours && minutes == 0) return
        
        animatingToTarget = targetHours * 60
        
        scope.launch {
            val totalAnimDuration = 400L
            val frameDelay = 16L
            val startTime = System.currentTimeMillis()
            var lastVibrateTime = 0L
            
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= totalAnimDuration) {
                    TimerManager.setTime(targetHours, 0)
                    animatingToTarget = -1
                    performConfirmVibrate()
                    break
                }
                
                val rawProgress = elapsed.toFloat() / totalAnimDuration
                val easedProgress = easeOutCubic(rawProgress)
                
                val currentH = (hours + (targetHours - hours) * easedProgress).toInt()
                val currentM = (minutes + (0 - minutes) * easedProgress).toInt()
                TimerManager.setTime(currentH, currentM)
                
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastVibrateTime > 30) {
                    performVibrate()
                    lastVibrateTime = currentTime
                }
                
                delay(frameDelay)
            }
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            minutePresets.forEach { (mins, label) ->
                PresetButton(
                    label = label,
                    onClick = { animateToPreset(mins) },
                    isSelected = hours * 60 + minutes == mins,
                    textAlpha = textAlpha,
                    isEnabled = isEnabled
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hourPresets.forEach { (h, label) ->
                PresetButton(
                    label = label,
                    onClick = { animateToHourPreset(h) },
                    isSelected = hours == h && minutes == 0,
                    textAlpha = textAlpha,
                    isEnabled = isEnabled
                )
            }
        }
    }
}

@Composable
fun TimeSlidersContent(
    hours: Int,
    minutes: Int,
    textAlpha: Float,
    isEnabled: Boolean = true
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    val timerState by TimerManager.timerState.collectAsState()
    val currentHours = timerState.hours
    val currentMinutes = timerState.minutes

    // 记录上次震动时的值，只有值真正变化时才震动
    var lastVibratedHours by remember { mutableIntStateOf(-1) }
    var lastVibratedMinutes by remember { mutableIntStateOf(-1) }

    // 分级震动：0=轻刻度, 1=中刻度(5分钟倍数), 2=强刻度(15分钟倍数/小时)
    fun performStepVibrate(level: Int = 0) {
        vibrator?.let {
            val (duration, amplitude) = when (level) {
                2 -> 50L to 255
                1 -> 35L to 200
                else -> 18L to 120
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(duration, amplitude.coerceIn(1, 255))
                it.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(duration)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 小时行：标题 + 数字 + 滑块 并排
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.hours_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * textAlpha),
                modifier = Modifier.width(28.dp)
            )
            Text(
                text = "$currentHours",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                modifier = Modifier.width(36.dp)
            )
            InteractiveSlider(
                value = currentHours.toFloat(),
                onValueChange = { h ->
                    val newHours = h.toInt()
                    if (newHours != lastVibratedHours) {
                        // 小时每一步都是重要刻度 → 强震动
                        performStepVibrate(2)
                        lastVibratedHours = newHours
                    }
                    TimerManager.setHours(newHours)
                },
                valueRange = 0f..23f,
                steps = 23,
                modifier = Modifier.weight(1f),
                enabled = isEnabled
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 分钟行：标题 + 数字 + 滑块 并排
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.minutes_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * textAlpha),
                modifier = Modifier.width(28.dp)
            )
            Text(
                text = "$currentMinutes",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                modifier = Modifier.width(36.dp)
            )
            InteractiveSlider(
                value = currentMinutes.toFloat(),
                onValueChange = { m ->
                    val newMinutes = m.toInt()
                    if (newMinutes != lastVibratedMinutes) {
                        // 15分钟倍数→强, 5分钟倍数→中, 其余→轻
                        val level = when {
                            newMinutes % 15 == 0 -> 2
                            newMinutes % 5 == 0 -> 1
                            else -> 0
                        }
                        performStepVibrate(level)
                        lastVibratedMinutes = newMinutes
                    }
                    TimerManager.setMinutes(newMinutes)
                },
                valueRange = 0f..59f,
                steps = 59,
                modifier = Modifier.weight(1f),
                enabled = isEnabled
            )
        }
    }
}

@Composable
fun RowScope.PresetButton(
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    textAlpha: Float,
    isEnabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .then(
                if (isPressed) {
                    Modifier.advancedInsetShadow(
                        config = InsetShadowConfig.Default,
                        cornerRadius = 20.dp,
                        clip = false
                    )
                } else {
                    Modifier.dropShadow(
                        config = ShadowConfig.Light,
                        shape = RoundedCornerShape(20.dp),
                        clip = false
                    )
                }
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = textAlpha)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = textAlpha)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color.White.copy(alpha = textAlpha) 
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha)
        )
    }
}

@Composable
fun TimerSettingsContent(
    pauseType: PauseType,
    snoozeMinutes: Int,
    ringEnabled: Boolean,
    textAlpha: Float,
    isEnabled: Boolean = true
) {
    var showPauseTypeMenu by remember { mutableStateOf(false) }
    var showSnoozeMenu by remember { mutableStateOf(false) }
    
    @Composable
    fun getPauseTypeLabel(type: PauseType): String = when(type) {
        PauseType.ALL -> stringResource(R.string.global)
        PauseType.MUSIC_ONLY -> stringResource(R.string.music_only)
        PauseType.WHITE_NOISE_ONLY -> stringResource(R.string.white_noise_only)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.pause_type),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * textAlpha)
            )
            
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = textAlpha))
                        .clickable(enabled = isEnabled) { showPauseTypeMenu = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = getPauseTypeLabel(pauseType),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                    )
                }
                
                DropdownMenu(
                    expanded = showPauseTypeMenu,
                    onDismissRequest = { showPauseTypeMenu = false }
                ) {
                    PauseType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(getPauseTypeLabel(type)) },
                            onClick = {
                                TimerManager.setPauseType(type)
                                showPauseTypeMenu = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.snooze_time),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * textAlpha)
            )
            
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = textAlpha))
                        .clickable(enabled = isEnabled) { showSnoozeMenu = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${snoozeMinutes}${stringResource(R.string.timer_15_min).replace("15", "")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                    )
                }
                
                DropdownMenu(
                    expanded = showSnoozeMenu,
                    onDismissRequest = { showSnoozeMenu = false }
                ) {
                    listOf(1, 3, 5, 10, 15, 20, 30).forEach { mins ->
                        DropdownMenuItem(
                            text = { Text("${mins}${stringResource(R.string.timer_15_min).replace("15", "")}") },
                            onClick = {
                                TimerManager.setSnoozeMinutes(mins)
                                showSnoozeMenu = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.timer_end_ring),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * textAlpha)
            )
            
            Switch(
                checked = ringEnabled,
                onCheckedChange = { TimerManager.setRingEnabled(it) },
                enabled = isEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}
