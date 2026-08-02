package com.bicy.whitenoise.data.agent.system.SystemToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject

class GetAppInfoTool(private val vm: MainViewModel) : AgentTool {
    override val name = "get_app_info"
    override val description = "获取应用当前状态信息，包括播放列表、音乐播放状态、主题、液态玻璃模式、可视化设置等。"
    override val parameters = ToolParameters(properties = emptyMap())

    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val sb = StringBuilder("应用当前状态：\n")
        val playing = vm.playingSounds.value
        sb.append("【白噪音】${if (playing.isEmpty()) "无播放" else "播放中 ${playing.size} 个"}\n")
        playing.take(5).forEach { sb.append("  - ${it.name}（音量 ${(it.volume * 100).toInt()}%）\n") }
        if (vm.isPaused.value) sb.append("  状态：已暂停\n")
        val musicState = MusicPlayerController.state.value
        val track = musicState.currentTrack
        sb.append("【音乐】")
        if (track != null) {
            sb.append("${if (musicState.isPlaying) "播放中" else "已暂停"}「${track.title}」")
            sb.append("（${musicState.position / 1000}s/${musicState.duration / 1000}s）\n")
            sb.append("  循环=${musicState.repeatMode}，随机=${musicState.shuffleMode}，列表=${musicState.playlist.size}首\n")
        } else {
            sb.append("无当前音轨\n")
        }
        val config = ConfigStorage.getConfig()
        sb.append("【主题】模式=${config.themeMode}，主题ID=${config.themeColorId}\n")
        if (config.themeColorId == "custom") sb.append("  自定义颜色已启用\n")
        sb.append("【可视化】白噪音=${if (config.vizWnEnabled) "开" else "关"}，")
        sb.append("音乐=${if (config.vizMusicEnabled) "开" else "关"}，")
        sb.append("闪光=${if (config.vizFlashEnabled) "开" else "关"}\n")
        return ToolResult.Success(sb.toString(), mapOf(
            "whiteNoiseCount" to playing.size,
            "musicPlaying" to (track != null && musicState.isPlaying),
            "themeMode" to config.themeMode
        ))
    }
}
