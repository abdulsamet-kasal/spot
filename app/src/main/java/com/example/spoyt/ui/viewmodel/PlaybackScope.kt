package com.example.spoyt.ui.viewmodel

import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.di.AppContainer
import com.example.spoyt.download.Downloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Small coroutine helpers shared by the ViewModels so each screen does not repeat the same
 * `viewModelScope.launch { ... }` plumbing. Every call is fire-and-forget: the playback/download
 * layers already swallow their own errors and surface Turkish messages through the UI state.
 */

fun CoroutineScope.playTracks(tracks: List<TrackEntity>, startIndex: Int = 0) {
    launch { runCatching { AppContainer.playback.play(tracks, startIndex) } }
}

fun CoroutineScope.playTrack(track: TrackEntity) {
    launch { runCatching { AppContainer.playback.play(track) } }
}

fun CoroutineScope.toggleLike(track: TrackEntity) {
    launch { runCatching { AppContainer.playback.toggleLike(track) } }
}

fun CoroutineScope.enqueueDownload(track: TrackEntity) {
    launch { runCatching { Downloads.enqueue(AppContainer.context, track) } }
}
