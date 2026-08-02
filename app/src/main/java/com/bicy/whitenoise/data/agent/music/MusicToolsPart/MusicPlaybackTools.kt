package com.bicy.whitenoise.data.agent.music.MusicToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject

class MusicPlayTool(private val vm: MainViewModel) : AgentTool {
    override val name = "music_play"; override val description = "播放音乐。若无当前音轨，将播放播放列表中的第一首。"
    override val parameters = ToolParameters(properties = emptyMap())
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val track = MusicPlayerController.state.value.currentTrack
            ?: return ToolResult.Error("当前没有可播放的音乐，请先在音乐页面选择歌曲")
        MusicPlayerController.play()
        return ToolResult.Success("正在播放「${track.title}」", operationType = "PLAY", targetType = "music", targetId = track.id, targetName = track.title)
    }
}

class MusicPauseTool(private val vm: MainViewModel) : AgentTool {
    override val name = "music_pause"; override val description = "暂停当前音乐播放。"
    override val parameters = ToolParameters(properties = emptyMap())
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val track = MusicPlayerController.state.value.currentTrack ?: return ToolResult.Error("当前没有正在播放的音乐")
        MusicPlayerController.pause()
        return ToolResult.Success("已暂停「${track.title}」", operationType = "PAUSE", targetType = "music", targetId = track.id, targetName = track.title)
    }
}

class MusicNextTool(private val vm: MainViewModel) : AgentTool {
    override val name = "music_next"; override val description = "切换到播放列表中的下一首音乐。"
    override val parameters = ToolParameters(properties = emptyMap())
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        if (MusicPlayerController.state.value.playlist.isEmpty()) return ToolResult.Error("播放列表为空")
        MusicPlayerController.next()
        val newTrack = MusicPlayerController.state.value.currentTrack
        return ToolResult.Success("已切换到下一首「${newTrack?.title ?: "未知"}」", operationType = "NEXT", targetType = "music", targetId = newTrack?.id ?: "unknown", targetName = newTrack?.title ?: "unknown")
    }
}

class MusicPreviousTool(private val vm: MainViewModel) : AgentTool {
    override val name = "music_previous"; override val description = "切换到播放列表中的上一首音乐。"
    override val parameters = ToolParameters(properties = emptyMap())
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        if (MusicPlayerController.state.value.playlist.isEmpty()) return ToolResult.Error("播放列表为空")
        MusicPlayerController.previous()
        val newTrack = MusicPlayerController.state.value.currentTrack
        return ToolResult.Success("已切换到上一首「${newTrack?.title ?: "未知"}」", operationType = "PREVIOUS", targetType = "music", targetId = newTrack?.id ?: "unknown", targetName = newTrack?.title ?: "unknown")
    }
}

class MusicSeekTool(private val vm: MainViewModel) : AgentTool {
    override val name = "music_seek"; override val description = "跳转到指定播放位置。position_ms 为毫秒数，也可通过 position_seconds 指定秒数。"
    override val parameters = ToolParameters(properties = mapOf("position_ms" to ToolProperty("number", "目标位置（毫秒）"), "position_seconds" to ToolProperty("number", "目标位置（秒），若提供则优先使用")))
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val track = MusicPlayerController.state.value.currentTrack ?: return ToolResult.Error("当前没有播放中的音乐")
        val positionMs = if (params.has("position_seconds")) (params.getDouble("position_seconds") * 1000).toLong()
        else if (params.has("position_ms")) params.getDouble("position_ms").toLong() else return ToolResult.Error("请提供 position_ms 或 position_seconds")
        val duration = MusicPlayerController.state.value.duration
        if (duration > 0 && positionMs > duration) return ToolResult.Error("跳转位置超出歌曲时长（${duration / 1000}秒）")
        MusicPlayerController.seekTo(positionMs.coerceAtLeast(0))
        val sec = positionMs / 1000
        return ToolResult.Success("「${track.title}」已跳转到 ${sec / 60}:${(sec % 60).toString().padStart(2, '0')}", operationType = "SEEK", targetType = "music", targetId = track.id, targetName = track.title)
    }
}

class MusicSetVolumeTool(private val vm: MainViewModel) : AgentTool {
    override val name = "music_set_volume"; override val description = "设置音乐播放音量。"
    override val parameters = ToolParameters(properties = mapOf("volume" to ToolProperty("number", "音量值，范围 0.0 到 1.0")), required = listOf("volume"))
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val volume = params.getDouble("volume").toFloat().coerceIn(0f, 1f)
        val track = MusicPlayerController.state.value.currentTrack
        MusicPlayerController.setVolume(volume)
        return ToolResult.Success("音乐音量已设置为 ${(volume * 100).toInt()}%", operationType = "UPDATE", targetType = "music_volume", targetId = track?.id ?: "music", targetName = track?.title ?: "music")
    }
}

class MusicRepeatShuffleTool(private val vm: MainViewModel) : AgentTool {
    override val name = "music_set_repeat_shuffle"
    override val description = "设置音乐循环和随机播放模式。repeat_mode 可选 off(不循环)/all(列表循环)/one(单曲循环)，shuffle 可选 on/off。"
    override val parameters = ToolParameters(properties = mapOf("repeat_mode" to ToolProperty("string", "循环模式", enum = listOf("off","all","one")), "shuffle" to ToolProperty("string", "随机播放", enum = listOf("on","off"))))
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val targetRepeat = params.optString("repeat_mode","").takeIf{it.isNotEmpty()}; val targetShuffle = params.optString("shuffle","").takeIf{it.isNotEmpty()}
        if (targetRepeat == null && targetShuffle == null) return ToolResult.Error("请至少提供 repeat_mode 或 shuffle 之一")
        val messages = mutableListOf<String>()
        if (targetRepeat != null) { val desired = when(targetRepeat){"off"->com.bicy.whitenoise.music.MusicRepeatMode.OFF;"all"->com.bicy.whitenoise.music.MusicRepeatMode.ALL;"one"->com.bicy.whitenoise.music.MusicRepeatMode.ONE;else->return ToolResult.Error("未知循环模式：$targetRepeat")}; var attempts=0; while(MusicPlayerController.state.value.repeatMode!=desired&&attempts<3){MusicPlayerController.toggleRepeatMode();attempts++}; messages.add("循环模式=$targetRepeat") }
        if (targetShuffle != null) { val desiredOn=targetShuffle=="on"; val currentOn=MusicPlayerController.state.value.shuffleMode==com.bicy.whitenoise.music.MusicShuffleMode.ON; if(desiredOn!=currentOn) MusicPlayerController.toggleShuffleMode(); messages.add("随机播放=$targetShuffle") }
        return ToolResult.Success("音乐模式已更新：${messages.joinToString("，")}", operationType = "UPDATE", targetType = "music_mode", targetId = "music", targetName = "playback_mode")
    }
}
