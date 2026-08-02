package com.bicy.whitenoise.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bicy.whitenoise.R
import com.bicy.whitenoise.ui.theme.ThemeColorManager

/** Tooltip 锚定位置：始终与高亮目标分处屏幕两侧，避免遮挡 */
enum class TooltipPosition { TOP, BOTTOM, CENTER }

/**
 * 教程提示浮层（位置固定，不跟随高亮像素移动）
 *
 * 同时充当触摸拦截层：
 * - 全屏透明 Box 消费所有指针事件，阻断用户与底层 UI 交互
 * - 卡片提供"下一步 / 完成 / 跳过"按钮，是教程期间唯一可点击区域
 *
 * @param position 卡片锚定位置：目标在屏幕上半部时用 BOTTOM，下半部时用 TOP，无目标时用 CENTER
 */
@Composable
fun TutorialTooltip(
    step: TutorialStep,
    currentStep: Int,
    totalSteps: Int,
    position: TooltipPosition,
    onNextClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()

    val alignment = when (position) {
        TooltipPosition.TOP -> Alignment.TopCenter
        TooltipPosition.BOTTOM -> Alignment.BottomCenter
        TooltipPosition.CENTER -> Alignment.Center
    }
    val cardPadding = when (position) {
        TooltipPosition.TOP -> PaddingValues(horizontal = 16.dp, vertical = 24.dp)
        TooltipPosition.BOTTOM -> PaddingValues(horizontal = 16.dp, vertical = 24.dp)
        TooltipPosition.CENTER -> PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 消费所有指针事件（点击/拖动/滚动），阻断对底层 UI 的交互
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 标题行 + 跳过按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColor.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (step.showSkipButton) {
                        IconButton(
                            onClick = onSkipClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )

                Spacer(Modifier.height(18.dp))

                // 步骤指示器 + 下一步按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(totalSteps) { idx ->
                            Box(
                                modifier = Modifier
                                    .size(if (idx == currentStep) 10.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (idx == currentStep) themeColor.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    )
                            )
                        }
                    }

                    Button(
                        onClick = onNextClick,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor.primary),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (currentStep < totalSteps - 1)
                                stringResource(R.string.next_step)
                            else
                                stringResource(R.string.confirm),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
