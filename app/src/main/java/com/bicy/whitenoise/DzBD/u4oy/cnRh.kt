package com.bicy.whitenoise.DzBD.u4oy

import android.content.Context
import android.content.SharedPreferences

object ItemList {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_AUTO_PLAY = "auto_play"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setAutoPlay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY, enabled).apply()
    }

    fun isAutoPlayEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_PLAY, true)
    }
}
