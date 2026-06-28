package com.bicy.whitenoise.utils

import android.app.Application
import android.content.Context
import com.bicy.whitenoise.audio.ReverbManager
import com.bicy.whitenoise.audio.aVzM
import com.bicy.whitenoise.music.MusicLibrary
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.music.MusicScanner
import com.bicy.whitenoise.storage.AppStorage
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.storage.playlist.PlaylistManager
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage
import com.bicy.whitenoise.subPage.setting.ItemList
import com.bicy.whitenoise.ui.theme.ThemeColorManager

object AppInitializer {
    
    private lateinit var applicationContext: Context
    
    fun init(application: Application) {
        applicationContext = application.applicationContext
        
        AppStorage.init(applicationContext)
        ConfigStorage.init()
        WhiteNoiseStorage.init()
        com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.PlaybackRestorer.init(applicationContext)
        ThemeColorManager.init(applicationContext)
        
        DownloadManager.init(applicationContext)
        ReverbManager.init(applicationContext)
        ItemList.init(applicationContext)
        
        MusicScanner.init(applicationContext)
        MusicLibrary.init(applicationContext)
        MusicPlayerController.init(applicationContext)
        PlaylistManager.init(applicationContext)
        
        aVzM.start()
    }
    
    fun getContext(): Context = applicationContext
}
