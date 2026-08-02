package com.bicy.whitenoise.ui.components

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 自定义输入框组件 - 使用原生 EditText 确保键盘能正常触发
 *
 * 主题应用：
 * - 背景色 -> surfaceVariant
 * - 文字色 -> onSurface
 * - 提示色 -> onSurfaceVariant
 * - 边框色 -> outline（失焦）/ primary（聚焦）
 * - 形状   -> RoundedCornerShape(20.dp)，与 ExpandableNavBar 中的 OutlinedTextField 保持一致
 * - 字号   -> MaterialTheme.typography.bodyMedium
 */
@Composable
fun FocusableEditText(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    enabled: Boolean = true,
    onSearch: (() -> Unit)? = null,
    // 可选：自定义输入类型（如数字小数 InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL）
    inputType: Int? = null,
    // 可选：自定义 IME 动作（默认搜索，数值场景传 IME_ACTION_DONE）
    imeAction: Int = EditorInfo.IME_ACTION_SEARCH
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val focusedBorderColor = MaterialTheme.colorScheme.primary
    val disabledContentAlpha = 0.38f
    val textStyle = MaterialTheme.typography.bodyMedium
    val cornerRadius = 20.dp

    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) focusedBorderColor else unfocusedBorderColor
    val currentTextColor = if (enabled) textColor else textColor.copy(alpha = disabledContentAlpha)
    val currentHintColor = if (enabled) hintColor else hintColor.copy(alpha = disabledContentAlpha)
    // 捕获参数到本地 val，避免在 EditText.apply{} 内被 EditText 同名属性 inputType 遮蔽
    val requestedInputType = inputType

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                EditText(ctx).apply {
                    setBackgroundColor(Color.Transparent.toArgb())
                    setPadding(16, 12, 16, 12)
                    textSize = textStyle.fontSize.value
                    setTextColor(currentTextColor.toArgb())
                    setHintTextColor(currentHintColor.toArgb())
                    hint = placeholder
                    isSingleLine = singleLine
                    isEnabled = enabled
                    // 应用自定义输入类型（如数字小数）与 IME 动作
                    if (requestedInputType != null) this.inputType = requestedInputType
                    imeOptions = imeAction

                    setOnFocusChangeListener { _, hasFocus ->
                        isFocused = hasFocus
                    }

                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                            actionId == EditorInfo.IME_ACTION_DONE ||
                            actionId == EditorInfo.IME_ACTION_GO ||
                            actionId == EditorInfo.IME_ACTION_NEXT) {
                            onSearch?.invoke()
                            true
                        } else {
                            false
                        }
                    }
                }
            },
            update = { editText ->
                // 更新文本（避免循环）
                if (editText.text.toString() != value) {
                    editText.setText(value)
                    editText.setSelection(value.length)
                }
                editText.hint = placeholder
                editText.isSingleLine = singleLine
                editText.isEnabled = enabled
                editText.setTextColor(currentTextColor.toArgb())
                editText.setHintTextColor(currentHintColor.toArgb())
                // 同步输入类型与 IME 动作
                if (inputType != null) editText.inputType = inputType
                editText.imeOptions = imeAction

                // 移除上次注册的 watcher 再添加新的，避免 update 重复调用时累积监听器
                (editText.tag as? TextWatcher)?.let { editText.removeTextChangedListener(it) }
                val textWatcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val newValue = s?.toString() ?: ""
                        if (newValue != value) {
                            onValueChange(newValue)
                        }
                    }
                }
                editText.addTextChangedListener(textWatcher)
                editText.tag = textWatcher
            },
            onRelease = { editText ->
                (editText.tag as? TextWatcher)?.let { editText.removeTextChangedListener(it) }
                editText.tag = null
                editText.setOnEditorActionListener(null)
                editText.setOnFocusChangeListener(null)
            }
        )
    }
}
