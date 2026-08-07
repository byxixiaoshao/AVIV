package com.bicy.whitenoise.data.agent.effect.EffectToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.audio.CreativeEffectManager
import com.bicy.whitenoise.audio.ReverbConfig
import com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStoragePart.CreativeEffectConfig
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject

class SetReverbTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_reverb"
    override val description = "调整指定白噪音的混响参数。可设置房间大小、衰减时间、湿润度、干度、预延迟、隔声等。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "sound_id" to ToolProperty("string", "声音ID"),
            "room_size" to ToolProperty("number", "房间大小 0.0-1.0"),
            "decay_time" to ToolProperty("number", "衰减时间（秒）0.1-10.0"),
            "damping" to ToolProperty("number", "高频衰减 0.0-1.0"),
            "wet_level" to ToolProperty("number", "湿润电平 0.0-1.0"),
            "dry_level" to ToolProperty("number", "干声电平 0.0-1.0"),
            "pre_delay" to ToolProperty("number", "预延迟（秒）0-0.2"),
            "insulation" to ToolProperty("number", "隔声 0.0-1.0")
        ), required = listOf("sound_id")
    )
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val soundId = params.getString("sound_id")
        val playing = vm.playingSounds.value.find { it.id == soundId }
            ?: return ToolResult.Error("声音未在播放：$soundId")
        val current = playing.reverbConfig
        val newConfig = ReverbConfig(
            enabled = true, preset = "CUSTOM",
            roomSize = (params.opt("room_size") as? Number)?.toFloat() ?: current.roomSize,
            decayTime = (params.opt("decay_time") as? Number)?.toFloat() ?: current.decayTime,
            damping = (params.opt("damping") as? Number)?.toFloat() ?: current.damping,
            wetLevel = (params.opt("wet_level") as? Number)?.toFloat() ?: current.wetLevel,
            dryLevel = (params.opt("dry_level") as? Number)?.toFloat() ?: current.dryLevel,
            preDelay = (params.opt("pre_delay") as? Number)?.toFloat() ?: current.preDelay,
            insulation = (params.opt("insulation") as? Number)?.toFloat() ?: current.insulation
        )
        vm.setReverbConfig(soundId, newConfig)
        return ToolResult.Success("「${playing.name}」混响已调整：房间=${newConfig.roomSize}，衰减=${newConfig.decayTime}s，湿润=${newConfig.wetLevel}",
            operationType = "UPDATE", targetType = "reverb", targetId = soundId, targetName = playing.name)
    }
}

class SetCreativeEffectTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_creative_effect"
    override val description = "调整创意效果（LoFi、8-bit、水下、外星信号、扩音器、HiFi、立体声加宽、虚拟低音、多段压缩）。强度范围 0.0-1.0。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "sound_id" to ToolProperty("string", "声音ID"),
            "effect_type" to ToolProperty("string", "效果类型",
                enum = listOf("lofi","eight_bit","underwater","alien_signal","megaphone","hifi","stereo_widener","virtual_bass","multiband_compressor")),
            "intensity" to ToolProperty("number", "强度 0.0-1.0")
        ), required = listOf("sound_id", "effect_type", "intensity")
    )
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val soundId = params.getString("sound_id")
        val effectType = params.getString("effect_type")
        val intensity = params.getDouble("intensity").toFloat().coerceIn(0f, 1f)
        val playing = vm.playingSounds.value.find { it.id == soundId }
            ?: return ToolResult.Error("声音未在播放：$soundId")
        val current = CreativeEffectManager.getConfig(soundId) ?: CreativeEffectConfig()
        val updated = when (effectType) {
            "lofi" -> current.copy(loFi = intensity)
            "eight_bit" -> current.copy(eightBit = intensity)
            "underwater" -> current.copy(underwater = intensity)
            "alien_signal" -> current.copy(alienSignal = intensity)
            "megaphone" -> current.copy(megaphone = intensity)
            "hifi" -> current.copy(hifi = intensity)
            "stereo_widener" -> current.copy(stereoWidener = intensity)
            "virtual_bass" -> current.copy(virtualBass = intensity)
            "multiband_compressor" -> current.copy(multibandCompressor = intensity)
            else -> return ToolResult.Error("未知效果类型：$effectType")
        }
        CreativeEffectManager.setConfig(soundId, updated)
        CreativeEffectManager.applyCreativeEffectConfig(soundId)
        return ToolResult.Success("「${playing.name}」的 $effectType 效果已设置为 $intensity",
            operationType = "UPDATE", targetType = "creative_effect", targetId = soundId, targetName = playing.name)
    }
}

class SetPitchSpeedTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_pitch_speed"
    override val description = "调整音乐或白噪音的播放速度和音调。速度范围 0.1-5.0（1.0=原速），音调范围 -12 到 +12 半音（0=原调）。" +
        "未指定具体数值时使用固定步进：速度±0.25x，音调±1 半音。音乐与白噪音速度完全独立。" +
        "算法说明：基于 SoundTouch WSOLA 时域拉伸，速度与音调解耦，变速不变调 / 变调不变速。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "target" to ToolProperty("string", "目标类型：music(音乐) 或 white_noise(白噪音)", enum = listOf("music", "white_noise")),
            "sound_id" to ToolProperty("string", "白噪音声音ID（target=white_noise 时必填）"),
            "parameter" to ToolProperty("string", "调整参数：speed(速度) 或 pitch(音调)", enum = listOf("speed", "pitch")),
            "action" to ToolProperty("string", "操作：set(设为绝对值) / up(增加步进) / down(减少步进)", enum = listOf("set", "up", "down")),
            "value" to ToolProperty("number", "绝对值（action=set 时必填）。speed: 0.1-5.0，pitch: -12 到 +12")
        ), required = listOf("target", "parameter", "action")
    )
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val target = params.getString("target")
        val parameter = params.getString("parameter")
        val action = params.getString("action")

        when (target) {
            "music" -> return adjustMusic(parameter, action, params)
            "white_noise" -> return adjustWhiteNoise(parameter, action, params)
            else -> return ToolResult.Error("未知目标类型：$target，支持 music 或 white_noise")
        }
    }

    private fun adjustMusic(parameter: String, action: String, params: JSONObject): ToolResult {
        val track = com.bicy.whitenoise.music.MusicPlayerController.state.value.currentTrack
            ?: return ToolResult.Error("当前没有播放中的音乐")
        val soundId = com.bicy.whitenoise.music.MusicCacheManager.getSoundId(track.id)

        return when (parameter) {
            "speed" -> {
                val current = com.bicy.whitenoise.storage.music.MusicStorage.getSpeed()
                val newValue = resolveValue(action, params, current, 0.25f, 0.1f, 5.0f)
                com.bicy.whitenoise.storage.music.MusicStorage.updateEffectIntensity("speed", newValue)
                com.bicy.whitenoise.audio.OboeAudioEngine.setPlaybackSpeed(soundId, newValue)
                ToolResult.Success("「${track.title}」播放速度已设为 ${String.format("%.2f", newValue)}x",
                    operationType = "UPDATE", targetType = "music_speed", targetId = track.id, targetName = track.title)
            }
            "pitch" -> {
                val current = com.bicy.whitenoise.storage.music.MusicStorage.getPitch()
                val newValue = resolveValue(action, params, current, 1f, -12f, 12f)
                com.bicy.whitenoise.storage.music.MusicStorage.updateEffectIntensity("pitch", newValue)
                com.bicy.whitenoise.audio.OboeAudioEngine.setPitchShift(soundId, newValue)
                ToolResult.Success("「${track.title}」音调已设为 ${newValue.toInt()} 半音",
                    operationType = "UPDATE", targetType = "music_pitch", targetId = track.id, targetName = track.title)
            }
            else -> ToolResult.Error("未知参数：$parameter，支持 speed 或 pitch")
        }
    }

    private fun adjustWhiteNoise(parameter: String, action: String, params: JSONObject): ToolResult {
        val soundId = params.optString("sound_id", "")
        if (soundId.isEmpty()) return ToolResult.Error("白噪音调整需提供 sound_id")
        val playing = vm.playingSounds.value.find { it.id == soundId }
            ?: return ToolResult.Error("声音未在播放：$soundId")

        val savedSpeed = com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage.getPlaybackState()
            .sounds.find { it.id == soundId }?.playbackSpeed ?: 1f
        val savedPitch = com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage.getPlaybackState()
            .sounds.find { it.id == soundId }?.pitchShift ?: 0f

        return when (parameter) {
            "speed" -> {
                val newValue = resolveValue(action, params, savedSpeed, 0.25f, 0.1f, 5.0f)
                com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage.updatePlayingSoundSpeed(soundId, newValue, savedPitch)
                com.bicy.whitenoise.audio.OboeAudioEngine.setPlaybackSpeed(soundId, newValue)
                ToolResult.Success("「${playing.name}」播放速度已设为 ${String.format("%.2f", newValue)}x",
                    operationType = "UPDATE", targetType = "white_noise_speed", targetId = soundId, targetName = playing.name)
            }
            "pitch" -> {
                val newValue = resolveValue(action, params, savedPitch, 1f, -12f, 12f)
                com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage.updatePlayingSoundSpeed(soundId, savedSpeed, newValue)
                com.bicy.whitenoise.audio.OboeAudioEngine.setPitchShift(soundId, newValue)
                ToolResult.Success("「${playing.name}」音调已设为 ${newValue.toInt()} 半音",
                    operationType = "UPDATE", targetType = "white_noise_pitch", targetId = soundId, targetName = playing.name)
            }
            else -> ToolResult.Error("未知参数：$parameter，支持 speed 或 pitch")
        }
    }

    /** 解析目标值：set=绝对值，up=当前+步进，down=当前-步进 */
    private fun resolveValue(action: String, params: JSONObject, current: Float, step: Float, min: Float, max: Float): Float {
        return when (action) {
            "set" -> (params.opt("value") as? Number)?.toFloat()?.coerceIn(min, max)
                ?: current
            "up" -> (current + step).coerceIn(min, max)
            "down" -> (current - step).coerceIn(min, max)
            else -> current
        }
    }
}
