package com.bicy.whitenoise.H3HO

import android.util.Log
import com.bicy.whitenoise.JwJY.sBYh.kcFp.CreativeEffectConfig

object CreativeEffectManager {
    
    private const val TAG = "CreativeEffectManager"
    
    private val soundCreativeConfigs = mutableMapOf<String, CreativeEffectConfig>()
    
    fun getConfig(soundId: String): CreativeEffectConfig? {
        return soundCreativeConfigs[soundId]
    }
    
    fun setConfig(soundId: String, config: CreativeEffectConfig) {
        soundCreativeConfigs[soundId] = config
        Log.d(TAG, "Set creative effect config for sound: $soundId, loFi=${config.loFi}, eightBit=${config.eightBit}, underwater=${config.underwater}, alienSignal=${config.alienSignal}, megaphone=${config.megaphone}")
    }
    
    fun removeConfig(soundId: String) {
        soundCreativeConfigs.remove(soundId)
        Log.d(TAG, "Removed creative effect config for sound: $soundId")
    }
    
    fun clearConfig(soundId: String? = null) {
        if (soundId != null) {
            soundCreativeConfigs.remove(soundId)
            Log.d(TAG, "Cleared creative effect config for sound: $soundId")
        } else {
            soundCreativeConfigs.clear()
            Log.d(TAG, "Cleared all creative effect configs")
        }
    }
    
    fun applyCreativeEffectConfig(soundId: String) {
        val config = getConfig(soundId)
        if (config == null) {
            Log.w(TAG, "applyCreativeEffectConfig: No config found for sound: $soundId")
            return
        }
        
        Log.d(TAG, "Applying creative effect config for sound: $soundId, loFi=${config.loFi}, eightBit=${config.eightBit}, underwater=${config.underwater}, alienSignal=${config.alienSignal}, megaphone=${config.megaphone}")
        
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.LoFi, config.loFi)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.EightBit, config.eightBit)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.Underwater, config.underwater)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.AlienSignal, config.alienSignal)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.Megaphone, config.megaphone)
        
        Log.d(TAG, "Applied creative effect config for sound: $soundId")
    }
}
