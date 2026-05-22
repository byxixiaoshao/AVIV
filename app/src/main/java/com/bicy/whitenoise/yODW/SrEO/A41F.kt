package com.bicy.whitenoise.yODW.SrEO

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bicy.whitenoise.yODW.ZFNn.InsetShadowConfig
import com.bicy.whitenoise.yODW.ZFNn.ShadowConfig
import com.bicy.whitenoise.yODW.ZFNn.advancedInsetShadow
import com.bicy.whitenoise.yODW.ZFNn.dropShadow

@Composable
fun ShadowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shadowConfig: ShadowConfig = ShadowConfig.Button,
    insetShadowConfig: InsetShadowConfig = InsetShadowConfig.Deep,
    enabled: Boolean = true,
    contentPadding: Dp = 12.dp,
    cornerRadius: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val animatedOffsetY by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 100),
        label = "buttonOffset"
    )
    
    Box(
        modifier = modifier
            .then(
                if (isPressed) {
                    Modifier
                        .advancedInsetShadow(
                            config = insetShadowConfig,
                            cornerRadius = cornerRadius,
                            clip = false
                        )
                } else {
                    Modifier.dropShadow(
                        config = shadowConfig,
                        shape = shape,
                        clip = false
                    )
                }
            )
            .background(backgroundColor, shape)
            .then(
                if (shape is RoundedCornerShape) {
                    Modifier.padding(contentPadding)
                } else {
                    Modifier.padding(contentPadding)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun ShadowCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .then(
                if (isPressed) {
                    Modifier.advancedInsetShadow(
                        config = InsetShadowConfig.Deep,
                        cornerRadius = size / 2,
                        clip = false
                    )
                } else {
                    Modifier.dropShadow(
                        config = ShadowConfig.Button,
                        shape = CircleShape,
                        clip = false
                    )
                }
            )
            .background(backgroundColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun ShadowCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    shadowConfig: ShadowConfig = ShadowConfig.Medium,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val finalShadowConfig = if (isPressed) {
        ShadowConfig(
            offsetY = shadowConfig.offsetY / 2,
            blurRadius = shadowConfig.blurRadius / 2,
            color = shadowConfig.color.copy(alpha = shadowConfig.color.alpha * 0.7f)
        )
    } else {
        shadowConfig
    }
    
    Box(
        modifier = modifier
            .dropShadow(
                config = finalShadowConfig,
                shape = shape,
                clip = false
            )
            .background(backgroundColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun ShadowIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val shape = RoundedCornerShape(size / 4)
    
    Box(
        modifier = modifier
            .then(
                if (isPressed) {
                    Modifier.advancedInsetShadow(
                        config = InsetShadowConfig.Default,
                        cornerRadius = size / 4,
                        clip = false
                    )
                } else {
                    Modifier.dropShadow(
                        config = ShadowConfig.Light,
                        shape = shape,
                        clip = false
                    )
                }
            )
            .background(backgroundColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
