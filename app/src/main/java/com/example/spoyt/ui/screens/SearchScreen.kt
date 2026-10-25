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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spoyt.R
import com.example.spoyt.download.Downloads
import com.example.spoyt.ui.components.AddToPlaylistDialog
import com.example.spoyt.ui.components.EmptyState
import com.example.spoyt.ui.components.SpoytBackground
import com.example.spoyt.ui.components.TrackRow
import com.example.spoyt.ui.viewmodel.SearchViewModel
import com.example.spoyt.ui.viewmodel.toTrackEntity

@Composable
fun SearchScreen(
    onOpenPlayer: () -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val progressMap by Downloads.progress.collectAsStateWithLifecycle()
    var addToPlaylistVideoId by remember { mutableStateOf<String?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current

    SpoytBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.search_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 12.dp)
            )

            SearchField(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = {
                    keyboard?.hide()
                    viewModel.searchNow()
                },
                onClear = { viewModel.onQueryChange("") }
            )

            if (state.results.isNotEmpty()) {
                ResultsHeader(
                    count = state.results.size,
                    onPlayAll = {
                        viewModel.playAll()
                        onOpenPlayer()
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 56.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    }

                    state.results.isEmpty() -> {
                        EmptyState(
                            icon = if (state.error != null) Icons.Default.SearchOff
                            else Icons.Default.Search,
                            title = state.error ?: stringResource(R.string.search_hint),
                            subtitle = if (state.error == null) {
                                "YouTube Music üzerinde arama yapılır."
                            } else null,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp)
                        ) {
                            itemsIndexed(state.results) { index, result ->
                                val track = remember(result.videoId) { result.toTrackEntity() }
                                TrackRow(
                                    track = track,
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    onClick = {
                                        viewModel.play(index)
                                        onOpenPlayer()
                                    },
                                    downloadProgress = progressMap[track.videoId],
                                    onLikeToggle = { viewModel.toggleLike(result.videoId) },
                                    onDownload = { viewModel.download(track) },
                                    onPlayNext = { viewModel.addToQueue(track) },
                                    onAddToPlaylist = { addToPlaylistVideoId = track.videoId }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    addToPlaylistVideoId?.let { videoId ->
        AddToPlaylistDialog(
            videoId = videoId,
            onDismiss = { addToPlaylistVideoId = null }
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp)),
        placeholder = {
            Text(
                text = stringResource(R.string.search_hint),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.action_cancel),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun ResultsHeader(count: Int, onPlayAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.search_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$count sonuç",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary,
            onClick = onPlayAll
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.action_play_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(2.dp))
}
