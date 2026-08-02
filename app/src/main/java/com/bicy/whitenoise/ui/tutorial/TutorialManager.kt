package com.bicy.whitenoise.ui.tutorial

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 教程步骤定义
 */
data class TutorialStep(
    val targetKey: String?,
    val title: String,
    val description: String,
    val highlightShape: HighlightShape = HighlightShape.ROUNDED_RECT,
    val highlightPadding: Float = 20f,   // 大边距确保内容可见
    val showSkipButton: Boolean = true
)

enum class HighlightShape { ROUNDED_RECT, RECTANGLE, CIRCLE }

/**
 * 教程目标在屏幕上的位置（像素，window 坐标）
 * 由实际 UI 组件通过 onGloballyPositioned 注册
 */
data class TutorialTargetPosition(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX get() = (left + right) / 2f
    val centerY get() = (top + bottom) / 2f
    val width get() = right - left
    val height get() = bottom - top
}

/**
 * 教程状态管理器
 *
 * z-ordering 方案（真层级，不挖孔）：
 * - 教程期间，bar 渲染在外层 Box（跳过 GlassContainer），与遮罩同级
 * - 遮罩 zIndex=100，高亮 bar zIndex=200，Tooltip zIndex=300
 * - 嵌套目标（示例声音/保存加载按钮）：无法提升到外层，改回大边距挖孔（padding=20dp）
 */
object TutorialManager {
    const val TUTORIAL_MAIN = "main_tutorial"

    // 教程目标键
    const val KEY_TOP_BAR = "top_bar_area"
    const val KEY_BOTTOM_NAV = "bottom_nav_bar"
    const val KEY_SAMPLE_SOUND = "tutorial_sample_sound"
    const val KEY_SAVE_LOAD = "save_load_area"

    /** 白噪音播放页索引 */
    const val PAGE_PLAY = 2

    private val scope = CoroutineScope(Dispatchers.Main)

    // --- 教程流程状态 ---
    private val _currentTutorial = MutableStateFlow<String?>(null)
    val currentTutorial: StateFlow<String?> = _currentTutorial

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex

    private val _tutorialCompleted = MutableStateFlow(false)
    val tutorialCompleted: StateFlow<Boolean> = _tutorialCompleted

    // --- UI 控制信号 ---
    private val _tutorialPageIndex = MutableStateFlow(-1)
    val tutorialPageIndex: StateFlow<Int> = _tutorialPageIndex

    private val _forceCollapseTopBar = MutableStateFlow(false)
    val forceCollapseTopBar: StateFlow<Boolean> = _forceCollapseTopBar

    private val _forceCollapseNavBar = MutableStateFlow(false)
    val forceCollapseNavBar: StateFlow<Boolean> = _forceCollapseNavBar

    /** 是否需要显示示例声音 */
    private val _showTutorialSample = MutableStateFlow(false)
    val showTutorialSample: StateFlow<Boolean> = _showTutorialSample

    // --- 目标位置注册（可观察，驱动遮罩挖孔重绘）---
    private val _targetPositions = MutableStateFlow<Map<String, TutorialTargetPosition>>(emptyMap())
    val targetPositions: StateFlow<Map<String, TutorialTargetPosition>> = _targetPositions

    fun registerTargetPosition(key: String, position: TutorialTargetPosition) {
        val current = _targetPositions.value
        val existing = current[key]
        // 仅当坐标真正变化时才更新，避免相同值触发 StateFlow 去重绕过
        if (existing == null || existing != position) {
            _targetPositions.value = current + (key to position)
        }
    }

    fun getTargetPosition(key: String?): TutorialTargetPosition? =
        key?.let { _targetPositions.value[it] }

    // --- 步骤管理 ---
    private val steps: List<TutorialStep> = TutorialSteps.getSteps()

    fun getCurrentStep(): TutorialStep? = steps.getOrNull(_currentStepIndex.value)
    fun getTotalSteps(): Int = steps.size

    // --- 教程开始/结束 ---
    fun startTutorial() {
        _currentStepIndex.value = 0
        _currentTutorial.value = TUTORIAL_MAIN
        _tutorialPageIndex.value = -1
        _forceCollapseTopBar.value = false
        _forceCollapseNavBar.value = false
        _showTutorialSample.value = false
        _targetPositions.value = emptyMap()
        executeStepEnter(0)
    }

    fun nextStep() {
        val next = _currentStepIndex.value + 1
        if (next >= steps.size) {
            skipTutorial()
            return
        }
        _currentStepIndex.value = next
        _tutorialPageIndex.value = -1
        executeStepEnter(next)
    }

    fun skipTutorial() {
        _currentTutorial.value = null
        _currentStepIndex.value = 0
        _tutorialPageIndex.value = -1
        _forceCollapseTopBar.value = false
        _forceCollapseNavBar.value = false
        _showTutorialSample.value = false
        _targetPositions.value = emptyMap()
        _tutorialCompleted.value = true
        com.bicy.whitenoise.storage.config.ConfigStorage.setCompletedTutorials(listOf(TUTORIAL_MAIN))
    }

    fun resetAllTutorials() {
        com.bicy.whitenoise.storage.config.ConfigStorage.setCompletedTutorials(emptyList())
        _tutorialCompleted.value = false
    }

    fun loadCompletedState() {
        val completed = com.bicy.whitenoise.storage.config.ConfigStorage.getCompletedTutorials()
        _tutorialCompleted.value = completed.contains(TUTORIAL_MAIN)
    }

    // --- 步骤进入动作 ---
    // 0: 顶部栏  1: 底部栏  2: 示例声音(播放页)  3: 保存/加载  4: 底部导航栏  5: 结束语
    private fun executeStepEnter(stepIndex: Int) {
        when (stepIndex) {
            0 -> {
                _forceCollapseTopBar.value = true
                _forceCollapseNavBar.value = true
            }
            1 -> {
                _forceCollapseTopBar.value = true
                _forceCollapseNavBar.value = true
            }
            2 -> {
                _tutorialPageIndex.value = PAGE_PLAY
                _forceCollapseTopBar.value = true
                _forceCollapseNavBar.value = true
                scope.launch {
                    delay(600)
                    _showTutorialSample.value = true
                }
            }
            3 -> { /* 仍在播放页，通过挖孔露出保存/加载按钮 */ }
            4 -> {
                _showTutorialSample.value = false
                _tutorialPageIndex.value = PAGE_PLAY
            }
            5 -> { /* 最终消息，全屏遮罩无挖孔 */ }
        }
    }
}
