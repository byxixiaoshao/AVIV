package com.bicy.whitenoise.yODW.SrEO

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bicy.whitenoise.R
import com.bicy.whitenoise.oJft.TimerManager
import com.bicy.whitenoise.oJft.TimerState
import com.bicy.whitenoise.yODW.SrEO.WIXN.CollapsedCornerRadius
import com.bicy.whitenoise.yODW.SrEO.WIXN.CollapsedHeight
import com.bicy.whitenoise.yODW.SrEO.WIXN.CollapsedMarginBottom
import com.bicy.whitenoise.yODW.SrEO.WIXN.CollapsedMarginHorizontal
import com.bicy.whitenoise.yODW.SrEO.WIXN.DecelerateEasing
import com.bicy.whitenoise.yODW.SrEO.WIXN.NavIconSize
import com.bicy.whitenoise.yODW.SrEO.WIXN.NavItemSize
import com.bicy.whitenoise.yODW.SrEO.WIXN.PresetButtonsContent
import com.bicy.whitenoise.yODW.SrEO.WIXN.StartButton
import com.bicy.whitenoise.yODW.SrEO.WIXN.TimeSlidersContent
import com.bicy.whitenoise.yODW.SrEO.WIXN.TimerCircleContent
import com.bicy.whitenoise.yODW.SrEO.WIXN.TimerFinishedButtons
import com.bicy.whitenoise.yODW.SrEO.WIXN.TimerFinishedContent
import com.bicy.whitenoise.yODW.SrEO.WIXN.TimerSettingsContent
import com.bicy.whitenoise.yODW.SrEO.WIXN.screens
import com.bicy.whitenoise.yODW.etkB.Screen
import com.bicy.whitenoise.yODW.ZFNn.NavItemUnselected
import com.bicy.whitenoise.yODW.ZFNn.ShadowConfig
import com.bicy.whitenoise.yODW.ZFNn.ThemeColorManager
import com.bicy.whitenoise.yODW.ZFNn.dropShadow
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
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current
    
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()
    
    val expandProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    var isExpanded by remember { mutableStateOf(false) }
    var isInteracting by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isAnimating by remember { mutableStateOf(false) }
    
    val timerState by TimerManager.timerState.collectAsState()
    
    LaunchedEffect(forceCollapse) {
        if (forceCollapse && expandProgress.value > 0.1f && !isAnimating) {
            isAnimating = true
            isExpanded = false
            isInteracting = true
            onInteractionStateChanged(true)
            expandProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = DecelerateEasing
                )
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
                animationSpec = tween(
                    durationMillis = 500,
                    easing = DecelerateEasing
                )
            )
            isInteracting = false
            onInteractionStateChanged(false)
        }
    }
    
    val navBgColor = themeColor.navBg
    val selectedColor = themeColor.navItemSelected
    
    val rawProgress = expandProgress.value
    val progress = if (rawProgress.isNaN()) 0f else rawProgress.coerceIn(0f, 1f)
    
    val currentMarginHorizontal = remember(progress) { 
        val value = CollapsedMarginHorizontal * (1f - progress)
        if (value.value.isNaN() || value < 0.dp) 0.dp else value
    }
    val currentMarginBottom = remember(progress) { 
        val value = CollapsedMarginBottom * (1f - progress)
        if (value.value.isNaN() || value < 0.dp) 0.dp else value
    }
    val currentCornerRadius = remember(progress) { 
        val value = CollapsedCornerRadius * (1f - progress)
        if (value.value.isNaN() || value < 0.dp) 0.dp else value
    }
    
    val collapsedWidth = remember(screenWidth) { screenWidth - CollapsedMarginHorizontal * 2 }
    val currentWidth = remember(progress, screenWidth, collapsedWidth) { 
        val value = collapsedWidth + (screenWidth - collapsedWidth) * progress
        if (value.value.isNaN() || value < 0.dp) screenWidth else value
    }
    
    val currentHeight = remember(progress, screenHeight) { 
        val value = CollapsedHeight + (screenHeight - CollapsedHeight) * progress
        if (value.value.isNaN() || value < 0.dp) CollapsedHeight else value
    }
    
    val contentAlpha = remember(progress) { (1f - progress).coerceIn(0f, 1f) }
    val panelAlpha = remember(progress) { progress.coerceIn(0f, 1f) }
    val textAlpha = remember(progress) { 
        if (progress > 0.9f) ((progress - 0.9f) / 0.1f).coerceIn(0f, 1f) else 0f 
    }
    
    val zIndex = if (isInteracting || isExpanded) 2f else if (isOtherInteracting) 0f else 1f
    
    onExpandProgress(progress)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(zIndex)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = currentMarginHorizontal,
                    end = currentMarginHorizontal,
                    bottom = currentMarginBottom
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .width(currentWidth)
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
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragOffset = 0f
                                isInteracting = true
                                onInteractionStateChanged(true)
                                scope.launch {
                                    expandProgress.stop()
                                }
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
                                            animationSpec = tween(
                                                durationMillis = 500,
                                                easing = DecelerateEasing
                                            )
                                        )
                                    }
                                    isExpanded = dragOffset < 0
                                } else {
                                    val targetValue = if (currentProgress > 0.5f) 1f else 0f
                                    scope.launch {
                                        expandProgress.animateTo(
                                            targetValue = targetValue,
                                            animationSpec = tween(
                                                durationMillis = 500,
                                                easing = DecelerateEasing
                                            )
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
                                scope.launch {
                                    expandProgress.snapTo(newProgress)
                                }
                            }
                        )
                    }
            ) {
                CollapsedNavBarContent(
                    currentRoute = currentRoute,
                    onRouteSelected = onRouteSelected,
                    alpha = contentAlpha,
                    selectedColor = selectedColor,
                    timerState = timerState,
                    isExpanded = progress > 0.1f
                )
                
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ExpandedNavBarContent(
                        alpha = panelAlpha,
                        textAlpha = textAlpha,
                        timerState = timerState,
                        isEnabled = progress >= 0.5f
                    )
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
    isExpanded: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CollapsedHeight)
            .alpha(alpha)
            .then(
                if (!isExpanded) Modifier.padding(horizontal = 32.dp) else Modifier
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isExpanded) {
            screens.forEach { screen ->
                NavItem(
                    screen = screen,
                    isSelected = currentRoute == screen.route,
                    selectedColor = selectedColor,
                    onClick = { onRouteSelected(screen.route) }
                )
            }
        }
    }
    
    if (timerState.isActive && timerState.remainingTime > 0) {
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
        
        val progressColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        val cornerRadius = CollapsedCornerRadius
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CollapsedHeight)
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                if (size.width.isNaN() || size.height.isNaN() || size.width <= 0f || size.height <= 0f) {
                    return@Canvas
                }
                
                if (animatedProgress.isNaN() || animatedProgress < 0f) {
                    return@Canvas
                }
                
                val strokeWidth = 2.dp.toPx()
                val cornerRadiusPx = cornerRadius.toPx()
                val halfStroke = strokeWidth / 2
                
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = halfStroke,
                            top = halfStroke,
                            right = size.width - halfStroke,
                            bottom = size.height - halfStroke,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
                        )
                    )
                }
                
                val pathMeasure = androidx.compose.ui.graphics.PathMeasure()
                pathMeasure.setPath(path, false)
                val pathLength = pathMeasure.length
                
                if (pathLength > 0f && !pathLength.isNaN()) {
                    val progressPath = Path()
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = pathLength * animatedProgress.coerceIn(0f, 1f),
                        destination = progressPath,
                        startWithMoveTo = true
                    )
                    
                    drawPath(
                        path = progressPath,
                        color = progressColor.copy(alpha = pulseAlpha),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
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
    isEnabled: Boolean = true
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
    
    LaunchedEffect(timerState.isActive) {
        if (timerState.isActive && !circleCentered) {
            showSetupContent = false
            showSetupCircleContent = false
            delay(200)
            circleCentered = true
            delay(500)
            showTimerContent = true
            showFillProgress = true
        } else if (!timerState.isActive && circleCentered) {
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
        targetValue = if (circleCentered) {
            with(density) { 
                (screenHeight - 200.dp) / 2 - 100.dp
            }
        } else {
            0.dp
        },
        animationSpec = tween(500, easing = DecelerateEasing),
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "定时器",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = setupAlpha)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .size(180.dp)
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                StartButton(
                    onStart = { TimerManager.startTimer() },
                    textAlpha = setupAlpha,
                    isEnabled = isEnabled
                )
            }
        }
        
        if (timerState.isFinished) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
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
    screen: Screen,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val iconRes = when (screen) {
        is Screen.Home -> R.drawable.ic_home
        is Screen.Scattered -> R.drawable.ic_scattered
        is Screen.Play -> R.drawable.ic_play
        is Screen.Setting -> R.drawable.ic_setting
        else -> R.drawable.ic_home
    }
    
    val icon = ImageVector.vectorResource(id = iconRes)
    
    Column(
        modifier = Modifier
            .size(NavItemSize)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = screen.title,
            tint = if (isSelected) selectedColor else NavItemUnselected,
            modifier = Modifier.size(NavIconSize)
        )
    }
}
