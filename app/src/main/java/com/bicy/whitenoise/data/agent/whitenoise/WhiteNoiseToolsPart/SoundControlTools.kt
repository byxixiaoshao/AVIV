package com.bicy.whitenoise.data.agent.whitenoise.WhiteNoiseToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject

class PlaySoundTool(private val vm: MainViewModel) : AgentTool {
    override val name = "play_sound"
    override val description = "播放指定的白噪音。需要声音ID，可通过 list_sounds 获取。"
    override val parameters = ToolParameters(properties = mapOf("sound_id" to ToolProperty("string","要播放的声音ID"), "sound_name" to ToolProperty("string","声音名称（用于日志显示）")), required = listOf("sound_id"))
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val soundId = params.getString("sound_id")
        val sound = vm.categories.value.flatMap{it.sounds}.find{it.id==soundId} ?: return ToolResult.Error("未找到声音ID：$soundId")
        val result = vm.ensureSoundPlayable(sound)
        return if(result.isSuccess) ToolResult.Success("已开始播放「${sound.name}」", operationType="PLAY", targetType="sound", targetId=soundId, targetName=sound.name)
        else ToolResult.Error("播放失败：${result.exceptionOrNull()?.message ?: "未知错误"}")
    }
}

class StopSoundTool(private val vm: MainViewModel) : AgentTool {
    override val name = "stop_sound"; override val description = "停止播放指定的白噪音。"
    override val parameters = ToolParameters(properties = mapOf("sound_id" to ToolProperty("string","要停止的声音ID")), required = listOf("sound_id"))
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val soundId = params.getString("sound_id")
        val playing = vm.playingSounds.value.find{it.id==soundId} ?: return ToolResult.Error("声音未在播放：$soundId")
        vm.removePlayingSound(soundId)
        return ToolResult.Success("已停止播放「${playing.name}」", operationType="STOP", targetType="sound", targetId=soundId, targetName=playing.name)
    }
}

class SetVolumeTool(private val vm: MainViewModel) : AgentTool {
    override val name = "set_volume"; override val description = "调整指定白噪音的音量。"
    override val parameters = ToolParameters(properties = mapOf("sound_id" to ToolProperty("string","声音ID"), "volume" to ToolProperty("number","音量值，范围 0.0 到 1.0")), required = listOf("sound_id","volume"))
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val soundId = params.getString("sound_id")
        val volume = params.getDouble("volume").toFloat().coerceIn(0f,1f)
        val playing = vm.playingSounds.value.find{it.id==soundId} ?: return ToolResult.Error("声音未在播放：$soundId")
        vm.setVolume(soundId, volume)
        return ToolResult.Success("「${playing.name}」音量已设置为 ${(volume*100).toInt()}%", operationType="UPDATE", targetType="sound_volume", targetId=soundId, targetName=playing.name)
    }
}

class TogglePauseTool(private val vm: MainViewModel) : AgentTool {
    override val name = "toggle_pause"; override val description = "切换白噪音播放的暂停/恢复状态。无需指定声音ID，影响整个播放列表。"
    override val parameters = ToolParameters(properties = emptyMap())
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        if(vm.playingSounds.value.isEmpty()) return ToolResult.Error("当前没有播放中的白噪音")
        vm.togglePauseResume()
        return ToolResult.Success(if(vm.isPaused.value)"白噪音已暂停" else "白噪音已恢复播放", operationType="TOGGLE", targetType="playback", targetId="all", targetName="playback_state")
    }
}

class GetPlaybackStatusTool(private val vm: MainViewModel) : AgentTool {
    override val name = "get_playback_status"; override val description = "获取当前白噪音播放列表的状态，包括正在播放的声音、音量、暂停状态等。"
    override val parameters = ToolParameters(properties = emptyMap())
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val playing = vm.playingSounds.value
        if(playing.isEmpty()) return ToolResult.Success("当前播放列表为空")
        val sb=StringBuilder("当前播放列表：\n")
        sb.append("- 暂停状态：${if(vm.isPaused.value)"已暂停" else "播放中"}\n")
        playing.forEachIndexed{i,s->sb.append("${i+1}. ${s.name} (ID: ${s.id})\n");sb.append("   音量: ${(s.volume*100).toInt()}%\n")}
        return ToolResult.Success(sb.toString(), mapOf("count" to playing.size, "isPaused" to vm.isPaused.value))
    }
}
