package com.bicy.whitenoise.ui.components.toast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bicy.whitenoise.storage.config.FrostedGlassConfig
import com.bicy.whitenoise.storage.config.FrostedGlassScopeConfig
import com.bicy.whitenoise.ui.theme.ThemeColorManager
import com.bicy.whitenoise.ui.theme.ThemeColorsPart.ThemeColorScheme

@Composable
fun ToastCard(
    item: ToastItem,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val isComplete = item.type == ToastType.LOADING && (item.progress ?: 0f) >= 1f
    val displayType = if (isComplete) ToastType.SUCCESS else item.type

    val isCritical = item.priority == ToastPriority.CRITICAL

    // 毛玻璃配置
    val frostedEnabled by FrostedGlassConfig.enabledFlow.collectAsState()
    val scopeEnabled by FrostedGlassScopeConfig.flow(FrostedGlassScopeConfig.SCOPE_NOTIFICATIONS).collectAsState()
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()
    val useFrosted = frostedEnabled && scopeEnabled

    // 毛玻璃参数（仅在启用时读取）
    val blur by if (useFrosted) FrostedGlassConfig.blurFlow.collectAsState() else remember { mutableFloatStateOf(0f) }
    val opacity by if (useFrosted) FrostedGlassConfig.opacityFlow.collectAsState() else remember { mutableFloatStateOf(0f) }
    val edgeHighlight by if (useFrosted) FrostedGlassConfig.edgeHighlightFlow.collectAsState() else remember { mutableFloatStateOf(0f) }
    val darkness by if (useFrosted) FrostedGlassConfig.darknessFlow.collectAsState() else remember { mutableFloatStateOf(0f) }

    // 主题色：CRITICAL 用红色背景强调，其它使用 navBg
    val bgColor = if (isCritical) {
        themeColor.navBg.copy(alpha = 0.92f)
    } else {
        themeColor.navBg
    }

    // 类型主题色（用于图标和按钮）
    val typeAccent = typeAccentColor(displayType, themeColor)

    Box(
        modifier = modifier
            .clip(shape)
            .then(buildBgModifier(useFrosted, shape, bgColor, blur, opacity, edgeHighlight, darkness, typeAccent, isCritical))
            // CRITICAL：红色描边放在最上层，确保在毛玻璃背景之上清晰可见
            .then(
                if (isCritical) Modifier
                    .border(width = 1.5.dp, color = Color(0xFFF44336).copy(alpha = 0.75f), shape = shape)
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .padding(start = if (isCritical) 4.dp else 0.dp), // 给左边框留视觉呼吸
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类型化图标（左侧）
            ToastIcon(displayType, item.progress, themeColor)

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.message,
                    color = if (isCritical) themeColor.text else themeColor.text,
                    fontSize = 14.sp,
                    fontWeight = if (isCritical) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                // 进度百分比文字（LOADING 且有确定进度时显示）
                if (item.type == ToastType.LOADING && item.progress != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(item.progress * 100).toInt()}%",
                        color = typeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 操作按钮区（右侧）
            // 1. ERROR/WARNING + onRetry → 显示「重试」
            // 2. LOADING + onCancel → 显示取消 X 图标（沿用既有样式）
            // 3. CRITICAL 任意类型 → 显示关闭 X（手动消除常驻通知）
            ActionButtons(item, typeAccent, themeColor)
        }
    }
}

/** 操作按钮区：重试 / 取消 / 关闭 */
@Composable
private fun ActionButtons(
    item: ToastItem,
    typeAccent: Color,
    themeColor: ThemeColorScheme
) {
    val isCritical = item.priority == ToastPriority.CRITICAL

    // ERROR/WARNING 带 onRetry：文字按钮「重试」
    if (item.onRetry != null && (item.type == ToastType.ERROR || item.type == ToastType.WARNING)) {
        Spacer(modifier = Modifier.width(6.dp))
        TextButton(
            onClick = { item.onRetry.invoke() },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Text(
                text = "重试",
                color = typeAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // LOADING 带 onCancel：取消 X 图标（保留既有交互）
    if (item.type == ToastType.LOADING && item.onCancel != null) {
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = { item.onCancel.invoke() },
            modifier = Modifier.size(22.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Cancel,
                contentDescription = "取消",
                tint = themeColor.text.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    // CRITICAL：手动关闭按钮（常驻通知仅靠手动消除）
    if (isCritical) {
        Spacer(modifier = Modifier.width(2.dp))
        IconButton(
            onClick = { ToastManager.removeById(item.id) },
            modifier = Modifier.size(22.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Cancel,
                contentDescription = "关闭",
                tint = themeColor.text.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** 计算每种类型对应的主题强调色 */
private fun typeAccentColor(type: ToastType, themeColor: ThemeColorScheme): Color = when (type) {
    ToastType.INFO -> themeColor.primary
    ToastType.SUCCESS -> Color(0xFF4CAF50)
    ToastType.WARNING -> Color(0xFFFFA726)
    ToastType.ERROR -> Color(0xFFF44336)
    ToastType.LOADING -> themeColor.primary
}

/**
 * 构建背景 modifier：
 * - 非毛玻璃 = 半透明纯色（CRITICAL 叠加极淡的类型色 tint）
 * - 毛玻璃 = 渐变+高光+暗度+边缘高光，并叠加极淡的类型主题色
 */
private fun buildBgModifier(
    useFrosted: Boolean, shape: RoundedCornerShape,
    bgColor: Color, blur: Float, opacity: Float, edgeHighlight: Float, darkness: Float,
    typeAccent: Color, isCritical: Boolean
): Modifier {
    // 类型 tint：在背景里叠一层极淡的强调色，使不同类型有视觉区分但不喧宾夺主
    val tintAlpha = if (isCritical) 0.10f else 0.06f
    val tintLayer = typeAccent.copy(alpha = tintAlpha)

    if (!useFrosted) {
        return Modifier
            .background(bgColor.copy(alpha = 0.88f), shape)
            .background(tintLayer, shape)
    }

    // 毛玻璃模拟：半透明背景 + 高光渐变 + 暗度层 + 边缘高光 + 类型 tint
    val baseAlpha = (0.2f + opacity * 0.6f).coerceIn(0f, 1f)
    val highlightIntensity = blur * 0.25f
    val highlightGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = highlightIntensity),
            Color.White.copy(alpha = highlightIntensity * 0.35f),
            Color.White.copy(alpha = highlightIntensity * 0.08f),
            Color.White.copy(alpha = highlightIntensity * 0.4f),
        )
    )
    val edgeAlpha = edgeHighlight * 0.3f

    return Modifier
        .let { if (edgeAlpha > 0f) it.border(1.dp, Color.White.copy(alpha = edgeAlpha), shape) else it }
        .background(bgColor.copy(alpha = baseAlpha), shape)
        .background(tintLayer, shape)
        .background(highlightGradient, shape)
        .let { m -> if (darkness > 0f) m.background(Color.Black.copy(alpha = darkness * 0.5f), shape) else m }
}

/**
 * 类型化图标：每种类型对应 Outlined 图标 + 主题色。
 * LOADING 用旋转进度环（确定进度显示百分比环，否则不确定旋转）。
 */
@Composable
private fun ToastIcon(type: ToastType, progress: Float?, themeColor: ThemeColorScheme) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        when (type) {
            ToastType.INFO -> Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = themeColor.primary,
                modifier = Modifier.size(20.dp)
            )
            ToastType.SUCCESS -> Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
            ToastType.WARNING -> Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = Color(0xFFFFA726),
                modifier = Modifier.size(20.dp)
            )
            ToastType.ERROR -> Icon(
                imageVector = Icons.Outlined.Error,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(20.dp)
            )
            ToastType.LOADING -> {
                if (progress != null) {
                    CircularProgressIndicator(
                        progress = { progress }, modifier = Modifier.size(20.dp),
                        color = themeColor.primary, strokeWidth = 2.dp,
                        trackColor = themeColor.text.copy(alpha = 0.2f),
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = themeColor.primary, strokeWidth = 2.dp,
                        trackColor = themeColor.text.copy(alpha = 0.2f),
                    )
                }
            }
        }
    }
}
