package com.example.spoyt.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spoyt.R
import com.example.spoyt.ui.components.EmptyState
import com.example.spoyt.ui.components.SectionHeader
import com.example.spoyt.ui.components.SpoytBackground
import com.example.spoyt.ui.components.TrackRow
import com.example.spoyt.ui.viewmodel.HomeViewModel
import com.example.spoyt.ui.theme.SpoytGreen
import com.example.spoyt.ui.theme.SpoytViolet
import java.util.Calendar

/**
 * Home: greeting, search entry point, quick-access tiles and recently played tracks.
 */
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenPlaylist: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenLibrary: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val recent by viewModel.recentTracks.collectAsStateWithLifecycle()
    val liked by viewModel.likedTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    SpoytBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                HeaderRow(onOpenSettings = onOpenSettings)
            }

            item {
                SearchPill(onClick = onOpenSearch)
            }

            item {
                SectionHeader(title = stringResource(R.string.home_quick_access))
            }

            item {
                QuickAccessGrid(
                    onOpenLibrary = onOpenLibrary,
                    playlists = playlists,
                    onOpenPlaylist = onOpenPlaylist
                )
            }

            if (recent.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.home_recent))
                }
                itemsIndexed(recent) { index, track ->
                    TrackRow(
                        track = track,
                        modifier = Modifier.padding(horizontal = 10.dp),
                        onClick = {
                            viewModel.play(recent, index)
                            onOpenPlayer()
                        },
                        isLiked = liked.any { it.videoId == track.videoId },
                        onLikeToggle = { viewModel.toggleLike(track) }
                    )
                }
            } else {
                item {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.home_empty_recent),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.home_greeting_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.home_search_pill),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickAccessGrid(
    onOpenLibrary: () -> Unit,
    playlists: List<com.example.spoyt.data.local.entities.PlaylistEntity>,
    onOpenPlaylist: (Int) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickTile(
                title = stringResource(R.string.home_liked),
                subtitle = stringResource(R.string.home_liked_subtitle),
                icon = Icons.Default.Favorite,
                gradient = listOf(Color(0xFF7C3AED), Color(0xFFB06BFF)),
                modifier = Modifier.weight(1f),
                onClick = onOpenLibrary
            )
            QuickTile(
                title = stringResource(R.string.home_downloads),
                subtitle = stringResource(R.string.home_downloads_subtitle),
                icon = Icons.Default.GetApp,
                gradient = listOf(Color(0xFF0E7C4A), SpoytGreen),
                modifier = Modifier.weight(1f),
                onClick = onOpenLibrary
            )
        }

        playlists.chunked(2).forEach { rowPlaylists ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowPlaylists.forEach { playlist ->
                    QuickTile(
                        title = playlist.name,
                        subtitle = stringResource(R.string.playlist_title),
                        icon = Icons.Default.LibraryMusic,
                        gradient = listOf(SpoytViolet, Color(0xFF4ECDC4)),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenPlaylist(playlist.id) }
                    )
                }
                if (rowPlaylists.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return stringResource(
        when {
            hour < 12 -> R.string.home_greeting_morning
            hour < 18 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    )
}
