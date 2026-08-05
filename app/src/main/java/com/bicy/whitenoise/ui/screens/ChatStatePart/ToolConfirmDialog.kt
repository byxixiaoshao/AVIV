package com.bicy.whitenoise.ui.screens.ChatStatePart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bicy.whitenoise.ui.viewmodel.PendingConfirmation

/**
 * 工具调用确认弹窗（confirmMode 开启时，修改类工具调用前弹出）
 * 任务A:
 * - 单工具：显示工具名+描述+可编辑参数，用户可修改参数后确认
 * - 多工具：显示工具列表（工具名+描述），不提供参数编辑，只能全部确认或全部拒绝
 * - 读取类工具不触发此弹窗（在 AIService 中直接执行）
 */
@Composable
fun ToolConfirmDialog(
    pending: PendingConfirmation,
    onConfirm: (modifiedArgs: String?) -> Unit,
    onReject: (reason: String) -> Unit
) {
    val firstTool = pending.tools.firstOrNull()
    var argsText by remember(pending) { mutableStateOf(firstTool?.arguments ?: "") }
    var rejectMode by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { /* 不允许外部点击关闭，必须明确确认或拒绝 */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 标题
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(20.dp)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = if (pending.isSingle) "工具调用确认" else "工具调用确认（${pending.tools.size} 个工具）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (pending.isSingle && firstTool != null) {
                    // ── 单工具模式：显示详细信息 + 参数编辑 ──
                    SingleToolContent(
                        toolName = firstTool.toolName,
                        description = firstTool.description,
                        argsText = argsText,
                        onArgsChange = { argsText = it },
                        rejectMode = rejectMode,
                        rejectReason = rejectReason,
                        onRejectReasonChange = { rejectReason = it },
                        originalArgs = firstTool.arguments,
                        onConfirm = { modified -> onConfirm(modified) },
                        onReject = { reason -> onReject(reason) },
                        onEnterRejectMode = { rejectMode = true },
                        onExitRejectMode = { rejectMode = false }
                    )
                } else {
                    // ── 多工具模式：显示工具列表，不支持参数编辑 ──
                    MultiToolContent(
                        tools = pending.tools,
                        rejectMode = rejectMode,
                        rejectReason = rejectReason,
                        onRejectReasonChange = { rejectReason = it },
                        onConfirm = { onConfirm(null) },
                        onReject = { reason -> onReject(reason) },
                        onEnterRejectMode = { rejectMode = true },
                        onExitRejectMode = { rejectMode = false }
                    )
                }
            }
        }
    }
}

/**
 * 单工具内容：工具名 + 描述 + 可编辑参数
 */
@Composable
private fun SingleToolContent(
    toolName: String,
    description: String,
    argsText: String,
    onArgsChange: (String) -> Unit,
    rejectMode: Boolean,
    rejectReason: String,
    onRejectReasonChange: (String) -> Unit,
    originalArgs: String,
    onConfirm: (modifiedArgs: String?) -> Unit,
    onReject: (reason: String) -> Unit,
    onEnterRejectMode: () -> Unit,
    onExitRejectMode: () -> Unit
) {
    // 工具名
    Text(
        text = "工具：$toolName",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    // 工具描述
    if (description.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 3
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    Spacer(modifier = Modifier.height(12.dp))

    if (!rejectMode) {
        // 参数编辑
        Text(
            text = "参数（可修改）：",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = argsText,
            onValueChange = onArgsChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 240.dp)
                .verticalScroll(rememberScrollState()),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 操作按钮
        ConfirmRejectButtons(
            onRejectClick = onEnterRejectMode,
            onConfirmClick = {
                val modified = if (argsText.trim() == originalArgs.trim()) null else argsText
                onConfirm(modified)
            }
        )
    } else {
        RejectReasonInput(
            reason = rejectReason,
            onReasonChange = onRejectReasonChange,
            onBack = onExitRejectMode,
            onConfirmReject = { onReject(rejectReason.ifBlank { "用户拒绝执行" }) }
        )
    }
}

/**
 * 多工具内容：工具列表（工具名+描述），不支持参数编辑
 */
@Composable
private fun MultiToolContent(
    tools: List<com.bicy.whitenoise.ui.viewmodel.PendingToolItem>,
    rejectMode: Boolean,
    rejectReason: String,
    onRejectReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onReject: (reason: String) -> Unit,
    onEnterRejectMode: () -> Unit,
    onExitRejectMode: () -> Unit
) {
    Text(
        text = "将一次性执行以下工具：",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 工具列表（纵向滚动，防止工具过多溢出）
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .verticalScroll(rememberScrollState())
    ) {
        tools.forEachIndexed { index, tool ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = "${index + 1}. ${tool.toolName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (tool.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    Spacer(modifier = Modifier.height(12.dp))

    if (!rejectMode) {
        Text(
            text = "多工具批量执行，不支持参数修改。确认将执行全部工具。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        ConfirmRejectButtons(
            onRejectClick = onEnterRejectMode,
            onConfirmClick = onConfirm
        )
    } else {
        RejectReasonInput(
            reason = rejectReason,
            onReasonChange = onRejectReasonChange,
            onBack = onExitRejectMode,
            onConfirmReject = { onReject(rejectReason.ifBlank { "用户拒绝执行" }) }
        )
    }
}

/**
 * 确认/拒绝按钮组合
 */
@Composable
private fun ConfirmRejectButtons(
    onRejectClick: () -> Unit,
    onConfirmClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onRejectClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("拒绝")
        }
        Button(
            onClick = onConfirmClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("确认执行")
        }
    }
}

/**
 * 拒绝原因输入
 */
@Composable
private fun RejectReasonInput(
    reason: String,
    onReasonChange: (String) -> Unit,
    onBack: () -> Unit,
    onConfirmReject: () -> Unit
) {
    Text(
        text = "拒绝原因（AI 将据此重试）：",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(
        value = reason,
        onValueChange = onReasonChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("如：速度太快，请降低到 1.5x") },
        shape = RoundedCornerShape(8.dp),
        singleLine = false,
        minLines = 2
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f)
        ) {
            Text("返回")
        }
        Button(
            onClick = onConfirmReject,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("确认拒绝")
        }
    }
}
