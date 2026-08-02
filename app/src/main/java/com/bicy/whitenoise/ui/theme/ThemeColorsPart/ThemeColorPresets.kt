package com.bicy.whitenoise.ui.theme.ThemeColorsPart

import androidx.compose.ui.graphics.Color

object ThemeColorPresets {
    val Default = ThemeColorScheme(id = "default", name = "默认", accent = Color(0xFFD4A574), primary = Color(0xFFB8A07A), background = Color(0xFFFAF6F0), text = Color(0xFF3D3A35))
    val DefaultDark = ThemeColorScheme(id = "default_dark", name = "默认·夜", accent = Color(0xFFE8C89E), primary = Color(0xFFD4C4A0), background = Color(0xFF1A1816), text = Color(0xFFE8E6E3))
    val OceanBlue = ThemeColorScheme(id = "ocean_blue", name = "海洋蓝", accent = Color(0xFF42A5F5), primary = Color(0xFF1976D2), background = Color(0xFFF5F9FC), text = Color(0xFF1A237E))
    val OceanBlueDark = ThemeColorScheme(id = "ocean_blue_dark", name = "海洋蓝·夜", accent = Color(0xFF90CAF9), primary = Color(0xFF64B5F6), background = Color(0xFF0D1B2A), text = Color(0xFFE3F2FD))
    val ForestGreen = ThemeColorScheme(id = "forest_green", name = "森林绿", accent = Color(0xFF66BB6A), primary = Color(0xFF388E3C), background = Color(0xFFF5F9F5), text = Color(0xFF1B5E20))
    val ForestGreenDark = ThemeColorScheme(id = "forest_green_dark", name = "森林绿·夜", accent = Color(0xFFA5D6A7), primary = Color(0xFF81C784), background = Color(0xFF0D1F0D), text = Color(0xFFE8F5E9))
    val SunsetOrange = ThemeColorScheme(id = "sunset_orange", name = "日落橙", accent = Color(0xFFFF8A65), primary = Color(0xFFE64A19), background = Color(0xFFFFF8F5), text = Color(0xFF3E2723))
    val SunsetOrangeDark = ThemeColorScheme(id = "sunset_orange_dark", name = "日落橙·夜", accent = Color(0xFFFFAB91), primary = Color(0xFFFF7043), background = Color(0xFF1F0D0A), text = Color(0xFFFBE9E7))
    val PurpleDream = ThemeColorScheme(id = "purple_dream", name = "紫梦", accent = Color(0xFFBA68C8), primary = Color(0xFF7B1FA2), background = Color(0xFFFAF5FC), text = Color(0xFF4A148C))
    val PurpleDreamDark = ThemeColorScheme(id = "purple_dream_dark", name = "紫梦·夜", accent = Color(0xFFE1BEE7), primary = Color(0xFFCE93D8), background = Color(0xFF1A0D1F), text = Color(0xFFF3E5F5))
    val RosePink = ThemeColorScheme(id = "rose_pink", name = "玫瑰粉", accent = Color(0xFFF06292), primary = Color(0xFFC2185B), background = Color(0xFFFFF5F8), text = Color(0xFF880E4F))
    val RosePinkDark = ThemeColorScheme(id = "rose_pink_dark", name = "玫瑰粉·夜", accent = Color(0xFFF8BBD0), primary = Color(0xFFF48FB1), background = Color(0xFF1F0A12), text = Color(0xFFFCE4EC))
    val CoffeeBrown = ThemeColorScheme(id = "coffee_brown", name = "咖啡棕", accent = Color(0xFFA1887F), primary = Color(0xFF5D4037), background = Color(0xFFFAF6F3), text = Color(0xFF3E2723))
    val CoffeeBrownDark = ThemeColorScheme(id = "coffee_brown_dark", name = "咖啡棕·夜", accent = Color(0xFFBCAAA4), primary = Color(0xFFA1887F), background = Color(0xFF1A1410), text = Color(0xFFEFEBE9))
    val PureWhite = ThemeColorScheme(id = "pure_white", name = "纯白", accent = Color(0xFF78909C), primary = Color(0xFF546E7A), background = Color(0xFFFAFAFA), text = Color(0xFF424242))
    val PureBlack = ThemeColorScheme(id = "pure_black", name = "纯黑", accent = Color(0xFFBDBDBD), primary = Color(0xFF757575), background = Color(0xFF121212), text = Color(0xFFE0E0E0))
    val MintGreen = ThemeColorScheme(id = "mint_green", name = "薄荷绿", accent = Color(0xFF80CBC4), primary = Color(0xFF00897B), background = Color(0xFFF5FAF9), text = Color(0xFF004D40))
    val MintGreenDark = ThemeColorScheme(id = "mint_green_dark", name = "薄荷绿·夜", accent = Color(0xFFB2DFDB), primary = Color(0xFF80CBC4), background = Color(0xFF0A1A18), text = Color(0xFFE0F2F1))
    val CherryRed = ThemeColorScheme(id = "cherry_red", name = "樱桃红", accent = Color(0xFFEF5350), primary = Color(0xFFC62828), background = Color(0xFFFFF5F5), text = Color(0xFFB71C1C))
    val CherryRedDark = ThemeColorScheme(id = "cherry_red_dark", name = "樱桃红·夜", accent = Color(0xFFEF9A9A), primary = Color(0xFFEF5350), background = Color(0xFF1F0A0A), text = Color(0xFFFFEBEE))
    val Lavender = ThemeColorScheme(id = "lavender", name = "薰衣草", accent = Color(0xFFB39DDB), primary = Color(0xFF5E35B1), background = Color(0xFFF8F6FC), text = Color(0xFF311B92))
    val LavenderDark = ThemeColorScheme(id = "lavender_dark", name = "薰衣草·夜", accent = Color(0xFFD1C4E9), primary = Color(0xFFB39DDB), background = Color(0xFF120D1F), text = Color(0xFFEDE7F6))

    val allPresets = listOf(Default, DefaultDark, OceanBlue, OceanBlueDark, ForestGreen, ForestGreenDark, SunsetOrange, SunsetOrangeDark, PurpleDream, PurpleDreamDark, RosePink, RosePinkDark, CoffeeBrown, CoffeeBrownDark, PureWhite, PureBlack, MintGreen, MintGreenDark, CherryRed, CherryRedDark, Lavender, LavenderDark)

    fun getPresetById(id: String) = allPresets.find { it.id == id } ?: Default

    fun createCustomColorScheme(accent: Color, primary: Color, background: Color, text: Color, name: String = "自定义") = ThemeColorScheme(id = "custom", name = name, accent = accent, primary = primary, background = background, text = text)
}
