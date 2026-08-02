package com.bicy.whitenoise.data.agent.effect.EffectToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.audio.OboeAudioEngine
import com.bicy.whitenoise.equalizer.ControlPoint
import com.bicy.whitenoise.equalizer.PresetStorage
import com.bicy.whitenoise.equalizer.UndoRedoManager
import com.bicy.whitenoise.equalizer.AddPointCommand
import com.bicy.whitenoise.equalizer.MovePointCommand
import com.bicy.whitenoise.equalizer.DeletePointCommand
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject
import android.util.Log

class SetEqualizerCurveTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_equalizer_curve"
    override val description = "无极均衡器控制点操作（音乐）。支持添加/移动/删除频段点、设置插值类型、重置。" +
        "频率范围 10Hz-24000Hz，增益范围 -24dB 到 +24dB。频段数限制 16 个（除非设置中解除限制）。" +
        "插值类型决定两个频段点之间的频响曲线变化规律：linear(折线) / catmull(样条) / cubic(三次贝塞尔) / step(阶梯保持)。" +
        "算法说明：C++ DSP 层 biquad 级联滤波器，支持 Peaking/LowShelf/HighShelf 三种滤波类型，Q 值控制带宽。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "sound_id" to ToolProperty("string", "声音ID"),
            "action" to ToolProperty("string", "操作类型：add(添加点) / move(移动点) / delete(删除点) / reset(重置为Flat) / set_interpolation(设置插值类型)"),
            "frequency" to ToolProperty("number", "频率(Hz)，10-24000"),
            "gain" to ToolProperty("number", "增益(dB)，-24到+24"),
            "q" to ToolProperty("number", "Q值（带宽因子），0.1-10.0，默认 1.0"),
            "filter_type" to ToolProperty("string", "滤波类型", enum = listOf("peaking", "low_shelf", "high_shelf")),
            "curve_in" to ToolProperty("string", "该点向低频方向的插值类型", enum = listOf("linear", "catmull", "cubic", "step")),
            "curve_out" to ToolProperty("string", "该点向高频方向的插值类型", enum = listOf("linear", "catmull", "cubic", "step"))
        ), required = listOf("sound_id", "action")
    )
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val soundId = params.getString("sound_id")
        val action = params.getString("action")

        val curve = PresetStorage.getTrackCurve(soundId)

        when (action) {
            "reset" -> {
                val default = com.bicy.whitenoise.equalizer.EqualizerCurve.defaultCurve()
                applyAndSave(soundId, default)
                return ToolResult.Success("均衡器已重置为Flat", targetType = "equalizer", targetId = soundId)
            }
            "delete" -> {
                val freq = params.optDouble("frequency", -1.0).toFloat()
                if (freq < 0) return ToolResult.Error("请提供要删除的频率点")
                val idx = curve.points.indexOfFirst { kotlin.math.abs(it.frequencyHz - freq) < 15f }
                if (idx < 0) return ToolResult.Error("未找到频率 ${freq}Hz 附近的控制点")
                curve.points.removeAt(idx)
                applyAndSave(soundId, curve)
                return ToolResult.Success("已删除 ${freq}Hz 控制点", targetType = "equalizer", targetId = soundId)
            }
            "move" -> {
                val freq = params.optDouble("frequency", -1.0).toFloat()
                val gain = params.optDouble("gain", 0.0).toFloat().coerceIn(-24f, 24f)
                if (freq < 0) return ToolResult.Error("请提供频率参数")
                val targetFreq = freq.coerceIn(10f, 24000f)
                val idx = curve.points.indexOfFirst { kotlin.math.abs(it.frequencyHz - freq) < 15f }
                if (idx >= 0) {
                    curve.points[idx].gainDb = gain
                    curve.points[idx].frequencyHz = targetFreq
                    // 同时更新 Q 值和滤波类型（如果提供）
                    params.opt("q")?.let { curve.points[idx].qOverride = (it as Number).toFloat().coerceIn(0.1f, 10f) }
                    parseFilterType(params)?.let { curve.points[idx].filterType = it }
                    parseCurveInterp(params, "curve_in")?.let { curve.points[idx].curveIn = it }
                    parseCurveInterp(params, "curve_out")?.let { curve.points[idx].curveOut = it }
                } else {
                    val cp = ControlPoint(targetFreq, gain)
                    params.opt("q")?.let { cp.qOverride = (it as Number).toFloat().coerceIn(0.1f, 10f) }
                    parseFilterType(params)?.let { cp.filterType = it }
                    parseCurveInterp(params, "curve_in")?.let { cp.curveIn = it }
                    parseCurveInterp(params, "curve_out")?.let { cp.curveOut = it }
                    curve.points.add(cp)
                    curve.points.sortBy { it.frequencyHz }
                }
                applyAndSave(soundId, curve)
                return ToolResult.Success("控制点已更新：${targetFreq.toInt()}Hz @ ${gain}dB", targetType = "equalizer", targetId = soundId)
            }
            "add" -> {
                val freq = params.optDouble("frequency", 1000.0).toFloat().coerceIn(10f, 24000f)
                val gain = params.optDouble("gain", 0.0).toFloat().coerceIn(-24f, 24f)
                val cp = ControlPoint(freq, gain)
                params.opt("q")?.let { cp.qOverride = (it as Number).toFloat().coerceIn(0.1f, 10f) }
                parseFilterType(params)?.let { cp.filterType = it }
                parseCurveInterp(params, "curve_in")?.let { cp.curveIn = it }
                parseCurveInterp(params, "curve_out")?.let { cp.curveOut = it }
                curve.points.add(cp)
                curve.points.sortBy { it.frequencyHz }
                applyAndSave(soundId, curve)
                return ToolResult.Success("已添加控制点：${freq.toInt()}Hz @ ${gain}dB", targetType = "equalizer", targetId = soundId)
            }
            "set_interpolation" -> {
                val freq = params.optDouble("frequency", -1.0).toFloat()
                if (freq < 0) return ToolResult.Error("请提供要修改插值类型的频率点")
                val idx = curve.points.indexOfFirst { kotlin.math.abs(it.frequencyHz - freq) < 15f }
                if (idx < 0) return ToolResult.Error("未找到频率 ${freq}Hz 附近的控制点")
                parseCurveInterp(params, "curve_in")?.let { curve.points[idx].curveIn = it }
                parseCurveInterp(params, "curve_out")?.let { curve.points[idx].curveOut = it }
                applyAndSave(soundId, curve)
                return ToolResult.Success("已更新 ${freq}Hz 控制点插值类型：in=${curve.points[idx].curveIn.label}，out=${curve.points[idx].curveOut.label}",
                    targetType = "equalizer", targetId = soundId)
            }
            else -> return ToolResult.Error("未知操作：$action，支持 add/move/delete/reset/set_interpolation")
        }
    }

    private fun parseFilterType(params: JSONObject): com.bicy.whitenoise.equalizer.EqFilterType? {
        return when (params.optString("filter_type", "")) {
            "peaking" -> com.bicy.whitenoise.equalizer.EqFilterType.Peaking
            "low_shelf" -> com.bicy.whitenoise.equalizer.EqFilterType.LowShelf
            "high_shelf" -> com.bicy.whitenoise.equalizer.EqFilterType.HighShelf
            else -> null
        }
    }

    private fun parseCurveInterp(params: JSONObject, key: String): com.bicy.whitenoise.equalizer.CurveInterpolation? {
        return when (params.optString(key, "")) {
            "linear" -> com.bicy.whitenoise.equalizer.CurveInterpolation.Linear
            "catmull" -> com.bicy.whitenoise.equalizer.CurveInterpolation.CatmullRom
            "cubic" -> com.bicy.whitenoise.equalizer.CurveInterpolation.CubicBezier
            "step" -> com.bicy.whitenoise.equalizer.CurveInterpolation.StepHold
            else -> null
        }
    }

    private fun applyAndSave(soundId: String, curve: com.bicy.whitenoise.equalizer.EqualizerCurve) {
        val sorted = curve.points.sortedBy { it.frequencyHz }
        val freqs = FloatArray(sorted.size) { sorted[it].frequencyHz }
        val gains = FloatArray(sorted.size) { sorted[it].gainDb }
        val types = IntArray(sorted.size) { sorted[it].filterType.nativeValue }
        val qs = FloatArray(sorted.size) { sorted[it].qOverride }
        val cIns = IntArray(sorted.size) { sorted[it].curveIn.nativeValue }
        val cOuts = IntArray(sorted.size) { sorted[it].curveOut.nativeValue }
        OboeAudioEngine.setEqualizerCurve(soundId, freqs, gains, types, qs, cIns, cOuts)
        OboeAudioEngine.setEqEnabled(soundId, true)
        PresetStorage.saveTrackCurve(soundId, curve)
    }
}

/**
 * 白噪音均衡器独立工具（独立于音乐 EQ）
 * 频段配置与音乐 EQ 一致，支持 in/out 插值类型
 */
class SetWhiteNoiseEqTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_white_noise_eq"
    override val description = "白噪音均衡器控制点操作（独立于音乐 EQ）。支持添加/移动/删除频段点、设置插值类型、重置。" +
        "频率范围 10Hz-24000Hz，增益范围 -24dB 到 +24dB。频段配置与音乐 EQ 一致（16 段可解除限制）。" +
        "插值类型：linear(折线) / catmull(样条) / cubic(三次贝塞尔) / step(阶梯保持)。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "sound_id" to ToolProperty("string", "白噪音声音ID"),
            "action" to ToolProperty("string", "操作类型：add / move / delete / reset / set_interpolation"),
            "frequency" to ToolProperty("number", "频率(Hz)，10-24000"),
            "gain" to ToolProperty("number", "增益(dB)，-24到+24"),
            "q" to ToolProperty("number", "Q值（带宽因子），0.1-10.0，默认 1.0"),
            "filter_type" to ToolProperty("string", "滤波类型", enum = listOf("peaking", "low_shelf", "high_shelf")),
            "curve_in" to ToolProperty("string", "该点向低频方向的插值类型", enum = listOf("linear", "catmull", "cubic", "step")),
            "curve_out" to ToolProperty("string", "该点向高频方向的插值类型", enum = listOf("linear", "catmull", "cubic", "step"))
        ), required = listOf("sound_id", "action")
    )
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val soundId = params.getString("sound_id")
        val action = params.getString("action")

        // 验证是白噪音声音
        val playing = vm.playingSounds.value.find { it.id == soundId }
            ?: return ToolResult.Error("白噪音声音未在播放：$soundId")

        val curve = PresetStorage.getTrackCurve(soundId)

        when (action) {
            "reset" -> {
                val default = com.bicy.whitenoise.equalizer.EqualizerCurve.defaultCurve()
                applyAndSave(soundId, default)
                return ToolResult.Success("「${playing.name}」均衡器已重置为Flat",
                    operationType = "UPDATE", targetType = "white_noise_eq", targetId = soundId, targetName = playing.name)
            }
            "delete" -> {
                val freq = params.optDouble("frequency", -1.0).toFloat()
                if (freq < 0) return ToolResult.Error("请提供要删除的频率点")
                val idx = curve.points.indexOfFirst { kotlin.math.abs(it.frequencyHz - freq) < 15f }
                if (idx < 0) return ToolResult.Error("未找到频率 ${freq}Hz 附近的控制点")
                curve.points.removeAt(idx)
                applyAndSave(soundId, curve)
                return ToolResult.Success("「${playing.name}」已删除 ${freq}Hz 控制点",
                    operationType = "UPDATE", targetType = "white_noise_eq", targetId = soundId, targetName = playing.name)
            }
            "move" -> {
                val freq = params.optDouble("frequency", -1.0).toFloat()
                val gain = params.optDouble("gain", 0.0).toFloat().coerceIn(-24f, 24f)
                if (freq < 0) return ToolResult.Error("请提供频率参数")
                val targetFreq = freq.coerceIn(10f, 24000f)
                val idx = curve.points.indexOfFirst { kotlin.math.abs(it.frequencyHz - freq) < 15f }
                if (idx >= 0) {
                    curve.points[idx].gainDb = gain
                    curve.points[idx].frequencyHz = targetFreq
                    params.opt("q")?.let { curve.points[idx].qOverride = (it as Number).toFloat().coerceIn(0.1f, 10f) }
                } else {
                    val cp = ControlPoint(targetFreq, gain)
                    params.opt("q")?.let { cp.qOverride = (it as Number).toFloat().coerceIn(0.1f, 10f) }
                    curve.points.add(cp)
                    curve.points.sortBy { it.frequencyHz }
                }
                applyAndSave(soundId, curve)
                return ToolResult.Success("「${playing.name}」控制点已更新：${targetFreq.toInt()}Hz @ ${gain}dB",
                    operationType = "UPDATE", targetType = "white_noise_eq", targetId = soundId, targetName = playing.name)
            }
            "add" -> {
                val freq = params.optDouble("frequency", 1000.0).toFloat().coerceIn(10f, 24000f)
                val gain = params.optDouble("gain", 0.0).toFloat().coerceIn(-24f, 24f)
                curve.points.add(ControlPoint(freq, gain))
                curve.points.sortBy { it.frequencyHz }
                applyAndSave(soundId, curve)
                return ToolResult.Success("「${playing.name}」已添加控制点：${freq.toInt()}Hz @ ${gain}dB",
                    operationType = "UPDATE", targetType = "white_noise_eq", targetId = soundId, targetName = playing.name)
            }
            "set_interpolation" -> {
                val freq = params.optDouble("frequency", -1.0).toFloat()
                if (freq < 0) return ToolResult.Error("请提供要修改插值类型的频率点")
                val idx = curve.points.indexOfFirst { kotlin.math.abs(it.frequencyHz - freq) < 15f }
                if (idx < 0) return ToolResult.Error("未找到频率 ${freq}Hz 附近的控制点")
                val cIn = params.optString("curve_in", "")
                val cOut = params.optString("curve_out", "")
                if (cIn.isNotEmpty()) curve.points[idx].curveIn = parseInterp(cIn)
                if (cOut.isNotEmpty()) curve.points[idx].curveOut = parseInterp(cOut)
                applyAndSave(soundId, curve)
                return ToolResult.Success("「${playing.name}」已更新 ${freq}Hz 插值类型",
                    operationType = "UPDATE", targetType = "white_noise_eq", targetId = soundId, targetName = playing.name)
            }
            else -> return ToolResult.Error("未知操作：$action，支持 add/move/delete/reset/set_interpolation")
        }
    }

    private fun parseInterp(s: String) = when (s) {
        "linear" -> com.bicy.whitenoise.equalizer.CurveInterpolation.Linear
        "catmull" -> com.bicy.whitenoise.equalizer.CurveInterpolation.CatmullRom
        "cubic" -> com.bicy.whitenoise.equalizer.CurveInterpolation.CubicBezier
        "step" -> com.bicy.whitenoise.equalizer.CurveInterpolation.StepHold
        else -> com.bicy.whitenoise.equalizer.CurveInterpolation.CatmullRom
    }

    private fun applyAndSave(soundId: String, curve: com.bicy.whitenoise.equalizer.EqualizerCurve) {
        val sorted = curve.points.sortedBy { it.frequencyHz }
        val freqs = FloatArray(sorted.size) { sorted[it].frequencyHz }
        val gains = FloatArray(sorted.size) { sorted[it].gainDb }
        val types = IntArray(sorted.size) { sorted[it].filterType.nativeValue }
        val qs = FloatArray(sorted.size) { sorted[it].qOverride }
        val cIns = IntArray(sorted.size) { sorted[it].curveIn.nativeValue }
        val cOuts = IntArray(sorted.size) { sorted[it].curveOut.nativeValue }
        OboeAudioEngine.setEqualizerCurve(soundId, freqs, gains, types, qs, cIns, cOuts)
        OboeAudioEngine.setEqEnabled(soundId, true)
        PresetStorage.saveTrackCurve(soundId, curve)
    }
}

class SetEqualizerBandTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_equalizer_band"
    override val description = "已废弃，请使用 set_equalizer_curve 代替。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "sound_id" to ToolProperty("string", "声音ID"),
            "band_index" to ToolProperty("number", "频段索引 0-9"),
            "gain" to ToolProperty("number", "增益 -12 到 +12")
        ), required = listOf("sound_id", "band_index", "gain")
    )
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val soundId = params.getString("sound_id")
        val band = params.getInt("band_index").coerceIn(0, 9)
        val gain = params.getDouble("gain").toFloat().coerceIn(-12f, 12f)
        val curve = PresetStorage.getTrackCurve(soundId)
        val freqs = listOf(32f, 64f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
        val targetFreq = freqs[band]
        val idx = curve.points.indexOfFirst { kotlin.math.abs(it.frequencyHz - targetFreq) < targetFreq * 0.15f }
        if (idx >= 0) {
            curve.points[idx].gainDb = gain
        } else {
            curve.points.add(ControlPoint(targetFreq, gain))
            curve.points.sortBy { it.frequencyHz }
        }
        val sorted = curve.points.sortedBy { it.frequencyHz }
        val f = FloatArray(sorted.size) { sorted[it].frequencyHz }
        val g = FloatArray(sorted.size) { sorted[it].gainDb }
        val t = IntArray(sorted.size) { sorted[it].filterType.nativeValue }
        val q = FloatArray(sorted.size) { sorted[it].qOverride }
        val cIns = IntArray(sorted.size) { sorted[it].curveIn.nativeValue }
        val cOuts = IntArray(sorted.size) { sorted[it].curveOut.nativeValue }
        OboeAudioEngine.setEqualizerCurve(soundId, f, g, t, q, cIns, cOuts)
        OboeAudioEngine.setEqEnabled(soundId, true)
        PresetStorage.saveTrackCurve(soundId, curve)
        return ToolResult.Success("已更新 ${freqs[band]} 频段增益为 ${gain}dB", targetType = "equalizer", targetId = soundId)
    }
}

class SetLimiterTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_limiter"; override val description = "调整全局限制器参数，避免音频过载。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "enabled" to ToolProperty("boolean", "是否启用限制器"),
            "threshold" to ToolProperty("number", "阈值 0.0-1.0（默认 0.9）"),
            "attack" to ToolProperty("number", "启动时间（毫秒）默认 5.0"),
            "release" to ToolProperty("number", "释放时间（毫秒）默认 50.0"),
            "limit_equalizer" to ToolProperty("boolean", "是否限制 EQ 输出"),
            "limit_effects" to ToolProperty("boolean", "是否限制效果输出"),
            "limit_reverb" to ToolProperty("boolean", "是否限制混响输出"),
            "limit_spatial" to ToolProperty("boolean", "是否限制空间输出")
        ), required = listOf("enabled")
    )
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val enabled = params.getBoolean("enabled")
        val threshold = (params.opt("threshold") as? Number)?.toFloat() ?: 0.9f
        val attack = (params.opt("attack") as? Number)?.toFloat() ?: 5.0f
        val release = (params.opt("release") as? Number)?.toFloat() ?: 50.0f
        OboeAudioEngine.setGlobalLimiterConfig(enabled,
            params.optBoolean("limit_equalizer",true), params.optBoolean("limit_effects",true),
            params.optBoolean("limit_reverb",true), params.optBoolean("limit_spatial",true),
            threshold, attack, release)
        return ToolResult.Success("限制器已${if (enabled) "启用" else "禁用"}：阈值=$threshold，启动=${attack}ms，释放=${release}ms",
            operationType = "UPDATE", targetType = "limiter", targetId = "global", targetName = "limiter")
    }
}
