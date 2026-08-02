package com.bicy.whitenoise.equalizer

import org.json.JSONArray
import org.json.JSONObject

enum class EqFilterType(val nativeValue: Int) {
    Peaking(0),
    LowShelf(1),
    HighShelf(2);

    companion object {
        fun fromNative(n: Int) = entries.firstOrNull { it.nativeValue == n } ?: Peaking
    }
}

enum class CurveInterpolation(val label: String) {
    Linear("折线"),
    CatmullRom("Catmull-Rom 样条"),
    CubicBezier("三次贝塞尔"),
    StepHold("阶梯保持");

    val nativeValue: Int get() = ordinal
}

data class ControlPoint(
    var frequencyHz: Float,
    var gainDb: Float,
    var filterType: EqFilterType = EqFilterType.Peaking,
    var qOverride: Float = 1.0f,
    var curveIn: CurveInterpolation = CurveInterpolation.CatmullRom,
    var curveOut: CurveInterpolation = CurveInterpolation.CatmullRom
) {
    fun isValid(): Boolean = frequencyHz in 10f..24000f && gainDb in -24f..24f

    fun toJson(): JSONObject = JSONObject().apply {
        put("freq", frequencyHz.toDouble())
        put("gain", gainDb.toDouble())
        put("type", filterType.nativeValue)
        put("q", qOverride.toDouble())
        put("curveIn", curveIn.ordinal)
        put("curveOut", curveOut.ordinal)
    }

    companion object {
        fun fromJson(json: JSONObject): ControlPoint = ControlPoint(
            frequencyHz = json.optDouble("freq", 1000.0).toFloat(),
            gainDb = json.optDouble("gain", 0.0).toFloat(),
            filterType = EqFilterType.fromNative(json.optInt("type", 0)),
            qOverride = json.optDouble("q", 1.0).toFloat(),
            curveIn = CurveInterpolation.entries.getOrElse(json.optInt("curveIn", 1)) { CurveInterpolation.CatmullRom },
            curveOut = CurveInterpolation.entries.getOrElse(json.optInt("curveOut", 1)) { CurveInterpolation.CatmullRom }
        )
    }
}

data class EqualizerCurve(
    val points: MutableList<ControlPoint> = mutableListOf(),
    var name: String = "Custom",
    val metadata: MutableMap<String, String> = mutableMapOf()
) {
    val pointCount: Int get() = points.size
    
    val pointCountLabel: String get() = "${pointCount}点"
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("points", JSONArray().apply {
            points.forEach { put(it.toJson()) }
        })
        put("metadata", JSONObject(metadata))
    }

    companion object {
        fun fromJson(json: JSONObject): EqualizerCurve {
            val pts = mutableListOf<ControlPoint>()
            val arr = json.optJSONArray("points") ?: JSONArray()
            for (i in 0 until arr.length()) {
                pts.add(ControlPoint.fromJson(arr.getJSONObject(i)))
            }
            val meta = mutableMapOf<String, String>()
            val metaObj = json.optJSONObject("metadata") ?: JSONObject()
            metaObj.keys().forEach { key -> meta[key] = metaObj.optString(key, "") }
            return EqualizerCurve(
                points = pts,
                name = json.optString("name", "Custom"),
                metadata = meta
            )
        }

        fun defaultCurve(): EqualizerCurve = EqualizerCurve(
            points = mutableListOf(
                ControlPoint(30f, 0f, EqFilterType.LowShelf),
                ControlPoint(200f, 0f, EqFilterType.Peaking),
                ControlPoint(1000f, 0f, EqFilterType.Peaking),
                ControlPoint(5000f, 0f, EqFilterType.Peaking),
                ControlPoint(16000f, 0f, EqFilterType.HighShelf)
            ),
            name = "Flat"
        )
    }
}

data class EqualizerPreset(
    val id: String,
    val name: String,
    val curve: EqualizerCurve,
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("builtIn", isBuiltIn)
        put("createdAt", createdAt)
        put("curve", curve.toJson())
    }

    companion object {
        fun fromJson(json: JSONObject): EqualizerPreset = EqualizerPreset(
            id = json.optString("id", ""),
            name = json.optString("name", "Untitled"),
            curve = EqualizerCurve.fromJson(json.optJSONObject("curve") ?: JSONObject()),
            isBuiltIn = json.optBoolean("builtIn", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
