package com.bicy.whitenoise.DzBD.PeeU

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class SoundItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val liquidPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var downloadProgress: Float = 0f
    private var isPlaying: Boolean = false
    private var animationTime: Float = 0f
    
    private val liquidColor = Color.parseColor("#4FC3F7")
    private val waveColor = Color.parseColor("#29B6F6")
    private val barColor = Color.parseColor("#6A1B9A")
    
    private val bars = mutableListOf<Bar>()
    private val wavePoints = mutableListOf<Float>()
    
    init {
        setupPaints()
        initBars()
    }
    
    private fun setupPaints() {
        liquidPaint.color = liquidColor
        liquidPaint.style = Paint.Style.FILL
        
        wavePaint.color = waveColor
        wavePaint.style = Paint.Style.STROKE
        wavePaint.strokeWidth = 2f
        
        barPaint.color = barColor
        barPaint.style = Paint.Style.FILL
    }
    
    private fun initBars() {
        bars.clear()
        repeat(3) { index ->
            bars.add(Bar(
                index = index,
                baseHeight = height * 0.3f,
                maxHeight = height * 0.6f
            ))
        }
    }
    
    fun setDownloadProgress(progress: Float) {
        downloadProgress = progress.coerceIn(0f, 1f)
        invalidate()
    }
    
    fun setPlaying(playing: Boolean) {
        isPlaying = playing
        if (playing) {
            startPlayingAnimation()
        } else {
            stopPlayingAnimation()
        }
        invalidate()
    }
    
    private var playingAnimator: ValueAnimator? = null
    
    private fun startPlayingAnimation() {
        stopPlayingAnimation()
        
        playingAnimator = ValueAnimator.ofFloat(0f, 1f)
        playingAnimator?.duration = 1500
        playingAnimator?.repeatCount = ValueAnimator.INFINITE
        playingAnimator?.repeatMode = ValueAnimator.RESTART
        playingAnimator?.addUpdateListener { animation ->
            animationTime = animation.animatedValue as Float
            updateBars()
            invalidate()
        }
        playingAnimator?.start()
    }
    
    private fun stopPlayingAnimation() {
        playingAnimator?.cancel()
        playingAnimator = null
        resetBars()
        invalidate()
    }
    
    private fun updateBars() {
        bars.forEach { bar ->
            val phaseOffset = bar.index * 0.5f
            val phase = animationTime * 6.28f + phaseOffset
            
            val heightVariation = sin(phase) * 0.5f + 0.5f
            bar.currentHeight = bar.baseHeight + heightVariation * (bar.maxHeight - bar.baseHeight)
        }
    }
    
    private fun resetBars() {
        bars.forEach { bar ->
            bar.currentHeight = bar.baseHeight
        }
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initBars()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (downloadProgress > 0f && downloadProgress < 1f) {
            drawLiquidFill(canvas)
        }
        
        if (isPlaying) {
            drawPlayingBars(canvas)
        }
    }
    
    private fun drawLiquidFill(canvas: Canvas) {
        val fillHeight = height * downloadProgress
        
        val gradient = LinearGradient(
            0f, height - fillHeight,
            0f, height.toFloat(),
            liquidColor,
            waveColor,
            Shader.TileMode.CLAMP
        )
        liquidPaint.shader = gradient
        
        val path = Path()
        path.moveTo(0f, height.toFloat())
        
        val waveAmplitude = 8f
        val waveFrequency = 0.05f
        val phase = System.currentTimeMillis() * 0.003f
        
        for (x in 0..width step 4) {
            val normalizedX = x.toFloat()
            val waveY = sin(normalizedX * waveFrequency + phase) * waveAmplitude
            val y = height - fillHeight + waveY
            path.lineTo(normalizedX, y)
        }
        
        path.lineTo(width.toFloat(), height.toFloat())
        path.close()
        
        canvas.drawPath(path, liquidPaint)
        liquidPaint.shader = null
        
        drawWave(canvas, fillHeight, phase)
    }
    
    private fun drawWave(canvas: Canvas, fillHeight: Float, phase: Float) {
        val waveY = height - fillHeight
        
        wavePaint.alpha = 180
        val path = Path()
        path.moveTo(0f, waveY)
        
        for (x in 0..width step 4) {
            val normalizedX = x.toFloat()
            val y = waveY + sin(normalizedX * 0.05f + phase) * 8f
            path.lineTo(normalizedX, y)
        }
        
        canvas.drawPath(path, wavePaint)
    }
    
    private fun drawPlayingBars(canvas: Canvas) {
        val barWidth = width * 0.06f
        val spacing = width * 0.04f
        val totalWidth = bars.size * barWidth + (bars.size - 1) * spacing
        val startX = width - totalWidth - 8f
        val bottomY = height - 6f
        
        bars.forEachIndexed { index, bar ->
            val x = startX + index * (barWidth + spacing)
            val barHeight = bar.currentHeight * 0.4f
            val y = bottomY - barHeight
            
            val rect = RectF(x, y, x + barWidth, bottomY)
            canvas.drawRoundRect(rect, 2f, 2f, barPaint)
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPlayingAnimation()
    }
    
    data class Bar(
        val index: Int,
        val baseHeight: Float,
        val maxHeight: Float,
        var currentHeight: Float = baseHeight
    )
}
