package com.example.spoyt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.spoyt.R
import com.example.spoyt.data.local.entities.TrackEntity

/**
 * One row in a track list: thumbnail, title, artist, duration and the three-dots "more" menu.
 *
 * The menu only shows entries whose callback is non-null, so each screen opts into the actions
 * that make sense there (search has "add to playlist", playlist detail has "remove", ...).
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
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index != null) {
            Text(
                text = (index + 1).toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!track.thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildSubtitle(track, downloadState),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (track.durationSeconds > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatDuration(track.durationSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(4.dp))
            trailing()
        }

        val hasMenu = onLikeToggle != null || onDownload != null || onAddToPlaylist != null ||
            onPlayNext != null || onRemove != null

        if (hasMenu) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (onLikeToggle != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (isLiked) R.string.menu_unlike else R.string.menu_like
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite
                                    else Icons.Default.FavoriteBorder,
                                    contentDescription = null
                                )
                            },
                            onClick = { menuOpen = false; onLikeToggle() }
                        )
                    }
                    if (onPlayNext != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_play_next)) },
                            leadingIcon = {
                                Icon(Icons.Default.QueueMusic, contentDescription = null)
                            },
                            onClick = { menuOpen = false; onPlayNext() }
                        )
                    }
                    if (onDownload != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_download)) },
                            leadingIcon = {
                                Icon(Icons.Default.Download, contentDescription = null)
                            },
                            onClick = { menuOpen = false; onDownload() }
                        )
                    }
                    if (onAddToPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_add_playlist)) },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                            },
                            onClick = { menuOpen = false; onAddToPlaylist() }
                        )
                    }
                    if (onRemove != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_delete_from_playlist)) },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null)
                            },
                            onClick = { menuOpen = false; onRemove() }
                        )
                    }
                }
            }
        }
    }
}

private fun buildSubtitle(track: TrackEntity, downloadState: String?): String {
    val artist = track.artist.ifBlank { "—" }
    val download = when (downloadState) {
        "QUEUED" -> " · Sırada"
        "DOWNLOADING" -> " · İndiriliyor"
        "DONE" -> " · İndirildi"
        "FAILED" -> " · Başarısız"
        else -> null
    }
    return if (download == null) artist else artist + download
}
