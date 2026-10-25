package com.example.spoyt.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.data.remote.ResolvedStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the ExoPlayer instance and the playback state shared across the app.
 *
 * Responsibilities:
 *   - Own a single ExoPlayer (single-activity app, background playback).
 *   - Expose current track, queue, index, and play/pause state as StateFlows.
 *   - Provide queue operations (play a track, play a list, next, previous).
 *
 * Stream URLs are resolved lazily: when a track becomes current and no source is known yet,
 * [onSourceMissing] is invoked so the controller can resolve it and call [reloadCurrent].
 * That keeps "next" instant instead of pre-resolving the whole queue over the network.
 *
 * Initialized once from [MusicPlaybackService] (which owns the Android Context).
 */
object PlayerHolder {

    /** Browser-like User-Agent; YouTube's CDN rejects some non-browser clients. */
    const val HTTP_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7 Build/TD1A.220804.031; wv) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.6099.144 " +
            "Mobile Safari/537.36"

    private var exoPlayer: ExoPlayer? = null

    /** videoId -> resolved stream, used to build the playable media item. */
    private val streamSources = mutableMapOf<String, ResolvedStream>()

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    private val _queue = MutableStateFlow<List<TrackEntity>>(emptyList())
    private val _currentIndex = MutableStateFlow(0)
    private val _isPlaying = MutableStateFlow(false)
    private val _isBuffering = MutableStateFlow(false)

    /** Read-only exposures for the UI layer. */
    val currentTrackState: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()
    val queueState: StateFlow<List<TrackEntity>> = _queue.asStateFlow()
    val currentIndexState: StateFlow<Int> = _currentIndex.asStateFlow()
    val isPlayingState: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val isBufferingState: StateFlow<Boolean> = _isBuffering.asStateFlow()

    /** Set by [PlaybackController]: the current track has no playable source yet. */
    var onSourceMissing: ((TrackEntity) -> Unit)? = null

    /** Set by [PlaybackController]: playback failed (expired URL / CDN error), retry once. */
    var onPlaybackFailed: ((TrackEntity) -> Unit)? = null

    /** videoIds with an in-flight resolution request, so we never resolve the same track twice. */
    private val resolving = mutableSetOf<String>()

    /** videoIds that already burned their single automatic retry. */
    private val retried = mutableSetOf<String>()

    /** Current playback position in milliseconds (read synchronously for the seek bar). */
    val positionMs: Long
        get() = exoPlayer?.currentPosition ?: 0L

    /** Total duration in milliseconds, or 0 when unknown. */
    val durationMs: Long
        get() = exoPlayer?.duration?.takeIf { it > 0 } ?: 0L

    fun init(context: Context) {
        if (exoPlayer == null) {
            // A browser-like HTTP stack: YouTube's CDN answers 403 to unknown clients and
            // redirects between http/https hosts, both of which ExoPlayer's defaults handle badly.
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(HTTP_USER_AGENT)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(30_000)
                .setAllowCrossProtocolRedirects(true)

            val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            val p = ExoPlayer.Builder(context, mediaSourceFactory).build()

            // Music audio focus: pause when another app takes audio, duck on interruptions.
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )

            p.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _isBuffering.value = playbackState == Player.STATE_BUFFERING
                    when (playbackState) {
                        Player.STATE_READY -> {
                            // The source works: clear the retry bookkeeping for this track.
                            _currentTrack.value?.let { retried.remove(it.videoId) }
                        }

                        Player.STATE_ENDED -> playNext()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _isPlaying.value = false
                    _isBuffering.value = false
                    val track = _currentTrack.value ?: return
                    // Expired googlevideo URLs and transient CDN errors are worth one retry:
                    // the controller re-resolves the stream and calls reloadCurrent().
                    val canRetry = onPlaybackFailed != null && retried.add(track.videoId)
                    if (canRetry) {
                        invalidateStream(track.videoId)
                        onPlaybackFailed?.invoke(track)
                    } else {
                        PlaybackEvents.post("Bu şarkı şu anda oynatılamıyor.")
                    }
                }
            })
            exoPlayer = p
        }
    }

    val isInitialized: Boolean
        get() = exoPlayer != null

    /**
     * The live [ExoPlayer] instance, exposed so [MusicPlaybackService] can wrap it in a
     * [androidx.media3.session.MediaSession].
     */
    val engine: ExoPlayer
        get() = exoPlayer ?: error("PlayerHolder not initialized. Call init(Context) first.")

    // ---------------------------------------------------------------- sources

    /** Register a resolved stream for a track so it can be played. */
    fun setStreamSource(videoId: String, stream: ResolvedStream) {
        streamSources[videoId] = stream
    }

    /** Look up a still-valid resolved stream for [videoId], if any. */
    fun streamFor(videoId: String): ResolvedStream? {
        val stream = streamSources[videoId] ?: return null
        if (stream.isExpired()) {
            streamSources.remove(videoId)
            return null
        }
        return stream
    }

    /** Drops a cached stream so the next attempt resolves a fresh URL. */
    fun invalidateStream(videoId: String) {
        streamSources.remove(videoId)
    }

    /** Called by the controller when a resolution attempt finished (success or failure). */
    fun onResolutionFinished(videoId: String) {
        resolving.remove(videoId)
        // A failed resolution should not block a later manual retry.
        if (resolveUri(videoId) == null) retried.remove(videoId)
    }

    val currentHasSource: Boolean
        get() = _currentTrack.value?.let { resolveUri(it.videoId) != null } ?: false

    // ---------------------------------------------------------------- queue

    /** Set the queue WITHOUT starting playback (the controller resolves, then calls [start]). */
    fun setQueueSilently(tracks: List<TrackEntity>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.size - 1)
        _queue.value = tracks.toList()
        _currentIndex.value = safeIndex
        _currentTrack.value = tracks[safeIndex]
    }

    /** Set the queue and start playing from the given index. */
    fun setQueue(tracks: List<TrackEntity>, startIndex: Int = 0) {
        setQueueSilently(tracks, startIndex)
        start()
    }

    /** Append tracks to the queue. */
    fun addToQueue(tracks: List<TrackEntity>) {
        _queue.value = _queue.value + tracks
    }

    /** Play a single track (replaces the queue with just this track). */
    fun play(track: TrackEntity) {
        setQueue(listOf(track), 0)
    }

    /** Play the track at [index] in the current queue. */
    fun playIndex(index: Int) {
        val q = _queue.value
        if (q.isEmpty() || index !in q.indices) return
        _currentIndex.value = index
        _currentTrack.value = q[index]
        start()
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        val next = _currentIndex.value + 1
        if (next > q.lastIndex) {
            // End of queue: stop instead of silently restarting the last track.
            exoPlayer?.let {
                it.pause()
                it.seekTo(0)
            }
            _isPlaying.value = false
            return
        }
        playIndex(next)
    }

    fun playPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return
        // Standard behaviour: restart the song unless we are already at the beginning.
        val p = exoPlayer
        if (p != null && p.currentPosition > 3_000) {
            p.seekTo(0)
            return
        }
        playIndex((_currentIndex.value - 1).coerceAtLeast(0))
    }

    fun playPause() {
        val p = exoPlayer ?: return
        if (p.isPlaying) {
            p.pause()
        } else {
            if (p.playbackState == Player.STATE_IDLE) start() else p.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    /** Start (or restart) the currently selected track using an already-known source. */
    fun start() {
        val track = _currentTrack.value ?: return
        if (resolveUri(track.videoId) == null) {
            // Not resolved yet: ask the controller to resolve it exactly once, the controller
            // then calls reloadCurrent() when it has a URL (or posts an error message).
            if (resolving.add(track.videoId)) onSourceMissing?.invoke(track)
            _isPlaying.value = false
            return
        }
        attachAndPlay(track)
    }

    /** Re-attach the current track to the player (used after a source was resolved/refreshed). */
    fun reloadCurrent() {
        val track = _currentTrack.value ?: return
        if (resolveUri(track.videoId) == null) {
            _isPlaying.value = false
            _isBuffering.value = false
            return
        }
        attachAndPlay(track)
    }

    private fun attachAndPlay(track: TrackEntity) {
        val uri = resolveUri(track.videoId) ?: return
        // Defensive: if the engine is not available yet, say so instead of throwing.
        val p = exoPlayer ?: run {
            _isBuffering.value = false
            _isPlaying.value = false
            PlaybackEvents.post("Oynatıcı hazır değil. Tekrar deneyin.")
            return
        }
        val stream = streamSources[track.videoId]
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(stream?.mimeType)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(track.thumbnailUrl?.let { Uri.parse(it) })
                    .build()
            )
            .build()

        p.setMediaItem(mediaItem, 0)
        p.prepare()
        p.play()
        _isBuffering.value = true
    }

    /** Prefer the local download file if available; otherwise use the resolved stream URL. */
    private fun resolveUri(videoId: String): Uri? {
        val downloadPath = DownloadPaths.pathFor(videoId)
        if (downloadPath.isNotEmpty()) {
            val file = java.io.File(downloadPath)
            if (file.exists() && file.length() > 0) return Uri.fromFile(file)
            DownloadPaths.remove(videoId)
        }
        val stream = streamFor(videoId) ?: return null
        return Uri.parse(stream.url)
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        streamSources.clear()
        resolving.clear()
        retried.clear()
    }
}

/**
 * In-memory record of downloaded file paths keyed by videoId.
 * Populated when a download finishes and re-hydrated from Room on every playback attempt.
 */
object DownloadPaths {
    private val paths = mutableMapOf<String, String>()

    fun pathFor(videoId: String): String = paths[videoId] ?: ""

    fun set(videoId: String, path: String) {
        paths[videoId] = path
    }

    fun remove(videoId: String) {
        paths.remove(videoId)
    }
}
