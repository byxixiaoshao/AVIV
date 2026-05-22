package com.bicy.whitenoise.yODW.SrEO

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun InteractiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    colors: SliderColors = SliderDefaults.colors(),
    trackHeight: Dp = 4.dp,
    thumbRadiusDefault: Dp = 6.dp,
    thumbRadiusPressed: Dp = 10.dp,
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    var sliderWidth by remember { mutableStateOf(0) }
    
    val thumbRadius by animateDpAsState(
        targetValue = if (isPressed) thumbRadiusPressed else thumbRadiusDefault,
        label = "thumbRadius"
    )
    
    val density = LocalDensity.current
    
    val rawFraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)
    
    val fraction = if (steps > 0) {
        val stepCount = steps + 1
        val stepSize = 1f / stepCount
        val steppedIndex = (rawFraction * stepCount).toInt()
        steppedIndex * stepSize
    } else {
        rawFraction
    }
    
    val actualColors = if (enabled) colors else SliderDefaults.colors(
        thumbColor = colors.thumbColor.copy(alpha = 0.38f),
        activeTrackColor = colors.activeTrackColor.copy(alpha = 0.38f),
        inactiveTrackColor = colors.inactiveTrackColor.copy(alpha = 0.12f)
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .onSizeChanged { sliderWidth = it.width }
            .then(
                if (enabled) {
                    Modifier.pointerInput(valueRange, steps) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            
                            if (sliderWidth > 0) {
                                val x = down.position.x.coerceIn(0f, sliderWidth.toFloat())
                                val rawFraction = x / sliderWidth
                                
                                val newFraction = if (steps > 0) {
                                    val stepCount = steps + 1
                                    val stepSize = 1f / stepCount
                                    val steppedIndex = (rawFraction * stepCount).toInt()
                                    steppedIndex * stepSize
                                } else {
                                    rawFraction
                                }
                                
                                val newValue = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                                onValueChange(newValue)
                            }
                            
                            drag(down.id) { change ->
                                if (sliderWidth > 0) {
                                    val x = change.position.x.coerceIn(0f, sliderWidth.toFloat())
                                    val rawFraction = x / sliderWidth
                                    
                                    val newFraction = if (steps > 0) {
                                        val stepCount = steps + 1
                                        val stepSize = 1f / stepCount
                                        val steppedIndex = (rawFraction * stepCount).toInt()
                                        steppedIndex * stepSize
                                    } else {
                                        rawFraction
                                    }
                                    
                                    val newValue = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                                    onValueChange(newValue)
                                }
                            }
                            
                            isPressed = false
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(actualColors.inactiveTrackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(actualColors.activeTrackColor)
            )
        }
        
        with(density) {
            val thumbOffset = (fraction * sliderWidth - thumbRadius.toPx()).toInt()
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffset.toDp())
                    .size(thumbRadius * 2)
                    .clip(CircleShape)
                    .background(actualColors.thumbColor)
            )
        }
    }
}
