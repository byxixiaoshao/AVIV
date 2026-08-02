package com.bicy.whitenoise.ui.components.ExpandableNavBarPart

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.bicy.whitenoise.timer.TimerManager
import com.bicy.whitenoise.ui.components.toast.ToastManager
import com.bicy.whitenoise.ui.theme.ShadowConfig
import com.bicy.whitenoise.ui.theme.dropShadow
import com.bicy.whitenoise.ui.theme.isLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class BouncingBubble(
    val id: Long,
    val angle: Float,       // 边缘生成角度
    val size: Float,        // dp
    var progress: Float,    // 0→1，边缘→中心
    var alpha: Float        // 淡入透明度
)

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
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    var isPressed by remember { mutableStateOf(false) }
    var hasStopped by remember { mutableStateOf(false) }
    var chargeValue by remember { mutableFloatStateOf(0f) }
    var visualProgress by remember { mutableFloatStateOf(0f) }
    var fadeOutProgress by remember { mutableFloatStateOf(1f) }
    // 记录按下时间戳：detectTapGestures 无 onLongPress 时，长按后松手也会触发 onTap，
    // 用按下持续时间过滤长按手势，防止长按取消定时后松手触发 onTap → startTimer 再次定时
    var pressStartTimeMs by remember { mutableLongStateOf(0L) }
    
    // 新气泡系统
    val bubbles = remember { mutableStateListOf<BouncingBubble>() }
    var bubbleIdCounter by remember { mutableFloatStateOf(0f) }
    
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
    
    // 新气泡生成逻辑：根据剩余时间百分比控制频率
    LaunchedEffect(showFillProgress) {
        if (!showFillProgress) {
            bubbles.clear()
            return@LaunchedEffect
        }
        
        while (true) {
            // 剩余时间越少，间隔越大（频率越低）
            val remainingPercent = (1f - fillProgress).coerceIn(0f, 1f)
            val minInterval = 100L    // 100% 剩余时，最快每100ms生成一个
            val maxInterval = 1200L   // 接近0%剩余时，每1.2s生成一个
            val interval = (minInterval + (maxInterval - minInterval) * (1f - remainingPercent)).toLong()
            
            delay(interval)
            
            if (!showFillProgress) break
            
            val id = bubbleIdCounter++
            val newBubble = BouncingBubble(
                id = id.toLong(),
                angle = Random.nextFloat() * 360f,
                size = Random.nextFloat() * 4f + 2f,  // 2~6dp
                progress = 0f,
                alpha = 0f  // 将从0淡入
            )
            bubbles.add(newBubble)
            
            // 启动该气泡的动画协程
            scope.launch {
                // 淡入阶段
                val fadeInDuration = 200L
                val fadeInStart = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - fadeInStart
                    val fadeProgress = (elapsed.toFloat() / fadeInDuration).coerceAtMost(1f)
                    val easedFade = FastOutSlowInEasing.transform(fadeProgress)
                    
                    val idx = bubbles.indexOfFirst { it.id == id.toLong() }
                    if (idx >= 0) {
                        bubbles[idx] = bubbles[idx].copy(alpha = (easedFade * 0.5f).coerceIn(0f, 1f))
                    }
                    
                    if (fadeProgress >= 1f) break
                    delay(16)
                }
                
                // 向中心移动阶段：贝塞尔曲线速率
                val moveDuration = (800L + Random.nextLong(600L))  // 800~1400ms
                val moveStart = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - moveStart
                    val rawProgress = (elapsed.toFloat() / moveDuration).coerceAtMost(1f)
                    val easedProgress = FastOutSlowInEasing.transform(rawProgress)
                    
                    val idx = bubbles.indexOfFirst { it.id == id.toLong() }
                    if (idx >= 0) {
                        // 接近中心时淡出
                        val progressAlpha = if (rawProgress > 0.7f) {
                            (1f - (rawProgress - 0.7f) / 0.3f).coerceIn(0f, 1f) * 0.5f
                        } else {
                            0.5f
                        }
                        bubbles[idx] = bubbles[idx].copy(progress = easedProgress, alpha = progressAlpha)
                    }
                    
                    if (rawProgress >= 1f || idx < 0) break
                    delay(16)
                }
                
                // 移除该气泡
                bubbles.removeAll { it.id == id.toLong() }
            }
        }
    }
    
    val wavePhase by rememberInfiniteTransition(label = "wave").animateFloat(
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
    ) {
        // 内层裁剪圆形：波+充电环（裁剪到圆内）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f * maxOf(setupCircleAlpha, timerAlpha)))
                .then(
                    // 合并为单一 pointerInput：避免 if/else 两分支共享同一 remember(Unit) 槽位
                    // 导致运行状态切换时新手势检测器不重建（长按取消失效的根因）。
                    // 运行态实时读取 TimerManager.timerState.value，规避闭包捕获过期 timerAlpha。
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressStartTimeMs = System.currentTimeMillis()
                                // 仅在定时运行中触长按取消
                                if (TimerManager.timerState.value.isActive) {
                                    isPressed = true
                                    scope.launch {
                                        hasStopped = false
                                        fadeOutProgress = 1f

                                        val totalChargeTime = 2000L
                                        val startTime = System.currentTimeMillis()
                                        var lastVibrateTime = 0L
                                        var stopped = false

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
                                                stopped = true
                                                break
                                            }
                                        }

                                        // 放电回退 — 如果未充满就松手
                                        if (!stopped && chargeValue > 0f) {
                                            val dischargeSpeed = 100f / 500f
                                            while (chargeValue > 0f) {
                                                delay(16)
                                                chargeValue = (chargeValue - dischargeSpeed * 16f).coerceAtLeast(0f)
                                                visualProgress = chargeValue / 100f
                                            }
                                        }
                                    }
                                }
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = {
                                // detectTapGestures 无 onLongPress 时，长按后松手也会触发 onTap。
                                // 通过按下持续时间过滤：超过 200ms 视为长按（充电取消定时），
                                // 不响应 tap，防止松手时 startTimer 再次定时。
                                // 旧实现仅靠 hasStopped 防护，但 hasStopped 在 fadeOut 动画
                                // 结束（~320ms）后被 LaunchedEffect 重置为 false，
                                // 若用户充电完成后稍晚松手，onTap 读到 hasStopped=false → 再次定时。
                                val pressDuration = System.currentTimeMillis() - pressStartTimeMs
                                if (pressDuration > 200L) return@detectTapGestures
                                // 刚通过长按取消定时不响应 tap，避免立即重启
                                if (hasStopped) return@detectTapGestures
                                // 直接读取 TimerManager 当前状态，避免 pointerInput 闭包捕获过期值
                                val state = TimerManager.timerState.value
                                if (!state.isActive && !state.isFinished) {
                                    if (state.totalMinutes == 0) {
                                        ToastManager.info("请先设置时间")
                                    } else {
                                        TimerManager.startTimer()
                                        ToastManager.info("已开始计时")
                                    }
                                }
                            }
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // 波+充电环 Canvas（在裁剪圆内）
            if (showFillProgress) {
                Canvas(modifier = Modifier.fillMaxSize()) {
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
                        
                        var xPos = 0f
                        val step = 5f
                        while (xPos <= size.width) {
                            val y = waveY + sin((xPos / size.width * 4 * PI) + Math.toRadians(wavePhase.toDouble())).toFloat() * waveAmplitude
                            if (!y.isNaN()) lineTo(xPos, y)
                            xPos += step
                        }
                        lineTo(size.width, size.height)
                        close()
                    }
                    
                    drawPath(path = wavePath, color = primaryColor.copy(alpha = 0.3f * timerAlpha))
                    
                    // 充电环（在圆内）
                    if (visualProgress > 0f && fadeOutProgress > 0f) {
                        val layerCount = 4
                        for (layer in 0 until layerCount) {
                            val layerStart = layer * 0.25f
                            val layerEnd = (layer + 1) * 0.25f
                            if (visualProgress > layerStart) {
                                val lp = if (visualProgress >= layerEnd) 1f else (visualProgress - layerStart) / 0.25f
                                drawCircle(
                                    color = chargeOverlayColor.copy(alpha = 0.25f * fadeOutProgress),
                                    radius = radius * lp,
                                    center = Offset(centerX, centerY)
                                )
                            }
                        }
                    }
                }
            }
            
            // 设置/计时文字
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (setupCircleAlpha > 0f) {
                    Text(
                        text = String.format("%02d:%02d", hours, minutes),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = setupCircleAlpha)
                    )
                }
                
                if (timerAlpha > 0f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = timerAlpha),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                            if (h > 0) {
                                RollingNumber(value = h.toInt(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = timerAlpha))
                                Text(text = ":", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(alpha = timerAlpha))
                            }
                            RollingNumber(value = m.toInt(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = timerAlpha))
                            Text(text = ":", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(alpha = timerAlpha))
                            RollingNumber(value = s.toInt(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = timerAlpha))
                        }
                    }
                }
            }
        }
        
        // 气泡层 Canvas（在裁剪圆外，可超出圆形边界）
        if (showFillProgress) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width.isNaN() || size.height.isNaN() || size.width <= 0f || size.height <= 0f) {
                    return@Canvas
                }
                
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.width / 2
                
                for (bubble in bubbles.toList()) {
                    if (bubble.alpha <= 0.01f) continue
                    
                    // progress=0 在圆边缘，progress=1 在中心附近
                    val currentR = radius - (radius * 0.85f) * bubble.progress
                    val radians = Math.toRadians(bubble.angle.toDouble()).toFloat()
                    val bx = centerX + kotlin.math.cos(radians) * currentR
                    val by = centerY + kotlin.math.sin(radians) * currentR
                    
                    if (bx.isNaN() || by.isNaN()) continue
                    
                    val bubbleSizePx = with(density) { bubble.size.dp.toPx() }
                    
                    drawCircle(
                        color = primaryColor.copy(alpha = bubble.alpha),
                        radius = bubbleSizePx,
                        center = Offset(bx, by)
                    )
                }
            }
        }
    }
}
