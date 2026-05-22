package com.bicy.whitenoise.H3HO

import android.util.Log
import com.bicy.whitenoise.JwJY.sBYh.kcFp.SpatialAudioConfig

object SpatialAudioManager {
    
    private const val TAG = "SpatialAudioManager"
    
    private val soundSpatialConfigs = mutableMapOf<String, SpatialAudioConfig>()
    
    fun getConfig(soundId: String): SpatialAudioConfig? {
        return soundSpatialConfigs[soundId]
    }
    
    fun setConfig(soundId: String, config: SpatialAudioConfig) {
        soundSpatialConfigs[soundId] = config
        Log.d(TAG, "Set spatial config for sound: $soundId, enabled=${config.enabled}")
    }
    
    fun removeConfig(soundId: String) {
        soundSpatialConfigs.remove(soundId)
        Log.d(TAG, "Removed spatial config for sound: $soundId")
    }
    
    fun clearConfig(soundId: String? = null) {
        if (soundId != null) {
            soundSpatialConfigs.remove(soundId)
            Log.d(TAG, "Cleared spatial config for sound: $soundId")
        } else {
            soundSpatialConfigs.clear()
            Log.d(TAG, "Cleared all spatial configs")
        }
    }
    
    fun applySpatialConfig(soundId: String) {
        val config = getConfig(soundId) ?: return
        
        OboeAudioEngine.setSpatialEnabled(soundId, config.enabled)
        OboeAudioEngine.setSpatialOffsetType(soundId, config.offsetType)
        OboeAudioEngine.setSpatialFixedOffset(
            soundId,
            config.fixedLeftRight,
            config.fixedUpDown,
            config.fixedFrontBack,
            config.fixedMultiplier
        )
        OboeAudioEngine.setSpatialSurroundParams(
            soundId,
            config.surroundMode,
            config.surroundRadius,
            config.surroundSpeed
        )
        OboeAudioEngine.setSpatialRandomParams(
            soundId,
            config.randomMaxDistance,
            config.randomMinDistance,
            config.randomValue,
            config.randomSpeed
        )
        
        Log.d(TAG, "Applied spatial config for sound: $soundId")
    }
}
