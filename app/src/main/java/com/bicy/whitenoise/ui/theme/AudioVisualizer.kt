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
import com.bicy.whitenoise.audio.aVzM
import com.bicy.whitenoise.storage.config.ConfigStorage

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
    
    val sensitivityMultiplier = when (config.vizWnSensitivity) {
        0 -> 0.8f
        1 -> 1.2f
        else -> 1.6f
    }
    
    val smoothFactor = when (config.vizRefreshRate) {
        0 -> 0.92f
        1 -> 0.88f
        else -> 0.85f
    }
    
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

        // 任务8：响应频段范围（clamp 到 0..11，白噪音共 12 段）
        val minBand = config.vizResponseMinBand.coerceIn(0, 11)
        val maxBand = config.vizResponseMaxBand.coerceIn(0, 11)
        val effectiveMin = minOf(minBand, maxBand)
        val effectiveMax = maxOf(minBand, maxBand)
        val barCount = effectiveMax - effectiveMin + 1
        if (barCount <= 0) return@Canvas
        val barWidth = size.width / barCount
        val maxBarHeight = size.height
        val minBarHeight = size.height * 0.02f
        val baseY = size.height

        for (i in 0 until barCount) {
            val index = (effectiveMin + i).coerceIn(0, smoothData.size - 1)
            val value = smoothData[index]
            
            // Skip if value is NaN, negative or too small
            if (value.isNaN() || value <= 0.01f) continue
            
            val barHeight = (minBarHeight + (maxBarHeight - minBarHeight) * value).coerceIn(minBarHeight, maxBarHeight)
            val xPos = i * barWidth
            
            // Ensure alpha is valid (not NaN and in valid range)
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
    val musicFftData by aVzM.musicFftData.collectAsState()
    val musicEnergyLevel by aVzM.musicEnergyLevel.collectAsState()
    val config by ConfigStorage.config.collectAsState()
    
    val themeColor by ThemeColorManager.currentThemeColor.collectAsState()
    
    val smoothData = remember { Array(16) { 0f } }
    var smoothEnergy by remember { mutableStateOf(0f) }
    
    if (!config.vizMusicEnabled) return
    
    val sensitivityMultiplier = when (config.vizMusicSensitivity) {
        0 -> 1.0f
        1 -> 1.5f
        else -> 2.0f
    }
    
    val smoothFactor = when (config.vizRefreshRate) {
        0 -> 0.92f
        1 -> 0.88f
        else -> 0.85f
    }
    
    val flashMultiplier = when (config.vizFlashSensitivity) {
        0 -> 0.5f
        1 -> 0.8f
        else -> 1.0f
    }
    
    musicFftData.forEachIndexed { index, value ->
        if (index < 16) {
            val targetValue = value * sensitivityMultiplier
            val currentValue = smoothData[index]
            
            if (targetValue > currentValue) {
                smoothData[index] = currentValue * 0.7f + targetValue * 0.3f
            } else {
                smoothData[index] = currentValue * smoothFactor + targetValue * (1f - smoothFactor)
            }
        }
    }
    smoothEnergy = smoothEnergy * 0.75f + musicEnergyLevel * 0.25f
    
    val baseDarkAlpha = if (config.vizFlashEnabled && isPlaying) 0.3f else 0f
    
    val flashBrightness = if (config.vizFlashEnabled && smoothEnergy > 0.15f) {
        (smoothEnergy * flashMultiplier).coerceIn(0f, 0.25f)
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
        
        if (musicFftData.isEmpty()) return@Canvas

        // 任务8：响应频段范围（音乐共 16 段）
        val minBand = config.vizResponseMinBand.coerceIn(0, 15)
        val maxBand = config.vizResponseMaxBand.coerceIn(0, 15)
        val effectiveMin = minOf(minBand, maxBand)
        val effectiveMax = maxOf(minBand, maxBand)
        val barCount = effectiveMax - effectiveMin + 1
        if (barCount <= 0) return@Canvas
        val barWidth = size.width / barCount
        val maxBarHeight = size.height
        val minBarHeight = size.height * 0.02f
        val baseY = size.height

        for (i in 0 until barCount) {
            val index = (effectiveMin + i).coerceIn(0, smoothData.size - 1)
            val value = smoothData[index]
            
            // Skip if value is NaN, negative or too small
            if (value.isNaN() || value <= 0.01f) continue
            
            val barHeight = (minBarHeight + (maxBarHeight - minBarHeight) * value).coerceIn(minBarHeight, maxBarHeight)
            val xPos = i * barWidth
            
            // Ensure alpha is valid (not NaN and in valid range)
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
