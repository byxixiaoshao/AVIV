package com.bicy.whitenoise.utils

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object LogManager {
    
    private const val TAG = "LogManager"
    private const val LOG_DIR_NAME = "logs/Logcat"
    private const val MAX_LOG_FILES = 10
    private const val MAX_LOG_SIZE_BYTES = 5 * 1024 * 1024L
    private const val APP_PACKAGE = "com.bicy.whitenoise"
    
    // Clash 日志过滤标签
    private val CLASH_TAGS = arrayOf(
        "clash", "Clash", "ClashMeta", "ClashForAndroid",
        "ClashR", "moe.matsuri", "ClashX", "proxy", "tun2socks"
    )
    
    private var contextRef: WeakReference<Context>? = null
    private var logDir: File? = null
    private var currentLogFile: File? = null
    private var crashLogFile: File? = null
    private var logWriter: PrintWriter? = null
    private var clashLogWriter: PrintWriter? = null
    private var currentClashLogFile: File? = null
    
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val clashLogQueue = ConcurrentLinkedQueue<String>()
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val isLogcatRunning = AtomicBoolean(false)
    private val isClashLogcatRunning = AtomicBoolean(false)
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    
    private var isInitialized = false
    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null
    private var logEnabled = false
    
    fun init(context: Context) {
        contextRef = WeakReference(context.applicationContext)
        
        try {
            logDir = File(context.filesDir, LOG_DIR_NAME)
            
            if (logDir?.exists() != true) {
                val created = logDir?.mkdirs()
                Log.d(TAG, "Log directory created: $created, path: ${logDir?.absolutePath}")
            }
            
            setupCrashHandler()
            
            executor.scheduleWithFixedDelay({
                flushLogs()
            }, 1, 1, TimeUnit.SECONDS)
            
            isInitialized = true
            Log.i(TAG, "LogManager initialized, log dir: ${logDir?.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LogManager: ${e.message}")
        }
    }
    
    private fun setupCrashHandler() {
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread: ${thread.name}", throwable)
            
            try {
                val crashFilePath = writeCrashLog(thread, throwable)
                startCrashReportActivity(crashFilePath, throwable)
                Thread.sleep(300)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle crash", e)
            }
            
            defaultExceptionHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun startCrashReportActivity(crashFilePath: String?, throwable: Throwable) {
        val ctx = contextRef?.get() ?: return
        try {
            val crashInfo = "${throwable.javaClass.name}: ${throwable.message ?: "无详细信息"}"
            val intent = android.content.Intent(ctx, com.bicy.whitenoise.ui.CrashReportActivity::class.java).apply {
                addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        or android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                putExtra(com.bicy.whitenoise.ui.CrashReportActivity.EXTRA_CRASH_INFO, crashInfo)
                putExtra(com.bicy.whitenoise.ui.CrashReportActivity.EXTRA_CRASH_FILE, crashFilePath ?: "")
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start crash report activity", e)
        }
    }
    
    /**
     * 供 WhiteNoiseApp 直接调用的崩溃日志写入（不依赖 LogManager 内部状态）
     */
    fun writeCrashLogDirect(thread: Thread, throwable: Throwable): String? {
        if (logDir == null) return null
        var writer: PrintWriter? = null
        try {
            val crashFileName = "crash_${fileDateFormat.format(Date())}.log"
            crashLogFile = File(logDir, crashFileName)

            writer = PrintWriter(FileWriter(crashLogFile, true), true)

            writer.println("========== CRASH LOG ==========")
            writer.println("Time: ${dateFormat.format(Date())}")
            writer.println("Thread: ${thread.name}")
            writer.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            writer.println("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            writer.println("App Version: ${getAppVersion()}")
            writer.println()
            writer.println("Exception: ${throwable.javaClass.name}")
            writer.println("Message: ${throwable.message}")
            writer.println()
            writer.println("Stack Trace:")
            writer.println(Log.getStackTraceString(throwable))
            writer.println()
            writer.println("========== END CRASH LOG ==========")
            writer.flush()

            return crashLogFile?.absolutePath
        } catch (e: Exception) {
            return null
        } finally {
            try { writer?.close() } catch (_: Exception) {}
        }
    }

    private fun writeCrashLog(thread: Thread, throwable: Throwable): String? {
        var writer: PrintWriter? = null
        try {
            val crashFileName = "crash_${fileDateFormat.format(Date())}.log"
            crashLogFile = File(logDir, crashFileName)
            
            writer = PrintWriter(FileWriter(crashLogFile, true), true)
            
            writer.println("========== CRASH LOG ==========")
            writer.println("Time: ${dateFormat.format(Date())}")
            writer.println("Thread: ${thread.name}")
            writer.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            writer.println("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            writer.println("App Version: ${getAppVersion()}")
            writer.println()
            writer.println("Exception: ${throwable.javaClass.name}")
            writer.println("Message: ${throwable.message}")
            writer.println()
            writer.println("Stack Trace:")
            
            val stackTrace = Log.getStackTraceString(throwable)
            writer.println(stackTrace)
            writer.println()
            
            writer.println("Caused by:")
            var cause = throwable.cause
            while (cause != null) {
                writer.println("  ${cause.javaClass.name}: ${cause.message}")
                writer.println("  ${Log.getStackTraceString(cause)}")
                cause = cause.cause
            }
            writer.println()
            
            writer.println("========== END CRASH LOG ==========")
            writer.flush()
            writer.close()
            
            Log.d(TAG, "Crash log written to: ${crashLogFile?.absolutePath}")
            return crashLogFile?.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log: ${e.message}")
            e.printStackTrace()
            return null
        } finally {
            try {
                writer?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close writer: ${e.message}")
            }
        }
    }
    
    private fun getAppVersion(): String {
        return try {
            val ctx = contextRef?.get()
            ctx?.packageManager?.getPackageInfo(ctx.packageName, 0)?.let {
                "${it.versionName} (${it.longVersionCode})"
            } ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun startLogcatCapture() {
        if (isLogcatRunning.getAndSet(true)) {
            return
        }
        
        Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            
            try {
                val pid = android.os.Process.myPid()
                val command = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    arrayOf("logcat", "-v", "time", "--pid", pid.toString())
                } else {
                    arrayOf("logcat", "-v", "time")
                }
                
                val process = Runtime.getRuntime().exec(command)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                
                var line: String? = reader.readLine()
                while (line != null && isLogcatRunning.get()) {
                    if (line.contains(APP_PACKAGE) || line.contains("com.bicy.whitenoise")) {
                        logQueue.offer(line)
                    }
                    line = reader.readLine()
                }
                
                reader.close()
                
            } catch (e: Exception) {
                Log.e(TAG, "Logcat capture error: ${e.message}")
            }
            
            isLogcatRunning.set(false)
        }.start()
    }
    
    private fun stopLogcatCapture() {
        isLogcatRunning.set(false)
        try {
            Runtime.getRuntime().exec("logcat -c")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logcat: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════
    // Clash 日志捕获（最高优先级）
    // ═══════════════════════════════════════════

    fun startClashLogcatCapture() {
        if (isClashLogcatRunning.getAndSet(true)) return

        if (currentClashLogFile == null) {
            createNewClashLogFile()
        }

        Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            try {
                val process = Runtime.getRuntime().exec(arrayOf("logcat", "-v", "time"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                var line: String? = reader.readLine()
                while (line != null && isClashLogcatRunning.get()) {
                    val lower = line.lowercase(Locale.ROOT)
                    for (tag in CLASH_TAGS) {
                        if (lower.contains(tag.lowercase(Locale.ROOT))) {
                            clashLogQueue.offer(line)
                            break
                        }
                    }
                    line = reader.readLine()
                }
                reader.close()
            } catch (e: Exception) {
                Log.e(TAG, "Clash logcat capture error: ${e.message}")
            }
            isClashLogcatRunning.set(false)
        }.apply {
            name = "ClashLogcatReader"
            isDaemon = true
            start()
        }
    }

    fun stopClashLogcatCapture() {
        isClashLogcatRunning.set(false)
    }

    private fun createNewClashLogFile() {
        try {
            closeClashLogFile()
            val fileName = "clash_${fileDateFormat.format(Date())}.log"
            currentClashLogFile = File(logDir, fileName)
            clashLogWriter = PrintWriter(FileWriter(currentClashLogFile, true), true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create clash log file: ${e.message}")
        }
    }

    private fun closeClashLogFile() {
        try {
            clashLogWriter?.flush()
            clashLogWriter?.close()
            clashLogWriter = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close clash log file: ${e.message}")
        }
    }

    private fun checkClashLogFileSize() {
        currentClashLogFile?.let { file ->
            if (file.length() > MAX_LOG_SIZE_BYTES) {
                createNewClashLogFile()
            }
        }
    }

    private fun flushClashLogs() {
        try {
            if (clashLogWriter == null) {
                createNewClashLogFile()
            }
            while (clashLogQueue.isNotEmpty()) {
                val logEntry = clashLogQueue.poll() ?: break
                clashLogWriter?.println(logEntry)
            }
            clashLogWriter?.flush()
            checkClashLogFileSize()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to flush clash logs: ${e.message}")
        }
    }
    
    private fun cleanOldLogFiles() {
        logDir?.listFiles()?.let { files ->
            // 预缓存 lastModified 避免 TimSort 传递性违反（文件在排序期间被删除/time=0）
            val logFiles = files
                .filter { it.name.endsWith(".log") }
                .map { Pair(it, it.lastModified()) }
                .sortedByDescending { (_, time) -> time }
                .map { (file, _) -> file }
            
            if (logFiles.size > MAX_LOG_FILES) {
                logFiles.drop(MAX_LOG_FILES).forEach { oldFile ->
                    oldFile.delete()
                }
            }
        }
    }
    
    private fun createNewLogFile() {
        try {
            closeLogFile()
            
            val fileName = "log_${fileDateFormat.format(Date())}.log"
            currentLogFile = File(logDir, fileName)
            
            logWriter = PrintWriter(FileWriter(currentLogFile, true), true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create log file: ${e.message}")
        }
    }
    
    private fun checkLogFileSize() {
        currentLogFile?.let { file ->
            if (file.length() > MAX_LOG_SIZE_BYTES) {
                createNewLogFile()
            }
        }
    }
    
    private fun closeLogFile() {
        try {
            logWriter?.flush()
            logWriter?.close()
            logWriter = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close log file: ${e.message}")
        }
    }
    
    private fun flushLogs() {
        if (logEnabled) {
            try {
                while (logQueue.isNotEmpty()) {
                    val logEntry = logQueue.poll() ?: break
                    logWriter?.println(logEntry)
                }
                logWriter?.flush()
                checkLogFileSize()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush logs: ${e.message}")
            }
        }
        flushClashLogs()
    }
    
    private fun formatLogEntry(level: String, tag: String, message: String): String {
        val timestamp = dateFormat.format(Date())
        val threadName = Thread.currentThread().name
        return "$timestamp [$level] [$threadName] $tag: $message"
    }
    
    private fun enqueueLog(level: String, tag: String, message: String) {
        if (!isInitialized) {
            Log.println(
                when (level) {
                    "V" -> Log.VERBOSE
                    "D" -> Log.DEBUG
                    "I" -> Log.INFO
                    "W" -> Log.WARN
                    "E" -> Log.ERROR
                    else -> Log.DEBUG
                },
                tag,
                message
            )
            return
        }
        
        if (logEnabled) {
            val entry = formatLogEntry(level, tag, message)
            logQueue.offer(entry)
        }
        
        when (level) {
            "V" -> Log.v(tag, message)
            "D" -> Log.d(tag, message)
            "I" -> Log.i(tag, message)
            "W" -> Log.w(tag, message)
            "E" -> Log.e(tag, message)
        }
    }
    
    fun value(tag: String, message: String) {
        enqueueLog("V", tag, message)
    }
    
    fun d(tag: String, message: String) {
        enqueueLog("D", tag, message)
    }
    
    fun i(tag: String, message: String) {
        enqueueLog("I", tag, message)
    }
    
    fun w(tag: String, message: String) {
        enqueueLog("W", tag, message)
    }
    
    fun e(tag: String, message: String) {
        enqueueLog("E", tag, message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable) {
        val fullMessage = "$message\n${Log.getStackTraceString(throwable)}"
        enqueueLog("E", tag, fullMessage)
    }
    
    fun setLogEnabled(enabled: Boolean) {
        if (enabled == logEnabled) return
        
        logEnabled = enabled
        
        if (enabled) {
            cleanOldLogFiles()
            createNewLogFile()
            startLogcatCapture()
            Log.i(TAG, "Log enabled")
        } else {
            stopLogcatCapture()
            closeLogFile()
            logQueue.clear()
            Log.i(TAG, "Log disabled")
        }
    }
    
    fun isLogEnabled(): Boolean = logEnabled
    
    fun flush() {
        flushLogs()
        flushClashLogs()
    }
    
    fun release() {
        stopLogcatCapture()
        stopClashLogcatCapture()
        executor.shutdown()
        flushLogs()
        closeLogFile()
        closeClashLogFile()
        isInitialized = false
    }
    
    fun getLogDirPath(): String? = logDir?.absolutePath
    
    fun getCurrentLogFilePath(): String? = currentLogFile?.absolutePath
    
    fun getCrashLogFiles(): List<File> {
        return logDir?.listFiles()
            ?.filter { it.name.startsWith("crash_") && it.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
