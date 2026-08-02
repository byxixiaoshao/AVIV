package com.bicy.whitenoise.ui.tutorial

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * 为 UI 元素注册教程目标位置
 *
 * 使用 positionInRoot() + size 而非 boundsInWindow()，
 * 确保坐标与 Canvas DrawScope 坐标系一致（均基于 Compose root）。
 */
fun Modifier.tutorialTarget(
    key: String,
    enabled: Boolean = true
): Modifier = this.then(
    if (enabled) {
        Modifier.onGloballyPositioned { coordinates ->
            val rootPos = coordinates.positionInRoot()
            val sz = coordinates.size
            TutorialManager.registerTargetPosition(
                key = key,
                position = TutorialTargetPosition(
                    left = rootPos.x,
                    top = rootPos.y,
                    right = rootPos.x + sz.width,
                    bottom = rootPos.y + sz.height
                )
            )
        }
    } else {
        Modifier
    }
)
