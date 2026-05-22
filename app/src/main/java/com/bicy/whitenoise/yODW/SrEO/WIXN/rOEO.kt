package com.bicy.whitenoise.yODW.SrEO.WIXN

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.bicy.whitenoise.yODW.SrEO.InteractiveSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bicy.whitenoise.R
import com.bicy.whitenoise.oJft.PauseType
import com.bicy.whitenoise.oJft.TimerManager
import com.bicy.whitenoise.yODW.ZFNn.InsetShadowConfig
import com.bicy.whitenoise.yODW.ZFNn.ShadowConfig
import com.bicy.whitenoise.yODW.ZFNn.advancedInsetShadow
import com.bicy.whitenoise.yODW.ZFNn.dropShadow
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
    var currentAnimMinutes by remember { mutableIntStateOf(hours * 60 + minutes) }
    
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
    
    fun animateToPreset(targetMinutes: Int) {
        val currentTotalMinutes = hours * 60 + minutes
        if (currentTotalMinutes == targetMinutes) return
        
        val targetHours = targetMinutes / 60
        val targetMins = targetMinutes % 60
        
        animatingToTarget = targetMinutes
        currentAnimMinutes = currentTotalMinutes
        
        val totalAnimDuration = 300L
        val minStepDelay = 16L
        val maxSteps = (totalAnimDuration / minStepDelay).toInt()
        
        if (hours > 0 && targetHours == 0) {
            val hourSteps = kotlin.math.min(hours, maxSteps / 2)
            val minuteSteps = kotlin.math.min(kotlin.math.abs(targetMins - minutes), maxSteps / 2)
            val totalSteps = hourSteps + minuteSteps
            
            if (totalSteps == 0) {
                TimerManager.setTime(targetMinutes)
                animatingToTarget = -1
                return
            }
            
            val stepDelay = totalAnimDuration / totalSteps.coerceAtLeast(1)
            
            scope.launch {
                var currentH = hours
                var currentM = minutes
                
                for (i in 1..hourSteps) {
                    currentH -= 1
                    TimerManager.setTime(currentH, currentM)
                    
                    vibrator?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val effect = VibrationEffect.createOneShot(10, 60)
                            it.vibrate(effect)
                        } else {
                            @Suppress("DEPRECATION")
                            it.vibrate(10)
                        }
                    }
                    
                    delay(stepDelay)
                }
                
                if (minuteSteps > 0) {
                    val minuteStep = if (targetMins > currentM) 1 else -1
                    for (i in 1..minuteSteps) {
                        currentM += minuteStep
                        TimerManager.setTime(0, currentM)
                        
                        vibrator?.let {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val effect = VibrationEffect.createOneShot(10, 60)
                                it.vibrate(effect)
                            } else {
                                @Suppress("DEPRECATION")
                                it.vibrate(10)
                            }
                        }
                        
                        delay(stepDelay)
                    }
                }
                
                TimerManager.setTime(targetMinutes)
                animatingToTarget = -1
            }
        } else {
            val steps = kotlin.math.min(kotlin.math.abs(targetMinutes - currentTotalMinutes), maxSteps)
            
            if (steps == 0) {
                TimerManager.setTime(targetMinutes)
                animatingToTarget = -1
                return
            }
            
            val stepDelay = totalAnimDuration / steps
            val stepSize = (targetMinutes - currentTotalMinutes) / steps
            val remainder = (targetMinutes - currentTotalMinutes) % steps
            
            scope.launch {
                var accumulatedRemainder = 0
                for (i in 1..steps) {
                    var step = stepSize
                    accumulatedRemainder += remainder
                    if (kotlin.math.abs(accumulatedRemainder) >= steps) {
                        step += if (accumulatedRemainder > 0) 1 else -1
                        accumulatedRemainder -= if (accumulatedRemainder > 0) steps else -steps
                    }
                    
                    currentAnimMinutes += step
                    TimerManager.setTime(currentAnimMinutes)
                    
                    vibrator?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val effect = VibrationEffect.createOneShot(10, 60)
                            it.vibrate(effect)
                        } else {
                            @Suppress("DEPRECATION")
                            it.vibrate(10)
                        }
                    }
                    
                    delay(stepDelay)
                }
                
                TimerManager.setTime(targetMinutes)
                animatingToTarget = -1
            }
        }
    }
    
    fun animateToHourPreset(targetHours: Int) {
        val currentHours = hours
        if (currentHours == targetHours && minutes == 0) return
        
        val targetTotalMinutes = targetHours * 60
        val currentTotalMinutes = hours * 60 + minutes
        
        animatingToTarget = targetTotalMinutes
        currentAnimMinutes = currentTotalMinutes
        
        val totalAnimDuration = 300L
        val minStepDelay = 16L
        val maxSteps = (totalAnimDuration / minStepDelay).toInt()
        
        if (minutes > 0) {
            val hourSteps = kotlin.math.min(kotlin.math.abs(targetHours - hours), maxSteps / 2)
            val minuteSteps = kotlin.math.min(minutes, maxSteps / 2)
            val totalSteps = hourSteps + minuteSteps
            
            if (totalSteps == 0) {
                TimerManager.setTime(targetHours, 0)
                animatingToTarget = -1
                return
            }
            
            val stepDelay = totalAnimDuration / totalSteps.coerceAtLeast(1)
            
            scope.launch {
                var currentH = hours
                var currentM = minutes
                
                for (i in 1..hourSteps) {
                    currentH += if (targetHours > hours) 1 else -1
                    TimerManager.setTime(currentH, currentM)
                    
                    vibrator?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val effect = VibrationEffect.createOneShot(10, 60)
                            it.vibrate(effect)
                        } else {
                            @Suppress("DEPRECATION")
                            it.vibrate(10)
                        }
                    }
                    
                    delay(stepDelay)
                }
                
                if (minuteSteps > 0) {
                    for (i in 1..minuteSteps) {
                        currentM -= 1
                        TimerManager.setTime(currentH, currentM)
                        
                        vibrator?.let {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val effect = VibrationEffect.createOneShot(10, 60)
                                it.vibrate(effect)
                            } else {
                                @Suppress("DEPRECATION")
                                it.vibrate(10)
                            }
                        }
                        
                        delay(stepDelay)
                    }
                }
                
                TimerManager.setTime(targetHours, 0)
                animatingToTarget = -1
            }
        } else {
            val steps = kotlin.math.min(kotlin.math.abs(targetHours - currentHours), maxSteps)
            
            if (steps == 0) {
                TimerManager.setTime(targetHours, 0)
                animatingToTarget = -1
                return
            }
            
            val stepDelay = totalAnimDuration / steps
            
            scope.launch {
                var currentH = currentHours
                val hourStep = if (targetHours > currentHours) 1 else -1
                
                for (i in 1..steps) {
                    currentH += hourStep
                    TimerManager.setTime(currentH, 0)
                    
                    vibrator?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val effect = VibrationEffect.createOneShot(10, 60)
                            it.vibrate(effect)
                        } else {
                            @Suppress("DEPRECATION")
                            it.vibrate(10)
                        }
                    }
                    
                    delay(stepDelay)
                }
                
                TimerManager.setTime(targetHours, 0)
                animatingToTarget = -1
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
    
    var lastHours by remember { mutableIntStateOf(hours) }
    var lastMinutes by remember { mutableIntStateOf(minutes) }
    var lastVibrateTime by remember { mutableLongStateOf(0L) }
    
    fun performSliderVibrate() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastVibrateTime > 50) {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(25, 150)
                    it.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(25)
                }
            }
            lastVibrateTime = currentTime
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.hours),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * textAlpha)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$hours",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                modifier = Modifier.width(40.dp)
            )
            InteractiveSlider(
                value = hours.toFloat(),
                onValueChange = { h -> 
                    val newHours = h.toInt()
                    if (newHours != lastHours) {
                        performSliderVibrate()
                        lastHours = newHours
                    }
                    TimerManager.setTime(newHours, minutes) 
                },
                valueRange = 0f..23f,
                steps = 23,
                modifier = Modifier.weight(1f),
                enabled = isEnabled
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.minutes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * textAlpha)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$minutes",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                modifier = Modifier.width(40.dp)
            )
            InteractiveSlider(
                value = minutes.toFloat(),
                onValueChange = { m -> 
                    val newMinutes = m.toInt()
                    if (newMinutes != lastMinutes) {
                        performSliderVibrate()
                        lastMinutes = newMinutes
                    }
                    TimerManager.setTime(hours, newMinutes) 
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
fun StartButton(
    onStart: () -> Unit,
    textAlpha: Float,
    isEnabled: Boolean = true
) {
    Button(
        onClick = onStart,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.start_timer),
            style = MaterialTheme.typography.titleMedium
        )
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
