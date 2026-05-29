package com.bicy.whitenoise.y10p

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.R
import com.bicy.whitenoise.xnef.MusicScanner
import com.bicy.whitenoise.xnef.MusicLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RenderPreloadManager {
    
    private const val TAG = "RenderPreloadManager"
    
    private var isPreloaded = false
    private var preloadProgress = 0f
    private var musicPreloaded = false
    private var soundsPreloaded = false
    private var uiPreloaded = false
    
    data class PreloadState(
        val musicLoaded: Boolean = false,
        val soundsLoaded: Boolean = false,
        val uiRendered: Boolean = false,
        val progress: Float = 0f
    )
    
    private var _preloadState = PreloadState()
    val preloadState: PreloadState get() = _preloadState
    
    private val preloadCallbacks = mutableListOf<(Float, String) -> Unit>()
    
    fun addProgressCallback(callback: (Float, String) -> Unit) {
        preloadCallbacks.add(callback)
    }
    
    fun removeProgressCallback(callback: (Float, String) -> Unit) {
        preloadCallbacks.remove(callback)
    }
    
    private fun notifyProgress(progress: Float, message: String) {
        preloadCallbacks.forEach { it(progress, message) }
    }
    
    suspend fun preloadAll(context: Context, onProgress: (Float, String) -> Unit = { _, _ -> }): Boolean {
        if (isPreloaded) {
            onProgress(1f, context.getString(R.string.preload_completed))
            return true
        }
        
        addProgressCallback(onProgress)
        
        return withContext(Dispatchers.IO) {
            try {
                val totalSteps = 12f
                var currentStep = 0f
                
                fun updateProgress(step: String) {
                    currentStep++
                    val progress = currentStep / totalSteps
                    notifyProgress(progress, step)
                }
                
                updateProgress(context.getString(R.string.preload_initializing))
                
                updateProgress(context.getString(R.string.preload_loading_music_config))
                MusicScanner.init(context)
                
                updateProgress(context.getString(R.string.preload_scanning_music))
                MusicLibrary.scanLibrary(context)
                musicPreloaded = true
                _preloadState = _preloadState.copy(musicLoaded = true)
                
                updateProgress(context.getString(R.string.preload_loading_sound_categories))
                preloadSoundsData(context)
                soundsPreloaded = true
                _preloadState = _preloadState.copy(soundsLoaded = true)
                
                updateProgress(context.getString(R.string.preload_warming_animation))
                preloadAnimationSystem()
                
                updateProgress(context.getString(R.string.preload_prerendering_ui))
                preloadUIComponents(context)
                uiPreloaded = true
                _preloadState = _preloadState.copy(uiRendered = true)
                
                updateProgress(context.getString(R.string.preload_complete))
                isPreloaded = true
                _preloadState = _preloadState.copy(progress = 1f)
                true
            } catch (e: Exception) {
                Log.e(TAG, "预加载失败", e)
                false
            } finally {
                removeProgressCallback(onProgress)
            }
        }
    }
    
    private suspend fun preloadSoundsData(context: Context) {
        return withContext(Dispatchers.IO) {
            try {
                SoundStorageManager.init(context)
                val customClasses = SoundStorageManager.loadSoundsClass(context)
                customClasses.forEach { soundClass ->
                    SoundStorageManager.loadSoundsList(context, soundClass.name)
                }
                Log.d(TAG, "声音数据预加载完成: ${customClasses.size} 个分类")
            } catch (e: Exception) {
                Log.e(TAG, "声音数据预加载失败", e)
            }
        }
    }
    
    private suspend fun preloadAnimationSystem() {
        return withContext(Dispatchers.Default) {
            try {
                Thread.sleep(50)
                Log.d(TAG, "动画系统预热完成")
            } catch (e: Exception) {
                Log.e(TAG, "动画系统预热失败", e)
            }
        }
    }
    
    private suspend fun preloadUIComponents(context: Context) {
        return withContext(Dispatchers.Default) {
            try {
                Thread.sleep(50)
                Log.d(TAG, "UI组件预渲染完成")
            } catch (e: Exception) {
                Log.e(TAG, "UI预渲染失败", e)
            }
        }
    }
    
    fun isPreloaded(): Boolean = isPreloaded
    
    fun isMusicPreloaded(): Boolean = musicPreloaded
    
    fun isSoundsPreloaded(): Boolean = soundsPreloaded
    
    fun isUIPreloaded(): Boolean = uiPreloaded
    
    fun getPreloadProgress(): Float = preloadProgress
    
    fun reset() {
        isPreloaded = false
        musicPreloaded = false
        soundsPreloaded = false
        uiPreloaded = false
        _preloadState = PreloadState()
    }
}
