package com.bicy.whitenoise.service

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * ## 系统指标采集器
 *
 * 在主线程+独立线程采集实时指标，供 StateFlow 消费。
 *
 * 采集维度：
 * - 堆内存使用率
 * - 活跃线程数
 * - GC 频率（每分钟）
 * - 主线程消息延迟
 */
internal class SystemMetricsCollector {

    companion object {
        private const val TAG = "MetricsCollector"
        private const val SAMPLE_INTERVAL_MS = 5000L
        private const val MAIN_THREAD_LATENCY_CHECK_MS = 30000L
        private const val THREAD_LEAK_THRESHOLD = 200
        private const val GC_HISTORY_WINDOW_MS = 60_000L // 1分钟
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val gcTimestamps = ConcurrentLinkedQueue<Long>()

    var currentMetrics: SystemMetrics = SystemMetrics()
        private set

    var onThreadLeak: (() -> Unit)? = null

    fun start() {
        executor.scheduleAtFixedRate(::collect, 0, SAMPLE_INTERVAL_MS, TimeUnit.MILLISECONDS)
        Log.i(TAG, "系统指标采集器启动 (间隔=${SAMPLE_INTERVAL_MS}ms)")
    }

    fun recordGc() {
        gcTimestamps.add(System.currentTimeMillis())
    }

    fun stop() {
        executor.shutdownNow()
        Log.i(TAG, "系统指标采集器停止")
    }

    private fun collect() {
        try {
            val heapUsage = collectHeapUsage()
            val threads = Thread.getAllStackTraces().size
            val gcCount = countRecentGc()

            // 主线程延迟（降低频率检测）
            var mainLatency = currentMetrics.mainThreadLatencyMs
            if (System.currentTimeMillis() % MAIN_THREAD_LATENCY_CHECK_MS < SAMPLE_INTERVAL_MS) {
                mainLatency = measureMainThreadLatency()
            }

            currentMetrics = SystemMetrics(
                heapUsagePercent = heapUsage,
                threadCount = threads,
                gcCountLastMin = gcCount,
                mainThreadLatencyMs = mainLatency
            )

            // 线程泄漏检测
            if (threads > THREAD_LEAK_THRESHOLD) {
                Log.w(TAG, "线程泄漏警告: $threads 个活跃线程 (阈值=$THREAD_LEAK_THRESHOLD)")
                onThreadLeak?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "指标采集异常", e)
        }
    }

    private fun collectHeapUsage(): Float {
        val rt = Runtime.getRuntime()
        return if (rt.maxMemory() > 0L)
            (rt.totalMemory() - rt.freeMemory()).toFloat() / rt.maxMemory().toFloat()
        else 0f
    }

    private fun countRecentGc(): Int {
        val cutoff = System.currentTimeMillis() - GC_HISTORY_WINDOW_MS
        gcTimestamps.removeIf { it < cutoff }
        return gcTimestamps.size
    }

    private fun measureMainThreadLatency(): Long {
        val startTime = System.currentTimeMillis()
        val latch = java.util.concurrent.CountDownLatch(1)
        mainHandler.post { latch.countDown() }
        return try {
            latch.await(100, TimeUnit.MILLISECONDS)
            System.currentTimeMillis() - startTime
        } catch (_: Exception) { 300L }
    }
}
