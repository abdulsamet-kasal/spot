package com.example.spoyt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spoyt.data.local.entities.PlaylistEntity
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.di.AppContainer
import com.example.spoyt.download.Downloads
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Playlist detail screen: rename/delete the playlist, list its tracks, add/remove/reorder rows.
 */
class PlaylistDetailViewModel : ViewModel() {

    private val playlistIdFlow = MutableStateFlow(0)

    private val _playlist = MutableStateFlow<PlaylistEntity?>(null)
    val playlist: StateFlow<PlaylistEntity?> = _playlist.asStateFlow()

    private val _tracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    val tracks: StateFlow<List<TrackEntity>> = _tracks.asStateFlow()

    /** videoIds marked as liked, kept in sync so heart icons render correctly. */
    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds: StateFlow<Set<String>> = _likedIds.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                AppContainer.tracks.getLikedTracks().collect { list ->
                    _likedIds.value = list.mapTo(HashSet()) { it.videoId }
                }
            }
        }
    }

    private var loadedId = 0

    fun load(playlistId: Int) {
        if (loadedId == playlistId && _playlist.value != null) return
        loadedId = playlistId
        playlistIdFlow.value = playlistId

        viewModelScope.launch {
            runCatching { _playlist.value = AppContainer.playlists.getPlaylist(playlistId.toLong()) }
        }
        viewModelScope.launch {
            runCatching {
                AppContainer.playlistTracks.getTracksInPlaylist(playlistId).collect { list ->
                    _tracks.value = list
                }
            }
        }
    }

    fun rename(newName: String) {
        val id = loadedId
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                AppContainer.playlists.renamePlaylist(id.toLong(), trimmed)
                _playlist.value = AppContainer.playlists.getPlaylist(id.toLong())
            }
        }
    }

    fun deletePlaylist() {
        val id = loadedId
        viewModelScope.launch {
            runCatching { AppContainer.playlists.deletePlaylist(id.toLong()) }
        }
    }

    fun playAll() {
        val list = _tracks.value
        if (list.isEmpty()) return
        viewModelScope.playTracks(list, 0)
    }

    fun playAt(index: Int) {
        val list = _tracks.value
        if (index !in list.indices) return
        viewModelScope.playTracks(list, index)
    }

    fun removeTrack(videoId: String) {
        val id = loadedId
        viewModelScope.launch {
            runCatching { AppContainer.playlistTracks.removeTrack(id, videoId) }
        }
    }

    fun toggleLike(track: TrackEntity) {
        viewModelScope.toggleLike(track)
    }

    fun download(track: TrackEntity) {
        viewModelScope.enqueueDownload(track)
    }

    /** Adds an already-known track to this playlist (used by the "add to playlist" menu). */
    fun addTrack(videoId: String) {
        val id = loadedId
        viewModelScope.launch {
            runCatching {
                val position = _tracks.value.size
                AppContainer.playlistTracks.addTrack(id, videoId, position)
            }
        }
    }
}
