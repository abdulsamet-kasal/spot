package com.example.spoyt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.data.remote.SearchResult
import com.example.spoyt.data.remote.YouTubeUtils
import com.example.spoyt.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

/**
 * Drives the search screen. NewPipe calls are isolated behind [com.example.spoyt.data.remote.YoutubeSource]
 * and always run on [Dispatchers.IO]; failures become a Turkish message instead of a crash.
 */
class SearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * Liked/download state is read from Room rather than kept in the search result model:
     * a result is a plain snapshot, so without this the heart icon and the download badge
     * never changed after a tap (the "senkronizasyon" problem).
     */
    val likedIds: StateFlow<Set<String>> = AppContainer.tracks.getLikedTracks()
        .map { list -> list.map { it.videoId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val downloadStates: StateFlow<Map<String, String>> = AppContainer.downloads.getAllDownloads()
        .map { list -> list.associate { it.videoId to it.state } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Past queries shown as tappable chips when the search field is empty. */
    val recentQueries: StateFlow<List<String>> = AppContainer.searchHistory.getRecent(10)
        .map { list -> list.map { it.query } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var searchJob: Job? = null

    /** Debounced search: fires ~400ms after the user stops typing. */
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, error = null) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), loading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            search(query.trim())
        }
    }

    /** Immediate search (used by the keyboard "search" action). */
    fun searchNow() {
        searchJob?.cancel()
        val query = _uiState.value.query.trim()
        if (query.isNotEmpty()) {
            searchJob = viewModelScope.launch { search(query) }
        }
    }

    private suspend fun search(query: String) {
        _uiState.update { it.copy(loading = true, error = null) }
        val results = try {
            withContext(Dispatchers.IO) { AppContainer.youtube.search(query) }
        } catch (t: Throwable) {
            _uiState.update {
                it.copy(loading = false, error = "Arama sırasında bir sorun oluştu. Tekrar deneyin.")
            }
            return
        }

        // Record the query so it comes back as a chip later (only on successful searches).
        if (results.isNotEmpty()) {
            runCatching { withContext(Dispatchers.IO) { AppContainer.searchHistory.record(query) } }
        }

        // Persist results so playlists/likes/downloads can reference them later.
        // Only insert missing rows: saveTracks() would REPLACE existing rows with these
        // fresh snapshots and wipe their liked flag (a real state-sync bug).
        runCatching {
            withContext(Dispatchers.IO) {
                results.forEach { result ->
                    if (!AppContainer.tracks.trackExists(result.videoId)) {
                        AppContainer.tracks.saveTrack(result.toTrackEntity())
                    }
                }
            }
        }

        _uiState.update {
            it.copy(
                loading = false,
                results = results,
                error = if (results.isEmpty()) "Sonuç bulunamadı." else null
            )
        }
    }

    fun play(index: Int) {
        val results = _uiState.value.results
        if (index !in results.indices) return
        val tracks = results.map { it.toTrackEntity() }
        viewModelScope.playTracks(tracks, index)
    }

    fun playAll() {
        val results = _uiState.value.results
        if (results.isEmpty()) return
        viewModelScope.playTracks(results.map { it.toTrackEntity() }, 0)
    }

    /**
     * The search field holds a YouTube link: resolve its metadata and play it directly,
     * replacing the queue (like tapping a single search result would).
     */
    fun playFromUrl(onReady: () -> Unit = {}) {
        val videoId = YouTubeUtils.extractVideoId(_uiState.value.query.trim()) ?: return
        viewModelScope.launch {
            runCatching {
                val result = withContext(Dispatchers.IO) { AppContainer.youtube.trackFor(videoId) }
                if (result == null) {
                    _uiState.update { it.copy(error = "Bu bağlantıdan şarkı alınamadı.") }
                } else {
                    AppContainer.playback.play(result.toTrackEntity())
                    onReady()
                }
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            runCatching { AppContainer.searchHistory.clearAll() }
        }
    }

    fun toggleLike(videoId: String) {
        viewModelScope.launch {
            runCatching {
                val existing = AppContainer.tracks.getTrack(videoId)
                if (existing == null) {
                    val found = _uiState.value.results.find { it.videoId == videoId } ?: return@launch
                    AppContainer.tracks.saveTrack(found.toTrackEntity().copy(liked = true))
                } else {
                    AppContainer.tracks.setLiked(videoId, !existing.liked)
                }
            }
        }
    }

    fun download(track: TrackEntity) {
        viewModelScope.enqueueDownload(track)
    }

    fun addToQueue(track: TrackEntity) {
        viewModelScope.launch {
            runCatching {
                if (!AppContainer.tracks.trackExists(track.videoId)) {
                    AppContainer.tracks.saveTrack(track)
                }
                com.example.spoyt.player.PlayerHolder.addToQueue(listOf(track))
            }
        }
    }
}

/** Maps a search result onto the Room entity used everywhere else in the app. */
fun SearchResult.toTrackEntity(): TrackEntity = TrackEntity(
    videoId = videoId,
    title = title,
    artist = artist,
    durationSeconds = durationSeconds,
    thumbnailUrl = thumbnailUrl
)
