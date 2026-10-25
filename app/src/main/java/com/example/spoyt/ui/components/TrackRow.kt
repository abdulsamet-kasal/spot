package com.example.spoyt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.spoyt.R
import com.example.spoyt.data.local.entities.TrackEntity

/**
 * One row in a track list: artwork, title, artist, download progress and the three-dots
 * "more" menu. The menu only shows entries whose callback is non-null, so each screen opts
 * into the actions that make sense there.
 */
@Composable
fun TrackRow(
    track: TrackEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int? = null,
    isCurrent: Boolean = false,
    isLiked: Boolean = false,
    downloadState: String? = null,
    downloadProgress: Int? = null,
    onLikeToggle: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index != null) {
            Text(
                text = (index + 1).toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .width(22.dp)
                    .padding(start = 2.dp)
            )
        }

        Artwork(url = track.thumbnailUrl, size = 50.dp, cornerRadius = 12.dp)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (downloadState == "DONE") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildSubtitle(track, downloadState),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (track.durationSeconds > 0) {
                    Text(
                        text = "  ·  " + formatDuration(track.durationSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (downloadState == "DOWNLOADING" && downloadProgress != null) {
                Spacer(modifier = Modifier.size(4.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(4.dp))
            trailing()
        }

        val hasMenu = onLikeToggle != null || onDownload != null || onAddToPlaylist != null ||
            onPlayNext != null || onRemove != null

        if (hasMenu) {
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (onLikeToggle != null) {
                        MenuItem(
                            text = stringResource(
                                if (isLiked) R.string.menu_unlike else R.string.menu_like
                            ),
                            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                        ) { menuOpen = false; onLikeToggle() }
                    }
                    if (onPlayNext != null) {
                        MenuItem(
                            text = stringResource(R.string.menu_play_next),
                            icon = Icons.Default.QueueMusic
                        ) { menuOpen = false; onPlayNext() }
                    }
                    if (onDownload != null) {
                        MenuItem(
                            text = stringResource(R.string.menu_download),
                            icon = Icons.Default.Download
                        ) { menuOpen = false; onDownload() }
                    }
                    if (onAddToPlaylist != null) {
                        MenuItem(
                            text = stringResource(R.string.menu_add_playlist),
                            icon = Icons.Default.PlaylistAdd
                        ) { menuOpen = false; onAddToPlaylist() }
                    }
                    if (onRemove != null) {
                        MenuItem(
                            text = stringResource(R.string.menu_delete_from_playlist),
                            icon = Icons.Default.PlaylistRemove
                        ) { menuOpen = false; onRemove() }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        },
        onClick = onClick
    )
}

private fun buildSubtitle(track: TrackEntity, downloadState: String?): String {
    val artist = track.artist.ifBlank { "Bilinmeyen sanatçı" }
    return when (downloadState) {
        "QUEUED" -> "$artist · Sırada"
        "DOWNLOADING" -> "$artist · İndiriliyor"
        "DONE" -> artist
        "FAILED" -> "$artist · İndirilemedi"
        else -> artist
    }
}
