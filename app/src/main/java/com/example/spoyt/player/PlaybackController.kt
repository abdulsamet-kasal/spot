package com.example.spoyt.player

import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.data.local.repositories.DownloadRepository
import com.example.spoyt.data.local.repositories.HistoryRepository
import com.example.spoyt.data.local.repositories.TrackRepository
import com.example.spoyt.data.prefs.AppPrefs
import com.example.spoyt.data.remote.YoutubeSource
import com.example.spoyt.di.AppContainer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
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

    /** Background job that warms up the stream URLs of the upcoming tracks. */
    private var prefetchJob: Job? = null

    /** videoIds currently being prefetched, so two prefetches never resolve the same track. */
    private val prefetching: MutableSet<String> =
        java.util.Collections.synchronizedSet(mutableSetOf())

    private val _recommendations = MutableStateFlow<List<TrackEntity>>(emptyList())

    /** Latest "for you" result; empty until the first [refreshRecommendations] completes. */
    val recommendations: StateFlow<List<TrackEntity>> = _recommendations.asStateFlow()

    private var recommendationsAt = 0L

    init {
        // Lazily resolve whatever track became current (next/previous/tap in queue).
        PlayerHolder.onSourceMissing = { track ->
            scope.launch { loadAndPlay(track, forceRefresh = false) }
        }
        // Stream failed (expired URL, CDN stall, screen-off network throttling): wait the stall
        // out, then re-resolve. Growing delays because a throttled background network usually
        // recovers within seconds, while an immediate retry hits the same dead link.
        PlayerHolder.onPlaybackFailed = { track, attempt ->
            scope.launch {
                val waitMs = RETRY_BACKOFF_BASE_MS * (1L shl ((attempt - 1).coerceAtMost(3)))
                PlaybackLog.line("retry ${track.videoId} deneme=$attempt ${waitMs / 1000}s sonra")
                delay(waitMs)
                loadAndPlay(track, forceRefresh = true)
            }
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
                if (!trackRepository.trackExists(track.videoId)) {
                    trackRepository.saveTrack(track)
                } else if (track.artist.isNotBlank()) {
                    // Older rows were written while the artist was still lost in parsing.
                    trackRepository.fillArtist(track.videoId, track.artist)
                }
            }
        }

        // Show the requested queue immediately; the player asks us for the source if missing.
        PlayerHolder.setQueueSilently(tracks, safeIndex)
        // Silence the outgoing track right away: while the new URL resolves the old song must
        // not keep playing (that used to look like a 4-5 second delay on every track change).
        PlayerHolder.beginLoading()

        // Bring the media service up first so the notification/session exists when audio starts.
        AppContainer.startPlaybackService()

        val started = prepare(tracks[safeIndex], forceRefresh = false)
        if (started) {
            PlayerHolder.start()
            PlayerHolder.onResolutionFinished(tracks[safeIndex].videoId)
            recordStart(tracks[safeIndex])
            prefetchUpcoming(safeIndex)
            return true
        }

        // The selected track is not playable: try a couple of neighbours before giving up.
        val lastCandidate = minOf(tracks.lastIndex, safeIndex + MAX_FALLBACK_TRACKS)
        for (index in (safeIndex + 1)..lastCandidate) {
            if (prepare(tracks[index], forceRefresh = false)) {
                PlayerHolder.playIndex(index)
                PlayerHolder.onResolutionFinished(tracks[index].videoId)
                recordStart(tracks[index])
                prefetchUpcoming(index)
                return true
            }
        }

        PlayerHolder.clearLoading()
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
        var ok = prepare(track, forceRefresh)
        // When playback stalls in the background the URL lookup rides on the same dead network,
        // so one failed lookup used to end the song. Retry it a couple of times while the user
        // is still on this track.
        var lookupTry = 1
        while (!ok && lookupTry < STREAM_LOOKUP_ATTEMPTS) {
            if (PlayerHolder.currentTrackState.value?.videoId != track.videoId) return
            delay(STREAM_LOOKUP_BACKOFF_MS * lookupTry)
            ensurePlayer()
            ok = prepare(track, forceRefresh = true)
            lookupTry++
        }
        PlayerHolder.onResolutionFinished(track.videoId)

        if (!ok) {
            if (PlayerHolder.currentTrackState.value?.videoId == track.videoId) {
                PlayerHolder.clearLoading()
            }
            PlaybackLog.line("unplayable ${track.videoId} (${track.title}) - URL cozulemedi")
            PlaybackEvents.post("Bu şarkı şu anda oynatılamıyor. Başka bir şarkı deneyin.")
            return
        }

        // The user may have skipped while we were resolving; only re-attach if still current.
        val current = PlayerHolder.currentTrackState.value
        if (current?.videoId == track.videoId) {
            PlayerHolder.reloadCurrent()
            recordStart(track)
            prefetchUpcoming(PlayerHolder.currentIndexState.value)
        }
    }

    /**
     * Warms up the URL of the next couple of tracks while the current one plays.
     *
     * Stream resolution is a network round-trip (NewPipe parses the watch page), so doing it
     * only when the user hits "next" made every skip feel slow. Resolving ahead in the
     * background turns skips into an instant swap; failures are ignored because the normal
     * on-demand path still runs later.
     */
    private fun prefetchUpcoming(fromIndex: Int) {
        val queue = PlayerHolder.queueState.value
        if (queue.isEmpty()) return
        prefetchJob?.cancel()
        prefetchJob = scope.launch {
            val lastIndex = minOf(queue.lastIndex, fromIndex + PREFETCH_TRACKS)
            for (index in (fromIndex + 1)..lastIndex) {
                if (!isActive) return@launch
                val track = queue.getOrNull(index) ?: continue
                if (PlayerHolder.streamFor(track.videoId) != null) continue
                if (!prefetching.add(track.videoId)) continue
                try {
                    prepare(track, forceRefresh = false)
                } finally {
                    prefetching.remove(track.videoId)
                }
            }
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

    /**
     * Starts a "radio" seeded by [seed]: the seed track plus related tracks resolved from
     * YouTube Music. Returns false (with no queue change) when nothing related could be resolved.
     */
    suspend fun startRadio(seed: TrackEntity): Boolean {
        ensurePlayer()
        val related = withContext(Dispatchers.IO) {
            runCatching { youtube.radio(seed.videoId, seed.artist) }.getOrDefault(emptyList())
        }
        if (related.isEmpty()) {
            PlaybackEvents.post("Radyo için benzer şarkı bulunamadı. Birazdan tekrar deneyin.")
            return false
        }

        val seedEntity = runCatching { trackRepository.getTrack(seed.videoId) }.getOrNull() ?: seed
        val radioTracks = buildList {
            add(seedEntity)
            related.forEach { result ->
                if (result.videoId != seedEntity.videoId) {
                    add(
                        TrackEntity(
                            videoId = result.videoId,
                            title = result.title,
                            artist = result.artist,
                            durationSeconds = result.durationSeconds,
                            thumbnailUrl = result.thumbnailUrl
                        )
                    )
                }
            }
        }

        // Persist the radio tracks so likes/downloads can reference them later (play() does
        // this too, but only for the tracks it is given).
        val ok = play(radioTracks, 0)
        if (ok) {
            PlaybackEvents.post("Radyo başlatıldı: ${seedEntity.title}")
        }
        return ok
    }

    /**
     * Repairs library rows that were saved while the artist was still being lost: asks NewPipe
     * for the uploader of a few blank rows per app start. Runs on [Dispatchers.IO] and never
     * touches the player, so it cannot interrupt playback.
     */
    fun repairMissingArtists() {
        scope.launch {
            val stale = runCatching { trackRepository.getTracksMissingArtist(ARTIST_REPAIR_BATCH) }
                .getOrDefault(emptyList())
            stale.forEach { track ->
                val meta = runCatching { youtube.trackFor(track.videoId) }.getOrNull()
                if (meta != null && meta.artist.isNotBlank()) {
                    runCatching { trackRepository.fillArtist(track.videoId, meta.artist) }
                }
                delay(400)
            }
        }
    }

    /**
     * Suggestions built from what the user actually listens to: the most-played tracks become
     * radio seeds and their similar tracks are collected. Cached in memory because each
     * refresh costs one radio lookup per seed.
     */
    fun refreshRecommendations() {
        val now = System.currentTimeMillis()
        val reuse = _recommendations.value.isNotEmpty() && now - recommendationsAt < RECOMMENDATION_TTL_MS
        if (reuse) return
        scope.launch {
            val seeds = runCatching { history.getMostPlayedTracks(RECOMMENDATION_SEEDS).first() }
                .getOrDefault(emptyList())
            if (seeds.isEmpty()) return@launch
            val alreadyHeard = seeds.mapTo(HashSet()) { it.videoId }
            val collected = LinkedHashMap<String, TrackEntity>()
            seeds.forEach { seed ->
                val similar = runCatching { youtube.radio(seed.videoId, seed.artist) }
                    .getOrDefault(emptyList())
                similar.forEach { result ->
                    if (result.videoId !in alreadyHeard) {
                        collected.putIfAbsent(
                            result.videoId,
                            TrackEntity(
                                videoId = result.videoId,
                                title = result.title,
                                artist = result.artist,
                                durationSeconds = result.durationSeconds,
                                thumbnailUrl = result.thumbnailUrl
                            )
                        )
                    }
                }
                delay(300)
            }
            if (collected.isNotEmpty()) {
                _recommendations.value = collected.values.take(RECOMMENDATION_LIMIT)
                recommendationsAt = now
            }
        }
    }

    /** Fire-and-forget wrapper used by the service's system-restart path ([MusicPlaybackService]). */
    fun revivePlaybackAsync() {
        scope.launch { reviveImpl(REVIVE_SNAPSHOT_MAX_AGE_MS, guardCurrentTrack = false) }
    }

    /**
     * Continues a session the ROM cut off when the user reopens the app. Vivo-style power
     * managers force-stop the whole process, so no service restart ever arrives; the only
     * recovery left is the next time the user opens the app. Guarded: if the user already
     * picked a track themselves, their choice wins.
     */
    fun reviveAfterAppOpen() {
        scope.launch { reviveImpl(REVIVE_ON_OPEN_MAX_AGE_MS, guardCurrentTrack = true) }
    }

    /**
     * Self-restart salvage: the service tried to restart itself after the ROM stopped it
     * (the process was clearly still alive — a force-stop never reaches this point).
     */
    fun reviveAfterServiceRescue() {
        scope.launch { reviveImpl(REVIVE_RESCUE_MAX_AGE_MS, guardCurrentTrack = false) }
    }

    /**
     * Auto-resume after the ROM stopped the media service mid-song (vivo-style power
     * management: `service destroyed (playing=true)` with no player error at all). Called on
     * a system restart, an app open or a self-restarted service, so music comes back by
     * itself instead of waiting for the user to press play again.
     *
     * Budgeted through [AppPrefs.claimRevive]: a power manager that keeps fighting the
     * service must not turn the kill/restart ping-pong into an endless loop that drains
     * the battery (and the user's patience).
     */
    private suspend fun reviveImpl(maxAgeMs: Long, guardCurrentTrack: Boolean): Boolean {
        val snapshot = runCatching { AppPrefs.loadPlaybackSnapshot() }.getOrNull() ?: return false
        if (!snapshot.playWhenReady || snapshot.videoIds.isEmpty()) return false
        // Resuming a session from yesterday is more surprising than helpful: only a fresh
        // stop is worth an automatic revival.
        val stale = System.currentTimeMillis() - snapshot.savedAt > maxAgeMs
        if (stale || !AppPrefs.claimRevive(REVIVE_LIMIT, REVIVE_WINDOW_MS)) {
            PlaybackLog.line("revive skipped (stale=$stale veya butce tukendi)")
            return false
        }
        val tracks = withContext(Dispatchers.IO) {
            runCatching { snapshot.videoIds.mapNotNull { trackRepository.getTrack(it) } }
                .getOrDefault(emptyList())
        }
        if (tracks.isEmpty()) {
            PlaybackLog.line("revive skipped: kuyruk bos veya temizlenmis")
            return false
        }
        // If the user's own choice is already on the player, it must win over the snapshot.
        val expected = snapshot.videoIds.getOrNull(snapshot.index)
        val current = PlayerHolder.currentTrackState.value
        if (guardCurrentTrack && current != null && current.videoId != expected) {
            PlaybackLog.line("revive skipped: kullanicinin secimi oncelikli (${current.videoId})")
            return false
        }
        PlaybackLog.line(
            "revive: ${tracks.size} parca index=${snapshot.index} pos=${snapshot.positionMs / 1000}s"
        )
        val ok = play(tracks, snapshot.index)
        if (ok) {
            if (snapshot.positionMs > 0) PlayerHolder.seekTo(snapshot.positionMs)
            PlaybackEvents.post("Arka plan kesintisinden devam ediliyor.")
        }
        return ok
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

    /**
     * Like toggle coming from the media notification or the system player card. Fire-and-forget:
     * the caller is a service intent and must return immediately.
     */
    fun toggleLikeAsync(track: TrackEntity) {
        scope.launch {
            val liked = runCatching { toggleLike(track) }.getOrElse {
                PlaybackLog.line("like failed: ${it.javaClass.simpleName}")
                return@launch
            }
            runCatching { PlayerHolder.applyLiked(liked) }
            PlaybackEvents.post(
                if (liked) "Beğenilenlere eklendi." else "Beğenilenlerden çıkarıldı."
            )
        }
    }

    /** Removes a downloaded file's path from the in-memory cache (used when a download is deleted). */
    fun forgetDownload(videoId: String) {
        DownloadPaths.remove(videoId)
    }

    companion object {
        /** How many following tracks to try when the selected one cannot be resolved. */
        private const val MAX_FALLBACK_TRACKS = 3

        /**
         * Auto-revive budget: how many automatic resumes a rolling window allows. A vivo-style
         * power manager that stops the service every minute must not turn the revive path
         * into a battery-draining restart loop.
         */
        private const val REVIVE_LIMIT = 4
        private const val REVIVE_WINDOW_MS = 30L * 60 * 1000

        /** A snapshot from a previous listen is not auto-resumed; only a fresh stop is. */
        private const val REVIVE_SNAPSHOT_MAX_AGE_MS = 8L * 60 * 60 * 1000

        /** Opening the app resumes only a very recent cut-off session, not an old one. */
        private const val REVIVE_ON_OPEN_MAX_AGE_MS = 10L * 60 * 1000

        /** The service's own rescue restart only makes sense minutes after the stop. */
        private const val REVIVE_RESCUE_MAX_AGE_MS = 30L * 60 * 1000

        /** How many upcoming tracks to resolve in the background while one plays. */
        private const val PREFETCH_TRACKS = 2

        /** How many artist-less library rows to repair per app start. */
        private const val ARTIST_REPAIR_BATCH = 12

        /** How many listened tracks feed the "for you" radio seeds. */
        private const val RECOMMENDATION_SEEDS = 3

        /** How many suggested tracks the home shelf shows. */
        private const val RECOMMENDATION_LIMIT = 12

        /** Refreshing costs network calls per seed, so results are reused for a few hours. */
        private const val RECOMMENDATION_TTL_MS = 3L * 60 * 60 * 1000

        /** Growing wait between automatic playback retries (attempt-1 doubles it, capped). */
        private const val RETRY_BACKOFF_BASE_MS = 2_000L

        /** How often a stream URL lookup may fail before the track counts as unplayable. */
        private const val STREAM_LOOKUP_ATTEMPTS = 3

        /** Base wait between URL lookups, multiplied by the attempt number. */
        private const val STREAM_LOOKUP_BACKOFF_MS = 2_000L
    }
}
