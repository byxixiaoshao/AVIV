package com.bicy.whitenoise.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun FrequencyResponseGraph(
    curve: EqualizerCurve,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onPointMoved: (Int, Float, Float) -> Unit,
    onPointAdded: (Float, Float) -> Unit,
    onPointDeleted: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showTargetCurve: Boolean = true,
    showActualResponse: Boolean = false,
    getActualResponse: ((Float) -> Float)? = null,
    refreshKey: Int = 0
) {
    // 驱动 Canvas 重绘但不破坏 pointerInput 手势
    val redrawTrigger = remember { mutableIntStateOf(0) }
    if (redrawTrigger.intValue != refreshKey) {
        redrawTrigger.intValue = refreshKey
    }
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val targetCurveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val actualCurveColor = Color(0xFF4CAF50)
    val pointFillColor = MaterialTheme.colorScheme.primary
    val pointStrokeColor = MaterialTheme.colorScheme.surface
    val selectedFillColor = Color(0xFFFF5722)

    val currentSelectedIndex by rememberUpdatedState(selectedIndex)

    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val textSizePx = with(LocalDensity.current) { 10.sp.toPx() }

    // Label frequencies for the grid (log scale)
    val labelFrequencies = listOf(20f, 50f, 100f, 200f, 500f, 1000f, 2000f, 5000f, 10000f, 20000f)
    val labelFormats = listOf("20", "50", "100", "200", "500", "1K", "2K", "5K", "10K", "20K")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
    ) {
        // Freq axis labels on top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 2.dp)
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labelFormats.forEach { label ->
                Text(
                    text = label,
                    fontSize = 8.sp,
                    color = textColor
                )
            }
        }

        // dB axis labels on left
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 2.dp, top = 16.dp, bottom = 16.dp)
                .width(36.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("+18", "0", "-18").forEach { label ->
                Text(
                    text = label,
                    fontSize = 8.sp,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, end = 12.dp, top = 16.dp, bottom = 16.dp)
                .pointerInput(curve) {
                    detectTapGestures { offset ->
                        val freq = pxToFreq(offset.x, size.width.toFloat())
                        val gain = pxToGain(offset.y, size.height.toFloat())
                        // Check if tapped near existing point
                        val nearIdx = findNearestPoint(curve, freq, gain, 30f)
                        if (nearIdx >= 0) {
                            onSelectedIndexChange(nearIdx)
                        } else {
                            // Tapped empty space → add new point at long press
                            // Single tap adds point
                            onPointAdded(freq, gain)
                        }
                    }
                }
                .pointerInput(curve) {
                    detectDragGestures(
                        onDragStart = { _ -> },
                        onDrag = { change, _ ->
                            change.consume()
                            if (currentSelectedIndex < 0 || currentSelectedIndex >= curve.points.size) return@detectDragGestures
                            val freq = pxToFreq(
                                change.position.x,
                                size.width.toFloat()
                            ).coerceIn(10f, 24000f)
                            val gain = pxToGain(
                                change.position.y,
                                size.height.toFloat()
                            ).coerceIn(-24f, 24f)
                            onPointMoved(currentSelectedIndex, freq, gain)
                        }
                    )
                }
        ) {
            // 强制 Canvas 在 redrawTrigger 变化时重绘
            @Suppress("UNUSED_VARIABLE")
            val _trigger = redrawTrigger.intValue

            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            // Draw horizontal grid lines
            val dbSteps = listOf(18f, 12f, 6f, 0f, -6f, -12f, -18f)
            dbSteps.forEach { db ->
                val y = gainToPy(db, h)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            // Draw vertical grid lines
            labelFrequencies.forEach { freq ->
                val x = freqToPx(freq, w)
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
            }

            // Draw center (0dB) line
            val centerY = gainToPy(0f, h)
            drawLine(
                color = axisColor,
                start = Offset(0f, centerY),
                end = Offset(w, centerY),
                strokeWidth = 1.5f
            )

            // === Target curve (Catmull-Rom interpolation, dashed) ===
            if (showTargetCurve && curve.points.size >= 2) {
                val path = Path()
                val steps = 200
                var firstPoint = true

                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val freq = 10.0.pow(log10(10.0) + t * (log10(24000.0) - log10(10.0)))
                    val gain = interpolateGain(curve, freq.toFloat())
                    val x = freqToPx(freq.toFloat(), w)
                    val y = gainToPy(gain, h)

                    if (firstPoint) {
                        path.moveTo(x, y)
                        firstPoint = false
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = targetCurveColor,
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // === Actual BiQuad response curve (solid, green) ===
            if (showActualResponse && getActualResponse != null && curve.points.size >= 2) {
                val path = Path()
                val steps = 200
                var firstPoint = true
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val freq = 10.0.pow(log10(10.0) + t * (log10(24000.0) - log10(10.0))).toFloat()
                    val gain = getActualResponse.invoke(freq).coerceIn(-24f, 24f)
                    val x = freqToPx(freq, w)
                    val y = gainToPy(gain, h)
                    if (firstPoint) { path.moveTo(x, y); firstPoint = false }
                    else { path.lineTo(x, y) }
                }
                drawPath(
                    path = path,
                    color = actualCurveColor,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // === Control points ===
            curve.points.forEachIndexed { i, pt ->
                val x = freqToPx(pt.frequencyHz, w)
                val y = gainToPy(pt.gainDb, h)
                val isSelected = i == selectedIndex
                val radius = if (isSelected) 8f else 6f
                val fillColor = if (isSelected) selectedFillColor else pointFillColor
                val drawAlpha = if (isSelected) 1f else 0.8f

                drawCircle(
                    color = fillColor.copy(alpha = drawAlpha),
                    radius = radius,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = pointStrokeColor,
                    radius = radius,
                    center = Offset(x, y),
                    style = Stroke(width = if (isSelected) 2.5f else 2f)
                )
            }
        }
    }
}

// ============ Coordinate conversion helpers ============

private fun freqToPx(freq: Float, width: Float): Float {
    val logMin = log10(10.0)
    val logMax = log10(24000.0)
    val t = (log10(freq.toDouble().coerceAtLeast(10.0)) - logMin) / (logMax - logMin)
    return (t * width).toFloat()
}

private fun pxToFreq(px: Float, width: Float): Float {
    val logMin = log10(10.0)
    val logMax = log10(24000.0)
    val t = (px / width).coerceIn(0f, 1f)
    return 10.0.pow(logMin + t * (logMax - logMin)).toFloat()
}

private fun gainToPy(gain: Float, height: Float): Float {
    val dbMin = -18f
    val dbMax = 18f
    val t = ((gain.coerceIn(dbMin, dbMax) - dbMin) / (dbMax - dbMin))
    return height * (1f - t)
}

private fun pxToGain(px: Float, height: Float): Float {
    val dbMin = -18f
    val dbMax = 18f
    val t = 1f - (px / height).coerceIn(0f, 1f)
    return dbMin + t * (dbMax - dbMin)
}

// ============ Catmull-Rom interpolation ============

private fun interpolateGain(curve: EqualizerCurve, freq: Float): Float {
    val pts = curve.points.sortedBy { it.frequencyHz }
    if (pts.isEmpty()) return 0f
    if (pts.size == 1) return pts[0].gainDb
    if (freq <= pts.first().frequencyHz) return pts.first().gainDb
    if (freq >= pts.last().frequencyHz) return pts.last().gainDb

    for (i in 0 until pts.size - 1) {
        if (freq in pts[i].frequencyHz..pts[i + 1].frequencyHz) {
            // 段 [i, i+1] 的算法：merge(pts[i].curveOut, pts[i+1].curveIn)，取较硬的
            // in/out 都生效：curveOut[i] 决定离开 i 的方式，curveIn[i+1] 决定进入 i+1 的方式，
            // 两者描述同一段，任一端要求更硬的过渡则采用更硬算法。
            // 旧实现只用 curveOut 且 Cat/Cub 不分，导致改 curveIn 无效、Cat↔Cub 无效。
            val mode = mergeInterpolation(pts[i].curveOut, pts[i + 1].curveIn)
            return when (mode) {
                CurveInterpolation.StepHold -> pts[i].gainDb  // 阶梯保持：段内维持前点增益
                CurveInterpolation.Linear -> {
                    val t = (freq - pts[i].frequencyHz) / (pts[i + 1].frequencyHz - pts[i].frequencyHz)
                    pts[i].gainDb + t * (pts[i + 1].gainDb - pts[i].gainDb)
                }
                CurveInterpolation.CatmullRom -> catmullRom(pts, i, freq)        // 自然样条（可能过冲）
                CurveInterpolation.CubicBezier -> cubicHermiteMono(pts, i, freq)  // 单调三次（防过冲，更平稳）
            }
        }
    }
    return 0f
}

// 合并 in/out 插值类型：取较硬的（StepHold > Linear > CubicBezier > CatmullRom）
// 硬算法（StepHold/Linear）是"约束"，软算法（Cat/Cub）是"自由"。
// 任一端要求硬约束即采用硬算法，确保 in/out 都能影响曲线形状。
private fun mergeInterpolation(out: CurveInterpolation, inn: CurveInterpolation): CurveInterpolation {
    fun hardness(m: CurveInterpolation) = when (m) {
        CurveInterpolation.StepHold -> 3
        CurveInterpolation.Linear -> 2
        CurveInterpolation.CubicBezier -> 1
        CurveInterpolation.CatmullRom -> 0
    }
    return if (hardness(out) >= hardness(inn)) out else inn
}

// 单调三次 Hermite 插值（Fritsch-Carlson 单调性约束，防过冲/振铃）
// 与 CatmullRom（自然样条，可能过冲）区分：CubicBezier 更平稳，不产生超出控制点范围的振荡。
// 切线由相邻点差分计算，斜率变号处切线归零，保证单调段不过冲。
private fun cubicHermiteMono(pts: List<ControlPoint>, seg: Int, x: Float): Float {
    val n = pts.size
    val x1 = pts[seg].frequencyHz; val y1 = pts[seg].gainDb
    val x2 = pts[seg + 1].frequencyHz; val y2 = pts[seg + 1].gainDb
    val dx = x2 - x1
    if (dx <= 0f) return y1
    val t = (x - x1) / dx

    // 相邻段斜率
    val dPrev = if (seg > 0) {
        (y1 - pts[seg - 1].gainDb) / (x1 - pts[seg - 1].frequencyHz).coerceAtLeast(1e-6f)
    } else (y2 - y1) / dx
    val dNext = if (seg + 2 < n) {
        (pts[seg + 2].gainDb - y2) / (pts[seg + 2].frequencyHz - x2).coerceAtLeast(1e-6f)
    } else (y2 - y1) / dx
    val dCur = (y2 - y1) / dx

    // 单调性约束（Fritsch-Carlson）：斜率变号处切线归零；同号时切线不超过 3·min(|d₋|,|d₊|)，
    // 确保单调段绝不产生超出控制点范围的过冲/振铃（CatmullRom 无此约束，可能过冲）。
    val m1 = monotoneTangent(dPrev, dCur)
    val m2 = monotoneTangent(dCur, dNext)

    // 三次 Hermite 基函数（切线需乘段宽 dx，因 m 是关于 x 的导数）
    val t2 = t * t
    val t3 = t2 * t
    val h00 = 2f * t3 - 3f * t2 + 1f
    val h10 = t3 - 2f * t2 + t
    val h01 = -2f * t3 + 3f * t2
    val h11 = t3 - t2
    return h00 * y1 + h10 * (m1 * dx) + h01 * y2 + h11 * (m2 * dx)
}

// Fritsch-Carlson 单调切线：斜率变号处切线归零；同号时切线不超过 3·min(|d₋|,|d₊|)
private fun monotoneTangent(dLo: Float, dHi: Float): Float {
    if (dLo * dHi <= 0f) return 0f
    val m = (dLo + dHi) / 2f
    val cap = 3f * minOf(abs(dLo), abs(dHi))
    return if (abs(m) > cap) sign(m) * cap else m
}

private fun catmullRom(pts: List<ControlPoint>, seg: Int, x: Float): Float {
    val x0 = if (seg > 0) pts[seg - 1].frequencyHz else pts[seg].frequencyHz - (pts[seg + 1].frequencyHz - pts[seg].frequencyHz)
    val y0 = if (seg > 0) pts[seg - 1].gainDb else pts[seg].gainDb
    val x1 = pts[seg].frequencyHz; val y1 = pts[seg].gainDb
    val x2 = pts[seg + 1].frequencyHz; val y2 = pts[seg + 1].gainDb
    val x3 = if (seg + 2 < pts.size) pts[seg + 2].frequencyHz else pts[seg + 1].frequencyHz + (pts[seg + 1].frequencyHz - pts[seg].frequencyHz)
    val y3 = if (seg + 2 < pts.size) pts[seg + 2].gainDb else pts[seg + 1].gainDb

    val t = (x - x1) / (x2 - x1)
    val t2 = t * t
    val t3 = t2 * t

    return 0.5f * (
        (2f * y1) +
        (-y0 + y2) * t +
        (2f * y0 - 5f * y1 + 4f * y2 - y3) * t2 +
        (-y0 + 3f * y1 - 3f * y2 + y3) * t3
    )
}

private fun findNearestPoint(curve: EqualizerCurve, freq: Float, gain: Float, thresholdPx: Float): Int {
    // Simple distance check in screen coordinates
    val logMin = log10(10.0)
    val logMax = log10(24000.0)
    val dbMin = -18f
    val dbMax = 18f

    val tx = ((log10(freq.toDouble()) - logMin) / (logMax - logMin)).toFloat()
    val ty = ((gain.coerceIn(dbMin, dbMax) - dbMin) / (dbMax - dbMin))

    var nearest = -1
    var minDist = Float.MAX_VALUE

    curve.points.forEachIndexed { i, pt ->
        val ptx = ((log10(pt.frequencyHz.toDouble()) - logMin) / (logMax - logMin)).toFloat()
        val pty = ((pt.gainDb.coerceIn(dbMin, dbMax) - dbMin) / (dbMax - dbMin))
        val dist = sqrt((ptx - tx) * (ptx - tx) + (pty - ty) * (pty - ty))
        if (dist < minDist) {
            minDist = dist
            nearest = i
        }
    }

    if (nearest >= 0 && minDist < 0.08f) return nearest
    return -1
}
