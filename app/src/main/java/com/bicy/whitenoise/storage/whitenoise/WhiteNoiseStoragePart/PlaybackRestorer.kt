package com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart

import android.content.Context
import android.content.Intent
import android.util.Log
import com.bicy.whitenoise.audio.CreativeEffectManager
import com.bicy.whitenoise.audio.OboeAudioEngine
import com.bicy.whitenoise.audio.PlaybackStateManager
import com.bicy.whitenoise.audio.ReverbManager
import com.bicy.whitenoise.audio.SpatialAudioManager
import com.bicy.whitenoise.servies.MusicService
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

object PlaybackRestorer {
    
    private const val TAG = "PlaybackRestorer"
    
    private var contextRef: WeakReference<Context>? = null
    
    fun init(context: Context) {
        contextRef = WeakReference(context.applicationContext)
    }
    
    fun restorePlaybackState() {
        val state = WhiteNoiseStorage.getPlaybackState()
        if (state.sounds.isEmpty()) return
        
        val ctx = contextRef?.get() ?: return
        
        val serviceIntent = Intent(ctx, MusicService::class.java)
        ctx.startForegroundService(serviceIntent)
        
        val autoPlayEnabled = com.bicy.whitenoise.subPage.setting.ItemList.isAutoPlayEnabled()
        
        CoroutineScope(Dispatchers.Main).launch {
            var retryCount = 0
            while (MusicService.getInstance() == null && retryCount < 50) {
                kotlinx.coroutines.delay(100)
                retryCount++
            }
            
            MusicService.getInstance()?.let { service ->
                state.sounds.forEach { sound ->
                    val cachedFile = getCachedFile(ctx, sound.id)
                    
                    if (sound.trackType == "scattered") {
                        PlaybackStateManager.playSound(sound.id, "", sound)
                        
                        service.registerScatteredTrack(
                            trackId = sound.id,
                            audioClips = sound.audioClips,
                            minIntervalMs = sound.minIntervalMs,
                            maxIntervalMs = sound.maxIntervalMs,
                            volume = sound.volume,
                            spatialRange = sound.spatialScatterRange,
                            spatialEnabled = sound.spatialScatterEnabled,
                            overlayMode = sound.overlayMode
                        )
                        if (autoPlayEnabled && !state.isPaused) {
                            service.startScatteredTrack(sound.id)
                        }
                    } else if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                        PlaybackStateManager.playSound(sound.id, cachedFile.absolutePath, sound)
                        
                        if (autoPlayEnabled) {
                            service.playSound(sound.id, cachedFile, sound.name)
                        } else {
                            service.preloadSound(sound.id, cachedFile)
                        }
                        service.setVolume(sound.id, sound.volume)
                        
                        // 恢复混响配置
                        service.setEffectEnabled(sound.id, true)
                        service.setReverbParams(sound.id, sound.reverbConfig.roomSize, sound.reverbConfig.damping, sound.reverbConfig.wetLevel)
                        OboeAudioEngine.setInsulation(sound.id, sound.reverbConfig.insulation)
                        OboeAudioEngine.setReverbDecayTime(sound.id, sound.reverbConfig.decayTime)
                        OboeAudioEngine.setReverbPreDelay(sound.id, sound.reverbConfig.preDelay)
                        OboeAudioEngine.setReverbDryLevel(sound.id, sound.reverbConfig.dryLevel)
                        OboeAudioEngine.setReflectionDensity(sound.id, sound.reverbConfig.reflectionDensity)
                        OboeAudioEngine.setReflectionSpread(sound.id, sound.reverbConfig.reflectionSpread)
                        OboeAudioEngine.setHighpassCutoff(sound.id, sound.reverbConfig.highpassCutoff)
                        OboeAudioEngine.setEarlyReflectionLevel(sound.id, sound.reverbConfig.earlyReflectionLevel)
                        
                        Log.d(TAG, "Restored reverb config for ${sound.id}: roomSize=${sound.reverbConfig.roomSize}")
                        
                        // 恢复音质效果配置
                        CreativeEffectManager.applyCreativeEffectConfig(sound.id)
                        Log.d(TAG, "Restored creative effect config for ${sound.id}: loFi=${sound.creativeEffectConfig.loFi}, stereoWidener=${sound.creativeEffectConfig.stereoWidener}")
                        
                        // 恢复空间音频配置
                        if (sound.spatialAudioConfig.enabled) {
                            OboeAudioEngine.setSpatialEnabled(sound.id, true)
                            OboeAudioEngine.setSpatialOffsetType(sound.id, sound.spatialAudioConfig.offsetType)
                            OboeAudioEngine.setSpatialFixedOffset(sound.id, 
                                sound.spatialAudioConfig.fixedLeftRight,
                                sound.spatialAudioConfig.fixedUpDown,
                                sound.spatialAudioConfig.fixedFrontBack,
                                sound.spatialAudioConfig.fixedMultiplier
                            )
                            OboeAudioEngine.setSpatialSurroundParams(sound.id,
                                sound.spatialAudioConfig.surroundMode,
                                sound.spatialAudioConfig.surroundRadius,
                                sound.spatialAudioConfig.surroundSpeed
                            )
                            OboeAudioEngine.setSpatialRandomParams(sound.id,
                                sound.spatialAudioConfig.randomMaxDistance,
                                sound.spatialAudioConfig.randomMinDistance,
                                sound.spatialAudioConfig.randomValue,
                                sound.spatialAudioConfig.randomSpeed
                            )
                            Log.d(TAG, "Restored spatial config for ${sound.id}")
                        }
                    } else {
                        Log.w(TAG, "缓存文件不存在或无效: ${sound.id}")
                    }
                }
                
                if (autoPlayEnabled) {
                    service.resumeAllSounds()
                }
            }
        }
    }
    
    private fun getCachedFile(context: Context, soundId: String): File? {
        return com.bicy.whitenoise.utils.DownloadManager.getCachedFile(context, soundId)
    }
}
