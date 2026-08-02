package com.bicy.whitenoise.ui.screens.ReverbConfigDialogPart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bicy.whitenoise.R
import com.bicy.whitenoise.audio.OboeAudioEngine
import com.bicy.whitenoise.equalizer.AddPointCommand
import com.bicy.whitenoise.equalizer.ChangeCurveInCommand
import com.bicy.whitenoise.equalizer.ChangeCurveOutCommand
import com.bicy.whitenoise.equalizer.ChangeQCommand
import com.bicy.whitenoise.equalizer.ChangeTypeCommand
import com.bicy.whitenoise.equalizer.ControlPoint
import com.bicy.whitenoise.equalizer.CurveInterpolation
import com.bicy.whitenoise.equalizer.DeletePointCommand
import com.bicy.whitenoise.equalizer.EqualizerCurve
import com.bicy.whitenoise.equalizer.EqualizerPreset
import com.bicy.whitenoise.equalizer.EqFilterType
import com.bicy.whitenoise.equalizer.FrequencyResponseGraph
import com.bicy.whitenoise.equalizer.MovePointCommand
import com.bicy.whitenoise.equalizer.PresetStorage
import com.bicy.whitenoise.equalizer.UndoRedoManager
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage
import com.bicy.whitenoise.ui.components.FocusableEditText
import com.bicy.whitenoise.ui.components.InteractiveSlider
import kotlin.math.abs

/** 同时启用 EQ 的白噪音上限，用于限制 CPU 开销 */
const val MAX_SIMULTANEOUS_EQ_SOUNDS = 8

private fun freqToRatio(freq: Float): Float =
    ((kotlin.math.log10(freq.coerceAtLeast(10f).toDouble()) - kotlin.math.log10(10.0)) /
        (kotlin.math.log10(24000.0) - kotlin.math.log10(10.0))).toFloat()

private fun ratioToFreq(ratio: Float): Float =
    Math.pow(10.0, kotlin.math.log10(10.0) + ratio.coerceIn(0f, 1f) *
        (kotlin.math.log10(24000.0) - kotlin.math.log10(10.0))).toFloat()

/**
 * 白噪音每音轨均衡器面板。
 *
 * - 直接使用白噪音的 [soundId] 作为 EQ 曲线的存储与音频引擎索引
 * - 复用 [PresetStorage] 的 trackCurves 实现按 soundId 持久化
 * - 复用 [FrequencyResponseGraph] 与 [UndoRedoManager] 等已有组件
 * - 不包含 AutoEQ（音乐专属）
 * - 通过 [MAX_SIMULTANEOUS_EQ_SOUNDS] 限制同时启用 EQ 的白噪音数量
 */
@Composable
fun WhiteNoiseEqPanel(
    soundId: String,
    modifier: Modifier = Modifier
) {
    var currentCurve by remember { mutableStateOf(EqualizerCurve.defaultCurve()) }
    var selectedPresetName by remember { mutableStateOf("Flat") }
    var selectedIndex by remember { mutableStateOf(-1) }
    var eqVersion by remember { mutableStateOf(0) }
    val undoManager = remember { UndoRedoManager() }
    val customPresetLabel = stringResource(R.string.eq_preset_custom)
    val flatPresetLabel = stringResource(R.string.wn_eq_flat)

    var showPresetDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var savePresetName by remember { mutableStateOf("") }
    val customPresets by PresetStorage.presets.collectAsState()
    val pointCountTabs = remember { listOf(5, 7, 11, 13, 16) }
    var selectedPointTab by remember { mutableStateOf(5) }

    fun applyCurve(curve: EqualizerCurve) {
        val selPt = curve.points.getOrNull(selectedIndex)
        curve.points.sortBy { it.frequencyHz }
        selectedIndex = if (selPt != null) curve.points.indexOfFirst { it === selPt } else -1
        eqVersion++

        val sorted = curve.points
        if (sorted.isEmpty()) return
        val freqs = FloatArray(sorted.size) { sorted[it].frequencyHz }
        val gains = FloatArray(sorted.size) { sorted[it].gainDb }
        val types = IntArray(sorted.size) { sorted[it].filterType.nativeValue }
        val qs = FloatArray(sorted.size) { sorted[it].qOverride }
        val cIns = IntArray(sorted.size) { sorted[it].curveIn.nativeValue }
        val cOuts = IntArray(sorted.size) { sorted[it].curveOut.nativeValue }

        OboeAudioEngine.setEqualizerCurve(soundId, freqs, gains, types, qs, cIns, cOuts)
        // 均衡器默认启用（保持默认平直曲线），无需开关
        OboeAudioEngine.setEqEnabled(soundId, true)
        PresetStorage.saveTrackCurve(soundId, currentCurve)
    }

    // 初次进入：加载已保存的曲线并启用 EQ（默认平直）
    LaunchedEffect(soundId) {
        currentCurve = PresetStorage.getTrackCurve(soundId)
        selectedPresetName = currentCurve.name.ifBlank { flatPresetLabel }
        applyCurve(currentCurve)
        OboeAudioEngine.setEqEnabled(soundId, true)
        WhiteNoiseStorage.updatePlayingSoundEqEnabled(soundId, true)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 标题（均衡器默认启用，保持默认平直曲线，无需开关）
        Text(
            text = stringResource(R.string.equalizer),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 预设选择 + 撤销/重做 + 保存
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .clickable {
                            selectedPointTab = currentCurve.pointCount.coerceIn(5..16)
                            if (selectedPointTab !in pointCountTabs) {
                                selectedPointTab = pointCountTabs.minByOrNull { abs(it - currentCurve.pointCount) } ?: 5
                            }
                            showPresetDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.preset) + ": $selectedPresetName",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.expand),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 撤销
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable(enabled = undoManager.canUndo) {
                        if (undoManager.undo(currentCurve)) {
                            selectedPresetName = customPresetLabel
                            applyCurve(currentCurve)
                        }
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u21A9",
                    fontSize = 16.sp,
                    color = if (undoManager.canUndo) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // 重做
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable(enabled = undoManager.canRedo) {
                        if (undoManager.redo(currentCurve)) {
                            selectedPresetName = customPresetLabel
                            applyCurve(currentCurve)
                        }
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u21AA",
                    fontSize = 16.sp,
                    color = if (undoManager.canRedo) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // 保存预设
            IconButton(
                onClick = {
                    savePresetName = selectedPresetName.takeIf { it != customPresetLabel && it != flatPresetLabel } ?: ""
                    showSaveDialog = true
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = stringResource(R.string.eq_save_preset),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 频响曲线图
        FrequencyResponseGraph(
            curve = currentCurve,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { idx -> selectedIndex = idx },
            onPointMoved = { idx, freq, gain ->
                val pt = currentCurve.points.getOrNull(idx) ?: return@FrequencyResponseGraph
                undoManager.execute(MovePointCommand(idx, pt.frequencyHz, pt.gainDb, freq, gain), currentCurve)
                selectedPresetName = customPresetLabel
                applyCurve(currentCurve)
            },
            onPointAdded = { freq, gain ->
                if (currentCurve.points.size >= 16) return@FrequencyResponseGraph
                if (currentCurve.points.any { abs(it.frequencyHz - freq) < 15f }) return@FrequencyResponseGraph
                val pt = ControlPoint(freq, gain)
                val idx = currentCurve.points.size
                undoManager.execute(AddPointCommand(pt, idx), currentCurve)
                selectedIndex = idx
                selectedPresetName = customPresetLabel
                applyCurve(currentCurve)
            },
            onPointDeleted = { idx ->
                val pt = currentCurve.points.getOrNull(idx) ?: return@FrequencyResponseGraph
                undoManager.execute(DeletePointCommand(pt, idx), currentCurve)
                selectedIndex = -1
                selectedPresetName = customPresetLabel
                applyCurve(currentCurve)
            },
            modifier = Modifier.fillMaxWidth(),
            showActualResponse = true,
            getActualResponse = { freq -> OboeAudioEngine.getFilterResponse(soundId, freq) },
            refreshKey = eqVersion
        )

        Spacer(modifier = Modifier.height(8.dp))

        val selPoint = currentCurve.points.getOrNull(selectedIndex)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            // 控制点芯片栏
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                @Suppress("UNUSED_VARIABLE")
                val _force = eqVersion
                items(currentCurve.points.size) { i ->
                    val pt = currentCurve.points[i]
                    val isSel = i == selectedIndex
                    val label = if (pt.frequencyHz >= 1000) String.format("%.1fK", pt.frequencyHz / 1000)
                                else String.format("%.0f", pt.frequencyHz)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface)
                            .clickable { selectedIndex = i }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (selPoint != null) {
                Spacer(modifier = Modifier.height(8.dp))

                // 频率滑块
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("f:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    var freqRatio by remember(selectedIndex, eqVersion) { mutableFloatStateOf(freqToRatio(selPoint.frequencyHz)) }
                    InteractiveSlider(
                        value = freqRatio,
                        onValueChange = { ratio ->
                            freqRatio = ratio
                            val freq = ratioToFreq(ratio)
                            val pt = currentCurve.points.getOrNull(selectedIndex) ?: return@InteractiveSlider
                            undoManager.execute(MovePointCommand(selectedIndex, pt.frequencyHz, pt.gainDb, freq, pt.gainDb), currentCurve)
                            selectedPresetName = customPresetLabel
                            applyCurve(currentCurve)
                        },
                        modifier = Modifier.weight(1f).height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                    var freqText by remember(selectedIndex, eqVersion) {
                        mutableStateOf(String.format("%.0f", selPoint.frequencyHz))
                    }
                    FocusableEditText(
                        value = freqText,
                        onValueChange = { freqText = it },
                        onSearch = {
                            val pt = currentCurve.points.getOrNull(selectedIndex)
                            val parsed = freqText.toFloatOrNull()
                            if (pt != null && parsed != null && parsed in 10f..24000f) {
                                undoManager.execute(MovePointCommand(selectedIndex, pt.frequencyHz, pt.gainDb, parsed, pt.gainDb), currentCurve)
                                selectedPresetName = customPresetLabel
                                applyCurve(currentCurve)
                            } else if (pt != null) {
                                freqText = String.format("%.0f", pt.frequencyHz)
                            }
                        },
                        modifier = Modifier.width(72.dp),
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                        imeAction = EditorInfo.IME_ACTION_DONE
                    )
                    Text("Hz", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                // 增益滑块
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("g:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    var gainRatio by remember(selectedIndex, eqVersion) { mutableFloatStateOf((selPoint.gainDb + 24f) / 48f) }
                    InteractiveSlider(
                        value = gainRatio,
                        onValueChange = { ratio ->
                            gainRatio = ratio
                            val gain = -24f + ratio * 48f
                            val pt = currentCurve.points.getOrNull(selectedIndex) ?: return@InteractiveSlider
                            undoManager.execute(MovePointCommand(selectedIndex, pt.frequencyHz, pt.gainDb, pt.frequencyHz, gain), currentCurve)
                            selectedPresetName = customPresetLabel
                            applyCurve(currentCurve)
                        },
                        modifier = Modifier.weight(1f).height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                    var gainText by remember(selectedIndex, eqVersion) {
                        mutableStateOf(String.format("%+.1f", selPoint.gainDb))
                    }
                    FocusableEditText(
                        value = gainText,
                        onValueChange = { gainText = it },
                        onSearch = {
                            val pt = currentCurve.points.getOrNull(selectedIndex)
                            val parsed = gainText.toFloatOrNull()
                            if (pt != null && parsed != null && parsed in -24f..24f) {
                                undoManager.execute(MovePointCommand(selectedIndex, pt.frequencyHz, pt.gainDb, pt.frequencyHz, parsed), currentCurve)
                                selectedPresetName = customPresetLabel
                                applyCurve(currentCurve)
                            } else if (pt != null) {
                                gainText = String.format("%+.1f", pt.gainDb)
                            }
                        },
                        modifier = Modifier.width(72.dp),
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED,
                        imeAction = EditorInfo.IME_ACTION_DONE
                    )
                    Text("dB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                // Q 值滑块
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Q:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    var qValue by remember(selectedIndex, eqVersion) { mutableFloatStateOf(selPoint.qOverride) }
                    InteractiveSlider(
                        value = (qValue / 10f).coerceIn(0f, 1f),
                        onValueChange = { ratio ->
                            val q = 0.1f + ratio * 9.9f
                            qValue = q
                            val oldQ = currentCurve.points.getOrNull(selectedIndex)?.qOverride ?: q
                            undoManager.execute(ChangeQCommand(selectedIndex, oldQ, q), currentCurve)
                            selectedPresetName = customPresetLabel
                            applyCurve(currentCurve)
                        },
                        modifier = Modifier.weight(1f).height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                    var qText by remember(selectedIndex, eqVersion) {
                        mutableStateOf(String.format("%.2f", selPoint.qOverride))
                    }
                    FocusableEditText(
                        value = qText,
                        onValueChange = { qText = it },
                        onSearch = {
                            val pt = currentCurve.points.getOrNull(selectedIndex)
                            val parsed = qText.toFloatOrNull()
                            if (pt != null && parsed != null && parsed in 0.1f..10f) {
                                undoManager.execute(ChangeQCommand(selectedIndex, pt.qOverride, parsed), currentCurve)
                                selectedPresetName = customPresetLabel
                                applyCurve(currentCurve)
                            } else if (pt != null) {
                                qText = String.format("%.2f", pt.qOverride)
                            }
                        },
                        modifier = Modifier.width(72.dp),
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                        imeAction = EditorInfo.IME_ACTION_DONE
                    )
                }

                // 滤波器类型
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (type in EqFilterType.entries) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (selPoint.filterType == type) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    undoManager.execute(ChangeTypeCommand(selectedIndex, selPoint.filterType, type), currentCurve)
                                    selectedPresetName = customPresetLabel
                                    applyCurve(currentCurve)
                                }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(type.name, fontSize = 10.sp,
                                color = if (selPoint.filterType == type) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // 插值方式 In
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("In:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    for (mode in CurveInterpolation.entries) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selPoint.curveIn == mode) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.surface)
                                .clickable {
                                    val oldMode = currentCurve.points.getOrNull(selectedIndex)?.curveIn ?: mode
                                    undoManager.execute(ChangeCurveInCommand(selectedIndex, oldMode, mode), currentCurve)
                                    selectedPresetName = customPresetLabel
                                    applyCurve(currentCurve)
                                }
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mode.name.take(3), fontSize = 8.sp,
                                color = if (selPoint.curveIn == mode) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // 插值方式 Out
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Out:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    for (mode in CurveInterpolation.entries) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selPoint.curveOut == mode) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.surface)
                                .clickable {
                                    val oldMode = currentCurve.points.getOrNull(selectedIndex)?.curveOut ?: mode
                                    undoManager.execute(ChangeCurveOutCommand(selectedIndex, oldMode, mode), currentCurve)
                                    selectedPresetName = customPresetLabel
                                    applyCurve(currentCurve)
                                }
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mode.name.take(3), fontSize = 8.sp,
                                color = if (selPoint.curveOut == mode) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // 删除当前点
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFD32F2F).copy(alpha = 0.15f))
                        .clickable {
                            val pt = currentCurve.points.getOrNull(selectedIndex) ?: return@clickable
                            undoManager.execute(DeletePointCommand(pt, selectedIndex), currentCurve)
                            selectedIndex = -1
                            selectedPresetName = customPresetLabel
                            applyCurve(currentCurve)
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.wn_eq_delete_point), fontSize = 12.sp, color = Color(0xFFD32F2F))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.wn_eq_point_hint),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 重置按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable {
                        currentCurve = EqualizerCurve.defaultCurve()
                        selectedPresetName = flatPresetLabel
                        selectedIndex = -1
                        undoManager.clear()
                        applyCurve(currentCurve)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u21BA",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(R.string.wn_eq_reset),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }

    // 保存预设对话框
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.eq_save_preset)) },
            text = {
                OutlinedTextField(
                    value = savePresetName,
                    onValueChange = { savePresetName = it },
                    placeholder = { Text(stringResource(R.string.eq_save_preset_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (savePresetName.isNotBlank()) {
                            PresetStorage.savePreset(savePresetName.trim(), currentCurve)
                            selectedPresetName = savePresetName.trim()
                            showSaveDialog = false
                        }
                    },
                    enabled = savePresetName.isNotBlank()
                ) {
                    Text(stringResource(R.string.save_preset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 预设选择弹窗
    if (showPresetDialog) {
        val builtInPresets = remember { PresetStorage.builtInPresets }
        val presetsForTab = if (selectedPointTab == -1) {
            customPresets
        } else {
            builtInPresets.filter { it.curve.pointCount == selectedPointTab } +
                customPresets.filter { it.curve.pointCount == selectedPointTab }
        }

        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text(stringResource(R.string.presets), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pointCountTabs.size + 1) { idx ->
                            val label = if (idx < pointCountTabs.size) "${pointCountTabs[idx]}点"
                                        else stringResource(R.string.eq_custom_presets)
                            val tab = if (idx < pointCountTabs.size) pointCountTabs[idx] else -1
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedPointTab == tab) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedPointTab = tab }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = if (selectedPointTab == tab) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (presetsForTab.isEmpty()) {
                        Text(
                            text = stringResource(R.string.eq_no_custom_presets),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            presetsForTab.forEach { preset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            currentCurve = preset.curve.copy(
                                                points = preset.curve.points.toMutableList()
                                            )
                                            selectedPresetName = preset.name
                                            selectedIndex = -1
                                            undoManager.clear()
                                            applyCurve(currentCurve)
                                            showPresetDialog = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = preset.name,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

