package com.bicy.whitenoise.floatingpet

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.ui.screens.createFloatingWindowView
import kotlin.math.roundToInt

enum class PetState {
    IDLE,
    MOVING,
    HIDING,
    HIDDEN
}

class FloatingPetService private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "FloatingPetService"
        private const val HIDE_RATIO = 0.5f
        private const val HIDE_ANIM_STEPS = 15
        private const val HIDE_ANIM_DURATION = 300L
        // hide() 时将视图移出屏幕的坐标，配合 View.INVISIBLE 实现保留视图的隐藏
        private const val HIDE_OFFSCREEN_POS = -10000

        @Volatile
        private var instance: FloatingPetService? = null

        fun getInstance(context: Context): FloatingPetService {
            return instance ?: synchronized(this) {
                instance ?: FloatingPetService(context.applicationContext).also { instance = it }
            }
        }

        fun start(context: Context) {
            val intent = Intent(context, FloatingPetService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            getInstance(context).hide()
        }
    }

    // ==================== 核心组件 ====================
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null

    // ==================== 宠物视图 ====================
    private var floatingView: ImageView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // ==================== 配置 ====================
    private var currentPetId: String = "Bicy"
    private var currentAnimationKey: String = "idle"
    private var currentFrameIndex: Int = 0
    private var currentFrames: List<Bitmap>? = null
    private var preScaledFrames: List<Bitmap>? = null  // 预缩放缓存
    private var scaledCacheScale: Float = 1.0f
    private var currentScale: Float = 1.0f
    private var antiAlias: Boolean = false
    private var normalAlpha: Float = 1.0f
    private var hiddenAlpha: Float = 0.6f

    // ==================== 坐标系统 ====================
    // 屏幕完整尺寸
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // 系统栏 insets（仅供参考，不限制拖动范围）
    private var statusBarHeight: Int = 0
    private var navBarHeight: Int = 0

    // 拖动边界 = 全屏（允许宠物到达绝对物理顶部）
    // FLAG_LAYOUT_IN_SCREEN 已切换为全屏坐标系，旋转时系统自动调对方向
    private var safeLeft: Int = 0
    private var safeTop: Int = 0
    private var safeRight: Int = 0
    private var safeBottom: Int = 0

    // 归一化坐标 (0.0~1.0，相对于安全区域)
    private var xFraction: Float = 0.5f
    private var yFraction: Float = 0.5f

    // 当前实际像素中心坐标（从分数派生）
    private var currentCenterX: Int = 0
    private var currentCenterY: Int = 0

    // ==================== 状态 ====================
    private var petState: PetState = PetState.IDLE
    private var isRunning = false
    private var isPaused = false

    private var hideRunnable: Runnable? = null
    private var lastHideEdge: String = "bottom"

    // ==================== 悬浮窗口 ====================
    private var floatingWindowView: View? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null
    private var windowAnimator: ValueAnimator? = null
    private var isWindowShowing = false
    private var isWindowHiding = false
    private var windowCleanup: (() -> Unit)? = null

    // ==================== 配置变更监听 ====================
    private var configCallback: ComponentCallbacks? = null

    // ==================== 帧动画 ====================
    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!isRunning || isPaused) return

            val frames = preScaledFrames
            if (frames.isNullOrEmpty()) return

            currentFrameIndex = (currentFrameIndex + 1) % frames.size
            updateFrame()

            val cfg = SpriteFrameLoader.loadConfig(appContext, currentPetId)
            val animConfig = cfg?.animations?.get(currentAnimationKey)
            val duration = (animConfig?.speed ?: 1.5f) * 1000L / frames.size

            handler.postDelayed(this, duration.toLong())
        }
    }

    // ==================== 初始化 ====================

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        // 唤醒路径：已运行且视图仍附着（soft-hidden），恢复显示而非重新创建
        if (isRunning && floatingView?.parent != null) {
            if (petState == PetState.HIDDEN) {
                wakeUpFromHidden()
            } else {
                Log.d(TAG, "Already showing")
            }
            return
        }

        currentPetId = ConfigStorage.getFloatingPetId()
        currentScale = ConfigStorage.getFloatingPetScale()
        antiAlias = ConfigStorage.isFloatingPetAntiAlias()
        normalAlpha = ConfigStorage.getFloatingPetAlpha()
        hiddenAlpha = ConfigStorage.getFloatingPetHiddenAlpha()

        windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val cfg = SpriteFrameLoader.loadConfig(appContext, currentPetId)
        if (cfg == null) {
            Log.e(TAG, "Failed to load config: $currentPetId")
            return
        }

        val baseSize = cfg.width
        val scaledSize = (baseSize * currentScale).toInt()
        val wm = windowManager!!

        // === 计算安全区域 ===
        calculateScreenBounds(wm)

        // 从归一化坐标恢复实际位置
        currentCenterX = xFractionToPixel(xFraction)
        currentCenterY = yFractionToPixel(yFraction)

        // clamp 到安全区域
        val halfSize = scaledSize / 2
        currentCenterX = currentCenterX.coerceIn(safeLeft + halfSize, safeRight - halfSize)
        currentCenterY = currentCenterY.coerceIn(safeTop + halfSize, safeBottom - halfSize)

        // 注册配置变更监听
        registerConfigurationCallback()

        val lp = WindowManager.LayoutParams(
            scaledSize,
            scaledSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.LEFT
        lp.x = currentCenterX - halfSize
        lp.y = currentCenterY - halfSize
        lp.alpha = normalAlpha
        layoutParams = lp

        floatingView = ImageView(appContext).apply {
            setLayerType(if (antiAlias) View.LAYER_TYPE_SOFTWARE else View.LAYER_TYPE_HARDWARE, null)

            // 监听 View 被系统意外移除（长时间后台后 Window Token 失效），自动恢复
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {}
                override fun onViewDetachedFromWindow(v: View) {
                    if (isRunning) {
                        Log.w(TAG, "FloatingView detached (state=$petState), re-adding...")
                        handler.postDelayed({
                            if (isRunning && floatingView?.parent == null) {
                                try {
                                    val lp = this@FloatingPetService.layoutParams
                                    val view = floatingView
                                    if (lp != null && view != null) {
                                        windowManager?.addView(view, lp)
                                        // 保留隐藏视觉状态：soft-hidden 时 INVISIBLE + 屏幕外，
                                        // timeout-hidden 时 VISIBLE + 边缘 + hiddenAlpha
                                        lp.alpha = when {
                                            petState == PetState.HIDDEN &&
                                                view.visibility == View.INVISIBLE -> 0f
                                            petState == PetState.HIDDEN -> hiddenAlpha
                                            else -> normalAlpha
                                        }
                                        windowManager?.updateViewLayout(view, lp)
                                    }
                                    Log.d(TAG, "FloatingView re-added after detach")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to re-add FloatingView", e)
                                }
                            }
                        }, 500)
                    }
                }
            })

            setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isDragging = false

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            if (petState == PetState.HIDDEN) {
                                // 从边缘隐藏状态唤醒：先将中心拉回安全区域，
                                // 避免 currentCenterX/Y 在屏幕外导致首帧 MOVE 被 clamp 而跳跃
                                val halfSz = lp.width / 2
                                currentCenterX = currentCenterX.coerceIn(
                                    safeLeft + halfSz, safeRight - halfSz)
                                currentCenterY = currentCenterY.coerceIn(
                                    safeTop + halfSz, safeBottom - halfSz)
                                lp.x = currentCenterX - halfSz
                                lp.y = currentCenterY - halfSz
                                lp.alpha = normalAlpha
                                try { wm.updateViewLayout(v, lp) } catch (_: Exception) {}
                                setPetState(PetState.MOVING)
                            }
                            // 记录初始窗口中心与触摸点，MOVE 时用增量更新避免绝对坐标跳变
                            initialX = currentCenterX
                            initialY = currentCenterY
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            cancelHideTimer()
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()

                            if (!isDragging && (Math.abs(dx) > 10 || Math.abs(dy) > 10)) {
                                isDragging = true
                                setPetState(PetState.MOVING)
                            }

                            if (isDragging) {
                                val newCx = initialX + dx
                                val newCy = initialY + dy
                                val halfSz = lp.width / 2

                                // 限制在安全区域内
                                currentCenterX = newCx.coerceIn(safeLeft + halfSz, safeRight - halfSz)
                                currentCenterY = newCy.coerceIn(safeTop + halfSz, safeBottom - halfSz)

                                lp.x = currentCenterX - halfSz
                                lp.y = currentCenterY - halfSz

                                try {
                                    wm.updateViewLayout(v, lp)
                                } catch (e: Exception) {
                                    // Window Token 失效 → 重试添加
                                    try { wm.removeView(v) } catch (_: Exception) {}
                                    try { wm.addView(v, lp) } catch (_: Exception) {}
                                }

                                // 更新归一化坐标
                                xFraction = pixelToXFraction(currentCenterX)
                                yFraction = pixelToYFraction(currentCenterY)
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            val wasHidden = petState == PetState.HIDDEN ||
                                    petState == PetState.MOVING && !isDragging
                            if (isDragging) {
                                isDragging = false
                                lastHideEdge = findNearestEdge(currentCenterX, currentCenterY)
                                setPetState(PetState.IDLE)
                                startHideTimer()
                            } else {
                                // 延迟执行避免竞态：触摸事件处理中 addView 新窗口
                                // 会导致系统投递 ACTION_OUTSIDE，窗口被立即关闭
                                handler.post { toggleWindow() }
                                if (wasHidden) {
                                    setPetState(PetState.HIDDEN)
                                }
                            }
                            return true
                        }
                    }
                    return false
                }
            })
        }

        loadAnimation("idle")
        setPetState(PetState.IDLE)

        windowManager?.addView(floatingView, layoutParams)
        isRunning = true

        startAnimation()
        startHideTimer()
        Log.d(TAG, "Floating pet shown: $currentPetId scale=$currentScale " +
                "safeTop=$safeTop safeBottom=$safeBottom position=($currentCenterX,$currentCenterY)")
    }

    /**
     * 从 HIDDEN（soft-hidden）状态唤醒：恢复可见性、位置，并切换到 IDLE。
     * currentCenterX/Y 在 hide() 时未被覆盖，保留了最后有效的中心坐标。
     */
    private fun wakeUpFromHidden() {
        val lp = layoutParams ?: return
        val view = floatingView ?: return
        val wm = windowManager ?: return

        // 屏幕可能已旋转，重新计算安全区域
        calculateScreenBounds(wm)

        val halfSize = lp.width / 2
        // 从保存的中心坐标恢复（hide() 时 currentCenterX/Y 未被修改）
        currentCenterX = currentCenterX.coerceIn(safeLeft + halfSize, safeRight - halfSize)
        currentCenterY = currentCenterY.coerceIn(safeTop + halfSize, safeBottom - halfSize)
        lp.x = currentCenterX - halfSize
        lp.y = currentCenterY - halfSize
        lp.alpha = normalAlpha

        view.visibility = View.VISIBLE
        try {
            wm.updateViewLayout(view, lp)
        } catch (e: Exception) {
            // Token 失效：重新添加
            try { wm.removeView(view) } catch (_: Exception) {}
            try { wm.addView(view, lp) } catch (_: Exception) {}
        }

        setPetState(PetState.IDLE)
        startAnimation()
        startHideTimer()
        Log.d(TAG, "Floating pet woken up from HIDDEN at ($currentCenterX,$currentCenterY)")
    }

    // ==================== 安全区域计算 ====================

    private fun calculateScreenBounds(wm: WindowManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = wm.currentWindowMetrics
            val bounds = metrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()

            val insets = metrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            statusBarHeight = insets.top
            navBarHeight = insets.bottom
        } else {
            @Suppress("DEPRECATION")
            val display: android.view.Display = wm.defaultDisplay
            val realSize = Point()
            @Suppress("DEPRECATION")
            display.getRealSize(realSize)
            screenWidth = realSize.x
            screenHeight = realSize.y

            statusBarHeight = getStatusBarHeight()
            navBarHeight = getNavigationBarHeight()
        }

        // 拖动边界 = 全屏（FLAG_LAYOUT_IN_SCREEN 保证旋转时 (0,0) 始终在物理顶角）
        safeLeft = 0
        safeTop = 0
        safeRight = screenWidth
        safeBottom = screenHeight

        Log.d(TAG, "Screen bounds: ${screenWidth}x${screenHeight}, " +
                "statusBar=$statusBarHeight navBar=$navBarHeight")
    }

    private fun getStatusBarHeight(): Int {
        val resId = appContext.resources.getIdentifier(
            "status_bar_height", "dimen", "android"
        )
        return if (resId > 0) appContext.resources.getDimensionPixelSize(resId) else 0
    }

    private fun getNavigationBarHeight(): Int {
        val resId = appContext.resources.getIdentifier(
            "navigation_bar_height", "dimen", "android"
        )
        return if (resId > 0) appContext.resources.getDimensionPixelSize(resId) else 0
    }

    // ==================== 归一化坐标 ====================

    private fun xFractionToPixel(fraction: Float): Int {
        return (fraction * (safeRight - safeLeft) + safeLeft).roundToInt()
    }

    private fun yFractionToPixel(fraction: Float): Int {
        return (fraction * (safeBottom - safeTop) + safeTop).roundToInt()
    }

    private fun pixelToXFraction(pixel: Int): Float {
        val range = safeRight - safeLeft
        if (range <= 0) return 0.5f
        return ((pixel - safeLeft).toFloat() / range).coerceIn(0f, 1f)
    }

    private fun pixelToYFraction(pixel: Int): Float {
        val range = safeBottom - safeTop
        if (range <= 0) return 0.5f
        return ((pixel - safeTop).toFloat() / range).coerceIn(0f, 1f)
    }

    // ==================== 配置变更 ====================

    private fun registerConfigurationCallback() {
        configCallback = object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                Log.d(TAG, "Configuration changed: orientation=${newConfig.orientation}")
                handler.post { onScreenConfigurationChanged() }
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onLowMemory() {}
        }
        appContext.registerComponentCallbacks(configCallback!!)
    }

    private fun onScreenConfigurationChanged() {
        val wm = windowManager ?: return
        val lp = layoutParams ?: return
        val view = floatingView ?: return

        calculateScreenBounds(wm)

        val cfg = SpriteFrameLoader.loadConfig(appContext, currentPetId) ?: return
        val scaledSize = (cfg.width * currentScale).toInt()
        val halfSize = scaledSize / 2

        // 从归一化坐标恢复（系统保证回调时 Configuration 已更新）
        currentCenterX = xFractionToPixel(xFraction).coerceIn(safeLeft + halfSize, safeRight - halfSize)
        currentCenterY = yFractionToPixel(yFraction).coerceIn(safeTop + halfSize, safeBottom - halfSize)

        lp.x = currentCenterX - halfSize
        lp.y = currentCenterY - halfSize

        try {
            wm.updateViewLayout(view, lp)
        } catch (e: Exception) {
            // Token 失效：移除后重新添加
            try { wm.removeView(view) } catch (_: Exception) {}
            try { wm.addView(view, lp) } catch (_: Exception) {}
        }

        // 更新悬浮窗口位置
        updateWindowPositionOnConfigurationChange()

        // HIDDEN 状态：区分 soft-hidden（hide() 触发，视图 INVISIBLE 在屏幕外）
        // 与 timeout-hidden（超时贴边，视图 VISIBLE 在边缘），分别重算位置
        if (petState == PetState.HIDDEN) {
            if (view.visibility == View.INVISIBLE) {
                // soft-hidden：currentCenterX/Y 已从分数恢复为有效坐标，
                // 视图保持在屏幕外，不更新 lp.x/y（仍为 HIDE_OFFSCREEN_POS）
                lp.x = HIDE_OFFSCREEN_POS
                lp.y = HIDE_OFFSCREEN_POS
            } else {
                // timeout-hidden：重新计算贴边位置
                val (targetCx, targetCy) = getTargetPosition()
                currentCenterX = targetCx
                currentCenterY = targetCy
                lp.x = currentCenterX - halfSize
                lp.y = currentCenterY - halfSize
            }
            try { wm.updateViewLayout(view, lp) } catch (_: Exception) {}
            xFraction = pixelToXFraction(currentCenterX)
            yFraction = pixelToYFraction(currentCenterY)
        }

        Log.d(TAG, "Configuration change applied: " +
                "screen=${screenWidth}x${screenHeight} " +
                "safe=($safeTop,$safeLeft)-($safeBottom,$safeRight) " +
                "position=($currentCenterX,$currentCenterY) " +
                "fractions=(${String.format("%.3f", xFraction)},${String.format("%.3f", yFraction)})")
    }

    private fun updateWindowPositionOnConfigurationChange() {
        val windowView = floatingWindowView ?: return
        val windowLp = windowLayoutParams ?: return
        val wm = windowManager ?: return

        val windowWidth = 280.dpToPx()
        val (petCx, petCy) = getCurrentCenter()

        var wx = petCx - windowWidth / 2
        var wy = petCy + 60.dpToPx()

        val margin = 8.dpToPx()
        if (wx < margin) wx = margin
        if (wx + windowWidth > screenWidth - margin) {
            wx = screenWidth - windowWidth - margin
        }

        if (wy + 300.dpToPx() > screenHeight - margin) {
            wy = petCy - 320.dpToPx()
            if (wy < margin) wy = margin
        }

        windowLp.x = wx
        windowLp.y = wy
        try {
            wm.updateViewLayout(windowView, windowLp)
        } catch (_: Exception) {}
    }

    // ==================== 显示/隐藏 ====================

    /**
     * 软隐藏：保留视图（不移除），移到屏幕外并设为 INVISIBLE。
     * 维护 PetState.HIDDEN，可通过 show() 唤醒。
     * 保留 configCallback 以处理屏幕旋转；保留 isRunning=true 以便唤醒路径识别。
     */
    fun hide() {
        if (!isRunning) return

        // 关闭悬浮窗口
        if (isWindowShowing) {
            windowAnimator?.cancel()
            floatingWindowView?.let { view ->
                if (view.parent != null) {
                    try { windowManager?.removeView(view) } catch (_: Exception) {}
                }
            }
            windowCleanup?.invoke()
            windowCleanup = null
            floatingWindowView = null
            windowLayoutParams = null
            isWindowShowing = false
            isWindowHiding = false
        }

        cancelHideTimer()
        stopAnimation()

        // 保留视图：移到屏幕外 + INVISIBLE，而非 removeView
        val lp = layoutParams
        val view = floatingView
        if (lp != null && view != null) {
            // currentCenterX/Y 不更新，保留最后有效中心坐标供 wakeUpFromHidden() 恢复
            lp.alpha = 0f
            lp.x = HIDE_OFFSCREEN_POS
            lp.y = HIDE_OFFSCREEN_POS
            view.visibility = View.INVISIBLE
            try { windowManager?.updateViewLayout(view, lp) } catch (_: Exception) {}
        }

        // 直接设置状态，不触发 setPetState 的动画/alpha 副作用（视图已 INVISIBLE）
        petState = PetState.HIDDEN
        Log.d(TAG, "Floating pet hidden (soft, view retained)")
    }

    /**
     * 完全销毁：移除视图、释放资源。仅在应用退出/彻底停止时调用。
     * 普通隐藏请使用 hide()。
     */
    fun destroy() {
        configCallback?.let { appContext.unregisterComponentCallbacks(it) }
        configCallback = null

        if (isWindowShowing) {
            windowAnimator?.cancel()
            floatingWindowView?.let { view ->
                if (view.parent != null) {
                    try { windowManager?.removeView(view) } catch (_: Exception) {}
                }
            }
            windowCleanup?.invoke()
            windowCleanup = null
            floatingWindowView = null
            windowLayoutParams = null
            isWindowShowing = false
            isWindowHiding = false
        }

        cancelHideTimer()
        stopAnimation()

        floatingView?.let { view ->
            if (view.parent != null) {
                try { windowManager?.removeView(view) } catch (_: Exception) {}
            }
        }
        floatingView = null
        layoutParams = null
        isRunning = false
        petState = PetState.IDLE

        SpriteFrameLoader.clearCache()
        instance = null
        Log.d(TAG, "Floating pet destroyed")
    }

    // ==================== 悬浮窗口 ====================

    private fun toggleWindow() {
        if (isWindowShowing || isWindowHiding) {
            hideWindow()
        } else {
            showWindow()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showWindow() {
        if (isWindowShowing || isWindowHiding) return

        val wm = windowManager ?: return
        val windowWidth = 280.dpToPx()

        val (petCx, petCy) = getCurrentCenter()
        val margin = 8.dpToPx()

        var wx = petCx - windowWidth / 2
        var wy = petCy + 60.dpToPx()

        if (wx < margin) wx = margin
        if (wx + windowWidth > screenWidth - margin) {
            wx = screenWidth - windowWidth - margin
        }
        if (wy + 300.dpToPx() > screenHeight - margin) {
            wy = petCy - 320.dpToPx()
            if (wy < margin) wy = margin
        }

        val lp = WindowManager.LayoutParams(
            windowWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.LEFT
        lp.x = wx
        lp.y = wy
        windowLayoutParams = lp

        val (composeView, cleanup) = createFloatingWindowView(appContext) {
            hideWindow()
        }
        windowCleanup = cleanup

        composeView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hideWindow()
                return@setOnTouchListener true
            }
            true
        }

        composeView.scaleX = 0.3f
        composeView.scaleY = 0.3f
        composeView.alpha = 0f

        floatingWindowView = composeView
        wm.addView(composeView, lp)
        isWindowShowing = true

        windowAnimator?.cancel()
        windowAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                composeView.scaleX = 0.3f + 0.7f * fraction
                composeView.scaleY = 0.3f + 0.7f * fraction
                composeView.alpha = fraction
            }
            start()
        }

        cancelHideTimer()
        Log.d(TAG, "Window shown at ($wx, $wy)")
    }

    private fun hideWindow() {
        val view = floatingWindowView ?: return
        val wm = windowManager ?: return

        if (isWindowHiding) {
            windowAnimator?.cancel()
            if (view.parent != null) {
                try { wm.removeView(view) } catch (_: Exception) {}
            }
            windowCleanup?.invoke()
            windowCleanup = null
            floatingWindowView = null
            windowLayoutParams = null
            isWindowShowing = false
            isWindowHiding = false
        }

        isWindowHiding = true

        windowAnimator?.cancel()
        windowAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 150
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                view.scaleX = 0.3f + 0.7f * fraction
                view.scaleY = 0.3f + 0.7f * fraction
                view.alpha = fraction
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                val cleanup = {
                    if (view.parent != null) {
                        try { wm.removeView(view) } catch (_: Exception) {}
                    }
                    windowCleanup?.invoke()
                    windowCleanup = null
                    floatingWindowView = null
                    windowLayoutParams = null
                    isWindowShowing = false
                    isWindowHiding = false
                    if (petState == PetState.IDLE) {
                        startHideTimer()
                    }
                }
                override fun onAnimationEnd(animation: android.animation.Animator) = cleanup()
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationCancel(animation: android.animation.Animator) = cleanup()
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            start()
        }
    }

    // ==================== 状态管理 ====================

    private fun setPetState(state: PetState) {
        if (petState == state) return

        petState = state
        Log.d(TAG, "State -> $state")

        when (state) {
            PetState.IDLE -> loadAnimation("idle")
            PetState.MOVING -> loadAnimation("move")
            PetState.HIDING -> animateToEdge()
            PetState.HIDDEN -> loadAnimation("hide_$lastHideEdge")
        }

        layoutParams?.alpha = when (state) {
            PetState.HIDDEN -> hiddenAlpha
            else -> normalAlpha
        }
        floatingView?.let { windowManager?.updateViewLayout(it, layoutParams) }

        if (isRunning) {
            stopAnimation()
            startAnimation()
        }
    }

    private fun startHideTimer() {
        cancelHideTimer()
        val delaySeconds = ConfigStorage.getFloatingPetHideDelay()
        hideRunnable = Runnable {
            if (petState == PetState.IDLE && isRunning) {
                setPetState(PetState.HIDING)
            }
        }
        handler.postDelayed(hideRunnable!!, delaySeconds * 1000L)
        Log.d(TAG, "Hide timer: ${delaySeconds}s")
    }

    private fun cancelHideTimer() {
        hideRunnable?.let {
            handler.removeCallbacks(it)
            hideRunnable = null
        }
    }

    private fun findNearestEdge(x: Int, y: Int): String {
        val distLeft = (x - safeLeft).toFloat()
        val distRight = (safeRight - x).toFloat()
        val distTop = (y - safeTop).toFloat()
        val distBottom = (safeBottom - y).toFloat()

        val minDist = minOf(distLeft, distRight, distTop, distBottom)

        return when (minDist) {
            distLeft -> "left"
            distRight -> "right"
            distTop -> "top"
            else -> "bottom"
        }
    }

    private fun getCurrentCenter(): Pair<Int, Int> = Pair(currentCenterX, currentCenterY)

    private fun getTargetPosition(): Pair<Int, Int> {
        val cfg = SpriteFrameLoader.loadConfig(appContext, currentPetId) ?: return Pair(0, 0)
        val scaledSize = (cfg.width * currentScale).toInt()
        val halfSize = scaledSize / 2

        // 使用内容区边界计算隐藏位置（仿备份行为）
        // HIDE_RATIO 部分的宠物延伸到系统栏区域，但不会超出物理屏
        val contentTop = safeTop + statusBarHeight
        val contentBottom = safeBottom - if (navBarHeight > 0) navBarHeight else halfSize

        val hideOffset = (scaledSize * (1 - HIDE_RATIO)).toInt()

        return when (lastHideEdge) {
            "left" -> Pair(
                safeLeft - hideOffset + halfSize,
                currentCenterY.coerceIn(contentTop + halfSize, contentBottom - halfSize)
            )
            "right" -> Pair(
                safeRight - scaledSize + hideOffset + halfSize,
                currentCenterY.coerceIn(contentTop + halfSize, contentBottom - halfSize)
            )
            "top" -> Pair(
                currentCenterX.coerceIn(safeLeft + halfSize, safeRight - halfSize),
                contentTop - hideOffset + halfSize
            )
            else -> Pair(
                currentCenterX.coerceIn(safeLeft + halfSize, safeRight - halfSize),
                contentBottom - scaledSize + hideOffset + halfSize
            )
        }
    }

    // ==================== 动画 ====================

    private fun animateToEdge() {
        val lp = layoutParams
        val wm = windowManager
        val view = floatingView
        if (lp == null || wm == null || view == null) return

        val (startCx, startCy) = getCurrentCenter()
        val (endCx, endCy) = getTargetPosition()

        val cfg = SpriteFrameLoader.loadConfig(appContext, currentPetId) ?: return
        val scaledSize = (cfg.width * currentScale).toInt()

        val stepCount = HIDE_ANIM_STEPS
        val stepDuration = HIDE_ANIM_DURATION / stepCount

        var step = 0

        val animRunnable = object : Runnable {
            override fun run() {
                if (!isRunning || view.parent == null) return
                // 若 hide() 在动画期间将视图 soft-hidden（INVISIBLE + 屏幕外），终止动画
                if (view.visibility == View.INVISIBLE || lp.x == HIDE_OFFSCREEN_POS) return

                step++
                val fraction = (step.toFloat() / stepCount).coerceAtMost(1.0f)

                val cx = startCx + ((endCx - startCx) * fraction).toInt()
                val cy = startCy + ((endCy - startCy) * fraction).toInt()

                currentCenterX = cx
                currentCenterY = cy

                lp.x = cx - scaledSize / 2
                lp.y = cy - scaledSize / 2
                try { wm.updateViewLayout(view, lp) } catch (_: Exception) {}

                if (step < stepCount) {
                    handler.postDelayed(this, stepDuration)
                } else {
                    setPetState(PetState.HIDDEN)
                }
            }
        }

        handler.post(animRunnable)
    }

    // ==================== 帧加载（预缩放）====================

    fun loadAnimation(animationKey: String) {
        currentAnimationKey = animationKey
        currentFrameIndex = 0

        val cfg = SpriteFrameLoader.loadConfig(appContext, currentPetId)
        val animConfig = cfg?.animations?.get(animationKey)

        if (animConfig == null) {
            Log.e(TAG, "Animation not found: $animationKey")
            return
        }

        currentFrames = SpriteFrameLoader.loadFrames(appContext, currentPetId, animationKey)
        if (currentFrames.isNullOrEmpty()) {
            Log.e(TAG, "Failed to load frames: $animationKey")
            return
        }

        preScaleFrames()
        updateFrame()
        Log.d(TAG, "Animation loaded: $animationKey (${currentFrames!!.size} frames)")
    }

    /** 加载时一次性预缩放所有帧，渲染时零计算 */
    private fun preScaleFrames() {
        val frames = currentFrames ?: return
        if (currentScale == 1.0f) {
            preScaledFrames = frames
            scaledCacheScale = 1.0f
            return
        }

        if (scaledCacheScale == currentScale && preScaledFrames != null) return

        preScaledFrames = frames.map { original ->
            val baseSize = original.width
            val scaledSize = (baseSize * currentScale).toInt()
            if (scaledSize <= 0) original
            else Bitmap.createScaledBitmap(original, scaledSize, scaledSize, antiAlias)
        }
        scaledCacheScale = currentScale
    }

    private fun updateFrame() {
        val frames = preScaledFrames
        val view = floatingView
        if (frames.isNullOrEmpty() || view == null) return

        view.setImageBitmap(frames[currentFrameIndex])
    }

    private fun startAnimation() {
        if (!isRunning) return
        handler.post(frameRunnable)
    }

    private fun stopAnimation() {
        handler.removeCallbacks(frameRunnable)
    }

    // ==================== 公开 API ====================

    fun updateConfig() {
        if (!isRunning) return

        val newPetId = ConfigStorage.getFloatingPetId()
        val newScale = ConfigStorage.getFloatingPetScale()
        val newAntiAlias = ConfigStorage.isFloatingPetAntiAlias()
        val newAlpha = ConfigStorage.getFloatingPetAlpha()
        val newHiddenAlpha = ConfigStorage.getFloatingPetHiddenAlpha()

        val needsReload = newPetId != currentPetId || newScale != currentScale || newAntiAlias != antiAlias
        val needsAlphaUpdate = newAlpha != normalAlpha || newHiddenAlpha != hiddenAlpha

        if (needsReload || needsAlphaUpdate) {
            currentPetId = newPetId
            currentScale = newScale
            antiAlias = newAntiAlias
            normalAlpha = newAlpha
            hiddenAlpha = newHiddenAlpha

            if (needsReload) {
                val cfg = SpriteFrameLoader.loadConfig(appContext, currentPetId) ?: return
                val scaledSize = (cfg.width * currentScale).toInt()
                val lp = layoutParams ?: return

                // 中心锚点缩放：保持窗口中心位置不变，对所有状态（含 HIDDEN）生效
                val oldWidth = lp.width
                val oldHeight = lp.height
                applyCenterAnchoredResize(lp, oldWidth, oldHeight, scaledSize, scaledSize)

                floatingView?.setLayerType(
                    if (antiAlias) View.LAYER_TYPE_SOFTWARE else View.LAYER_TYPE_HARDWARE, null
                )

                loadAnimation(currentAnimationKey)

                // 从调整后的 lp 反算中心坐标
                val halfSize = scaledSize / 2
                currentCenterX = lp.x + halfSize
                currentCenterY = lp.y + halfSize

                // HIDDEN（soft-hidden）时视图在屏幕外，不 clamp 以保留隐藏状态；
                // 其他状态 clamp 到安全区域
                if (petState != PetState.HIDDEN ||
                    floatingView?.visibility != View.INVISIBLE) {
                    currentCenterX = currentCenterX.coerceIn(safeLeft + halfSize, safeRight - halfSize)
                    currentCenterY = currentCenterY.coerceIn(safeTop + halfSize, safeBottom - halfSize)
                    lp.x = currentCenterX - halfSize
                    lp.y = currentCenterY - halfSize
                }

                xFraction = pixelToXFraction(currentCenterX)
                yFraction = pixelToYFraction(currentCenterY)
            }

            // soft-hidden 时视图 INVISIBLE，alpha 无实际影响；其他状态按状态设置
            layoutParams?.alpha = when {
                petState == PetState.HIDDEN &&
                    floatingView?.visibility == View.INVISIBLE -> 0f
                petState == PetState.HIDDEN -> hiddenAlpha
                else -> normalAlpha
            }

            floatingView?.let { windowManager?.updateViewLayout(it, layoutParams) }
            Log.d(TAG, "Config updated: pet=$currentPetId scale=$currentScale")
        }
    }

    fun pauseAnimation() {
        isPaused = true
    }

    fun resumeAnimation() {
        if (isPaused) {
            isPaused = false
            startAnimation()
        }
    }

    fun setAnimation(animationKey: String) {
        loadAnimation(animationKey)
        if (isRunning) {
            stopAnimation()
            startAnimation()
        }
    }

    fun isShowing(): Boolean = isRunning &&
            floatingView?.parent != null &&
            petState != PetState.HIDDEN

    // ==================== 工具 ====================

    private fun Int.dpToPx(): Int {
        return (this * appContext.resources.displayMetrics.density).toInt()
    }

    /**
     * 中心锚点缩放：调整 lp 的 width/height，同时平移 x/y 以保持窗口中心位置不变。
     * 对所有状态（含 HIDDEN）生效，避免缩放时中心偏移、大面积留空。
     */
    private fun applyCenterAnchoredResize(
        lp: WindowManager.LayoutParams,
        oldWidth: Int, oldHeight: Int,
        newWidth: Int, newHeight: Int
    ) {
        lp.width = newWidth
        lp.height = newHeight
        lp.x -= (newWidth - oldWidth) / 2
        lp.y -= (newHeight - oldHeight) / 2
    }
}
