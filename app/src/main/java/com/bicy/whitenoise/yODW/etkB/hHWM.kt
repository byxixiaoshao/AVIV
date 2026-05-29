package com.bicy.whitenoise.yODW.etkB

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.bicy.whitenoise.R
import com.bicy.whitenoise.yODW.ZFNn.NavItemUnselected
import com.bicy.whitenoise.yODW.ZFNn.ThemeColorManager

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onRouteSelected: (String) -> Unit
) {
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()
    val navBgColor = themeColor.navBg
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(navBgColor)
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                NavItem(
                    screen = screen,
                    isSelected = currentRoute == screen.route,
                    selectedColor = themeColor.navItemSelected,
                    onClick = { onRouteSelected(screen.route) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    screen: Screen,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val iconRes = when (screen) {
        is Screen.Home -> R.drawable.ic_home
        is Screen.Scattered -> R.drawable.ic_scattered
        is Screen.Play -> R.drawable.ic_play
        is Screen.Setting -> R.drawable.ic_setting
        else -> R.drawable.ic_home
    }

    val color = if (isSelected) selectedColor else NavItemUnselected

    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = iconRes),
            contentDescription = stringResource(screen.titleResId),
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}
