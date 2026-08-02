package com.bicy.whitenoise

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import android.widget.Toast
import com.bicy.whitenoise.service.AnomalyLevel
import com.bicy.whitenoise.service.LogCaptureService
import com.bicy.whitenoise.service.MemoryLockService
import com.bicy.whitenoise.utils.AppInitializer
import com.bicy.whitenoise.utils.LogManager
import com.bicy.whitenoise.storage.config.ConfigStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WhiteNoiseApp : Application() {

    companion object {
        lateinit var context: Context
            private set
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        // 最早初始化日志系统（提供 crash log 写入目录）
        LogManager.init(this)

        // 第一道防线：安装崩溃处理器（必须在任何可能崩溃的代码之前）
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("WhiteNoiseApp", "Crash intercepted: ${throwable.javaClass.name}", throwable)
            try {
                LogManager.writeCrashLogDirect(thread, throwable)
                Thread.sleep(500)
                Process.killProcess(Process.myPid())
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // 启动内存锁（增强崩溃处理 + ANR 检测 + 内存压力监控）
        MemoryLockService.start(this)

        // 始终启动 :log 独立进程（Clash 捕获 + 崩溃监控）
        startService(Intent(this, LogCaptureService::class.java))

        // 主进程 App 日志：仅在设置启用时捕获
        if (ConfigStorage.isLogEnabled()) {
            LogManager.setLogEnabled(true)
        }
        AppInitializer.init(this)

        // 观察内存锁 StateFlow，LOW 级别异常显示应用内通知
        observeMemoryLockState()
    }

    private fun observeMemoryLockState() {
        var lastToastId = ""
        appScope.launch {
            MemoryLockService.state.collect { state ->
                val latest = state.recentAnomalies.firstOrNull() ?: return@collect
                if (latest.id == lastToastId) return@collect
                if (latest.level == AnomalyLevel.LOW) {
                    lastToastId = latest.id
                    Toast.makeText(
                        this@WhiteNoiseApp,
                        "已记录异常: ${latest.message}\n查看详情: MemoryLock/ 目录",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
