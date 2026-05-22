package com.bicy.whitenoise.H3HO

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object ReverbManager {
    
    private const val TAG = "ReverbManager"
    private const val PREFS_NAME = "reverb_settings"
    private const val KEY_REVERB_CONFIG = "reverb_config"
    
    private lateinit var prefs: android.content.SharedPreferences
    
    private val _reverbConfig = MutableStateFlow(ReverbConfig())
    val reverbConfig: StateFlow<ReverbConfig> = _reverbConfig.asStateFlow()
    
    private val listeners = mutableListOf<() -> Unit>()
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadReverbConfig()
    }
    
    suspend fun initAsync(context: Context) {
        withContext(Dispatchers.IO) {
            init(context)
        }
    }
    
    private fun loadReverbConfig() {
        val config = com.bicy.whitenoise.JwJY.EY9i.MusicStorage.getReverbConfig()
        _reverbConfig.value = config
    }
    
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }
    
    private fun notifyListeners() {
        listeners.forEach { it() }
    }
    
    fun getReverbConfig(): ReverbConfig = _reverbConfig.value
    
    fun updateReverbConfig(config: ReverbConfig) {
        com.bicy.whitenoise.JwJY.EY9i.MusicStorage.updateReverbConfig(config)
        _reverbConfig.value = config
        notifyListeners()
        Log.d(TAG, "Reverb config updated: $config")
    }
    
    fun setEnabled(enabled: Boolean) {
        com.bicy.whitenoise.JwJY.EY9i.MusicStorage.setReverbEnabled(enabled)
        _reverbConfig.value = _reverbConfig.value.copy(enabled = enabled)
        notifyListeners()
        Log.d(TAG, "Reverb enabled: $enabled")
    }
    
    fun setPreset(preset: String) {
        com.bicy.whitenoise.JwJY.EY9i.MusicStorage.setReverbPreset(preset)
        _reverbConfig.value = _reverbConfig.value.copy(preset = preset)
        notifyListeners()
        Log.d(TAG, "Reverb preset: $preset")
    }
    
    fun setRoomSize(size: Float) {
        val current = _reverbConfig.value
        com.bicy.whitenoise.JwJY.EY9i.MusicStorage.updateReverbConfig(current.copy(roomSize = size))
        _reverbConfig.value = current.copy(roomSize = size)
        notifyListeners()
    }
    
    fun setDecayTime(decayTime: Float) {
        val current = _reverbConfig.value
        com.bicy.whitenoise.JwJY.EY9i.MusicStorage.updateReverbConfig(current.copy(decayTime = decayTime))
        _reverbConfig.value = current.copy(decayTime = decayTime)
        notifyListeners()
    }
    
    fun setDamping(damping: Float) {
        val current = _reverbConfig.value
        com.bicy.whitenoise.JwJY.EY9i.MusicStorage.updateReverbConfig(current.copy(damping = damping))
        _reverbConfig.value = current.copy(damping = damping)
        notifyListeners()
    }
    
    fun setWetLevel(level: Float) {
        val current = _reverbConfig.value
        com.bicy.whitenoise.JwJY.EY9i.MusicStorage.updateReverbConfig(current.copy(wetLevel = level))
        _reverbConfig.value = current.copy(wetLevel = level)
        notifyListeners()
    }
    
    fun setDryLevel(level: Float) {
        val current = _reverbConfig.value
        com.bicy.whitenoise.JwJY.EY9i.MusicStorage.updateReverbConfig(current.copy(dryLevel = level))
        _reverbConfig.value = current.copy(dryLevel = level)
        notifyListeners()
    }
    
    fun setPreDelay(delay: Float) {
        val current = _reverbConfig.value
        com.bicy.whitenoise.JwJY.EY9i.MusicStorage.updateReverbConfig(current.copy(preDelay = delay))
        _reverbConfig.value = current.copy(preDelay = delay)
        notifyListeners()
    }
    
    fun setInsulation(insulation: Float) {
        val current = _reverbConfig.value
        com.bicy.whitenoise.JwJY.EY9i.MusicStorage.updateReverbConfig(current.copy(insulation = insulation))
        _reverbConfig.value = current.copy(insulation = insulation)
        notifyListeners()
    }
    
    private val soundReverbConfigs = mutableMapOf<String, ReverbConfig>()
    
    fun getConfig(soundId: String): ReverbConfig? {
        return soundReverbConfigs[soundId]
    }
    
    fun setReverbEffect(soundId: String, config: ReverbConfig) {
        soundReverbConfigs[soundId] = config
        com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage.updatePlayingSoundReverb(soundId, config)
        Log.d(TAG, "Set reverb effect for sound: $soundId, roomSize=${config.roomSize}, insulation=${config.insulation}, decayTime=${config.decayTime}")
    }
    
    fun removeReverbEffect(soundId: String) {
        soundReverbConfigs.remove(soundId)
        Log.d(TAG, "Removed reverb effect for sound: $soundId")
    }
    
    fun clearConfig(soundId: String? = null) {
        if (soundId != null) {
            soundReverbConfigs.remove(soundId)
            Log.d(TAG, "Cleared reverb config for sound: $soundId")
        } else {
            soundReverbConfigs.clear()
            Log.d(TAG, "Cleared all sound reverb configs")
        }
    }
    
    fun setConfig(soundId: String, config: ReverbConfig) {
        setReverbEffect(soundId, config)
    }
    
    fun applyReverbConfig(soundId: String) {
        val config = getConfig(soundId)
        if (config == null) {
            Log.w(TAG, "applyReverbConfig: No config found for sound: $soundId")
            return
        }
        
        Log.d(TAG, "Applying reverb config for sound: $soundId, roomSize=${config.roomSize}, decayTime=${config.decayTime}, damping=${config.damping}, wetLevel=${config.wetLevel}, dryLevel=${config.dryLevel}, preDelay=${config.preDelay}, insulation=${config.insulation}")
        
        OboeAudioEngine.setReverbParams(soundId, config.roomSize, config.damping, config.wetLevel)
        OboeAudioEngine.setReverbDecayTime(soundId, config.decayTime)
        OboeAudioEngine.setReverbDryLevel(soundId, config.dryLevel)
        OboeAudioEngine.setReverbPreDelay(soundId, config.preDelay)
        OboeAudioEngine.setInsulation(soundId, config.insulation)
        OboeAudioEngine.setReflectionDensity(soundId, config.reflectionDensity)
        OboeAudioEngine.setReflectionSpread(soundId, config.reflectionSpread)
        OboeAudioEngine.setHighpassCutoff(soundId, config.highpassCutoff)
        OboeAudioEngine.setEarlyReflectionLevel(soundId, config.earlyReflectionLevel)
        
        Log.d(TAG, "Applied reverb config for sound: $soundId")
    }
}
