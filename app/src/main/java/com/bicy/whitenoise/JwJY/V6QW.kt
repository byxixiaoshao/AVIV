package com.bicy.whitenoise.JwJY

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.JwJY.NATg.ConfigStorage
import com.bicy.whitenoise.JwJY.Jauc.StorageManager
import com.bicy.whitenoise.JwJY.EY9i.MusicStorage
import com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage

object AppStorage {
    
    private const val TAG = "AppStorage"
    
    private var isInitialized = false
    
    fun init(context: Context) {
        if (isInitialized) {
            Log.w(TAG, "AppStorage already initialized")
            return
        }
        
        Log.d(TAG, "Initializing AppStorage...")
        
        StorageManager.init(context)
        
        ConfigStorage.init()
        WhiteNoiseStorage.init()
        MusicStorage.init()
        
        isInitialized = true
        Log.d(TAG, "AppStorage initialized successfully")
    }
    
    fun isInitialized(): Boolean = isInitialized
    
    fun clearAllData() {
        ConfigStorage.clearAllData()
        WhiteNoiseStorage.clearPlayback()
        MusicStorage.clearPlaybackState()
        MusicStorage.clearDirectories()
        Log.d(TAG, "All data cleared")
    }
}
