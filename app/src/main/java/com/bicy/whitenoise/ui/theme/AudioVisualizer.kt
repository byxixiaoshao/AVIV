package com.bicy.whitenoise.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage
import com.bicy.whitenoise.audio.OboeAudioEngine
import com.bicy.whitenoise.audio.aVzM
import com.bicy.whitenoise.storage.config.ConfigStorage

/**
 * 从 [smoothData] 的 [minBand, maxBand] 范围内，按 [barCount] 等分取每段最大值。
 * 用于解耦"柱形数量"与"响应频段范围"：柱形数量仅控制显示柱数，频段范围控制采样区间。
 */
private fun sampleBars(
    smoothData: Array<Float>,
    minBand: Int,
    maxBand: Int,
    barCount: Int
): FloatArray {
    val lo = minOf(minBand, maxBand).coerceIn(0, smoothData.size - 1)
    val hi = maxOf(minBand, maxBand).coerceIn(0, smoothData.size - 1)
    val range = hi - lo + 1
    if (range <= 0 || barCount <= 0) return FloatArray(0)
    val out = FloatArray(barCount)
    for (i in 0 until barCount) {
        val start = lo + i * range / barCount
        val end = lo + (i + 1) * range / barCount
        var mx = 0f
        var b = start
        while (b < end && b < smoothData.size) {
            val v = smoothData[b]
            if (!v.isNaN() && v > mx) mx = v
            b++
        }
        out[i] = mx
    }
    return out
}

/**
 * 从 [smoothBins]（FFT 幅度谱，bin i 频率 = i * sampleRate / fftN）按 [minHz, maxHz] 频率范围
 * 等分取每段最大值。频率范围直接以 Hz 表示，用户可直观调整（替代旧的频段索引）。
 */
private fun sampleBarsFromBins(
    smoothBins: FloatArray,
    minHz: Float,
    maxHz: Float,
    sampleRate: Int,
    barCount: Int
): FloatArray {
    if (smoothBins.isEmpty() || barCount <= 0) return FloatArray(0)
    val nyquist = sampleRate / 2f
    val binCount = smoothBins.size
    val lo = (minHz / nyquist * (binCount - 1)).toInt().coerceIn(0, binCount - 1)
    val hi = (maxHz / nyquist * (binCount - 1)).toInt().coerceIn(0, binCount - 1)
    val range = hi - lo + 1
    if (range <= 0) return FloatArray(0)
    val out = FloatArray(barCount)
    for (i in 0 until barCount) {
        val start = lo + i * range / barCount
        val end = lo + (i + 1) * range / barCount
        var mx = 0f
        var b = start
        while (b < end && b < binCount) {
            val v = smoothBins[b]
            if (!v.isNaN() && v > mx) mx = v
            b++
        }
        out[i] = mx
    }
    return out
}

@Composable
fun WhiteNoiseVisualizerBackground(
    modifier: Modifier = Modifier
) {
    val playbackState by WhiteNoiseStorage.playbackState.collectAsState()
    val sounds = playbackState.sounds

    val whiteNoiseFftData by aVzM.whiteNoiseFftData.collectAsState()
    val config by ConfigStorage.config.collectAsState()

    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()

    val smoothData = remember { Array(12) { 0f } }

    if (!config.vizWnEnabled || sounds.isEmpty()) return

    // 灵敏度 0..1 线性映射到 0.8..1.6
    val sensitivityMultiplier = 0.8f + config.vizWnSensitivity * 0.8f
    // 降落速度 0..1 映射到 smoothFactor 1.0..0.8（越大下降越快）
    val smoothFactor = 1.0f - config.vizWnFallSpeed * 0.2f

    whiteNoiseFftData.forEachIndexed { index, value ->
        if (index < 12) {
            val targetValue = value * sensitivityMultiplier
            val currentValue = smoothData[index]

            if (targetValue > currentValue) {
                smoothData[index] = currentValue * 0.7f + targetValue * 0.3f
            } else {
                smoothData[index] = currentValue * smoothFactor + targetValue * (1f - smoothFactor)
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (whiteNoiseFftData.isEmpty()) return@Canvas

        // 响应频段范围（WN clamp 到 0..11，共 12 段）
        val minBand = config.vizWnMinBand.coerceIn(0, 11)
        val maxBand = config.vizWnMaxBand.coerceIn(0, 11)
        val barCount = config.vizWnBarCount.coerceIn(8, 64)
        val bars = sampleBars(smoothData, minBand, maxBand, barCount)
        if (bars.isEmpty()) return@Canvas

        val barWidth = size.width / bars.size
        val maxBarHeight = size.height
        val minBarHeight = size.height * 0.02f
        val baseY = size.height

        for (i in bars.indices) {
            val value = bars[i]
            if (value.isNaN() || value <= 0.01f) continue

            val barHeight = (minBarHeight + (maxBarHeight - minBarHeight) * value)
                .coerceIn(minBarHeight, maxBarHeight)
            val xPos = i * barWidth

            val alphaValue = value.coerceIn(0.4f, 1f)
            val alpha = (0.25f * alphaValue).coerceIn(0f, 1f)

            drawRoundRect(
                color = themeColor.primary.copy(alpha = alpha),
                topLeft = Offset(xPos + barWidth * 0.05f, baseY - barHeight),
                size = Size(barWidth * 0.9f, barHeight),
                cornerRadius = CornerRadius(barWidth * 0.25f, barWidth * 0.25f)
            )
        }
    }
}

@Composable
fun MusicGradientBackground(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val musicSpectrumBins by aVzM.musicSpectrumBins.collectAsState()
    val musicEnergyLevel by aVzM.musicEnergyLevel.collectAsState()
    val config by ConfigStorage.config.collectAsState()

    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()

    val smoothData = remember { FloatArray(512) }
    var smoothEnergy by remember { mutableStateOf(0f) }

    if (!config.vizMusicEnabled) return

    // 音乐灵敏度 0..1 → 1.0..2.0
    val sensitivityMultiplier = 1.0f + config.vizMusicSensitivity * 1.0f
    // 音乐降落速度 0..1 → smoothFactor 1.0..0.8
    val smoothFactor = 1.0f - config.vizMusicFallSpeed * 0.2f

    // 闪烁灵敏度 0..1 → 0.5..1.0
    val flashMultiplier = 0.5f + config.vizFlashSensitivity * 0.5f
    // 闪烁暗淡速度 0..1 → smoothFactor 1.0..0.8（控制能量衰减快慢）
    val flashSmoothFactor = 1.0f - config.vizFlashFallSpeed * 0.2f

    musicSpectrumBins.forEachIndexed { index, value ->
        if (index < smoothData.size) {
            val targetValue = value * sensitivityMultiplier
            val currentValue = smoothData[index]

            if (targetValue > currentValue) {
                smoothData[index] = currentValue * 0.7f + targetValue * 0.3f
            } else {
                smoothData[index] = currentValue * smoothFactor + targetValue * (1f - smoothFactor)
            }
        }
    }
    // 闪烁能量按暗淡速度衰减
    smoothEnergy = smoothEnergy * flashSmoothFactor + musicEnergyLevel * (1f - flashSmoothFactor)

    // 闪烁基底暗度（最低暗度）与最高明度
    val baseDarkAlpha = if (config.vizFlashEnabled && isPlaying) config.vizFlashMinDarkness else 0f

    val flashBrightness = if (config.vizFlashEnabled && smoothEnergy > 0.15f) {
        (smoothEnergy * flashMultiplier).coerceIn(0f, config.vizFlashMaxBrightness)
    } else {
        0f
    }

    val bgAlpha by animateFloatAsState(
        targetValue = baseDarkAlpha,
        animationSpec = tween(durationMillis = 600),
        label = "bgAlpha"
    )

    val flashAlpha by animateFloatAsState(
        targetValue = flashBrightness,
        animationSpec = tween(durationMillis = 100),
        label = "flashAlpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val finalAlpha = (bgAlpha - flashAlpha).coerceIn(0f, 1f)

        if (finalAlpha > 0f) {
            drawRect(
                color = Color.Black.copy(alpha = finalAlpha)
            )
        }

        if (musicSpectrumBins.isEmpty()) return@Canvas

        // 响应频率范围 (Hz, 直接按 FFT bins 采样, 人耳可听 20..20000)
        val minFreq = config.vizMusicMinFreq.coerceIn(20f, 20000f)
        val maxFreq = config.vizMusicMaxFreq.coerceIn(20f, 20000f)
        val barCount = config.vizMusicBarCount.coerceIn(8, 64)
        val sampleRate = OboeAudioEngine.getSampleRate().coerceAtLeast(44100)
        val bars = sampleBarsFromBins(smoothData, minFreq, maxFreq, sampleRate, barCount)
        if (bars.isEmpty()) return@Canvas

        val barWidth = size.width / bars.size
        val maxBarHeight = size.height
        val minBarHeight = size.height * 0.02f
        val baseY = size.height

        for (i in bars.indices) {
            val value = bars[i]
            if (value.isNaN() || value <= 0.01f) continue

            val barHeight = (minBarHeight + (maxBarHeight - minBarHeight) * value)
                .coerceIn(minBarHeight, maxBarHeight)
            val xPos = i * barWidth

            val alphaValue = value.coerceIn(0.3f, 1f)
            val alpha = (0.3f * alphaValue).coerceIn(0f, 1f)

            drawRoundRect(
                color = themeColor.primary.copy(alpha = alpha),
                topLeft = Offset(xPos + barWidth * 0.05f, baseY - barHeight),
                size = Size(barWidth * 0.9f, barHeight),
                cornerRadius = CornerRadius(barWidth * 0.2f, barWidth * 0.2f)
            )
        }
    }
}
