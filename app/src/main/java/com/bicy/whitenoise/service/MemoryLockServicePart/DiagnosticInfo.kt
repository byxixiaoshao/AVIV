package com.bicy.whitenoise.service.MemoryLockServicePart

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
