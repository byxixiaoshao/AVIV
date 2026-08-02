package com.bicy.whitenoise.ui.theme.ThemeColorsPart

import androidx.compose.ui.graphics.Color

data class ThemeColorScheme(
    val id: String,
    val name: String,
    val accent: Color,
    val primary: Color,
    val background: Color,
    val text: Color
) {
    val isLight: Boolean get() = background.red + background.green + background.blue > 1.5f
    val onPrimary: Color get() = if (isLight) Color.White else text
    val surface: Color get() = if (isLight) Color((background.red*255+5).coerceAtMost(255f)/255f, (background.green*255+5).coerceAtMost(255f)/255f, (background.blue*255+5).coerceAtMost(255f)/255f) else Color((background.red*255+15).coerceAtMost(255f)/255f, (background.green*255+15).coerceAtMost(255f)/255f, (background.blue*255+15).coerceAtMost(255f)/255f)
    val surfaceVariant: Color get() = if (isLight) Color((background.red*255-10).coerceAtLeast(0f)/255f, (background.green*255-10).coerceAtLeast(0f)/255f, (background.blue*255-10).coerceAtLeast(0f)/255f) else Color((background.red*255+25).coerceAtMost(255f)/255f, (background.green*255+25).coerceAtMost(255f)/255f, (background.blue*255+25).coerceAtMost(255f)/255f)
    val onBackground: Color get() = text
    val onSurface: Color get() = text
    val onSurfaceVariant: Color get() = if (isLight) Color((text.red*255*0.6f+background.red*255*0.4f)/255f, (text.green*255*0.6f+background.green*255*0.4f)/255f, (text.blue*255*0.6f+background.blue*255*0.4f)/255f) else Color((text.red*255*0.7f+background.red*255*0.3f)/255f, (text.green*255*0.7f+background.green*255*0.3f)/255f, (text.blue*255*0.7f+background.blue*255*0.3f)/255f)
    val navBg: Color get() = Color(background.red, background.green, background.blue, 0.9f)
    val navItemSelected: Color get() = accent
    val navItemUnselected: Color get() = if (isLight) Color(0xFF9E9E9E) else Color(0xFF757575)
    val primaryVariant: Color get() = Color((primary.red*255*0.85f).coerceAtMost(255f)/255f, (primary.green*255*0.85f).coerceAtMost(255f)/255f, (primary.blue*255*0.85f).coerceAtMost(255f)/255f)
    val secondary: Color get() = if (isLight) Color((primary.red*255*0.7f+background.red*255*0.3f)/255f, (primary.green*255*0.7f+background.green*255*0.3f)/255f, (primary.blue*255*0.7f+background.blue*255*0.3f)/255f) else Color((primary.red*255*0.6f+background.red*255*0.4f)/255f, (primary.green*255*0.6f+background.green*255*0.4f)/255f, (primary.blue*255*0.6f+background.blue*255*0.4f)/255f)
    val onSecondary: Color get() = if (isLight) text else Color.White
}
