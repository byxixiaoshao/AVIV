package com.bicy.whitenoise.audio

import android.util.Log
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.CreativeEffectConfig

object CreativeEffectManager {
    
    private const val TAG = "CreativeEffectManager"
    
    fun getConfig(soundId: String): CreativeEffectConfig? {
        val soundConfig = PlaybackStateManager.getSoundConfig(soundId)
        return soundConfig?.creativeEffectConfig
    }
    
    fun setConfig(soundId: String, config: CreativeEffectConfig) {
        PlaybackStateManager.updateCreativeConfig(soundId, config)
        Log.d(TAG, "Set creative effect config for sound: $soundId, loFi=${config.loFi}, eightBit=${config.eightBit}, underwater=${config.underwater}, alienSignal=${config.alienSignal}, megaphone=${config.megaphone}, hifi=${config.hifi}, stereoWidener=${config.stereoWidener}, virtualBass=${config.virtualBass}, multibandCompressor=${config.multibandCompressor}")
    }
    
    fun removeConfig(soundId: String) {
        Log.d(TAG, "Removed creative effect config for sound: $soundId")
    }
    
    fun clearConfig(soundId: String? = null) {
        if (soundId != null) {
            Log.d(TAG, "Cleared creative effect config for sound: $soundId")
        } else {
            Log.d(TAG, "Cleared all creative effect configs")
        }
    }
    
    fun applyCreativeEffectConfig(soundId: String) {
        val config = getConfig(soundId)
        if (config == null) {
            Log.w(TAG, "applyCreativeEffectConfig: No config found for sound: $soundId")
            return
        }
        
        Log.d(TAG, "Applying creative effect config for sound: $soundId, loFi=${config.loFi}, eightBit=${config.eightBit}, underwater=${config.underwater}, alienSignal=${config.alienSignal}, megaphone=${config.megaphone}, hifi=${config.hifi}, stereoWidener=${config.stereoWidener}, virtualBass=${config.virtualBass}, multibandCompressor=${config.multibandCompressor}")
        
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.LoFi, config.loFi)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.EightBit, config.eightBit)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.Underwater, config.underwater)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.AlienSignal, config.alienSignal)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.Megaphone, config.megaphone)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.HiFi, config.hifi)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.StereoWidener, config.stereoWidener)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.VirtualBass, config.virtualBass)
        OboeAudioEngine.setCreativeEffectIntensity(soundId, CreativeEffectType.MultibandCompressor, config.multibandCompressor)
        
        Log.d(TAG, "Applied creative effect config for sound: $soundId")
    }
}
