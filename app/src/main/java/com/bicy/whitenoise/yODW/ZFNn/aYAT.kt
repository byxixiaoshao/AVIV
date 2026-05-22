package com.bicy.whitenoise.yODW.ZFNn

import androidx.compose.ui.graphics.Color

data class ThemeColorScheme(
    val id: String,
    val name: String,
    val accent: Color,
    val primary: Color,
    val background: Color,
    val text: Color
) {
    val isLight: Boolean
        get() = background.red + background.green + background.blue > 1.5f
    
    val onPrimary: Color
        get() = if (isLight) Color.White else text
    
    val surface: Color
        get() = if (isLight) {
            Color(
                red = background.red,
                green = background.green,
                blue = background.blue,
                alpha = 1f
            ).let { bg ->
                Color(
                    red = (bg.red * 255 + 5).coerceAtMost(255f) / 255f,
                    green = (bg.green * 255 + 5).coerceAtMost(255f) / 255f,
                    blue = (bg.blue * 255 + 5).coerceAtMost(255f) / 255f
                )
            }
        } else {
            Color(
                red = (background.red * 255 + 15).coerceAtMost(255f) / 255f,
                green = (background.green * 255 + 15).coerceAtMost(255f) / 255f,
                blue = (background.blue * 255 + 15).coerceAtMost(255f) / 255f
            )
        }
    
    val surfaceVariant: Color
        get() = if (isLight) {
            Color(
                red = (background.red * 255 - 10).coerceAtLeast(0f) / 255f,
                green = (background.green * 255 - 10).coerceAtLeast(0f) / 255f,
                blue = (background.blue * 255 - 10).coerceAtLeast(0f) / 255f
            )
        } else {
            Color(
                red = (background.red * 255 + 25).coerceAtMost(255f) / 255f,
                green = (background.green * 255 + 25).coerceAtMost(255f) / 255f,
                blue = (background.blue * 255 + 25).coerceAtMost(255f) / 255f
            )
        }
    
    val onBackground: Color
        get() = text
    
    val onSurface: Color
        get() = text
    
    val onSurfaceVariant: Color
        get() = if (isLight) {
            Color(
                red = (text.red * 255 * 0.6f + background.red * 255 * 0.4f) / 255f,
                green = (text.green * 255 * 0.6f + background.green * 255 * 0.4f) / 255f,
                blue = (text.blue * 255 * 0.6f + background.blue * 255 * 0.4f) / 255f
            )
        } else {
            Color(
                red = (text.red * 255 * 0.7f + background.red * 255 * 0.3f) / 255f,
                green = (text.green * 255 * 0.7f + background.green * 255 * 0.3f) / 255f,
                blue = (text.blue * 255 * 0.7f + background.blue * 255 * 0.3f) / 255f
            )
        }
    
    val navBg: Color
        get() = Color(
            red = background.red,
            green = background.green,
            blue = background.blue,
            alpha = 0.9f
        )
    
    val navItemSelected: Color
        get() = accent
    
    val navItemUnselected: Color
        get() = if (isLight) Color(0xFF9E9E9E) else Color(0xFF757575)
    
    val primaryVariant: Color
        get() = Color(
            red = (primary.red * 255 * 0.85f).coerceAtMost(255f) / 255f,
            green = (primary.green * 255 * 0.85f).coerceAtMost(255f) / 255f,
            blue = (primary.blue * 255 * 0.85f).coerceAtMost(255f) / 255f
        )
    
    val secondary: Color
        get() = if (isLight) {
            Color(
                red = (primary.red * 255 * 0.7f + background.red * 255 * 0.3f) / 255f,
                green = (primary.green * 255 * 0.7f + background.green * 255 * 0.3f) / 255f,
                blue = (primary.blue * 255 * 0.7f + background.blue * 255 * 0.3f) / 255f
            )
        } else {
            Color(
                red = (primary.red * 255 * 0.6f + background.red * 255 * 0.4f) / 255f,
                green = (primary.green * 255 * 0.6f + background.green * 255 * 0.4f) / 255f,
                blue = (primary.blue * 255 * 0.6f + background.blue * 255 * 0.4f) / 255f
            )
        }
    
    val onSecondary: Color
        get() = if (isLight) text else Color.White
}

object ThemeColorPresets {
    
    val Default = ThemeColorScheme(
        id = "default",
        name = "默认",
        accent = Color(0xFFB8A07A),
        primary = Color(0xFFB8A07A),
        background = Color(0xFFFAF6F0),
        text = Color(0xFF3D3A35)
    )
    
    val DefaultDark = ThemeColorScheme(
        id = "default_dark",
        name = "默认·夜",
        accent = Color(0xFFD4C4A0),
        primary = Color(0xFFD4C4A0),
        background = Color(0xFF1A1816),
        text = Color(0xFFE8E6E3)
    )
    
    val OceanBlue = ThemeColorScheme(
        id = "ocean_blue",
        name = "海洋蓝",
        accent = Color(0xFF2196F3),
        primary = Color(0xFF2196F3),
        background = Color(0xFFF5F9FC),
        text = Color(0xFF1A237E)
    )
    
    val OceanBlueDark = ThemeColorScheme(
        id = "ocean_blue_dark",
        name = "海洋蓝·夜",
        accent = Color(0xFF64B5F6),
        primary = Color(0xFF64B5F6),
        background = Color(0xFF0D1B2A),
        text = Color(0xFFE3F2FD)
    )
    
    val ForestGreen = ThemeColorScheme(
        id = "forest_green",
        name = "森林绿",
        accent = Color(0xFF4CAF50),
        primary = Color(0xFF4CAF50),
        background = Color(0xFFF5F9F5),
        text = Color(0xFF1B5E20)
    )
    
    val ForestGreenDark = ThemeColorScheme(
        id = "forest_green_dark",
        name = "森林绿·夜",
        accent = Color(0xFF81C784),
        primary = Color(0xFF81C784),
        background = Color(0xFF0D1F0D),
        text = Color(0xFFE8F5E9)
    )
    
    val SunsetOrange = ThemeColorScheme(
        id = "sunset_orange",
        name = "日落橙",
        accent = Color(0xFFFF7043),
        primary = Color(0xFFFF7043),
        background = Color(0xFFFFF8F5),
        text = Color(0xFF3E2723)
    )
    
    val SunsetOrangeDark = ThemeColorScheme(
        id = "sunset_orange_dark",
        name = "日落橙·夜",
        accent = Color(0xFFFFAB91),
        primary = Color(0xFFFFAB91),
        background = Color(0xFF1F0D0A),
        text = Color(0xFFFBE9E7)
    )
    
    val PurpleDream = ThemeColorScheme(
        id = "purple_dream",
        name = "紫梦",
        accent = Color(0xFF9C27B0),
        primary = Color(0xFF9C27B0),
        background = Color(0xFFFAF5FC),
        text = Color(0xFF4A148C)
    )
    
    val PurpleDreamDark = ThemeColorScheme(
        id = "purple_dream_dark",
        name = "紫梦·夜",
        accent = Color(0xFFCE93D8),
        primary = Color(0xFFCE93D8),
        background = Color(0xFF1A0D1F),
        text = Color(0xFFF3E5F5)
    )
    
    val RosePink = ThemeColorScheme(
        id = "rose_pink",
        name = "玫瑰粉",
        accent = Color(0xFFE91E63),
        primary = Color(0xFFE91E63),
        background = Color(0xFFFFF5F8),
        text = Color(0xFF880E4F)
    )
    
    val RosePinkDark = ThemeColorScheme(
        id = "rose_pink_dark",
        name = "玫瑰粉·夜",
        accent = Color(0xFFF48FB1),
        primary = Color(0xFFF48FB1),
        background = Color(0xFF1F0A12),
        text = Color(0xFFFCE4EC)
    )
    
    val CoffeeBrown = ThemeColorScheme(
        id = "coffee_brown",
        name = "咖啡棕",
        accent = Color(0xFF795548),
        primary = Color(0xFF795548),
        background = Color(0xFFFAF6F3),
        text = Color(0xFF3E2723)
    )
    
    val CoffeeBrownDark = ThemeColorScheme(
        id = "coffee_brown_dark",
        name = "咖啡棕·夜",
        accent = Color(0xFFA1887F),
        primary = Color(0xFFA1887F),
        background = Color(0xFF1A1410),
        text = Color(0xFFEFEBE9)
    )
    
    val PureWhite = ThemeColorScheme(
        id = "pure_white",
        name = "纯白",
        accent = Color(0xFF607D8B),
        primary = Color(0xFF607D8B),
        background = Color(0xFFFAFAFA),
        text = Color(0xFF424242)
    )
    
    val PureBlack = ThemeColorScheme(
        id = "pure_black",
        name = "纯黑",
        accent = Color(0xFF9E9E9E),
        primary = Color(0xFF9E9E9E),
        background = Color(0xFF121212),
        text = Color(0xFFE0E0E0)
    )
    
    val allPresets: List<ThemeColorScheme> = listOf(
        Default,
        DefaultDark,
        OceanBlue,
        OceanBlueDark,
        ForestGreen,
        ForestGreenDark,
        SunsetOrange,
        SunsetOrangeDark,
        PurpleDream,
        PurpleDreamDark,
        RosePink,
        RosePinkDark,
        CoffeeBrown,
        CoffeeBrownDark,
        PureWhite,
        PureBlack
    )
    
    fun getPresetById(id: String): ThemeColorScheme {
        return allPresets.find { it.id == id } ?: Default
    }
    
    fun createCustomColorScheme(
        accent: Color,
        primary: Color,
        background: Color,
        text: Color,
        name: String = "自定义"
    ): ThemeColorScheme {
        return ThemeColorScheme(
            id = "custom",
            name = name,
            accent = accent,
            primary = primary,
            background = background,
            text = text
        )
    }
}
