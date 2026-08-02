package com.bicy.whitenoise.ui.components.ExpandableTopBarPart

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bicy.whitenoise.R
import com.bicy.whitenoise.audio.aVzM
import com.bicy.whitenoise.ui.components.InteractiveSlider
import com.bicy.whitenoise.ui.components.glass.GlassCard
import kotlin.math.log10

/**
 * 限幅器现代化可视化组件（方案3：传递函数曲线 + 电平表 + 阈值线）。
 *
 * 组成：
 *  - 传递函数曲线（输入 dB → 输出 dB），含 1:1 参考线、阈值线、膝曲线、压缩区高亮
 *  - 当前电平动态点（实时取自 [aVzM] 能量回调，带光晕脉冲）
 *  - 垂直 dB VU 电平表（绿/黄/红渐变 + 峰值保持 + 阈值虚线）
 *  - 增益削减量表（GR）
 *  - 阈值/启动/释放参数滑块
 *
 * 传递函数数学：
 *  - 线性区（input < threshold - knee/2）：output = input
 *  - 膝曲线区（threshold - knee/2 ≤ input < threshold + knee/2）：smoothstep 平滑过渡
 *  - 压缩区（input ≥ threshold + knee/2）：output = threshold + (input - threshold) / ratio
 *  - ratio ≥ [BRICK_WALL_RATIO] 时视为砖墙限幅（output = threshold），匹配 C++ LimiterEffect 行为
 *
 * 实时电平：复用 [aVzM.musicEnergyLevel]（0..1 线性振幅）作为限幅器输入电平代理，
 * 转换为 dBFS：20·log10(energy)。该值为音频引擎输出能量，用作可视化驱动；
 * 若需精确的前置限幅器输入电平，需在 C++ 侧新增回调。
 */
@Composable
fun LimiterVisualizer(
    thresholdDb: Float,
    attackMs: Float,
    releaseMs: Float,
    enabled: Boolean,
    onThresholdChange: (Float) -> Unit,
    onAttackChange: (Float) -> Unit,
    onReleaseChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    /** 输入电平（dBFS）。NaN 时使用 [aVzM] 实时能量。 */
    inputLevelDb: Float = Float.NaN
) {
    // 参数平滑过渡：滑块变化时曲线morph
    val animatedThreshold by animateFloatAsState(
        targetValue = thresholdDb,
        animationSpec = tween(durationMillis = 220),
        label = "threshold"
    )

    // 实时输入电平：优先外部传入，否则取音频引擎能量
    val liveLevelDb = if (!inputLevelDb.isNaN()) {
        inputLevelDb
    } else {
        val energy by aVzM.musicEnergyLevel.collectAsState()
        energyToDb(energy)
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            TransferFunctionRow(
                thresholdDb = animatedThreshold,
                inputLevelDb = liveLevelDb,
                enabled = enabled
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 参数滑块
            val thresholdLabel = stringResource(R.string.limit_threshold)
            val attackLabel = stringResource(R.string.limit_attack)
            val releaseLabel = stringResource(R.string.limit_release)

            ParameterSliderRow(
                label = thresholdLabel,
                valueText = "%.1f dB".format(thresholdDb),
                value = thresholdDb,
                valueRange = -24f..0f,
                enabled = enabled,
                onValueChange = onThresholdChange
            )
            Spacer(modifier = Modifier.height(8.dp))
            ParameterSliderRow(
                label = attackLabel,
                valueText = "%.1f ms".format(attackMs),
                value = attackMs,
                valueRange = 0.1f..50f,
                enabled = enabled,
                onValueChange = onAttackChange
            )
            Spacer(modifier = Modifier.height(8.dp))
            ParameterSliderRow(
                label = releaseLabel,
                valueText = "%.0f ms".format(releaseMs),
                value = releaseMs,
                valueRange = 10f..1000f,
                enabled = enabled,
                onValueChange = onReleaseChange
            )
        }
    }
}

/** 传递函数曲线 + 两侧电平表/GR 表的横向布局。 */
@Composable
private fun TransferFunctionRow(
    thresholdDb: Float,
    inputLevelDb: Float,
    enabled: Boolean
) {
    // 峰值保持：瞬时上升、缓慢衰减
    var peakLevelDb by remember { mutableFloatStateOf(-60f) }
    val currentLevel = inputLevelDb.coerceIn(MIN_DB, MAX_DB)
    val latestLevel by rememberUpdatedState(currentLevel)

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60)
            val lvl = latestLevel
            if (lvl >= peakLevelDb) {
                peakLevelDb = lvl
            } else {
                // 每 60ms 衰减约 0.8 dB（≈13 dB/s），低于当前电平时停止
                peakLevelDb = (peakLevelDb - 0.8f).coerceAtLeast(lvl)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 左：输入电平表
        LevelMeter(
            levelDb = currentLevel,
            peakDb = peakLevelDb,
            thresholdDb = thresholdDb,
            enabled = enabled,
            label = "IN",
            modifier = Modifier.width(28.dp).fillMaxSize()
        )

        // 中：传递函数曲线
        TransferFunctionGraph(
            thresholdDb = thresholdDb,
            inputLevelDb = inputLevelDb,
            enabled = enabled,
            modifier = Modifier.weight(1f).fillMaxSize()
        )

        // 右：增益削减量表
        GainReductionMeter(
            thresholdDb = thresholdDb,
            inputLevelDb = inputLevelDb,
            enabled = enabled,
            modifier = Modifier.width(28.dp).fillMaxSize()
        )
    }
}

/** 传递函数曲线绘制。 */
@Composable
private fun TransferFunctionGraph(
    thresholdDb: Float,
    inputLevelDb: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = onSurfaceColor.copy(alpha = 0.08f)
    val referenceColor = onSurfaceColor.copy(alpha = 0.28f)
    val thresholdColor = Color(0xFFFF5252)
    val curveColor = if (enabled) primaryColor else primaryColor.copy(alpha = 0.35f)
    val glowColor = primaryColor.copy(alpha = 0.35f)
    val pointColor = if (enabled) Color(0xFFFFAB40) else Color(0xFFFFAB40).copy(alpha = 0.4f)

    // 电平点脉冲光晕
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val density = LocalDensity.current
    val textSizePx = with(density) { 9.sp.toPx() }
    val labelColor = onSurfaceColor.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(surfaceColor.copy(alpha = 0.45f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            // 边距留白给轴标签
            val leftPad = textSizePx * 2.2f
            val bottomPad = textSizePx * 1.6f
            val rightPad = 6f
            val topPad = 6f
            val plotW = (w - leftPad - rightPad).coerceAtLeast(1f)
            val plotH = (h - topPad - bottomPad).coerceAtLeast(1f)
            val plotLeft = leftPad
            val plotTop = topPad

            // ---- 网格 ----
            val dbGridSteps = listOf(0f, -12f, -24f, -36f, -48f, -60f)
            dbGridSteps.forEach { db ->
                val y = dbToY(db, plotTop, plotH)
                drawLine(
                    color = gridColor,
                    start = Offset(plotLeft, y),
                    end = Offset(plotLeft + plotW, y),
                    strokeWidth = 1f
                )
            }

            // ---- 1:1 参考对角线（半透明虚线）----
            val refPath = Path().apply {
                moveTo(dbToX(MIN_DB, plotLeft, plotW), dbToY(MIN_DB, plotTop, plotH))
                lineTo(dbToX(MAX_DB, plotLeft, plotW), dbToY(MAX_DB, plotTop, plotH))
            }
            drawPath(
                path = refPath,
                color = referenceColor,
                style = Stroke(
                    width = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            )

            // ---- 阈值线（水平 + 垂直，红色虚线）----
            val thX = dbToX(thresholdDb, plotLeft, plotW)
            val thY = dbToY(thresholdDb, plotTop, plotH)
            val thDash = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
            drawLine(
                color = thresholdColor.copy(alpha = if (enabled) 0.7f else 0.3f),
                start = Offset(thX, plotTop),
                end = Offset(thX, plotTop + plotH),
                strokeWidth = 1.2f,
                pathEffect = thDash
            )
            drawLine(
                color = thresholdColor.copy(alpha = if (enabled) 0.7f else 0.3f),
                start = Offset(plotLeft, thY),
                end = Offset(plotLeft + plotW, thY),
                strokeWidth = 1.2f,
                pathEffect = thDash
            )

            // ---- 传递函数曲线 ----
            // 砖墙限幅：ratio=∞, knee=0（匹配 C++ LimiterEffect）
            val curvePath = Path()
            val steps = 160
            var firstPoint = true
            // 收集压缩区点用于高亮填充
            var compressionStartX = -1f
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                val inDb = MIN_DB + t * (MAX_DB - MIN_DB)
                val outDb = limiterTransferFunction(
                    inputDb = inDb,
                    thresholdDb = thresholdDb,
                    ratio = BRICK_WALL_RATIO,
                    kneeDb = 0f
                )
                val x = dbToX(inDb, plotLeft, plotW)
                val y = dbToY(outDb, plotTop, plotH)
                if (firstPoint) {
                    curvePath.moveTo(x, y)
                    firstPoint = false
                } else {
                    curvePath.lineTo(x, y)
                }
                if (compressionStartX < 0f && inDb >= thresholdDb) {
                    compressionStartX = x
                }
            }

            // 压缩区高亮（曲线与参考线之间填充淡色）
            if (enabled && compressionStartX > 0f) {
                val fillPath = Path()
                fillPath.moveTo(compressionStartX, dbToY(thresholdDb, plotTop, plotH))
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val inDb = MIN_DB + t * (MAX_DB - MIN_DB)
                    if (inDb < thresholdDb) continue
                    val outDb = limiterTransferFunction(
                        inputDb = inDb,
                        thresholdDb = thresholdDb,
                        ratio = BRICK_WALL_RATIO,
                        kneeDb = 0f
                    )
                    fillPath.lineTo(
                        dbToX(inDb, plotLeft, plotW),
                        dbToY(outDb, plotTop, plotH)
                    )
                }
                // 回到参考线
                for (i in steps downTo 0) {
                    val t = i.toFloat() / steps
                    val inDb = MIN_DB + t * (MAX_DB - MIN_DB)
                    if (inDb < thresholdDb) continue
                    fillPath.lineTo(
                        dbToX(inDb, plotLeft, plotW),
                        dbToY(inDb, plotTop, plotH)
                    )
                }
                fillPath.close()
                drawPath(
                    path = fillPath,
                    color = glowColor.copy(alpha = 0.18f)
                )
            }

            // 曲线描边（带发光：先粗描淡色，再细描主色）
            if (enabled) {
                drawPath(
                    path = curvePath,
                    color = curveColor.copy(alpha = 0.3f),
                    style = Stroke(
                        width = 6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            drawPath(
                path = curvePath,
                color = curveColor,
                style = Stroke(
                    width = 2.4f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // ---- 当前电平动态点 ----
            if (!inputLevelDb.isNaN()) {
                val lvlIn = inputLevelDb.coerceIn(MIN_DB, MAX_DB)
                val lvlOut = limiterTransferFunction(
                    inputDb = lvlIn,
                    thresholdDb = thresholdDb,
                    ratio = BRICK_WALL_RATIO,
                    kneeDb = 0f
                )
                val px = dbToX(lvlIn, plotLeft, plotW)
                val py = dbToY(lvlOut, plotTop, plotH)

                // 输入电平垂直引导线
                drawLine(
                    color = pointColor.copy(alpha = 0.25f),
                    start = Offset(px, plotTop),
                    end = Offset(px, plotTop + plotH),
                    strokeWidth = 1f
                )

                // 光晕（脉冲）
                if (enabled) {
                    drawCircle(
                        color = pointColor.copy(alpha = 0.18f),
                        radius = 10f * pulseScale,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = pointColor.copy(alpha = 0.3f),
                        radius = 7f,
                        center = Offset(px, py)
                    )
                }
                // 实心点
                drawCircle(
                    color = pointColor,
                    radius = 4f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.6f,
                    center = Offset(px, py)
                )
            }

            // ---- 轴标签 ----
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = textSizePx
                    isAntiAlias = true
                }
                // Y 轴标签（输出 dB）
                listOf(0f, -12f, -24f, -36f, -48f).forEach { db ->
                    val y = dbToY(db, plotTop, plotH)
                    canvas.nativeCanvas.drawText(
                        "${db.toInt()}",
                        2f,
                        y + textSizePx / 3f,
                        paint
                    )
                }
                // X 轴标签（输入 dB）
                listOf(0f, -12f, -24f, -36f, -48f, -60f).forEach { db ->
                    val x = dbToX(db, plotLeft, plotW)
                    canvas.nativeCanvas.drawText(
                        "${db.toInt()}",
                        x - textSizePx / 2f,
                        h - 2f,
                        paint
                    )
                }
                // 阈值标注
                val thLabel = "T:${thresholdDb.toInt()}"
                paint.color = thresholdColor.toArgb()
                canvas.nativeCanvas.drawText(
                    thLabel,
                    (thX - textSizePx * 1.5f).coerceAtLeast(2f),
                    plotTop + textSizePx,
                    paint
                )
            }
        }
    }
}

/** 垂直 dB VU 电平表。 */
@Composable
private fun LevelMeter(
    levelDb: Float,
    peakDb: Float,
    thresholdDb: Float,
    enabled: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val thresholdColor = Color(0xFFFF5252)
    val meterEnabled = enabled

    val density = LocalDensity.current
    val textSizePx = with(density) { 8.sp.toPx() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(surfaceColor.copy(alpha = 0.5f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val topPad = textSizePx * 1.2f
            val bottomPad = textSizePx * 1.2f
            val meterH = (h - topPad - bottomPad).coerceAtLeast(1f)
            val barW = (w * 0.6f).coerceAtLeast(2f)
            val barLeft = (w - barW) / 2f

            // 背景轨
            drawRoundRect(
                color = onSurfaceColor.copy(alpha = 0.1f),
                topLeft = Offset(barLeft, topPad),
                size = Size(barW, meterH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f)
            )

            // 当前电平填充（自底向上，渐变色）
            val levelFrac = dbToFraction(levelDb)
            val fillH = meterH * levelFrac
            if (fillH > 0f) {
                val brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE53935).copy(alpha = if (meterEnabled) 1f else 0.4f), // 红 -3~0
                        Color(0xFFE53935).copy(alpha = if (meterEnabled) 1f else 0.4f),
                        Color(0xFFFFC107).copy(alpha = if (meterEnabled) 1f else 0.4f), // 黄 -12~-3
                        Color(0xFF4CAF50).copy(alpha = if (meterEnabled) 1f else 0.4f)  // 绿 -60~-12
                    )
                )
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(barLeft, topPad + meterH - fillH),
                    size = Size(barW, fillH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f)
                )
            }

            // 峰值保持指针
            val peakFrac = dbToFraction(peakDb)
            val peakY = topPad + meterH * (1f - peakFrac)
            drawLine(
                color = Color.White.copy(alpha = if (meterEnabled) 0.85f else 0.4f),
                start = Offset(barLeft - 2f, peakY),
                end = Offset(barLeft + barW + 2f, peakY),
                strokeWidth = 1.5f
            )

            // 阈值线（红色虚线）
            val thFrac = dbToFraction(thresholdDb)
            val thY = topPad + meterH * (1f - thFrac)
            drawLine(
                color = thresholdColor.copy(alpha = if (meterEnabled) 0.9f else 0.4f),
                start = Offset(barLeft - 3f, thY),
                end = Offset(barLeft + barW + 3f, thY),
                strokeWidth = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
            )

            // 标签
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = onSurfaceColor.copy(alpha = 0.6f).toArgb()
                    textSize = textSizePx
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(label, w / 2f, textSizePx, paint)
            }
        }
    }
}

/** 增益削减量表（GR），向下延伸。 */
@Composable
private fun GainReductionMeter(
    thresholdDb: Float,
    inputLevelDb: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val grColor = Color(0xFF7C4DFF)

    val density = LocalDensity.current
    val textSizePx = with(density) { 8.sp.toPx() }

    // GR = input - output（dB），超阈值时为正，表示削减量
    val grDb = if (!inputLevelDb.isNaN() && inputLevelDb > thresholdDb) {
        (inputLevelDb - thresholdDb).coerceAtLeast(0f)
    } else 0f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(surfaceColor.copy(alpha = 0.5f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val topPad = textSizePx * 1.2f
            val bottomPad = textSizePx * 1.2f
            val meterH = (h - topPad - bottomPad).coerceAtLeast(1f)
            val barW = (w * 0.6f).coerceAtLeast(2f)
            val barLeft = (w - barW) / 2f

            // 背景轨
            drawRoundRect(
                color = onSurfaceColor.copy(alpha = 0.1f),
                topLeft = Offset(barLeft, topPad),
                size = Size(barW, meterH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f)
            )

            // GR 填充：从顶部向下延伸（表示削减）
            val maxGr = 20f
            val grFrac = (grDb / maxGr).coerceIn(0f, 1f)
            val fillH = meterH * grFrac
            if (fillH > 0f) {
                val brush = Brush.verticalGradient(
                    colors = listOf(
                        grColor.copy(alpha = if (enabled) 0.9f else 0.35f),
                        grColor.copy(alpha = (if (enabled) 0.4f else 0.15f))
                    )
                )
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(barLeft, topPad),
                    size = Size(barW, fillH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f)
                )
            }

            // 标签
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = onSurfaceColor.copy(alpha = 0.6f).toArgb()
                    textSize = textSizePx
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText("GR", w / 2f, textSizePx, paint)
                // GR 数值
                if (grDb > 0.1f) {
                    paint.color = grColor.toArgb()
                    paint.textSize = textSizePx * 0.85f
                    canvas.nativeCanvas.drawText(
                        "-%.1f".format(grDb),
                        w / 2f,
                        h - 2f,
                        paint
                    )
                }
            }
        }
    }
}

/** 参数滑块行（标签 + 数值 + InteractiveSlider）。 */
@Composable
private fun ParameterSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = if (enabled) onSurfaceColor else onSurfaceColor.copy(alpha = 0.4f)
            )
            Text(
                text = valueText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) primaryColor else primaryColor.copy(alpha = 0.4f)
            )
        }
        InteractiveSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = primaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = onSurfaceColor.copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============ 传递函数数学 ============

/**
 * 限幅器/压缩器传递函数。
 *
 * @param inputDb 输入电平（dB）
 * @param thresholdDb 阈值（dB）
 * @param ratio 压缩比（≥1，[BRICK_WALL_RATIO] 视为砖墙）
 * @param kneeDb 膝宽（dB，0 = 硬膝）
 * @return 输出电平（dB）
 */
fun limiterTransferFunction(
    inputDb: Float,
    thresholdDb: Float,
    ratio: Float,
    kneeDb: Float
): Float {
    val halfKnee = kneeDb / 2f
    val kneeBottom = thresholdDb - halfKnee
    val kneeTop = thresholdDb + halfKnee
    val brickWall = ratio >= BRICK_WALL_RATIO

    // 压缩区输出
    fun compressedOut(x: Float): Float = if (brickWall) thresholdDb
                                          else thresholdDb + (x - thresholdDb) / ratio

    return when {
        kneeDb <= 0f -> {
            // 硬膝
            if (inputDb <= thresholdDb) inputDb else compressedOut(inputDb)
        }
        inputDb < kneeBottom -> inputDb
        inputDb > kneeTop -> compressedOut(inputDb)
        else -> {
            // 软膝：smoothstep 平滑过渡
            val t = ((inputDb - kneeBottom) / (kneeTop - kneeBottom)).coerceIn(0f, 1f)
            val smooth = t * t * (3f - 2f * t)
            val linearOut = inputDb
            val compOut = compressedOut(inputDb)
            linearOut + (compOut - linearOut) * smooth
        }
    }
}

// ============ 坐标换算与常量 ============

/** 电平范围（dBFS）。 */
private const val MIN_DB = -60f
private const val MAX_DB = 0f
/** 视为砖墙限幅的 ratio 阈值。 */
private const val BRICK_WALL_RATIO = 100f

/** dB → Canvas X 坐标。 */
private fun dbToX(db: Float, left: Float, width: Float): Float {
    val t = (db - MIN_DB) / (MAX_DB - MIN_DB)
    return left + t.coerceIn(0f, 1f) * width
}

/** dB → Canvas Y 坐标（0 dB 在顶部）。 */
private fun dbToY(db: Float, top: Float, height: Float): Float {
    val t = (db - MIN_DB) / (MAX_DB - MIN_DB)
    return top + (1f - t.coerceIn(0f, 1f)) * height
}

/** dB → 电平表填充比例（0..1）。 */
private fun dbToFraction(db: Float): Float {
    return ((db - MIN_DB) / (MAX_DB - MIN_DB)).coerceIn(0f, 1f)
}

/** 线性能量（0..1）→ dBFS。 */
private fun energyToDb(energy: Float): Float {
    val e = energy.coerceAtLeast(1e-5f)
    return (20f * log10(e)).coerceIn(MIN_DB, MAX_DB)
}

// ============ 小工具 ============

/** 将 Compose Color 转为 ARGB Int（供 native Paint 使用）。 */
private fun Color.toArgb(): Int {
    val a = (alpha * 255).toInt() and 0xFF
    val r = (red * 255).toInt() and 0xFF
    val g = (green * 255).toInt() and 0xFF
    val b = (blue * 255).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
