package com.example.spoyt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spoyt.data.local.entities.PlaylistEntity
import com.example.spoyt.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the "add to playlist" picker dialog. */
class AddToPlaylistViewModel : ViewModel() {

    val playlists: StateFlow<List<PlaylistEntity>> =
        AppContainer.playlists.getAllPlaylists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addToPlaylist(playlistId: Int, videoId: String) {
        viewModelScope.launch {
            runCatching {
                val existing = AppContainer.playlistTracks.getVideoIdsInPlaylist(playlistId)
                if (!existing.contains(videoId)) {
                    AppContainer.playlistTracks.addTrack(playlistId, videoId, existing.size)
                }
            }
        }
    }

    fun createAndAdd(name: String, videoId: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val id = AppContainer.playlists.createPlaylist(trimmed).toInt()
                AppContainer.playlistTracks.addTrack(id, videoId, 0)
            }
        }
    }
}
