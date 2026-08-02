package com.bicy.whitenoise.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

/**
 * Responsive dimension utilities for tablet adaptation.
 * Scales dimensions based on screen width while maintaining layout structure.
 */
object ResponsiveDimensions {
    
    // Base screen width for scaling calculations (typical phone width)
    private const val BASE_WIDTH_DP = 360
    
    /**
     * 判断是否为横屏模式
     */
    @Composable
    fun isLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp > configuration.screenHeightDp
    }
    
    /**
     * Calculate scale factor based on screen width
     * Tablets (sw600dp+) get larger scale factor
     * Landscape mode gets adjusted scaling
     */
    @Composable
    fun scaleFactor(): Float {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        val isLandscape = isLandscape()
        
        return remember(screenWidthDp, isLandscape) {
            when {
                screenWidthDp >= 900 -> 1.8f  // Large tablets
                screenWidthDp >= 720 -> 1.5f  // Medium tablets
                screenWidthDp >= 600 -> 1.3f  // Small tablets
                screenWidthDp >= 480 -> 1.15f // Large phones
                isLandscape -> 1.05f // 横屏手机：轻微放大
                else -> 1f          // Standard phones
            }
        }
    }
    
    /**
     * Scale a Dp value based on screen size
     */
    @Composable
    fun scaledDp(baseDp: Dp): Dp {
        val scale = scaleFactor()
        return (baseDp.value * scale).dp
    }
    
    /**
     * Scale a TextUnit (sp) value based on screen size
     * Use smaller scaling for text to maintain readability
     */
    @Composable
    fun scaledSp(baseSp: TextUnit): TextUnit {
        val scale = scaleFactor()
        // Use smaller scaling for text (max 1.4x) to maintain readability
        val textScale = scale.coerceAtMost(1.4f)
        return (baseSp.value * textScale).sp
    }
    
    /**
     * Scale icon size - use moderate scaling for icons
     */
    @Composable
    fun scaledIconSize(baseDp: Dp): Dp {
        val scale = scaleFactor()
        // Icons scale moderately (max 1.5x)
        val iconScale = scale.coerceAtMost(1.5f)
        return (baseDp.value * iconScale).dp
    }
    
    /**
     * Scale padding - use moderate scaling
     * Landscape mode gets slightly larger horizontal padding to utilize width
     */
    @Composable
    fun scaledPadding(baseDp: Dp): Dp {
        val scale = scaleFactor()
        val isLandscape = isLandscape()
        // Padding scales moderately
        val paddingScale = scale.coerceAtMost(1.5f)
        val finalScale = if (isLandscape) paddingScale * 1.1f else paddingScale
        return (baseDp.value * finalScale).dp
    }
    
    /**
     * Scale corner radius - use moderate scaling
     */
    @Composable
    fun scaledCornerRadius(baseDp: Dp): Dp {
        val scale = scaleFactor()
        val radiusScale = scale.coerceAtMost(1.3f)
        return (baseDp.value * radiusScale).dp
    }
    
    /**
     * Get dialog max width for current screen size
     * Landscape mode uses smaller percentage to avoid too wide dialogs
     */
    @Composable
    fun dialogMaxWidth(): Dp {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        val isLandscape = isLandscape()
        
        return remember(screenWidthDp, isLandscape) {
            when {
                screenWidthDp >= 900 -> 600.dp  // Large tablets
                screenWidthDp >= 720 -> 500.dp  // Medium tablets
                screenWidthDp >= 600 -> 450.dp  // Small tablets
                screenWidthDp >= 480 -> 400.dp  // Large phones
                isLandscape -> (screenWidthDp * 0.55f).dp.coerceAtMost(450.dp) // 横屏手机：55%宽度
                else -> (screenWidthDp * 0.9f).dp // Standard phones
            }
        }
    }
    
    /**
     * Get dialog max height for current screen size
     * Landscape mode uses smaller percentage to avoid too tall dialogs
     */
    @Composable
    fun dialogMaxHeight(): Dp {
        val configuration = LocalConfiguration.current
        val screenHeightDp = configuration.screenHeightDp
        val isLandscape = isLandscape()
        
        return remember(screenHeightDp, isLandscape) {
            when {
                screenHeightDp >= 900 -> 700.dp
                screenHeightDp >= 720 -> 600.dp
                screenHeightDp >= 600 -> 550.dp
                isLandscape -> (screenHeightDp * 0.65f).dp.coerceAtMost(400.dp) // 横屏：65%高度，最大400dp
                else -> 500.dp
            }
        }
    }
}

/**
 * Extension functions for easier use in Composables
 */

@Composable
fun Dp.scaled(): Dp = ResponsiveDimensions.scaledDp(this)

@Composable
fun TextUnit.scaled(): TextUnit = ResponsiveDimensions.scaledSp(this)

@Composable
fun Dp.scaledIcon(): Dp = ResponsiveDimensions.scaledIconSize(this)

@Composable
fun Dp.scaledPadding(): Dp = ResponsiveDimensions.scaledPadding(this)

@Composable
fun Dp.scaledCorner(): Dp = ResponsiveDimensions.scaledCornerRadius(this)