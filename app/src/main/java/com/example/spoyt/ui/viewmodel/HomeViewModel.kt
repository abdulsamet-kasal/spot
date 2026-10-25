package com.example.spoyt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spoyt.data.local.entities.PlaylistEntity
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.di.AppContainer
import com.example.spoyt.player.PlayerHolder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Home screen: recently played tracks, liked shortcuts, and the user's playlists.
 *
 * No-arg constructor so the default [androidx.lifecycle.ViewModelProvider] factory can build it;
 * dependencies come from the manual [AppContainer].
 */
class HomeViewModel : ViewModel() {

    val recentTracks: StateFlow<List<TrackEntity>> =
        AppContainer.history.getRecentTracks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val likedTracks: StateFlow<List<TrackEntity>> =
        AppContainer.tracks.getLikedTracks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> =
        AppContainer.playlists.getAllPlaylists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun play(tracks: List<TrackEntity>, index: Int = 0) {
        viewModelScope.playTracks(tracks, index)
    }

    /** "Karıştır": plays the given list in random order. */
    fun playShuffled(tracks: List<TrackEntity>) {
        if (tracks.isEmpty()) return
        viewModelScope.playTracks(tracks.shuffled(), 0)
    }

    fun toggleLike(track: TrackEntity) {
        viewModelScope.toggleLike(track)
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
                PlayerHolder.addToQueue(listOf(track))
            }
        }
    }
}
