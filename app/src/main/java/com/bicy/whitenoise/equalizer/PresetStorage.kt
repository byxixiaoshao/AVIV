package com.bicy.whitenoise.equalizer

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class EqMode { PER_TRACK, GLOBAL }

object PresetStorage {
    private const val PRESETS_FILE = "eq_presets.json"
    private const val CURVES_FILE = "eq_curves.json"
    private const val GLOBAL_CURVE_FILE = "eq_global_curve.json"
    private const val EQ_MODE_FILE = "eq_mode.txt"

    private val _presets = MutableStateFlow<List<EqualizerPreset>>(emptyList())
    val presets: StateFlow<List<EqualizerPreset>> = _presets.asStateFlow()

    private val _trackCurves = MutableStateFlow<Map<String, EqualizerCurve>>(emptyMap())
    val trackCurves: StateFlow<Map<String, EqualizerCurve>> = _trackCurves.asStateFlow()

    private val _globalCurve = MutableStateFlow(EqualizerCurve.defaultCurve())
    val globalCurve: StateFlow<EqualizerCurve> = _globalCurve.asStateFlow()

    private val _eqMode = MutableStateFlow(EqMode.PER_TRACK)
    val eqMode: StateFlow<EqMode> = _eqMode.asStateFlow()

    private lateinit var appContext: Context

    // region ==================== 5点 预设 ====================
    private val presets5Point: List<EqualizerPreset> by lazy {
        listOf(
            createPreset("builtin_flat_5", "Flat", listOf(
                30f to 0f, 200f to 0f, 1000f to 0f, 5000f to 0f, 16000f to 0f
            )),
            createPreset("builtin_bass_5", "Bass Boost", listOf(
                30f to 8f, LoShelf, 200f to 3f, 1000f to 0f, 5000f to 0f, 16000f to 0f
            )),
            createPreset("builtin_treble_5", "Treble Boost", listOf(
                30f to 0f, 200f to 0f, 1000f to 0f, 5000f to 4f, 16000f to 8f, HiShelf
            )),
            createPreset("builtin_vocal_5", "Vocal", listOf(
                30f to -4f, LoShelf, 200f to -2f, 800f to 4f, 3000f to 3f, 16000f to -4f, HiShelf
            )),
            createPreset("builtin_rock_5", "Rock", listOf(
                30f to 6f, LoShelf, 100f to 4f, 1000f to -3f, 8000f to 4f, 16000f to 6f, HiShelf
            )),
            createPreset("builtin_pop_5", "Pop", listOf(
                30f to 3f, LoShelf, 100f to 2f, 1000f to 0f, 8000f to 3f, 16000f to 4f, HiShelf
            )),
            createPreset("builtin_classical_5", "Classical", listOf(
                30f to 4f, LoShelf, 200f to 2f, 1000f to 0f, 3000f to 1f, 16000f to 3f, HiShelf
            )),
            createPreset("builtin_jazz_5", "Jazz", listOf(
                30f to 5f, LoShelf, 100f to 3f, 1000f to 0f, 3000f to 2f, 16000f to 2f, HiShelf
            )),
            createPreset("builtin_electronic_5", "Electronic", listOf(
                30f to 7f, LoShelf, 200f to -1f, 1000f to -2f, 8000f to 5f, 16000f to 6f, HiShelf
            )),
            createPreset("builtin_vshape_5", "V-Shape", listOf(
                30f to 8f, LoShelf, 200f to 2f, 1000f to -4f, 8000f to 4f, 16000f to 8f, HiShelf
            )),
            createPreset("builtin_lofi_5", "Lo-Fi", listOf(
                30f to -6f, LoShelf, 200f to -3f, 1000f to 0f, 3000f to -2f, 16000f to -8f, HiShelf
            )),
            createPreset("builtin_dance_5", "Dance", listOf(
                30f to 8f, LoShelf, 100f to 5f, 1000f to -2f, 5000f to 3f, 16000f to 6f, HiShelf
            )),
            createPreset("builtin_hiphop_5", "Hip-Hop", listOf(
                30f to 9f, LoShelf, 80f to 6f, 1000f to -1f, 5000f to 0f, 16000f to 3f, HiShelf
            )),
            createPreset("builtin_rnb_5", "R&B", listOf(
                30f to 6f, LoShelf, 200f to 3f, 800f to 2f, 3000f to 2f, 16000f to 4f, HiShelf
            )),
            createPreset("builtin_acoustic_5", "Acoustic", listOf(
                30f to 2f, LoShelf, 200f to 1f, 1000f to 1f, 5000f to 2f, 16000f to 1f, HiShelf
            ))
        )
    }

    // region ==================== 7点 预设 ====================
    private val presets7Point: List<EqualizerPreset> by lazy {
        listOf(
            createPreset("builtin_flat_7", "Flat", listOf(
                30f to 0f, 100f to 0f, 300f to 0f, 1000f to 0f, 3000f to 0f, 8000f to 0f, 16000f to 0f
            )),
            createPreset("builtin_warm_7", "Warm", listOf(
                30f to 5f, LoShelf, 100f to 4f, 300f to 2f, 1000f to 0f, 3000f to -1f, 8000f to -3f, 16000f to -4f, HiShelf
            )),
            createPreset("builtin_bright_7", "Bright", listOf(
                30f to -2f, LoShelf, 100f to -1f, 300f to 0f, 1000f to 1f, 3000f to 3f, 8000f to 5f, 16000f to 6f, HiShelf
            )),
            createPreset("builtin_deepbass_7", "Deep Bass", listOf(
                30f to 10f, LoShelf, 60f to 8f, 200f to 2f, 1000f to -1f, 3000f to 0f, 8000f to 0f, 16000f to 0f
            )),
            createPreset("builtin_crisp_7", "Crisp Treble", listOf(
                30f to 0f, 100f to 0f, 300f to 0f, 1000f to 1f, 3000f to 4f, 8000f to 7f, 16000f to 9f, HiShelf
            )),
            createPreset("builtin_loudness_7", "Loudness", listOf(
                30f to 6f, LoShelf, 100f to 5f, 300f to 2f, 1000f to -1f, 3000f to -2f, 8000f to 3f, 16000f to 5f, HiShelf
            )),
            createPreset("builtin_voice_7", "Podcast Voice", listOf(
                30f to -8f, LoShelf, 100f to -4f, 300f to 0f, 1000f to 4f, 2000f to 5f, 5000f to 0f, 16000f to -6f, HiShelf
            )),
            createPreset("builtin_gaming_7", "Gaming", listOf(
                30f to 4f, LoShelf, 100f to 3f, 300f to -2f, 1000f to -1f, 4000f to 3f, 8000f to 5f, 16000f to 6f, HiShelf
            )),
            createPreset("builtin_live_7", "Live Concert", listOf(
                30f to 6f, LoShelf, 100f to 3f, 500f to -1f, 1000f to 0f, 4000f to 2f, 10000f to 3f, 16000f to 2f, HiShelf
            )),
            createPreset("builtin_studio_7", "Studio Monitor", listOf(
                30f to 1f, LoShelf, 100f to 0f, 300f to 0f, 1000f to 1f, 3000f to 0f, 8000f to 0f, 16000f to 1f, HiShelf
            ))
        )
    }

    // region ==================== 11点 预设 ====================
    private val presets11Point: List<EqualizerPreset> by lazy {
        listOf(
            createPreset("builtin_flat_11", "Flat", listOf(
                30f to 0f, 60f to 0f, 120f to 0f, 250f to 0f, 500f to 0f,
                1000f to 0f, 2000f to 0f, 4000f to 0f, 8000f to 0f, 12000f to 0f, 16000f to 0f
            )),
            createPreset("builtin_warm_detail_11", "Detailed Warm", listOf(
                30f to 5f, LoShelf, 60f to 5f, 120f to 4f, 250f to 2f,
                500f to 1f, 1000f to 0f, 2000f to -1f, 4000f to -2f, 8000f to -3f,
                12000f to -2f, 16000f to -1f, HiShelf
            )),
            createPreset("builtin_bright_detail_11", "Detailed Bright", listOf(
                30f to -1f, LoShelf, 60f to 0f, 120f to 0f, 250f to 0f,
                500f to 1f, 1000f to 2f, 2000f to 3f, 4000f to 4f, 8000f to 5f,
                12000f to 6f, 16000f to 7f, HiShelf
            )),
            createPreset("builtin_full_spectrum_11", "Full Spectrum", listOf(
                30f to 6f, LoShelf, 60f to 5f, 120f to 3f, 250f to 1f,
                500f to 0f, 1000f to 0f, 2000f to 0f, 4000f to 1f, 8000f to 3f,
                12000f to 5f, 16000f to 6f, HiShelf
            )),
            createPreset("builtin_reference_11", "Reference", listOf(
                30f to 1f, LoShelf, 60f to 0f, 120f to 0f, 250f to 1f,
                500f to 0f, 1000f to 1f, 2000f to 0f, 4000f to 1f, 8000f to 0f,
                12000f to 0f, 16000f to 1f, HiShelf
            )),
            createPreset("builtin_smooth_11", "Smooth", listOf(
                30f to 4f, LoShelf, 60f to 3f, 120f to 2f, 250f to 1f,
                500f to 0f, 1000f to -1f, 2000f to 0f, 4000f to 0f, 8000f to 1f,
                12000f to 2f, 16000f to 3f, HiShelf
            ))
        )
    }

    // region ==================== 13点 预设 ====================
    private val presets13Point: List<EqualizerPreset> by lazy {
        listOf(
            createPreset("builtin_flat_13", "Flat", listOf(
                30f to 0f, 50f to 0f, 80f to 0f, 150f to 0f, 250f to 0f, 500f to 0f,
                1000f to 0f, 2000f to 0f, 3500f to 0f, 6000f to 0f, 9000f to 0f,
                13000f to 0f, 16000f to 0f
            )),
            createPreset("builtin_ultra_warm_13", "Ultra Warm", listOf(
                30f to 6f, LoShelf, 50f to 6f, 80f to 5f, 150f to 3f,
                250f to 2f, 500f to 1f, 1000f to 0f, 2000f to -1f, 3500f to -2f,
                6000f to -3f, 9000f to -3f, 13000f to -2f, 16000f to -1f, HiShelf
            )),
            createPreset("builtin_ultra_bright_13", "Ultra Bright", listOf(
                30f to -2f, LoShelf, 50f to -1f, 80f to 0f, 150f to 0f,
                250f to 1f, 500f to 1f, 1000f to 2f, 2000f to 3f, 3500f to 4f,
                6000f to 5f, 9000f to 6f, 13000f to 7f, 16000f to 8f, HiShelf
            )),
            createPreset("builtin_mastering_13", "Mastering Grade", listOf(
                30f to 1f, LoShelf, 50f to 0f, 80f to 0f, 150f to -1f,
                250f to 0f, 500f to 1f, 1000f to 1f, 2000f to 0f, 3500f to 1f,
                6000f to 0f, 9000f to 0f, 13000f to -1f, 16000f to 0f
            )),
            createPreset("builtin_cinema_13", "Cinema", listOf(
                30f to 8f, LoShelf, 50f to 6f, 80f to 3f, 150f to 0f,
                250f to -1f, 500f to 0f, 1000f to 2f, 2000f to 3f, 3500f to 4f,
                6000f to 5f, 9000f to 5f, 13000f to 3f, 16000f to 2f, HiShelf
            )),
            createPreset("builtin_gamingfps_13", "Gaming FPS", listOf(
                30f to -6f, LoShelf, 50f to -4f, 80f to -2f, 150f to 0f,
                250f to 2f, 500f to 3f, 1000f to 4f, 2000f to 5f, 3500f to 5f,
                6000f to 4f, 9000f to 3f, 13000f to 2f, 16000f to 1f
            ))
        )
    }

    // region ==================== 16点 预设 ====================
    private val presets16Point: List<EqualizerPreset> by lazy {
        listOf(
            createPreset("builtin_flat_16", "Flat", listOf(
                25f to 0f, 40f to 0f, 63f to 0f, 100f to 0f, 160f to 0f, 250f to 0f,
                400f to 0f, 630f to 0f, 1000f to 0f, 1600f to 0f, 2500f to 0f,
                4000f to 0f, 6300f to 0f, 10000f to 0f, 13000f to 0f, 16000f to 0f
            )),
            createPreset("builtin_pro_reference_16", "Pro Reference", listOf(
                25f to 1f, 40f to 1f, 63f to 0f, 100f to 0f, 160f to -1f,
                250f to 0f, 400f to 1f, 630f to 0f, 1000f to 1f, 1600f to 0f,
                2500f to -1f, 4000f to 0f, 6300f to 1f, 10000f to 0f, 13000f to 1f, 16000f to 2f
            )),
            createPreset("builtin_surgical_16", "Surgical Precision", listOf(
                25f to 2f, 40f to 1f, 63f to 0f, 100f to 0f, 160f to -1f,
                250f to 1f, 400f to 0f, 630f to -1f, 1000f to 1f, 1600f to 0f,
                2500f to 1f, 4000f to 0f, 6300f to -1f, 10000f to 0f, 13000f to 1f, 16000f to 2f
            )),
            createPreset("builtin_calibration_16", "Full Calibration", listOf(
                25f to 0f, 40f to -1f, 63f to 0f, 100f to 1f, 160f to 0f,
                250f to -1f, 400f to 1f, 630f to 0f, 1000f to 0f, 1600f to -1f,
                2500f to 1f, 4000f to 0f, 6300f to -1f, 10000f to 1f, 13000f to 0f, 16000f to -1f
            ))
        )
    }

    // region ==================== 辅助方法 ====================

    private fun createPreset(id: String, name: String, rawPoints: List<Any>): EqualizerPreset {
        val points = mutableListOf<ControlPoint>()
        var i = 0
        while (i < rawPoints.size) {
            val pair = rawPoints[i] as? Pair<*, *>
                ?: throw IllegalStateException("Expected Pair at index $i")
            val freq = pair.first as Float
            val gain = pair.second as Float
            i++
            var filterType = EqFilterType.Peaking
            if (i < rawPoints.size && rawPoints[i] is EqFilterType) {
                filterType = rawPoints[i] as EqFilterType
                i++
            }
            points.add(ControlPoint(freq, gain, filterType))
        }
        return EqualizerPreset(id, name, EqualizerCurve(points, name), isBuiltIn = true)
    }

    // DSL helpers
    private val LoShelf = EqFilterType.LowShelf
    private val HiShelf = EqFilterType.HighShelf

    val builtInPresets: List<EqualizerPreset> by lazy {
        presets5Point + presets7Point + presets11Point + presets13Point + presets16Point
    }

    val allPointCounts: List<Int> by lazy {
        builtInPresets.map { it.curve.pointCount }.distinct().sorted()
    }

    fun getPresetsByPointCount(count: Int): List<EqualizerPreset> {
        val builtIn = builtInPresets.filter { it.curve.pointCount == count }
        val custom = _presets.value.filter { it.curve.pointCount == count }
        return builtIn + custom
    }

    // endregion

    fun init(context: Context) {
        appContext = context.applicationContext
        loadAll()
    }

    fun getAllPresets(): List<EqualizerPreset> =
        builtInPresets + _presets.value

    fun savePreset(name: String, curve: EqualizerCurve): EqualizerPreset {
        val preset = EqualizerPreset(
            id = UUID.randomUUID().toString(),
            name = name,
            curve = curve.copy(points = curve.points.toMutableList(), name = name),
            isBuiltIn = false
        )
        _presets.value = _presets.value + preset
        persistPresets()
        return preset
    }

    fun deletePreset(id: String) {
        _presets.value = _presets.value.filter { it.id != id }
        persistPresets()
    }

    fun renamePreset(id: String, newName: String) {
        val presets = _presets.value.toMutableList()
        val idx = presets.indexOfFirst { it.id == id }
        if (idx >= 0) {
            presets[idx] = presets[idx].copy(name = newName)
            _presets.value = presets
            persistPresets()
        }
    }

    // region ==================== EQ模式管理 ====================

    fun setEqMode(mode: EqMode) {
        _eqMode.value = mode
        persistEqMode()
    }

    fun getGlobalCurve(): EqualizerCurve = _globalCurve.value.copy(
        points = _globalCurve.value.points.toMutableList()
    )

    fun saveGlobalCurve(curve: EqualizerCurve) {
        _globalCurve.value = curve.copy(points = curve.points.toMutableList(), name = curve.name)
        persistGlobalCurve()
    }

    // endregion

    // region ==================== 轨道曲线管理 ====================

    fun getTrackCurve(trackId: String): EqualizerCurve =
        _trackCurves.value[trackId] ?: EqualizerCurve.defaultCurve()

    fun saveTrackCurve(trackId: String, curve: EqualizerCurve) {
        _trackCurves.value = _trackCurves.value + (trackId to curve.copy(points = curve.points.toMutableList(), name = curve.name))
        persistCurves()
    }

    // endregion

    // region ==================== 持久化 ====================

    private fun loadAll() {
        val presetsFile = File(appContext.filesDir, PRESETS_FILE)
        val curvesFile = File(appContext.filesDir, CURVES_FILE)
        val globalCurveFile = File(appContext.filesDir, GLOBAL_CURVE_FILE)
        val eqModeFile = File(appContext.filesDir, EQ_MODE_FILE)

        if (presetsFile.exists()) {
            try {
                val json = JSONObject(presetsFile.readText())
                val arr = json.optJSONArray("presets") ?: org.json.JSONArray()
                val list = mutableListOf<EqualizerPreset>()
                for (i in 0 until arr.length()) {
                    list.add(EqualizerPreset.fromJson(arr.getJSONObject(i)))
                }
                _presets.value = list
            } catch (_: Exception) {
                _presets.value = emptyList()
            }
        }

        if (curvesFile.exists()) {
            try {
                val json = JSONObject(curvesFile.readText())
                val map = mutableMapOf<String, EqualizerCurve>()
                json.keys().forEach { key ->
                    map[key] = EqualizerCurve.fromJson(json.getJSONObject(key))
                }
                _trackCurves.value = map
            } catch (_: Exception) {
                _trackCurves.value = emptyMap()
            }
        }

        if (globalCurveFile.exists()) {
            try {
                val json = JSONObject(globalCurveFile.readText())
                _globalCurve.value = EqualizerCurve.fromJson(json)
            } catch (_: Exception) {}
        }

        if (eqModeFile.exists()) {
            try {
                _eqMode.value = if (eqModeFile.readText().trim() == "GLOBAL") EqMode.GLOBAL else EqMode.PER_TRACK
            } catch (_: Exception) {}
        }
    }

    private fun persistPresets() {
        try {
            val json = JSONObject().apply {
                put("presets", org.json.JSONArray().apply {
                    _presets.value.forEach { put(it.toJson()) }
                })
            }
            File(appContext.filesDir, PRESETS_FILE).writeText(json.toString())
        } catch (_: Exception) {}
    }

    private fun persistCurves() {
        try {
            val json = JSONObject().apply {
                _trackCurves.value.forEach { (id, curve) ->
                    put(id, curve.toJson())
                }
            }
            File(appContext.filesDir, CURVES_FILE).writeText(json.toString())
        } catch (_: Exception) {}
    }

    private fun persistGlobalCurve() {
        try {
            File(appContext.filesDir, GLOBAL_CURVE_FILE)
                .writeText(_globalCurve.value.toJson().toString())
        } catch (_: Exception) {}
    }

    private fun persistEqMode() {
        try {
            File(appContext.filesDir, EQ_MODE_FILE)
                .writeText(_eqMode.value.name)
        } catch (_: Exception) {}
    }

    // endregion
}
