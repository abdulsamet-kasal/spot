package com.example.spoyt.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
 * Initialized once from [MusicPlaybackService] (which owns the Android Context).
 */
object PlayerHolder {

    private var exoPlayer: ExoPlayer? = null

    /** videoId -> resolved stream, used to build the playable media item. */
    private val streamSources = mutableMapOf<String, ResolvedStream>()

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    private val _queue = MutableStateFlow<List<TrackEntity>>(emptyList())
    private val _currentIndex = MutableStateFlow(0)
    private val _isPlaying = MutableStateFlow(false)

    /** Read-only exposures for the UI layer. */
    val currentTrackState: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()
    val queueState: StateFlow<List<TrackEntity>> = _queue.asStateFlow()
    val currentIndexState: StateFlow<Int> = _currentIndex.asStateFlow()
    val isPlayingState: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** Current playback position in milliseconds (read synchronously for the seek bar). */
    val positionMs: Long
        get() = exoPlayer?.currentPosition ?: 0L

    /** Total duration in milliseconds, or 0 when unknown. */
    val durationMs: Long
        get() = exoPlayer?.duration?.takeIf { it > 0 } ?: 0L

    fun init(context: Context) {
        if (exoPlayer == null) {
            val p = ExoPlayer.Builder(context).build()

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
                    if (playbackState == Player.STATE_ENDED) {
                        playNext()
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

    private val player: ExoPlayer
        get() = exoPlayer ?: error("PlayerHolder not initialized. Call init(Context) first.")

    /** Register a resolved stream for a track so it can be played. */
    fun setStreamSource(videoId: String, stream: ResolvedStream) {
        streamSources[videoId] = stream
    }

    /** Look up the resolved stream registered for [videoId], if any. */
    fun streamFor(videoId: String): ResolvedStream? = streamSources[videoId]

    /** Set the queue and start playing from the given index. */
    fun setQueue(tracks: List<TrackEntity>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.size - 1)
        _queue.value = tracks.toList()
        _currentIndex.value = safeIndex
        playTrack(tracks[safeIndex])
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
        playTrack(q[index])
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        val next = (_currentIndex.value + 1).coerceAtMost(q.size - 1)
        if (next == _currentIndex.value && q.size == 1) return // single track, nothing to do
        playIndex(next)
    }

    fun playPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return
        val prev = (_currentIndex.value - 1).coerceAtLeast(0)
        playIndex(prev)
    }

    fun playPause() {
        val p = exoPlayer ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    private fun playTrack(track: TrackEntity) {
        _currentTrack.value = track
        val uri = resolveUri(track)
        if (uri == null) {
            // No playable source available yet (stream not resolved, no download).
            return
        }
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(streamSources[track.videoId]?.mimeType)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .build()
            )
            .build()
        val p = player
        p.setMediaItem(mediaItem, 0)
        p.prepare()
        p.play()
        _isPlaying.value = true
    }

    /** Prefer the local download file if available; otherwise use the resolved stream URL. */
    private fun resolveUri(track: TrackEntity): Uri? {
        val downloadPath = DownloadPaths.pathFor(track.videoId)
        if (downloadPath.isNotEmpty()) {
            return Uri.fromFile(java.io.File(downloadPath))
        }
        val stream = streamSources[track.videoId] ?: return null
        return Uri.parse(stream.url)
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        streamSources.clear()
    }
}

/**
 * In-memory record of downloaded file paths keyed by videoId.
 * Populated by the download worker when a download completes.
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
