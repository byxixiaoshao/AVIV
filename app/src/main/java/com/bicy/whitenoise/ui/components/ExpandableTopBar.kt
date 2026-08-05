package com.bicy.whitenoise.ui.components

import android.graphics.BitmapFactory
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.bicy.whitenoise.ui.components.InteractiveSlider
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bicy.whitenoise.R
import com.bicy.whitenoise.music.AlbumArtCache
import com.bicy.whitenoise.music.MusicLibraryPart.MusicLibrary
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.music.MusicPlayerState
import com.bicy.whitenoise.music.MusicRepeatMode
import com.bicy.whitenoise.music.MusicShuffleMode
import com.bicy.whitenoise.music.MusicLibraryPart.MusicTrack
import com.bicy.whitenoise.music.MusicLibraryPart.ScanProgress

import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.DecelerateEasing
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.MainAlbumIconSize
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.MainAlbumSize
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.MainControlSize
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.MainPlayButtonSize
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.MainTitleFontSize
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.MusicCategory
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.PanelState
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.PanelType
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.SidebarAlbumIconSize
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.SidebarAlbumSize
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.SidebarControlSize
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.SidebarPlayButtonSize
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.SidebarTitleFontSize
import com.bicy.whitenoise.ui.utils.LocalPlaylistNavigation
import com.bicy.whitenoise.ui.utils.LocalPlaylistNavigationHolder
import com.bicy.whitenoise.ui.utils.PlaylistNavigationState
import com.bicy.whitenoise.ui.utils.rememberPlaylistNavigationState
import com.bicy.whitenoise.ui.utils.ResponsiveDimensions
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.SidebarWidth
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.SlideInPanel
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.TopBarCornerRadius
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.TopBarHeight
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.CollapsedHeight
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.TopBarPaddingHorizontal
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.TopBarPaddingTop
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.TransitionProgress
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.calculateTransitionProgress
import com.bicy.whitenoise.ui.theme.ShadowConfig
import com.bicy.whitenoise.ui.theme.ThemeColorManager
import com.bicy.whitenoise.ui.theme.dropShadow
import com.bicy.whitenoise.ui.theme.MusicGradientBackground
import com.bicy.whitenoise.utils.AudioMetadataReader
import com.bicy.whitenoise.ui.components.glass.GlassMode
import com.bicy.whitenoise.ui.components.glass.GlassBoxScope
import com.bicy.whitenoise.storage.config.GlassRenderConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun ExpandableTopBar(
    onExpandProgress: (Float) -> Unit = {},
    onInteractionStateChanged: (Boolean) -> Unit = {},
    isOtherInteracting: Boolean = false,
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
    glassScope: GlassBoxScope? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current
    val isLandscape = ResponsiveDimensions.isLandscape()
    
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()

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
    
    val statusBarHeight = with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    
    val expandProgress = remember { Animatable(0f) }
    val panelProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    val playlistNavigationState = rememberPlaylistNavigationState()
    
    var isExpanded by remember { mutableStateOf(false) }
    var isInteracting by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var panelState by remember { mutableStateOf(PanelState.Main) }
    var displayedPanelType by remember { mutableStateOf<PanelType?>(null) }
    var isAnimating by remember { mutableStateOf(false) }
    var panelResetSignal by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(forceCollapse) {
        if (forceCollapse && isExpanded && !isAnimating) {
            isAnimating = true
            isExpanded = false
            scope.launch {
                expandProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                )
                panelProgress.snapTo(0f)
                onForceCollapseComplete()
                isAnimating = false
            }
            panelState = PanelState.Main
            displayedPanelType = null
            panelResetSignal++
        }
    }
    
    var forceCollapsePanel by remember { mutableStateOf(false) }

    LaunchedEffect(forceCollapsePanel) {
        if (forceCollapsePanel && panelState != PanelState.Main && !isAnimating) {
            isAnimating = true
            panelState = PanelState.Main
            scope.launch {
                panelProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(300, easing = DecelerateEasing)
                )
                isAnimating = false
            }
            displayedPanelType = null
            panelResetSignal++
            forceCollapsePanel = false
        }
    }

    // 横竖屏切换时：重置面板状态，避免横屏的Mixer/Playlist状态影响竖屏的手势处理
    LaunchedEffect(isLandscape) {
        if (panelState != PanelState.Main || displayedPanelType != null || panelProgress.value > 0.1f) {
            panelState = PanelState.Main
            displayedPanelType = null
            panelResetSignal++
            scope.launch {
                panelProgress.snapTo(0f)
            }
        }
    }

    val navBgColor = themeColor.navBg

    val rawProgress = expandProgress.value
    val progress = if (rawProgress.isNaN()) 0f else rawProgress.coerceIn(0f, 1f)

    BackHandler(enabled = progress > 0.1f && panelState != PanelState.Main && !playlistNavigationState.hasSubPage && !isAnimating) {
        forceCollapsePanel = true
    }

    BackHandler(enabled = progress > 0.1f && panelState == PanelState.Playlist && playlistNavigationState.hasSubPage && !isAnimating) {
        playlistNavigationState.onNavigateBack?.invoke()
    }

    // 横屏时：宽度变化（从CollapsedHeight扩展到screenWidth）
    // 竖屏时：高度变化（从TopBarHeight扩展到screenHeight）
    val currentWidth = remember(progress, screenWidth, isLandscape) {
        if (isLandscape) {
            val collapsedWidth = CollapsedHeight
            val value = collapsedWidth + (screenWidth - collapsedWidth) * progress
            if (value.value.isNaN() || value < collapsedWidth) collapsedWidth else value
        } else {
            screenWidth
        }
    }
    val currentHeight = remember(progress, screenHeight, isLandscape) {
        if (isLandscape) {
            screenHeight
        } else {
            val value = TopBarHeight + (screenHeight - TopBarHeight) * progress
            if (value.value.isNaN() || value < TopBarHeight) TopBarHeight else value
        }
    }

    val currentCornerRadius = remember(progress) {
        val value = TopBarCornerRadius * (1f - progress)
        if (value.value.isNaN() || value < 0.dp) 0.dp else value
    }

    // 根据动态的currentCornerRadius构建glassModifier（当glassScope可用时）
    val glassModifier = if (glassScope != null && glassMode != GlassMode.OFF) {
        remember(currentCornerRadius, glassScale, glassBlur, glassCenterDistortion,
                 glassElevation, glassDarkness, glassWarpEdges) {
            glassScope.run {
                Modifier.glassBackground(
                    id = 1L, // 使用不同的id避免与bottom_nav冲突
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

    val currentStatusBarPadding = remember(progress, statusBarHeight, isLandscape) {
        if (isLandscape) {
            val value = TopBarPaddingHorizontal * (1f - progress)
            if (value.value.isNaN() || value < 0.dp) 0.dp else value
        } else {
            // 展开时保留 statusBarHeight，防止面板内容与摄像头/刘海重合
            val value = statusBarHeight + (TopBarHeight * (1f - progress))
            if (value.value.isNaN() || value < statusBarHeight) statusBarHeight else value
        }
    }

    val currentPaddingHorizontal = remember(progress, isLandscape) {
        if (isLandscape) {
            val value = TopBarPaddingTop * (1f - progress)
            if (value.value.isNaN() || value < 0.dp) 0.dp else value
        } else {
            val value = TopBarPaddingHorizontal * (1f - progress)
            if (value.value.isNaN() || value < 0.dp) 0.dp else value
        }
    }
    val currentPaddingTop = remember(progress, isLandscape) {
        if (isLandscape) {
            val value = TopBarPaddingHorizontal * (1f - progress)
            if (value.value.isNaN() || value < 0.dp) 0.dp else value
        } else {
            val value = TopBarPaddingTop * (1f - progress)
            if (value.value.isNaN() || value < 0.dp) 0.dp else value
        }
    }
    
    val contentAlpha = remember(progress) { (1f - progress).coerceIn(0f, 1f) }
    val panelAlpha = remember(progress) { progress.coerceIn(0f, 1f) }
    
    val zIndex = if (isInteracting || isExpanded) 2f else if (isOtherInteracting) 0f else 1f
    
    onExpandProgress(progress)
    
    CompositionLocalProvider(
        LocalPlaylistNavigation provides playlistNavigationState.toState(),
        LocalPlaylistNavigationHolder provides playlistNavigationState
    ) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isLandscape) {
                        // 横屏时：左侧边栏，从左边开始
                        Modifier.padding(start = currentStatusBarPadding)
                    } else {
                        // 竖屏时：顶部栏，从顶部开始
                        Modifier.padding(top = currentStatusBarPadding)
                    }
                )
                .then(
                    if (isLandscape) {
                        // 横屏时：上下padding
                        Modifier.padding(vertical = currentPaddingHorizontal)
                    } else {
                        // 竖屏时：顶部额外padding
                        Modifier.padding(top = currentPaddingTop)
                    }
                )
                .padding(horizontal = if (isLandscape) 0.dp else currentPaddingHorizontal),
            contentAlignment = if (isLandscape) Alignment.CenterStart else Alignment.TopCenter
        ) {
            Box(
            modifier = Modifier
                .then(
                    if (isLandscape) {
                        // 横屏时：宽度变化，高度固定
                        Modifier.width(currentWidth).height(currentHeight)
                    } else {
                        // 竖屏时：宽度固定，高度变化
                        Modifier.fillMaxWidth().height(currentHeight)
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
                    // 播放列表/调音台模式时禁用手势检测，让输入框、下拉菜单正常响应
                    .pointerInput(panelState, isLandscape) {
                        if (isLandscape) {
                            // 横屏时：使用左右滑动
                            detectDragGestures(
                                onDragStart = {
                                    dragOffset = 0f
                                    isInteracting = true
                                    onInteractionStateChanged(true)
                                    scope.launch {
                                        expandProgress.stop()
                                        panelProgress.stop()
                                    }
                                },
                                onDragEnd = {
                                    isInteracting = false
                                    onInteractionStateChanged(false)

                                    val threshold = with(density) { 50.dp.toPx() }
                                    val currentProgress = expandProgress.value

                                    if (abs(dragOffset) > threshold) {
                                        // 横屏时左侧边栏：向右滑动（正方向）展开，向左滑动（负方向）收缩
                                        val targetValue = if (dragOffset > 0) 1f else 0f
                                        scope.launch {
                                            expandProgress.animateTo(
                                                targetValue = targetValue,
                                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        isExpanded = dragOffset > 0
                                    } else {
                                        val targetValue = if (currentProgress > 0.5f) 1f else 0f
                                        scope.launch {
                                            expandProgress.animateTo(
                                                targetValue = targetValue,
                                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        isExpanded = currentProgress > 0.5f
                                    }
                                    dragOffset = 0f
                                },
                                onDrag = { _, dragAmount ->
                                    // 横屏时：只处理水平滑动（左右）
                                    val isHorizontal = abs(dragAmount.x) > abs(dragAmount.y)
                                    if (isHorizontal) {
                                        dragOffset += dragAmount.x
                                        val totalDragNeeded = with(density) {
                                            screenWidth.toPx() - CollapsedHeight.toPx()
                                        }
                                        val currentProgress = expandProgress.value
                                        val newProgress = (currentProgress + dragAmount.x / totalDragNeeded).coerceIn(0f, 1f)
                                        scope.launch {
                                            expandProgress.snapTo(newProgress)
                                        }
                                    }
                                }
                            )
                        } else {
                            // 竖屏时手势检测
                            if (panelState == PanelState.Main) {
                                // Main 模式：正常检测拖拽手势
                                detectDragGestures(
                                    onDragStart = {
                                        dragOffset = 0f
                                        isInteracting = true
                                        onInteractionStateChanged(true)
                                        scope.launch {
                                            expandProgress.stop()
                                            panelProgress.stop()
                                        }
                                    },
                                    onDragEnd = {
                                        isInteracting = false
                                        onInteractionStateChanged(false)

                                        val threshold = with(density) { 50.dp.toPx() }
                                        val currentProgress = expandProgress.value

                                        if (abs(dragOffset) > threshold) {
                                            val targetValue = if (dragOffset > 0) 1f else 0f
                                            scope.launch {
                                                expandProgress.animateTo(
                                                    targetValue = targetValue,
                                                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                                                )
                                            }
                                            isExpanded = dragOffset > 0
                                        } else {
                                            val targetValue = if (currentProgress > 0.5f) 1f else 0f
                                            scope.launch {
                                                expandProgress.animateTo(
                                                    targetValue = targetValue,
                                                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                                                )
                                            }
                                            isExpanded = currentProgress > 0.5f
                                        }
                                        dragOffset = 0f
                                    },
                                    onDrag = { _, dragAmount ->
                                        val isVertical = abs(dragAmount.y) > abs(dragAmount.x)
                                        if (isVertical) {
                                            dragOffset += dragAmount.y
                                            val totalDragNeeded = with(density) {
                                                screenHeight.toPx() - TopBarHeight.toPx()
                                            }
                                            val currentProgress = expandProgress.value
                                            val newProgress = (currentProgress + dragAmount.y / totalDragNeeded).coerceIn(0f, 1f)
                                            scope.launch {
                                                expandProgress.snapTo(newProgress)
                                            }
                                        }
                                    }
                                )
                            }
                            // 播放列表模式：不进入手势检测，事件直接传递给子组件
                        }
                    }
                    .clickable(enabled = !isExpanded) {
                        MusicPlayerController.playPause()
                    }
            ) {
                CollapsedTopBarContent(
                    alpha = contentAlpha,
                    isLandscape = isLandscape
                )
                
                // 移除高级版限制，所有用户都可以使用面板功能
                ExpandedTopBarContent(
                    alpha = panelAlpha,
                    panelProgress = panelProgress.value,
                    panelState = panelState,
                    displayedPanelType = displayedPanelType,
                    scope = scope,
                    resetSignal = panelResetSignal,
                    onPanelStateChange = { newState ->
                        if (newState == PanelState.Main) {
                            panelState = newState
                            scope.launch { panelProgress.snapTo(0f) }
                            displayedPanelType = null
                        } else {
                            val newPanelType = if (newState == PanelState.Mixer) PanelType.Mixer else PanelType.Playlist
                            displayedPanelType = newPanelType
                            panelState = newState
                            scope.launch { panelProgress.snapTo(1f) }
                        }
                    }
                )
            }
        }
    }
    }
}
}

@Composable
private fun CollapsedTopBarContent(
    alpha: Float,
    isLandscape: Boolean = false
) {
    val playerState by MusicPlayerController.state.collectAsState()
    val currentTrack = playerState.currentTrack
    val isPlaying = playerState.isPlaying

    val infiniteTransition = rememberInfiniteTransition(label = "audioVisualizer")

    val barCount = 4

    val barPhases = List(barCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (isPlaying) 1f else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600 + index * 100,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    val barColors = MaterialTheme.colorScheme.primary
    val trackName = currentTrack?.title ?: stringResource(R.string.still_empty)

    if (isLandscape) {
        // 横屏时：纵向布局
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .width(20.dp)
                    .height(36.dp)
            ) {
                if (size.width.isNaN() || size.height.isNaN() || size.width <= 0f || size.height <= 0f) {
                    return@Canvas
                }

                val barWidth = 3.dp.toPx()
                val barSpacing = (size.height - barWidth * barCount) / (barCount - 1)
                val maxBarHeight = size.width * 0.8f
                val minBarHeight = size.width * 0.2f
                val centerX = size.width / 2

                for (i in 0 until barCount) {
                    val phase by barPhases[i]
                    val progress = phase

                    if (progress.isNaN()) continue

                    val barHeight = minBarHeight + (maxBarHeight - minBarHeight) * progress

                    val yPos = i * (barWidth + barSpacing)

                    drawRect(
                        color = barColors.copy(alpha = alpha),
                        topLeft = Offset(centerX - barHeight / 2, yPos),
                        size = Size(barHeight, barWidth)
                    )
                }

            }
        }
    } else {
        // 竖屏时：横向布局
        Row(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = trackName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Canvas(
                modifier = Modifier
                    .width(36.dp)
                    .height(20.dp)
            ) {
                if (size.width.isNaN() || size.height.isNaN() || size.width <= 0f || size.height <= 0f) {
                    return@Canvas
                }

                val barWidth = 3.dp.toPx()
                val barSpacing = (size.width - barWidth * barCount) / (barCount - 1)
                val maxBarHeight = size.height * 0.8f
                val minBarHeight = size.height * 0.2f
                val centerY = size.height / 2

                for (i in 0 until barCount) {
                    val phase by barPhases[i]
                    val progress = phase

                    if (progress.isNaN()) continue

                    val barHeight = minBarHeight + (maxBarHeight - minBarHeight) * progress

                    val xPos = i * (barWidth + barSpacing)

                    drawRect(
                        color = barColors.copy(alpha = alpha),
                        topLeft = Offset(xPos, centerY - barHeight / 2),
                        size = Size(barWidth, barHeight)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedTopBarContent(
    alpha: Float,
    panelProgress: Float,
    panelState: PanelState,
    displayedPanelType: PanelType?,
    scope: CoroutineScope,
    resetSignal: Int = 0,
    onPanelStateChange: (PanelState) -> Unit
) {
    val playerState by MusicPlayerController.state.collectAsState()
    val currentTrack = playerState.currentTrack
    val tracks by MusicLibrary.tracks.collectAsState()
    val scanProgress by MusicLibrary.scanProgress.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    var isCompactSpacing by remember { mutableStateOf(false) }
    var expandedPanel by remember { mutableStateOf<PanelType?>(null) }
    var panelTransitionProgress by remember { mutableFloatStateOf(0f) }
    var targetPanelType by remember { mutableStateOf<PanelType?>(null) }
    var isPanelTransitioning by remember { mutableStateOf(false) }
    
    // 当顶栏收起或横竖屏切换时，重置所有局部状态
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            isCompactSpacing = false
            expandedPanel = null
            panelTransitionProgress = 0f
            isPanelTransitioning = false
            targetPanelType = null
        }
    }
    
    val animatedPanelTransitionProgress by animateFloatAsState(
        targetValue = panelTransitionProgress,
        animationSpec = tween<Float>(durationMillis = 300),
        finishedListener = {
            if (isPanelTransitioning && panelTransitionProgress == 1f) {
                onPanelStateChange(if (targetPanelType == PanelType.Mixer) PanelState.Mixer else PanelState.Playlist)
                isPanelTransitioning = false
            }
        }
    )
    
    LaunchedEffect(tracks) {
        if (tracks.isNotEmpty() && playerState.playlist.isEmpty()) {
            MusicPlayerController.setPlaylist(tracks, 0)
        }
    }
    
    val safePanelProgress = if (panelProgress.isNaN()) 0f else panelProgress.coerceIn(0f, 1f)
    val isOnRight = targetPanelType == PanelType.Mixer
    val isPanelMode = displayedPanelType != null
    
    val transition = calculateTransitionProgress(safePanelProgress, isOnRight, screenWidth, screenHeight)
    
    val shouldShowMainContent = !isPanelMode || safePanelProgress < 0.5f
    val shouldShowPanelTransition = isPanelTransitioning || animatedPanelTransitionProgress > 0.01f
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
    ) {
        if (alpha > 0.1f) {
            MusicGradientBackground(
                isPlaying = playerState.isPlaying,
                modifier = Modifier.fillMaxSize()
            )
            
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (shouldShowMainContent || shouldShowPanelTransition) {
                    TransitioningMainContent(
                        track = currentTrack,
                        isPlaying = playerState.isPlaying,
                        position = playerState.position,
                        duration = playerState.duration,
                        repeatMode = playerState.repeatMode,
                        shuffleMode = playerState.shuffleMode,
                        isScanning = scanProgress.isScanning,
                        transition = transition,
                        isOnRight = isOnRight,
                        panelTransitionProgress = animatedPanelTransitionProgress,
                        isCompactSpacing = isCompactSpacing,
                        onCompactSpacingChange = { isCompactSpacing = it },
                        expandedPanel = expandedPanel,
                        onExpandedPanelChange = { expandedPanel = it },
                        tracks = tracks,
                        currentTrack = currentTrack,
                        scanProgress = scanProgress,
                        playlist = playerState.playlist,
                        playlistIndex = playerState.playlistIndex,
                        onOpenMixer = {
                            isCompactSpacing = !isCompactSpacing
                            expandedPanel = if (expandedPanel == PanelType.Mixer) null else PanelType.Mixer
                        },
                        onOpenPlaylist = {
                            isCompactSpacing = !isCompactSpacing
                            expandedPanel = if (expandedPanel == PanelType.Playlist) null else PanelType.Playlist
                        },
                        onBackToMain = { onPanelStateChange(PanelState.Main) },
                        onPanelTransitionBack = {
                            isCompactSpacing = false
                            expandedPanel = null
                            panelTransitionProgress = 0f
                            isPanelTransitioning = false
                            targetPanelType = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                if (shouldShowPanelTransition && targetPanelType != null) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (isOnRight) {
                            SlideInPanel(
                                panelProgress = animatedPanelTransitionProgress,
                                slideProgress = animatedPanelTransitionProgress,
                                isOnRight = true,
                                content = {
                                    /* MixerPanel(
                                        panelProgress = animatedPanelTransitionProgress,
                                        modifier = Modifier.fillMaxSize()
                                    ) */
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .width(SidebarWidth)
                                    .fillMaxHeight()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .width(SidebarWidth)
                                    .fillMaxHeight()
                            )
                            SlideInPanel(
                                panelProgress = animatedPanelTransitionProgress,
                                slideProgress = animatedPanelTransitionProgress,
                                isOnRight = false,
                                content = {
                                    /* PlaylistPanel(
                                        tracks = tracks,
                                        currentTrack = currentTrack,
                                        isScanning = scanProgress.isScanning,
                                        panelProgress = animatedPanelTransitionProgress,
                                        modifier = Modifier.fillMaxSize()
                                    ) */
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                if (isPanelMode && !shouldShowPanelTransition) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (isOnRight) {
                            SlideInPanel(
                                panelProgress = safePanelProgress,
                                slideProgress = transition.panelSlide,
                                isOnRight = true,
                                content = {
                                    /* MixerPanel(
                                        panelProgress = safePanelProgress,
                                        modifier = Modifier.fillMaxSize()
                                    ) */
                                },
                                modifier = Modifier.weight(1f)
                            )
                            TransitioningSidebarContent(
                                track = currentTrack,
                                isPlaying = playerState.isPlaying,
                                position = playerState.position,
                                duration = playerState.duration,
                                transition = transition,
                                isOnRight = true,
                                onBackToMain = { onPanelStateChange(PanelState.Main) },
                                modifier = Modifier.width(SidebarWidth)
                            )
                        } else {
                            TransitioningSidebarContent(
                                track = currentTrack,
                                isPlaying = playerState.isPlaying,
                                position = playerState.position,
                                duration = playerState.duration,
                                transition = transition,
                                isOnRight = false,
                                onBackToMain = { onPanelStateChange(PanelState.Main) },
                                modifier = Modifier.width(SidebarWidth)
                            )
                            SlideInPanel(
                                panelProgress = safePanelProgress,
                                slideProgress = transition.panelSlide,
                                isOnRight = false,
                                content = {
                                    /* PlaylistPanel(
                                        tracks = tracks,
                                        currentTrack = currentTrack,
                                        isScanning = scanProgress.isScanning,
                                        panelProgress = safePanelProgress,
                                        modifier = Modifier.fillMaxSize()
                                    ) */
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitioningMainContent(
    track: MusicTrack?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    repeatMode: MusicRepeatMode,
    shuffleMode: MusicShuffleMode,
    isScanning: Boolean,
    transition: TransitionProgress,
    isOnRight: Boolean,
    panelTransitionProgress: Float = 0f,
    isCompactSpacing: Boolean = false,
    onCompactSpacingChange: (Boolean) -> Unit = {},
    expandedPanel: PanelType? = null,
    onExpandedPanelChange: (PanelType?) -> Unit = {},
    tracks: List<MusicTrack> = emptyList(),
    currentTrack: MusicTrack?,
    scanProgress: ScanProgress,
    playlist: List<MusicTrack>,
    playlistIndex: Int,
    onOpenMixer: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onBackToMain: () -> Unit,
    onPanelTransitionBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    val isPanelTransition = panelTransitionProgress > 0.01f || expandedPanel != null
    
    val buttonSpacing by animateDpAsState(
        targetValue = if (isCompactSpacing) 12.dp else 24.dp,
        animationSpec = tween<Dp>(
            durationMillis = 300,
            delayMillis = if (isCompactSpacing) 0 else 250
        )
    )
    val controlSize by animateDpAsState(
        targetValue = if (isCompactSpacing) SidebarControlSize else MainControlSize,
        animationSpec = tween<Dp>(
            durationMillis = 300,
            delayMillis = if (isCompactSpacing) 0 else 250
        )
    )
    val albumSize by animateDpAsState(
        targetValue = if (isCompactSpacing) SidebarAlbumSize else MainAlbumSize,
        animationSpec = tween<Dp>(
            durationMillis = 300,
            delayMillis = if (isCompactSpacing) 0 else 100
        )
    )
    val albumIconSize by animateDpAsState(
        targetValue = if (isCompactSpacing) SidebarAlbumIconSize else MainAlbumIconSize,
        animationSpec = tween<Dp>(
            durationMillis = 300,
            delayMillis = if (isCompactSpacing) 0 else 100
        )
    )
    val titleFontSize by animateFloatAsState(
        targetValue = if (isCompactSpacing) SidebarTitleFontSize.value else MainTitleFontSize.value,
        animationSpec = tween<Float>(
            durationMillis = 300,
            delayMillis = if (isCompactSpacing) 0 else 100
        )
    )
    val artistFontSize by animateFloatAsState(
        targetValue = if (isCompactSpacing) SidebarTitleFontSize.value * 0.7f else MainTitleFontSize.value * 0.7f,
        animationSpec = tween<Float>(
            durationMillis = 300,
            delayMillis = if (isCompactSpacing) 0 else 100
        )
    )
    val progressAlpha by animateFloatAsState(
        targetValue = if (isCompactSpacing) 0f else 1f,
        animationSpec = tween<Float>(
            durationMillis = 300,
            delayMillis = if (isCompactSpacing) 0 else 50
        )
    )
    val progressHeight by animateDpAsState(
        targetValue = if (isCompactSpacing) 0.dp else 80.dp,
        animationSpec = tween<Dp>(
            durationMillis = 300,
            delayMillis = if (isCompactSpacing) 0 else 50
        )
    )
    val progressWidth by animateFloatAsState(
        targetValue = if (isCompactSpacing) 0f else 1f,
        animationSpec = tween<Float>(
            durationMillis = 300,
            delayMillis = if (isCompactSpacing) 0 else 50
        )
    )
    val featureButtonsAlpha by animateFloatAsState(
        targetValue = if (isCompactSpacing) 0f else 1f,
        animationSpec = tween<Float>(
            durationMillis = 300,
            delayMillis = if (isCompactSpacing) 0 else 250
        )
    )
    
    val shouldHideExtraButtons = transition.panelSlide > 0.3f || isCompactSpacing
    
    val shuffleButtonAlpha by animateFloatAsState(
        targetValue = if (shouldHideExtraButtons) 0f else 1f,
        animationSpec = tween<Float>(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        )
    )
    val repeatButtonAlpha by animateFloatAsState(
        targetValue = if (shouldHideExtraButtons) 0f else 1f,
        animationSpec = tween<Float>(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        )
    )
    
    val panelAlbumSize by animateDpAsState(
        targetValue = if (isPanelTransition) SidebarAlbumSize else MainAlbumSize,
        animationSpec = tween<Dp>(durationMillis = 300)
    )
    val panelAlbumIconSize by animateDpAsState(
        targetValue = if (isPanelTransition) SidebarAlbumIconSize else MainAlbumIconSize,
        animationSpec = tween<Dp>(durationMillis = 300)
    )
    val panelTitleFontSize by animateFloatAsState(
        targetValue = if (isPanelTransition) SidebarTitleFontSize.value else MainTitleFontSize.value,
        animationSpec = tween<Float>(durationMillis = 300)
    )
    val panelArtistFontSize by animateFloatAsState(
        targetValue = if (isPanelTransition) SidebarTitleFontSize.value * 0.7f else MainTitleFontSize.value * 0.7f,
        animationSpec = tween<Float>(durationMillis = 300)
    )
    val panelProgressAlpha by animateFloatAsState(
        targetValue = if (isPanelTransition) 0f else 1f,
        animationSpec = tween<Float>(durationMillis = 300)
    )
    val panelProgressHeight by animateDpAsState(
        targetValue = if (isPanelTransition) 0.dp else 80.dp,
        animationSpec = tween<Dp>(durationMillis = 300)
    )
    val panelProgressWidth by animateFloatAsState(
        targetValue = if (isPanelTransition) 0f else 1f,
        animationSpec = tween<Float>(durationMillis = 300)
    )
    val panelFeatureButtonsAlpha by animateFloatAsState(
        targetValue = if (panelTransitionProgress > 0.01f) 0f else 1f,
        animationSpec = tween<Float>(durationMillis = 300)
    )
    val panelShuffleButtonAlpha by animateFloatAsState(
        targetValue = if (isPanelTransition) 0f else 1f,
        animationSpec = tween<Float>(durationMillis = 300)
    )
    val panelRepeatButtonAlpha by animateFloatAsState(
        targetValue = if (isPanelTransition) 0f else 1f,
        animationSpec = tween<Float>(durationMillis = 300)
    )
    val panelControlSize by animateDpAsState(
        targetValue = if (isPanelTransition) SidebarControlSize else MainControlSize,
        animationSpec = tween<Dp>(durationMillis = 300)
    )
    val panelButtonSpacing by animateDpAsState(
        targetValue = if (isCompactSpacing || isPanelTransition) 16.dp else 48.dp,
        animationSpec = tween<Dp>(durationMillis = 300)
    )
    
    val leftPanelWeight by animateFloatAsState(
        targetValue = if (expandedPanel == PanelType.Mixer) 5f else 0f,
        animationSpec = tween<Float>(durationMillis = 300)
    )
    
    val rightPanelWeight by animateFloatAsState(
        targetValue = if (expandedPanel == PanelType.Playlist) 5f else 0f,
        animationSpec = tween<Float>(durationMillis = 300)
    )
    
    val leftPanelAlpha by animateFloatAsState(
        targetValue = if (expandedPanel == PanelType.Mixer) 1f else 0f,
        animationSpec = tween<Float>(durationMillis = 300)
    )
    
    val rightPanelAlpha by animateFloatAsState(
        targetValue = if (expandedPanel == PanelType.Playlist) 1f else 0f,
        animationSpec = tween<Float>(durationMillis = 300)
    )
    
    val panelProgressBarScale by animateFloatAsState(
        targetValue = if (expandedPanel != null) 1f else 0f,
        animationSpec = tween<Float>(durationMillis = 300)
    )
    
    val albumTransition = transition.album
    val titleTransition = transition.title
    val playButtonTransition = transition.playButton
    val prevButtonTransition = transition.prevButton
    val nextButtonTransition = transition.nextButton
    val shuffleButtonTransition = transition.shuffleButton
    val repeatButtonTransition = transition.repeatButton
    val horizontalProgressTransition = transition.horizontalProgress
    val verticalProgressTransition = transition.verticalProgress
    val featureButtonsTransition = transition.featureButtons
    
    val effectiveAlbumSize = if (isPanelTransition) panelAlbumSize else albumSize
    val effectiveAlbumIconSize = if (isPanelTransition) panelAlbumIconSize else albumIconSize
    val effectiveTitleFontSize = if (isPanelTransition) panelTitleFontSize else titleFontSize
    val effectiveArtistFontSize = if (isPanelTransition) panelArtistFontSize else artistFontSize
    val effectiveProgressAlpha = if (isPanelTransition) panelProgressAlpha else progressAlpha
    val effectiveProgressHeight = if (isPanelTransition) panelProgressHeight else progressHeight
    val effectiveProgressWidth = if (isPanelTransition) panelProgressWidth else progressWidth
    val effectiveFeatureButtonsAlpha = if (isPanelTransition) panelFeatureButtonsAlpha else featureButtonsAlpha
    val effectiveShuffleButtonAlpha = if (isPanelTransition) panelShuffleButtonAlpha else shuffleButtonAlpha
    val effectiveRepeatButtonAlpha = if (isPanelTransition) panelRepeatButtonAlpha else repeatButtonAlpha
    val effectiveControlSize = if (isPanelTransition) panelControlSize else controlSize
    val effectiveButtonSpacing = panelButtonSpacing

    // 专辑封面：观察 AlbumArtCache，曲目切换时按需加载（内存→磁盘→内嵌→在线占位）
    val trackId = track?.id
    val artMap by AlbumArtCache.artFlow.collectAsState()
    val albumArtBytes = trackId?.let { artMap[it] } ?: trackId?.let { AlbumArtCache.getCached(it) }
    LaunchedEffect(trackId) {
        if (trackId != null && albumArtBytes == null && !artMap.containsKey(trackId)) {
            AlbumArtCache.requestAlbumArt(track)
        }
    }
    val albumBitmap = remember(albumArtBytes) {
        albumArtBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .graphicsLayer {
                clip = true
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            if (leftPanelWeight > 0.01f) {
                Box(
                    modifier = Modifier
                        .weight(leftPanelWeight)
                        .fillMaxHeight()
                        .alpha(leftPanelAlpha)
                ) {
                    MixerPanel(
                        panelProgress = 1f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            if (leftPanelWeight > 0.01f && panelProgressBarScale > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .graphicsLayer {
                            scaleY = panelProgressBarScale
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                ) {
                    VerticalProgressBar(
                        progress = if (duration > 0) position.toFloat() / duration else 0f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(screenWidth)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = albumTransition.scale
                        scaleY = albumTransition.scale
                        translationX = albumTransition.offsetX
                        translationY = albumTransition.offsetY
                        alpha = albumTransition.alpha
                    }
                    .size(effectiveAlbumSize)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .then(
                        if (isCompactSpacing) {
                            Modifier.clickable { 
                                onCompactSpacingChange(false)
                                if (expandedPanel != null) {
                                    onExpandedPanelChange(null)
                                }
                            }
                        } else if (expandedPanel != null) {
                            Modifier.clickable { onExpandedPanelChange(null) }
                        } else if (isPanelTransition) {
                            Modifier.clickable { onPanelTransitionBack() }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (albumBitmap != null) {
                    Image(
                        bitmap = albumBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(effectiveAlbumIconSize)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = titleTransition.scale
                        scaleY = titleTransition.scale
                        translationX = titleTransition.offsetX
                        translationY = titleTransition.offsetY
                        alpha = titleTransition.alpha
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (track != null) {
                    Text(
                        text = track.title,
                        fontSize = effectiveTitleFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    track.artist?.let { artist ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = artist,
                            fontSize = effectiveArtistFontSize.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = if (isScanning) stringResource(R.string.scanning_music) else stringResource(R.string.still_empty),
                        fontSize = effectiveTitleFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (effectiveShuffleButtonAlpha > 0.01f) {
                    IconButton(
                        onClick = { MusicPlayerController.toggleShuffleMode() },
                        modifier = Modifier
                            .size(effectiveControlSize)
                            .graphicsLayer {
                                scaleX = shuffleButtonTransition.scale
                                scaleY = shuffleButtonTransition.scale
                                translationX = shuffleButtonTransition.offsetX
                                translationY = shuffleButtonTransition.offsetY
                                alpha = shuffleButtonTransition.alpha * effectiveShuffleButtonAlpha
                            }
                    ) {
                        Icon(
                            imageVector = if (shuffleMode == MusicShuffleMode.ON) Icons.Default.ShuffleOn else Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleMode == MusicShuffleMode.ON)
                                MaterialTheme.colorScheme.primary
                                else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(effectiveControlSize)
                        )
                    }
                }

                if (effectiveShuffleButtonAlpha > 0.01f) {
                    Spacer(modifier = Modifier.width(effectiveButtonSpacing))
                }
                
                IconButton(
                    onClick = { MusicPlayerController.previous() },
                    modifier = Modifier
                        .size(effectiveControlSize)
                        .graphicsLayer {
                            // 统一缩放锚点：所有按钮以自身中心缩放
                            transformOrigin = TransformOrigin.Center
                            scaleX = prevButtonTransition.scale
                            scaleY = prevButtonTransition.scale
                            translationX = prevButtonTransition.offsetX
                            translationY = prevButtonTransition.offsetY
                            alpha = prevButtonTransition.alpha
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(effectiveControlSize)
                    )
                }
                
                Spacer(modifier = Modifier.width(effectiveButtonSpacing))
                
                IconButton(
                    onClick = { MusicPlayerController.playPause() },
                    modifier = Modifier
                        .size(effectiveControlSize)
                        .graphicsLayer {
                            // 播放按钮以自身中心缩放
                            transformOrigin = TransformOrigin.Center
                            scaleX = playButtonTransition.scale
                            scaleY = playButtonTransition.scale
                            translationX = playButtonTransition.offsetX
                            translationY = playButtonTransition.offsetY
                            alpha = playButtonTransition.alpha
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(effectiveControlSize)
                    )
                }
                
                Spacer(modifier = Modifier.width(effectiveButtonSpacing))
                
                IconButton(
                    onClick = { MusicPlayerController.next() },
                    modifier = Modifier
                        .size(effectiveControlSize)
                        .graphicsLayer {
                            // 统一缩放锚点：所有按钮以自身中心缩放
                            transformOrigin = TransformOrigin.Center
                            scaleX = nextButtonTransition.scale
                            scaleY = nextButtonTransition.scale
                            translationX = nextButtonTransition.offsetX
                            translationY = nextButtonTransition.offsetY
                            alpha = nextButtonTransition.alpha
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(effectiveControlSize)
                    )
                }
                
                if (effectiveRepeatButtonAlpha > 0.01f) {
                    Spacer(modifier = Modifier.width(effectiveButtonSpacing))
                
                    IconButton(
                        onClick = { MusicPlayerController.toggleRepeatMode() },
                        modifier = Modifier
                            .size(effectiveControlSize)
                            .graphicsLayer {
                                scaleX = repeatButtonTransition.scale
                                scaleY = repeatButtonTransition.scale
                                translationX = repeatButtonTransition.offsetX
                                translationY = repeatButtonTransition.offsetY
                                alpha = repeatButtonTransition.alpha * effectiveRepeatButtonAlpha
                            }
                    ) {
                        Icon(
                            imageVector = when (repeatMode) {
                                MusicRepeatMode.OFF -> Icons.Default.Repeat
                                MusicRepeatMode.ALL -> Icons.Default.RepeatOn
                                MusicRepeatMode.ONE -> Icons.Default.RepeatOneOn
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != MusicRepeatMode.OFF)
                                MaterialTheme.colorScheme.primary
                                else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(effectiveControlSize)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .graphicsLayer {
                        alpha = featureButtonsTransition.alpha * effectiveFeatureButtonsAlpha
                    },
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (expandedPanel != PanelType.Mixer) {
                    IconButton(
                        onClick = {
                        onCompactSpacingChange(true)
                        onExpandedPanelChange(PanelType.Mixer)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.mixer),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 胶囊状进度条（位于调音台按钮和播放列表按钮中间）
            if (horizontalProgressTransition.alpha > 0.01f && effectiveProgressHeight > 20.dp) {
                Slider(
                    value = if (duration > 0) position.toFloat() else 0f,
                    onValueChange = { if (track != null) MusicPlayerController.seekTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            alpha = horizontalProgressTransition.alpha * effectiveProgressAlpha
                        },
                    enabled = track != null,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent, // 隐藏thumb实现胶囊状
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )
            }

            if (expandedPanel != PanelType.Playlist) {
                    IconButton(
                        onClick = {
                            onCompactSpacingChange(true)
                            onExpandedPanelChange(PanelType.Playlist)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.playlist),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

            }
                    }
                }
            }
            
            if (rightPanelWeight > 0.01f && panelProgressBarScale > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .graphicsLayer {
                            scaleY = panelProgressBarScale
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                ) {
                    VerticalProgressBar(
                        progress = if (duration > 0) position.toFloat() / duration else 0f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            if (rightPanelWeight > 0.01f) {
                Box(
                    modifier = Modifier
                        .weight(rightPanelWeight)
                        .fillMaxHeight()
                        .alpha(rightPanelAlpha)
                ) {
                    PlaylistPanel(
                        tracks = tracks,
                        currentTrack = currentTrack,
                        isScanning = scanProgress.isScanning,
                        panelProgress = 1f,
                        playlist = playlist,
                        playlistIndex = playlistIndex,
                        onBack = {
                            // 先恢复主体（退出紧凑模式），再关闭面板
                            // 顺序与专辑封面点击保持一致
                            onCompactSpacingChange(false)
                            if (expandedPanel != null) {
                                onExpandedPanelChange(null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        if (verticalProgressTransition.alpha > 0.01f) {
            VerticalProgressBar(
                progress = if (duration > 0) position.toFloat() / duration else 0f,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .alpha(verticalProgressTransition.alpha)
                    .align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun TransitioningSidebarContent(
    track: MusicTrack?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    transition: TransitionProgress,
    isOnRight: Boolean,
    onBackToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sidebarAlpha = transition.verticalProgress.alpha
    
    if (sidebarAlpha <= 0f) {
        return
    }
    
    Row(
        modifier = modifier
            .fillMaxHeight()
            .alpha(sidebarAlpha),
        horizontalArrangement = if (isOnRight) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isOnRight) {
            VerticalProgressBar(
                progress = if (duration > 0) position.toFloat() / duration else 0f,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
            )
        }
        
        Column(
            modifier = Modifier
                .width(SidebarWidth - 8.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(SidebarAlbumSize)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .clickable(onClick = onBackToMain),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "返回播放页面",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(SidebarAlbumIconSize)
                )
            }
            
            Spacer(modifier = Modifier.height(7.dp))
            
            if (track != null) {
                Text(
                    text = track.title,
                    fontSize = SidebarTitleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(5.dp))
            
            IconButton(
                onClick = { MusicPlayerController.previous() },
                modifier = Modifier.size(SidebarPlayButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(SidebarControlSize)
                )
            }
            
            IconButton(
                onClick = { MusicPlayerController.playPause() },
                modifier = Modifier
                    .size(SidebarPlayButtonSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(SidebarControlSize)
                )
            }
            
            IconButton(
                onClick = { MusicPlayerController.next() },
                modifier = Modifier.size(SidebarPlayButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(SidebarControlSize)
                )
            }
        }
        
        if (!isOnRight) {
            VerticalProgressBar(
                progress = if (duration > 0) position.toFloat() / duration else 0f,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
            )
        }
    }
}

@Composable
private fun VerticalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.VerticalProgressBar(progress, modifier)
}

@Composable
private fun MixerPanel(
    panelProgress: Float,
    modifier: Modifier = Modifier
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.MixerPanel(panelProgress, modifier)
}

@Composable
private fun TabButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.TabButton(text, icon, isSelected, onClick)
}

@Composable
private fun EqualizerPanel() {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.EqualizerPanel()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqBandSliderHorizontal(
    frequency: String,
    gain: Float,
    onGainChange: (Float) -> Unit
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.EqBandSliderHorizontal(frequency, gain, onGainChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReverbPanel() {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.ReverbPanel()
}

@Composable
private fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    subtitle: String? = null,
    warningText: String? = null,
    content: @Composable () -> Unit
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.CollapsibleSection(title, expanded, onToggle, subtitle, warningText, content)
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.PresetChip(label, selected, onClick)
}

@Composable
private fun EffectPlaceholderItem(name: String) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.EffectPlaceholderItem(name)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EffectSliderItem(
    name: String,
    intensity: Float,
    onIntensityChange: (Float) -> Unit
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.EffectSliderItem(name, intensity, onIntensityChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReverbSliderComponent(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
    steps: Int = 0
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.ReverbSliderComponent(
        label, value, valueRange, valueText, onValueChange, steps
    )
}

@Composable
private fun PlaylistPanel(
    tracks: List<MusicTrack>,
    currentTrack: MusicTrack?,
    isScanning: Boolean,
    panelProgress: Float,
    playlist: List<MusicTrack>,
    playlistIndex: Int,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.PlaylistPanel(
        tracks, currentTrack, isScanning, panelProgress, playlist, playlistIndex, modifier, onBack
    )
}

@Composable
private fun CategorySidebar(
    selectedCategory: MusicCategory,
    onCategorySelected: (MusicCategory) -> Unit
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.CategorySidebar(selectedCategory, onCategorySelected)
}

@Composable
private fun CategoryTab(
    imageVector: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.CategoryTab(imageVector, contentDescription, isSelected, onClick)
}

@Composable
private fun CategorySelectionList(
    items: List<String>,
    onItemClick: (String) -> Unit
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.CategorySelectionList(items, onItemClick)
}

@Composable
private fun TrackList(
    tracks: List<MusicTrack>,
    currentTrack: MusicTrack?,
    showFavoriteButton: Boolean = true
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.TrackList(tracks, currentTrack, showFavoriteButton)
}

@Composable
private fun PlaylistItem(
    track: MusicTrack,
    isPlaying: Boolean,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null
) {
    com.bicy.whitenoise.ui.components.ExpandableTopBarPart.PlaylistItem(track, isPlaying, isFavorite, onClick, onFavoriteClick)
}
