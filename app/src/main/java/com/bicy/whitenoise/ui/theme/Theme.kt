package com.bicy.whitenoise.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.bicy.whitenoise.ui.theme.ThemeColorsPart.*

private fun createColorScheme(themeColor: ThemeColorScheme): ColorScheme {
    val isDark = !themeColor.isLight
    
    return if (isDark) {
        darkColorScheme(
            primary = themeColor.primary,
            onPrimary = themeColor.onPrimary,
            primaryContainer = themeColor.primaryVariant,
            secondary = themeColor.secondary,
            onSecondary = themeColor.onSecondary,
            background = themeColor.background,
            surface = themeColor.surface,
            surfaceVariant = themeColor.surfaceVariant,
            onBackground = themeColor.onBackground,
            onSurface = themeColor.onSurface,
            onSurfaceVariant = themeColor.onSurfaceVariant
        )
    } else {
        lightColorScheme(
            primary = themeColor.primary,
            onPrimary = themeColor.onPrimary,
            primaryContainer = themeColor.primaryVariant,
            secondary = themeColor.secondary,
            onSecondary = themeColor.onSecondary,
            background = themeColor.background,
            surface = themeColor.surface,
            surfaceVariant = themeColor.surfaceVariant,
            onBackground = themeColor.onBackground,
            onSurface = themeColor.onSurface,
            onSurfaceVariant = themeColor.onSurfaceVariant
        )
    }
}

@Composable
fun WhiteNoiseTheme(
    themeColor: ThemeColorScheme = ThemeColorPresets.Default,
    content: @Composable () -> Unit
) {
    val colorScheme = createColorScheme(themeColor)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun WhiteNoiseTheme(
    content: @Composable () -> Unit
) {
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()
    
    val colorScheme = createColorScheme(themeColor)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


