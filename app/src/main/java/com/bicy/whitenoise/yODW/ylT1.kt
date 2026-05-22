package com.bicy.whitenoise.yODW

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bicy.whitenoise.yODW.SrEO.ExpandableNavBar
import com.bicy.whitenoise.yODW.SrEO.WIXN.BottomNavTotalHeight
import com.bicy.whitenoise.yODW.SrEO.ExpandableTopBar
import com.bicy.whitenoise.yODW.etkB.screens
import com.bicy.whitenoise.yODW.NvYq.HomeScreen
import com.bicy.whitenoise.yODW.NvYq.PlayScreen
import com.bicy.whitenoise.yODW.NvYq.ScatteredScreen
import com.bicy.whitenoise.yODW.NvYq.SettingScreen
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val ANIM_DURATION = 400
private val TopBarHeight = 48.dp
private val ContentPaddingTop = 8.dp
private val ContentPaddingBottom = 16.dp
private val DecelerateEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}

@Composable
fun MainScreen() {
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val context = LocalContext.current
    
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var expandProgress by remember { mutableFloatStateOf(0f) }
    var topBarExpandProgress by remember { mutableFloatStateOf(0f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isTopBarInteracting by remember { mutableStateOf(false) }
    var isNavInteracting by remember { mutableStateOf(false) }
    var forceCollapseTopBar by remember { mutableStateOf(false) }
    var forceCollapseNavBar by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val pageOffset = remember { Animatable(0f) }
    
    val contentAlpha = 1f - expandProgress * 0.7f
    val mainContentAlpha = (1f - topBarExpandProgress).coerceIn(0f, 1f)
    
    var lastCollapseTime by remember { mutableLongStateOf(0L) }
    val collapseCooldown = 600L
    
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = BottomNavTotalHeight + ContentPaddingBottom)
        ) {
            Spacer(
                modifier = Modifier
                    .height(ContentPaddingTop)
                    .alpha(contentAlpha)
            )
            
            Spacer(modifier = Modifier.height(TopBarHeight))
            
            Spacer(modifier = Modifier.height(TopBarHeight))
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(contentAlpha * mainContentAlpha)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragOffset = 0f
                                scope.launch {
                                    pageOffset.stop()
                                }
                            },
                            onDragEnd = {
                                val threshold = screenWidthPx * 0.3f
                                val currentOffset = pageOffset.value
                                
                                if (abs(dragOffset) > threshold) {
                                    val targetPage = when {
                                        dragOffset > 0 && currentPageIndex > 0 -> currentPageIndex - 1
                                        dragOffset < 0 && currentPageIndex < 3 -> currentPageIndex + 1
                                        else -> currentPageIndex
                                    }
                                    
                                    if (targetPage != currentPageIndex) {
                                        scope.launch {
                                            pageOffset.animateTo(
                                                targetValue = targetPage.toFloat(),
                                                animationSpec = tween(
                                                    durationMillis = 400,
                                                    easing = DecelerateEasing
                                                )
                                            )
                                        }
                                        currentPageIndex = targetPage
                                    } else {
                                        scope.launch {
                                            pageOffset.animateTo(
                                                targetValue = currentPageIndex.toFloat(),
                                                animationSpec = tween(
                                                    durationMillis = 300,
                                                    easing = DecelerateEasing
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        pageOffset.animateTo(
                                            targetValue = currentPageIndex.toFloat(),
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = DecelerateEasing
                                            )
                                        )
                                    }
                                }
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragOffset += dragAmount
                                val newOffset = currentPageIndex - dragAmount / screenWidthPx
                                scope.launch {
                                    pageOffset.snapTo(newOffset.coerceIn(0f, 3f))
                                }
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
                    content = { PlayScreen() }
                )
                PageContent(
                    pageIndex = 3,
                    pageOffset = pageOffset.value,
                    screenWidthPx = screenWidthPx,
                    content = { SettingScreen() }
                )
            }
        }
        
        ExpandableTopBar(
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
        
        ExpandableNavBar(
            currentRoute = screens[currentPageIndex].route,
            onRouteSelected = { route ->
                val targetIndex = screens.indexOfFirst { it.route == route }
                if (targetIndex != currentPageIndex && targetIndex >= 0) {
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
            }
        )
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
