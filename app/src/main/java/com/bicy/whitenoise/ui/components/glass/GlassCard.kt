package com.bicy.whitenoise.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bicy.whitenoise.storage.config.FrostedGlassConfig
import com.bicy.whitenoise.storage.config.FrostedGlassScopeConfig
import com.bicy.whitenoise.ui.theme.ThemeColorManager

/**
 * 毛玻璃卡片。
 * 装饰方案：半透明背景 + 高光渐变 + 边缘高光 + 暗度叠加。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val frostedEnabled by FrostedGlassConfig.enabledFlow.collectAsState()
    val scopeEnabled by FrostedGlassScopeConfig.flow(FrostedGlassScopeConfig.SCOPE_CARDS).collectAsState()
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()

    if (frostedEnabled && scopeEnabled) {
        FrostedCard(modifier, shape, content)
    } else {
        Box(modifier = modifier.background(themeColor.navBg, shape), content = content)
    }
}

@Composable
private fun FrostedCard(
    modifier: Modifier,
    shape: CornerBasedShape,
    content: @Composable BoxScope.() -> Unit,
) {
    val blur by FrostedGlassConfig.blurFlow.collectAsState()
    val opacity by FrostedGlassConfig.opacityFlow.collectAsState()
    val edgeHighlight by FrostedGlassConfig.edgeHighlightFlow.collectAsState()
    val darkness by FrostedGlassConfig.darknessFlow.collectAsState()
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()

    val bgColor = themeColor.navBg

    // opacity 映射到背景不透明度
    val baseAlpha = (0.2f + opacity * 0.6f).coerceIn(0f, 1f)

    // 高光渐变（顶部亮 → 底部暗，模拟毛玻璃受光）
    val highlightIntensity = blur * 0.25f
    val highlightGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = highlightIntensity),
            Color.White.copy(alpha = highlightIntensity * 0.35f),
            Color.White.copy(alpha = highlightIntensity * 0.08f),
            Color.White.copy(alpha = highlightIntensity * 0.4f),
        )
    )

    // 暗度层
    val darknessColor = if (darkness > 0f) {
        Color.Black.copy(alpha = darkness * 0.5f)
    } else {
        Color.Transparent
    }

    // 边缘高光边框
    val edgeAlpha = edgeHighlight * 0.3f
    val edgeBorder = if (edgeAlpha > 0f) {
        Modifier.border(1.dp, Color.White.copy(alpha = edgeAlpha), shape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(edgeBorder)
            .background(bgColor.copy(alpha = baseAlpha), shape)
            .background(highlightGradient, shape)
            .let { mod ->
                if (darknessColor != Color.Transparent)
                    mod.background(darknessColor, shape)
                else mod
            },
        content = content
    )
}

/**
 * 毛玻璃分类 section，与 GlassCard 相同模式。
 */
@Composable
fun GlassCategorySection(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val frostedEnabled by FrostedGlassConfig.enabledFlow.collectAsState()
    val scopeEnabled by FrostedGlassScopeConfig.flow(FrostedGlassScopeConfig.SCOPE_SECTIONS).collectAsState()
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()

    if (frostedEnabled && scopeEnabled) {
        FrostedCard(modifier, shape, content)
    } else {
        Box(modifier = modifier.background(themeColor.navBg, shape), content = content)
    }
}
