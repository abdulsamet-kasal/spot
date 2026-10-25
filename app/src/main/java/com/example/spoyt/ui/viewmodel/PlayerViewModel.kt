package com.example.spoyt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.di.AppContainer
import com.example.spoyt.player.PlayerHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val track: TrackEntity? = null,
    val queue: List<TrackEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

/**
 * Full-player screen state. Position/duration are polled (500ms) because ExoPlayer reports them
 * synchronously rather than through a Flow.
 */
class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /** videoIds currently marked as liked, so the heart icon stays in sync across screens. */
    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds: StateFlow<Set<String>> = _likedIds.asStateFlow()

    init {
        viewModelScope.launch {
            PlayerHolder.currentTrackState.collect { track ->
                _uiState.update { it.copy(track = track) }
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
                delay(500)
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

    fun download(track: TrackEntity) {
        viewModelScope.enqueueDownload(track)
    }

    fun toggleLike(track: TrackEntity) {
        viewModelScope.toggleLike(track)
    }
}
