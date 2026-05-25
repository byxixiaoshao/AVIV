package com.bicy.whitenoise.y10p

import android.app.Application
import android.content.Context
import com.bicy.whitenoise.H3HO.ReverbManager
import com.bicy.whitenoise.H3HO.aVzM
import com.bicy.whitenoise.xnef.MusicLibrary
import com.bicy.whitenoise.xnef.MusicPlayerController
import com.bicy.whitenoise.xnef.MusicScanner
import com.bicy.whitenoise.JwJY.AppStorage
import com.bicy.whitenoise.JwJY.NATg.ConfigStorage
import com.bicy.whitenoise.JwJY.GqOr.PlaylistManager
import com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage
import com.bicy.whitenoise.DzBD.u4oy.ItemList
import com.bicy.whitenoise.yODW.ZFNn.ThemeColorManager

object AppInitializer {
    
    private lateinit var applicationContext: Context
    
    fun init(application: Application) {
        applicationContext = application.applicationContext
        
        AppStorage.init(applicationContext)
        ConfigStorage.init()
        WhiteNoiseStorage.init()
        com.bicy.whitenoise.JwJY.sBYh.kcFp.PlaybackRestorer.init(applicationContext)
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
