package com.bicy.whitenoise.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bicy.whitenoise.storage.config.FrostedGlassConfig
import com.bicy.whitenoise.storage.config.FrostedGlassScopeConfig
import com.bicy.whitenoise.ui.theme.ThemeColorManager

/**
 * 毛玻璃对话框。
 * 装饰方案：半透明背景 + 高光渐变 + 暗度叠加。
 *
 * @param scrollableContent 可滚动的内容区域（用于长列表等）
 * @param bottomContent 固定在底部的按钮区域
 */
@Composable
fun GlassAlertDialogSimple(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
    scrollableContent: @Composable () -> Unit = {},
    bottomContent: @Composable () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val maxDialogHeight = if (isLandscape) configuration.screenHeightDp.dp * 0.7f else configuration.screenHeightDp.dp * 0.85f

    val frostedEnabled by FrostedGlassConfig.enabledFlow.collectAsState()
    val scopeEnabled by FrostedGlassScopeConfig.flow(FrostedGlassScopeConfig.SCOPE_DIALOGS).collectAsState()
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()
    val blur by FrostedGlassConfig.blurFlow.collectAsState()
    val opacity by FrostedGlassConfig.opacityFlow.collectAsState()
    val edgeHighlight by FrostedGlassConfig.edgeHighlightFlow.collectAsState()
    val darkness by FrostedGlassConfig.darknessFlow.collectAsState()
    val shape = RoundedCornerShape(cornerRadius.dp)
    val bgColor = themeColor.navBg

    val shouldApplyFrosted = frostedEnabled && scopeEnabled

    DisposableEffect(Unit) {
        DialogBlurState.onDialogShown()
        onDispose { DialogBlurState.onDialogDismissed() }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = maxDialogHeight)
                .clip(shape)
                .then(
                    if (shouldApplyFrosted) {
                        val baseAlpha = (0.3f + opacity * 0.55f).coerceIn(0f, 1f)
                        val highlightIntensity = blur * 0.25f
                        val highlightGradient = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = highlightIntensity),
                                Color.White.copy(alpha = highlightIntensity * 0.35f),
                                Color.White.copy(alpha = highlightIntensity * 0.08f),
                                Color.White.copy(alpha = highlightIntensity * 0.4f),
                            )
                        )
                        val darknessColor = if (darkness > 0f) {
                            Color.Black.copy(alpha = darkness * 0.5f)
                        } else {
                            Color.Transparent
                        }
                        val edgeAlpha = edgeHighlight * 0.3f
                        val edgeBorder = if (edgeAlpha > 0f) {
                            Modifier.border(1.dp, Color.White.copy(alpha = edgeAlpha), shape)
                        } else {
                            Modifier
                        }
                        edgeBorder
                            .background(bgColor.copy(alpha = baseAlpha), shape)
                            .background(highlightGradient, shape)
                            .let { mod ->
                                if (darknessColor != Color.Transparent)
                                    mod.background(darknessColor, shape)
                                else mod
                            }
                    } else {
                        Modifier.background(bgColor, shape)
                    }
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 可滚动内容区域
                Box(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Column {
                        scrollableContent()
                    }
                }
                // 固定底部按钮区域
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    bottomContent()
                }
            }
        }
    }
}

/**
 * 兼容旧 API：使用单一 content 参数
 */
@Composable
fun GlassAlertDialogSimple(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val maxDialogHeight = if (isLandscape) configuration.screenHeightDp.dp * 0.7f else configuration.screenHeightDp.dp * 0.85f

    val frostedEnabled by FrostedGlassConfig.enabledFlow.collectAsState()
    val scopeEnabled by FrostedGlassScopeConfig.flow(FrostedGlassScopeConfig.SCOPE_DIALOGS).collectAsState()
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()
    val blur by FrostedGlassConfig.blurFlow.collectAsState()
    val opacity by FrostedGlassConfig.opacityFlow.collectAsState()
    val edgeHighlight by FrostedGlassConfig.edgeHighlightFlow.collectAsState()
    val darkness by FrostedGlassConfig.darknessFlow.collectAsState()
    val shape = RoundedCornerShape(cornerRadius.dp)
    val bgColor = themeColor.navBg

    val shouldApplyFrosted = frostedEnabled && scopeEnabled

    DisposableEffect(Unit) {
        DialogBlurState.onDialogShown()
        onDispose { DialogBlurState.onDialogDismissed() }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = maxDialogHeight)
                .clip(shape)
                .then(
                    if (shouldApplyFrosted) {
                        val baseAlpha = (0.3f + opacity * 0.55f).coerceIn(0f, 1f)
                        val highlightIntensity = blur * 0.25f
                        val highlightGradient = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = highlightIntensity),
                                Color.White.copy(alpha = highlightIntensity * 0.35f),
                                Color.White.copy(alpha = highlightIntensity * 0.08f),
                                Color.White.copy(alpha = highlightIntensity * 0.4f),
                            )
                        )
                        val darknessColor = if (darkness > 0f) {
                            Color.Black.copy(alpha = darkness * 0.5f)
                        } else {
                            Color.Transparent
                        }
                        val edgeAlpha = edgeHighlight * 0.3f
                        val edgeBorder = if (edgeAlpha > 0f) {
                            Modifier.border(1.dp, Color.White.copy(alpha = edgeAlpha), shape)
                        } else {
                            Modifier
                        }
                        edgeBorder
                            .background(bgColor.copy(alpha = baseAlpha), shape)
                            .background(highlightGradient, shape)
                            .let { mod ->
                                if (darknessColor != Color.Transparent)
                                    mod.background(darknessColor, shape)
                                else mod
                            }
                    } else {
                        Modifier.background(bgColor, shape)
                    }
                )
        ) {
            content()
        }
    }
}

@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    confirmText: String? = null,
    dismissText: String? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    cornerRadius: Int = 16,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    GlassAlertDialogSimple(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        cornerRadius = cornerRadius
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
            Box(modifier = Modifier.fillMaxWidth().padding(top = if (title != null) 24.dp else 16.dp)) {
                if (dismissText != null && onDismiss != null) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) { Text(text = dismissText) }
                }
                if (confirmText != null && onConfirm != null) {
                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) { Text(text = confirmText, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}
