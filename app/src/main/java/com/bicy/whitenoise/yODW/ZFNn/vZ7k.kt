package com.bicy.whitenoise.yODW.ZFNn

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
import com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage
import com.bicy.whitenoise.H3HO.aVzM
import com.bicy.whitenoise.JwJY.NATg.ConfigStorage

@Composable
fun wNvB(
    modifier: Modifier = Modifier
) {
    val pS by WhiteNoiseStorage.playbackState.collectAsState()
    val sDs = pS.sounds
    
    val wnFD by aVzM.wnFD.collectAsState()
    val config by ConfigStorage.config.collectAsState()
    
    val tC by ThemeColorManager.currentThemeColor.collectAsState()
    
    val smoothData = remember { Array(12) { 0f } }
    
    if (!config.vizWnEnabled || sDs.isEmpty()) return
    
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
    
    wnFD.forEachIndexed { index, value ->
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
        if (wnFD.isEmpty()) return@Canvas
        
        val bCt = 12
        val bW = size.width / bCt
        val mBH = size.height
        val mnBH = size.height * 0.02f
        val bY = size.height
        
        for (i in 0 until bCt) {
            val idx = (i * wnFD.size / bCt).coerceIn(0, wnFD.size - 1)
            val v = smoothData[i]
            
            if (v <= 0.01f) continue
            
            val bH = (mnBH + (mBH - mnBH) * v).coerceIn(mnBH, mBH)
            val x = i * bW
            
            drawRoundRect(
                color = tC.primary.copy(alpha = 0.25f * v.coerceIn(0.4f, 1f)),
                topLeft = Offset(x + bW * 0.05f, bY - bH),
                size = Size(bW * 0.9f, bH),
                cornerRadius = CornerRadius(bW * 0.25f, bW * 0.25f)
            )
        }
    }
}

@Composable
fun mGbG(
    iPg: Boolean,
    modifier: Modifier = Modifier
) {
    val mFD by aVzM.mFD.collectAsState()
    val mEL by aVzM.mEL.collectAsState()
    val config by ConfigStorage.config.collectAsState()
    
    val tC by ThemeColorManager.currentThemeColor.collectAsState()
    
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
    
    mFD.forEachIndexed { index, value ->
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
    smoothEnergy = smoothEnergy * 0.75f + mEL * 0.25f
    
    val baseDarkAlpha = if (config.vizFlashEnabled && iPg) 0.3f else 0f
    
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
        
        if (mFD.isEmpty()) return@Canvas
        
        val bCt = 16
        val bW = size.width / bCt
        val mBH = size.height
        val mnBH = size.height * 0.02f
        val bY = size.height
        
        for (i in 0 until bCt) {
            val idx = (i * mFD.size / bCt).coerceIn(0, mFD.size - 1)
            val v = smoothData[i]
            
            if (v <= 0.01f) continue
            
            val bH = (mnBH + (mBH - mnBH) * v).coerceIn(mnBH, mBH)
            val x = i * bW
            
            drawRoundRect(
                color = tC.primary.copy(alpha = 0.3f * v.coerceIn(0.3f, 1f)),
                topLeft = Offset(x + bW * 0.05f, bY - bH),
                size = Size(bW * 0.9f, bH),
                cornerRadius = CornerRadius(bW * 0.2f, bW * 0.2f)
            )
        }
    }
}
