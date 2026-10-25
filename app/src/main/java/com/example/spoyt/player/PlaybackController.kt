package com.example.spoyt.player

import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.data.local.repositories.DownloadRepository
import com.example.spoyt.data.local.repositories.HistoryRepository
import com.example.spoyt.data.local.repositories.TrackRepository
import com.example.spoyt.data.remote.YoutubeSource
import com.example.spoyt.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coordinates "play this list of tracks":
 *   1. Persists the track (search results are ephemeral until played/liked).
 *   2. Prefers an already-downloaded local file over a network stream.
 *   3. Otherwise resolves a stream URL through [YoutubeSource] (isolated NewPipe usage).
 *   4. Registers the play in the history table.
 *
 * All blocking work happens on [Dispatchers.IO]; every failure is swallowed so the UI never crashes.
 */
class PlaybackController(
    private val trackRepository: TrackRepository,
    private val downloads: DownloadRepository,
    private val history: HistoryRepository,
    private val youtube: YoutubeSource
) {

    /**
     * Plays [tracks] starting at [startIndex].
     * Returns true if playback was started (or resumed from a local file).
     */
    suspend fun play(tracks: List<TrackEntity>, startIndex: Int = 0): Boolean {
        if (tracks.isEmpty()) return false
        val safeIndex = startIndex.coerceIn(0, tracks.size - 1)

        // Make sure everything we are about to play exists locally (liked/playlists/history
        // rows already do; search results do not).
        tracks.forEach { track -> runCatching { trackRepository.saveTrack(track) } }

        val prepared = tracks.map { prepare(it) }
        val playableIndex = prepared.indexOfFirst { it }
        val indexToPlay = if (prepared[safeIndex]) safeIndex else if (playableIndex >= 0) playableIndex else -1

        if (indexToPlay < 0) {
            // Nothing resolvable; still surface the queue so the UI shows what was requested.
            PlayerHolder.setQueue(tracks, safeIndex)
            return false
        }

        PlayerHolder.setQueue(tracks, indexToPlay)
        runCatching { history.addPlay(tracks[indexToPlay].videoId) }
        // Make sure the background service (and its media notification) is running.
        AppContainer.startPlaybackService()
        return true
    }

    /** Play a single track (replaces the queue). */
    suspend fun play(track: TrackEntity): Boolean = play(listOf(track), 0)

    /**
     * Ensures a playable source exists for [track]: local download first, then a resolved stream.
     * Returns true when a source is available.
     */
    private suspend fun prepare(track: TrackEntity): Boolean {
        // 1. Local download wins.
        val download = runCatching { downloads.getDownload(track.videoId) }.getOrNull()
        if (download != null && download.state == "DONE" && download.filePath.isNotEmpty()) {
            DownloadPaths.set(track.videoId, download.filePath)
            return true
        }

        // 2. Already resolved in this process?
        if (PlayerHolder.streamFor(track.videoId) != null) return true

        // 3. Resolve over the network.
        return try {
            val stream = withContext(Dispatchers.IO) {
                youtube.resolveAudioStream(track.videoId)
            }
            if (stream != null) {
                PlayerHolder.setStreamSource(track.videoId, stream)
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            false
        }
    }

    /** Toggles the liked flag for [track] and returns the new value. */
    suspend fun toggleLike(track: TrackEntity): Boolean {
        val newValue = !track.liked
        runCatching {
            trackRepository.saveTrack(track)
            trackRepository.setLiked(track.videoId, newValue)
        }
        return newValue
    }

    /** Removes a downloaded file's path from the in-memory cache (used when a download is deleted). */
    fun forgetDownload(videoId: String) {
        DownloadPaths.remove(videoId)
    }
}
