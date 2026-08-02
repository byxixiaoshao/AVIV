package com.bicy.whitenoise.ui.components.ExpandableNavBarPart

import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.dp
import com.bicy.whitenoise.ui.navigation.ScreenPart.*

val DecelerateEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}


val CollapsedHeight = 64.dp
val CollapsedMarginHorizontal = 24.dp
val CollapsedMarginBottom = 24.dp // 增加底部margin，避免与系统导航栏手势冲突
val CollapsedCornerRadius = 32.dp
val NavItemSize = 56.dp
val NavIconSize = 28.dp
val BottomNavTotalHeight = CollapsedHeight + CollapsedMarginBottom
