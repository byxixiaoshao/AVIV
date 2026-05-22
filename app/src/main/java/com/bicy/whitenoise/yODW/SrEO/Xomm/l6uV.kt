package com.bicy.whitenoise.yODW.SrEO.Xomm

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

fun calculateTransitionProgress(
    progress: Float,
    isOnRight: Boolean,
    screenWidth: Dp,
    screenHeight: Dp
): TransitionProgress {
    val p = progress
    
    val mainCenterX = screenWidth.value / 2f
    val sidebarContentWidth = SidebarWidth.value - 8f
    val sidebarCenterX = if (isOnRight) {
        screenWidth.value - sidebarContentWidth / 2f
    } else {
        sidebarContentWidth / 2f
    }
    
    val albumScale = SidebarAlbumSize.value / MainAlbumSize.value
    val titleScale = SidebarTitleFontSize.value / MainTitleFontSize.value
    val controlScale = SidebarControlSize.value / MainControlSize.value
    val playButtonScale = SidebarPlayButtonSize.value / MainPlayButtonSize.value
    
    val horizontalDistance = sidebarCenterX - mainCenterX
    
    val albumOffsetX = p * horizontalDistance
    val albumOffsetY = 0f
    
    val titleOffsetX = p * horizontalDistance
    val titleOffsetY = 0f
    
    val artistOffsetX = titleOffsetX
    val artistOffsetY = 0f
    
    val controlsOffsetX = p * horizontalDistance
    val controlsOffsetY = 0f
    
    val prevButtonOffsetX = controlsOffsetX - p * 20f
    val nextButtonOffsetX = controlsOffsetX + p * 20f
    
    val mainContentAlpha = if (p >= 1f) 0f else 1f
    val extraButtonsAlpha = 1f - (p * 2f).coerceIn(0f, 1f)
    val horizontalProgressAlpha = 1f - (p * 2f).coerceIn(0f, 1f)
    val verticalProgressAlpha = if (p >= 1f) 1f else 0f
    val featureButtonsAlpha = 1f - (p * 2f).coerceIn(0f, 1f)
    
    return TransitionProgress(
        album = ElementTransition(
            scale = 1f - p * (1f - albumScale),
            offsetX = albumOffsetX,
            offsetY = albumOffsetY,
            alpha = mainContentAlpha
        ),
        title = ElementTransition(
            scale = 1f - p * (1f - titleScale),
            offsetX = titleOffsetX,
            offsetY = titleOffsetY,
            alpha = mainContentAlpha
        ),
        artist = ElementTransition(
            scale = 1f - p * (1f - titleScale),
            offsetX = artistOffsetX,
            offsetY = artistOffsetY,
            alpha = mainContentAlpha
        ),
        prevButton = ElementTransition(
            scale = 1f - p * (1f - controlScale),
            offsetX = prevButtonOffsetX,
            offsetY = controlsOffsetY,
            alpha = mainContentAlpha
        ),
        playButton = ElementTransition(
            scale = 1f - p * (1f - playButtonScale),
            offsetX = controlsOffsetX,
            offsetY = controlsOffsetY,
            alpha = mainContentAlpha
        ),
        nextButton = ElementTransition(
            scale = 1f - p * (1f - controlScale),
            offsetX = nextButtonOffsetX,
            offsetY = controlsOffsetY,
            alpha = mainContentAlpha
        ),
        shuffleButton = ElementTransition(
            scale = 1f,
            offsetX = 0f,
            offsetY = 0f,
            alpha = extraButtonsAlpha
        ),
        repeatButton = ElementTransition(
            scale = 1f,
            offsetX = 0f,
            offsetY = 0f,
            alpha = extraButtonsAlpha
        ),
        horizontalProgress = ElementTransition(
            scale = 1f,
            offsetX = 0f,
            offsetY = 0f,
            alpha = horizontalProgressAlpha
        ),
        verticalProgress = ElementTransition(
            scale = 1f,
            offsetX = 0f,
            offsetY = 0f,
            alpha = verticalProgressAlpha
        ),
        featureButtons = ElementTransition(
            scale = 1f,
            offsetX = 0f,
            offsetY = 0f,
            alpha = featureButtonsAlpha
        ),
        panelSlide = p
    )
}

@Composable
fun SlideInPanel(
    panelProgress: Float,
    slideProgress: Float,
    isOnRight: Boolean,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedSlide = AnimationEasing.transform(slideProgress)
    val offsetX = if (isOnRight) {
        (1f - animatedSlide) * 200f
    } else {
        -(1f - animatedSlide) * 200f
    }
    
    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = offsetX
                alpha = animatedSlide
            }
    ) {
        content()
    }
}
