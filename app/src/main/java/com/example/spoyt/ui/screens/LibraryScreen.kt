package com.example.spoyt.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spoyt.R
import com.example.spoyt.ui.components.TrackRow
import com.example.spoyt.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    onOpenPlaylist: (Int) -> Unit,
    onOpenPlayer: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val liked by viewModel.likedTracks.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val tabs = listOf(
        stringResource(R.string.library_liked),
        stringResource(R.string.library_downloads),
        stringResource(R.string.library_playlists)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Text(
                    text = stringResource(R.string.library_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = { newPlaylistName = ""; showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.playlist_create)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    if (liked.isEmpty()) {
                        item { EmptyMessage(stringResource(R.string.library_empty_liked)) }
                    }
                    itemsIndexed(liked) { index, track ->
                        TrackRow(
                            track = track,
                            onClick = {
                                viewModel.play(liked, index)
                                onOpenPlayer()
                            },
                            isLiked = true,
                            onLikeToggle = { viewModel.toggleLike(track) }
                        )
                    }
                }

                1 -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    if (downloads.isEmpty()) {
                        item { EmptyMessage(stringResource(R.string.library_empty_downloads)) }
                    }
                    items(downloads) { download ->
                        val track = allTracks.find { it.videoId == download.videoId }
                            ?: remember(download.videoId) {
                                com.example.spoyt.data.local.entities.TrackEntity(
                                    videoId = download.videoId,
                                    title = download.videoId,
                                    artist = "",
                                    durationSeconds = 0,
                                    thumbnailUrl = null
                                )
                            }
                        TrackRow(
                            track = track,
                            onClick = { viewModel.play(listOf(track)); onOpenPlayer() },
                            downloadState = download.state,
                            onRemove = { viewModel.deleteDownload(download.videoId) }
                        )
                    }
                }

                2 -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    if (playlists.isEmpty()) {
                        item { EmptyMessage(stringResource(R.string.library_empty_playlists)) }
                    }
                    items(playlists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPlaylist(playlist.id) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onOpenPlaylist(playlist.id) }) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = stringResource(R.string.action_play_all)
                                )
                            }
                            IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.playlist_delete)
                                )
                            }
                        }
                    }
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
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    )
}
