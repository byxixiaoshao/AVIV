package com.bicy.whitenoise.ui.navigation

import com.bicy.whitenoise.R

sealed class Screen(val route: String, val titleResId: Int) {
    object Home : Screen("home", R.string.home)
    object Scattered : Screen("scattered", R.string.scattered)
    object Play : Screen("play", R.string.play)
    object Setting : Screen("setting", R.string.setting)
}

val screens = listOf(
    Screen.Home,
    Screen.Scattered,
    Screen.Play,
    Screen.Setting
)
