package com.bicy.whitenoise.audio

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object aVzM {
    private const val TAG = "AudioVisualizer"
    
    private val _fftData = MutableStateFlow(FloatArray(16) { 0f })
    val fftData: StateFlow<FloatArray> = _fftData.asStateFlow()
    
    private val _wnFD = MutableStateFlow(FloatArray(16) { 0f })
    val whiteNoiseFftData: StateFlow<FloatArray> = _wnFD.asStateFlow()
    
    private val _mFD = MutableStateFlow(FloatArray(16) { 0f })
    val musicFftData: StateFlow<FloatArray> = _mFD.asStateFlow()
    
    private val _energyLevel = MutableStateFlow(0f)
    val energyLevel: StateFlow<Float> = _energyLevel.asStateFlow()
    
    private val _wnEL = MutableStateFlow(0f)
    val whiteNoiseEnergyLevel: StateFlow<Float> = _wnEL.asStateFlow()
    
    private val _mEL = MutableStateFlow(0f)
    val musicEnergyLevel: StateFlow<Float> = _mEL.asStateFlow()
    
    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    
    fun start() {
        Log.d(TAG, "启动音频可视化更新")
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                try {
                    val data = OboeAudioEngine.getVisualizationData()
                    val wnData = OboeAudioEngine.getWhiteNoiseVisualizationData()
                    val mData = OboeAudioEngine.getMusicVisualizationData()
                    val energy = OboeAudioEngine.getVisualizationEnergy()
                    val wnEnergy = OboeAudioEngine.getWhiteNoiseVisualizationEnergy()
                    val mEnergy = OboeAudioEngine.getMusicVisualizationEnergy()
                    
                    // Filter out NaN values and ensure data is valid
                    if (data.isNotEmpty()) {
                        val cleanData = data.map { if (it.isNaN() || it < 0f) 0f else it.coerceIn(0f, 1f) }.toFloatArray()
                        _fftData.value = cleanData
                        _energyLevel.value = if (energy.isNaN() || energy < 0f) 0f else energy.coerceIn(0f, 1f)
                    }
                    
                    if (wnData.isNotEmpty()) {
                        val cleanWnData = wnData.map { if (it.isNaN() || it < 0f) 0f else it.coerceIn(0f, 1f) }.toFloatArray()
                        _wnFD.value = cleanWnData
                        _wnEL.value = if (wnEnergy.isNaN() || wnEnergy < 0f) 0f else wnEnergy.coerceIn(0f, 1f)
                    }
                    
                    if (mData.isNotEmpty()) {
                        val cleanMData = mData.map { if (it.isNaN() || it < 0f) 0f else it.coerceIn(0f, 1f) }.toFloatArray()
                        _mFD.value = cleanMData
                        _mEL.value = if (mEnergy.isNaN() || mEnergy < 0f) 0f else mEnergy.coerceIn(0f, 1f)
                    }
                    
                    delay(33)
                } catch (e: Exception) {
                    Log.e(TAG, "获取可视化数据失败: ${e.message}")
                    delay(100)
                }
            }
        }
    }
    
    fun stop() {
        Log.d(TAG, "停止音频可视化更新")
        updateJob?.cancel()
        updateJob = null
        _fftData.value = FloatArray(16) { 0f }
        _wnFD.value = FloatArray(16) { 0f }
        _mFD.value = FloatArray(16) { 0f }
        _energyLevel.value = 0f
        _wnEL.value = 0f
        _mEL.value = 0f
    }
    
    fun isEnabled(): Boolean = updateJob?.isActive == true
}
