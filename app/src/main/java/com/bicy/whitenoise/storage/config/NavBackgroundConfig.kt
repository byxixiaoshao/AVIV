package com.bicy.whitenoise.storage.config

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

/**
 * 导航栏背景配置管理类
 * 支持背景图片和背景色透明度设置
 * 背景图片会从用户选择的源复制到内部存储，避免 URI 权限过期问题
 */
object NavBackgroundConfig {

    private const val TAG = "NavBackgroundConfig"
    private const val PREFS_NAME = "nav_background_config"
    private const val KEY_BACKGROUND_URI = "background_uri"
    private const val KEY_BACKGROUND_ALPHA = "background_alpha"
    private const val WALLPAPER_FILENAME = "wallpaper.jpg"

    // 默认值
    private const val DEFAULT_ALPHA = 0.85f

    private val _backgroundUriFlow = MutableStateFlow<String?>(null)
    val backgroundUriFlow: StateFlow<String?> = _backgroundUriFlow.asStateFlow()

    private val _backgroundAlphaFlow = MutableStateFlow(DEFAULT_ALPHA)
    val backgroundAlphaFlow: StateFlow<Float> = _backgroundAlphaFlow.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getWallpaperDir(context: Context): File {
        val dir = File(context.filesDir, "backgrounds")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 初始化状态，验证存储的图片是否存在
     */
    fun initialize(context: Context) {
        val uri = getPrefs(context).getString(KEY_BACKGROUND_URI, null)
        // 如果路径为空，或者文件不存在（content:// URI 或已被删除），清除记录
        if (uri != null && !File(uri).exists()) {
            getPrefs(context).edit().remove(KEY_BACKGROUND_URI).apply()
            _backgroundUriFlow.value = null
        } else {
            _backgroundUriFlow.value = uri
        }
        val alpha = getPrefs(context).getFloat(KEY_BACKGROUND_ALPHA, DEFAULT_ALPHA)
        _backgroundAlphaFlow.value = alpha
    }

    /**
     * 获取背景图片 URI
     */
    fun getBackgroundUri(context: Context): String? {
        return getPrefs(context).getString(KEY_BACKGROUND_URI, null)
    }

    /**
     * 设置背景图片：从用户选择的 URI 复制到内部存储
     */
    fun setBackgroundImage(context: Context, sourceUri: Uri) {
        try {
            val wallpaperDir = getWallpaperDir(context)
            // 用唯一文件名避免缓存问题
            val destFile = File(wallpaperDir, WALLPAPER_FILENAME)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            val path = destFile.absolutePath
            getPrefs(context).edit().putString(KEY_BACKGROUND_URI, path).apply()
            _backgroundUriFlow.value = path
            Log.d(TAG, "Background image saved to $path")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save background image", e)
        }
    }

    /**
     * 设置背景图片 URI（兼容旧 API，仅供内部清理使用）
     */
    private fun setBackgroundUri(uri: String?) {
        val context = com.bicy.whitenoise.utils.AppInitializer.getContext()
        getPrefs(context).edit().putString(KEY_BACKGROUND_URI, uri).apply()
        _backgroundUriFlow.value = uri
    }

    /**
     * 清除背景图片（同时删除内部存储文件）
     */
    fun clearBackgroundUri() {
        val context = com.bicy.whitenoise.utils.AppInitializer.getContext()
        val path = _backgroundUriFlow.value
        if (path != null) {
            try { File(path).delete() } catch (_: Exception) {}
        }
        getPrefs(context).edit().remove(KEY_BACKGROUND_URI).apply()
        _backgroundUriFlow.value = null
    }

    /**
     * 获取背景色透明度
     */
    fun getBackgroundAlpha(context: Context): Float {
        return getPrefs(context).getFloat(KEY_BACKGROUND_ALPHA, DEFAULT_ALPHA)
    }

    /**
     * 设置背景色透明度
     */
    fun setBackgroundAlpha(alpha: Float) {
        val context = com.bicy.whitenoise.utils.AppInitializer.getContext()
        val clampedAlpha = alpha.coerceIn(0.1f, 1.0f)
        getPrefs(context).edit().putFloat(KEY_BACKGROUND_ALPHA, clampedAlpha).apply()
        _backgroundAlphaFlow.value = clampedAlpha
    }
}