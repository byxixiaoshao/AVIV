package com.bicy.whitenoise.data.agent.music.MusicToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import com.bicy.whitenoise.music.MusicLibraryPart.MusicLibrary
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.ui.viewmodel.MainViewModel
import org.json.JSONObject

class ListMusicTool(private val vm: MainViewModel) : AgentTool {
    override val name = "list_music"
    override val description = "列出音乐库中的歌曲。支持按艺术家、专辑过滤，按关键词搜索。不传参数则列出全部歌曲（最多200首）。"
    override val parameters = ToolParameters(properties = mapOf(
        "search" to ToolProperty("string", "搜索关键词（匹配标题、艺术家、专辑）"),
        "artist" to ToolProperty("string", "按艺术家过滤"),
        "album" to ToolProperty("string", "按专辑过滤"),
        "limit" to ToolProperty("number", "返回数量上限（默认50，最大200）")
    ))
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val allTracks = MusicLibrary.tracks.value
        if (allTracks.isEmpty()) return ToolResult.Error("音乐库为空，请先导入音乐文件或确保已扫描完成")
        var result = allTracks; val filterDesc = mutableListOf<String>()
        val search = params.optString("search","").takeIf{it.isNotBlank()}
        if (search != null) { result = MusicLibrary.searchTracks(search); filterDesc.add("搜索\"$search\"") }
        val artist = params.optString("artist","").takeIf{it.isNotBlank()}
        if (artist != null) { result = result.filter{it.artist?.contains(artist,ignoreCase=true)==true}; filterDesc.add("艺术家\"$artist\"") }
        val album = params.optString("album","").takeIf{it.isNotBlank()}
        if (album != null) { result = result.filter{it.album?.contains(album,ignoreCase=true)==true}; filterDesc.add("专辑\"$album\"") }
        val limit = params.optInt("limit",50).coerceIn(1,200); val display = result.take(limit)
        if (display.isEmpty()) return ToolResult.Success("未找到匹配的歌曲${if(filterDesc.isNotEmpty())"（${filterDesc.joinToString()}）" else ""}")
        val sb = StringBuilder()
        if (filterDesc.isNotEmpty()) sb.append("筛选条件：${filterDesc.joinToString("，")}\n")
        sb.append("共找到 ${result.size} 首歌曲，显示前 ${display.size} 首：\n")
        display.forEachIndexed{i,track->val min=track.duration/60000;val sec=(track.duration%60000)/1000;sb.append("${i+1}. ${track.title}");if(!track.artist.isNullOrBlank())sb.append(" - ${track.artist}");sb.append(" [${min}:${sec.toString().padStart(2,'0')}]");sb.append(" (ID: ${track.id})\n")}
        if (search==null&&artist==null&&album==null){val artists=MusicLibrary.getAllArtists();val albums=MusicLibrary.getAllAlbums();sb.append("\n可用艺术家（${artists.size}）：${artists.take(10).joinToString("，")}");if(artists.size>10)sb.append("…");sb.append("\n可用专辑（${albums.size}）：${albums.take(10).joinToString("，")}");if(albums.size>10)sb.append("…")}
        return ToolResult.Success(sb.toString(), mapOf("totalCount" to result.size, "shownCount" to display.size))
    }
}

class GetMusicPlaylistTool(private val vm: MainViewModel) : AgentTool {
    override val name = "get_music_playlist"; override val description = "获取当前音乐播放列表，包括当前播放位置、播放模式等信息。"
    override val parameters = ToolParameters(properties = emptyMap())
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val state = MusicPlayerController.state.value
        if (state.playlist.isEmpty()) return ToolResult.Success("当前播放列表为空")
        val sb=StringBuilder();sb.append("播放模式：循环=${state.repeatMode}，随机=${state.shuffleMode}\n")
        if(state.isPlaying){val posMin=state.position/60000;val posSec=(state.position%60000)/1000;sb.append("状态：播放中（${posMin}:${posSec.toString().padStart(2,'0')}）\n")}else sb.append("状态：已暂停\n")
        sb.append("播放列表（${state.playlist.size} 首）：\n")
        state.playlist.forEachIndexed{i,track->val marker=if(i==state.playlistIndex)"▶ " else "  ";val min=track.duration/60000;val sec=(track.duration%60000)/1000;sb.append("${marker}${i+1}. ${track.title}");if(!track.artist.isNullOrBlank())sb.append(" - ${track.artist}");sb.append(" [${min}:${sec.toString().padStart(2,'0')}]");sb.append(" (ID: ${track.id})\n")}
        return ToolResult.Success(sb.toString(), mapOf("playlistSize" to state.playlist.size, "currentIndex" to state.playlistIndex, "isPlaying" to state.isPlaying))
    }
}

class PlayMusicTrackTool(private val vm: MainViewModel) : AgentTool {
    override val name = "play_music_track"; override val description = "播放指定的音乐曲目。传入曲目ID即可开始播放。"
    override val parameters = ToolParameters(properties = mapOf("track_id" to ToolProperty("string", "曲目ID，可通过 list_music 获取")), required = listOf("track_id"))
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val trackId = params.getString("track_id")
        val track = MusicLibrary.getTrackById(trackId) ?: return ToolResult.Error("未找到曲目ID：$trackId，请使用 list_music 获取正确的曲目ID")
        MusicPlayerController.playTrack(track)
        return ToolResult.Success("正在播放「${track.title}」${if(!track.artist.isNullOrBlank())" - ${track.artist}" else ""}", operationType="PLAY", targetType="music", targetId=track.id, targetName=track.title)
    }
}

class AddMusicToQueueTool(private val vm: MainViewModel) : AgentTool {
    override val name = "add_music_to_queue"; override val description = "将歌曲添加到播放队列末尾。可一次添加多首（track_ids 为逗号分隔的ID列表）。"
    override val parameters = ToolParameters(properties = mapOf("track_id" to ToolProperty("string","单首曲目ID"), "track_ids" to ToolProperty("string","多首曲目ID，逗号分隔（如 id1,id2,id3）")))
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val ids = mutableListOf<String>()
        params.optString("track_id","").takeIf{it.isNotBlank()}?.let{ids.add(it)}
        params.optString("track_ids","").takeIf{it.isNotBlank()}?.let{ids.addAll(it.split(",").map{s->s.trim()}.filter{s->s.isNotBlank()})}
        if(ids.isEmpty()) return ToolResult.Error("请提供 track_id 或 track_ids")
        val added=mutableListOf<String>();val notFound=mutableListOf<String>()
        for(id in ids){val track=MusicLibrary.getTrackById(id);if(track!=null){MusicPlayerController.addToQueue(track);added.add(track.title)}else notFound.add(id)}
        if(added.isEmpty()) return ToolResult.Error("所有曲目ID未找到：${notFound.joinToString()}")
        val msg="已添加 ${added.size} 首到播放列表：${added.take(5).joinToString("，")}"+if(added.size>5)"…" else ""+"（当前播放列表共 ${MusicPlayerController.state.value.playlist.size} 首）"
        return ToolResult.Success(msg, operationType="UPDATE", targetType="music_queue", targetId="queue", targetName="queue")
    }
}
