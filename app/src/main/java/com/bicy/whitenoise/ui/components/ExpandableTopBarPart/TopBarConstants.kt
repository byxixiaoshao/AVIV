package com.bicy.whitenoise.ui.components.ExpandableTopBarPart

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.bicy.whitenoise.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.Search

val DecelerateEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}

object AdditionalParamType {
    const val Pitch = 500
    const val Speed = 501
    const val HiFi = 502
    const val Distortion = 503
    const val StereoWidener = 700
    const val VirtualBass = 701
    const val MultibandCompressor = 702
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

// 侧栏尺寸：进一步缩小控件，让侧栏布局更紧凑
// 缩放比例：Control 0.67 (24/36), PlayButton 0.56 (36/64)
val SidebarAlbumSize = 60.dp
val SidebarAlbumIconSize = 24.dp
val SidebarTitleFontSize = 12.sp
val SidebarControlSize = 24.dp    // 从 28dp 减小到 24dp，比例 0.67
val SidebarPlayButtonSize = 18.dp // 从 44dp 减小到 36dp，比例 0.56

enum class PanelState {
    Main,
    Mixer,
    Playlist
}

enum class PanelType {
    Mixer, Playlist
}

enum class MusicCategory {
    Online, CurrentList, All, Folder, Artist, Album, Playlist
}

enum class SortType {
    Title, Artist, Duration, DateAdded
}

@Composable
fun MusicCategory.getLabel(): String = when(this) {
    MusicCategory.Online -> stringResource(R.string.online_music)
    MusicCategory.CurrentList -> stringResource(R.string.current_list)
    MusicCategory.All -> stringResource(R.string.all)
    MusicCategory.Folder -> stringResource(R.string.folder)
    MusicCategory.Artist -> stringResource(R.string.artist)
    MusicCategory.Album -> stringResource(R.string.album)
    MusicCategory.Playlist -> stringResource(R.string.playlist)
}

fun MusicCategory.getIcon(): ImageVector = when(this) {
    MusicCategory.Online -> Icons.Filled.Search
    MusicCategory.CurrentList -> Icons.AutoMirrored.Filled.List
    MusicCategory.All -> Icons.Filled.MusicNote
    MusicCategory.Folder -> Icons.Filled.Folder
    MusicCategory.Artist -> Icons.Filled.Person
    MusicCategory.Album -> Icons.Filled.Album
    MusicCategory.Playlist -> Icons.AutoMirrored.Filled.PlaylistPlay
}

@Composable
fun SortType.getLabel(): String = when(this) {
    SortType.Title -> stringResource(R.string.title)
    SortType.Artist -> stringResource(R.string.artist)
    SortType.Duration -> stringResource(R.string.duration)
    SortType.DateAdded -> stringResource(R.string.date_added)
}

/** 在线搜索渠道（硬编码） */
enum class SearchChannel(val source: String) {
    ALL(""), WY("wy"), KG("kg"), TX("tx"), KW("kw"), MG("mg")
}

@Composable
fun SearchChannel.getLabel(): String = when(this) {
    SearchChannel.ALL -> stringResource(R.string.search_channel_all)
    SearchChannel.WY -> stringResource(R.string.search_channel_wy)
    SearchChannel.KG -> stringResource(R.string.search_channel_kg)
    SearchChannel.TX -> stringResource(R.string.search_channel_tx)
    SearchChannel.KW -> stringResource(R.string.search_channel_kw)
    SearchChannel.MG -> stringResource(R.string.search_channel_mg)
}
