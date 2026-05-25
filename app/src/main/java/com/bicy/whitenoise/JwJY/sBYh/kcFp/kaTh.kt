package com.bicy.whitenoise.JwJY.sBYh.kcFp

import android.content.Context
import android.content.Intent
import android.util.Log
import com.bicy.whitenoise.H3HO.CreativeEffectManager
import com.bicy.whitenoise.H3HO.OboeAudioEngine
import com.bicy.whitenoise.H3HO.PlaybackStateManager
import com.bicy.whitenoise.H3HO.ReverbManager
import com.bicy.whitenoise.H3HO.SpatialAudioManager
import com.bicy.whitenoise.rgRE.MusicService
import com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage
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
        
        val autoPlayEnabled = com.bicy.whitenoise.DzBD.u4oy.ItemList.isAutoPlayEnabled()
        
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
                        
                        service.setEffectEnabled(sound.id, true)
                        service.setReverbParams(sound.id, sound.reverbConfig.roomSize, sound.reverbConfig.damping, sound.reverbConfig.wetLevel)
                        OboeAudioEngine.setInsulation(sound.id, sound.reverbConfig.insulation)
                        OboeAudioEngine.setReverbDecayTime(sound.id, sound.reverbConfig.decayTime)
                        OboeAudioEngine.setReverbPreDelay(sound.id, sound.reverbConfig.preDelay)
                        OboeAudioEngine.setReverbDryLevel(sound.id, sound.reverbConfig.dryLevel)
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
        return com.bicy.whitenoise.y10p.DownloadManager.getCachedFile(context, soundId)
    }
}
