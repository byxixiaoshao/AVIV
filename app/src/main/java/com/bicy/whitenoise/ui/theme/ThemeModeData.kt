package com.bicy.whitenoise.ui.theme

import androidx.compose.ui.graphics.Color
import java.util.UUID
import com.bicy.whitenoise.ui.theme.ThemeColorsPart.*

/**
 * 主题模式枚举
 */
enum class ThemeMode(val value: String, val displayName: String) {
    OFF("off", "关闭"),           // 单一主题色设置
    FOLLOW_SYSTEM("follow_system", "跟随系统"), // 日夜间主题跟随系统
    SCHEDULED("scheduled", "定时");  // 定时主题切换

    companion object {
        fun fromValue(value: String): ThemeMode {
            return values().find { it.value == value } ?: OFF
        }
    }
}

/**
 * 自定义主题数据类
 */
data class CustomTheme(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val accent: Int,
    val primary: Int,
    val background: Int,
    val text: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 转换为 ThemeColorScheme
     */
    fun toThemeColorScheme(): ThemeColorScheme {
        return ThemeColorScheme(
            id = id,
            name = name,
            accent = Color(accent),
            primary = Color(primary),
            background = Color(background),
            text = Color(text)
        )
    }

    /**
     * 判断是否为日间主题
     */
    val isLight: Boolean
        get() {
            val bgColor = Color(background)
            return bgColor.red + bgColor.green + bgColor.blue > 1.5f
        }

    companion object {
        /**
         * 从 ThemeColorScheme 创建自定义主题
         */
        fun fromThemeColorScheme(scheme: ThemeColorScheme, customName: String? = null): CustomTheme {
            return CustomTheme(
                id = scheme.id,
                name = customName ?: scheme.name,
                accent = scheme.accent.value.toInt(),  // Color.value 是 ULong，转换为 Int
                primary = scheme.primary.value.toInt(),
                background = scheme.background.value.toInt(),
                text = scheme.text.value.toInt()
            )
        }
    }
}

/**
 * 定时主题任务数据类
 */
data class ThemeScheduleTask(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Int,  // 分钟数 (例如: 8:00 = 480)
    val endTime: Int,    // 分钟数 (例如: 18:00 = 1080)
    val themeId: String, // 主题 ID
    val name: String = "" // 任务名称
) {
    /**
     * 获取起始时间字符串 (HH:mm)
     */
    fun getStartTimeString(): String {
        val hours = startTime / 60
        val minutes = startTime % 60
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }

    /**
     * 获取结束时间字符串 (HH:mm)
     */
    fun getEndTimeString(): String {
        val hours = endTime / 60
        val minutes = endTime % 60
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }

    /**
     * 检查是否跨天 (结束时间小于起始时间)
     */
    fun isCrossDay(): Boolean = endTime < startTime

    /**
     * 检查给定时间是否在此任务的时间范围内
     * @param currentTime 当前时间的分钟数
     */
    fun isTimeInRange(currentTime: Int): Boolean {
        if (isCrossDay()) {
            // 跨天情况: startTime 到 24:00, 或 0:00 到 endTime
            return currentTime >= startTime || currentTime <= endTime
        } else {
            // 正常情况: startTime 到 endTime
            return currentTime >= startTime && currentTime <= endTime
        }
    }

    /**
     * 检查是否与另一个任务时间冲突
     */
    fun hasConflict(other: ThemeScheduleTask): Boolean {
        // 两个任务都跨天的情况
        if (isCrossDay() && other.isCrossDay()) {
            return true // 两个跨天任务必定冲突
        }

        // 一个跨天,一个不跨天
        if (isCrossDay() && !other.isCrossDay()) {
            // 当前任务跨天,检查是否覆盖了另一个任务的时间段
            return other.startTime >= startTime || other.endTime <= endTime ||
                   other.startTime <= endTime || other.endTime >= startTime
        }

        if (!isCrossDay() && other.isCrossDay()) {
            // 另一个任务跨天,检查是否覆盖了当前任务的时间段
            return startTime >= other.startTime || endTime <= other.endTime ||
                   startTime <= other.endTime || endTime >= other.startTime
        }

        // 两个都不跨天,检查时间段重叠
        return startTime <= other.endTime && endTime >= other.startTime
    }

    companion object {
        /**
         * 从时间字符串创建任务
         * @param startTimeString 格式: "HH:mm"
         * @param endTimeString 格式: "HH:mm"
         */
        fun fromTimeString(
            startTimeString: String,
            endTimeString: String,
            themeId: String,
            name: String = ""
        ): ThemeScheduleTask {
            val startParts = startTimeString.split(":")
            val endParts = endTimeString.split(":")
            
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
            
            return ThemeScheduleTask(
                startTime = startMinutes,
                endTime = endMinutes,
                themeId = themeId,
                name = name
            )
        }
    }
}
