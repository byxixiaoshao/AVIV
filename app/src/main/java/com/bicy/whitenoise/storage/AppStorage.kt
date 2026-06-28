package com.bicy.whitenoise.storage

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.storage.core.StorageManager
import com.bicy.whitenoise.storage.music.MusicStorage
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.PlaybackRestorer

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
        
        PlaybackRestorer.init(context)
        
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
