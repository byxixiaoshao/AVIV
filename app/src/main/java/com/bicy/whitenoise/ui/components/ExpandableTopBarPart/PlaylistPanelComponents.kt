package com.bicy.whitenoise.ui.components.ExpandableTopBarPart

import android.util.Log
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bicy.whitenoise.ui.adapters.CategoryAdapter
import com.bicy.whitenoise.ui.adapters.CategoryItem
import com.bicy.whitenoise.ui.views.NonInterceptRecyclerView
import com.bicy.whitenoise.ui.adapters.PlaylistAdapter
import com.bicy.whitenoise.ui.adapters.FolderContentAdapter
import com.bicy.whitenoise.ui.adapters.FolderItem
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.bicy.whitenoise.ui.components.FocusableEditText
import com.bicy.whitenoise.ui.components.toast.ToastManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bicy.whitenoise.R
import com.bicy.whitenoise.storage.music.MusicStorage
import com.bicy.whitenoise.onlinemusic.OnlineMusicStorage
import com.bicy.whitenoise.storage.playlist.PlaylistManagerPart.PlaylistManager
import com.bicy.whitenoise.storage.playlist.PlaylistManagerPart.UserPlaylist
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.music.MusicLibraryPart.MusicTrack
import com.bicy.whitenoise.onlinemusic.OnlineMusicController
import com.bicy.whitenoise.onlinemusic.SourceScriptManager
import com.bicy.whitenoise.onlinemusic.model.SourceModelsPart.MusicInfoOnline
import com.bicy.whitenoise.onlinemusic.model.SourceModelsPart.Sources
import com.bicy.whitenoise.utils.AudioMetadataReader
import com.bicy.whitenoise.ui.utils.LocalPlaylistNavigation
import com.bicy.whitenoise.ui.utils.LocalPlaylistNavigationHolder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PlaylistPanel(
    tracks: List<MusicTrack>,
    currentTrack: MusicTrack?,
    isScanning: Boolean,
    panelProgress: Float,
    playlist: List<MusicTrack>,
    playlistIndex: Int,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val renderStartTime = System.currentTimeMillis()

    var selectedCategory by remember { mutableStateOf(MusicCategory.CurrentList) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var selectedPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var sortType by remember { mutableStateOf(SortType.Title) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showSaveCurrentListDialog by remember { mutableStateOf(false) }

    // 歌曲操作菜单状态
    var showTrackOptionsFor by remember { mutableStateOf<MusicTrack?>(null) }
    var showAddToPlaylistFor by remember { mutableStateOf<MusicTrack?>(null) }

    // 歌单操作菜单状态
    var showPlaylistOptionsFor by remember { mutableStateOf<UserPlaylist?>(null) }
    var showDeletePlaylistConfirm by remember { mutableStateOf<UserPlaylist?>(null) }
    var showRenamePlaylistFor by remember { mutableStateOf<UserPlaylist?>(null) }
    
    // 在线搜索状态
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scriptManager = remember { SourceScriptManager.getInstance(context.applicationContext) }
    val onlineMusicController = remember { OnlineMusicController(context.applicationContext) }
    var searchQuery by remember { mutableStateOf("") }
    var searchChannel by remember { mutableStateOf(SearchChannel.ALL) }
    var searchResults by remember { mutableStateOf<List<MusicInfoOnline>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var showChannelMenu by remember { mutableStateOf(false) }
    
    val isOnlineMode = selectedCategory == MusicCategory.Online

    val folderPathStack = remember { mutableStateListOf<String>() }
    val currentFolderPath: String? = folderPathStack.lastOrNull()

    val userPlaylists by PlaylistManager.userPlaylists.collectAsState()
    val favorites by PlaylistManager.favorites.collectAsState()
    
    val topDirectories = remember {
        MusicStorage.getEnabledDirectories().map { it.path }
    }
    
    val artists = remember(tracks) {
        tracks.mapNotNull { it.artist }.distinct().sorted()
    }
    
    val albums = remember(tracks) {
        tracks.mapNotNull { it.album }.distinct().sorted()
    }
    
    val tracksByArtist = remember(tracks) {
        tracks.groupBy { it.artist }
    }
    
    val tracksByAlbum = remember(tracks) {
        tracks.groupBy { it.album }
    }
    
    // 异步扫描下载目录（不阻塞 UI）
    val scannedTracks = remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    LaunchedEffect(panelProgress > 0.1f, tracks) {
        if (panelProgress > 0.1f) {
            scannedTracks.value = withContext(Dispatchers.IO) {
                scanDirectoryForTracks(onlineMusicController.downloadDir)
            }
        }
    }

    val tracksByFolder = remember(tracks, scannedTracks.value) {
        val allTracks = tracks + scannedTracks.value
        allTracks.groupBy { it.path.substringBeforeLast('/') }
    }
    
    val (subDirectories, tracksInCurrentFolder) = remember(tracksByFolder, currentFolderPath) {
        if (currentFolderPath == null) {
            Pair(emptyList(), emptyList())
        } else {
            val directTracks = tracksByFolder[currentFolderPath] ?: emptyList()
            
            val subDirs = tracksByFolder.keys
                .filter { it.startsWith(currentFolderPath) && it != currentFolderPath }
                .map { fullPath ->
                    val relativePath = fullPath.removePrefix(currentFolderPath).removePrefix("/")
                    relativePath.substringBefore('/')
                }
                .distinct()
                .sorted()
            
            Pair(subDirs, directTracks)
        }
    }
    
    val artistTracks = remember(tracksByArtist, selectedArtist) {
        if (selectedArtist == null) tracks
        else tracksByArtist[selectedArtist] ?: emptyList()
    }
    
    val albumTracks = remember(tracksByAlbum, selectedAlbum) {
        if (selectedAlbum == null) tracks
        else tracksByAlbum[selectedAlbum] ?: emptyList()
    }
    
    val playlistTracks = remember(selectedPlaylist, tracks) {
        val pl = selectedPlaylist
        if (pl == null) {
            emptyList()
        } else {
            PlaylistManager.getTracksForPlaylist(pl, tracks)
        }
    }

    // 加载歌单中的在线曲目
    val onlineTracksInPlaylist = remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    LaunchedEffect(selectedPlaylist) {
        val pl = selectedPlaylist ?: return@LaunchedEffect
        val onlineIds = pl.trackIds.filter { it.startsWith("online_") }
        if (onlineIds.isNotEmpty()) {
            onlineTracksInPlaylist.value = withContext(Dispatchers.IO) {
                onlineIds.mapNotNull { OnlineMusicStorage.getTrackById(it) }
            }
        } else {
            onlineTracksInPlaylist.value = emptyList()
        }
    }

    val playlistTracksWithOnline = remember(playlistTracks, onlineTracksInPlaylist.value) {
        playlistTracks + onlineTracksInPlaylist.value
    }
    
    val favoriteTracks = remember(favorites, tracks, selectedCategory, selectedPlaylist) {
        if (selectedCategory == MusicCategory.Playlist && selectedPlaylist == null) {
            favorites?.let { fav -> PlaylistManager.getTracksForPlaylist(fav, tracks) } ?: emptyList()
        } else {
            emptyList()
        }
    }

    // 加载收藏中的在线曲目
    val onlineTracksInFavorites = remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    LaunchedEffect(favorites, selectedCategory) {
        if (selectedCategory == MusicCategory.Playlist) {
            val fav = favorites ?: return@LaunchedEffect
            val onlineIds = fav.trackIds.filter { it.startsWith("online_") }
            if (onlineIds.isNotEmpty()) {
                onlineTracksInFavorites.value = withContext(Dispatchers.IO) {
                    onlineIds.mapNotNull { OnlineMusicStorage.getTrackById(it) }
                }
            } else {
                onlineTracksInFavorites.value = emptyList()
            }
        } else {
            onlineTracksInFavorites.value = emptyList()
        }
    }

    val favoriteTracksWithOnline = remember(favoriteTracks, onlineTracksInFavorites.value) {
        favoriteTracks + onlineTracksInFavorites.value
    }
    
    val baseTracks = remember(selectedCategory, tracks, tracksInCurrentFolder, artistTracks, albumTracks, playlistTracksWithOnline) {
        when(selectedCategory) {
            MusicCategory.Online -> emptyList()
            MusicCategory.CurrentList -> emptyList()
            MusicCategory.All -> tracks
            MusicCategory.Folder -> tracksInCurrentFolder
            MusicCategory.Artist -> artistTracks
            MusicCategory.Album -> albumTracks
            MusicCategory.Playlist -> playlistTracksWithOnline
        }
    }
    
    val unsortedTracks = remember(selectedCategory, selectedPlaylist, playlist, favoriteTracksWithOnline, baseTracks) {
        when {
            selectedCategory == MusicCategory.CurrentList -> playlist
            selectedCategory == MusicCategory.Playlist && selectedPlaylist == null -> favoriteTracksWithOnline
            else -> baseTracks
        }
    }
    
    val displayTracks = remember(unsortedTracks, sortType) {
        val startTime = System.currentTimeMillis()
        val result = when(sortType) {
            SortType.Title -> unsortedTracks.sortedBy { it.title.lowercase() }
            SortType.Artist -> unsortedTracks.sortedBy { it.artist?.lowercase() ?: "" }
            SortType.Duration -> unsortedTracks.sortedBy { it.duration }
            SortType.DateAdded -> unsortedTracks.sortedByDescending { it.dateAdded }
        }
        val elapsed = System.currentTimeMillis() - startTime
        Log.d("PlaylistDebug", "displayTracks computed: ${result.size} tracks in ${elapsed}ms")
        result
    }
    
    LaunchedEffect(displayTracks) {
        val elapsed = System.currentTimeMillis() - renderStartTime
        Log.d("PlaylistDebug", "PlaylistPanel render: ${elapsed}ms, tracks=${tracks.size}, display=${displayTracks.size}")
    }
    
    val hasSubPage = remember(selectedCategory, folderPathStack, selectedArtist, selectedAlbum, selectedPlaylist) {
        when {
            selectedCategory == MusicCategory.Folder && folderPathStack.isNotEmpty() -> true
            selectedCategory == MusicCategory.Playlist && selectedPlaylist != null -> true
            selectedArtist != null -> true
            selectedAlbum != null -> true
            else -> false
        }
    }
    
    val navigateBack: () -> Unit = remember {
        {
            when {
                selectedCategory == MusicCategory.Folder && folderPathStack.isNotEmpty() -> {
                    folderPathStack.removeLast()
                }
                selectedCategory == MusicCategory.Playlist && selectedPlaylist != null -> {
                    selectedPlaylist = null
                }
                selectedArtist != null -> selectedArtist = null
                selectedAlbum != null -> selectedAlbum = null
            }
        }
    }
    
    val playlistNavigationHolder = LocalPlaylistNavigationHolder.current
    LaunchedEffect(hasSubPage, navigateBack, playlistNavigationHolder) {
        playlistNavigationHolder?.updateState(hasSubPage, navigateBack)
    }
    
    val playlistNavigation = LocalPlaylistNavigation.current
    
    BackHandler(enabled = hasSubPage) {
        navigateBack()
    }
    
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                PlaylistManager.createPlaylist(name)
                showCreatePlaylistDialog = false
                ToastManager.success("已创建「$name」")
            }
        )
    }
    
    if (showSaveCurrentListDialog) {
        SaveCurrentListDialog(
            currentPlaylist = playlist,
            onDismiss = { showSaveCurrentListDialog = false },
            onSave = { name ->
                val trackIds = playlist.map { it.id }
                val newPlaylist = PlaylistManager.createPlaylist(name)
                PlaylistManager.addToPlaylist(newPlaylist.id, trackIds)
                showSaveCurrentListDialog = false
                ToastManager.success("已保存为「$name」")
            }
        )
    }

    // 歌曲操作菜单对话框
    if (showTrackOptionsFor != null) {
        val track = showTrackOptionsFor!!
        val isFavorite = PlaylistManager.isFavorite(track.id)
        val isInPlaylist = selectedPlaylist != null && selectedPlaylist?.id != "favorites"

        TrackOptionsMenuDialog(
            track = track,
            isFavorite = isFavorite,
            isInPlaylist = isInPlaylist,
            onDismiss = { showTrackOptionsFor = null },
            onAddToPlaylist = { showAddToPlaylistFor = track },
            onToggleFavorite = {
                val wasFavorite = PlaylistManager.isFavorite(track.id)
                PlaylistManager.toggleFavorite(track.id)
                ToastManager.success(if (wasFavorite) "已从收藏移除" else "已添加到收藏")
            },
            onRemoveFromPlaylist = if (isInPlaylist && selectedPlaylist != null) {
                { PlaylistManager.removeFromPlaylist(selectedPlaylist!!.id, listOf(track.id))
                  ToastManager.success("已从播放列表移除") }
            } else null
        )
    }

    // 添加到歌单选择对话框
    if (showAddToPlaylistFor != null) {
        AddToPlaylistDialog(
            track = showAddToPlaylistFor!!,
            onDismiss = { showAddToPlaylistFor = null },
            onAddToPlaylist = { playlistId ->
                PlaylistManager.addToPlaylist(playlistId, listOf(showAddToPlaylistFor!!.id))
                ToastManager.success("已添加到歌单")
            }
        )
    }

    // 歌单操作菜单对话框（直接显示删除和重命名选项）
    if (showPlaylistOptionsFor != null) {
        AlertDialog(
            onDismissRequest = { showPlaylistOptionsFor = null },
            title = { Text(showPlaylistOptionsFor!!.name) },
            text = {
                Column {
                    TextButton(onClick = {
                        showRenamePlaylistFor = showPlaylistOptionsFor
                        showPlaylistOptionsFor = null
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.rename_playlist))
                    }
                    TextButton(onClick = {
                        showDeletePlaylistConfirm = showPlaylistOptionsFor
                        showPlaylistOptionsFor = null
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.delete_playlist), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlaylistOptionsFor = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 删除歌单确认对话框
    if (showDeletePlaylistConfirm != null) {
        DeletePlaylistConfirmDialog(
            playlistName = showDeletePlaylistConfirm!!.name,
            onDismiss = { showDeletePlaylistConfirm = null },
            onConfirm = {
                val name = showDeletePlaylistConfirm!!.name
                PlaylistManager.deletePlaylist(showDeletePlaylistConfirm!!.id)
                ToastManager.success("已删除「$name」")
            }
        )
    }

    // 重命名歌单对话框
    if (showRenamePlaylistFor != null) {
        RenamePlaylistDialog(
            currentName = showRenamePlaylistFor!!.name,
            onDismiss = { showRenamePlaylistFor = null },
            onRename = { newName ->
                PlaylistManager.renamePlaylist(showRenamePlaylistFor!!.id, newName)
                ToastManager.success("已重命名为「$newName」")
            }
        )
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // 内容区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val listKey = remember(selectedCategory, currentFolderPath, selectedArtist, selectedAlbum, selectedPlaylist, isScanning, playlist.isEmpty(), displayTracks.isEmpty()) {
                    when {
                        isScanning -> "scanning"
                        selectedCategory == MusicCategory.CurrentList && playlist.isEmpty() -> "current_empty"
                        selectedCategory == MusicCategory.CurrentList -> "current"
                        selectedCategory == MusicCategory.All && displayTracks.isEmpty() -> "all_empty"
                        selectedCategory == MusicCategory.All -> "all"
                        selectedCategory == MusicCategory.Folder && folderPathStack.isEmpty() -> "folder_top"
                        selectedCategory == MusicCategory.Folder && currentFolderPath != null -> "folder_$currentFolderPath"
                        selectedCategory == MusicCategory.Artist && selectedArtist == null -> "artist_list"
                        selectedCategory == MusicCategory.Artist -> "artist_$selectedArtist"
                        selectedCategory == MusicCategory.Album && selectedAlbum == null -> "album_list"
                        selectedCategory == MusicCategory.Album -> "album_$selectedAlbum"
                        selectedCategory == MusicCategory.Playlist && selectedPlaylist == null -> "playlist_list"
                        selectedCategory == MusicCategory.Playlist -> "playlist_${selectedPlaylist?.id}"
                        else -> "empty"
                    }
                }
                
                AnimatedContent(
                    targetState = listKey,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "list_animated_content"
                ) { key ->
                    when {
                        selectedCategory == MusicCategory.Online -> {
                            OnlineSearchContent(
                                searchQuery = searchQuery,
                                searchResults = searchResults,
                                isSearching = isSearching,
                                searchError = searchError,
                                searchChannel = searchChannel,
                                onQueryChange = { searchQuery = it },
                                onSearch = {
                                    scope.launch {
                                        isSearching = true
                                        searchError = null
                                        searchResults = emptyList()
                                        try {
                                            val results = performOnlineSearch(
                                                scriptManager, searchChannel, searchQuery
                                            )
                                            searchResults = results
                                        } catch (e: Exception) {
                                            searchError = e.message ?: "搜索失败"
                                        } finally {
                                            isSearching = false
                                        }
                                    }
                                },
                                onPlayClick = { musicInfo ->
                                    scope.launch {
                                        onlineMusicController.playOnline(musicInfo)
                                    }
                                }
                            )
                        }
                        isScanning -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.scanning_music),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        selectedCategory == MusicCategory.CurrentList -> {
                            if (playlist.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_music_playing),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                TrackList(
                                    tracks = displayTracks,
                                    currentTrack = currentTrack,
                                    onMoreClick = { track -> showTrackOptionsFor = track }
                                )
                            }
                        }
                        selectedCategory == MusicCategory.Folder && folderPathStack.isEmpty() -> {
                            TopDirectorySelectionList(
                                directories = topDirectories,
                                downloadDir = onlineMusicController.downloadDir.absolutePath,
                                onDirectoryClick = { path ->
                                    folderPathStack.add(path)
                                }
                            )
                        }
                        selectedCategory == MusicCategory.Folder && currentFolderPath != null -> {
                            FolderContentList(
                                subDirectories = subDirectories,
                                tracks = tracksInCurrentFolder,
                                currentTrack = currentTrack,
                                onSubDirectoryClick = { subDirName ->
                                    val newPath = "$currentFolderPath/$subDirName"
                                    folderPathStack.add(newPath)
                                },
                                onTrackClick = { track ->
                                    val index = tracksInCurrentFolder.indexOf(track)
                                    if (index >= 0) {
                                        MusicPlayerController.setPlaylist(tracksInCurrentFolder, index)
                                        MusicPlayerController.play()
                                    }
                                },
                                onMoreClick = { track -> showTrackOptionsFor = track }
                            )
                        }
                        selectedCategory == MusicCategory.Artist && selectedArtist == null -> {
                            CategorySelectionList(
                                items = artists,
                                onItemClick = { selectedArtist = it }
                            )
                        }
                        selectedCategory == MusicCategory.Album && selectedAlbum == null -> {
                            CategorySelectionList(
                                items = albums,
                                onItemClick = { selectedAlbum = it }
                            )
                        }
                        selectedCategory == MusicCategory.Playlist && selectedPlaylist == null -> {
                            PlaylistSelectionList(
                                favorites = favorites,
                                userPlaylists = userPlaylists,
                                tracks = tracks,
                                onFavoritesClick = {
                                    selectedPlaylist = favorites
                                },
                                onPlaylistClick = { playlist ->
                                    selectedPlaylist = playlist
                                },
                                onCreatePlaylist = {
                                    showCreatePlaylistDialog = true
                                },
                                onPlaylistMoreClick = { playlist ->
                                    showPlaylistOptionsFor = playlist
                                }
                            )
                        }
                        displayTracks.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.still_empty),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        else -> {
                            TrackList(
                                tracks = displayTracks,
                                currentTrack = currentTrack,
                                onMoreClick = { track -> showTrackOptionsFor = track }
                            )
                        }
                    }
                }
            }
            
            CategorySidebar(
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    selectedCategory = category
                    folderPathStack.clear()
                    selectedArtist = null
                    selectedAlbum = null
                    selectedPlaylist = null
                }
            )
        }
        
        // 底部栏（全宽，横穿整个面板）
        if (isOnlineMode) {
            // 在线搜索模式：渠道选择在上，输入框+搜索按钮在下
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(10f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // 搜索渠道选择行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.search_channel) + ":",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        Row(
                            modifier = Modifier.clickable { showChannelMenu = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = searchChannel.getLabel(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showChannelMenu,
                            onDismissRequest = { showChannelMenu = false }
                        ) {
                            SearchChannel.entries.forEach { channel ->
                                DropdownMenuItem(
                                    text = { Text(channel.getLabel()) },
                                    onClick = {
                                        searchChannel = channel
                                        showChannelMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                // 搜索输入行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FocusableEditText(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = stringResource(R.string.search_online_music),
                        singleLine = true,
                        enabled = true,
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                scope.launch {
                                    isSearching = true
                                    searchError = null
                                    searchResults = emptyList()
                                    try {
                                        val results = performOnlineSearch(
                                            scriptManager, searchChannel, searchQuery
                                        )
                                        searchResults = results
                                    } catch (e: Exception) {
                                        searchError = e.message ?: "搜索失败"
                                    } finally {
                                        isSearching = false
                                    }
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (searchQuery.isNotBlank()) {
                            scope.launch {
                                isSearching = true
                                searchError = null
                                searchResults = emptyList()
                                try {
                                    val results = performOnlineSearch(
                                        scriptManager, searchChannel, searchQuery
                                    )
                                    searchResults = results
                                } catch (e: Exception) {
                                    searchError = e.message ?: "搜索失败"
                                } finally {
                                    isSearching = false
                                }
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else {
            // 普通模式：返回按钮 + 标题 + 排序
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (hasSubPage) {
                            when {
                                selectedCategory == MusicCategory.Folder && folderPathStack.isNotEmpty() -> {
                                    folderPathStack.removeLast()
                                }
                                selectedCategory == MusicCategory.Playlist && selectedPlaylist != null -> {
                                    selectedPlaylist = null
                                }
                                selectedArtist != null -> selectedArtist = null
                                selectedAlbum != null -> selectedAlbum = null
                            }
                        } else {
                            onBack()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                when {
                    selectedCategory == MusicCategory.Folder && currentFolderPath != null -> {
                        Text(
                            text = currentFolderPath.substringAfterLast('/'),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    selectedCategory == MusicCategory.Playlist && selectedPlaylist != null -> {
                        Text(
                            text = selectedPlaylist!!.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    selectedCategory == MusicCategory.CurrentList -> {
                        Text(
                            text = stringResource(R.string.current_playing_count, playlist.size),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (playlist.isNotEmpty()) {
                            IconButton(
                                onClick = { showSaveCurrentListDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = stringResource(R.string.save_as_playlist)
                                )
                            }
                        }
                    }
                    else -> Spacer(modifier = Modifier.weight(1f))
                }
                
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.sort)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(when(type) {
                                    SortType.Title -> stringResource(R.string.title)
                                    SortType.Artist -> stringResource(R.string.artist)
                                    SortType.Duration -> stringResource(R.string.duration)
                                    SortType.DateAdded -> stringResource(R.string.date_added)
                                }) },
                                onClick = {
                                    sortType = type
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 在线搜索内容 */
@Composable
fun OnlineSearchContent(
    searchQuery: String,
    searchResults: List<MusicInfoOnline>,
    isSearching: Boolean,
    searchError: String?,
    searchChannel: SearchChannel,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPlayClick: (MusicInfoOnline) -> Unit
) {
    // 更多操作弹窗状态
    var showMoreOptionsFor by remember { mutableStateOf<MusicInfoOnline?>(null) }
    var showAddToPlaylistForOnline by remember { mutableStateOf<MusicInfoOnline?>(null) }
    val favorites by PlaylistManager.favorites.collectAsState()
    
    // 更多操作弹窗
    if (showMoreOptionsFor != null) {
        val info = showMoreOptionsFor!!
        val trackId = "online_${info.source}_${info.songId}"
        val isFav = favorites?.trackIds?.contains(trackId) == true
        OnlineMusicMoreOptionsDialog(
            musicInfo = info,
            onDismiss = { showMoreOptionsFor = null },
            onPlayClick = {
                onPlayClick(info)
                showMoreOptionsFor = null
            },
            onAddToPlaylistClick = {
                showAddToPlaylistForOnline = info
            },
            isFavorite = isFav,
            onToggleFavorite = {
                val wasFav = isFav
                PlaylistManager.toggleFavorite(trackId)
                ToastManager.success(if (wasFav) "已从收藏移除" else "已添加到收藏")
            }
        )
    }

    // 添加到歌单（在线音乐）
    if (showAddToPlaylistForOnline != null) {
        val info = showAddToPlaylistForOnline!!
        val onlineTrack = MusicTrack(
            id = "online_${info.source}_${info.songId}",
            path = "",
            title = info.name,
            artist = info.singer,
            album = info.albumName,
            duration = 0L,
            isOnline = true,
            streamUrl = null,
            source = info.source,
            dateAdded = System.currentTimeMillis(),
            albumArt = null,
            mediaStoreId = 0
        )
        AddToPlaylistDialog(
            track = onlineTrack,
            onDismiss = { showAddToPlaylistForOnline = null },
            onAddToPlaylist = { playlistId ->
                // 保存在线曲目元数据到数据库
                @OptIn(DelicateCoroutinesApi::class)
                kotlinx.coroutines.GlobalScope.launch {
                    OnlineMusicStorage.saveOnlineTrack(onlineTrack)
                }
                PlaylistManager.addToPlaylist(playlistId, listOf(onlineTrack.id))
                showAddToPlaylistForOnline = null
            }
        )
    }
    
    when {
        isSearching -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.searching),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        searchError != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = searchError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onSearch) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
        searchQuery.isBlank() || searchResults.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchResults.isEmpty() && searchQuery.isNotBlank())
                        stringResource(R.string.no_search_results)
                    else stringResource(R.string.search_online_music_hint),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
            ) {
                items(searchResults, key = { "${it.source}_${it.songId}" }) { musicInfo ->
                    OnlineSearchItem(
                        musicInfo = musicInfo,
                        onPlayClick = { onPlayClick(musicInfo) },
                        onMoreClick = { showMoreOptionsFor = it }
                    )
                }
            }
        }
    }
}

/** 在线音乐更多操作弹窗 */
@Composable
fun OnlineMusicMoreOptionsDialog(
    musicInfo: MusicInfoOnline,
    onDismiss: () -> Unit,
    onPlayClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = musicInfo.name) },
        text = {
            Column {
                Text(
                    text = musicInfo.singer,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = musicInfo.albumName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = {
                    onPlayClick()
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.play))
                }

                TextButton(onClick = {
                    onAddToPlaylistClick()
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_to_playlist))
                }

                TextButton(onClick = {
                    onToggleFavorite()
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isFavorite) stringResource(R.string.remove_from_favorites)
                         else stringResource(R.string.add_to_favorites))
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun OnlineSearchItem(
    musicInfo: MusicInfoOnline,
    onPlayClick: () -> Unit,
    onMoreClick: (MusicInfoOnline) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = musicInfo.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = musicInfo.singer,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = musicInfo.source.uppercase(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        // 更多操作按钮
        IconButton(
            onClick = { onMoreClick(musicInfo) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "更多操作",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/** 执行在线搜索 */
suspend fun performOnlineSearch(
    scriptManager: SourceScriptManager,
    channel: SearchChannel,
    keyword: String
): List<MusicInfoOnline> {
    if (keyword.isBlank()) return emptyList()

    // 确定搜索源
    val sources = if (channel == SearchChannel.ALL) {
        listOf(Sources.KW, Sources.KG, Sources.TX, Sources.WY, Sources.MG)
    } else {
        listOf(channel.source)
    }

    Log.d("OnlineSearch", "开始搜索，关键词: $keyword，源: $sources")

    // 优先使用内置 SDK 搜索
    val allResults = mutableListOf<MusicInfoOnline>()
    for (source in sources) {
        try {
            // 内置 SDK 搜索
            val results = com.bicy.whitenoise.onlinemusic.OnlineSearchEngine.search(source, keyword)
            if (results.isNotEmpty()) {
                allResults.addAll(results)
                Log.d("OnlineSearch", "[内置SDK] 源 $source 搜索到 ${results.size} 条结果")
            }
        } catch (e: Exception) {
            Log.w("OnlineSearch", "[内置SDK] 搜索 $source 失败: ${e.message}")
            
            // 内置 SDK 失败时，尝试脚本搜索
            try {
                if (scriptManager.isScriptActive()) {
                    val scriptResults = scriptManager.searchMusic(source, keyword)
                    if (scriptResults.isNotEmpty()) {
                        allResults.addAll(scriptResults)
                        Log.d("OnlineSearch", "[脚本] 源 $source 搜索到 ${scriptResults.size} 条结果")
                    }
                }
            } catch (e2: Exception) {
                Log.w("OnlineSearch", "[脚本] 搜索 $source 失败: ${e2.message}")
            }
        }
    }

    Log.d("OnlineSearch", "搜索完成，共 ${allResults.size} 条结果")
    return allResults
}

@Composable
fun CategorySidebar(
    selectedCategory: MusicCategory,
    onCategorySelected: (MusicCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MusicCategory.entries.forEach { category ->
            CategoryTab(
                imageVector = category.getIcon(),
                contentDescription = category.getLabel(),
                isSelected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CategoryTab(
    imageVector: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }
    
    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
    
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun TopDirectorySelectionList(
    directories: List<String>,
    downloadDir: String,
    onDirectoryClick: (String) -> Unit
) {
    // 构建完整文件夹列表：固定1个虚拟文件夹 + 用户添加的目录
    val allDirectories = remember(directories, downloadDir) {
        mutableListOf<String>().apply {
            // 固定文件夹始终在最前面
            add(downloadDir)  // 下载音乐
            addAll(directories)
        }
    }
    
    if (allDirectories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.please_add_music_directory),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        val adapter = remember { 
            CategoryAdapter(initialOnItemClick = { item ->
                onDirectoryClick(item.id)
            })
        }
        
        // 定义固定文件夹详情，用于显示名称
        val fixedDirDetails = mapOf(
            downloadDir to Pair("下载音乐", "已下载的歌曲文件")
        )
        
        val items = remember(allDirectories) {
            allDirectories.map { path ->
                val detail = fixedDirDetails[path]
                if (detail != null) {
                    CategoryItem(
                        id = path,
                        title = detail.first,
                        subtitle = detail.second,
                        iconRes = R.drawable.ic_folder,
                        showArrow = true
                    )
                } else {
                    CategoryItem(
                        id = path,
                        title = path.substringAfterLast('/'),
                        subtitle = path,
                        iconRes = R.drawable.ic_folder,
                        showArrow = true
                    )
                }
            }
        }
        
        LaunchedEffect(items) {
            adapter.submitList(items)
        }
        
        val surfaceColor = MaterialTheme.colorScheme.surface
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        val secondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        
        val surfaceColorArgb = remember(surfaceColor) { surfaceColor.toArgb() }
        val onSurfaceColorArgb = remember(onSurfaceColor) { onSurfaceColor.toArgb() }
        val secondaryTextColorArgb = remember(secondaryTextColor) { secondaryTextColor.toArgb() }
        
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                NonInterceptRecyclerView(context).apply {
                    layoutManager = LinearLayoutManager(context)
                    this.adapter = adapter
                    setHasFixedSize(true)
                    setItemViewCacheSize(20)
                    itemAnimator = null
                    
                    adapter.setColors(
                        surface = surfaceColorArgb,
                        onSurface = onSurfaceColorArgb,
                        secondaryText = secondaryTextColorArgb
                    )
                }
            },
            update = { recyclerView ->
                adapter.updateOnItemClick { item ->
                    onDirectoryClick(item.id)
                }
                adapter.setColors(
                    surface = surfaceColorArgb,
                    onSurface = onSurfaceColorArgb,
                    secondaryText = secondaryTextColorArgb
                )
            }
        )
    }
}

@Composable
fun FolderContentList(
    subDirectories: List<String>,
    tracks: List<MusicTrack>,
    currentTrack: MusicTrack?,
    onSubDirectoryClick: (String) -> Unit,
    onTrackClick: (MusicTrack) -> Unit,
    onMoreClick: (MusicTrack) -> Unit = {}
) {
    val currentTrackId = currentTrack?.id
    
    if (subDirectories.isEmpty() && tracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.still_empty),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        val adapter = remember { 
            FolderContentAdapter(
                initialOnDirectoryClick = onSubDirectoryClick,
                initialOnTrackClick = onTrackClick,
                initialOnMoreClick = onMoreClick
            )
        }
        
        val items = remember(subDirectories, tracks, currentTrackId) {
            val list = mutableListOf<FolderItem>()
            
            subDirectories.forEach { dir ->
                list.add(FolderItem.Directory(dir))
            }
            
            tracks.forEach { track ->
                list.add(FolderItem.Track(track, currentTrackId == track.id))
            }
            
            list
        }
        
        LaunchedEffect(items) {
            adapter.submitList(items)
        }
        
        val surfaceColor = MaterialTheme.colorScheme.surface
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        val secondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        val primaryColor = MaterialTheme.colorScheme.primary
        val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
        
        val surfaceColorArgb = remember(surfaceColor) { surfaceColor.toArgb() }
        val onSurfaceColorArgb = remember(onSurfaceColor) { onSurfaceColor.toArgb() }
        val secondaryTextColorArgb = remember(secondaryTextColor) { secondaryTextColor.toArgb() }
        val primaryColorArgb = remember(primaryColor) { primaryColor.toArgb() }
        val onPrimaryColorArgb = remember(onPrimaryColor) { onPrimaryColor.toArgb() }
        
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                NonInterceptRecyclerView(context).apply {
                    layoutManager = LinearLayoutManager(context)
                    this.adapter = adapter
                    setHasFixedSize(true)
                    setItemViewCacheSize(20)
                    itemAnimator = null
                    
                    adapter.setColors(
                        surface = surfaceColorArgb,
                        onSurface = onSurfaceColorArgb,
                        secondaryText = secondaryTextColorArgb,
                        primary = primaryColorArgb,
                        onPrimary = onPrimaryColorArgb
                    )
                }
            },
            update = { recyclerView ->
                adapter.updateClickListeners(
                    newOnDirectoryClick = onSubDirectoryClick,
                    newOnTrackClick = onTrackClick,
                    newOnMoreClick = onMoreClick
                )
                adapter.setColors(
                    surface = surfaceColorArgb,
                    onSurface = onSurfaceColorArgb,
                    secondaryText = secondaryTextColorArgb,
                    primary = primaryColorArgb,
                    onPrimary = onPrimaryColorArgb
                )
            }
        )
    }
}

@Composable
fun CategorySelectionList(
    items: List<String>,
    onItemClick: (String) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.still_empty),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        val adapter = remember { 
            CategoryAdapter(initialOnItemClick = { item ->
                onItemClick(item.id)
            })
        }
        
        val categoryItems = remember(items) {
            items.map { item ->
                CategoryItem(
                    id = item,
                    title = item,
                    showArrow = true
                )
            }
        }
        
        LaunchedEffect(categoryItems) {
            adapter.submitList(categoryItems)
        }
        
        val surfaceColor = MaterialTheme.colorScheme.surface
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        val secondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        
        val surfaceColorArgb = remember(surfaceColor) { surfaceColor.toArgb() }
        val onSurfaceColorArgb = remember(onSurfaceColor) { onSurfaceColor.toArgb() }
        val secondaryTextColorArgb = remember(secondaryTextColor) { secondaryTextColor.toArgb() }
        
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                NonInterceptRecyclerView(context).apply {
                    layoutManager = LinearLayoutManager(context)
                    this.adapter = adapter
                    setHasFixedSize(true)
                    setItemViewCacheSize(20)
                    itemAnimator = null
                    
                    adapter.setColors(
                        surface = surfaceColorArgb,
                        onSurface = onSurfaceColorArgb,
                        secondaryText = secondaryTextColorArgb
                    )
                }
            },
            update = { recyclerView ->
                adapter.updateOnItemClick { item ->
                    onItemClick(item.id)
                }
                adapter.setColors(
                    surface = surfaceColorArgb,
                    onSurface = onSurfaceColorArgb,
                    secondaryText = secondaryTextColorArgb
                )
            }
        )
    }
}

@Composable
fun TrackList(
    tracks: List<MusicTrack>,
    currentTrack: MusicTrack?,
    showFavoriteButton: Boolean = true,
    onMoreClick: (MusicTrack) -> Unit = {}
) {
    val favorites by PlaylistManager.favorites.collectAsState()
    val favoriteIds = remember(favorites) { favorites?.trackIds?.toSet() ?: emptySet() }
    val currentTrackId = currentTrack?.id

    val adapter = remember {
        com.bicy.whitenoise.ui.adapters.PlaylistAdapter(
            initialOnTrackClick = { index ->
                MusicPlayerController.setPlaylist(tracks, index)
                MusicPlayerController.play()
            },
            initialOnMoreClick = { track ->
                onMoreClick(track)
            }
        )
    }
    
    val trackItems = remember(tracks, currentTrackId) {
        tracks.mapIndexed { index, track ->
            com.bicy.whitenoise.ui.adapters.PlaylistAdapter.TrackItem(
                track = track,
                index = index,
                isPlaying = currentTrackId == track.id
            )
        }
    }
    
    LaunchedEffect(trackItems) {
        adapter.submitList(trackItems)
    }
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    
    val surfaceColorArgb = remember(surfaceColor) { surfaceColor.toArgb() }
    val onSurfaceColorArgb = remember(onSurfaceColor) { onSurfaceColor.toArgb() }
    val secondaryTextColorArgb = remember(secondaryTextColor) { secondaryTextColor.toArgb() }
    val primaryColorArgb = remember(primaryColor) { primaryColor.toArgb() }
    val onPrimaryColorArgb = remember(onPrimaryColor) { onPrimaryColor.toArgb() }
    
    var hasScrolledToCurrent by remember { mutableStateOf(false) }
    
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            NonInterceptRecyclerView(context).apply {
                    layoutManager = LinearLayoutManager(context)
                    this.adapter = adapter
                    setHasFixedSize(true)
                    setItemViewCacheSize(20)
                    @Suppress("DEPRECATION")
                    setDrawingCacheEnabled(true)
                    @Suppress("DEPRECATION")
                    setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
                    itemAnimator = null
                
                adapter.setColors(
                    surface = surfaceColorArgb,
                    onSurface = onSurfaceColorArgb,
                    secondaryText = secondaryTextColorArgb,
                    primary = primaryColorArgb,
                    onPrimary = onPrimaryColorArgb
                )
                
                post {
                    if (!hasScrolledToCurrent) {
                        val currentIndex = tracks.indexOfFirst { it.id == currentTrackId }
                        if (currentIndex >= 0) {
                            val layoutManager = layoutManager as? LinearLayoutManager ?: return@post
                            val itemHeight = (68 * context.resources.displayMetrics.density).toInt()
                            val centerOffset = (height - itemHeight) / 2
                            layoutManager.scrollToPositionWithOffset(currentIndex, centerOffset)
                            hasScrolledToCurrent = true
                        }
                    }
                }
            }
        },
        update = { recyclerView ->
            adapter.updateOnTrackClick { index ->
                MusicPlayerController.setPlaylist(tracks, index)
                MusicPlayerController.play()
            }
            adapter.setColors(
                surface = surfaceColorArgb,
                onSurface = onSurfaceColorArgb,
                secondaryText = secondaryTextColorArgb,
                primary = primaryColorArgb,
                onPrimary = onPrimaryColorArgb
            )
        }
    )
}

@Composable
fun PlaylistItem(
    track: MusicTrack,
    isPlaying: Boolean,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null
) {
    val itemStartTime = System.currentTimeMillis()
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                color = if (isPlaying) primaryColor else onSurfaceColor
            )
            Text(
                text = track.artist ?: stringResource(R.string.unknown_artist),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = onSurfaceColor.copy(alpha = 0.6f)
            )
        }
    }
    
    SideEffect {
        val elapsed = System.currentTimeMillis() - itemStartTime
        if (elapsed > 2) {
            Log.d("PlaylistDebug", "PlaylistItem render: ${track.title} in ${elapsed}ms")
        }
    }
}

@Composable
fun PlaylistSelectionList(
    favorites: UserPlaylist?,
    userPlaylists: List<UserPlaylist>,
    tracks: List<MusicTrack>,
    onFavoritesClick: () -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlaylistMoreClick: (UserPlaylist) -> Unit = {}
) {
    val adapter = remember {
        CategoryAdapter(
            initialOnItemClick = { item ->
                when (item.id) {
                    "favorites" -> onFavoritesClick()
                    "create" -> onCreatePlaylist()
                    else -> {
                        val playlist = userPlaylists.find { it.id == item.id }
                        if (playlist != null) {
                            onPlaylistClick(playlist)
                        }
                    }
                }
            },
            initialOnMoreClick = { item ->
                val playlist = userPlaylists.find { it.id == item.id }
                if (playlist != null) {
                    onPlaylistMoreClick(playlist)
                }
            }
        )
    }

    val favoritesTitle = stringResource(R.string.favorites)
    val favoritesSubtitle = stringResource(R.string.track_count, favorites?.trackIds?.size ?: 0)
    val createPlaylistTitle = stringResource(R.string.create_playlist)
    val context = LocalContext.current

    val items = remember(favorites, userPlaylists, favoritesTitle, favoritesSubtitle, createPlaylistTitle) {
        val list = mutableListOf<CategoryItem>()

        list.add(
            CategoryItem(
                id = "favorites",
                title = favoritesTitle,
                subtitle = favoritesSubtitle,
                iconRes = R.drawable.ic_favorite,
                showArrow = true
            )
        )

        userPlaylists.forEach { playlist ->
            list.add(
                CategoryItem(
                    id = playlist.id,
                    title = playlist.name,
                    subtitle = context.getString(R.string.track_count, playlist.trackIds.size),
                    iconRes = R.drawable.ic_playlist,
                    showMoreButton = true
                )
            )
        }

        list.add(
            CategoryItem(
                id = "create",
                title = createPlaylistTitle,
                iconRes = R.drawable.ic_add
            )
        )

        list
    }
    
    LaunchedEffect(items) {
        adapter.submitList(items)
    }
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    
    val surfaceColorArgb = remember(surfaceColor) { surfaceColor.toArgb() }
    val onSurfaceColorArgb = remember(onSurfaceColor) { onSurfaceColor.toArgb() }
    val secondaryTextColorArgb = remember(secondaryTextColor) { secondaryTextColor.toArgb() }
    
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            NonInterceptRecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                this.adapter = adapter
                setHasFixedSize(true)
                setItemViewCacheSize(20)
                itemAnimator = null
                
                adapter.setColors(
                    surface = surfaceColorArgb,
                    onSurface = onSurfaceColorArgb,
                    secondaryText = secondaryTextColorArgb
                )
            }
        },
        update = { recyclerView ->
            adapter.updateOnItemClick { item ->
                when (item.id) {
                    "favorites" -> onFavoritesClick()
                    "create" -> onCreatePlaylist()
                    else -> {
                        val playlist = userPlaylists.find { it.id == item.id }
                        if (playlist != null) {
                            onPlaylistClick(playlist)
                        }
                    }
                }
            }
            adapter.setColors(
                surface = surfaceColorArgb,
                onSurface = onSurfaceColorArgb,
                secondaryText = secondaryTextColorArgb
            )
        }
    )
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun SaveCurrentListDialog(
    currentPlaylist: List<MusicTrack>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_as_playlist)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.save_current_playlist_hint, currentPlaylist.size),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * 歌曲操作菜单对话框
 */
@Composable
fun TrackOptionsMenuDialog(
    track: MusicTrack,
    isFavorite: Boolean,
    isInPlaylist: Boolean = false,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = track.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                Text(
                    text = track.artist ?: stringResource(R.string.unknown_artist),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = {
                    onAddToPlaylist()
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_to_playlist))
                }

                TextButton(onClick = {
                    onToggleFavorite()
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isFavorite) stringResource(R.string.remove_from_favorites)
                         else stringResource(R.string.add_to_favorites))
                }

                if (isInPlaylist && onRemoveFromPlaylist != null) {
                    TextButton(onClick = {
                        onRemoveFromPlaylist()
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.remove_from_playlist),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

/**
 * 添加到歌单选择对话框
 */
@Composable
fun AddToPlaylistDialog(
    track: MusicTrack,
    onDismiss: () -> Unit,
    onAddToPlaylist: (String) -> Unit
) {
    val userPlaylists by PlaylistManager.userPlaylists.collectAsState()
    val favorites by PlaylistManager.favorites.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_target_playlist)) },
        text = {
            LazyColumn {
                // 收藏
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAddToPlaylist("favorites")
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.favorites),
                            fontSize = 14.sp
                        )
                    }
                }

                // 用户歌单
                items(userPlaylists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAddToPlaylist(playlist.id)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = playlist.name,
                                fontSize = 14.sp
                            )
                            Text(
                                text = stringResource(R.string.track_count, playlist.trackIds.size),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * 删除歌单确认对话框
 */
@Composable
fun DeletePlaylistConfirmDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_playlist)) },
        text = {
            Text(stringResource(R.string.delete_playlist_confirm, playlistName))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(
                    stringResource(R.string.remove),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * 重命名歌单对话框
 */
@Composable
fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && name != currentName) {
                        onRename(name.trim())
                    }
                },
                enabled = name.isNotBlank() && name != currentName
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * 扫描目录下的音频文件，构建 MusicTrack 列表（不通过 MusicScanner，直接读文件系统）
 */
private fun scanDirectoryForTracks(dir: File): List<MusicTrack> {
    if (!dir.exists() || !dir.isDirectory) return emptyList()
    
    val audioExtensions = setOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "wma")
    return dir.listFiles()
        ?.filter { it.isFile && it.extension.lowercase() in audioExtensions }
        ?.mapNotNull { file ->
            val metadata = AudioMetadataReader.readFromFile(file)
            val title = metadata?.title?.takeUnless { it.isBlank() } ?: file.nameWithoutExtension
            val artist = metadata?.artist?.takeUnless { it == "<unknown>" || it.isBlank() }
            val album = metadata?.album?.takeUnless { it == "<unknown>" || it.isBlank() }
            val duration = metadata?.duration?.takeIf { it > 0 } ?: 0L
            
            MusicTrack(
                id = file.absolutePath,  // 用文件路径作 ID，确保播放进度可恢复
                path = file.absolutePath,
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                isOnline = false,
                streamUrl = null,
                source = null,
                dateAdded = file.lastModified(),
                albumArt = null,
                mediaStoreId = 0
            )
        }
        ?: emptyList()
}
