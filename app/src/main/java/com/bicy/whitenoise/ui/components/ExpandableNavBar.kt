package com.bicy.whitenoise.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bicy.whitenoise.R
import androidx.compose.ui.unit.Dp
import com.bicy.whitenoise.storage.config.GlassRenderConfig
import com.bicy.whitenoise.storage.config.LiquidGlassConfig
import com.bicy.whitenoise.storage.config.NavBackgroundConfig
import com.bicy.whitenoise.timer.TimerManager
import com.bicy.whitenoise.timer.TimerState
import com.bicy.whitenoise.ui.components.glass.GlassMode
import com.bicy.whitenoise.ui.components.glass.GlassBoxScope
import com.bicy.whitenoise.ui.utils.ResponsiveDimensions
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.CollapsedCornerRadius
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.CollapsedHeight
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.CollapsedMarginBottom
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.CollapsedMarginHorizontal
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.NavIconSize
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.NavItemSize
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.PresetButtonsContent
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.TimeSlidersContent
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.TimerCircleContent
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.TimerFinishedButtons
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.TimerFinishedContent
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.TimerSettingsContent
import com.bicy.whitenoise.ui.navigation.ScreenPart.screens
import com.bicy.whitenoise.ui.navigation.ScreenPart.*
import com.bicy.whitenoise.ui.theme.NavItemUnselected
import com.bicy.whitenoise.ui.theme.ShadowConfig
import com.bicy.whitenoise.ui.theme.ThemeColorManager
import com.bicy.whitenoise.ui.theme.dropShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ExpandableNavBar(
    currentRoute: String?,
    onRouteSelected: (String) -> Unit,
    onExpandProgress: (Float) -> Unit = {},
    onInteractionStateChanged: (Boolean) -> Unit = {},
    isOtherInteracting: Boolean = false,
    forceCollapseOther: () -> Unit = {},
    forceCollapse: Boolean = false,
    onForceCollapseComplete: () -> Unit = {},
    modifier: Modifier = Modifier,
    // 渲染配置参数（用于动态构建glassModifier）
    glassMode: GlassMode = GlassMode.OFF,
    glassScale: Float = 0f,
    glassBlur: Float = 0f,
    glassCenterDistortion: Float = 0f,
    glassElevation: Int = 4,
    glassDarkness: Float = 0f,
    glassWarpEdges: Float = 0f,
    // GlassBoxScope用于调用glassBackground方法
    glassScope: GlassBoxScope? = null,
    // 白噪音播放状态（用于播放页tab图标变化）
    isWhiteNoisePlaying: Boolean = false,
    // Chat 输入区参数
    showChatInput: Boolean = false,
    chatInputValue: String = "",
    chatInputEnabled: Boolean = true,
    onChatInputChange: (String) -> Unit = {},
    onChatSend: () -> Unit = {},
    // 工具调用确认模式（变色按钮）
    confirmMode: Boolean = false,
    onToggleConfirmMode: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current
    val isLandscape = ResponsiveDimensions.isLandscape()

    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()
    val backgroundAlpha by NavBackgroundConfig.backgroundAlphaFlow.collectAsState()

    // 阴影参数 - 根据玻璃模式读取对应配置
    val shadowEnabled by remember(glassMode) {
        if (glassMode == GlassMode.PERFECT)
            GlassRenderConfig.perfShadowEnabledFlow
        else
            GlassRenderConfig.compatShadowEnabledFlow
    }.collectAsState(initial = false)

    val shadowStrength by remember(glassMode) {
        if (glassMode == GlassMode.PERFECT)
            GlassRenderConfig.perfShadowStrengthFlow
        else
            GlassRenderConfig.compatShadowStrengthFlow
    }.collectAsState(initial = 0.5f)

    val shadowHeight by remember(glassMode) {
        if (glassMode == GlassMode.PERFECT)
            GlassRenderConfig.perfElevationFlow
        else
            GlassRenderConfig.compatShadowHeightFlow
    }.collectAsState(initial = 0.5f)

    val expandProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Chat 输入框过渡动画
    val chatInputHeightAnim = remember { Animatable(0f) }
    val chatInputAlphaAnim = remember { Animatable(0f) }

    var isExpanded by remember { mutableStateOf(false) }
    var isInteracting by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isAnimating by remember { mutableStateOf(false) }

    // Chat 输入框动画：展开→淡入 / 淡出→收起
    LaunchedEffect(showChatInput) {
        if (showChatInput) {
            chatInputHeightAnim.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
            chatInputAlphaAnim.animateTo(1f, tween(200))
        } else {
            chatInputAlphaAnim.animateTo(0f, tween(200))
            chatInputHeightAnim.animateTo(0f, tween(250, easing = FastOutSlowInEasing))
        }
    }

    val timerState by TimerManager.timerState.collectAsState()

    LaunchedEffect(forceCollapse) {
        if (forceCollapse && expandProgress.value > 0.1f && !isAnimating) {
            isAnimating = true
            isExpanded = false
            isInteracting = true
            onInteractionStateChanged(true)
            expandProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
            isInteracting = false
            onInteractionStateChanged(false)
            onForceCollapseComplete()
            isAnimating = false
        }
    }

    LaunchedEffect(timerState.isFinished) {
        if (timerState.isFinished && timerState.ringEnabled && !isExpanded) {
            forceCollapseOther()
            isExpanded = true
            isInteracting = true
            onInteractionStateChanged(true)
            expandProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
            isInteracting = false
            onInteractionStateChanged(false)
        }
    }

    val selectedColor = themeColor.navItemSelected
    val navBgColor = themeColor.navBg

    val rawProgress = expandProgress.value
    val progress = if (rawProgress.isNaN()) 0f else rawProgress.coerceIn(0f, 1f)

    val currentMarginHorizontal = remember(progress, isLandscape) {
        if (isLandscape) {
            val value = CollapsedMarginBottom * (1f - progress)
            if (value.value.isNaN() || value < 0.dp) 0.dp else value
        } else {
            val value = CollapsedMarginHorizontal * (1f - progress)
            if (value.value.isNaN() || value < 0.dp) 0.dp else value
        }
    }
    val currentMarginBottom = remember(progress, isLandscape) {
        if (isLandscape) {
            val value = CollapsedMarginHorizontal * (1f - progress)
            if (value.value.isNaN() || value < 0.dp) 0.dp else value
        } else {
            val value = CollapsedMarginBottom * (1f - progress)
            if (value.value.isNaN() || value < 0.dp) 0.dp else value
        }
    }
    val currentCornerRadius = remember(progress) {
        val value = CollapsedCornerRadius * (1f - progress)
        if (value.value.isNaN() || value < 0.dp) 0.dp else value
    }

    // 根据动态的currentCornerRadius构建glassModifier（当glassScope可用时）
    val glassModifier = if (glassScope != null && glassMode != GlassMode.OFF) {
        remember(currentCornerRadius, glassScale, glassBlur, glassCenterDistortion,
                 glassElevation, glassDarkness, glassWarpEdges) {
            glassScope.run {
                Modifier.glassBackground(
                    id = 0L,
                    scale = glassScale.coerceIn(0f, 1f),
                    blur = glassBlur.coerceIn(0f, 1f),
                    centerDistortion = glassCenterDistortion.coerceIn(0f, 1f),
                    shape = RoundedCornerShape(currentCornerRadius),
                    elevation = glassElevation.dp,
                    tint = Color.Transparent,
                    darkness = glassDarkness.coerceIn(0f, 1f),
                    warpEdges = glassWarpEdges.coerceIn(0f, 1f)
                )
            }
        }
    } else {
        Modifier
    }

    val collapsedWidth = remember(screenWidth, isLandscape) {
        screenWidth - CollapsedMarginHorizontal * 2
    }
    val currentWidth = remember(progress, screenWidth, collapsedWidth, isLandscape) {
        if (isLandscape) {
            val value = CollapsedHeight + (screenWidth - CollapsedHeight) * progress
            if (value.value.isNaN() || value < CollapsedHeight) CollapsedHeight else value
        } else {
            val value = collapsedWidth + (screenWidth - collapsedWidth) * progress
            if (value.value.isNaN() || value < 0.dp) screenWidth else value
        }
    }

    val chatInputExtraHeight = if (!isLandscape) 56.dp * chatInputHeightAnim.value else 0.dp
    val currentHeight = remember(progress, screenHeight, isLandscape, chatInputExtraHeight) {
        if (isLandscape) {
            screenHeight
        } else {
            val baseHeight = CollapsedHeight + chatInputExtraHeight
            val value = baseHeight + (screenHeight - baseHeight) * progress
            if (value.value.isNaN() || value < CollapsedHeight) CollapsedHeight else value
        }
    }

    val contentAlpha = remember(progress) { (1f - progress).coerceIn(0f, 1f) }
    val panelAlpha = remember(progress) { progress.coerceIn(0f, 1f) }
    val textAlpha = remember(progress) {
        if (progress > 0.9f) ((progress - 0.9f) / 0.1f).coerceIn(0f, 1f) else 0f
    }

    val zIndex = if (isInteracting || isExpanded) 2f else if (isOtherInteracting) 0f else 1f

    onExpandProgress(progress)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isLandscape) {
                        Modifier.padding(
                            end = currentMarginHorizontal,
                            top = currentMarginBottom,
                            bottom = currentMarginBottom
                        )
                    } else {
                        Modifier.padding(
                            start = currentMarginHorizontal,
                            end = currentMarginHorizontal,
                            bottom = currentMarginBottom
                        )
                    }
                )
                .navigationBarsPadding(),
            contentAlignment = if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .then(
                        if (isLandscape) {
                            Modifier.width(currentWidth).height(currentHeight)
                        } else {
                            Modifier.width(currentWidth).height(currentHeight)
                        }
                    )
                    .then(
                        if (shadowEnabled) {
                            Modifier.dropShadow(
                                config = ShadowConfig(
                                    offsetY = (shadowHeight * 12).dp.coerceIn(0.dp, 12.dp),
                                    blurRadius = (shadowHeight * 24 + 4).dp.coerceIn(4.dp, 28.dp),
                                    color = Color.Black.copy(alpha = 0.1f + shadowStrength * 0.4f)
                                ),
                                shape = RoundedCornerShape(currentCornerRadius),
                                clip = false
                            )
                        } else {
                            Modifier
                        }
                    )
                    .then(glassModifier)
                    .graphicsLayer {
                        shape = RoundedCornerShape(currentCornerRadius)
                        clip = true
                    }
            ) {
                // 背景层 - 玻璃模式透明，让 Shader 效果透出；关闭模式显示背景色
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(
                            if (glassScope != null && glassMode != GlassMode.OFF) {
                                Modifier.background(Color.Transparent)
                            } else {
                                Modifier.background(navBgColor)
                            }
                        )
                )

                // 内容层 - 包含交互逻辑
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit, isLandscape) {
                        if (isLandscape) {
                            detectDragGestures(
                                onDragStart = {
                                    dragOffset = 0f
                                    isInteracting = true
                                    onInteractionStateChanged(true)
                                    scope.launch { expandProgress.stop() }
                                },
                                onDragEnd = {
                                    isInteracting = false
                                    onInteractionStateChanged(false)
                                    val threshold = with(density) { 50.dp.toPx() }
                                    val currentProgress = expandProgress.value

                                    if (abs(dragOffset) > threshold) {
                                        val targetValue = if (dragOffset < 0) 1f else 0f
                                        scope.launch {
                                            expandProgress.animateTo(
                                                targetValue = targetValue,
                                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        isExpanded = dragOffset < 0
                                    } else {
                                        val targetValue = if (currentProgress > 0.5f) 1f else 0f
                                        scope.launch {
                                            expandProgress.animateTo(
                                                targetValue = targetValue,
                                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        isExpanded = currentProgress > 0.5f
                                    }
                                    dragOffset = 0f
                                },
                                onDrag = { _, dragAmount ->
                                    val isHorizontal = abs(dragAmount.x) > abs(dragAmount.y)
                                    if (isHorizontal) {
                                        dragOffset += dragAmount.x
                                        val totalDragNeeded = with(density) {
                                            screenWidth.toPx() - CollapsedHeight.toPx()
                                        }
                                        val currentProgress = expandProgress.value
                                        val newProgress = (currentProgress - dragAmount.x / totalDragNeeded).coerceIn(0f, 1f)
                                        scope.launch { expandProgress.snapTo(newProgress) }
                                    }
                                }
                            )
                        } else {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    dragOffset = 0f
                                    isInteracting = true
                                    onInteractionStateChanged(true)
                                    scope.launch { expandProgress.stop() }
                                },
                                onDragEnd = {
                                    isInteracting = false
                                    onInteractionStateChanged(false)
                                    val threshold = with(density) { 100.dp.toPx() }
                                    val currentProgress = expandProgress.value

                                    if (abs(dragOffset) > threshold) {
                                        val targetValue = if (dragOffset < 0) 1f else 0f
                                        scope.launch {
                                            expandProgress.animateTo(
                                                targetValue = targetValue,
                                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        isExpanded = dragOffset < 0
                                    } else {
                                        val targetValue = if (currentProgress > 0.5f) 1f else 0f
                                        scope.launch {
                                            expandProgress.animateTo(
                                                targetValue = targetValue,
                                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        isExpanded = currentProgress > 0.5f
                                    }
                                    dragOffset = 0f
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    dragOffset += dragAmount
                                    val totalDragNeeded = with(density) {
                                        screenHeight.toPx() - CollapsedHeight.toPx()
                                    }
                                    val currentProgress = expandProgress.value
                                    val newProgress = (currentProgress - dragAmount / totalDragNeeded).coerceIn(0f, 1f)
                                    scope.launch { expandProgress.snapTo(newProgress) }
                                }
                            )
                        }
                    }
                ) {
                    // 底部对齐布局：Tabs 始终在底部，Timer 面板向上展开
                    // Chat 输入使用覆盖层叠在 tabs 上方，不挤占 tab 位置
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Timer 面板：填充剩余空间，随展开淡入
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .alpha(panelAlpha)
                            ) {
                                ExpandedNavBarContent(
                                    alpha = panelAlpha,
                                    textAlpha = textAlpha,
                                    timerState = timerState,
                                    isEnabled = progress >= 0.5f,
                                    isLandscape = isLandscape
                                )
                            }

                            // Tabs: 始终在底部，随展开淡出（不隐藏，由 alpha 控制可见性）
                            CollapsedNavBarContent(
                                currentRoute = currentRoute,
                                onRouteSelected = onRouteSelected,
                                alpha = contentAlpha,
                                selectedColor = selectedColor,
                                timerState = timerState,
                                isExpanded = false,
                                isLandscape = isLandscape,
                                glassMode = glassMode,
                                isWhiteNoisePlaying = isWhiteNoisePlaying
                            )
                        }

                        // Chat 输入区：覆盖叠在 tabs 上方，从下方滑入/滑出，不挤占 tab 位置
                        if ((showChatInput || chatInputHeightAnim.value > 0.01f) && progress < 0.1f && !isLandscape) {
                            val density = LocalDensity.current
                            val slideOffsetY = with(density) {
                                -(CollapsedHeight.toPx() * chatInputHeightAnim.value)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .align(Alignment.BottomCenter)
                                    .offset { IntOffset(0, slideOffsetY.toInt()) }
                                    .alpha(chatInputAlphaAnim.value)
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 变色按钮：灰=直接执行 / 主题色=弹窗确认
                                FilledIconButton(
                                    onClick = onToggleConfirmMode,
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (confirmMode)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (confirmMode)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = if (confirmMode) "弹窗确认已开启" else "弹窗确认已关闭",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = chatInputValue,
                                    onValueChange = onChatInputChange,
                                    placeholder = {
                                        Text(
                                            stringResource(R.string.ai_input_hint),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 3,
                                    enabled = chatInputEnabled,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledIconButton(
                                    onClick = onChatSend,
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    enabled = chatInputValue.isNotBlank() && chatInputEnabled,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(R.string.ai_send),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsedNavBarContent(
    currentRoute: String?,
    onRouteSelected: (String) -> Unit,
    alpha: Float,
    selectedColor: Color,
    timerState: TimerState,
    isExpanded: Boolean = false,
    isLandscape: Boolean = false,
    glassMode: GlassMode,
    isWhiteNoisePlaying: Boolean = false
) {
    val showRing = timerState.isActive && timerState.remainingTime > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val ringHelper = remember { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RingTimerShaderHelper() else null }
    val totalTime = timerState.totalMinutes * 60 * 1000L
    val targetProgress = if (totalTime > 0) timerState.remainingTime.toFloat() / totalTime else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "navProgress"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val ringColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    val timerRingModifier = if (showRing && ringHelper != null) {
        Modifier.graphicsLayer {
            val progress = animatedProgress.coerceIn(0f, 1f)
            if (progress.isNaN()) return@graphicsLayer
            ringHelper.updateUniforms(
                width = size.width,
                height = size.height,
                progress = progress,
                strokeWidth = 2.dp.toPx(),
                cornerRadius = with(density) { CollapsedCornerRadius.toPx() },
                colorRed = ringColor.red,
                colorGreen = ringColor.green,
                colorBlue = ringColor.blue,
                colorAlpha = ringColor.alpha * pulseAlpha
            )
            renderEffect = ringHelper.asComposeRenderEffect()
        }
    } else Modifier

    if (isLandscape) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .alpha(alpha)
                .then(timerRingModifier),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .then(if (!isExpanded) Modifier.padding(vertical = 32.dp) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!isExpanded) {
                    screens.forEach { screen ->
                        NavItem(
                            screen = screen,
                            isSelected = currentRoute == screen.route,
                            selectedColor = selectedColor,
                            onClick = { onRouteSelected(screen.route) },
                            glassMode = glassMode,
                            isWhiteNoisePlaying = isWhiteNoisePlaying
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CollapsedHeight)
                .alpha(alpha)
                .then(timerRingModifier),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CollapsedHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isExpanded) {
                    screens.forEach { screen ->
                        NavItem(
                            modifier = Modifier.weight(1f),
                            screen = screen,
                            isSelected = currentRoute == screen.route,
                            selectedColor = selectedColor,
                            onClick = { onRouteSelected(screen.route) },
                            glassMode = glassMode,
                            isWhiteNoisePlaying = isWhiteNoisePlaying
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedNavBarContent(
    alpha: Float,
    textAlpha: Float,
    timerState: TimerState,
    isEnabled: Boolean = true,
    isLandscape: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current

    val hours = timerState.totalMinutes / 60
    val minutes = timerState.totalMinutes % 60
    val remainingTime = timerState.remainingTime
    val h = remainingTime / 3600000f
    val m = (remainingTime % 3600000f) / 60000f
    val s = (remainingTime % 60000f) / 1000f

    var circleCentered by remember { mutableStateOf(false) }
    var showTimerContent by remember { mutableStateOf(false) }
    var showSetupContent by remember { mutableStateOf(true) }
    var showSetupCircleContent by remember { mutableStateOf(true) }
    var showFillProgress by remember { mutableStateOf(false) }

    LaunchedEffect(timerState.isFinished) {
        if (timerState.isFinished) {
            showSetupContent = false
            showSetupCircleContent = false
            showTimerContent = false
            showFillProgress = false
            circleCentered = false
        } else if (!timerState.isActive) {
            showSetupContent = true
            showSetupCircleContent = true
            showTimerContent = false
            showFillProgress = false
        }
    }

    LaunchedEffect(timerState.isActive) {
        if (timerState.isActive && !circleCentered && !timerState.isFinished) {
            showSetupContent = false
            showSetupCircleContent = false
            delay(200)
            circleCentered = true
            delay(500)
            showTimerContent = true
            showFillProgress = true
        } else if (!timerState.isActive && circleCentered && !timerState.isFinished) {
            showTimerContent = false
            showFillProgress = false
            delay(200)
            circleCentered = false
            delay(500)
            showSetupContent = true
            showSetupCircleContent = true
        }
    }

    val circleOffsetY by animateDpAsState(
        targetValue = if (circleCentered && !isLandscape) {
            with(density) { (screenHeight - 200.dp) / 2 - 100.dp }
        } else {
            0.dp
        },
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "circleOffset"
    )

    val setupAlpha by animateFloatAsState(
        targetValue = if (showSetupContent) textAlpha else 0f,
        animationSpec = tween(200),
        label = "setupAlpha"
    )

    val timerAlpha by animateFloatAsState(
        targetValue = if (showTimerContent) textAlpha else 0f,
        animationSpec = tween(200),
        label = "timerAlpha"
    )

    val setupCircleAlpha by animateFloatAsState(
        targetValue = if (showSetupCircleContent) textAlpha else 0f,
        animationSpec = tween(200),
        label = "setupCircleAlpha"
    )

    val totalTime = timerState.totalMinutes * 60 * 1000L
    val targetFillProgress = if (totalTime > 0 && showFillProgress) {
        val progress = 1f - (remainingTime.toFloat() / totalTime)
        if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    } else 0f

    val animatedFillProgress by animateFloatAsState(
        targetValue = targetFillProgress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "fillProgress"
    )

    val circleSize = if (isLandscape) 130.dp else 180.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(0.4f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(circleSize),
                        contentAlignment = Alignment.Center
                    ) {
                        TimerCircleContent(
                            hours = hours,
                            minutes = minutes,
                            fillProgress = animatedFillProgress,
                            showFillProgress = showFillProgress,
                            setupCircleAlpha = setupCircleAlpha,
                            timerAlpha = timerAlpha,
                            h = h,
                            m = m,
                            s = s
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 定时球下方提示：引导用户点击定时球开始计时
                    Text(
                        text = "↑点定时球开始定时↑",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = setupAlpha * 0.6f)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PresetButtonsContent(
                            hours = hours,
                            minutes = minutes,
                            textAlpha = setupAlpha,
                            isEnabled = isEnabled
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(0.6f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TimeSlidersContent(
                        hours = hours,
                        minutes = minutes,
                        textAlpha = setupAlpha,
                        isEnabled = isEnabled
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TimerSettingsContent(
                        pauseType = timerState.pauseType,
                        snoozeMinutes = timerState.snoozeMinutes,
                        ringEnabled = timerState.ringEnabled,
                        textAlpha = setupAlpha,
                        isEnabled = isEnabled
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.timer),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = setupAlpha)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .size(circleSize)
                        .offset { IntOffset(0, with(density) { circleOffsetY.roundToPx() }) },
                    contentAlignment = Alignment.Center
                ) {
                    TimerCircleContent(
                        hours = hours,
                        minutes = minutes,
                        fillProgress = animatedFillProgress,
                        showFillProgress = showFillProgress,
                        setupCircleAlpha = setupCircleAlpha,
                        timerAlpha = timerAlpha,
                        h = h,
                        m = m,
                        s = s
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (showSetupContent) {
                    // 定时球下方提示：引导用户点击定时球开始计时
                    Text(
                        text = "↑点定时球开始定时↑",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = setupAlpha * 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    PresetButtonsContent(
                        hours = hours,
                        minutes = minutes,
                        textAlpha = setupAlpha,
                        isEnabled = isEnabled
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TimeSlidersContent(
                        hours = hours,
                        minutes = minutes,
                        textAlpha = setupAlpha,
                        isEnabled = isEnabled
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TimerSettingsContent(
                        pauseType = timerState.pauseType,
                        snoozeMinutes = timerState.snoozeMinutes,
                        ringEnabled = timerState.ringEnabled,
                        textAlpha = setupAlpha,
                        isEnabled = isEnabled
                    )
                }
            }
        }

        if (timerState.isFinished) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                TimerFinishedContent(
                    snoozeMinutes = timerState.snoozeMinutes,
                    textAlpha = textAlpha
                )

                Spacer(modifier = Modifier.height(24.dp))

                TimerFinishedButtons(
                    snoozeMinutes = timerState.snoozeMinutes,
                    textAlpha = textAlpha,
                    isEnabled = isEnabled
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    modifier: Modifier = Modifier,
    screen: Screen,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    glassMode: GlassMode = GlassMode.OFF,
    isWhiteNoisePlaying: Boolean = false
) {
    val density = LocalDensity.current
    val iconRes = when (screen) {
        is Screen.Home -> R.drawable.ic_home
        is Screen.Scattered -> R.drawable.ic_scattered
        is Screen.Play -> R.drawable.ic_play
        is Screen.Chat -> R.drawable.ic_chat
        is Screen.Setting -> R.drawable.ic_setting
    }

    // 播放页tab在白噪音播放时显示暂停图标
    val icon = if (screen is Screen.Play && isWhiteNoisePlaying && isSelected) {
        Icons.Default.Pause
    } else {
        ImageVector.vectorResource(id = iconRes)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.15f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )

    val iconColor: Color by animateColorAsState(
        targetValue = if (isSelected) selectedColor else NavItemUnselected,
        animationSpec = tween(durationMillis = 300),
        label = "navIconColor"
    )

    // 选中高光效果 - 使用简单的半透明背景
    val highlightModifier = if (isSelected) {
        Modifier.background(selectedColor.copy(alpha = 0.3f), CircleShape)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .size(NavItemSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .then(highlightModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(screen.titleResId),
            tint = iconColor,
            modifier = Modifier.size(NavIconSize)
        )
    }
}