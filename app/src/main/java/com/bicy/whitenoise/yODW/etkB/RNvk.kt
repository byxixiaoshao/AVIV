package com.bicy.whitenoise.yODW.etkB

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "主页")
    object Scattered : Screen("scattered", "散点")
    object Play : Screen("play", "播放")
    object Setting : Screen("setting", "设置")
}

val screens = listOf(
    Screen.Home,
    Screen.Scattered,
    Screen.Play,
    Screen.Setting
)
