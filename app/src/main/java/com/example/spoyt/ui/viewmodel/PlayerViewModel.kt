package com.example.spoyt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.data.remote.LrcLine
import com.example.spoyt.di.AppContainer
import com.example.spoyt.player.PlaybackEvents
import com.example.spoyt.player.PlayerHolder
import com.example.spoyt.player.RepeatMode
import com.example.spoyt.player.SleepTimer
import com.example.spoyt.ui.components.Thumbnails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlayerUiState(
    val track: TrackEntity? = null,
    val queue: List<TrackEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

/** Lyrics lookup state for the current track (loaded on demand from the lyrics sheet). */
data class LyricsUiState(
    val loading: Boolean = false,
    val synced: List<LrcLine> = emptyList(),
    val plain: String? = null,
    val notFound: Boolean = false
)

/**
 * Full-player screen state. Position/duration are polled (250ms) because ExoPlayer reports them
 * synchronously rather than through a Flow.
 */
class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /** videoIds currently marked as liked, so the heart icon stays in sync across screens. */
    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds: StateFlow<Set<String>> = _likedIds.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffling = MutableStateFlow(false)
    val isShuffling: StateFlow<Boolean> = _isShuffling.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepRemainingMs = MutableStateFlow(0L)
    val sleepRemainingMs: StateFlow<Long> = _sleepRemainingMs.asStateFlow()

    private val _lyrics = MutableStateFlow(LyricsUiState())
    val lyrics: StateFlow<LyricsUiState> = _lyrics.asStateFlow()

    /** Dominant dark color extracted from the artwork, used as the player background accent. */
    private val _artworkColor = MutableStateFlow<Int?>(null)
    val artworkColor: StateFlow<Int?> = _artworkColor.asStateFlow()

    private val imageLoader: ImageLoader by lazy { ImageLoader(AppContainer.context) }

    /** videoId -> lyrics result, so re-opening the sheet never re-hits the network. */
    private val lyricsCache = HashMap<String, LyricsUiState>()

    init {
        viewModelScope.launch {
            PlayerHolder.currentTrackState.collect { track ->
                _uiState.update { it.copy(track = track) }
                _artworkColor.value = null
                track?.let { loadPalette(it) }
            }
        }
        viewModelScope.launch {
            PlayerHolder.queueState.collect { queue ->
                _uiState.update { it.copy(queue = queue) }
            }
        }
        viewModelScope.launch {
            PlayerHolder.currentIndexState.collect { index ->
                _uiState.update { it.copy(currentIndex = index) }
            }
        }
        viewModelScope.launch {
            PlayerHolder.isPlayingState.collect { playing ->
                _uiState.update { it.copy(isPlaying = playing) }
            }
        }
        viewModelScope.launch {
            PlayerHolder.isBufferingState.collect { buffering ->
                _uiState.update { it.copy(isBuffering = buffering) }
            }
        }
        viewModelScope.launch {
            PlayerHolder.repeatModeState.collect { mode ->
                _repeatMode.value = mode
            }
        }
        viewModelScope.launch {
            PlayerHolder.isShufflingState.collect { on ->
                _isShuffling.value = on
            }
        }
        viewModelScope.launch {
            PlayerHolder.playbackSpeedState.collect { speed ->
                _playbackSpeed.value = speed
            }
        }
        viewModelScope.launch {
            SleepTimer.remainingMs.collect { remaining ->
                _sleepRemainingMs.value = remaining
            }
        }
        viewModelScope.launch {
            AppContainer.tracks.getLikedTracks().collect { list ->
                _likedIds.value = list.mapTo(HashSet()) { it.videoId }
            }
        }
        viewModelScope.launch {
            while (true) {
                _uiState.update {
                    it.copy(
                        positionMs = PlayerHolder.positionMs,
                        durationMs = PlayerHolder.durationMs
                    )
                }
                delay(250)
            }
        }
    }

    fun playPause() = PlayerHolder.playPause()

    fun next() = PlayerHolder.playNext()

    fun previous() = PlayerHolder.playPrevious()

    fun seekTo(positionMs: Long) {
        PlayerHolder.seekTo(positionMs)
        _uiState.update { it.copy(positionMs = positionMs) }
    }

    fun playAt(index: Int) = PlayerHolder.playIndex(index)

    /** Queues the given track after the current one (no queue replacement). */
    fun addToQueue(track: TrackEntity) {
        PlayerHolder.addToQueue(listOf(track))
    }

    /** Queue management from the bottom sheet (only upcoming rows are touchable). */
    fun removeFromQueue(index: Int) = PlayerHolder.removeFromQueue(index)

    fun moveQueueItem(from: Int, to: Int) = PlayerHolder.moveInQueue(from, to)

    /** Cycles repeat OFF -> ALL -> ONE -> OFF. */
    fun cycleRepeatMode() = PlayerHolder.cycleRepeatMode()

    /** Reorders the upcoming queue and keeps it reordered while shuffle is on. */
    fun toggleShuffle() = PlayerHolder.toggleShuffle()

    /** Playback speed multiplier (0.5x..2.5x). */
    fun setSpeed(speed: Float) = PlayerHolder.setSpeed(speed)

    /** Sleep timer: minutes <= 0 cancels. */
    fun startSleepTimer(minutes: Int) {
        if (minutes <= 0) SleepTimer.cancel() else SleepTimer.start(minutes)
    }

    /** Radio: queues the current track plus related tracks resolved from YouTube. */
    fun startRadio() {
        val track = _uiState.value.track ?: return
        viewModelScope.launch {
            PlaybackEvents.post("Radyo hazırlanıyor…")
            runCatching { AppContainer.playback.startRadio(track) }
        }
    }

    /**
     * Loads lyrics for [track] on demand (called when the lyrics sheet opens). Results are
     * cached in memory per videoId.
     */
    fun ensureLyrics(track: TrackEntity) {
        val cached = lyricsCache[track.videoId]
        if (cached != null) {
            _lyrics.value = cached
            return
        }
        _lyrics.value = LyricsUiState(loading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                AppContainer.lyrics.fetch(track.title, track.artist, track.durationSeconds)
            }.getOrNull()
            val state = result?.let {
                LyricsUiState(plain = it.plain, synced = it.synced)
            } ?: LyricsUiState(notFound = true)
            synchronized(lyricsCache) { lyricsCache[track.videoId] = state }
            _lyrics.value = state
        }
    }

    private fun loadPalette(track: TrackEntity) {
        val url = Thumbnails.high(track.thumbnailUrl) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(AppContainer.context)
                    .data(url)
                    .allowHardware(false)
                    .build()
                val drawable = imageLoader.execute(request).drawable
                val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    ?: return@launch
                val palette = androidx.palette.graphics.Palette.from(bitmap)
                    .maximumColorCount(24)
                    .generate()
                val fallback = 0xFF101015.toInt()
                val color = palette.getDarkVibrantColor(
                    palette.getDarkMutedColor(fallback)
                )
                withContext(Dispatchers.Main) {
                    _artworkColor.value = color
                }
            }
        }
    }

    fun download(track: TrackEntity) {
        viewModelScope.enqueueDownload(track)
    }

    fun toggleLike(track: TrackEntity) {
        viewModelScope.toggleLike(track)
    }
}
