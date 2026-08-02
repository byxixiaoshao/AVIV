package com.bicy.whitenoise.service

import java.util.UUID

/** 异常等级 — 低/中仅记录通知，高/极高触发拦截 */
enum class AnomalyLevel {
    NORMAL, LOW, MEDIUM, HIGH, CRITICAL
}

/** 异常分类 */
enum class AnomalyCategory {
    SYSTEM, AUDIO, NETWORK, GENERAL
}

/** 异常类型枚举 */
enum class AnomalyType {
    // === 极高 (CRITICAL) ===
    JAVA_CRASH,
    ANR,
    MEMORY_EXHAUSTED,

    // === 高 (HIGH) ===
    TRIM_MEMORY_CRITICAL,
    THREAD_LEAK,
    AUDIO_ENGINE_RESTART,

    // === 中 (MEDIUM) ===
    TRIM_MEMORY_LOW,
    GC_PRESSURE,
    MAIN_THREAD_STALL,
    AUDIO_PLAYER_ERROR,
    AUDIO_DECODE_ERROR,
    AUDIO_BUFFER_UNDERRUN,
    AUDIO_LOAD_TIMEOUT,
    AUDIO_FOCUS_ERROR,

    // === 低 (LOW) ===
    TRIM_MEMORY_MODERATE,
    MEMORY_WARNING,
    MAIN_THREAD_JANK,
    NETWORK_TIMEOUT,
    NETWORK_ERROR,
    IO_ERROR,
    CRYPTO_ERROR,
    AUDIO_VISUALIZATION_ERROR,
}

/** 异常记录 */
data class AnomalyRecord(
    val id: String = UUID.randomUUID().toString().take(8),
    val type: AnomalyType,
    val level: AnomalyLevel,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val detail: String? = null
)

/** 系统实时指标 */
data class SystemMetrics(
    val heapUsagePercent: Float = 0f,
    val threadCount: Int = 0,
    val gcCountLastMin: Int = 0,
    val mainThreadLatencyMs: Long = 0L
)

/** 内存锁全局状态 — StateFlow 暴露 */
data class MemoryLockState(
    val currentLevel: AnomalyLevel = AnomalyLevel.NORMAL,
    val recentAnomalies: List<AnomalyRecord> = emptyList(),
    val systemMetrics: SystemMetrics = SystemMetrics()
)
