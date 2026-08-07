package com.bicy.whitenoise.service

/**
 * ## 异常分级器
 *
 * 将 AnomalyType 映射到 AnomalyLevel，全部硬编码阈值。
 */
object AnomalyClassifier {

    /**
     * 根据异常类型和可选上下文分级
     */
    fun classify(type: AnomalyType, context: ClassifyContext = ClassifyContext()): AnomalyLevel = when (type) {
        // === CRITICAL ===
        AnomalyType.JAVA_CRASH,
        AnomalyType.ANR,
        AnomalyType.MEMORY_EXHAUSTED -> AnomalyLevel.CRITICAL

        // === HIGH ===
        AnomalyType.TRIM_MEMORY_CRITICAL,
        AnomalyType.THREAD_LEAK,
        AnomalyType.AUDIO_ENGINE_RESTART -> AnomalyLevel.HIGH

        // === MEDIUM ===
        AnomalyType.TRIM_MEMORY_LOW,
        AnomalyType.GC_PRESSURE,
        AnomalyType.MAIN_THREAD_STALL,
        AnomalyType.AUDIO_PLAYER_ERROR,
        AnomalyType.AUDIO_DECODE_ERROR,
        AnomalyType.AUDIO_LOAD_TIMEOUT,
        AnomalyType.AUDIO_FOCUS_ERROR -> AnomalyLevel.MEDIUM

        // === LOW ===
        // AUDIO_BUFFER_UNDERRUN 降级为 LOW：欠载多为瞬时抖动或减速时的持续卡顿，
        // 通过 MusicService 轮询 XRun 计数增长实现持续捕获（带节流），无需 MEDIUM 级系统通知打扰用户
        AnomalyType.TRIM_MEMORY_MODERATE,
        AnomalyType.MEMORY_WARNING,
        AnomalyType.MAIN_THREAD_JANK,
        AnomalyType.NETWORK_TIMEOUT,
        AnomalyType.NETWORK_ERROR,
        AnomalyType.IO_ERROR,
        AnomalyType.CRYPTO_ERROR,
        AnomalyType.AUDIO_VISUALIZATION_ERROR,
        AnomalyType.AUDIO_BUFFER_UNDERRUN,
        AnomalyType.RENDER_GL_MEMORY_ANOMALY -> AnomalyLevel.LOW
    }

    /**
     * 分级上下文（未来扩展用，如连续次数）
     */
    data class ClassifyContext(
        val consecutiveCount: Int = 1,
        val extra: Map<String, Any> = emptyMap()
    )
}
