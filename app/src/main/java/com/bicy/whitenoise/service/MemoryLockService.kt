package com.bicy.whitenoise.service

import android.app.*
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import com.bicy.whitenoise.R
import com.bicy.whitenoise.ui.CrashReportActivity
import com.bicy.whitenoise.utils.LogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ## 内存锁 v2（MemoryLockService）
 *
 * 运行在主进程。作为「内存锁闸门」，全方位监控应用异常状态。
 *
 * 【监控维度】
 * 1. CrashInterceptor — Java 未捕获异常
 * 2. ANRWatchdog — 主线程无响应检测 (独立线程心跳)
 * 3. MemoryPressure — 堆内存枯竭告警
 * 4. SystemMetrics — 线程数/GC频率/主线程延迟采集
 * 5. TrimMemory — 系统内存压力回调
 * 6. MainThreadJank — 主线程卡顿检测 (100-300ms jank, 300-5000ms stall)
 *
 * 【四级响应】
 * - LOW：应用内通知 (StateFlow → UI Toast)
 * - MEDIUM：系统通知
 * - HIGH：拦截页 (继续运行 / 重新启动)
 * - CRITICAL：拦截页 (关闭应用 / 重新启动) + kill 主进程
 *
 * 【诊断报告】
 * 所有等级异常均写入 memory_lock_*.log
 */
class MemoryLockService : Service() {

    companion object {
        private const val TAG = "MemoryLock"

        // === 配置常量 ===
        private const val WATCHDOG_INTERVAL_MS = 1000L
        private const val ANR_THRESHOLD_MS = 5000L
        private const val JANK_THRESHOLD_MS = 100L
        private const val STALL_THRESHOLD_MS = 300L
        private const val MEMORY_CHECK_INTERVAL_MS = 5000L
        private const val MEMORY_CRITICAL_RATIO = 0.90f
        private const val MEMORY_HIGH_RATIO = 0.85f
        private const val HEARTBEAT_WAIT_MS = 100L
        private const val CONSECUTIVE_MISSES_TO_KILL = 3

        // === 通知 ===
        const val CHANNEL_ID = "memory_lock_channel"
        const val ALERT_CHANNEL_ID = "memory_lock_alert_channel"
        const val NOTIFICATION_ID = 3001
        private const val ALERT_NOTIFICATION_ID = 3002
        private const val MAX_RECENT_ANOMALIES = 20

        private val isRunning = AtomicBoolean(false)
        private var appContext: Context? = null
        @Volatile
        private var instance: MemoryLockService? = null

        // === StateFlow ===
        private val _state = MutableStateFlow(MemoryLockState())
        val state: StateFlow<MemoryLockState> = _state.asStateFlow()

        /**
         * ## 全局异常上报 API
         *
         * 任意模块调用此方法上报异常，内存锁自动分级并分发响应。
         */
        fun reportAnomaly(type: AnomalyType, message: String, detail: String? = null, throwable: Throwable? = null) {
            val level = AnomalyClassifier.classify(type)
            val record = AnomalyRecord(type = type, level = level, message = message, detail = detail)

            val current = _state.value
            val updated = listOf(record) + current.recentAnomalies.take(MAX_RECENT_ANOMALIES - 1)
            val newLevel = if (level.ordinal > current.currentLevel.ordinal) level else current.currentLevel

            _state.value = current.copy(currentLevel = newLevel, recentAnomalies = updated)

            Log.i(TAG, "上报异常 [${level.name}] $type: $message")
            
            // 所有级别均写入诊断日志
            instance?.writeAnomalyLog(type.name.lowercase(), record, throwable)
            
            dispatchResponse(record)
        }

        /** 重置异常等级至 NORMAL */
        fun resetLevel() {
            _state.value = _state.value.copy(currentLevel = AnomalyLevel.NORMAL)
        }

        fun start(context: Context) {
            appContext = context.applicationContext
            val intent = Intent(context, MemoryLockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MemoryLockService::class.java))
        }

        // === 响应分发 ===
        private fun dispatchResponse(record: AnomalyRecord) {
            val ctx = appContext ?: return
            when (record.level) {
                AnomalyLevel.LOW -> {
                    // 仅更新 StateFlow，UI 层显示 Toast
                    Log.i(TAG, "LOW 异常已记录: ${record.message}")
                }
                AnomalyLevel.MEDIUM -> {
                    showSystemAlert(ctx, record)
                }
                AnomalyLevel.HIGH -> {
                    startInterceptionPage(ctx, record, isCritical = false)
                }
                AnomalyLevel.CRITICAL -> {
                    startInterceptionPage(ctx, record, isCritical = true)
                }
                AnomalyLevel.NORMAL -> {}
            }
        }

        private fun showSystemAlert(context: Context, record: AnomalyRecord) {
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(ALERT_CHANNEL_ID, "内存锁异常告警", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "异常状态警告"
                }
                nm.createNotificationChannel(channel)

                val logDir = File(context.filesDir, "logs/MemoryLock").absolutePath
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                val pi = PendingIntent.getActivity(context, ALERT_NOTIFICATION_ID, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val notification = Notification.Builder(context, ALERT_CHANNEL_ID)
                    .setContentTitle("异常告警: ${record.message}")
                    .setContentText("已记录异常，请查看: $logDir")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build()

                nm.notify(ALERT_NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e(TAG, "系统通知发送失败", e)
            }
        }

        private fun startInterceptionPage(context: Context, record: AnomalyRecord, isCritical: Boolean) {
            try {
                val intent = Intent(context, CrashReportActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(CrashReportActivity.EXTRA_CRASH_INFO, record.message)
                    putExtra(CrashReportActivity.EXTRA_CRASH_FILE, "")
                    putExtra(CrashReportActivity.EXTRA_ANOMALY_LEVEL, record.level.name)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "无法启动拦截页面", e)
            }
        }
    }

    // ==================== 实例 ====================
    private val watchdogExecutor = Executors.newSingleThreadScheduledExecutor()
    private val diagnosticExecutor = Executors.newSingleThreadExecutor()
    private val metricsCollector = SystemMetricsCollector()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastHeartbeatAck: Long = 0L
    private var consecutiveAnrMisses: Int = 0
    private var crashLogDir: File? = null
    private var foregroundStarted = false

    // ==================== 生命周期 ====================

    override fun onCreate() {
        super.onCreate()
        if (isRunning.getAndSet(true)) return

        Log.i(TAG, "内存锁 v2 启动 — 全面监控")

        appContext = applicationContext
        instance = this
        crashLogDir = File(filesDir, "logs/MemoryLock")
        crashLogDir?.mkdirs()

        enhanceCrashHandler()
        startANRWatchdog()
        startMemoryMonitor()
        startJankDetector()
        metricsCollector.start()
        metricsCollector.onThreadLeak = {
            reportAnomaly(AnomalyType.THREAD_LEAK, "线程泄漏: ${metricsCollector.currentMetrics.threadCount} 个活跃线程")
        }

        tryForeground()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                reportAnomaly(AnomalyType.TRIM_MEMORY_CRITICAL, "系统内存严重不足 (TRIM_MEMORY_RUNNING_CRITICAL)")
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                reportAnomaly(AnomalyType.TRIM_MEMORY_LOW, "系统内存不足 (TRIM_MEMORY_RUNNING_LOW)")
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                reportAnomaly(AnomalyType.TRIM_MEMORY_MODERATE, "系统内存压力 (TRIM_MEMORY_RUNNING_MODERATE)")
            }
        }
    }

    override fun onDestroy() {
        isRunning.set(false)
        instance = null
        watchdogExecutor.shutdownNow()
        diagnosticExecutor.shutdownNow()
        metricsCollector.stop()
        if (foregroundStarted) stopForeground(STOP_FOREGROUND_REMOVE)
        Log.i(TAG, "内存锁服务停止")
        super.onDestroy()
    }

    private fun tryForeground() {
        try {
            val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            startForeground(NOTIFICATION_ID, buildNotification(), foregroundType)
            foregroundStarted = true
        } catch (_: ForegroundServiceStartNotAllowedException) {
            Log.w(TAG, "前台服务启动被拒绝，以降级模式运行")
        } catch (_: RuntimeException) {
            Log.w(TAG, "前台服务启动失败，以降级模式运行")
        }
    }

    // ==================== 1. CrashInterceptor ====================

    private fun enhanceCrashHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "内存锁拦截到崩溃: ${throwable.javaClass.name}")

                reportAnomaly(AnomalyType.JAVA_CRASH,
                    "${throwable.javaClass.name}: ${throwable.message ?: "无详细信息"}",
                    Log.getStackTraceString(throwable))

                val diagnostic = collectDiagnostic("Java Crash", throwable, thread)
                val logFile = writeDiagnosticLog("crash", diagnostic, throwable)

                startCrashReport("Java Crash", throwable, logFile)
            } catch (e: Exception) {
                Log.e(TAG, "崩溃处理失败，回退到原始 handler", e)
                previousHandler?.uncaughtException(thread, throwable)
                return@setDefaultUncaughtExceptionHandler
            } finally {
                Thread.sleep(1000)
                ProcessKiller.kill("Java Crash: ${throwable.message}")
            }
        }
    }

    // ==================== 2. ANRWatchdog ====================

    private fun startANRWatchdog() {
        lastHeartbeatAck = System.currentTimeMillis()
        consecutiveAnrMisses = 0

        watchdogExecutor.scheduleAtFixedRate({
            try {
                mainHandler.post { lastHeartbeatAck = System.currentTimeMillis(); consecutiveAnrMisses = 0 }
                Thread.sleep(HEARTBEAT_WAIT_MS)

                val elapsed = System.currentTimeMillis() - lastHeartbeatAck
                when {
                    elapsed > ANR_THRESHOLD_MS -> {
                        consecutiveAnrMisses++
                        Log.w(TAG, "ANR 检测: 主线程 ${elapsed}ms 无响应 (连续 ${consecutiveAnrMisses}/${CONSECUTIVE_MISSES_TO_KILL})")
                        if (consecutiveAnrMisses >= CONSECUTIVE_MISSES_TO_KILL) {
                            Log.e(TAG, "!! ANR 确认，执行诊断并终止")
                            reportAnomaly(AnomalyType.ANR, "ANR: 主线程阻塞 ${elapsed}ms",
                                collectThreadDump().take(2000))
                            diagnosticExecutor.execute {
                                try {
                                    val diagnostic = collectDiagnostic("ANR（主线程无响应 ${elapsed}ms）", null, null)
                                    val logFile = writeDiagnosticLog("anr", diagnostic, null)
                                    startCrashReport("ANR", null, logFile)
                                    Thread.sleep(1000)
                                } catch (e: Exception) {
                                    Log.e(TAG, "ANR 诊断收集失败", e)
                                }
                                ProcessKiller.kill("ANR: 主线程阻塞 ${elapsed}ms")
                            }
                        }
                    }
                    // 连续超时 3 次即 ANR → 在此之前不单独上报 jank/stall
                }
            } catch (e: Exception) {
                Log.e(TAG, "Watchdog error", e)
            }
        }, 0, WATCHDOG_INTERVAL_MS, TimeUnit.MILLISECONDS)

        Log.i(TAG, "ANRWatchdog 启动 (心跳=${WATCHDOG_INTERVAL_MS}ms, 阈值=${ANR_THRESHOLD_MS}ms, 连续=${CONSECUTIVE_MISSES_TO_KILL}次)")
    }

    // ==================== 3. JankDetector (主线程卡顿) ====================

    private fun startJankDetector() {
        watchdogExecutor.scheduleAtFixedRate({
            try {
                val startTime = System.currentTimeMillis()
                val latch = java.util.concurrent.CountDownLatch(1)
                mainHandler.post { latch.countDown() }
                latch.await(ANR_THRESHOLD_MS, TimeUnit.MILLISECONDS)
                val latency = System.currentTimeMillis() - startTime

                when {
                    latency in JANK_THRESHOLD_MS..<STALL_THRESHOLD_MS -> {
                        reportAnomaly(AnomalyType.MAIN_THREAD_JANK, "主线程卡顿 ${latency}ms")
                    }
                    latency >= STALL_THRESHOLD_MS && latency < ANR_THRESHOLD_MS -> {
                        reportAnomaly(AnomalyType.MAIN_THREAD_STALL, "主线程严重阻塞 ${latency}ms")
                    }
                }
            } catch (_: Exception) {}
        }, 0, 2000L, TimeUnit.MILLISECONDS)

        Log.i(TAG, "JankDetector 启动 (间隔=2000ms, jank>$JANK_THRESHOLD_MS ms, stall>$STALL_THRESHOLD_MS ms)")
    }

    // ==================== 4. MemoryPressure ====================

    private fun startMemoryMonitor() {
        watchdogExecutor.scheduleAtFixedRate({
            try {
                val rt = Runtime.getRuntime()
                val used = rt.totalMemory() - rt.freeMemory()
                val max = rt.maxMemory()
                val ratio = used.toFloat() / max.toFloat()
                val usedMb = used / (1024.0 * 1024.0)
                val maxMb = max / (1024.0 * 1024.0)

                // 更新 StateFlow 指标
                val cur = _state.value
                _state.value = cur.copy(
                    systemMetrics = cur.systemMetrics.copy(
                        heapUsagePercent = ratio * 100f,
                        threadCount = Thread.getAllStackTraces().size
                    )
                )

                when {
                    ratio > MEMORY_CRITICAL_RATIO -> {
                        Log.e(TAG, "!! 内存枯竭: ${String.format("%.1f", ratio * 100)}% (${String.format("%.1f", usedMb)}/${String.format("%.1f", maxMb)} MB)")

                        reportAnomaly(AnomalyType.MEMORY_EXHAUSTED,
                            "内存枯竭: ${String.format("%.1f", ratio * 100)}% (${String.format("%.1f", usedMb)}/${String.format("%.1f", maxMb)} MB)",
                            collectMemoryStats().take(1000))

                        diagnosticExecutor.execute {
                            try {
                                val diagnostic = collectDiagnostic("内存枯竭（${String.format("%.1f", ratio * 100)}%）", null, null)
                                val logFile = writeDiagnosticLog("memory", diagnostic, null)
                                startCrashReport("MemoryPressure", null, logFile)
                                Thread.sleep(1000)
                            } catch (e: Exception) {
                                Log.e(TAG, "内存诊断收集失败", e)
                            }
                            ProcessKiller.kill("内存枯竭: ${String.format("%.1f", ratio * 100)}%")
                        }
                    }
                    ratio > MEMORY_HIGH_RATIO -> {
                        reportAnomaly(AnomalyType.MEMORY_WARNING,
                            "内存预警: ${String.format("%.1f", ratio * 100)}%")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Memory monitor error", e)
            }
        }, MEMORY_CHECK_INTERVAL_MS, MEMORY_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS)

        Log.i(TAG, "MemoryPressureMonitor 启动 (间隔=${MEMORY_CHECK_INTERVAL_MS}ms, 临界=$MEMORY_CRITICAL_RATIO, 预警=$MEMORY_HIGH_RATIO)")
    }

    // ==================== 诊断采集 ====================

    data class DiagnosticInfo(
        val crashType: String,
        val timestamp: String,
        val exceptionDetail: String,
        val threadDump: String,
        val memoryStats: String,
        val processState: String,
        val recentLogs: String
    ) {
        fun toFullReport(): String = buildString {
            appendLine("══════════════════════════════════════════")
            appendLine("  内存锁 · 诊断报告")
            appendLine("══════════════════════════════════════════")
            appendLine("类型: $crashType")
            appendLine("时间: $timestamp")
            appendLine()
            appendLine("─── 异常详情 ───")
            appendLine(exceptionDetail)
            appendLine()
            appendLine("─── 线程 Dump ───")
            appendLine(threadDump)
            appendLine()
            appendLine("─── 内存统计 ───")
            appendLine(memoryStats)
            appendLine()
            appendLine("─── 进程状态 ───")
            appendLine(processState)
            appendLine()
            appendLine("─── 最近日志 ───")
            appendLine(recentLogs)
            appendLine("══════════════════════════════════════════")
        }
    }

    private fun collectDiagnostic(crashType: String, exception: Throwable?, thread: Thread?): DiagnosticInfo {
        return DiagnosticInfo(
            crashType = crashType,
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
            exceptionDetail = formatException(exception, thread),
            threadDump = collectThreadDump(),
            memoryStats = collectMemoryStats(),
            processState = collectProcessState(),
            recentLogs = collectRecentLogs()
        )
    }

    private fun formatException(throwable: Throwable?, thread: Thread?): String {
        if (throwable == null) return "无异常对象（系统触发）"
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        pw.println("线程: ${thread?.name ?: "unknown"}")
        pw.println("异常: ${throwable.javaClass.name}")
        pw.println("消息: ${throwable.message ?: "(无)"}")
        pw.println()
        pw.println("Stack Trace:")
        throwable.printStackTrace(pw)
        var cause = throwable.cause
        while (cause != null) {
            pw.println()
            pw.println("Caused by: ${cause.javaClass.name}: ${cause.message}")
            cause.printStackTrace(pw)
            cause = cause.cause
        }
        pw.flush()
        return sw.toString()
    }

    private fun collectThreadDump(): String = buildString {
        val allThreads = Thread.getAllStackTraces()
        appendLine("共 ${allThreads.size} 个活跃线程")
        appendLine()
        val mainThread = allThreads.entries.find { it.key.name == "main" }
        if (mainThread != null) {
            appendLine("── 主线程 (main) ──")
            formatThread(mainThread.key, mainThread.value)
            appendLine()
        }
        for ((t, stack) in allThreads.entries.sortedBy { it.key.name }) {
            if (t.name == "main") continue
            appendLine("── ${t.name} [${t.state}] [${t.priority}] ──")
            formatThread(t, stack)
            appendLine()
        }
    }

    private fun StringBuilder.formatThread(t: Thread, stack: Array<StackTraceElement>) {
        for (element in stack) appendLine("  at $element")
    }

    private fun collectMemoryStats(): String = buildString {
        val rt = Runtime.getRuntime()
        val used = rt.totalMemory() - rt.freeMemory()
        appendLine("堆内存:")
        appendLine("  已使用: ${formatMB(used)} (${String.format("%.1f", used.toFloat() / rt.maxMemory().toFloat() * 100)}%)")
        appendLine("  已分配: ${formatMB(rt.totalMemory())}")
        appendLine("  最大堆: ${formatMB(rt.maxMemory())}")
        appendLine("  空闲:   ${formatMB(rt.freeMemory())}")
        try {
            val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfoArray = actManager.getProcessMemoryInfo(intArrayOf(android.os.Process.myPid()))
            if (memInfoArray.isNotEmpty()) {
                val memInfo = memInfoArray[0]
                appendLine()
                appendLine("进程内存 (PSS):")
                appendLine("  Total PSS:   ${formatKB(memInfo.totalPss)}")
                appendLine("  Dalvik PSS:  ${formatKB(memInfo.dalvikPss)}")
                appendLine("  Native PSS:  ${formatKB(memInfo.nativePss)}")
                appendLine("  Other PSS:   ${formatKB(memInfo.otherPss)}")
            }
        } catch (_: Exception) {}
    }

    private fun collectProcessState(): String {
        return try {
            File("/proc/self/status").readText().take(4096)
        } catch (e: Exception) {
            "无法读取进程状态: ${e.message}"
        }
    }

    private fun collectRecentLogs(): String {
        return try {
            val logDir = File(filesDir, "logs/Logcat")
            if (!logDir.exists()) return "日志目录不存在"
            val logFiles = logDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".log") }
                ?.sortedByDescending { it.lastModified() }
                ?: return "无日志文件"
            if (logFiles.isEmpty()) return "无日志文件"
            logFiles.first().readLines().takeLast(50).joinToString("\n")
        } catch (e: Exception) {
            "无法读取日志: ${e.message}"
        }
    }

    // ==================== 输出 ====================

    /**
     * 由 reportAnomaly 调用，异步写入简约诊断日志
     */
    private fun writeAnomalyLog(prefix: String, record: AnomalyRecord, throwable: Throwable?) {
        diagnosticExecutor.execute {
            try {
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                val exceptionDetail = buildString {
                    appendLine("等级: ${record.level.name}")
                    appendLine("类型: ${record.type.name}")
                    appendLine("消息: ${record.message}")
                    record.detail?.let { appendLine("详情: $it") }
                    throwable?.let {
                        appendLine()
                        appendLine("异常堆栈:")
                        val sw = java.io.StringWriter()
                        it.printStackTrace(PrintWriter(sw))
                        appendLine(sw.toString())
                    }
                }
                val diagnostic = DiagnosticInfo(
                    crashType = "[${record.level.name}] ${record.type.name}",
                    timestamp = dateStr,
                    exceptionDetail = exceptionDetail,
                    threadDump = collectThreadDump(),
                    memoryStats = collectMemoryStats(),
                    processState = collectProcessState(),
                    recentLogs = collectRecentLogs()
                )
                writeDiagnosticLog(prefix, diagnostic, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "异步写入异常日志失败", e)
            }
        }
    }

    private fun writeDiagnosticLog(prefix: String, diagnostic: DiagnosticInfo, exception: Throwable?): String? {
        return try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val file = File(crashLogDir, "memory_lock_${prefix}_$dateStr.log")
            file.writeText(diagnostic.toFullReport())
            Log.i(TAG, "诊断报告已写入: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "写入诊断报告失败", e)
            null
        }
    }

    private fun startCrashReport(type: String, throwable: Throwable?, logFile: String?) {
        try {
            val intent = Intent(this, CrashReportActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(CrashReportActivity.EXTRA_CRASH_INFO,
                    throwable?.let { "${it.javaClass.name}: ${it.message ?: "无详细信息"}" }
                        ?: "内存锁检测: $type")
                putExtra(CrashReportActivity.EXTRA_CRASH_FILE, logFile ?: "")
                putExtra(CrashReportActivity.EXTRA_ANOMALY_LEVEL, AnomalyLevel.CRITICAL.name)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "无法启动崩溃报告 Activity", e)
        }
    }

    // ==================== 通知 ====================

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(CHANNEL_ID, "内存锁监控", NotificationManager.IMPORTANCE_LOW).apply {
            description = "应用运行时保护服务"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, com.bicy.whitenoise.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("内存锁保护中")
            .setContentText("自动监控应用异常状态")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    // ==================== 工具 ====================

    private fun formatMB(bytes: Long): String = String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    private fun formatKB(kb: Int): String = if (kb >= 1024) String.format("%.1f MB", kb / 1024f) else "${kb} KB"
}

// ==================== ProcessKiller ====================

object ProcessKiller {
    fun kill(reason: String) {
        Log.e("MemoryLock", "正在终止进程... 原因: $reason")
        android.os.Process.killProcess(android.os.Process.myPid())
        Thread.sleep(200)
        System.exit(0)
    }
}
