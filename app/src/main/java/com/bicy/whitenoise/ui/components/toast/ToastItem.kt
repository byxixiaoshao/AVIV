package com.bicy.whitenoise.ui.components.toast

enum class ToastType { INFO, SUCCESS, WARNING, ERROR, LOADING }

/**
 * Toast 优先级。
 * - NORMAL：按添加顺序显示，自动消失（按 duration）。
 * - CRITICAL：插入到列表顶部，常驻显示（不自动消失），仅手动消除；
 *   视觉上有红色左边框等突出样式。
 */
enum class ToastPriority { NORMAL, CRITICAL }

/**
 * 单条 toast 数据，不可变，由 ToastManager 创建。
 *
 * @param id        唯一 ID（UUID）
 * @param type      类型（INFO/SUCCESS/WARNING/ERROR/LOADING）
 * @param message   显示文字
 * @param progress  LOADING 时的进度 0f..1f，null=不确定进度旋转
 * @param onCancel  LOADING 时「取消」按钮回调
 * @param onRetry   ERROR/WARNING 时「重试」按钮回调（可选）
 * @param priority  优先级，CRITICAL 会置顶且常驻
 * @param durationMs 自定义持续时间（毫秒），null=使用全局配置；CRITICAL 强制常驻（忽略此值）
 */
data class ToastItem(
    val id: String,
    val type: ToastType,
    val message: String,
    val progress: Float? = null,
    val onCancel: (() -> Unit)? = null,
    val onRetry: (() -> Unit)? = null,
    val priority: ToastPriority = ToastPriority.NORMAL,
    val durationMs: Long? = null
)
