package com.bicy.whitenoise.yODW.SrEO

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
import com.bicy.whitenoise.yODW.SrEO.InteractiveSlider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.bicy.whitenoise.xnef.MusicLibrary
import com.bicy.whitenoise.xnef.MusicPlayerController
import com.bicy.whitenoise.xnef.MusicPlayerState
import com.bicy.whitenoise.xnef.MusicRepeatMode
import com.bicy.whitenoise.xnef.MusicShuffleMode
import com.bicy.whitenoise.xnef.MusicTrack
import com.bicy.whitenoise.xnef.ScanProgress
import com.bicy.whitenoise.JwJY.NATg.ConfigStorage
import com.bicy.whitenoise.yODW.SrEO.Xomm.DecelerateEasing
import com.bicy.whitenoise.yODW.SrEO.Xomm.MainAlbumIconSize
import com.bicy.whitenoise.yODW.SrEO.Xomm.MainAlbumSize
import com.bicy.whitenoise.yODW.SrEO.Xomm.MainControlSize
import com.bicy.whitenoise.yODW.SrEO.Xomm.MainTitleFontSize
import com.bicy.whitenoise.yODW.SrEO.Xomm.MusicCategory
import com.bicy.whitenoise.yODW.SrEO.Xomm.PanelState
import com.bicy.whitenoise.yODW.SrEO.Xomm.PanelType
import com.bicy.whitenoise.yODW.SrEO.Xomm.SidebarAlbumIconSize
import com.bicy.whitenoise.yODW.SrEO.Xomm.SidebarAlbumSize
import com.bicy.whitenoise.yODW.SrEO.Xomm.SidebarControlSize
import com.bicy.whitenoise.yODW.SrEO.Xomm.SidebarPlayButtonSize
import com.bicy.whitenoise.yODW.SrEO.Xomm.SidebarTitleFontSize
import com.bicy.whitenoise.yODW.G2qv.LocalPlaylistNavigation
import com.bicy.whitenoise.yODW.G2qv.LocalPlaylistNavigationHolder
import com.bicy.whitenoise.yODW.G2qv.PlaylistNavigationState
import com.bicy.whitenoise.yODW.G2qv.rememberPlaylistNavigationState
import com.bicy.whitenoise.yODW.SrEO.Xomm.SidebarWidth
import com.bicy.whitenoise.yODW.SrEO.Xomm.SlideInPanel
import com.bicy.whitenoise.yODW.SrEO.Xomm.TopBarCornerRadius
import com.bicy.whitenoise.yODW.SrEO.Xomm.TopBarHeight
import com.bicy.whitenoise.yODW.SrEO.Xomm.TopBarPaddingHorizontal
import com.bicy.whitenoise.yODW.SrEO.Xomm.TopBarPaddingTop
import com.bicy.whitenoise.yODW.SrEO.Xomm.TransitionProgress
import com.bicy.whitenoise.yODW.SrEO.Xomm.calculateTransitionProgress
import com.bicy.whitenoise.yODW.ZFNn.ShadowConfig
import com.bicy.whitenoise.yODW.ZFNn.ThemeColorManager
import com.bicy.whitenoise.yODW.ZFNn.dropShadow
import com.bicy.whitenoise.yODW.ZFNn.mGbG
import com.bicy.whitenoise.y10p.AudioMetadataReader
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
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current
    
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()
    
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
    
    val globalState by ConfigStorage.config.collectAsState()
    val isPremiumUser = globalState.isPremium
    
    LaunchedEffect(forceCollapse) {
        if (forceCollapse && isExpanded && !isAnimating) {
            isAnimating = true
            isExpanded = false
            scope.launch {
                expandProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(500, easing = DecelerateEasing)
                )
                panelProgress.snapTo(0f)
                onForceCollapseComplete()
                isAnimating = false
            }
            panelState = PanelState.Main
            displayedPanelType = null
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
            forceCollapsePanel = false
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
    
    val currentPaddingHorizontal = remember(progress) {
        val value = TopBarPaddingHorizontal * (1f - progress)
        if (value.value.isNaN() || value < 0.dp) 0.dp else value
    }
    val currentPaddingTop = remember(progress) {
        val value = TopBarPaddingTop * (1f - progress)
        if (value.value.isNaN() || value < 0.dp) 0.dp else value
    }
    val currentCornerRadius = remember(progress) {
        val value = TopBarCornerRadius * (1f - progress)
        if (value.value.isNaN() || value < 0.dp) 0.dp else value
    }
    
    val currentStatusBarPadding = remember(progress, statusBarHeight) {
        val value = (statusBarHeight + TopBarHeight) * (1f - progress)
        if (value.value.isNaN() || value < 0.dp) 0.dp else value
    }
    
    val currentHeight = remember(progress, screenHeight) {
        val value = TopBarHeight + (screenHeight - TopBarHeight) * progress
        if (value.value.isNaN() || value < TopBarHeight) TopBarHeight else value
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
            .zIndex(zIndex)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = currentStatusBarPadding)
                .padding(top = currentPaddingTop)
                .padding(horizontal = currentPaddingHorizontal),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentHeight)
                    .dropShadow(
                        config = ShadowConfig.Deep,
                        shape = RoundedCornerShape(currentCornerRadius),
                        clip = false
                    )
                    .graphicsLayer {
                        shape = RoundedCornerShape(currentCornerRadius)
                        clip = true
                    }
                    .background(navBgColor)
                    .pointerInput(Unit) {
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
                                
                                val horizontalThreshold = with(density) { 80.dp.toPx() }
                                val verticalThreshold = with(density) { 50.dp.toPx() }
                                
                                if (progress > 0.9f && abs(dragOffset) > horizontalThreshold) {
                                    val targetPanelType = if (dragOffset < 0) {
                                        PanelType.Mixer
                                    } else {
                                        PanelType.Playlist
                                    }
                                    displayedPanelType = targetPanelType
                                    panelState = if (targetPanelType == PanelType.Mixer) PanelState.Mixer else PanelState.Playlist
                                    scope.launch { panelProgress.snapTo(1f) }
                                } else if (progress > 0.9f && displayedPanelType != null) {
                                    panelState = PanelState.Main
                                    scope.launch { panelProgress.snapTo(0f) }
                                    displayedPanelType = null
                                } else {
                                    val verticalThreshold = with(density) { 50.dp.toPx() }
                                    val currentProgress = expandProgress.value
                                    
                                    if (abs(dragOffset) > verticalThreshold) {
                                        val targetValue = if (dragOffset > 0) 1f else 0f
                                        scope.launch {
                                            expandProgress.animateTo(
                                                targetValue = targetValue,
                                                animationSpec = tween(500, easing = DecelerateEasing)
                                            )
                                        }
                                        isExpanded = dragOffset > 0
                                    } else {
                                        val targetValue = if (currentProgress > 0.5f) 1f else 0f
                                        scope.launch {
                                            expandProgress.animateTo(
                                                targetValue = targetValue,
                                                animationSpec = tween(500, easing = DecelerateEasing)
                                            )
                                        }
                                        isExpanded = currentProgress > 0.5f
                                    }
                                }
                                dragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                val touchX = change.position.x
                                val sidebarWidthPx = with(density) { SidebarWidth.toPx() }
                                val screenWidthPx = with(density) { screenWidth.toPx() }
                                
                                val isInLeftSidebar = touchX < sidebarWidthPx
                                val isInRightSidebar = touchX > screenWidthPx - sidebarWidthPx
                                val isInSidebarArea = isInLeftSidebar || isInRightSidebar
                                val isInCenterArea = !isInSidebarArea
                                
                                val totalDrag = sqrt(dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y)
                                val angle = atan2(dragAmount.y, dragAmount.x) * 180 / kotlin.math.PI
                                
                                val isHorizontal = abs(angle) > 135 || abs(angle) < 45
                                val isVertical = abs(angle) > 45 && abs(angle) < 135
                                
                                if (progress > 0.9f && isHorizontal) {
                                    if (displayedPanelType == null && abs(dragAmount.x) > 10) {
                                        displayedPanelType = if (dragAmount.x < 0) PanelType.Mixer else PanelType.Playlist
                                    }
                                    
                                    dragOffset += dragAmount.x
                                    val totalDragNeeded = with(density) { 
                                        (screenWidth - SidebarWidth).toPx() 
                                    }
                                    val currentPanelProgress = panelProgress.value
                                    val newProgress = when {
                                        displayedPanelType == PanelType.Mixer -> {
                                            (-dragOffset / totalDragNeeded).coerceIn(0f, 1f)
                                        }
                                        displayedPanelType == PanelType.Playlist -> {
                                            (dragOffset / totalDragNeeded).coerceIn(0f, 1f)
                                        }
                                        else -> 0f
                                    }
                                    scope.launch {
                                        panelProgress.snapTo(newProgress)
                                    }
                                } else if (isVertical) {
                                    val canProcessVertical = if (progress > 0.9f && displayedPanelType != null) {
                                        isInCenterArea
                                    } else {
                                        true
                                    }
                                    
                                    if (canProcessVertical) {
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
                            }
                        )
                    }
                    .clickable(enabled = !isExpanded) {
                        MusicPlayerController.playPause()
                    }
            ) {
                CollapsedTopBarContent(
                    alpha = contentAlpha
                )
                
                if (isPremiumUser) {
                    ExpandedTopBarContent(
                        alpha = panelAlpha,
                        panelProgress = panelProgress.value,
                        panelState = panelState,
                        displayedPanelType = displayedPanelType,
                        scope = scope,
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
                } else {
                    PremiumRequiredContent(
                        alpha = panelAlpha,
                        onCollapse = {
                            scope.launch {
                                expandProgress.animateTo(0f)
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
    alpha: Float
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
                
                val x = i * (barWidth + barSpacing)
                
                drawRect(
                    color = barColors.copy(alpha = alpha),
                    topLeft = Offset(x, centerY - barHeight / 2),
                    size = Size(barWidth, barHeight)
                )
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
            mGbG(
                iPg = playerState.isPlaying,
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
    
    Box(
        modifier = modifier
            .fillMaxSize()
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
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(effectiveAlbumIconSize)
                )
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
            
            if (horizontalProgressTransition.alpha > 0.01f) {
                Column(
                    modifier = Modifier
                        .height(effectiveProgressHeight)
                        .graphicsLayer {
                            alpha = horizontalProgressTransition.alpha * effectiveProgressAlpha
                        }
                ) {
                    if (effectiveProgressHeight > 20.dp) {
                        InteractiveSlider(
                            value = if (duration > 0) position.toFloat() else 0f,
                            onValueChange = { if (track != null) MusicPlayerController.seekTo(it.toLong()) },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = effectiveProgressWidth
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                }
                                .padding(horizontal = 16.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = AudioMetadataReader.formatDuration(position),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = AudioMetadataReader.formatDuration(duration),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
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

                if (false) {
                    FeatureButton(
                        text = "测试",
                        onClick = { onCompactSpacingChange(!isCompactSpacing) }
                    )
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
                        onBack = { onExpandedPanelChange(null) },
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
    com.bicy.whitenoise.yODW.SrEO.Xomm.VerticalProgressBar(progress, modifier)
}

@Composable
private fun MixerPanel(
    panelProgress: Float,
    modifier: Modifier = Modifier
) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.MixerPanel(panelProgress, modifier)
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.TabButton(text, isSelected, onClick)
}

@Composable
private fun EqualizerPanel() {
    com.bicy.whitenoise.yODW.SrEO.Xomm.EqualizerPanel()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqBandSliderHorizontal(
    frequency: String,
    gain: Float,
    onGainChange: (Float) -> Unit
) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.EqBandSliderHorizontal(frequency, gain, onGainChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReverbPanel() {
    com.bicy.whitenoise.yODW.SrEO.Xomm.ReverbPanel()
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
    com.bicy.whitenoise.yODW.SrEO.Xomm.CollapsibleSection(title, expanded, onToggle, subtitle, warningText, content)
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.PresetChip(label, selected, onClick)
}

@Composable
private fun EffectPlaceholderItem(name: String) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.EffectPlaceholderItem(name)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EffectSliderItem(
    name: String,
    intensity: Float,
    onIntensityChange: (Float) -> Unit
) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.EffectSliderItem(name, intensity, onIntensityChange)
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
    com.bicy.whitenoise.yODW.SrEO.Xomm.ReverbSliderComponent(
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
    com.bicy.whitenoise.yODW.SrEO.Xomm.PlaylistPanel(
        tracks, currentTrack, isScanning, panelProgress, playlist, playlistIndex, modifier, onBack
    )
}

@Composable
private fun CategorySidebar(
    selectedCategory: MusicCategory,
    onCategorySelected: (MusicCategory) -> Unit
) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.CategorySidebar(selectedCategory, onCategorySelected)
}

@Composable
private fun CategoryTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.CategoryTab(label, isSelected, onClick)
}

@Composable
private fun CategorySelectionList(
    items: List<String>,
    onItemClick: (String) -> Unit
) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.CategorySelectionList(items, onItemClick)
}

@Composable
private fun TrackList(
    tracks: List<MusicTrack>,
    currentTrack: MusicTrack?,
    showFavoriteButton: Boolean = true
) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.TrackList(tracks, currentTrack, showFavoriteButton)
}

@Composable
private fun PlaylistItem(
    track: MusicTrack,
    isPlaying: Boolean,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null
) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.PlaylistItem(track, isPlaying, isFavorite, onClick, onFavoriteClick)
}

@Composable
private fun FeatureButton(
    text: String,
    onClick: () -> Unit
) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.FeatureButton(text, onClick)
}

@Composable
private fun PremiumRequiredContent(
    alpha: Float,
    onCollapse: () -> Unit
) {
    com.bicy.whitenoise.yODW.SrEO.Xomm.PremiumRequiredContent(alpha, onCollapse)
}

@Composable
private fun PremiumUnlockDialog(
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
    com.bicy.whitenoise.yODW.NvYq.BxAd.UnlockPremiumDialog(
        isPremium = false,
        onDismiss = onDismiss,
        onPayClick = onUnlock
    )
}
