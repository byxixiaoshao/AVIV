package com.bicy.whitenoise.yODW.ZFNn

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

fun ColorScheme.isLight(): Boolean {
    return background.luminance() > 0.5f
}

private fun Color.luminance(): Float {
    val r = red * 0.2126f
    val g = green * 0.7152f
    val b = blue * 0.0722f
    return r + g + b
}
