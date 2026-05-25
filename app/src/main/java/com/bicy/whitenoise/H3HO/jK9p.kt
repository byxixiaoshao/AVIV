package com.bicy.whitenoise.H3HO

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
    
    private val _fD = MutableStateFlow(FloatArray(16) { 0f })
    val fD: StateFlow<FloatArray> = _fD.asStateFlow()
    
    private val _wnFD = MutableStateFlow(FloatArray(16) { 0f })
    val wnFD: StateFlow<FloatArray> = _wnFD.asStateFlow()
    
    private val _mFD = MutableStateFlow(FloatArray(16) { 0f })
    val mFD: StateFlow<FloatArray> = _mFD.asStateFlow()
    
    private val _eL = MutableStateFlow(0f)
    val eL: StateFlow<Float> = _eL.asStateFlow()
    
    private val _wnEL = MutableStateFlow(0f)
    val wnEL: StateFlow<Float> = _wnEL.asStateFlow()
    
    private val _mEL = MutableStateFlow(0f)
    val mEL: StateFlow<Float> = _mEL.asStateFlow()
    
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
                    
                    if (data.isNotEmpty()) {
                        _fD.value = data
                        _eL.value = energy
                    }
                    
                    if (wnData.isNotEmpty()) {
                        _wnFD.value = wnData
                        _wnEL.value = wnEnergy
                    }
                    
                    if (mData.isNotEmpty()) {
                        _mFD.value = mData
                        _mEL.value = mEnergy
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
        _fD.value = FloatArray(16) { 0f }
        _wnFD.value = FloatArray(16) { 0f }
        _mFD.value = FloatArray(16) { 0f }
        _eL.value = 0f
        _wnEL.value = 0f
        _mEL.value = 0f
    }
    
    fun iE(): Boolean = updateJob?.isActive == true
}
