package com.bicy.whitenoise.y10p

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
    private const val LOG_DIR_NAME = "AddSky/Log"
    private const val MAX_LOG_FILES = 10
    private const val MAX_LOG_SIZE_BYTES = 5 * 1024 * 1024L
    private const val APP_PACKAGE = "com.bicy.whitenoise"
    
    private var contextRef: WeakReference<Context>? = null
    private var logDir: File? = null
    private var currentLogFile: File? = null
    private var crashLogFile: File? = null
    private var logWriter: PrintWriter? = null
    
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val logcatThread: Thread? = null
    private val isLogcatRunning = AtomicBoolean(false)
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    
    private var isInitialized = false
    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null
    private var logEnabled = false
    
    fun init(context: Context) {
        contextRef = WeakReference(context.applicationContext)
        
        try {
            logDir = File(context.getExternalFilesDir(null), LOG_DIR_NAME)
            
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
            
            writeCrashLog(thread, throwable)
            
            defaultExceptionHandler?.uncaughtException(thread, throwable)
        }
    }
    
    private fun writeCrashLog(thread: Thread, throwable: Throwable) {
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
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log: ${e.message}")
            e.printStackTrace()
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
    
    private fun cleanOldLogFiles() {
        logDir?.listFiles()?.let { files ->
            val logFiles = files.filter { it.name.endsWith(".log") }
                .sortedByDescending { it.lastModified() }
            
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
        if (!logEnabled) return
        
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
    
    fun v(tag: String, message: String) {
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
    }
    
    fun release() {
        stopLogcatCapture()
        executor.shutdown()
        flushLogs()
        closeLogFile()
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
