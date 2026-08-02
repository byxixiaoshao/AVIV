package com.bicy.whitenoise.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * 电池优化 / 应用后台保活跳转工具类。
 *
 * 跳转策略（按优先级依次尝试，任一成功即返回 true）：
 * 1. 原生应用详情页（ACTION_APPLICATION_DETAILS_SETTINGS）—— 让用户在该页找到电池/省电选项
 * 2. 电池优化白名单列表页（ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS）
 * 3. 直接弹窗请求加入白名单（ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS）
 * 4. 厂商自启动管理页（小米 / 华为 / 荣耀 / OPPO / Vivo / 三星）
 * 5. 通用兜底：再次回到应用详情页
 *
 * 部分国产 ROM 没有原生电池优化页面，会抛出 ActivityNotFoundException，
 * 此时自动回退到厂商自启动管理页面。
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptHelper"

    /**
     * 尝试跳转到省电策略 / 后台保活相关页面。
     *
     * @param context 任意 Context（内部会处理 Activity 跳转）
     * @return true 表示成功打开某个设置页面；false 表示所有尝试均失败
     */
    fun openKeepAliveSettings(context: Context): Boolean {
        val packageName = context.packageName

        // 1. 首选：原生应用详情页
        if (tryStartActivity(context, buildAppDetailsIntent(packageName))) {
            return true
        }

        // 2. 备选：电池优化白名单列表页
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            tryStartActivity(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        ) {
            return true
        }

        // 3. 再备选：直接弹窗请求加入白名单（需要 REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 权限）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            tryStartActivity(context, Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        ) {
            return true
        }

        // 4. 厂商自启动管理页面回退
        if (openVendorAutoStartSettings(context)) {
            return true
        }

        // 5. 通用兜底：再次尝试应用详情页（最基础，几乎一定存在）
        if (tryStartActivity(context, buildAppDetailsIntent(packageName))) {
            return true
        }

        Log.w(TAG, "All keep-alive settings intents failed to launch")
        return false
    }

    /**
     * 厂商自启动管理页面跳转。
     * 覆盖主流国产 ROM：小米、华为、荣耀、OPPO、Vivo、三星。
     */
    private fun openVendorAutoStartSettings(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase().orEmpty()

        val vendorIntents = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.miui.permcenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                )
            )

            manufacturer.contains("huawei") -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                )
            )

            manufacturer.contains("honor") || manufacturer.contains("hihonor") -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.hihonor.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                ),
                Intent().setComponent(
                    ComponentName(
                        "com.hihonor.systemmanager",
                        "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                )
            )

            manufacturer.contains("oppo") || manufacturer.contains("realme") ||
                manufacturer.contains("oneplus") -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                ),
                Intent().setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.startupapp.StartupAppListActivity"
                    )
                )
            )

            manufacturer.contains("vivo") -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                )
            )

            manufacturer.contains("samsung") -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.lool.activity.applist.AppListActivity"
                    )
                )
            )

            else -> emptyList()
        }

        for (intent in vendorIntents) {
            if (tryStartActivity(context, intent)) {
                return true
            }
        }
        return false
    }

    /**
     * 构建跳转应用详情页的 Intent。
     */
    private fun buildAppDetailsIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
    }

    /**
     * 安全地启动 Activity，捕获 ActivityNotFoundException。
     * 使用 NEW_TASK 标志以兼容非 Activity Context 调用。
     */
    private fun tryStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.d(TAG, "Intent not available: ${intent.action ?: intent.component?.flattenToString()}")
            false
        } catch (e: Exception) {
            Log.d(TAG, "Failed to launch intent: ${intent.action ?: intent.component?.flattenToString()}: ${e.message}")
            false
        }
    }
}
