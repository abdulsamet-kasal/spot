package com.example.spoyt.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spoyt.R
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.download.Downloads
import com.example.spoyt.ui.components.AddToPlaylistDialog
import com.example.spoyt.ui.components.EmptyState
import com.example.spoyt.ui.components.SpoytBackground
import com.example.spoyt.ui.components.TrackRow
import com.example.spoyt.ui.theme.SpoytGreen
import com.example.spoyt.ui.theme.SpoytViolet
import com.example.spoyt.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    onOpenPlaylist: (Int) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    viewModel: LibraryViewModel = viewModel()
) {
    val liked by viewModel.likedTracks.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val recentTracks by viewModel.recentTracks.collectAsStateWithLifecycle()
    val topTracks by viewModel.topTracks.collectAsStateWithLifecycle()
    val progressMap by Downloads.progress.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var addToPlaylistVideoId by remember { mutableStateOf<String?>(null) }

    // App shortcuts may request a specific tab ("Beğenilenler", "İndirilenler").
    LaunchedEffect(Unit) {
        com.example.spoyt.ui.nav.UiRequests.libraryTab.collect { tab ->
            if (tab >= 0) {
                selectedTab = tab
                com.example.spoyt.ui.nav.UiRequests.libraryTab.value = -1
            }
        }
    }

    SpoytBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.drawer_open),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.library_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (selectedTab == 2) {
                    IconButton(onClick = { newPlaylistName = ""; showCreateDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.playlist_create),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            SegmentedTabs(
                selected = selectedTab,
                onSelect = { selectedTab = it },
                labels = listOf(
                    stringResource(R.string.library_liked),
                    stringResource(R.string.library_downloads),
                    stringResource(R.string.library_playlists),
                    stringResource(R.string.library_history)
                )
            )

            Crossfade(targetState = selectedTab, label = "library-tab") { tab ->
                when (tab) {
                    0 -> LikedTab(
                        liked = liked,
                        onPlay = { index ->
                            viewModel.play(liked, index)
                            onOpenPlayer()
                        },
                        onLikeToggle = viewModel::toggleLike,
                        onDownload = viewModel::download,
                        onPlayNext = viewModel::addToQueue,
                        onAddToPlaylist = { addToPlaylistVideoId = it }
                    )

                    1 -> DownloadsTab(
                        downloads = downloads,
                        allTracks = allTracks,
                        progress = progressMap,
                        likedIds = liked.map { it.videoId }.toSet(),
                        onPlay = { track ->
                            viewModel.play(listOf(track))
                            onOpenPlayer()
                        },
                        onLikeToggle = viewModel::toggleLike,
                        onDelete = viewModel::deleteDownload,
                        onPlayNext = viewModel::addToQueue,
                        onAddToPlaylist = { addToPlaylistVideoId = it }
                    )

                    2 -> PlaylistsTab(
                        playlists = playlists,
                        onCreate = { newPlaylistName = ""; showCreateDialog = true },
                        onOpen = onOpenPlaylist,
                        onDelete = viewModel::deletePlaylist
                    )

                    else -> HistoryTab(
                        recent = recentTracks,
                        top = topTracks,
                        onPlay = { tracks, index ->
                            viewModel.play(tracks, index)
                            onOpenPlayer()
                        },
                        onLikeToggle = viewModel::toggleLike,
                        onDownload = viewModel::download,
                        onPlayNext = viewModel::addToQueue,
                        onAddToPlaylist = { addToPlaylistVideoId = it }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.playlist_create)) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(stringResource(R.string.playlist_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createPlaylist(newPlaylistName) { id ->
                            showCreateDialog = false
                            onOpenPlaylist(id)
                        }
                    },
                    enabled = newPlaylistName.trim().isNotEmpty()
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    addToPlaylistVideoId?.let { videoId ->
        AddToPlaylistDialog(
            videoId = videoId,
            onDismiss = { addToPlaylistVideoId = null }
        )
    }
}

@Composable
private fun SegmentedTabs(selected: Int, onSelect: (Int) -> Unit, labels: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LikedTab(
    liked: List<TrackEntity>,
    onPlay: (Int) -> Unit,
    onLikeToggle: (TrackEntity) -> Unit,
    onDownload: (TrackEntity) -> Unit,
    onPlayNext: (TrackEntity) -> Unit,
    onAddToPlaylist: (String) -> Unit
) {
    if (liked.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Favorite,
            title = stringResource(R.string.library_empty_liked),
            subtitle = stringResource(R.string.library_empty_liked_subtitle)
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp)) {
        item {
            CountLabel(stringResource(R.string.library_song_count, liked.size))
        }
        itemsIndexed(liked) { index, track ->
            TrackRow(
                track = track,
                modifier = Modifier.padding(horizontal = 10.dp),
                onClick = { onPlay(index) },
                isLiked = true,
                onLikeToggle = { onLikeToggle(track) },
                onDownload = { onDownload(track) },
                onPlayNext = { onPlayNext(track) },
                onAddToPlaylist = { onAddToPlaylist(track.videoId) }
            )
        }
    }
}

@Composable
private fun DownloadsTab(
    downloads: List<com.example.spoyt.data.local.entities.DownloadEntity>,
    allTracks: List<TrackEntity>,
    progress: Map<String, Int>,
    likedIds: Set<String>,
    onPlay: (TrackEntity) -> Unit,
    onLikeToggle: (TrackEntity) -> Unit,
    onDelete: (String) -> Unit,
    onPlayNext: (TrackEntity) -> Unit,
    onAddToPlaylist: (String) -> Unit
) {
    if (downloads.isEmpty()) {
        EmptyState(
            icon = Icons.Default.DownloadDone,
            title = stringResource(R.string.library_empty_downloads),
            subtitle = stringResource(R.string.library_empty_downloads_subtitle)
        )
        return
    }

    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp)) {
        item { CountLabel(stringResource(R.string.library_song_count, downloads.size)) }
        items(downloads) { download ->
            val track = allTracks.find { it.videoId == download.videoId }
                ?: remember(download.videoId) {
                    TrackEntity(
                        videoId = download.videoId,
                        title = "İndirilen şarkı",
                        artist = "",
                        durationSeconds = 0,
                        thumbnailUrl = null
                    )
                }
            TrackRow(
                track = track,
                modifier = Modifier.padding(horizontal = 10.dp),
                onClick = { onPlay(track) },
                isLiked = likedIds.contains(track.videoId),
                downloadState = download.state,
                downloadProgress = progress[download.videoId],
                onLikeToggle = { onLikeToggle(track) },
                onPlayNext = { onPlayNext(track) },
                onAddToPlaylist = { onAddToPlaylist(track.videoId) },
                onRemove = { onDelete(download.videoId) }
            )
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<com.example.spoyt.data.local.entities.PlaylistEntity>,
    onCreate: () -> Unit,
    onOpen: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyState(
            icon = Icons.Default.LibraryMusic,
            title = stringResource(R.string.library_empty_playlists),
            subtitle = stringResource(R.string.library_empty_playlists_subtitle)
        )
        return
    }

    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)) {
        items(playlists) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpen(playlist.id) }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(listOf(SpoytViolet, SpoytGreen))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.playlist_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onOpen(playlist.id) }) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.action_play_all),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onDelete(playlist.id) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.playlist_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CountLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 4.dp)
    )
}

/** "Geçmiş" tab: recent listens and most played, each section playable. */
@Composable
private fun HistoryTab(
    recent: List<TrackEntity>,
    top: List<TrackEntity>,
    onPlay: (List<TrackEntity>, Int) -> Unit,
    onLikeToggle: (TrackEntity) -> Unit,
    onDownload: (TrackEntity) -> Unit,
    onPlayNext: (TrackEntity) -> Unit,
    onAddToPlaylist: (String) -> Unit
) {
    if (recent.isEmpty() && top.isEmpty()) {
        EmptyState(
            icon = Icons.Default.History,
            title = stringResource(R.string.history_empty),
            subtitle = stringResource(R.string.history_empty_subtitle)
        )
        return
    }

    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp)) {
        if (recent.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.history_recent)) }
            itemsIndexed(recent) { index, track ->
                TrackRow(
                    track = track,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    onClick = { onPlay(recent, index) },
                    onLikeToggle = { onLikeToggle(track) },
                    onDownload = { onDownload(track) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToPlaylist = { onAddToPlaylist(track.videoId) }
                )
            }
        }
        if (top.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.history_top)) }
            itemsIndexed(top) { index, track ->
                TrackRow(
                    track = track,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    index = index,
                    onClick = { onPlay(top, index) },
                    onLikeToggle = { onLikeToggle(track) },
                    onDownload = { onDownload(track) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToPlaylist = { onAddToPlaylist(track.videoId) }
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 2.dp)
    )
}
