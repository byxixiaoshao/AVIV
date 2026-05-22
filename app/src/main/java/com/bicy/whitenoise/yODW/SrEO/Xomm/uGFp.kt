package com.bicy.whitenoise.yODW.SrEO.Xomm

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val DecelerateEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}

object AdditionalParamType {
    const val Pitch = 500
    const val Speed = 501
    const val HiFi = 502
    const val Distortion = 503
}

val AnimationEasing = FastOutSlowInEasing

val TopBarHeight = 48.dp
val TopBarPaddingTop = 8.dp
val TopBarPaddingHorizontal = 16.dp
val TopBarCornerRadius = 24.dp
val SidebarWidth = 100.dp

val MainAlbumSize = 200.dp
val MainAlbumIconSize = 80.dp
val MainTitleFontSize = 20.sp
val MainControlSize = 36.dp
val MainPlayButtonSize = 64.dp

val SidebarAlbumSize = 60.dp
val SidebarAlbumIconSize = 24.dp
val SidebarTitleFontSize = 12.sp
val SidebarControlSize = 28.dp
val SidebarPlayButtonSize = 44.dp

enum class PanelState {
    Main,
    Mixer,
    Playlist
}

enum class PanelType {
    Mixer, Playlist
}

enum class MusicCategory {
    CurrentList, All, Folder, Artist, Album, Playlist
}

enum class SortType {
    Title, Artist, Duration, DateAdded
}

fun MusicCategory.getLabel(): String = when(this) {
    MusicCategory.CurrentList -> "当前列表"
    MusicCategory.All -> "所有"
    MusicCategory.Folder -> "文件夹"
    MusicCategory.Artist -> "歌手"
    MusicCategory.Album -> "专辑"
    MusicCategory.Playlist -> "歌单"
}

fun SortType.getLabel(): String = when(this) {
    SortType.Title -> "标题"
    SortType.Artist -> "歌手"
    SortType.Duration -> "时长"
    SortType.DateAdded -> "添加时间"
}
