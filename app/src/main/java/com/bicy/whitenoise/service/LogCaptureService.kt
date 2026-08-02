package com.bicy.whitenoise.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.bicy.whitenoise.MainActivity
import com.bicy.whitenoise.R
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.ui.CrashReportActivity
import com.bicy.whitenoise.utils.LogManager
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 日志捕获前台服务，运行在独立进程 :log 中，始终运行。
 * - Clash 日志捕获：始终以最高优先级运行
 * - App 日志捕获：根据用户设置开关
 * - 崩溃文件监控：扫描 crash_*.log 文件，检测到后启动 CrashReportActivity
 */
class LogCaptureService : Service() {

    companion object {
        const val CHANNEL_ID = "log_capture_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_ENABLE_APP_LOG = "com.bicy.whitenoise.ACTION_ENABLE_APP_LOG"
        const val ACTION_DISABLE_APP_LOG = "com.bicy.whitenoise.ACTION_DISABLE_APP_LOG"

        private const val TAG = "LogCaptureService"
        private const val CRASH_WATCH_INTERVAL_MS = 1500L
    }

    private val crashWatcherExecutor = Executors.newSingleThreadScheduledExecutor()
    private val processedCrashFiles = mutableSetOf<String>()
    private val isWatching = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        LogManager.init(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, buildNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Log.w(TAG, "前台服务启动被拒绝（dataSync 时间额度已耗尽），以降级模式运行", e)
        } catch (e: RuntimeException) {
            Log.w(TAG, "前台服务启动失败，以降级模式运行", e)
        }

        // ── 始终运行 ──
        LogManager.startClashLogcatCapture()
        startCrashFileWatcher()

        // ── App 日志：根据 Intent action 或 ConfigStorage ──
        when (intent?.action) {
            ACTION_ENABLE_APP_LOG -> {
                LogManager.setLogEnabled(true)
                updateNotification()
            }
            ACTION_DISABLE_APP_LOG -> {
                LogManager.setLogEnabled(false)
                updateNotification()
            }
            else -> {
                if (!LogManager.isLogEnabled() && ConfigStorage.isLogEnabled()) {
                    LogManager.setLogEnabled(true)
                }
                LogManager.i(TAG, "日志监控服务已启动（独立进程）")
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCrashFileWatcher()
        LogManager.stopClashLogcatCapture()
        LogManager.setLogEnabled(false)
        // 异步释放资源，避免阻塞 onDestroy 导致系统超时
        Thread {
            LogManager.release()
        }.start()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    // ═══════════════════════════════════════════
    // 崩溃文件监控
    // ═══════════════════════════════════════════

    private fun startCrashFileWatcher() {
        if (!isWatching.compareAndSet(false, true)) return

        // 预填充已有崩溃文件，只监控启动后新产生的
        try {
            val logDir = File(filesDir, "logs/Logcat")
            if (logDir.exists()) {
                logDir.listFiles()
                    ?.filter { it.name.startsWith("crash_") && it.name.endsWith(".log") }
                    ?.forEach { processedCrashFiles.add(it.absolutePath) }
            }
        } catch (_: Exception) {}

        crashWatcherExecutor.scheduleWithFixedDelay({
            try {
                val logDir = File(filesDir, "logs/Logcat")
                if (!logDir.exists()) return@scheduleWithFixedDelay

                val crashFiles = logDir.listFiles()
                    ?.filter { it.name.startsWith("crash_") && it.name.endsWith(".log") }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()

                for (file in crashFiles) {
                    val absPath = file.absolutePath
                    if (processedCrashFiles.add(absPath)) {
                        LogManager.e(TAG, "检测到崩溃日志: ${file.name}")
                        launchCrashReport(file)
                        break
                    }
                }
            } catch (e: Exception) {
                LogManager.e(TAG, "崩溃文件监控异常", e)
            }
        }, 0, CRASH_WATCH_INTERVAL_MS, TimeUnit.MILLISECONDS)
    }

    private fun stopCrashFileWatcher() {
        isWatching.set(false)
        crashWatcherExecutor.shutdown()
    }

    private fun launchCrashReport(crashFile: File) {
        try {
            val content = crashFile.readText()
            val crashInfo = content.lines()
                .firstOrNull { it.startsWith("Exception:") }
                ?.removePrefix("Exception:")
                ?: "应用崩溃"

            val intent = Intent(this, CrashReportActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(CrashReportActivity.EXTRA_CRASH_INFO, crashInfo)
                putExtra(CrashReportActivity.EXTRA_CRASH_FILE, crashFile.absolutePath)
            }
            startActivity(intent)
        } catch (e: Exception) {
            LogManager.e(TAG, "无法启动崩溃报告页面", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "日志监控",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示日志监控服务的运行状态"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = if (LogManager.isLogEnabled()) "正在记录 Clash 与应用日志" else "正在记录 Clash 日志"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("日志监控运行中")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("日志监控运行中")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }
}
