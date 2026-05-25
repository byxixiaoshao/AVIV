package com.bicy.whitenoise.DzBD.PeeU

import android.content.Context
import android.content.Intent
import android.util.Log
import com.bicy.whitenoise.rgRE.MusicService
import com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundMetadata
import com.bicy.whitenoise.y10p.DownloadManager

object Function {
    
    private const val TAG = "Function"
    
    private var onDownloadProgressListener: ((String, Float) -> Unit)? = null
    
    fun setDownloadProgressListener(listener: (String, Float) -> Unit) {
        onDownloadProgressListener = listener
    }
    
    fun clearDownloadProgressListener() {
        onDownloadProgressListener = null
    }
    
    fun getCachedFile(context: Context, soundId: String) = DownloadManager.getCachedFile(context, soundId)
    
    fun isCached(context: Context, soundId: String) = DownloadManager.isCached(context, soundId)
    
    fun isDownloading(soundId: String) = DownloadManager.isDownloading(soundId)
    
    fun getDownloadProgress(soundId: String) = DownloadManager.getDownloadProgress(soundId)
    
    fun downloadAudio(
        context: Context,
        sound: SoundMetadata,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        DownloadManager.downloadAudio(
            context = context,
            sound = sound,
            onProgress = { progress ->
                onProgress(progress)
                onDownloadProgressListener?.invoke(sound.id, progress)
            },
            onComplete = onComplete
        )
    }
    
    fun playSound(context: Context, sound: SoundMetadata) {
        val cachedFile = getCachedFile(context, sound.id)
        
        if (cachedFile != null) {
            val serviceIntent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_PLAY_SOUND
                putExtra(MusicService.EXTRA_SOUND_ID, sound.id)
                putExtra(MusicService.EXTRA_FILE_PATH, cachedFile.absolutePath)
                putExtra(MusicService.EXTRA_SOUND_NAME, sound.name)
            }
            context.startForegroundService(serviceIntent)
            Log.d(TAG, "播放缓存文件: ${sound.name}")
        } else {
            Log.d(TAG, "音频未缓存，需要先下载: ${sound.name}")
        }
    }
    
    fun stopSound(context: Context, soundId: String) {
        MusicService.getInstance()?.stopSound(soundId)
        Log.d(TAG, "停止播放: $soundId")
    }
    
    fun stopAllSounds() {
        MusicService.getInstance()?.stopAllSounds()
    }
    
    fun isPlaying(soundId: String): Boolean {
        return MusicService.getInstance()?.isSoundPlaying(soundId) ?: false
    }
    
    fun getPlayingSounds(): Set<String> {
        return MusicService.getInstance()?.getPlayingSounds() ?: emptySet()
    }
    
    fun deleteCache(context: Context, soundId: String): Boolean {
        return DownloadManager.deleteCache(context, soundId)
    }
    
    fun clearAllCache(context: Context) {
        DownloadManager.clearAllCache(context)
    }
    
    fun cancelDownload(soundId: String) {
        DownloadManager.cancelDownload(soundId)
    }
    
    fun cancelAllDownloads() {
        DownloadManager.cancelAllDownloads()
    }
}
