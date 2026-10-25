package com.example.spoyt.data.remote

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor

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
 */
data class ResolvedStream(
    val url: String,
    val mimeType: String?,
    val fileExtension: String?
)

/**
 * Encapsulates all NewPipe Extractor usage so the rest of the app is not coupled to the library.
 * If YouTube changes break extraction, only this class needs updating.
 *
 * Responsibilities:
 *   - Search YouTube (music-filtered) for tracks.
 *   - Resolve the best audio-only stream URL for a given video ID.
 *
 * All methods perform blocking I/O and must be called on [kotlinx.coroutines.Dispatchers.IO].
 */
class YoutubeSource {

    private val service: StreamingService by lazy {
        NewPipe.getService(YOUTUBE_SERVICE_NAME)
    }

    private val downloader: Downloader = NewPipe.getDownloader()

    /**
     * Searches YouTube Music for songs matching [query].
     * Returns an empty list on any network or extraction error (never throws).
     */
    fun search(query: String): List<SearchResult> {
        return try {
            // "music_songs" is the only way to reach YouTube Music through this service:
            // YoutubeService switches to YoutubeMusicSearchExtractor when the filter
            // starts with "music_". Other valid values: music_videos, music_albums,
            // music_playlists, videos, channels, playlists, ALL.
            val extractor = service.getSearchExtractor(query, listOf(MUSIC_SONGS_FILTER), "")
            extractor.fetchPage()
            val page = extractor.getInitialPage()
            page.items.mapNotNull { item ->
                toSearchResult(item)
            }
        } catch (e: Exception) {
            // Network/extraction failures must not crash the app.
            emptyList()
        }
    }

    /**
     * Resolves the best audio-only stream URL for [videoId].
     * Returns null if no audio stream can be extracted.
     * Must be called on [kotlinx.coroutines.Dispatchers.IO].
     */
    fun resolveAudioStream(videoId: String): ResolvedStream? {
        return try {
            val streamUrl = "https://www.youtube.com/watch?v=$videoId"
            val extractor = service.getStreamExtractor(streamUrl)
            extractor.fetchPage()
            val audioStreams = extractor.getAudioStreams()

            val best = audioStreams.maxByOrNull { it.averageBitrate }
                ?: audioStreams.firstOrNull()

            best?.let { stream ->
                val format: MediaFormat? = stream.format
                ResolvedStream(
                    url = stream.content,
                    mimeType = format?.mimeType,
                    fileExtension = format?.suffix
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun toSearchResult(item: org.schabi.newpipe.extractor.InfoItem): SearchResult? {
        return try {
            val videoId = extractVideoId(item.url)
            if (videoId.isNullOrEmpty()) return null

            val title = item.name
            val artist: String
            val duration: Long
            val thumbnail: String?

            if (item is StreamInfoItemExtractor) {
                artist = cleanArtistName(item.getUploaderName())
                duration = item.getDuration().coerceAtLeast(0)
                thumbnail = item.getThumbnails().firstOrNull()?.getUrl()
            } else {
                artist = ""
                duration = 0
                thumbnail = item.getThumbnails().firstOrNull()?.getUrl()
            }

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
     * Turns YouTube/YouTube Music channel names into something readable in the UI:
     * "Pink Floyd - Topic" -> "Pink Floyd", "Daft Punk - VEVO" -> "Daft Punk".
     */
    private fun cleanArtistName(uploaderName: String): String {
        var name = uploaderName.trim()
        if (name.isEmpty()) return ""
        // Auto-generated topic channels are named "<Artist> - Topic".
        name = name.removeSuffix("- Topic").removeSuffix("- VEVO")
        // Case variations seen on YouTube ("- topic", "- vevo").
        name = name.removeSuffix("- topic").removeSuffix("- vevo")
        name = name.trim().trimEnd('-', '–').trim()
        return if (name.isEmpty()) uploaderName.trim() else name
    }

    private fun extractVideoId(url: String): String? {
        // YouTube stream URLs from NewPipe look like: https://www.youtube.com/watch?v=VIDEO_ID
        val param = "v="
        val idx = url.indexOf(param)
        return if (idx >= 0) {
            val start = idx + param.length
            val end = url.indexOf('&', start)
            if (end == -1) url.substring(start) else url.substring(start, end)
        } else null
    }

    companion object {
        /** The canonical name of the YouTube service in NewPipe Extractor. */
        const val YOUTUBE_SERVICE_NAME = "YouTube"

        /**
         * Content filter that makes NewPipe search YouTube Music instead of plain
         * YouTube. Any filter starting with "music_" enables YoutubeMusicSearchExtractor.
         */
        private const val MUSIC_SONGS_FILTER = "music_songs"
    }
}
