package com.example.spoyt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spoyt.data.local.entities.DownloadEntity
import com.example.spoyt.data.local.entities.PlaylistEntity
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.di.AppContainer
import com.example.spoyt.download.Downloads
import com.example.spoyt.player.PlayerHolder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Library screen: liked songs, downloads and playlists.
 */
class LibraryViewModel : ViewModel() {

    val likedTracks: StateFlow<List<TrackEntity>> =
        AppContainer.tracks.getLikedTracks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every track we have ever saved; used to resolve download rows back to their metadata. */
    val allTracks: StateFlow<List<TrackEntity>> =
        AppContainer.tracks.getAllTracks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> =
        AppContainer.downloads.getAllDownloads()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> =
        AppContainer.playlists.getAllPlaylists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** "Geçmiş" tab: everything played, most recent first. */
    val recentTracks: StateFlow<List<TrackEntity>> =
        AppContainer.history.getRecentTracks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** "Geçmiş" tab: most played tracks, by play count. */
    val topTracks: StateFlow<List<TrackEntity>> =
        AppContainer.history.getMostPlayedTracks(50)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createPlaylist(name: String, onCreated: (Int) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val id = AppContainer.playlists.createPlaylist(trimmed)
                onCreated(id.toInt())
            }
        }
    }

    fun deletePlaylist(id: Int) {
        viewModelScope.launch { runCatching { AppContainer.playlists.deletePlaylist(id.toLong()) } }
    }

    fun play(tracks: List<TrackEntity>, index: Int = 0) {
        viewModelScope.playTracks(tracks, index)
    }

    fun toggleLike(track: TrackEntity) {
        viewModelScope.toggleLike(track)
    }

    fun deleteDownload(videoId: String) {
        Downloads.delete(AppContainer.context, videoId)
    }

    /** Starts an offline download for a track the user picked from a library row. */
    fun download(track: TrackEntity) {
        viewModelScope.enqueueDownload(track)
    }

    /** Plays the track right after the current one without touching the rest of the queue. */
    fun addToQueue(track: TrackEntity) {
        viewModelScope.launch {
            runCatching {
                if (!AppContainer.tracks.trackExists(track.videoId)) {
                    AppContainer.tracks.saveTrack(track)
                }
                PlayerHolder.addToQueue(listOf(track))
            }
        }
    }

    fun playDownloaded(videoId: String) {
        viewModelScope.launch {
            runCatching {
                val track = AppContainer.tracks.getTrack(videoId) ?: return@launch
                AppContainer.playback.play(track)
            }
        }
    }
}
