package com.example.spoyt.data.remote

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import java.util.concurrent.atomic.AtomicReference

/**
 * Search result returned by [YoutubeSource.search].
 */
data class SearchResult(
    val videoId: String,
    val title: String,
    val artist: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?
)

/**
 * Resolved stream information returned by [YoutubeSource.resolveAudioStream].
 *
 * YouTube CDN URLs carry an expiry (`expire=...`), usually ~6 hours, so [resolvedAtMs] is
 * stored to allow [isExpired] checks: replaying a stale URL just yields a 403.
 */
data class ResolvedStream(
    val url: String,
    val mimeType: String?,
    val fileExtension: String?,
    val resolvedAtMs: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        now - resolvedAtMs > MAX_AGE_MS

    companion object {
        /** Stay well inside YouTube's ~6h URL lifetime. */
        const val MAX_AGE_MS = 90L * 60L * 1000L
    }
}

/**
 * The single place in the app that talks to NewPipe Extractor.
 *
 * Everything here is blocking; callers must use `Dispatchers.IO`. Every public method is
 * wrapped in try/catch and returns an empty result on failure so the UI never crashes.
 *
 * Notes on YouTube Music: NewPipe Extractor has no separate "YouTube Music" service. Its
 * YouTube service switches to `YoutubeMusicSearchExtractor` when the search content filter
 * starts with "music_", which is why [MUSIC_SONGS_FILTER] is used below.
 */
class YoutubeSource {

    private val serviceRef = AtomicReference<StreamingService?>(null)

    private fun service(): StreamingService {
        serviceRef.get()?.let { return it }
        val resolved = NewPipe.getService(YOUTUBE_SERVICE_NAME)
        serviceRef.set(resolved)
        return resolved
    }

    /**
     * Searches YouTube Music for songs matching [query].
     * Returns an empty list on any network or extraction error (never throws).
     */
    fun search(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        return try {
            val extractor = service().getSearchExtractor(query, listOf(MUSIC_SONGS_FILTER), "")
            extractor.fetchPage()
            extractor.initialPage.items.mapNotNull { toSearchResult(it) }
        } catch (e: Exception) {
            // Network/extraction failures must not crash the app.
            emptyList()
        }
    }

    /**
     * Resolves the metadata (title/artist/duration/thumbnail) of a single video id, e.g. one
     * pasted as a YouTube link into the search field. Returns null on any failure.
     */
    fun trackFor(videoId: String): SearchResult? = try {
        val extractor = service().getStreamExtractor(urlFor(videoId))
        extractor.fetchPage()
        SearchResult(
            videoId = videoId,
            title = extractor.name?.trim().orEmpty(),
            artist = cleanArtistName(extractor.uploaderName.orEmpty()),
            durationSeconds = extractor.length.coerceAtLeast(0),
            thumbnailUrl = extractor.thumbnails.firstOrNull()?.url
        ).takeIf { it.title.isNotEmpty() }
    } catch (e: Exception) {
        null
    }

    /**
     * Related tracks ("radio") for a seed video, used by the "Radyo başlat" action.
     * Related items include videos of every kind; the caller decides what to queue.
     */
    fun related(videoId: String): List<SearchResult> = try {
        val extractor = service().getStreamExtractor(urlFor(videoId))
        extractor.fetchPage()
        // getRelatedStreams() is @Nullable and yields a collector; .items is List<StreamInfoItem>.
        val items = extractor.relatedStreams?.items.orEmpty()
        items
            .mapNotNull { toSearchResult(it) }
            .filter { it.durationSeconds > 0 }
            .take(15)
    } catch (e: Exception) {
        // Related items are a bonus; an empty list means "no radio".
        emptyList()
    }

    /**
     * Resolves the best audio-only stream URL for [videoId].
     * Returns null if no audio stream can be extracted.
     * Must be called on [kotlinx.coroutines.Dispatchers.IO].
     */
    fun resolveAudioStream(videoId: String): ResolvedStream? {
        return try {
            val extractor = service().getStreamExtractor(urlFor(videoId))
            extractor.fetchPage()

            val playable = extractor.audioStreams
                .filter { stream -> isPlayable(stream) }

            // Prefer a well-supported container (m4a/AAC) at the highest bitrate; fall back
            // to any playable audio stream (Opus/WebM) when only those exist.
            val bestM4a = playable
                .filter { it.format?.mimeType?.contains("mp4", ignoreCase = true) == true }
                .maxByOrNull { it.averageBitrate }
            val bestAny = playable.maxByOrNull { it.averageBitrate }
            val best = bestM4a ?: bestAny

            best?.let { stream ->
                val format = stream.format
                ResolvedStream(
                    url = stream.content,
                    mimeType = format?.mimeType,
                    fileExtension = format?.suffix,
                    resolvedAtMs = System.currentTimeMillis(),
                    durationSeconds = extractor.length.takeIf { it > 0 } ?: 0
                )
            }
        } catch (e: Exception) {
            // Extraction failures are expected when YouTube changes something; report "cannot
            // play" instead of crashing.
            null
        }
    }

    private fun isPlayable(stream: AudioStream): Boolean {
        val content = stream.content
        val urlOk = content.startsWith("https://") || content.startsWith("http://")
        return urlOk && stream.averageBitrate > 0
    }

    private fun toSearchResult(item: org.schabi.newpipe.extractor.InfoItem): SearchResult? {
        return try {
            val videoId = YouTubeUtils.extractVideoId(item.url)
            if (videoId.isNullOrEmpty()) return null

            val title = item.name?.trim().orEmpty()
            if (title.isEmpty()) return null

            var artist = ""
            var duration = 0L
            if (item is StreamInfoItemExtractor) {
                artist = cleanArtistName(item.uploaderName.orEmpty())
                duration = item.duration.coerceAtLeast(0)
            }
            val thumbnail = item.thumbnails.firstOrNull()?.url

            SearchResult(
                videoId = videoId,
                title = title,
                artist = artist,
                durationSeconds = duration,
                thumbnailUrl = thumbnail
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Turns YouTube/YouTube Music channel names into something readable in the UI.
     * Delegates to [YouTubeUtils] so the logic stays unit-testable.
     */
    private fun cleanArtistName(uploaderName: String): String =
        YouTubeUtils.cleanArtistName(uploaderName)

    private fun urlFor(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    companion object {
        /** The canonical name of the YouTube service in NewPipe Extractor. */
        const val YOUTUBE_SERVICE_NAME = "YouTube"

        /**
         * Content filter that makes NewPipe search YouTube Music instead of plain YouTube.
         * Any filter starting with "music_" enables YoutubeMusicSearchExtractor.
         */
        private const val MUSIC_SONGS_FILTER = "music_songs"
    }
}
