package com.bicy.whitenoise.yODW.SrEO.Xomm

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bicy.whitenoise.yODW.w0GA.CategoryAdapter
import com.bicy.whitenoise.yODW.w0GA.CategoryItem
import com.bicy.whitenoise.yODW.AQH6.NonInterceptRecyclerView
import com.bicy.whitenoise.yODW.w0GA.PlaylistAdapter
import com.bicy.whitenoise.yODW.w0GA.FolderContentAdapter
import com.bicy.whitenoise.yODW.w0GA.FolderItem
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bicy.whitenoise.R
import com.bicy.whitenoise.JwJY.EY9i.MusicStorage
import com.bicy.whitenoise.JwJY.GqOr.PlaylistManager
import com.bicy.whitenoise.JwJY.GqOr.UserPlaylist
import com.bicy.whitenoise.xnef.MusicPlayerController
import com.bicy.whitenoise.xnef.MusicTrack
import com.bicy.whitenoise.y10p.AudioMetadataReader
import com.bicy.whitenoise.yODW.G2qv.LocalPlaylistNavigation
import com.bicy.whitenoise.yODW.G2qv.LocalPlaylistNavigationHolder

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
    
    val tracksByFolder = remember(tracks) {
        tracks.groupBy { it.path.substringBeforeLast('/') }
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
    
    val favoriteTracks = remember(favorites, tracks, selectedCategory, selectedPlaylist) {
        if (selectedCategory == MusicCategory.Playlist && selectedPlaylist == null) {
            favorites?.let { fav -> PlaylistManager.getTracksForPlaylist(fav, tracks) } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    val baseTracks = remember(selectedCategory, tracks, tracksInCurrentFolder, artistTracks, albumTracks, playlistTracks) {
        when(selectedCategory) {
            MusicCategory.CurrentList -> emptyList()
            MusicCategory.All -> tracks
            MusicCategory.Folder -> tracksInCurrentFolder
            MusicCategory.Artist -> artistTracks
            MusicCategory.Album -> albumTracks
            MusicCategory.Playlist -> playlistTracks
        }
    }
    
    val unsortedTracks = remember(selectedCategory, selectedPlaylist, playlist, favoriteTracks, baseTracks) {
        when {
            selectedCategory == MusicCategory.CurrentList -> playlist
            selectedCategory == MusicCategory.Playlist && selectedPlaylist == null -> favoriteTracks
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
    
    BackHandler(enabled = hasSubPage && !playlistNavigation.hasSubPage) {
        navigateBack()
    }
    
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                PlaylistManager.createPlaylist(name)
                showCreatePlaylistDialog = false
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
            }
        )
    }
    
    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp)
                    .zIndex(10f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
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
                    },
                    enabled = when {
                        selectedCategory == MusicCategory.Folder -> folderPathStack.isNotEmpty()
                        selectedCategory == MusicCategory.Playlist -> selectedPlaylist != null
                        selectedArtist != null -> true
                        selectedAlbum != null -> true
                        else -> false
                    }
                ) {
                    Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
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
                            text = "当前播放 (${playlist.size}首)",
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
                                contentDescription = "保存为歌单"
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
                        contentDescription = stringResource(R.string.preset)
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
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                                        text = "暂无播放中的音乐",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                TrackList(
                                    tracks = displayTracks,
                                    currentTrack = currentTrack
                                )
                            }
                        }
                        selectedCategory == MusicCategory.Folder && folderPathStack.isEmpty() -> {
                            TopDirectorySelectionList(
                                directories = topDirectories,
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
                                }
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
                                currentTrack = currentTrack
                            )
                        }
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
                label = category.getLabel(),
                isSelected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CategoryTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }
    
    val textColor = if (isSelected) {
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
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
fun TopDirectorySelectionList(
    directories: List<String>,
    onDirectoryClick: (String) -> Unit
) {
    if (directories.isEmpty()) {
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
        
        val items = remember(directories) {
            directories.map { path ->
                CategoryItem(
                    id = path,
                    title = path.substringAfterLast('/'),
                    subtitle = path,
                    iconRes = R.drawable.ic_folder,
                    showArrow = true
                )
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
    onTrackClick: (MusicTrack) -> Unit
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
                initialOnTrackClick = onTrackClick
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
                    newOnTrackClick = onTrackClick
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
    showFavoriteButton: Boolean = true
) {
    val favorites by PlaylistManager.favorites.collectAsState()
    val favoriteIds = remember(favorites) { favorites?.trackIds?.toSet() ?: emptySet() }
    val currentTrackId = currentTrack?.id
    
    val adapter = remember { 
        com.bicy.whitenoise.yODW.w0GA.PlaylistAdapter(initialOnTrackClick = { index ->
            MusicPlayerController.setPlaylist(tracks, index)
            MusicPlayerController.play()
        })
    }
    
    val trackItems = remember(tracks, currentTrackId) {
        tracks.mapIndexed { index, track ->
            com.bicy.whitenoise.yODW.w0GA.PlaylistAdapter.TrackItem(
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
                    setDrawingCacheEnabled(true)
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
                text = track.artist ?: "未知艺术家",
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
    onCreatePlaylist: () -> Unit
) {
    val adapter = remember { 
        CategoryAdapter(initialOnItemClick = { item ->
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
        })
    }
    
    val items = remember(favorites, userPlaylists) {
        val list = mutableListOf<CategoryItem>()
        
        list.add(
            CategoryItem(
                id = "favorites",
                title = "收藏",
                subtitle = "${favorites?.trackIds?.size ?: 0}首",
                iconRes = R.drawable.ic_favorite,
                showArrow = true
            )
        )
        
        userPlaylists.forEach { playlist ->
            list.add(
                CategoryItem(
                    id = playlist.id,
                    title = playlist.name,
                    subtitle = "${playlist.trackIds.size}首",
                    iconRes = R.drawable.ic_playlist,
                    showArrow = true
                )
            )
        }
        
        list.add(
            CategoryItem(
                id = "create",
                title = "新建歌单",
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
        title = { Text("新建歌单") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("歌单名称") },
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
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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
        title = { Text("保存为歌单") },
        text = {
            Column {
                Text(
                    text = "将当前播放列表 (${currentPlaylist.size}首) 保存为新歌单",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("歌单名称") },
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
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
