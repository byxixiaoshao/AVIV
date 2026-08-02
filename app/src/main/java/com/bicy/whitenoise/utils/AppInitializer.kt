package com.bicy.whitenoise.utils

import android.app.Application
import android.content.Context
import android.util.Log
import com.bicy.whitenoise.audio.ReverbManager
import com.bicy.whitenoise.equalizer.PresetStorage
import com.bicy.whitenoise.audio.aVzM
import com.bicy.whitenoise.floatingpet.FloatingPetInitializer
import com.bicy.whitenoise.music.MusicLibraryPart.MusicLibrary
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.music.MusicScannerPart.MusicScanner
import com.bicy.whitenoise.storage.AppStorage
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.storage.config.LiquidGlassConfig
import com.bicy.whitenoise.storage.config.NavBackgroundConfig
import com.bicy.whitenoise.storage.core.JsonStorageManager
import com.bicy.whitenoise.storage.playlist.PlaylistManagerPart.PlaylistManager
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage
import com.bicy.whitenoise.subPage.setting.ItemList
import com.bicy.whitenoise.ui.theme.ThemeColorManager
import java.io.File

object AppInitializer {
    
    private const val TAG = "AppInitializer"
    private const val SCRIPT_EXPORTED_KEY = "builtin_scripts_exported"
    private lateinit var applicationContext: Context
    
    fun init(application: Application) {
        applicationContext = application.applicationContext
        
        AppStorage.init(applicationContext)
        JsonStorageManager.init(applicationContext)
        ConfigStorage.init()
        LiquidGlassConfig.initialize(applicationContext)
        NavBackgroundConfig.initialize(applicationContext)
        WhiteNoiseStorage.init()
        com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.PlaybackRestorer.init(applicationContext)
        ThemeColorManager.init(applicationContext)
        
        DownloadManager.init(applicationContext)
        ReverbManager.init(applicationContext)
        ItemList.init(applicationContext)
        
        exportBuiltinScripts()
        
        MusicScanner.init(applicationContext)
        MusicLibrary.init(applicationContext)
        MusicPlayerController.init(applicationContext)
        PlaylistManager.init(applicationContext)
        
        UsageStatsManager.init(applicationContext)
        PresetStorage.init(applicationContext)
        
        FloatingPetInitializer.initialize(applicationContext)
        
        aVzM.start()
    }
    
    /** 首次启动时从 assets/Scripts 导出内置音源脚本 */
    private fun exportBuiltinScripts() {
        val prefs = applicationContext.getSharedPreferences("app_init", Context.MODE_PRIVATE)
        if (prefs.getBoolean(SCRIPT_EXPORTED_KEY, false)) return

        try {
            val scriptDir = File(applicationContext.filesDir, "source_scripts")
            val assetsScripts = applicationContext.assets.list("Scripts") ?: run {
                Log.i(TAG, "assets/Scripts 目录为空或不存在，跳过脚本导出")
                prefs.edit().putBoolean(SCRIPT_EXPORTED_KEY, true).apply()
                return
            }

            if (assetsScripts.isEmpty()) {
                Log.i(TAG, "assets/Scripts 目录为空，跳过脚本导出")
                prefs.edit().putBoolean(SCRIPT_EXPORTED_KEY, true).apply()
                return
            }

            if (!scriptDir.exists()) scriptDir.mkdirs()

            var count = 0
            for (fileName in assetsScripts) {
                if (!fileName.endsWith(".js")) continue
                try {
                    val destFile = File(scriptDir, fileName)
                    applicationContext.assets.open("Scripts/$fileName").use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    count++
                    Log.d(TAG, "导出脚本: $fileName")
                } catch (e: Exception) {
                    Log.e(TAG, "导出脚本失败: $fileName", e)
                }
            }

            Log.i(TAG, "内置脚本导出完成: $count 个")
        } catch (_: Exception) {
            // assets/Scripts 目录不存在也不崩溃
        }

        prefs.edit().putBoolean(SCRIPT_EXPORTED_KEY, true).apply()
    }
    
    fun getContext(): Context = applicationContext
}
