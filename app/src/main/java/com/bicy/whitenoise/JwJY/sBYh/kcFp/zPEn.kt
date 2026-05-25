package com.bicy.whitenoise.JwJY.sBYh.kcFp

import com.bicy.whitenoise.H3HO.ReverbConfig
import org.json.JSONArray
import org.json.JSONObject

object ConfigParser {
    
    fun parseSoundPlayConfig(json: JSONObject): SoundPlayConfig {
        val reverbConfig = parseReverbConfig(json.optJSONObject("reverbConfig"))
        val spatialConfig = parseSpatialAudioConfig(json.optJSONObject("spatialAudioConfig"))
        val creativeConfig = parseCreativeEffectConfig(json.optJSONObject("creativeEffectConfig"))
        
        return SoundPlayConfig(
            id = json.getString("id"),
            name = json.getString("name"),
            volume = json.optDouble("volume", 1.0).toFloat(),
            reverbConfig = reverbConfig,
            spatialAudioConfig = spatialConfig,
            creativeEffectConfig = creativeConfig,
            translations = parseTranslations(json.optJSONObject("translations")),
            trackType = json.optString("trackType", "loop"),
            audioClips = parseAudioClips(json.optJSONArray("audioClips")),
            minIntervalMs = json.optLong("minIntervalMs", 3000),
            maxIntervalMs = json.optLong("maxIntervalMs", 10000),
            spatialScatterRange = parseSpatialScatterRange(json.optJSONObject("spatialScatterRange")),
            spatialScatterEnabled = json.optBoolean("spatialScatterEnabled", false),
            overlayMode = json.optBoolean("overlayMode", false)
        )
    }
    
    fun parseReverbConfig(json: JSONObject?): ReverbConfig {
        if (json == null) return ReverbConfig()
        return ReverbConfig(
            enabled = json.optBoolean("enabled", false),
            roomSize = json.optDouble("roomSize", 0.0).toFloat(),
            decayTime = json.optDouble("decayTime", 1.5).toFloat(),
            damping = json.optDouble("damping", 0.0).toFloat(),
            wetLevel = json.optDouble("wetLevel", 0.0).toFloat(),
            dryLevel = json.optDouble("dryLevel", 1.0).toFloat(),
            preDelay = json.optDouble("preDelay", 0.025).toFloat(),
            insulation = json.optDouble("insulation", 0.0).toFloat(),
            reflectionDensity = json.optDouble("reflectionDensity", 0.5).toFloat(),
            reflectionSpread = json.optDouble("reflectionSpread", 0.5).toFloat(),
            highpassCutoff = json.optDouble("highpassCutoff", 100.0).toFloat(),
            earlyReflectionLevel = json.optDouble("earlyReflectionLevel", 0.0).toFloat()
        )
    }
    
    fun parseSpatialAudioConfig(json: JSONObject?): SpatialAudioConfig {
        if (json == null) return SpatialAudioConfig()
        return SpatialAudioConfig(
            enabled = json.optBoolean("enabled", false),
            offsetType = json.optInt("offsetType", 0),
            fixedLeftRight = json.optDouble("fixedLeftRight", 0.0).toFloat(),
            fixedUpDown = json.optDouble("fixedUpDown", 0.0).toFloat(),
            fixedFrontBack = json.optDouble("fixedFrontBack", 0.0).toFloat(),
            fixedMultiplier = json.optDouble("fixedMultiplier", 1.0).toFloat(),
            surroundMode = json.optInt("surroundMode", 0),
            surroundRadius = json.optDouble("surroundRadius", 1.0).toFloat(),
            surroundSpeed = json.optDouble("surroundSpeed", 5.0).toFloat(),
            randomMaxDistance = json.optDouble("randomMaxDistance", 5.0).toFloat(),
            randomMinDistance = json.optDouble("randomMinDistance", 0.0).toFloat(),
            randomValue = json.optDouble("randomValue", 0.5).toFloat(),
            randomSpeed = json.optDouble("randomSpeed", 0.3).toFloat()
        )
    }
    
    fun parseCreativeEffectConfig(json: JSONObject?): CreativeEffectConfig {
        if (json == null) return CreativeEffectConfig()
        return CreativeEffectConfig(
            loFi = json.optDouble("loFi", 0.0).toFloat(),
            eightBit = json.optDouble("eightBit", 0.0).toFloat(),
            underwater = json.optDouble("underwater", 0.0).toFloat(),
            alienSignal = json.optDouble("alienSignal", 0.0).toFloat(),
            megaphone = json.optDouble("megaphone", 0.0).toFloat()
        )
    }
    
    fun parseAudioClips(array: JSONArray?): List<ScatteredAudioClipData> {
        if (array == null) return emptyList()
        val clips = mutableListOf<ScatteredAudioClipData>()
        for (i in 0 until array.length()) {
            val json = array.getJSONObject(i)
            clips.add(
                ScatteredAudioClipData(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    filePath = json.getString("filePath"),
                    durationMs = json.optLong("durationMs", 0)
                )
            )
        }
        return clips
    }
    
    fun parseSpatialScatterRange(json: JSONObject?): SpatialScatterRangeData {
        if (json == null) return SpatialScatterRangeData()
        return SpatialScatterRangeData(
            xMin = json.optDouble("xMin", -5.0).toFloat(),
            xMax = json.optDouble("xMax", 5.0).toFloat(),
            yMin = json.optDouble("yMin", 0.0).toFloat(),
            yMax = json.optDouble("yMax", 3.0).toFloat(),
            zMin = json.optDouble("zMin", -5.0).toFloat(),
            zMax = json.optDouble("zMax", 5.0).toFloat()
        )
    }
    
    fun parseTranslations(json: JSONObject?): Map<String, String>? {
        if (json == null) return null
        val map = mutableMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = json.getString(key)
        }
        return map.takeIf { it.isNotEmpty() }
    }
    
    fun parseSoundMetadata(json: JSONObject): SoundMetadata {
        return SoundMetadata(
            id = json.getString("id"),
            name = json.getString("name"),
            displayName = json.optString("displayName", json.getString("name")),
            category = json.optString("category", ""),
            assetPath = json.optString("assetPath").takeIf { it.isNotEmpty() },
            customPath = json.optString("customPath").takeIf { it.isNotEmpty() },
            duration = json.optLong("duration", 0),
            isCustom = json.optBoolean("isCustom", false),
            isFavorite = json.optBoolean("isFavorite", false),
            remoteUrl = json.optString("remoteUrl").takeIf { it.isNotEmpty() },
            author = json.optString("author").takeIf { it.isNotEmpty() },
            authorUrl = json.optString("authorUrl").takeIf { it.isNotEmpty() },
            type = SoundType.valueOf(json.optString("type", "NETWORK_DOWNLOAD")),
            downloadDate = if (json.has("downloadDate")) json.optLong("downloadDate") else null,
            fileSize = if (json.has("fileSize")) json.optLong("fileSize") else null,
            uri = json.optString("uri").takeIf { it.isNotEmpty() }?.let { android.net.Uri.parse(it) },
            addedAt = json.optLong("addedAt", System.currentTimeMillis())
        )
    }
    
    fun toJson(config: SoundPlayConfig): JSONObject {
        return JSONObject().apply {
            put("id", config.id)
            put("name", config.name)
            put("volume", config.volume)
            put("reverbConfig", toJson(config.reverbConfig))
            put("spatialAudioConfig", toJson(config.spatialAudioConfig))
            put("creativeEffectConfig", toJson(config.creativeEffectConfig))
            config.translations?.let { put("translations", JSONObject(it)) }
            put("trackType", config.trackType)
            put("audioClips", toJsonArray(config.audioClips))
            put("minIntervalMs", config.minIntervalMs)
            put("maxIntervalMs", config.maxIntervalMs)
            put("spatialScatterRange", toJson(config.spatialScatterRange))
            put("spatialScatterEnabled", config.spatialScatterEnabled)
            put("overlayMode", config.overlayMode)
        }
    }
    
    fun toJson(config: ReverbConfig): JSONObject {
        return JSONObject().apply {
            put("enabled", config.enabled)
            put("roomSize", config.roomSize)
            put("decayTime", config.decayTime)
            put("damping", config.damping)
            put("wetLevel", config.wetLevel)
            put("dryLevel", config.dryLevel)
            put("preDelay", config.preDelay)
            put("insulation", config.insulation)
            put("reflectionDensity", config.reflectionDensity)
            put("reflectionSpread", config.reflectionSpread)
            put("highpassCutoff", config.highpassCutoff)
            put("earlyReflectionLevel", config.earlyReflectionLevel)
        }
    }
    
    fun toJson(config: SpatialAudioConfig): JSONObject {
        return JSONObject().apply {
            put("enabled", config.enabled)
            put("offsetType", config.offsetType)
            put("fixedLeftRight", config.fixedLeftRight)
            put("fixedUpDown", config.fixedUpDown)
            put("fixedFrontBack", config.fixedFrontBack)
            put("fixedMultiplier", config.fixedMultiplier)
            put("surroundMode", config.surroundMode)
            put("surroundRadius", config.surroundRadius)
            put("surroundSpeed", config.surroundSpeed)
            put("randomMaxDistance", config.randomMaxDistance)
            put("randomMinDistance", config.randomMinDistance)
            put("randomValue", config.randomValue)
            put("randomSpeed", config.randomSpeed)
        }
    }
    
    fun toJson(config: CreativeEffectConfig): JSONObject {
        return JSONObject().apply {
            put("loFi", config.loFi)
            put("eightBit", config.eightBit)
            put("underwater", config.underwater)
            put("alienSignal", config.alienSignal)
            put("megaphone", config.megaphone)
        }
    }
    
    fun toJsonArray(clips: List<ScatteredAudioClipData>): JSONArray {
        return JSONArray().apply {
            clips.forEach { clip ->
                put(JSONObject().apply {
                    put("id", clip.id)
                    put("name", clip.name)
                    put("filePath", clip.filePath)
                    put("durationMs", clip.durationMs)
                })
            }
        }
    }
    
    fun toJson(range: SpatialScatterRangeData): JSONObject {
        return JSONObject().apply {
            put("xMin", range.xMin)
            put("xMax", range.xMax)
            put("yMin", range.yMin)
            put("yMax", range.yMax)
            put("zMin", range.zMin)
            put("zMax", range.zMax)
        }
    }
    
    fun toJson(metadata: SoundMetadata): JSONObject {
        return JSONObject().apply {
            put("id", metadata.id)
            put("name", metadata.name)
            put("displayName", metadata.displayName)
            put("category", metadata.category)
            metadata.assetPath?.let { put("assetPath", it) }
            metadata.customPath?.let { put("customPath", it) }
            put("duration", metadata.duration)
            put("isCustom", metadata.isCustom)
            put("isFavorite", metadata.isFavorite)
            metadata.remoteUrl?.let { put("remoteUrl", it) }
            metadata.author?.let { put("author", it) }
            metadata.authorUrl?.let { put("authorUrl", it) }
            put("type", metadata.type.name)
            metadata.downloadDate?.let { put("downloadDate", it) }
            metadata.fileSize?.let { put("fileSize", it) }
            metadata.uri?.let { put("uri", it.toString()) }
            put("addedAt", metadata.addedAt)
        }
    }
}
