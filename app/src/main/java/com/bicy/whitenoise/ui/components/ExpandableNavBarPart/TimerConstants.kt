package com.bicy.whitenoise.ui.components.ExpandableNavBarPart

import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.dp
import com.bicy.whitenoise.ui.navigation.Screen

val DecelerateEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}

val screens = listOf(Screen.Home, Screen.Scattered, Screen.Play, Screen.Setting)

val CollapsedHeight = 64.dp
val CollapsedMarginHorizontal = 24.dp
val CollapsedMarginBottom = 16.dp
val CollapsedCornerRadius = 32.dp
val NavItemSize = 56.dp
val NavIconSize = 28.dp
val BottomNavTotalHeight = CollapsedHeight + CollapsedMarginBottom
