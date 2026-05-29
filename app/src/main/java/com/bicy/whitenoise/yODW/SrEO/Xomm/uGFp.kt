package com.bicy.whitenoise.yODW.SrEO.Xomm

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bicy.whitenoise.R
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

@Composable
fun MusicCategory.getLabel(): String = when(this) {
    MusicCategory.CurrentList -> stringResource(R.string.current_list)
    MusicCategory.All -> stringResource(R.string.all)
    MusicCategory.Folder -> stringResource(R.string.folder)
    MusicCategory.Artist -> stringResource(R.string.artist)
    MusicCategory.Album -> stringResource(R.string.album)
    MusicCategory.Playlist -> stringResource(R.string.playlist)
}

@Composable
fun SortType.getLabel(): String = when(this) {
    SortType.Title -> stringResource(R.string.title)
    SortType.Artist -> stringResource(R.string.artist)
    SortType.Duration -> stringResource(R.string.duration)
    SortType.DateAdded -> stringResource(R.string.date_added)
}
