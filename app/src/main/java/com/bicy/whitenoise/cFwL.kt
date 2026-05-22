package com.bicy.whitenoise

import android.app.Application
import android.content.Context
import com.bicy.whitenoise.y10p.AppInitializer

class WhiteNoiseApp : Application() {
    
    companion object {
        lateinit var context: Context
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        AppInitializer.init(this)
    }
}
