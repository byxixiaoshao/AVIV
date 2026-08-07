package com.bicy.whitenoise.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import com.bicy.whitenoise.ui.theme.ThemeColorsPart.ThemeColorPresets
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * 闪屏主题色同步解析器
 *
 * 解决时序冲突：SplashActivity.onCreate 中 Lottie 同步加载播放，
 * 但 ThemeColorManager 在 initializeComponents 中异步初始化。
 * 本工具直接从持久化文件同步读取主题色配置，不依赖 ThemeColorManager。
 *
 * - 配置文件：filesDir/json_storage/app_config.json
 * - 自定义主题：filesDir/custom_themes.json
 * - 预设主题：ThemeColorPresets（内存中，同步快速）
 */
object SplashThemeResolver {

    private const val TAG = "SplashThemeResolver"
    private const val CONFIG_FILE = "json_storage/app_config.json"
    private const val CUSTOM_THEMES_FILE = "custom_themes.json"

    /** 主题色对（ARGB Int） */
    data class ThemeColors(val primary: Int, val background: Int)

    /**
     * 同步读取当前主题色（primary + background）
     * 失败时回退到默认主题
     */
    fun resolveSync(context: Context): ThemeColors {
        return try {
            val themeId = resolveThemeId(context)
            resolveColorsById(context, themeId)
        } catch (e: Exception) {
            Log.w(TAG, "resolveSync failed, using default", e)
            default()
        }
    }

    private fun default() = ThemeColors(
        primary = ThemeColorPresets.Default.primary.toArgb(),
        background = ThemeColorPresets.Default.background.toArgb()
    )

    /** 根据主题模式解析当前 themeId */
    private fun resolveThemeId(context: Context): String {
        val configFile = File(context.filesDir, CONFIG_FILE)
        if (!configFile.exists()) return ThemeColorPresets.Default.id

        val json = JSONObject(configFile.readText())
        return when (json.optString("themeMode", "OFF")) {
            "FOLLOW_SYSTEM" -> {
                val isDark = (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                if (isDark) json.optString("nightThemeId", ThemeColorPresets.Default.id)
                else json.optString("dayThemeId", ThemeColorPresets.Default.id)
            }
            "SCHEDULED" -> json.optString("scheduledDefaultThemeId", ThemeColorPresets.Default.id)
            else -> json.optString("themeColorId", ThemeColorPresets.Default.id)
        }
    }

    /** 根据 themeId 解析颜色：先查预设，再查自定义主题库 */
    private fun resolveColorsById(context: Context, themeId: String): ThemeColors {
        // 预设主题（内存同步）
        ThemeColorPresets.getPresetById(themeId)
            .takeIf { it.id == themeId }
            ?.let { return ThemeColors(it.primary.toArgb(), it.background.toArgb()) }

        // 自定义主题（读文件）
        File(context.filesDir, CUSTOM_THEMES_FILE)
            .takeIf { it.exists() }
            ?.let { findCustomTheme(it, themeId) }
            ?.let { return it }

        return default()
    }

    private fun findCustomTheme(file: File, themeId: String): ThemeColors? = try {
        JSONArray(file.readText()).let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it) }
                .firstOrNull { it.getString("id") == themeId }
                ?.let { ThemeColors(it.getInt("primary"), it.getInt("background")) }
        }
    } catch (e: Exception) {
        Log.w(TAG, "findCustomTheme failed", e); null
    }

    // ==================== HSL 明度保留算法 ====================

    /**
     * 保留原始灰阶的明度(L)，替换为主题色的色相(H)和饱和度(S)
     * 实现亮度梯度映射：深灰→深色主题色，浅灰→浅色主题色
     */
    fun applyHslLuminance(originalColor: Int, themeColor: Int): Int {
        val origL = luminance(
            Color.red(originalColor), Color.green(originalColor), Color.blue(originalColor)
        )
        val themeHsl = FloatArray(3)
        rgbToHsl(
            Color.red(themeColor) / 255f, Color.green(themeColor) / 255f, Color.blue(themeColor) / 255f,
            themeHsl
        )
        return hslToRgb(themeHsl[0], themeHsl[1], origL)
    }

    /** 判断原始颜色是否为背景色（高亮度，如奶油白）→ 映射到 background */
    fun isBackgroundLight(color: Int): Boolean =
        luminance(Color.red(color), Color.green(color), Color.blue(color)) > 0.85f

    private fun luminance(r: Int, g: Int, b: Int): Float {
        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
        return (max(rf, max(gf, bf)) + min(rf, min(gf, bf))) / 2f
    }

    private fun rgbToHsl(r: Float, g: Float, b: Float, out: FloatArray) {
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b))
        val l = (mx + mn) / 2f
        var h = 0f; var s = 0f
        if (mx != mn) {
            val d = mx - mn
            s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
            h = when (mx) {
                r -> (g - b) / d + if (g < b) 6f else 0f
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            } / 6f
        }
        out[0] = h; out[1] = s; out[2] = l
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): Int {
        if (s == 0f) return Color.rgb(l, l, l)
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        return Color.rgb(
            hueToRgb(p, q, h + 1f / 3f),
            hueToRgb(p, q, h),
            hueToRgb(p, q, h - 1f / 3f)
        )
    }

    private fun hueToRgb(p: Float, q: Float, t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        if (tt < 1f / 6f) return p + (q - p) * 6f * tt
        if (tt < 1f / 2f) return q
        if (tt < 2f / 3f) return p + (q - p) * (2f / 3f - tt) * 6f
        return p
    }
}
