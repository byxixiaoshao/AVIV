package com.bicy.whitenoise.ui.components.glass

/**
 * 液态玻璃效果模式
 */
enum class GlassMode {
    OFF,        // 关闭：标准半透明背景
    COMPATIBLE, // 兼容模式：渐变背景 + 半透明叠加（所有 Android 版本）
    PERFECT     // 完美模式：AGSL Shader 效果（仅 Android 13+）
}