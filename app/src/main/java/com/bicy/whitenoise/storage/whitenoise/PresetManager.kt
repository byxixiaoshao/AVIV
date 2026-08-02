package com.bicy.whitenoise.storage.whitenoise

import com.bicy.whitenoise.storage.core.JsonStorageManager
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.SoundPlayConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object PresetManager {

    private const val PRESETS_FILE = "presets.json"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _presets = MutableStateFlow<List<WhiteNoisePreset>>(emptyList())
    val presets: StateFlow<List<WhiteNoisePreset>> = _presets

    fun load() {
        try {
            val array = runBlocking {
                JsonStorageManager.read(PRESETS_FILE, Array<WhiteNoisePreset>::class.java)
            }
            _presets.value = array?.toList() ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            _presets.value = emptyList()
        }
    }

    fun save(name: String, sounds: List<SoundPlayConfig>): WhiteNoisePreset {
        val preset = WhiteNoisePreset(
            name = name,
            sounds = sounds
        )
        _presets.value = (_presets.value + preset).sortedByDescending { it.createdAt }
        persistPresets()
        return preset
    }

    fun delete(id: String) {
        _presets.value = _presets.value.filter { it.id != id }
        persistPresets()
    }

    fun get(id: String): WhiteNoisePreset? {
        return _presets.value.find { it.id == id }
    }

    private fun persistPresets() {
        scope.launch {
            try {
                JsonStorageManager.write(PRESETS_FILE, _presets.value.toTypedArray())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
