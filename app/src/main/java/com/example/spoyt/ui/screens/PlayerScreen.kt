package com.example.spoyt.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.spoyt.R
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.ui.components.SpoytBackground
import com.example.spoyt.ui.theme.SpoytGreen
import com.example.spoyt.ui.theme.SpoytViolet
import com.example.spoyt.ui.viewmodel.PlayerViewModel

/**
 * Full-screen player: large artwork, seek bar, transport controls and the up-next queue.
 * Everything scrolls so the layout survives small screens and landscape.
 */
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedIds.collectAsStateWithLifecycle()
    val track = state.track

    SpoytBackground(
        accent = if (state.isPlaying) SpoytGreen else SpoytViolet
    ) {
        if (track == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.player_unknown_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@SpoytBackground
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item { PlayerTopBar(onBack = onBack, onDownload = { viewModel.download(track) }) }

            item { ArtworkBlock(track = track) }

            item {
                TitleBlock(
                    track = track,
                    liked = likedIds.contains(track.videoId),
                    onLikeToggle = { viewModel.toggleLike(track) }
                )
            }

            item {
                SeekBlock(
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    onSeek = viewModel::seekTo
                )
            }

            item {
                Controls(
                    isPlaying = state.isPlaying,
                    isBuffering = state.isBuffering,
                    onPrevious = viewModel::previous,
                    onPlayPause = viewModel::playPause,
                    onNext = viewModel::next
                )
            }

            val upcoming = state.queue.drop(state.currentIndex + 1)
            if (upcoming.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sırada",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${upcoming.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                itemsIndexed(upcoming) { i, item ->
                    QueueRow(
                        track = item,
                        index = state.currentIndex + 2 + i,
                        onClick = { viewModel.playAt(state.currentIndex + 1 + i) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerTopBar(onBack: () -> Unit, onDownload: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "Şimdi çalıyor",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDownload) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = stringResource(R.string.menu_download),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ArtworkBlock(track: TrackEntity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 12.dp)
            .aspectRatio(1f)
            .shadow(
                elevation = 26.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color.Black,
                spotColor = SpoytGreen.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF1E1E27), Color(0xFF111116)))
            ),
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
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

@Composable
private fun TitleBlock(track: TrackEntity, liked: Boolean, onLikeToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track.artist.ifBlank { stringResource(R.string.player_unknown_artist) },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onLikeToggle, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = stringResource(
                    if (liked) R.string.menu_unlike else R.string.menu_like
                ),
                tint = if (liked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SeekBlock(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    val max = durationMs.coerceAtLeast(1L).toFloat()
    val progress by animateFloatAsState(
        targetValue = positionMs.coerceIn(0L, durationMs.coerceAtLeast(1L)).toFloat(),
        label = "seek"
    )

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Slider(
            value = progress,
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..max,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPlayerTime(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatPlayerTime(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Controls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(52.dp)) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = stringResource(R.string.player_previous),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.width(18.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(14.dp, CircleShape, spotColor = SpoytGreen.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(SpoytGreen, Color(0xFF12B34B)))
                )
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.player_pause else R.string.player_play
                    ),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(18.dp))

        IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = stringResource(R.string.player_next),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun QueueRow(track: TrackEntity, index: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
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

private fun formatPlayerTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0)) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
