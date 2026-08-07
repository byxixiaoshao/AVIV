package com.bicy.whitenoise.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import com.bicy.whitenoise.R
import com.bicy.whitenoise.storage.config.NavBackgroundConfig
import com.bicy.whitenoise.ui.components.ExpandableNavBar
import com.bicy.whitenoise.ui.components.glass.GlassContainer
import com.bicy.whitenoise.ui.components.glass.GlassAlertDialog
import com.bicy.whitenoise.utils.BatteryOptimizationHelper
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.BottomNavTotalHeight
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.CollapsedMarginBottom
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.CollapsedMarginHorizontal
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.TopBarPaddingHorizontal
import com.bicy.whitenoise.ui.components.ExpandableTopBarPart.TopBarPaddingTop
import com.bicy.whitenoise.ui.components.ExpandableTopBar
import com.bicy.whitenoise.ui.tutorial.TutorialManager
import com.bicy.whitenoise.ui.tutorial.TutorialTooltip
import com.bicy.whitenoise.ui.tutorial.TooltipPosition
import com.bicy.whitenoise.ui.tutorial.TutorialTargetPosition
import com.bicy.whitenoise.ui.tutorial.tutorialTarget
import com.bicy.whitenoise.ui.viewmodel.AIChatViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bicy.whitenoise.ui.navigation.ScreenPart.*

import com.bicy.whitenoise.ui.screens.HomeScreen
import com.bicy.whitenoise.ui.screens.PlayScreen
import com.bicy.whitenoise.ui.screens.ScatteredScreen
import com.bicy.whitenoise.ui.screens.SettingScreen
import com.bicy.whitenoise.ui.screens.ChatScreen
import com.bicy.whitenoise.ui.screens.ChatStatePart.ToolConfirmDialog
import com.bicy.whitenoise.ui.theme.WhiteNoiseVisualizerBackground
import com.bicy.whitenoise.ui.utils.ResponsiveDimensions
import com.bicy.whitenoise.storage.config.LiquidGlassConfig
import com.bicy.whitenoise.storage.config.GlassRenderConfig
import com.bicy.whitenoise.storage.config.GlassScopeConfig
import com.bicy.whitenoise.ui.components.glass.GlassMode
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.bicy.whitenoise.ui.components.glass.DialogBlurState
import com.bicy.whitenoise.storage.config.BlurBackdropConfig
import com.bicy.whitenoise.storage.config.BackgroundRenderConfig
import com.bicy.whitenoise.storage.config.BackgroundGlassConfig
import com.bicy.whitenoise.storage.config.Filament3DConfig
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.ui.background.Particle3DBackground
import android.os.Build
import android.content.Context
import android.widget.Toast
import android.graphics.ColorMatrix as AndroidColorMatrix
import androidx.compose.foundation.Canvas

private const val ANIM_DURATION = 400
private val TopBarHeight = 48.dp
private val ContentPaddingTop = 8.dp
private val ContentPaddingBottom = 16.dp
val PageTopPadding = ContentPaddingTop + TopBarHeight + 24.dp
val PageBottomPadding = BottomNavTotalHeight + ContentPaddingBottom + 48.dp
private val DecelerateEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}

@Composable
fun MainScreen() {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isLandscape = ResponsiveDimensions.isLandscape()

    // 背景图片和透明度配置
    val backgroundUri by NavBackgroundConfig.backgroundUriFlow.collectAsState()
    val backgroundAlpha by NavBackgroundConfig.backgroundAlphaFlow.collectAsState()

    // 背景渲染配置
    val bgRenderEnabled by BackgroundRenderConfig.enabledFlow.collectAsState()
    val bgBrightness by BackgroundRenderConfig.brightnessFlow.collectAsState()
    val bgContrast by BackgroundRenderConfig.contrastFlow.collectAsState()
    val bgSaturation by BackgroundRenderConfig.saturationFlow.collectAsState()
    val bgHighlights by BackgroundRenderConfig.highlightsFlow.collectAsState()
    val bgShadows by BackgroundRenderConfig.shadowsFlow.collectAsState()
    val bgRenderBlur by BackgroundRenderConfig.blurFlow.collectAsState()
    val bgVignette by BackgroundRenderConfig.vignetteFlow.collectAsState()

    // 背景玻璃模糊配置
    val bgGlassEnabled by BackgroundGlassConfig.enabledFlow.collectAsState()
    val bgGlassType by BackgroundGlassConfig.typeFlow.collectAsState()
    val bgGlassBlur by BackgroundGlassConfig.blurFlow.collectAsState()
    val bgGlassOpacity by BackgroundGlassConfig.opacityFlow.collectAsState()
    val bgGlassDarkness by BackgroundGlassConfig.darknessFlow.collectAsState()
    val bgGlassNoise by BackgroundGlassConfig.noiseFlow.collectAsState()
    val bgGlassGridSize by BackgroundGlassConfig.gridSizeFlow.collectAsState()
    val bgGlassGradient by BackgroundGlassConfig.gradientFlow.collectAsState()
    val bgGlassSheen by BackgroundGlassConfig.sheenFlow.collectAsState()

    // 液态玻璃效果配置
    val glassMode by LiquidGlassConfig.modeFlow.collectAsState()
    val glassScope by GlassScopeConfig.scopeFlow.collectAsState()
    val glassScopeBottomNav = glassScope == GlassScopeConfig.SCOPE_BOTTOM_NAV || glassScope == GlassScopeConfig.SCOPE_ALL
    val glassScopeTopBar = glassScope == GlassScopeConfig.SCOPE_TOP_BAR || glassScope == GlassScopeConfig.SCOPE_ALL

    // 白噪音播放状态
    val viewModel: com.bicy.whitenoise.ui.viewmodel.MainViewModel = viewModel()
    val aiChatViewModel: AIChatViewModel = viewModel()
    val isPaused by viewModel.isPaused.collectAsState()
    val playingSounds by viewModel.playingSounds.collectAsState()
    val isWhiteNoisePlaying = playingSounds.isNotEmpty() && !isPaused

    // AI 聊天状态
    val chatState by aiChatViewModel.uiState.collectAsState()

    // 初始化 AgentService
    com.bicy.whitenoise.data.agent.AgentService.init(viewModel)

    var currentPageIndex by remember { mutableIntStateOf(0) }

    // 同步当前页面到 AgentService
    LaunchedEffect(currentPageIndex) {
        com.bicy.whitenoise.data.agent.AgentService.updateCurrentPage(currentPageIndex)
    }
    var expandProgress by remember { mutableFloatStateOf(0f) }
    var topBarExpandProgress by remember { mutableFloatStateOf(0f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isTopBarInteracting by remember { mutableStateOf(false) }
    var isNavInteracting by remember { mutableStateOf(false) }
    var forceCollapseTopBar by remember { mutableStateOf(false) }
    var forceCollapseNavBar by remember { mutableStateOf(false) }
    
    // ========== 新手教程系统 ==========
    TutorialManager.loadCompletedState()
    val tutorialActive by TutorialManager.currentTutorial.collectAsState()
    val tutorialStepIndex by TutorialManager.currentStepIndex.collectAsState()
    val tutorialPageIndex by TutorialManager.tutorialPageIndex.collectAsState()
    val tutorialForceCollapseTopBar by TutorialManager.forceCollapseTopBar.collectAsState()
    val tutorialForceCollapseNavBar by TutorialManager.forceCollapseNavBar.collectAsState()
    val tutorialCompleted by TutorialManager.tutorialCompleted.collectAsState()
    val showTutorialSample by TutorialManager.showTutorialSample.collectAsState()
    // 目标位置注册（驱动遮罩挖孔重绘）
    val tutorialTargetPositions by TutorialManager.targetPositions.collectAsState()
    // 当前步骤——用于非玻璃模式下决定哪个 bar 通过 zIndex 提升至遮罩之上
    val tutorialStep = TutorialManager.getCurrentStep()

    // 首次启动自动触发教程
    LaunchedEffect(tutorialCompleted) {
        if (!tutorialCompleted && tutorialActive == null) {
            kotlinx.coroutines.delay(1000)
            TutorialManager.startTutorial()
        }
    }

    // ========== 应用后台保活引导弹窗（首次教程完成后弹出）==========
    var showKeepAliveGuideDialog by remember { mutableStateOf(false) }
    val keepAlivePrefs = remember {
        context.getSharedPreferences("keep_alive_prefs", Context.MODE_PRIVATE)
    }

    LaunchedEffect(tutorialCompleted) {
        if (tutorialCompleted &&
            !keepAlivePrefs.getBoolean("keep_alive_guide_shown", false)
        ) {
            // 延迟以避免与教程最后一步动画/遮罩消失冲突
            kotlinx.coroutines.delay(800)
            showKeepAliveGuideDialog = true
        }
    }

    fun dismissKeepAliveGuide() {
        keepAlivePrefs.edit().putBoolean("keep_alive_guide_shown", true).apply()
        showKeepAliveGuideDialog = false
    }

    val scope = rememberCoroutineScope()
    val pageOffset = remember { Animatable(0f) }
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }

    // 教程期间强制收起顶部栏和底部栏
    LaunchedEffect(tutorialForceCollapseTopBar) {
        if (tutorialForceCollapseTopBar) {
            forceCollapseTopBar = true
            kotlinx.coroutines.delay(300)
            forceCollapseTopBar = false
        }
    }
    LaunchedEffect(tutorialForceCollapseNavBar) {
        if (tutorialForceCollapseNavBar) {
            forceCollapseNavBar = true
            kotlinx.coroutines.delay(300)
            forceCollapseNavBar = false
        }
    }

    // 教程页面导航（放在 pageOffset 声明之后）
    LaunchedEffect(tutorialPageIndex) {
        if (tutorialPageIndex in 0..4 && currentPageIndex != tutorialPageIndex) {
            val target = tutorialPageIndex
            pageOffset.snapTo(target.toFloat())
            currentPageIndex = target
        }
    }


    var lastCollapseTime by remember { mutableLongStateOf(0L) }
    val collapseCooldown = 600L

    val contentAlpha = (1f - expandProgress).coerceIn(0f, 1f)
    val mainContentAlpha = (1f - topBarExpandProgress).coerceIn(0f, 1f)

    BackHandler(enabled = topBarExpandProgress > 0.1f) {
        lastCollapseTime = System.currentTimeMillis()
        forceCollapseTopBar = true
    }

    BackHandler(enabled = expandProgress > 0.1f && topBarExpandProgress <= 0.1f) {
        lastCollapseTime = System.currentTimeMillis()
        forceCollapseNavBar = true
    }

    BackHandler(enabled = topBarExpandProgress <= 0.1f && expandProgress <= 0.1f) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCollapseTime > collapseCooldown) {
            (context as? android.app.Activity)?.moveTaskToBack(true)
        }
    }

    @Composable
    fun BackgroundLayer() {
        // ── Filament 3D 背景：播放时淡出背景层露出 3D 可视化 ──
        val bg3DEnabled by Filament3DConfig.threeDEnabledFlow.collectAsState()
        val glassFadeExempt by Filament3DConfig.glassFadeExemptFlow.collectAsState()
        val playerState by MusicPlayerController.state.collectAsState()
        val isMusicPlaying = playerState.isPlaying

        // 背景淡出透明度: 1=正常显示, 0=完全淡出(透明度100)
        val bgFadeAlpha = remember { Animatable(1f) }
        LaunchedEffect(bg3DEnabled, isMusicPlaying) {
            val target = if (bg3DEnabled && isMusicPlaying) 0f else 1f
            bgFadeAlpha.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = if (target == 0f) 800 else 400)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = bgFadeAlpha.value))
        ) {
            // 背景玻璃模糊作用对象: 3D 粒子 + 白噪音可视化 + 背景图片 (仅「模糊3D背景」开启时整体模糊成玻璃质感)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (bgGlassEnabled && glassFadeExempt && bgGlassBlur > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur((bgGlassBlur * 20).dp)
                        } else {
                            Modifier
                        }
                    )
            ) {
            // ── 最底层: 3D 粒子音频可视化层 (TextureView, 可被 RenderEffect 采样) ──
            if (bg3DEnabled) {
                Particle3DBackground(enabled = true, modifier = Modifier.fillMaxSize())
            }

            // 白噪音可视化层随 3D 模式淡出, 避免遮挡 3D 粒子
            WhiteNoiseVisualizerBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(bgFadeAlpha.value)
            )

            val hasRenderEffect = bgRenderEnabled && (
                bgBrightness != 0f || bgContrast != 0f || bgSaturation != 0f ||
                bgHighlights != 0f || bgShadows != 0f || bgRenderBlur > 0f
            )
            val renderColorMatrix = remember(
                bgRenderEnabled, bgBrightness, bgContrast, bgSaturation, bgHighlights, bgShadows
            ) {
                if (bgRenderEnabled) buildRenderColorMatrix(
                    bgBrightness, bgContrast, bgSaturation, bgHighlights, bgShadows
                ) else null
            }
            val composeColorMatrix = if (renderColorMatrix != null) {
                androidx.compose.ui.graphics.ColorMatrix(renderColorMatrix.array)
            } else null

            // 背景图片层 + 纯色背景层（3D 模式下随播放淡出, 透明度100）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(bgFadeAlpha.value)
                    .then(
                        if (hasRenderEffect && bgRenderBlur > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur((bgRenderBlur * 25).dp)
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (backgroundUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(backgroundUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = if (hasRenderEffect && composeColorMatrix != null) {
                            ColorFilter.colorMatrix(composeColorMatrix)
                        } else null
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = backgroundAlpha))
                )
            }
            }  // 关闭背景玻璃模糊作用层

            // 模糊玻璃渲染层（开启「模糊3D背景」时豁免淡出）
            if (bgGlassEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (glassFadeExempt) 1f else bgFadeAlpha.value)
                ) {
                    BackgroundGlassOverlay(
                        type = bgGlassType,
                        opacity = bgGlassOpacity,
                        darkness = bgGlassDarkness,
                        noise = bgGlassNoise,
                        gridSize = bgGlassGridSize,
                        gradient = bgGlassGradient,
                        sheen = bgGlassSheen
                    )
                }
            }

            if (bgRenderEnabled && bgVignette > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(bgFadeAlpha.value)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = bgVignette * 0.8f)
                                ),
                                radius = 1.4f
                            )
                        )
                )
            }
        }
    }

    val mainContent: @Composable () -> Unit = {
        val layoutDensity = LocalDensity.current
        val extendTopPx = with(layoutDensity) { TopBarHeight.toPx() }
        val extendBottomPx = with(layoutDensity) { (BottomNavTotalHeight * 0.5f).toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentAlpha * mainContentAlpha)
                .then(
                    if (isLandscape) {
                        val topSpacing = TopBarHeight + TopBarHeight + ContentPaddingTop
                        val bottomSpacing = BottomNavTotalHeight + ContentPaddingBottom
                        Modifier.padding(start = topSpacing, end = bottomSpacing)
                    } else {
                        Modifier
                    }
                )
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        constraints.copy(
                            minHeight = 0,
                            maxHeight = constraints.maxHeight + (extendTopPx + extendBottomPx).toInt()
                        )
                    )
                    layout(placeable.width, constraints.maxHeight) {
                        placeable.placeRelative(0, -extendTopPx.toInt())
                    }
                }
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.00f to Color(0x00000000),
                            0.05f to Color(0x0A000000),
                            0.10f to Color(0x1A000000),
                            0.15f to Color(0x66000000),
                            0.25f to Color.Black,
                            0.75f to Color.Black,
                            0.85f to Color(0x66000000),
                            0.90f to Color(0x1A000000),
                            0.95f to Color(0x0A000000),
                            1.00f to Color(0x00000000)
                        ),
                        blendMode = BlendMode.DstIn,
                        size = size
                    )
                }
                .then(
                    if (isLandscape) {
                        Modifier.padding(
                            start = ContentPaddingBottom,
                            end = ContentPaddingBottom
                        )
                    } else {
                        Modifier
                    }
                )
                .pointerInput(currentPageIndex) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragOffset = 0f
                            scope.launch { pageOffset.stop() }
                        },
                        onDragEnd = {
                            val threshold = screenWidthPx * 0.3f
                            val currentOffset = pageOffset.value

                            val targetPage = when {
                                dragOffset > 0 && currentPageIndex > 0 -> currentPageIndex - 1
                                dragOffset < 0 && currentPageIndex < 4 -> currentPageIndex + 1
                                abs(dragOffset) > threshold -> currentPageIndex
                                else -> currentPageIndex
                            }

                            scope.launch {
                                pageOffset.animateTo(
                                    targetValue = targetPage.toFloat(),
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                            currentPageIndex = targetPage
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                            val newOffset = (pageOffset.value - dragAmount / screenWidthPx)
                                .coerceIn(0f, 4f)
                            scope.launch { pageOffset.snapTo(newOffset) }
                        }
                    )
                }
        ) {
            PageContent(
                pageIndex = 0,
                pageOffset = pageOffset.value,
                screenWidthPx = screenWidthPx,
                content = { HomeScreen() }
            )
            PageContent(
                pageIndex = 1,
                pageOffset = pageOffset.value,
                screenWidthPx = screenWidthPx,
                content = { ScatteredScreen() }
            )
            PageContent(
                pageIndex = 2,
                pageOffset = pageOffset.value,
                screenWidthPx = screenWidthPx,
                content = {
                    PlayScreen()
                }
            )
            PageContent(
                pageIndex = 3,
                pageOffset = pageOffset.value,
                screenWidthPx = screenWidthPx,
                content = { ChatScreen() }
            )
            PageContent(
                pageIndex = 4,
                pageOffset = pageOffset.value,
                screenWidthPx = screenWidthPx,
                content = { SettingScreen() }
            )
        }
    }

    // 读取渲染配置参数
    val compatOpacity by GlassRenderConfig.compatOpacityFlow.collectAsState()
    val compatDarkness by GlassRenderConfig.compatDarknessFlow.collectAsState()
    val compatScale by GlassRenderConfig.compatScaleFlow.collectAsState()
    val perfBlur by GlassRenderConfig.perfBlurFlow.collectAsState()
    val perfScale by GlassRenderConfig.perfScaleFlow.collectAsState()
    val perfDistortion by GlassRenderConfig.perfDistortionFlow.collectAsState()
    val perfDarkness by GlassRenderConfig.perfDarknessFlow.collectAsState()
    val perfWarp by GlassRenderConfig.perfWarpFlow.collectAsState()
    val perfElevation by GlassRenderConfig.perfElevationFlow.collectAsState()

    // 模糊背板状态
    val blurBackdropEnabled by BlurBackdropConfig.enabledFlow.collectAsState()
    val blurBackdropBlur by BlurBackdropConfig.blurFlow.collectAsState()
    val blurBackdropDarkness by BlurBackdropConfig.darknessFlow.collectAsState()
    val isDialogShowing by DialogBlurState.isDialogShowing.collectAsState()

    // 只有当液态玻璃模式开启且任意作用范围启用时才应用效果
    val shouldApplyGlass = glassMode != GlassMode.OFF && (glassScopeBottomNav || glassScopeTopBar)

    // 计算精确位移量（使用屏幕高度确保完全移出）
    val statusBarHeight = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    val navigationBarHeight = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    // 横屏时位移方向不同
    val topBarOffsetPx = if (isLandscape) {
        screenWidthPx // 横屏：顶部栏从左侧向左移出
    } else {
        screenHeightPx // 竖屏：顶部栏从顶部向上移出
    }
    val bottomNavOffsetPx = if (isLandscape) {
        screenWidthPx // 横屏：底部栏从右侧向右移出
    } else {
        screenHeightPx // 竖屏：底部栏从底部向下移出
    }

    // 外层Box：包含所有UI元素，使用位移动画实现互斥
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (blurBackdropEnabled && isDialogShowing) {
                    Modifier.blur((blurBackdropBlur * 20).dp)
                } else {
                    Modifier
                }
            )
    ) {
        if (shouldApplyGlass) {
            // 玻璃模式：背景+页面内容作为content，NavBar和TopBar作为glassContent
            GlassContainer(
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        BackgroundLayer()
                        mainContent()
                    },
                    glassContent = {
                    // glassContent的receiver是GlassBoxScope，传递给ExpandableNavBar以便调用glassBackground
                    ExpandableNavBar(
                        modifier = Modifier.offset {
                            IntOffset(
                                if (isLandscape) (bottomNavOffsetPx * topBarExpandProgress).roundToInt() else 0,
                                if (isLandscape) 0 else (bottomNavOffsetPx * topBarExpandProgress).roundToInt()
                            )
                        }
                            .tutorialTarget(TutorialManager.KEY_BOTTOM_NAV),
                        currentRoute = screens[currentPageIndex].route,
                        onRouteSelected = { route ->
                            val targetIndex = screens.indexOfFirst { it.route == route }
                            // 特殊处理：已在播放页时，点击播放tab切换白噪音播放/暂停
                            if (targetIndex == 2 && currentPageIndex == 2 && playingSounds.isNotEmpty()) {
                                viewModel.togglePauseResume()
                            } else if (targetIndex != currentPageIndex && targetIndex >= 0) {
                                scope.launch {
                                    pageOffset.animateTo(
                                        targetValue = targetIndex.toFloat(),
                                        animationSpec = tween(
                                            durationMillis = ANIM_DURATION,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                }
                                currentPageIndex = targetIndex
                            }
                        },
                        onExpandProgress = { progress ->
                            expandProgress = progress
                        },
                        onInteractionStateChanged = { isInteracting ->
                            isNavInteracting = isInteracting
                        },
                        isOtherInteracting = isTopBarInteracting,
                        forceCollapseOther = {
                            forceCollapseTopBar = true
                        },
                        forceCollapse = forceCollapseNavBar,
                        onForceCollapseComplete = {
                            forceCollapseNavBar = false
                        },
                        // 传递渲染配置参数，让ExpandableNavBar内部根据动态圆角构建glassModifier
                        glassMode = glassMode,
                        glassScale = if (glassMode == GlassMode.PERFECT) perfScale else compatScale,
                        glassBlur = if (glassMode == GlassMode.PERFECT) perfBlur else compatOpacity,
                        isWhiteNoisePlaying = isWhiteNoisePlaying,
                        glassCenterDistortion = if (glassMode == GlassMode.PERFECT) perfDistortion else 0f,
                        glassElevation = if (glassMode == GlassMode.PERFECT) perfElevation.toInt() else 4,
                        glassDarkness = if (glassMode == GlassMode.PERFECT) perfDarkness else compatDarkness,
                        glassWarpEdges = if (glassMode == GlassMode.PERFECT) perfWarp else 0f,
                        // 传递GlassBoxScope，用于调用glassBackground方法（当bottom_nav启用时）
                        glassScope = if (glassScopeBottomNav) this else null,
                        // Chat 输入区
                        showChatInput = currentPageIndex == 3,
                        chatInputValue = chatState.inputText,
                        chatInputEnabled = !chatState.isLoading,
                        onChatInputChange = { aiChatViewModel.setInputText(it) },
                        onChatSend = {
                            if (chatState.inputText.isNotBlank() && !chatState.isLoading) {
                                aiChatViewModel.sendMessage(chatState.inputText)
                                aiChatViewModel.setInputText("")
                            }
                        },
                        confirmMode = chatState.confirmMode,
                        onToggleConfirmMode = { aiChatViewModel.toggleConfirmMode() }
                    )
                    // ExpandableTopBar添加液态玻璃效果（当top_bar启用时）
                    ExpandableTopBar(
                        modifier = Modifier.offset {
                            IntOffset(
                                if (isLandscape) -(topBarOffsetPx * expandProgress).roundToInt() else 0,
                                if (isLandscape) 0 else -(topBarOffsetPx * expandProgress).roundToInt()
                            )
                        }
                            .tutorialTarget(TutorialManager.KEY_TOP_BAR),
                        onExpandProgress = { progress ->
                            topBarExpandProgress = progress
                        },
                        onInteractionStateChanged = { isInteracting ->
                            isTopBarInteracting = isInteracting
                        },
                        isOtherInteracting = isNavInteracting,
                        forceCollapse = forceCollapseTopBar,
                        onForceCollapseComplete = {
                            forceCollapseTopBar = false
                        },
                        // 传递渲染配置参数
                        glassMode = glassMode,
                        glassScale = if (glassMode == GlassMode.PERFECT) perfScale else compatScale,
                        glassBlur = if (glassMode == GlassMode.PERFECT) perfBlur else compatOpacity,
                        glassCenterDistortion = if (glassMode == GlassMode.PERFECT) perfDistortion else 0f,
                        glassElevation = if (glassMode == GlassMode.PERFECT) perfElevation.toInt() else 4,
                        glassDarkness = if (glassMode == GlassMode.PERFECT) perfDarkness else compatDarkness,
                        glassWarpEdges = if (glassMode == GlassMode.PERFECT) perfWarp else 0f,
                        // 传递GlassBoxScope，用于调用glassBackground方法（当top_bar启用时）
                        glassScope = if (glassScopeTopBar) this else null
                    )
                }
            )
        } else {
            // 关闭模式：背景层 + 页面 + bar 直接渲染
            BackgroundLayer()
            mainContent()
            ExpandableNavBar(
                modifier = Modifier.offset {
                    IntOffset(
                        if (isLandscape) (bottomNavOffsetPx * topBarExpandProgress).roundToInt() else 0,
                        if (isLandscape) 0 else (bottomNavOffsetPx * topBarExpandProgress).roundToInt()
                    )
                }
                    .tutorialTarget(TutorialManager.KEY_BOTTOM_NAV),
                currentRoute = screens[currentPageIndex].route,
                onRouteSelected = { route ->
                    val targetIndex = screens.indexOfFirst { it.route == route }
                    // 特殊处理：已在播放页时，点击播放tab切换白噪音播放/暂停
                    if (targetIndex == 2 && currentPageIndex == 2 && playingSounds.isNotEmpty()) {
                        viewModel.togglePauseResume()
                    } else if (targetIndex != currentPageIndex && targetIndex >= 0) {
                        scope.launch {
                            pageOffset.animateTo(
                                targetValue = targetIndex.toFloat(),
                                animationSpec = tween(
                                    durationMillis = ANIM_DURATION,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                        currentPageIndex = targetIndex
                    }
                },
                onExpandProgress = { progress ->
                    expandProgress = progress
                },
                onInteractionStateChanged = { isInteracting ->
                    isNavInteracting = isInteracting
                },
                isOtherInteracting = isTopBarInteracting,
                forceCollapseOther = {
                    forceCollapseTopBar = true
                },
                forceCollapse = forceCollapseNavBar,
                onForceCollapseComplete = {
                    forceCollapseNavBar = false
                },
                // 玻璃效果关闭时传递默认参数
                glassMode = GlassMode.OFF,
                isWhiteNoisePlaying = isWhiteNoisePlaying,
                glassScale = 0f,
                glassBlur = 0f,
                glassCenterDistortion = 0f,
                glassElevation = 4,
                glassDarkness = 0f,
                glassWarpEdges = 0f,
                // Chat 输入区
                showChatInput = currentPageIndex == 3,
                chatInputValue = chatState.inputText,
                chatInputEnabled = !chatState.isLoading,
                onChatInputChange = { aiChatViewModel.setInputText(it) },
                onChatSend = {
                    if (chatState.inputText.isNotBlank() && !chatState.isLoading) {
                        aiChatViewModel.sendMessage(chatState.inputText)
                        aiChatViewModel.setInputText("")
                    }
                },
                confirmMode = chatState.confirmMode,
                onToggleConfirmMode = { aiChatViewModel.toggleConfirmMode() }
            )
            // ExpandableTopBar添加位移：底部导航栏展开时向上移出屏幕
            ExpandableTopBar(
                modifier = Modifier.offset {
                    IntOffset(
                        if (isLandscape) -(topBarOffsetPx * expandProgress).roundToInt() else 0,
                        if (isLandscape) 0 else -(topBarOffsetPx * expandProgress).roundToInt()
                    )
                }
                    .tutorialTarget(TutorialManager.KEY_TOP_BAR),
                onExpandProgress = { progress ->
                    topBarExpandProgress = progress
                },
                onInteractionStateChanged = { isInteracting ->
                    isTopBarInteracting = isInteracting
                },
                isOtherInteracting = isNavInteracting,
                forceCollapse = forceCollapseTopBar,
                onForceCollapseComplete = {
                    forceCollapseTopBar = false
                }
            )
        }

        // 弹窗模糊背板：暗幕叠加层
        if (blurBackdropEnabled && isDialogShowing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = blurBackdropDarkness))
            )
        }
        
        // ========== 新手教程遮罩层 ==========
        // 布尔遮罩：全屏半透明黑底 + BlendMode.Clear 擦除目标区域
        // bar 位置用已知常数直接算（不依赖 onGloballyPositioned，因为 bar 的 root Box 是 fillMaxSize 全屏），
        // 嵌套目标（PlayScreen 按钮/声音）用 tutorialTargetPositions 注册的精确位置
        if (tutorialActive != null && tutorialStep != null) {
            val step = tutorialStep
            val screenDensity = LocalDensity.current
            // 顶部栏折叠态：padding 链 (statusBar+48dp) + 16dp(vertical) + 8dp(top) 
            // 内容 TopCenter × 48dp 高，水平内缩 16dp
            val topBarPos = remember {
                val sbPx = with(screenDensity) { statusBarHeight.toPx() }
                val tbPx = with(screenDensity) { TopBarHeight.toPx() }
                val padH = with(screenDensity) { TopBarPaddingHorizontal.toPx() }  // 16dp
                val padT = with(screenDensity) { TopBarPaddingTop.toPx() }          // 8dp
                val top = sbPx + tbPx + padH + padT   // statusBar + 48 + 16 + 8
                TutorialTargetPosition(
                    left = padH,
                    top = top - padT - padT,                 // 再往上收 8dp
                    right = screenWidthPx - padH,
                    bottom = top + tbPx - padT        // + 48dp content, 同上
                )
            }
            // 底部导航栏：padding(start/end=24dp, bottom=24dp) + navigationBars
            // 内容 BottomCenter × 64dp 高
            val navBarPos = remember(screenWidthPx, screenHeightPx, navigationBarHeight) {
                val navPx = with(screenDensity) { navigationBarHeight.toPx() }
                val totalPx = with(screenDensity) { BottomNavTotalHeight.toPx() }
                val marginH = with(screenDensity) { CollapsedMarginHorizontal.toPx() }
                val marginB = with(screenDensity) { CollapsedMarginBottom.toPx() }
                TutorialTargetPosition(
                    left = marginH,
                    top = screenHeightPx - navPx - totalPx,
                    right = screenWidthPx - marginH,
                    bottom = screenHeightPx - navPx - marginB
                )
            }
            val targetPos = when (step.targetKey) {
                TutorialManager.KEY_TOP_BAR -> topBarPos
                TutorialManager.KEY_BOTTOM_NAV -> navBarPos
                null -> null
                else -> tutorialTargetPositions[step.targetKey]
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                val dark = Color.Black.copy(alpha = 0.72f)
                drawRect(dark)
                if (targetPos != null) {
                    val pad = step.highlightPadding * density
                    val holeLeft = (targetPos.left - pad).coerceAtLeast(0f)
                    val holeTop = (targetPos.top - pad).coerceAtLeast(0f)
                    val holeRight = (targetPos.right + pad).coerceAtMost(size.width)
                    val holeBottom = (targetPos.bottom + pad).coerceAtMost(size.height)
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(holeLeft, holeTop),
                        size = Size(holeRight - holeLeft, holeBottom - holeTop),
                        cornerRadius = CornerRadius(16f, 16f),
                        blendMode = BlendMode.Clear
                    )
                }
            }

            TutorialTooltip(
                step = step,
                currentStep = tutorialStepIndex,
                totalSteps = TutorialManager.getTotalSteps(),
                position = TooltipPosition.CENTER,
                onNextClick = { TutorialManager.nextStep() },
                onSkipClick = { TutorialManager.skipTutorial() },
                modifier = Modifier.zIndex(300f)
            )
        }

        // ========== 应用后台保活引导弹窗 ==========
        if (showKeepAliveGuideDialog) {
            GlassAlertDialog(
                onDismissRequest = { dismissKeepAliveGuide() },
                title = stringResource(R.string.keep_alive_dialog_title),
                confirmText = stringResource(R.string.keep_alive_dialog_confirm),
                dismissText = stringResource(R.string.keep_alive_dialog_dismiss),
                onConfirm = {
                    dismissKeepAliveGuide()
                    val success = BatteryOptimizationHelper.openKeepAliveSettings(context)
                    if (!success) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.keep_alive_guide_open_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onDismiss = { dismissKeepAliveGuide() }
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.keep_alive_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ========== AI 工具调用确认弹窗（confirmMode 开启时） ==========
        chatState.pendingConfirmation?.let { pending ->
            ToolConfirmDialog(
                pending = pending,
                onConfirm = { modifiedArgs -> aiChatViewModel.confirmToolCall(modifiedArgs) },
                onReject = { reason -> aiChatViewModel.rejectToolCall(reason) }
            )
        }
    }
}

@Composable
private fun PageContent(
    pageIndex: Int,
    pageOffset: Float,
    screenWidthPx: Float,
    content: @Composable () -> Unit
) {
    val pageAlpha = calculatePageAlpha(pageOffset, pageIndex)
    val pageOffsetX = calculatePageOffset(pageOffset, pageIndex, screenWidthPx)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = PageTopPadding)
            .alpha(pageAlpha)
            .offset { IntOffset(pageOffsetX.roundToInt(), 0) }
    ) {
        content()
    }
}

private fun calculatePageAlpha(currentOffset: Float, pageIndex: Int): Float {
    val distance = abs(currentOffset - pageIndex)
    return (1f - distance).coerceIn(0f, 1f)
}

private fun calculatePageOffset(currentOffset: Float, pageIndex: Int, screenWidthPx: Float): Float {
    val diff = pageIndex - currentOffset
    return diff * screenWidthPx
}

/**
 * 构建背景渲染的颜色矩阵
 * 参数范围: brightness/contrast/saturation/highlights/shadows 均为 -1.0 ~ 1.0, 0 = 原始
 */
private fun buildRenderColorMatrix(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    highlights: Float,
    shadows: Float
): AndroidColorMatrix {
    val matrix = AndroidColorMatrix()

    // 1. 饱和度: 0 ~ 2
    val sat = (1f + saturation).coerceIn(0f, 2f)
    matrix.setSaturation(sat)

    // 2. 对比度: 围绕 0.5 缩放
    val con = (1f + contrast).coerceIn(0f, 2f)
    val conT = 128f * (1f - con)
    val conMatrix = AndroidColorMatrix(floatArrayOf(
        con, 0f, 0f, 0f, conT,
        0f, con, 0f, 0f, conT,
        0f, 0f, con, 0f, conT,
        0f, 0f, 0f, 1f, 0f
    ))
    matrix.postConcat(conMatrix)

    // 3. 亮度: 平移 RGB
    val bright = brightness * 200f
    if (bright != 0f) {
        val brightMatrix = AndroidColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, bright,
            0f, 1f, 0f, 0f, bright,
            0f, 0f, 1f, 0f, bright,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(brightMatrix)
    }

    // 4. 高亮/阴影: 简化近似
    // shadows > 0: 提亮暗部; shadows < 0: 压暗暗部
    // highlights > 0: 提亮亮部; highlights < 0: 压暗亮部
    if (highlights != 0f || shadows != 0f) {
        val shadowOffset = shadows * 60f  // 暗部偏移
        val highlightScale = (1f + highlights * 0.3f).coerceIn(0.5f, 1.5f)  // 亮部缩放
        val highlightOffset = -highlights * 30f  // 亮部偏移补偿
        val totalOffset = shadowOffset + highlightOffset
        val hsMatrix = AndroidColorMatrix(floatArrayOf(
            highlightScale, 0f, 0f, 0f, totalOffset,
            0f, highlightScale, 0f, 0f, totalOffset,
            0f, 0f, highlightScale, 0f, totalOffset,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(hsMatrix)
    }

    return matrix
}

/**
 * 背景玻璃模糊叠加层
 * 根据5种玻璃类型渲染不同的视觉效果
 */
@Composable
private fun BackgroundGlassOverlay(
    type: BackgroundGlassConfig.GlassType,
    opacity: Float,
    darkness: Float,
    noise: Float,
    gridSize: Float,
    gradient: Float,
    sheen: Float
) {
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor.copy(alpha = opacity * 0.4f))
    ) {
        when (type) {
            BackgroundGlassConfig.GlassType.SANDBLASTED -> {
                // 磨砂玻璃: 噪点纹理
                if (noise > 0f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val dotCount = (noise * 8000).toInt()
                        val maxDotSize = noise * 3f
                        repeat(dotCount) { i ->
                            val x = (i * 37.0 % w)
                            val y = (i * 71.0 % h)
                            val alpha = (noise * 0.15f * ((i % 100) / 100f + 0.3f))
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = maxDotSize * ((i % 3) / 3f + 0.3f),
                                center = Offset(x.toFloat(), y.toFloat())
                            )
                        }
                    }
                }
            }

            BackgroundGlassConfig.GlassType.FROSTED -> {
                // 毛玻璃: 纯净的半透明白色高光渐变
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = opacity * 0.12f),
                                    Color.White.copy(alpha = opacity * 0.04f),
                                    Color.White.copy(alpha = opacity * 0.10f)
                                )
                            )
                        )
                )
            }

            BackgroundGlassConfig.GlassType.GRID -> {
                // 格栅玻璃: 网格线条
                if (gridSize > 0f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cellSize = (40f + (1f - gridSize) * 60f) // gridSize越大格子越小
                        val lineAlpha = gridSize * 0.25f
                        val lineColor = Color.White.copy(alpha = lineAlpha)
                        var x = 0f
                        while (x < w) {
                            drawLine(
                                color = lineColor,
                                start = Offset(x, 0f),
                                end = Offset(x, h),
                                strokeWidth = 1f
                            )
                            x += cellSize
                        }
                        var y = 0f
                        while (y < h) {
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                            y += cellSize
                        }
                    }
                }
            }

            BackgroundGlassConfig.GlassType.MISTY -> {
                // 云雾玻璃: 柔和渐变
                if (gradient > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = gradient * 0.18f),
                                        Color.White.copy(alpha = gradient * 0.08f),
                                        Color.White.copy(alpha = gradient * 0.02f),
                                        Color.White.copy(alpha = gradient * 0.10f)
                                    ),
                                    radius = 1f
                                )
                            )
                    )
                    // 叠加一层对角渐变模拟云雾流动
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = gradient * 0.06f),
                                        Color.Transparent,
                                        Color.White.copy(alpha = gradient * 0.04f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                    )
                }
            }

            BackgroundGlassConfig.GlassType.SILK -> {
                // 丝绸玻璃: 对角光泽
                if (sheen > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = sheen * 0.05f),
                                        Color.White.copy(alpha = sheen * 0.20f),
                                        Color.White.copy(alpha = sheen * 0.08f),
                                        Color.White.copy(alpha = sheen * 0.16f),
                                        Color.White.copy(alpha = sheen * 0.04f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                    )
                }
            }
        }

        // 暗度叠加（通用）
        if (darkness > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = darkness * 0.5f))
            )
        }
    }
}
