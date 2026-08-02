package com.bicy.whitenoise.ui.components.toast

import android.os.Handler
import android.os.Looper
import com.bicy.whitenoise.storage.config.ConfigStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * 全局 toast 管理器，单例。
 *
 * 调用示例：
 *   ToastManager.info("正在获取音乐...")
 *   ToastManager.loading("下载中...", onCancel = { downloadJob.cancel() })
 *   ToastManager.updateProgress(0.45f, "下载中: 45%")
 *   ToastManager.complete("下载完成")
 *   ToastManager.error("获取失败")
 *   ToastManager.warning("网络不稳定")
 *   // CRITICAL 用法（置顶常驻，仅手动消除）：
 *   ToastManager.error("严重错误", priority = ToastPriority.CRITICAL, onRetry = { retry() })
 *
 * ## 排队规则（最多 3 条可见）
 *   - < 3 条：直接追加（CRITICAL 插入到顶部，NORMAL 插入到所有 CRITICAL 之前）
 *   - = 3 条：优先替换最早的非 LOADING NORMAL toast；CRITICAL 不会被普通 toast 顶掉
 *   - 全部 LOADING：替换最早的 LOADING
 *
 * ## 优先级
 *   - NORMAL：按添加顺序显示，到时自动消失
 *   - CRITICAL：插入到列表顶部，常驻不自动消失，仅手动 removeById 触发
 *
 * ## 线程安全
 *   所有公开方法通过 mainHandler post 到主线程执行，
 *   确保 StateFlow 更新和 UI 重组在同线程。
 */
object ToastManager {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _toasts = MutableStateFlow<List<ToastItem>>(emptyList())
    val toasts: StateFlow<List<ToastItem>> = _toasts.asStateFlow()

    /** 当前 active 的 loading toast ID，用于 updateProgress/complete */
    private var activeLoadingId: String? = null

    /** 通知持续时间（秒），来自设置 */
    fun getDurationSeconds(): Int = ConfigStorage.getToastDurationSeconds()

    // ─── 公开 API ──────────────────────────────────────

    fun info(message: String, priority: ToastPriority = ToastPriority.NORMAL) =
        show(ToastType.INFO, message, priority = priority)

    fun success(message: String, priority: ToastPriority = ToastPriority.NORMAL) =
        show(ToastType.SUCCESS, message, priority = priority)

    fun error(
        message: String,
        priority: ToastPriority = ToastPriority.NORMAL,
        onRetry: (() -> Unit)? = null
    ) = show(ToastType.ERROR, message, priority = priority, onRetry = onRetry)

    /** 警告提示（橙色图标），可附带「重试」按钮 */
    fun warning(
        message: String,
        priority: ToastPriority = ToastPriority.NORMAL,
        onRetry: (() -> Unit)? = null
    ) = show(ToastType.WARNING, message, priority = priority, onRetry = onRetry)

    fun loading(
        message: String,
        progress: Float? = null,
        onCancel: (() -> Unit)? = null,
        priority: ToastPriority = ToastPriority.NORMAL
    ) {
        val item = createItem(
            ToastType.LOADING, message, progress, onCancel,
            priority = priority
        )
        activeLoadingId = item.id
        runOnMain { addOrReplace(item) }
    }

    fun updateProgress(progress: Float, message: String? = null) {
        runOnMain {
            val list = _toasts.value.toMutableList()
            val idx = list.indexOfFirst { it.id == activeLoadingId }
            if (idx >= 0) {
                val old = list[idx]
                list[idx] = old.copy(progress = progress.coerceIn(0f, 1f), message = message ?: old.message)
                _toasts.value = list
                // 如果进度到 1.0 且是外部更新的（非 complete），让 toast 宿主接管消失逻辑
                // toast 宿主检测到进度 = 1.0 且 type = LOADING → 自动转为 success
            }
        }
    }

    /**
     * 仅更新当前 LOADING toast 的文案，不改进度。
     * 用于切换音源/阶段时刷新提示（例如 "正在尝试音源 2/3: xxx"）。
     */
    fun updateMessage(message: String) {
        runOnMain {
            val list = _toasts.value.toMutableList()
            val idx = list.indexOfFirst { it.id == activeLoadingId }
            if (idx >= 0) {
                val old = list[idx]
                list[idx] = old.copy(message = message)
                _toasts.value = list
            }
        }
    }

    /**
     * 当前 LOADING toast 原地切换为 SUCCESS 样式，
     * 倒计时结束后消失（时长 = 通知持续时间）。
     */
    fun complete(message: String) {
        runOnMain {
            val list = _toasts.value.toMutableList()
            val idx = list.indexOfFirst { it.id == activeLoadingId }
            if (idx >= 0) {
                val old = list[idx]
                list[idx] = old.copy(
                    type = ToastType.SUCCESS,
                    message = message,
                    progress = null,
                    onCancel = null
                )
                _toasts.value = list
                activeLoadingId = null
            }
        }
    }

    /**
     * 当前 LOADING toast 原地切换为 ERROR 样式，
     * 倒计时结束后消失（时长 = 通知持续时间）。
     * 若没有 active loading，则降级为普通 error 提示。
     *
     * @param onRetry 切换为 ERROR 后附带的「重试」回调（可选）
     */
    fun fail(message: String, onRetry: (() -> Unit)? = null) {
        runOnMain {
            val list = _toasts.value.toMutableList()
            val idx = list.indexOfFirst { it.id == activeLoadingId }
            if (idx >= 0) {
                val old = list[idx]
                list[idx] = old.copy(
                    type = ToastType.ERROR,
                    message = message,
                    progress = null,
                    onCancel = null,
                    onRetry = onRetry
                )
                _toasts.value = list
                activeLoadingId = null
            } else {
                // 没有 active loading，直接追加 error
                addOrReplace(createItem(ToastType.ERROR, message, onRetry = onRetry))
            }
        }
    }

    fun dismiss() {
        runOnMain {
            val id = activeLoadingId ?: _toasts.value.firstOrNull()?.id ?: return@runOnMain
            removeById(id)
        }
    }

    /** 由 toast 宿主在倒计时到期后调用 */
    fun removeById(id: String) {
        runOnMain {
            val list = _toasts.value.toMutableList()
            list.removeAll { it.id == id }
            _toasts.value = list
            if (id == activeLoadingId) activeLoadingId = null
        }
    }

    // ─── 内部逻辑 ──────────────────────────────────────

    private fun show(
        type: ToastType,
        message: String,
        priority: ToastPriority = ToastPriority.NORMAL,
        onRetry: (() -> Unit)? = null
    ) {
        val item = createItem(type, message, priority = priority, onRetry = onRetry)
        runOnMain { addOrReplace(item) }
    }

    private fun createItem(
        type: ToastType, message: String,
        progress: Float? = null, onCancel: (() -> Unit)? = null,
        priority: ToastPriority = ToastPriority.NORMAL,
        onRetry: (() -> Unit)? = null
    ) = ToastItem(
        id = UUID.randomUUID().toString().take(8),
        type = type, message = message,
        progress = progress, onCancel = onCancel,
        onRetry = onRetry, priority = priority
    )

    /**
     * 入队逻辑：CRITICAL 始终插入到列表尾部（asReversed 后即顶部），NORMAL 插入到所有
     * CRITICAL 之前（即位于 CRITICAL 下方）。队列满时优先替换 NORMAL，CRITICAL 不会被
     * 普通 toast 顶掉。
     */
    private fun addOrReplace(item: ToastItem) {
        val list = _toasts.value.toMutableList()
        if (list.size < 3) {
            insertByPriority(list, item)
        } else {
            // 替换策略：优先找 NORMAL 非 LOADING → NORMAL LOADING → （仅当新条目为 CRITICAL）CRITICAL
            val victimIdx = findReplaceVictim(list, item)
            list[victimIdx] = item
            // 替换后重排，确保 CRITICAL 始终位于列表尾端（视觉顶部）
            sortByPriority(list)
        }
        _toasts.value = list
    }

    /** CRITICAL 追加到尾部（顶部），NORMAL 插入到首个 CRITICAL 之前 */
    private fun insertByPriority(list: MutableList<ToastItem>, item: ToastItem) {
        if (item.priority == ToastPriority.CRITICAL) {
            list.add(item)
        } else {
            val firstCriticalIdx = list.indexOfFirst { it.priority == ToastPriority.CRITICAL }
            if (firstCriticalIdx >= 0) list.add(firstCriticalIdx, item) else list.add(item)
        }
    }

    /** 找到可被替换的 victim 索引：NORMAL 优先于 CRITICAL，非 LOADING 优先于 LOADING */
    private fun findReplaceVictim(list: List<ToastItem>, newItem: ToastItem): Int {
        // 1. 最早的 NORMAL 非 LOADING
        val normalNonLoading = list.indexOfFirst {
            it.priority == ToastPriority.NORMAL && it.type != ToastType.LOADING
        }
        if (normalNonLoading >= 0) return normalNonLoading

        // 2. 最早的 NORMAL LOADING
        val normalLoading = list.indexOfFirst {
            it.priority == ToastPriority.NORMAL && it.type == ToastType.LOADING
        }
        if (normalLoading >= 0) return normalLoading

        // 3. 只有 CRITICAL 时，仅当新条目也是 CRITICAL 才替换最早的 CRITICAL 非 LOADING
        if (newItem.priority == ToastPriority.CRITICAL) {
            val criticalNonLoading = list.indexOfFirst {
                it.priority == ToastPriority.CRITICAL && it.type != ToastType.LOADING
            }
            if (criticalNonLoading >= 0) return criticalNonLoading

            val criticalLoading = list.indexOfFirst {
                it.priority == ToastPriority.CRITICAL && it.type == ToastType.LOADING
            }
            if (criticalLoading >= 0) return criticalLoading
        }

        // 4. 兜底：替换最早的（极少触发）
        return 0
    }

    /** 稳定排序：NORMAL 在前，CRITICAL 在后（视觉顶部） */
    private fun sortByPriority(list: MutableList<ToastItem>) {
        list.sortBy { if (it.priority == ToastPriority.CRITICAL) 1 else 0 }
    }

    private inline fun runOnMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { block() }
        }
    }
}
