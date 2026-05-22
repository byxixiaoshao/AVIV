package com.bicy.whitenoise.yODW.SrEO.WIXN

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bicy.whitenoise.oJft.TimerManager
import com.bicy.whitenoise.yODW.ZFNn.ShadowConfig
import com.bicy.whitenoise.yODW.ZFNn.dropShadow
import com.bicy.whitenoise.yODW.ZFNn.isLight
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun TimerCircleContent(
    hours: Int,
    minutes: Int,
    fillProgress: Float,
    showFillProgress: Boolean,
    setupCircleAlpha: Float,
    timerAlpha: Float,
    h: Float,
    m: Float,
    s: Float
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    
    var isPressed by remember { mutableStateOf(false) }
    var isCharging by remember { mutableStateOf(false) }
    var hasStopped by remember { mutableStateOf(false) }
    var chargeValue by remember { mutableFloatStateOf(0f) }
    var visualProgress by remember { mutableFloatStateOf(0f) }
    var fadeOutProgress by remember { mutableFloatStateOf(1f) }
    
    LaunchedEffect(isPressed) {
        if (isPressed && timerAlpha > 0f) {
            isCharging = true
            hasStopped = false
            fadeOutProgress = 1f
            
            val totalChargeTime = 2000L
            val startTime = System.currentTimeMillis()
            var lastVibrateTime = 0L
            
            while (isPressed && chargeValue < 100f) {
                delay(16)
                val elapsed = System.currentTimeMillis() - startTime
                chargeValue = (elapsed.toFloat() / totalChargeTime * 100f).coerceAtMost(100f)
                visualProgress = chargeValue / 100f
                
                if (vibrator?.hasVibrator() == true) {
                    val minInterval = 50L
                    val maxInterval = 500L
                    val interval = (maxInterval - (chargeValue / 100f) * (maxInterval - minInterval)).toLong()
                    
                    if (elapsed - lastVibrateTime >= interval) {
                        lastVibrateTime = elapsed
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val amplitude = (250 - chargeValue / 100f * 200).toInt().coerceIn(1, 255)
                            val effect = VibrationEffect.createOneShot(20, amplitude)
                            vibrator.vibrate(effect)
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(20)
                        }
                    }
                }
                
                if (chargeValue >= 100f) {
                    hasStopped = true
                    TimerManager.stopTimer()
                    break
                }
            }
            
            isCharging = false
        }
    }
    
    LaunchedEffect(isPressed, isCharging) {
        if (!isPressed && !isCharging && chargeValue > 0f && !hasStopped) {
            val dischargeSpeed = 100f / 500f
            
            while (chargeValue > 0f && !isPressed) {
                delay(16)
                chargeValue = (chargeValue - dischargeSpeed * 16f).coerceAtLeast(0f)
                visualProgress = chargeValue / 100f
            }
        }
    }
    
    LaunchedEffect(hasStopped) {
        if (hasStopped) {
            while (fadeOutProgress > 0f) {
                delay(16)
                fadeOutProgress = (fadeOutProgress - 0.05f).coerceAtLeast(0f)
            }
            chargeValue = 0f
            visualProgress = 0f
            hasStopped = false
        }
    }
    
    val bubbleCount = 12
    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")
    
    val bubblePhases = remember { List(bubbleCount) { it.toFloat() / bubbleCount } }
    
    val bubbleAnimProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (showFillProgress && fillProgress > 0f) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbleProgress"
    )
    
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (showFillProgress && fillProgress > 0f) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val isLight = MaterialTheme.colorScheme.isLight()
    val chargeOverlayColor = if (isLight) {
        Color.White.copy(alpha = 0.25f)
    } else {
        Color(0xFF6B7B8C).copy(alpha = 0.25f)
    }
    
    Box(
        modifier = Modifier
            .size(180.dp)
            .dropShadow(
                config = ShadowConfig.Medium,
                shape = CircleShape,
                clip = false
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f * maxOf(setupCircleAlpha, timerAlpha)))
            .then(
                if (timerAlpha > 0f) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showFillProgress && fillProgress > 0f) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                if (size.width.isNaN() || size.height.isNaN() || size.width <= 0f || size.height <= 0f) {
                    return@Canvas
                }
                
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.width / 2
                val waveAmplitude = 8.dp.toPx()
                
                val fillHeight = size.height * fillProgress.coerceIn(0f, 1f)
                val waveY = size.height - fillHeight
                
                val wavePath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, waveY)
                    
                    var x = 0f
                    val step = 5f
                    while (x <= size.width) {
                        val y = waveY + sin((x / size.width * 4 * PI) + Math.toRadians(wavePhase.toDouble())).toFloat() * waveAmplitude
                        if (!y.isNaN()) {
                            lineTo(x, y)
                        }
                        x += step
                    }
                    
                    lineTo(size.width, size.height)
                    close()
                }
                
                drawPath(
                    path = wavePath,
                    color = primaryColor.copy(alpha = 0.3f * timerAlpha)
                )
                
                for (i in 0 until bubbleCount) {
                    val phase = bubblePhases[i]
                    val progress = (bubbleAnimProgress + phase) % 1f
                    
                    val angle = (i * 360f / bubbleCount + bubbleAnimProgress * 180f) * PI / 180f
                    val startRadius = radius * 1.5f
                    val endRadius = radius * 0.3f
                    
                    val currentRadius = startRadius - (startRadius - endRadius) * progress
                    val bubbleX = centerX + kotlin.math.cos(angle).toFloat() * currentRadius
                    val bubbleY = centerY + kotlin.math.sin(angle).toFloat() * currentRadius
                    
                    if (bubbleX.isNaN() || bubbleY.isNaN()) continue
                    
                    val bubbleSize = (4.dp.toPx() * (1f - progress * 0.5f)).coerceAtLeast(2.dp.toPx())
                    val bubbleAlpha = (1f - progress) * timerAlpha * 0.6f
                    
                    drawCircle(
                        color = primaryColor.copy(alpha = bubbleAlpha),
                        radius = bubbleSize,
                        center = Offset(bubbleX, bubbleY)
                    )
                }
                
                if (visualProgress > 0f && fadeOutProgress > 0f) {
                    val layerCount = 4
                    val layerDuration = 1f / layerCount
                    
                    for (layer in 0 until layerCount) {
                        val layerStart = layer * layerDuration
                        val layerEnd = (layer + 1) * layerDuration
                        
                        if (visualProgress > layerStart) {
                            val layerProgress = if (visualProgress >= layerEnd) {
                                1f
                            } else {
                                (visualProgress - layerStart) / layerDuration
                            }
                            
                            val layerRadius = radius * layerProgress
                            val layerAlpha = 0.25f * fadeOutProgress
                            
                            drawCircle(
                                color = chargeOverlayColor.copy(alpha = layerAlpha),
                                radius = layerRadius,
                                center = Offset(centerX, centerY)
                            )
                        }
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (setupCircleAlpha > 0f) {
                Text(
                    text = String.format("%02d:%02d", hours, minutes),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = setupCircleAlpha)
                )
            }
            
            if (timerAlpha > 0f) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = timerAlpha),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (h > 0) {
                            RollingNumber(
                                value = h.toInt(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = timerAlpha)
                            )
                            Text(
                                text = ":",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = timerAlpha)
                            )
                        }
                        RollingNumber(
                            value = m.toInt(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = timerAlpha)
                        )
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = timerAlpha)
                        )
                        RollingNumber(
                            value = s.toInt(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = timerAlpha)
                        )
                    }
                }
            }
        }
    }
}
