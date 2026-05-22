package com.bicy.whitenoise.yODW.SrEO.WIXN

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun RollingNumber(
    value: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = MaterialTheme.colorScheme.primary,
    alpha: Float = 1f
) {
    var displayedValue by remember { mutableIntStateOf(value) }
    var targetValue by remember { mutableIntStateOf(value) }
    
    LaunchedEffect(value) {
        if (value != targetValue) {
            targetValue = value
        }
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (targetValue != displayedValue) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "rollingProgress"
    )
    
    val displayValue = if (animatedProgress < 1f) {
        displayedValue + ((targetValue - displayedValue) * animatedProgress).toInt()
    } else {
        targetValue.also { displayedValue = it }
    }
    
    Text(
        text = String.format("%02d", displayValue),
        style = style,
        fontWeight = FontWeight.Bold,
        color = color.copy(alpha = alpha),
        modifier = modifier
    )
}
