package com.example.spoyt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.spoyt.R
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.ui.components.EmptyState
import com.example.spoyt.ui.components.SectionHeader
import com.example.spoyt.ui.components.SpoytBackground
import com.example.spoyt.ui.components.TrackRow
import com.example.spoyt.ui.theme.SpoytGreen
import com.example.spoyt.ui.theme.SpoytViolet
import com.example.spoyt.ui.viewmodel.HomeViewModel
import java.util.Calendar

/**
 * Home: greeting, search entry point, a hero "shuffle" card, quick-access tiles and the
 * recently played tracks. Content is width-limited and the tile grid gains columns on
 * tablets/landscape so the layout does not stretch on large screens.
 */
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenPlaylist: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenLibrary: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val recent by viewModel.recentTracks.collectAsStateWithLifecycle()
    val liked by viewModel.likedTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    SpoytBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val gridColumns = when {
                maxWidth >= 900.dp -> 4
                maxWidth >= 600.dp -> 3
                else -> 2
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 940.dp)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item {
                    HeaderRow(onOpenDrawer = onOpenDrawer, onOpenSettings = onOpenSettings)
                }

                item {
                    SearchPill(onClick = onOpenSearch)
                }

                if (recent.isNotEmpty()) {
                    item {
                        HeroCard(
                            track = recent.first(),
                            onShuffle = {
                                viewModel.playShuffled(recent)
                                onOpenPlayer()
                            },
                            onOpenLiked = onOpenLibrary
                        )
                    }
                }

                item {
                    SectionHeader(title = stringResource(R.string.home_quick_access))
                }

                item {
                    QuickAccessGrid(
                        columns = gridColumns,
                        onOpenLibrary = onOpenLibrary,
                        onOpenDownloads = onOpenLibrary,
                        playlists = playlists,
                        onOpenPlaylist = onOpenPlaylist
                    )
                }

                if (recent.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Son çalınanlar")
                    }
                    item {
                        RecentCarousel(
                            tracks = recent.take(10),
                            onPlay = { index ->
                                viewModel.play(recent, index)
                                onOpenPlayer()
                            }
                        )
                    }

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
}

@Composable
private fun HeaderRow(onOpenDrawer: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 8.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenDrawer) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menüyü aç",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
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

/** Big gradient card: artwork backdrop, "Karıştır" and a shortcut to liked songs. */
@Composable
private fun HeroCard(
    track: TrackEntity,
    onShuffle: () -> Unit,
    onOpenLiked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(172.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF3A1D6E), Color(0xFF123B2A))
                )
            )
    ) {
        if (!track.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.32f,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bugünün ritmi",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.78f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.78f)
            )
            Text(
                text = track.artist.ifBlank { stringResource(R.string.player_unknown_artist) },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.78f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SpoytGreen,
                    onClick = onShuffle
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.home_shuffle),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f),
                    onClick = onOpenLiked
                ) {
                    Text(
                        text = stringResource(R.string.home_liked),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

/** Horizontal artwork cards for the most recent tracks. */
@Composable
private fun RecentCarousel(tracks: List<TrackEntity>, onPlay: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(tracks) { index, track ->
            Column(
                modifier = Modifier
                    .width(132.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onPlay(index) }
                    .padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (!track.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist.ifBlank { stringResource(R.string.player_unknown_artist) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickAccessGrid(
    columns: Int,
    onOpenLibrary: () -> Unit,
    onOpenDownloads: () -> Unit,
    playlists: List<com.example.spoyt.data.local.entities.PlaylistEntity>,
    onOpenPlaylist: (Int) -> Unit
) {
    val likedTitle = stringResource(R.string.home_liked)
    val likedSubtitle = stringResource(R.string.home_liked_subtitle)
    val downloadsTitle = stringResource(R.string.home_downloads)
    val downloadsSubtitle = stringResource(R.string.home_downloads_subtitle)
    val playlistSubtitle = stringResource(R.string.playlist_title)

    val tiles = buildList {
        add(
            QuickTileSpec(
                title = likedTitle,
                subtitle = likedSubtitle,
                icon = Icons.Default.Favorite,
                gradient = listOf(Color(0xFF7C3AED), Color(0xFFB06BFF)),
                onClick = onOpenLibrary
            )
        )
        add(
            QuickTileSpec(
                title = downloadsTitle,
                subtitle = downloadsSubtitle,
                icon = Icons.Default.GetApp,
                gradient = listOf(Color(0xFF0E7C4A), SpoytGreen),
                onClick = onOpenDownloads
            )
        )
        playlists.forEach { playlist ->
            add(
                QuickTileSpec(
                    title = playlist.name,
                    subtitle = playlistSubtitle,
                    icon = Icons.Default.LibraryMusic,
                    gradient = listOf(SpoytViolet, Color(0xFF4ECDC4)),
                    onClick = { onOpenPlaylist(playlist.id) }
                )
            )
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiles.chunked(columns.coerceAtLeast(1)).forEach { rowTiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTiles.forEach { tile ->
                    QuickTile(
                        title = tile.title,
                        subtitle = tile.subtitle,
                        icon = tile.icon,
                        gradient = tile.gradient,
                        modifier = Modifier.weight(1f),
                        onClick = tile.onClick
                    )
                }
                repeat((columns - rowTiles.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class QuickTileSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val onClick: () -> Unit
)

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
