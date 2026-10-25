package com.example.spoyt.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.spoyt.ui.components.AddToPlaylistDialog
import com.example.spoyt.ui.components.EmptyState
import com.example.spoyt.ui.components.SpoytBackground
import com.example.spoyt.ui.components.TrackRow
import com.example.spoyt.ui.theme.SpoytGreen
import com.example.spoyt.ui.theme.SpoytViolet
import com.example.spoyt.ui.viewmodel.PlaylistDetailViewModel

@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    viewModel: PlaylistDetailViewModel = viewModel()
) {
    LaunchedEffect(playlistId) { viewModel.load(playlistId) }

    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedIds.collectAsStateWithLifecycle()

    var menuOpen by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf("") }
    var addToPlaylistVideoId by remember { mutableStateOf<String?>(null) }

    SpoytBackground(accent = SpoytViolet) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.playlist_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = playlist?.name ?: stringResource(R.string.playlist_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_rename)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                renameValue = playlist?.name ?: ""
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = { menuOpen = false; showDeleteDialog = true }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item {
                    PlaylistHeader(
                        songCount = tracks.size,
                        enabled = tracks.isNotEmpty(),
                        onPlayAll = {
                            viewModel.playAll()
                            onOpenPlayer()
                        }
                    )
                }

                if (tracks.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.LibraryMusic,
                            title = stringResource(R.string.playlist_empty),
                            subtitle = "Aramadan veya kütüphaneden şarkı ekleyebilirsin."
                        )
                    }
                } else {
                    itemsIndexed(tracks) { index, track ->
                        TrackRow(
                            track = track,
                            modifier = Modifier.padding(horizontal = 10.dp),
                            index = index,
                            onClick = {
                                viewModel.playAt(index)
                                onOpenPlayer()
                            },
                            isLiked = likedIds.contains(track.videoId),
                            onLikeToggle = { viewModel.toggleLike(track) },
                            onDownload = { viewModel.download(track) },
                            onPlayNext = { viewModel.addToQueue(track) },
                            onAddToPlaylist = { addToPlaylistVideoId = track.videoId },
                            onRemove = { viewModel.removeTrack(track.videoId) }
                        )
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.playlist_rename)) },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text(stringResource(R.string.playlist_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.rename(renameValue)
                        showRenameDialog = false
                    },
                    enabled = renameValue.trim().isNotEmpty()
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.playlist_delete)) },
            text = { Text("Bu çalma listesini silmek istediğine emin misin?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deletePlaylist()
                        onBack()
                    }
                ) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
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
private fun PlaylistHeader(songCount: Int, enabled: Boolean, onPlayAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(SpoytViolet, SpoytGreen))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.library_song_count, songCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (enabled) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                onClick = onPlayAll
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.playlist_play_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
