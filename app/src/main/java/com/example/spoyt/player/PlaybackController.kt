package com.example.spoyt.player

import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.data.local.repositories.DownloadRepository
import com.example.spoyt.data.local.repositories.HistoryRepository
import com.example.spoyt.data.local.repositories.TrackRepository
import com.example.spoyt.data.remote.YoutubeSource
import com.example.spoyt.di.AppContainer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Coordinates "play this list of tracks":
 *   1. Persists the track (search results are ephemeral until played/liked).
 *   2. Prefers an already-downloaded local file over a network stream.
 *   3. Otherwise resolves a stream URL through [YoutubeSource] (isolated NewPipe usage).
 *   4. Registers the play in the history table.
 *
 * Resolution is lazy: only the track that is about to play is resolved, and later tracks are
 * resolved on demand through [PlayerHolder.onSourceMissing]. Resolving the whole queue up
 * front made the first play take many seconds on a 20-item list.
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
     * Background failures must never reach the default uncaught handler: that would kill the
     * whole process. Report them as a friendly message instead.
     */
    private val crashGuard = CoroutineExceptionHandler { _, t ->
        if (t !is kotlinx.coroutines.CancellationException) {
            PlaybackEvents.post("Beklenmeyen bir sorun oluştu. Lütfen tekrar deneyin.")
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + crashGuard)

    init {
        // Lazily resolve whatever track became current (next/previous/tap in queue).
        PlayerHolder.onSourceMissing = { track ->
            scope.launch { loadAndPlay(track, forceRefresh = false) }
        }
        // Expired URL or CDN failure: refresh the URL once and re-attach.
        PlayerHolder.onPlaybackFailed = { track ->
            scope.launch { loadAndPlay(track, forceRefresh = true) }
        }
    }

    /**
     * Plays [tracks] starting at [startIndex].
     * Returns true if playback was started (or resumed from a local file).
     */
    suspend fun play(tracks: List<TrackEntity>, startIndex: Int = 0): Boolean {
        if (tracks.isEmpty()) return false
        ensurePlayer()
        val safeIndex = startIndex.coerceIn(0, tracks.size - 1)

        // Make sure everything we are about to play exists locally (liked/playlists/history
        // rows already do; search results do not). Never overwrite an existing row: a search
        // result carries liked=false and would silently un-like a liked track.
        tracks.forEach { track ->
            runCatching {
                if (!trackRepository.trackExists(track.videoId)) trackRepository.saveTrack(track)
            }
        }

        // Show the requested queue immediately; the player asks us for the source if missing.
        PlayerHolder.setQueueSilently(tracks, safeIndex)

        // Bring the media service up first so the notification/session exists when audio starts.
        AppContainer.startPlaybackService()

        val started = prepare(tracks[safeIndex], forceRefresh = false)
        if (started) {
            PlayerHolder.start()
            PlayerHolder.onResolutionFinished(tracks[safeIndex].videoId)
            recordStart(tracks[safeIndex])
            return true
        }

        // The selected track is not playable: try a couple of neighbours before giving up.
        val lastCandidate = minOf(tracks.lastIndex, safeIndex + MAX_FALLBACK_TRACKS)
        for (index in (safeIndex + 1)..lastCandidate) {
            if (prepare(tracks[index], forceRefresh = false)) {
                PlayerHolder.playIndex(index)
                PlayerHolder.onResolutionFinished(tracks[index].videoId)
                recordStart(tracks[index])
                return true
            }
        }

        PlaybackEvents.post("Bu şarkı şu anda oynatılamıyor. Başka bir şarkı deneyin.")
        return false
    }

    /** Play a single track (replaces the queue). */
    suspend fun play(track: TrackEntity): Boolean = play(listOf(track), 0)

    /**
     * Resolves [track] (local file first) and plays it if it is still the current track.
     * Used for both the "no source yet" and "stream failed" paths.
     */
    private suspend fun loadAndPlay(track: TrackEntity, forceRefresh: Boolean) {
        ensurePlayer()
        val ok = prepare(track, forceRefresh)
        PlayerHolder.onResolutionFinished(track.videoId)

        if (!ok) {
            PlaybackEvents.post("Bu şarkı şu anda oynatılamıyor. Başka bir şarkı deneyin.")
            return
        }

        // The user may have skipped while we were resolving; only re-attach if still current.
        val current = PlayerHolder.currentTrackState.value
        if (current?.videoId == track.videoId) {
            PlayerHolder.reloadCurrent()
            recordStart(track)
        }
    }

    /**
     * Ensures a playable source exists for [track]: local download first, then a resolved stream.
     * Returns true when a source is available.
     */
    private suspend fun prepare(track: TrackEntity, forceRefresh: Boolean): Boolean {
        // 1. Local download wins.
        val download = runCatching { downloads.getDownload(track.videoId) }.getOrNull()
        if (download != null && download.state == "DONE" && download.filePath.isNotEmpty()) {
            val file = java.io.File(download.filePath)
            if (file.exists() && file.length() > 0) {
                DownloadPaths.set(track.videoId, download.filePath)
                return true
            }
            // The row says DONE but the file is gone (app data cleared): fall through to streaming.
        }

        // 2. Already resolved in this process and still inside its lifetime?
        if (!forceRefresh && PlayerHolder.streamFor(track.videoId) != null) return true
        if (forceRefresh) PlayerHolder.invalidateStream(track.videoId)

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

    /** Records the history entry and makes sure the background service is running. */
    private fun recordStart(track: TrackEntity) {
        scope.launch { runCatching { history.addPlay(track.videoId) } }
        AppContainer.startPlaybackService()
    }

    /**
     * The ExoPlayer normally comes from [com.example.spoyt.di.SpoytApplication], but the media
     * service releases it when it is destroyed. Re-create it on demand so playback never
     * silently no-ops because the engine is missing.
     */
    private fun ensurePlayer() {
        if (!PlayerHolder.isInitialized) {
            runCatching { PlayerHolder.init(AppContainer.context) }
        }
    }

    /** Toggles the liked flag for [track] and returns the new value. */
    suspend fun toggleLike(track: TrackEntity): Boolean {
        // Read the current value from Room instead of trusting the caller's (possibly stale)
        // copy: rapid taps used to flip the flag back to its old value.
        val fresh = runCatching { trackRepository.getTrack(track.videoId) }.getOrNull()
        val newValue = !(fresh?.liked ?: track.liked)
        runCatching {
            if (fresh == null) trackRepository.saveTrack(track.copy(liked = newValue))
            trackRepository.setLiked(track.videoId, newValue)
        }
        return newValue
    }

    /** Removes a downloaded file's path from the in-memory cache (used when a download is deleted). */
    fun forgetDownload(videoId: String) {
        DownloadPaths.remove(videoId)
    }

    companion object {
        /** How many following tracks to try when the selected one cannot be resolved. */
        private const val MAX_FALLBACK_TRACKS = 3
    }
}
