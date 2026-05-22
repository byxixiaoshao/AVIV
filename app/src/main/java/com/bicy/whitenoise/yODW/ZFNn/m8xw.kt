package com.bicy.whitenoise.yODW.ZFNn

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ShadowConfig(
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 0.dp,
    val blurRadius: Dp = 8.dp,
    val spreadRadius: Dp = 0.dp,
    val color: Color = Color.Black.copy(alpha = 0.25f)
) {
    companion object {
        val Light = ShadowConfig(
            offsetY = 2.dp,
            blurRadius = 4.dp,
            spreadRadius = 0.dp,
            color = Color.Black.copy(alpha = 0.18f)
        )
        
        val Medium = ShadowConfig(
            offsetY = 4.dp,
            blurRadius = 12.dp,
            spreadRadius = 0.dp,
            color = Color.Black.copy(alpha = 0.25f)
        )
        
        val Deep = ShadowConfig(
            offsetY = 8.dp,
            blurRadius = 24.dp,
            spreadRadius = 2.dp,
            color = Color.Black.copy(alpha = 0.35f)
        )
        
        val NavigationBar = ShadowConfig(
            offsetY = (-4).dp,
            blurRadius = 16.dp,
            spreadRadius = 0.dp,
            color = Color.Black.copy(alpha = 0.3f)
        )
        
        val TopBar = ShadowConfig(
            offsetY = 4.dp,
            blurRadius = 16.dp,
            spreadRadius = 0.dp,
            color = Color.Black.copy(alpha = 0.3f)
        )
        
        val Button = ShadowConfig(
            offsetY = 4.dp,
            blurRadius = 8.dp,
            spreadRadius = 0.dp,
            color = Color.Black.copy(alpha = 0.3f)
        )
        
        val ButtonPressed = ShadowConfig(
            offsetX = 2.dp,
            offsetY = 2.dp,
            blurRadius = 4.dp,
            spreadRadius = 0.dp,
            color = Color.Black.copy(alpha = 0.4f)
        )
    }
}

@Immutable
data class InsetShadowConfig(
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 2.dp,
    val blurRadius: Dp = 4.dp,
    val color: Color = Color.Black.copy(alpha = 0.35f)
) {
    companion object {
        val Default = InsetShadowConfig(
            offsetY = 2.dp,
            blurRadius = 4.dp,
            color = Color.Black.copy(alpha = 0.35f)
        )
        
        val Deep = InsetShadowConfig(
            offsetY = 4.dp,
            blurRadius = 8.dp,
            color = Color.Black.copy(alpha = 0.45f)
        )
    }
}

fun Modifier.dropShadow(
    config: ShadowConfig = ShadowConfig.Medium,
    shape: Shape = RoundedCornerShape(0.dp),
    clip: Boolean = true
): Modifier = this.drawBehind {
    if (size.width.isNaN() || size.height.isNaN() || size.width <= 0f || size.height <= 0f) {
        return@drawBehind
    }
    
    val spreadPx = config.spreadRadius.toPx()
    val blurPx = config.blurRadius.toPx()
    
    drawIntoCanvas { canvas ->
        val nativePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.TRANSPARENT
            setShadowLayer(
                blurPx,
                config.offsetX.toPx(),
                config.offsetY.toPx(),
                config.color.toArgb()
            )
        }
        
        val outline = shape.createOutline(size, layoutDirection, this)
        
        when (outline) {
            is Outline.Rectangle -> {
                canvas.nativeCanvas.drawRect(
                    -spreadPx,
                    -spreadPx,
                    size.width + spreadPx,
                    size.height + spreadPx,
                    nativePaint
                )
            }
            is Outline.Rounded -> {
                val radiusX = outline.roundRect.topLeftCornerRadius.x
                val radiusY = outline.roundRect.topLeftCornerRadius.y
                canvas.nativeCanvas.drawRoundRect(
                    -spreadPx,
                    -spreadPx,
                    size.width + spreadPx,
                    size.height + spreadPx,
                    radiusX,
                    radiusY,
                    nativePaint
                )
            }
            is Outline.Generic -> {
            }
        }
    }
}.then(if (clip) Modifier.clip(shape) else Modifier)

fun Modifier.insetShadow(
    config: InsetShadowConfig = InsetShadowConfig.Default,
    shape: Shape = RoundedCornerShape(0.dp),
    clip: Boolean = true
): Modifier = this.drawBehind {
    if (size.width.isNaN() || size.height.isNaN() || size.width <= 0f || size.height <= 0f) {
        return@drawBehind
    }
    
    val shadowColor = config.color
    val blurPx = config.blurRadius.toPx()
    val offsetY = config.offsetY.toPx()
    val offsetX = config.offsetX.toPx()
    
    drawIntoCanvas { canvas ->
        val outline = shape.createOutline(size, layoutDirection, this)
        
        when (outline) {
            is Outline.Rectangle -> {
                canvas.save()
                canvas.clipRect(0f, 0f, size.width, size.height)
                
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(shadowColor, Color.Transparent),
                        startY = 0f,
                        endY = blurPx * 2
                    ),
                    topLeft = Offset(0f, offsetY - blurPx),
                    size = Size(size.width, blurPx * 2)
                )
                
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, shadowColor),
                        startY = size.height - blurPx * 2,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, size.height - blurPx * 2 - offsetY),
                    size = Size(size.width, blurPx * 2)
                )
                
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(shadowColor, Color.Transparent),
                        startX = 0f,
                        endX = blurPx * 2
                    ),
                    topLeft = Offset(offsetX - blurPx, 0f),
                    size = Size(blurPx * 2, size.height)
                )
                
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, shadowColor),
                        startX = size.width - blurPx * 2,
                        endX = size.width
                    ),
                    topLeft = Offset(size.width - blurPx * 2 - offsetX, 0f),
                    size = Size(blurPx * 2, size.height)
                )
                
                canvas.restore()
            }
            is Outline.Rounded -> {
                canvas.save()
                val roundRect = outline.roundRect
                
                val path = Path().apply {
                    addRoundRect(roundRect)
                }
                canvas.clipPath(path)
                
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(shadowColor, Color.Transparent),
                        startY = 0f,
                        endY = blurPx * 2
                    ),
                    topLeft = Offset(0f, offsetY - blurPx),
                    size = Size(size.width, blurPx * 2),
                    cornerRadius = roundRect.topLeftCornerRadius
                )
                
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, shadowColor),
                        startY = size.height - blurPx * 2,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, size.height - blurPx * 2 - offsetY),
                    size = Size(size.width, blurPx * 2),
                    cornerRadius = roundRect.topLeftCornerRadius
                )
                
                canvas.restore()
            }
            is Outline.Generic -> {
                canvas.save()
                canvas.clipPath(outline.path)
                
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(shadowColor, Color.Transparent),
                        startY = 0f,
                        endY = blurPx * 2
                    ),
                    topLeft = Offset(0f, offsetY - blurPx),
                    size = Size(size.width, blurPx * 2)
                )
                
                canvas.restore()
            }
        }
    }
}.then(if (clip) Modifier.clip(shape) else Modifier)

fun Modifier.advancedInsetShadow(
    config: InsetShadowConfig = InsetShadowConfig.Default,
    cornerRadius: Dp = 0.dp,
    clip: Boolean = true
): Modifier = this.drawBehind {
    if (size.width.isNaN() || size.height.isNaN() || size.width <= 0f || size.height <= 0f) {
        return@drawBehind
    }
    
    val shadowColor = config.color
    val blurPx = config.blurRadius.toPx()
    val offsetX = config.offsetX.toPx()
    val offsetY = config.offsetY.toPx()
    val radius = cornerRadius.toPx()
    
    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.TRANSPARENT
            
            setShadowLayer(
                -blurPx,
                offsetX,
                offsetY,
                shadowColor.toArgb()
            )
            
            val rect = android.graphics.RectF(
                radius,
                radius,
                size.width - radius,
                size.height - radius
            )
            
            nativeCanvas.drawRoundRect(rect, radius, radius, this)
        }
    }
}.then(if (clip) Modifier.clip(RoundedCornerShape(cornerRadius)) else Modifier)

fun Modifier.lightShadow(
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier = this
    .dropShadow(ShadowConfig.Light, shape, clip = false)
    .clip(shape)

fun Modifier.mediumShadow(
    shape: Shape = RoundedCornerShape(12.dp)
): Modifier = this
    .dropShadow(ShadowConfig.Medium, shape, clip = false)
    .clip(shape)

fun Modifier.deepShadow(
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier = this
    .dropShadow(ShadowConfig.Deep, shape, clip = false)
    .clip(shape)

fun Modifier.navigationBarShadow(
    shape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
): Modifier = this
    .dropShadow(ShadowConfig.NavigationBar, shape, clip = false)
    .clip(shape)

fun Modifier.topBarShadow(
    shape: Shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
): Modifier = this
    .dropShadow(ShadowConfig.TopBar, shape, clip = false)
    .clip(shape)

fun Modifier.buttonShadow(
    isPressed: Boolean = false,
    shape: Shape = RoundedCornerShape(12.dp),
    cornerRadius: Dp = 12.dp
): Modifier {
    return if (isPressed) {
        this
            .advancedInsetShadow(
                config = InsetShadowConfig.Deep,
                cornerRadius = cornerRadius,
                clip = false
            )
            .clip(shape)
    } else {
        this
            .dropShadow(ShadowConfig.Button, shape, clip = false)
            .clip(shape)
    }
}
