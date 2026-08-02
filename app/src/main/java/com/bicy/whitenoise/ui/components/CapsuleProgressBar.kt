package com.bicy.whitenoise.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * 胶囊状进度条组件（无滑块版本）
 * 纯胶囊形状，支持拖动和点击调整进度
 */
@Composable
fun CapsuleProgressBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    capsuleHeight: Dp = 32.dp,
    capsuleCornerRadius: Dp = 16.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    progressColor: Color = MaterialTheme.colorScheme.primary,
    animationEnabled: Boolean = true
) {
    // 确保progress在有效范围内
    val safeProgress = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    
    // 贝塞尔曲线动画参数
    val bezierAnimationSpec: AnimationSpec<Float> = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing // 贝塞尔曲线速率
    )
    
    // 进度动画
    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = if (animationEnabled) bezierAnimationSpec else tween(0),
        label = "progressAnimation"
    )
    
    // 手势状态
    var isDragging by remember { mutableStateOf(false) }
    
    val density = LocalDensity.current
    val capsuleHeightPx = with(density) { capsuleHeight.toPx() }
    val cornerRadiusPx = with(density) { capsuleCornerRadius.toPx() }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(capsuleHeight)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(capsuleHeight)
                .pointerInput(enabled, progress) {
                    if (!enabled) return@pointerInput
                    
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        
                        isDragging = true
                        
                        // 计算点击位置的进度
                        val canvasWidth = size.width
                        if (canvasWidth > 0) {
                            val clickX = down.position.x
                            val newProgress = (clickX / canvasWidth).coerceIn(0f, 1f)
                            onProgressChange(newProgress)
                        }
                        
                        // 拖动处理 - 确保跟随拖动而不是点击跳转
                        var lastProgress = if (size.width > 0) (down.position.x / size.width).coerceIn(0f, 1f) else 0f
                        drag(down.id) { change ->
                            change.consume()
                            
                            val canvasWidth = size.width
                            if (canvasWidth > 0) {
                                val currentX = change.position.x
                                val currentProgress = (currentX / canvasWidth).coerceIn(0f, 1f)
                                // 实时跟随拖动更新进度
                                onProgressChange(currentProgress)
                                lastProgress = currentProgress
                            }
                        }
                        
                        isDragging = false
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            if (canvasWidth > 0 && canvasHeight > 0) {
                // 绘制背景胶囊
                drawRoundRect(
                    color = backgroundColor,
                    topLeft = Offset.Zero,
                    size = Size(canvasWidth, canvasHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
                )
                
                // 绘制进度胶囊（纯胶囊形状，无滑块）
                val progressWidth = canvasWidth * animatedProgress
                if (progressWidth > 0) {
                    drawRoundRect(
                        color = progressColor.copy(alpha = if (isDragging) 0.8f else 1f),
                        topLeft = Offset.Zero,
                        size = Size(progressWidth, canvasHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
                    )
                }
                
                // 拖动时的视觉反馈：在进度位置显示一个小的指示点
                if (isDragging) {
                    val indicatorX = canvasWidth * animatedProgress
                    val indicatorY = canvasHeight / 2
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = 8f,
                        center = Offset(indicatorX, indicatorY)
                    )
                }
            }
        }
    }
}

/**
 * 可动画位移的胶囊状进度条
 * 支持展开/收缩时的位移动画
 */
@Composable
fun AnimatedCapsuleProgressBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    capsuleHeight: Dp = 32.dp,
    capsuleCornerRadius: Dp = 16.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    progressColor: Color = MaterialTheme.colorScheme.primary,
    expandedOffsetY: Dp = 0.dp,
    collapsedOffsetY: Dp = 100.dp,
    animationDuration: Int = 300
) {
    // 贝塞尔曲线位移动画
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isExpanded) expandedOffsetY.value else collapsedOffsetY.value,
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = FastOutSlowInEasing // 贝塞尔曲线速率
        ),
        label = "offsetYAnimation"
    )
    
    // Alpha动画（收缩时透明度降低）
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = FastOutSlowInEasing
        ),
        label = "alphaAnimation"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = animatedOffsetY
                alpha = animatedAlpha
            }
    ) {
        CapsuleProgressBar(
            progress = progress,
            onProgressChange = onProgressChange,
            enabled = enabled,
            capsuleHeight = capsuleHeight,
            capsuleCornerRadius = capsuleCornerRadius,
            backgroundColor = backgroundColor,
            progressColor = progressColor,
            animationEnabled = true
        )
    }
}