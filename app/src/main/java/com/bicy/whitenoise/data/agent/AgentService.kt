package com.bicy.whitenoise.data.agent

import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject
import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.data.agent.appearance.AppearanceToolsPart.*
import com.bicy.whitenoise.data.agent.effect.EffectToolsPart.*
import com.bicy.whitenoise.data.agent.music.MusicToolsPart.*
import com.bicy.whitenoise.data.agent.preset.PresetToolsPart.*
import com.bicy.whitenoise.data.agent.system.SystemToolsPart.*
import com.bicy.whitenoise.data.agent.whitenoise.WhiteNoiseToolsPart.*

object AgentService {

    private var mainViewModel: MainViewModel? = null
    private var currentPageIndex: Int = 0

    private val tools: MutableMap<String, AgentTool> = mutableMapOf()

    fun init(mainViewModel: MainViewModel) {
        this.mainViewModel = mainViewModel
        registerTools()
    }

    fun updateCurrentPage(index: Int) {
        currentPageIndex = index
    }

    fun getCurrentContext(): ToolContext = ToolContext(
        mainViewModel = mainViewModel,
        currentPageIndex = currentPageIndex
    )

    private fun registerTools() {
        val vm = mainViewModel ?: return
        tools.clear()
        registerWhiteNoiseTools(vm)
        registerPresetTools(vm)
        registerEffectTools(vm)
        registerMusicTools(vm)
        registerAppearanceTools(vm)
        registerSystemTools(vm)
    }

    private fun registerWhiteNoiseTools(vm: MainViewModel) {
        tools["list_sounds"] = ListSoundsTool(vm)
        tools["play_sound"] = PlaySoundTool(vm)
        tools["stop_sound"] = StopSoundTool(vm)
        tools["set_volume"] = SetVolumeTool(vm)
        tools["toggle_pause"] = TogglePauseTool(vm)
        tools["get_playback_status"] = GetPlaybackStatusTool(vm)
    }

    private fun registerPresetTools(vm: MainViewModel) {
        tools["list_presets"] = ListPresetsTool(vm)
        tools["save_preset"] = SavePresetTool(vm)
        tools["load_preset"] = LoadPresetTool(vm)
        tools["delete_preset"] = DeletePresetTool(vm)
    }

    private fun registerEffectTools(vm: MainViewModel) {
        tools["set_reverb"] = SetReverbTool(vm)
        tools["set_creative_effect"] = SetCreativeEffectTool(vm)
        tools["set_equalizer_band"] = SetEqualizerBandTool(vm)
        tools["set_equalizer_curve"] = SetEqualizerCurveTool(vm)
        tools["set_white_noise_eq"] = SetWhiteNoiseEqTool(vm)
        tools["set_limiter"] = SetLimiterTool(vm)
        tools["set_pitch_speed"] = SetPitchSpeedTool(vm)
        tools["get_algorithm_info"] = GetAlgorithmInfoTool()
    }

    private fun registerMusicTools(vm: MainViewModel) {
        tools["list_music"] = ListMusicTool(vm)
        tools["get_music_playlist"] = GetMusicPlaylistTool(vm)
        tools["play_music_track"] = PlayMusicTrackTool(vm)
        tools["add_music_to_queue"] = AddMusicToQueueTool(vm)
        tools["music_play"] = MusicPlayTool(vm)
        tools["music_pause"] = MusicPauseTool(vm)
        tools["music_next"] = MusicNextTool(vm)
        tools["music_previous"] = MusicPreviousTool(vm)
        tools["music_seek"] = MusicSeekTool(vm)
        tools["music_set_volume"] = MusicSetVolumeTool(vm)
        tools["music_set_repeat_shuffle"] = MusicRepeatShuffleTool(vm)
    }

    private fun registerAppearanceTools(vm: MainViewModel) {
        tools["set_custom_theme_color"] = SetCustomThemeColorTool(vm)
        tools["set_background_image"] = SetBackgroundImageTool(vm)
        tools["set_liquid_glass_mode"] = SetLiquidGlassModeTool(vm)
        tools["set_glass_render_param"] = SetGlassRenderParamTool(vm)
    }

    private fun registerSystemTools(vm: MainViewModel) {
        tools["get_app_info"] = GetAppInfoTool(vm)
        tools["toggle_visualization"] = ToggleVisualizationTool(vm)
        tools["set_viz_sensitivity"] = SetVizSensitivityTool(vm)
    }

    fun getToolsDefinition(): List<Map<String, Any>> = tools.values.map { it.toOpenAITool() }

    suspend fun executeTool(
        toolName: String,
        arguments: JSONObject,
        context: ToolContext
    ): ToolResult {
        val tool = tools[toolName] ?: return ToolResult.Error("未知工具：$toolName")
        return runCatching { tool.execute(arguments, context) }
            .getOrElse { ToolResult.Error("工具执行失败：${it.message}") }
    }

    fun getToolDescription(toolName: String): String? = tools[toolName]?.description

    fun listAvailableTools(): List<String> = tools.keys.toList()
}

