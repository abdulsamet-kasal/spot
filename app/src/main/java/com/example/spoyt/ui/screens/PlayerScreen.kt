package com.example.spoyt.ui.screens

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.spoyt.R
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.player.RepeatMode
import com.example.spoyt.ui.components.AddToPlaylistDialog
import com.example.spoyt.ui.components.SpoytBackground
import com.example.spoyt.ui.components.Thumbnails
import com.example.spoyt.ui.theme.SpoytGreen
import com.example.spoyt.ui.theme.SpoytViolet
import com.example.spoyt.ui.viewmodel.PlayerUiState
import com.example.spoyt.ui.viewmodel.PlayerViewModel

/**
 * Full-screen player: large artwork, seek bar, transport controls, secondary actions and the
 * up-next queue (inline on compact screens, side-by-side on tablets/landscape).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedIds.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffling by viewModel.isShuffling.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val sleepRemainingMs by viewModel.sleepRemainingMs.collectAsStateWithLifecycle()
    val artworkColor by viewModel.artworkColor.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyrics.collectAsStateWithLifecycle()
    val track = state.track
    var queueSheetOpen by remember { mutableStateOf(false) }
    var addToPlaylistVideoId by remember { mutableStateOf<String?>(null) }
    var lyricsSheetOpen by remember { mutableStateOf(false) }
    var sleepDialogOpen by remember { mutableStateOf(false) }
    var speedDialogOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun shareCurrent() {
        val current = track ?: return
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "${current.title} — ${current.artist}\n" +
                        "https://www.youtube.com/watch?v=${current.videoId}"
                )
            }
            context.startActivity(
                Intent.createChooser(send, context.getString(R.string.menu_share))
            )
        }
    }

    val accent = artworkColor?.let { Color(it) }
        ?: (if (state.isPlaying) SpoytGreen else SpoytViolet)

    SpoytBackground(accent = accent) {
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

        val liked = likedIds.contains(track.videoId)
        val upcoming = state.queue.drop(state.currentIndex + 1)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wide = maxWidth >= 720.dp

            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1200.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PlayerTopBar(
                            onBack = onBack,
                            liked = liked,
                            onLikeToggle = { viewModel.toggleLike(track) },
                            onDownload = { viewModel.download(track) },
                            onPlayNext = { viewModel.addToQueue(track) },
                            onAddToPlaylist = { addToPlaylistVideoId = track.videoId },
                            onRadio = { viewModel.startRadio() },
                            onLyrics = {
                                viewModel.ensureLyrics(track)
                                lyricsSheetOpen = true
                            },
                            onShare = { shareCurrent() },
                            onSleepTimer = { sleepDialogOpen = true },
                            onSpeed = { speedDialogOpen = true }
                        )
                        ArtworkBlock(track = track, modifier = Modifier.fillMaxWidth(0.88f))
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                            .padding(start = 12.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TitleBlock(
                            track = track,
                            liked = liked,
                            onLikeToggle = { viewModel.toggleLike(track) }
                        )
                        SeekBlock(
                            positionMs = state.positionMs,
                            durationMs = state.durationMs,
                            onSeek = viewModel::seekTo
                        )
                        Controls(
                            isPlaying = state.isPlaying,
                            isBuffering = state.isBuffering,
                            onPrevious = viewModel::previous,
                            onPlayPause = viewModel::playPause,
                            onNext = viewModel::next
                        )
                        ActionRow(
                            liked = liked,
                            repeatMode = repeatMode,
                            shuffling = shuffling,
                            onLikeToggle = { viewModel.toggleLike(track) },
                            onAddToPlaylist = { addToPlaylistVideoId = track.videoId },
                            onDownload = { viewModel.download(track) },
                            onOpenQueue = { queueSheetOpen = true },
                            onRepeat = { viewModel.cycleRepeatMode() },
                            onShuffle = { viewModel.toggleShuffle() }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        QueueList(
                            state = state,
                            upcoming = upcoming,
                            modifier = Modifier.weight(1f),
                            onTrackClick = { index -> viewModel.playAt(index) },
                            onRemove = viewModel::removeFromQueue,
                            onMoveUp = { index -> viewModel.moveQueueItem(index, index - 1) },
                            onMoveDown = { index -> viewModel.moveQueueItem(index, index + 1) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    item {
                        PlayerTopBar(
                            onBack = onBack,
                            liked = liked,
                            onLikeToggle = { viewModel.toggleLike(track) },
                            onDownload = { viewModel.download(track) },
                            onPlayNext = { viewModel.addToQueue(track) },
                            onAddToPlaylist = { addToPlaylistVideoId = track.videoId },
                            onRadio = { viewModel.startRadio() },
                            onLyrics = {
                                viewModel.ensureLyrics(track)
                                lyricsSheetOpen = true
                            },
                            onShare = { shareCurrent() },
                            onSleepTimer = { sleepDialogOpen = true },
                            onSpeed = { speedDialogOpen = true }
                        )
                    }
                    item { ArtworkBlock(track = track) }
                    item {
                        TitleBlock(
                            track = track,
                            liked = liked,
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
                    item {
                        ActionRow(
                            liked = liked,
                            repeatMode = repeatMode,
                            shuffling = shuffling,
                            onLikeToggle = { viewModel.toggleLike(track) },
                            onAddToPlaylist = { addToPlaylistVideoId = track.videoId },
                            onDownload = { viewModel.download(track) },
                            onOpenQueue = { queueSheetOpen = true },
                            onRepeat = { viewModel.cycleRepeatMode() },
                            onShuffle = { viewModel.toggleShuffle() }
                        )
                    }
                    if (upcoming.isNotEmpty()) {
                        item { QueueHeader(count = upcoming.size) }
                        itemsIndexed(upcoming) { i, item ->
                            QueueRow(
                                track = item,
                                index = state.currentIndex + 2 + i,
                                onClick = { viewModel.playAt(state.currentIndex + 1 + i) },
                                onRemove = { viewModel.removeFromQueue(state.currentIndex + 1 + i) },
                                onMoveUp = if (i > 0) {
                                    { viewModel.moveQueueItem(state.currentIndex + 1 + i, state.currentIndex + i) }
                                } else null,
                                onMoveDown = if (i < upcoming.lastIndex) {
                                    { viewModel.moveQueueItem(state.currentIndex + 1 + i, state.currentIndex + 2 + i) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        if (queueSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { queueSheetOpen = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                QueueList(
                    state = state,
                    upcoming = upcoming,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    onTrackClick = { index ->
                        viewModel.playAt(index)
                        queueSheetOpen = false
                    },
                    onRemove = { index -> viewModel.removeFromQueue(index) },
                    onMoveUp = { index -> viewModel.moveQueueItem(index, index - 1) },
                    onMoveDown = { index -> viewModel.moveQueueItem(index, index + 1) }
                )
            }
        }
    }

    if (sleepDialogOpen) {
        SleepTimerDialog(
            remainingMs = sleepRemainingMs,
            onSelect = { minutes ->
                viewModel.startSleepTimer(minutes)
                sleepDialogOpen = false
            },
            onDismiss = { sleepDialogOpen = false }
        )
    }

    if (speedDialogOpen) {
        SpeedDialog(
            current = playbackSpeed,
            onSelect = { speed ->
                viewModel.setSpeed(speed)
                speedDialogOpen = false
            },
            onDismiss = { speedDialogOpen = false }
        )
    }

    if (lyricsSheetOpen) {
        LyricsSheet(
            state = lyricsState,
            positionMs = state.positionMs,
            onDismiss = { lyricsSheetOpen = false }
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
private fun PlayerTopBar(
    onBack: () -> Unit,
    liked: Boolean,
    onLikeToggle: () -> Unit,
    onDownload: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRadio: () -> Unit = {},
    onLyrics: () -> Unit = {},
    onShare: () -> Unit = {},
    onSleepTimer: () -> Unit = {},
    onSpeed: () -> Unit = {}
) {
    var menuOpen by remember { mutableStateOf(false) }

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
            text = stringResource(R.string.player_now_playing),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.action_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                PlayerMenuItem(
                    text = stringResource(if (liked) R.string.menu_unlike else R.string.menu_like),
                    icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                ) { menuOpen = false; onLikeToggle() }
                PlayerMenuItem(
                    text = stringResource(R.string.menu_play_next),
                    icon = Icons.Default.QueueMusic
                ) { menuOpen = false; onPlayNext() }
                PlayerMenuItem(
                    text = stringResource(R.string.menu_add_playlist),
                    icon = Icons.Default.PlaylistAdd
                ) { menuOpen = false; onAddToPlaylist() }
                PlayerMenuItem(
                    text = stringResource(R.string.menu_download),
                    icon = Icons.Default.Download
                ) { menuOpen = false; onDownload() }
                PlayerMenuItem(
                    text = stringResource(R.string.menu_radio),
                    icon = Icons.Default.Radio
                ) { menuOpen = false; onRadio() }
                PlayerMenuItem(
                    text = stringResource(R.string.menu_lyrics),
                    icon = Icons.Default.Lyrics
                ) { menuOpen = false; onLyrics() }
                PlayerMenuItem(
                    text = stringResource(R.string.menu_share),
                    icon = Icons.Default.Share
                ) { menuOpen = false; onShare() }
                PlayerMenuItem(
                    text = stringResource(R.string.menu_speed),
                    icon = Icons.Default.Speed
                ) { menuOpen = false; onSpeed() }
                PlayerMenuItem(
                    text = stringResource(R.string.menu_sleep_timer),
                    icon = Icons.Default.Bedtime
                ) { menuOpen = false; onSleepTimer() }
            }
        }
    }
}

@Composable
private fun PlayerMenuItem(
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

@Composable
private fun ArtworkBlock(track: TrackEntity, modifier: Modifier = Modifier) {
    val originalUrl = track.thumbnailUrl
    val highRes = remember(originalUrl) { Thumbnails.high(originalUrl) }
    val mediumRes = remember(originalUrl) { Thumbnails.medium(originalUrl) }
    var highResFailed by remember(originalUrl) { mutableStateOf(false) }
    val model = if (highResFailed) mediumRes else highRes

    Box(
        modifier = modifier
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
        if (!model.isNullOrBlank()) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                // maxresdefault.jpg does not exist for every video; fall back to hqdefault.
                onError = { if (!highResFailed) highResFailed = true },
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

/** Secondary actions: like, add to playlist, download, queue sheet, shuffle and repeat. */
@Composable
private fun ActionRow(
    liked: Boolean,
    repeatMode: RepeatMode,
    shuffling: Boolean,
    onLikeToggle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onOpenQueue: () -> Unit,
    onRepeat: () -> Unit,
    onShuffle: () -> Unit
) {
    val repeatIcon = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
    val repeatActive = repeatMode != RepeatMode.OFF
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SecondaryAction(
            icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = stringResource(if (liked) R.string.menu_unlike else R.string.menu_like),
            tint = if (liked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onLikeToggle
        )
        SecondaryAction(
            icon = Icons.Default.PlaylistAdd,
            label = stringResource(R.string.menu_add_playlist),
            onClick = onAddToPlaylist
        )
        SecondaryAction(
            icon = Icons.Default.Download,
            label = stringResource(R.string.menu_download),
            onClick = onDownload
        )
        SecondaryAction(
            icon = Icons.Default.Shuffle,
            label = stringResource(R.string.player_shuffle),
            tint = if (shuffling) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onShuffle
        )
        SecondaryAction(
            icon = repeatIcon,
            label = stringResource(R.string.player_repeat),
            tint = if (repeatActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onRepeat
        )
        SecondaryAction(
            icon = Icons.Default.QueueMusic,
            label = stringResource(R.string.player_queue_open),
            onClick = onOpenQueue
        )
    }
}

@Composable
private fun SecondaryAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 68.dp)
        )
    }
}

@Composable
private fun QueueHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.player_queue_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Scrollable queue used both inline (wide layout) and inside the bottom sheet. */
@Composable
private fun QueueList(
    state: PlayerUiState,
    upcoming: List<TrackEntity>,
    modifier: Modifier = Modifier,
    onTrackClick: ((Int) -> Unit)? = null,
    onRemove: ((Int) -> Unit)? = null,
    onMoveUp: ((Int) -> Unit)? = null,
    onMoveDown: ((Int) -> Unit)? = null
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item { QueueHeader(count = upcoming.size) }
        if (upcoming.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.player_queue_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 12.dp)
                )
            }
        }
        itemsIndexed(upcoming) { i, item ->
            val absoluteIndex = state.currentIndex + 1 + i
            QueueRow(
                track = item,
                index = absoluteIndex + 1,
                onClick = { onTrackClick?.invoke(absoluteIndex) },
                onRemove = onRemove?.let { cb -> { cb(absoluteIndex) } },
                onMoveUp = if (i > 0) {
                    { onMoveUp?.invoke(absoluteIndex) }
                } else null,
                onMoveDown = if (i < upcoming.lastIndex) {
                    { onMoveDown?.invoke(absoluteIndex) }
                } else null
            )
        }
    }
}

@Composable
private fun QueueRow(
    track: TrackEntity,
    index: Int,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }
    val hasMenu = onRemove != null || onMoveUp != null || onMoveDown != null

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
        if (hasMenu) {
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (onMoveUp != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.queue_move_up)) },
                            leadingIcon = {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null)
                            },
                            onClick = { menuOpen = false; onMoveUp() }
                        )
                    }
                    if (onMoveDown != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.queue_move_down)) },
                            leadingIcon = {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null)
                            },
                            onClick = { menuOpen = false; onMoveDown() }
                        )
                    }
                    if (onRemove != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.queue_remove)) },
                            leadingIcon = {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            },
                            onClick = { menuOpen = false; onRemove() }
                        )
                    }
                }
            }
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

/** Sleep timer picker: preset durations + "off". */
@Composable
private fun SleepTimerDialog(
    remainingMs: Long,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val remainingLabel = if (remainingMs > 0) {
        stringResource(R.string.sleep_active, formatPlayerTime(remainingMs))
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_title)) },
        text = {
            Column {
                remainingLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                listOf(15, 30, 60).forEach { minutes ->
                    TextButton(onClick = { onSelect(minutes) }) {
                        Text(stringResource(R.string.sleep_minutes, minutes))
                    }
                }
                TextButton(onClick = { onSelect(0) }) {
                    Text(stringResource(R.string.sleep_off))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/** Playback speed picker. */
@Composable
private fun SpeedDialog(
    current: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.speed_title)) },
        text = {
            Column {
                options.forEach { speed ->
                    TextButton(onClick = { onSelect(speed) }) {
                        Text(
                            text = "${speed}x",
                            color = if (speed == current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Lyrics sheet: synced lines follow the playback position; plain lyrics are shown as text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsSheet(
    state: com.example.spoyt.ui.viewmodel.LyricsUiState,
    positionMs: Long,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.lyrics_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            when {
                state.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                state.notFound || (state.synced.isEmpty() && state.plain == null) -> {
                    Text(
                        text = stringResource(R.string.lyrics_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }

                state.synced.isNotEmpty() -> {
                    val activeIndex = state.synced.indexOfLast { it.timeMs <= positionMs }
                    val listState = rememberLazyListState()
                    LaunchedEffect(activeIndex) {
                        if (activeIndex >= 0) {
                            runCatching { listState.animateScrollToItem(activeIndex) }
                        }
                    }
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(state.synced) { i, line ->
                            val active = i == activeIndex
                            Text(
                                text = line.text,
                                style = if (active) MaterialTheme.typography.titleMedium
                                else MaterialTheme.typography.bodyLarge,
                                color = if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = state.plain.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
