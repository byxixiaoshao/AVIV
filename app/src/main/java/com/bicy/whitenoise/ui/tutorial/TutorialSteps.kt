package com.bicy.whitenoise.ui.tutorial

import com.bicy.whitenoise.utils.AppInitializer

/**
 * 教程步骤定义
 */
object TutorialSteps {

    fun getSteps(): List<TutorialStep> {
        val ctx = AppInitializer.getContext()
        return listOf(
            // 步骤0：介绍顶部栏（音乐播放器）—— zIndex 提升
            TutorialStep(
                targetKey = TutorialManager.KEY_TOP_BAR,
                title = ctx.getString(com.bicy.whitenoise.R.string.tutorial_top_bar_title),
                description = ctx.getString(com.bicy.whitenoise.R.string.tutorial_top_bar_desc),
                highlightShape = HighlightShape.ROUNDED_RECT,
                highlightPadding = 4f
            ),
            // 步骤1：介绍底部栏（定时器）—— zIndex 提升
            TutorialStep(
                targetKey = TutorialManager.KEY_BOTTOM_NAV,
                title = ctx.getString(com.bicy.whitenoise.R.string.tutorial_bottom_bar_title),
                description = ctx.getString(com.bicy.whitenoise.R.string.tutorial_bottom_bar_desc),
                highlightShape = HighlightShape.ROUNDED_RECT,
                highlightPadding = 4f
            ),
            // 步骤2：播放页示例声音控制（齿轮/×）—— 嵌套目标，精确挖孔
            TutorialStep(
                targetKey = TutorialManager.KEY_SAMPLE_SOUND,
                title = ctx.getString(com.bicy.whitenoise.R.string.tutorial_sample_sound_title),
                description = ctx.getString(com.bicy.whitenoise.R.string.tutorial_sample_sound_desc),
                highlightShape = HighlightShape.ROUNDED_RECT,
                highlightPadding = 4f
            ),
            // 步骤3：保存/加载按钮 —— 嵌套目标，精确挖孔
            TutorialStep(
                targetKey = TutorialManager.KEY_SAVE_LOAD,
                title = ctx.getString(com.bicy.whitenoise.R.string.tutorial_save_load_title),
                description = ctx.getString(com.bicy.whitenoise.R.string.tutorial_save_load_desc),
                highlightShape = HighlightShape.ROUNDED_RECT,
                highlightPadding = 4f
            ),
            // 步骤4：底部导航栏播放控制 —— zIndex 提升
            TutorialStep(
                targetKey = TutorialManager.KEY_BOTTOM_NAV,
                title = ctx.getString(com.bicy.whitenoise.R.string.tutorial_nav_play_title),
                description = ctx.getString(com.bicy.whitenoise.R.string.tutorial_nav_play_desc),
                highlightShape = HighlightShape.ROUNDED_RECT,
                highlightPadding = 4f
            ),
            // 步骤5：结束语 —— 无高亮目标
            TutorialStep(
                targetKey = null,
                title = ctx.getString(com.bicy.whitenoise.R.string.tutorial_final_title),
                description = ctx.getString(com.bicy.whitenoise.R.string.tutorial_final_desc),
                highlightShape = HighlightShape.ROUNDED_RECT,
                highlightPadding = 0f,
                showSkipButton = false
            )
        )
    }
}
