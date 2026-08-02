package com.bicy.whitenoise.ui.components.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 顶层 toast 宿主，放置在 AppContent 之上。
 *
 * ## 职责
 *   - 监听 ToastManager.toasts StateFlow 并渲染
 *   - 从顶部滑入（带弹性），向上淡出
 *   - 管理非 LOADING / 非 CRITICAL toast 的自动消失倒计时
 *   - 支持左右滑动消除（CRITICAL 不可滑动消除）
 *   - LOADING → complete() 后，原地切换 SUCCESS 并启动倒计时
 *   - CRITICAL 常驻显示，仅手动消除
 */
@Composable
fun SmartToastHost(
    modifier: Modifier = Modifier,
    maxWidthFraction: Float = 0.85f
) {
    val toasts by ToastManager.toasts.collectAsState()
    val orientation = LocalConfiguration.current.orientation
    val isPortrait = orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    // 追踪每条 toast 的动画可见性（用于退出动画）
    var visibleIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 新 toast 加入 → 标记可见
    val currentIds = toasts.map { it.id }.toSet()
    LaunchedEffect(currentIds) {
        visibleIds = currentIds
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (isPortrait) Alignment.TopEnd else Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .padding(
                    top = 12.dp,
                    start = if (isPortrait) 0.dp else 12.dp,
                    end = if (isPortrait) 8.dp else 12.dp
                )
                .widthIn(max = if (isPortrait) 280.dp else 360.dp)
                .fillMaxWidth(maxWidthFraction),
            horizontalAlignment = if (isPortrait) Alignment.End else Alignment.CenterHorizontally,
            // 堆叠时每条向下偏移：spacedBy 提供基础间距
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 逆序遍历：最新的在最上面（CRITICAL 因插入逻辑已位于列表尾端 = 视觉顶部）
            toasts.asReversed().forEachIndexed { visualIndex, item ->
                val isVisible = item.id in visibleIds

                // loading 已完成（progress=1.0 且 type=LOADING ⇒ 自动完成）
                val autoComplete = item.type == ToastType.LOADING && (item.progress ?: 0f) >= 1f

                // 自动消失逻辑：非 LOADING 且非 CRITICAL
                // CRITICAL 常驻，仅手动 removeById 触发
                if (item.type != ToastType.LOADING && item.priority != ToastPriority.CRITICAL) {
                    AutoDismissEffect(item.id, item.durationMs)
                }

                // loading 自动完成
                if (autoComplete) {
                    AutoCompleteEffect(item.id)
                }

                // 弹性滑入 / 向上淡出
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetY = { -it }
                    ) + fadeIn(tween(220, easing = FastOutSlowInEasing)),
                    exit = slideOutVertically(
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                        targetOffsetY = { -it }
                    ) + fadeOut(tween(180))
                ) {
                    // CRITICAL 不可滑动消除（仅靠 ToastCard 内的关闭按钮）
                    if (item.priority == ToastPriority.CRITICAL) {
                        ToastCard(
                            item = item,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        SwipeableToastCard(
                            item = item,
                            onDismiss = { ToastManager.removeById(item.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * 可滑动消除的 ToastCard 包装：
 * - 跟随手指水平移动，同步降低透明度
 * - 滑动超过阈值时自动滑出并触发 onDismiss
 * - 未达阈值时回弹到原位
 */
@Composable
private fun SwipeableToastCard(
    item: ToastItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // 触发消除的阈值（约 100dp）
    val swipeThresholdPx = with(density) { 100.dp.toPx() }
    // 滑出动画的目标距离（足够远以确保完全离开屏幕）
    val offScreenPx = with(density) { 400.dp.toPx() }
    val offset = remember(item.id) { Animatable(0f) }

    Box(
        modifier = modifier
            .pointerInput(item.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val current = offset.value
                        if (abs(current) > swipeThresholdPx) {
                            // 超过阈值：滑出屏幕后回调消除
                            scope.launch {
                                offset.animateTo(
                                    targetValue = if (current > 0) offScreenPx else -offScreenPx,
                                    animationSpec = tween(180)
                                )
                                onDismiss()
                            }
                        } else {
                            // 未达阈值：回弹
                            scope.launch { offset.animateTo(0f, tween(150)) }
                        }
                    },
                    onDragCancel = {
                        scope.launch { offset.animateTo(0f, tween(150)) }
                    }
                ) { _, dragAmount ->
                    scope.launch { offset.snapTo(offset.value + dragAmount) }
                }
            }
            .graphicsLayer {
                translationX = offset.value
                // 滑动时透明度降低（最多降至 0.4）
                val progress = (abs(offset.value) / swipeThresholdPx).coerceIn(0f, 1f)
                alpha = 1f - progress * 0.6f
            }
    ) {
        ToastCard(
            item = item,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 非 LOADING / 非 CRITICAL toast：延迟 N 秒后自动移除 */
@Composable
private fun AutoDismissEffect(id: String, durationMs: Long? = null) {
    LaunchedEffect(id) {
        val ms = durationMs ?: (ToastManager.getDurationSeconds() * 1000L)
        delay(ms)
        ToastManager.removeById(id)
    }
}

/** LOADING 进度到 1.0 自动触发 complete */
@Composable
private fun AutoCompleteEffect(id: String) {
    LaunchedEffect(id) {
        ToastManager.complete("完成")
    }
}
