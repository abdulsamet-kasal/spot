package com.example.spoyt.data.remote

/**
 * Pure string helpers for YouTube URLs and channel names.
 *
 * Kept in their own object so they can be unit tested without touching NewPipe.
 */
internal object YouTubeUtils {

    /**
     * Accepts `https://www.youtube.com/watch?v=ID`, `https://music.youtube.com/watch?v=ID`,
     * `https://youtu.be/ID`, `m.youtube.com` and `...&v=ID` forms. Returns the 11-char id.
     */
    fun extractVideoId(url: String): String? {
        val value = url.trim()
        if (value.isEmpty()) return null
        // Only treat YouTube hosts as links; "example.com/watch?v=x" is not ours.
        if (!value.contains("youtube.com") && !value.contains("youtu.be")) return null
        if (value.contains("/watch")) {
            val marker = value.indexOf("v=")
            if (marker >= 0) {
                val start = marker + 2
                val end = value.indexOf('&', start)
                val id = if (end == -1) value.substring(start) else value.substring(start, end)
                if (id.isNotBlank()) return id
            }
        }
        // youtu.be/<id>
        val short = Regex("youtu\\.be/([A-Za-z0-9_-]{6,})").find(value)
        return short?.groupValues?.get(1)
    }

    /** True when the text looks like a YouTube link the user pasted into the search field. */
    fun looksLikeLink(text: String): Boolean {
        val value = text.trim()
        if (!value.startsWith("http://") && !value.startsWith("https://")) return false
        return value.contains("youtube.com") || value.contains("youtu.be")
    }

    /**
     * Turns YouTube/YouTube Music channel names into something readable in the UI:
     * "Pink Floyd - Topic" -> "Pink Floyd", "Daft Punk - VEVO" -> "Daft Punk".
     */
    fun cleanArtistName(uploaderName: String): String {
        var name = uploaderName.trim()
        if (name.isEmpty()) return ""
        listOf("- Topic", "- topic", "- VEVO", "- vevo").forEach { suffix ->
            if (name.endsWith(suffix, ignoreCase = true)) {
                name = name.dropLast(suffix.length).trim()
            }
        }
        name = name.trimEnd('-', '–', '—', ' ').trim()
        // Nothing readable left after stripping decorations: keep the original.
        if (name.isEmpty()) return uploaderName.trim()
        return name
    }

    /**
     * Strips noise from video titles before they are sent to the lyrics provider:
     * "Song (Official Video)" -> "Song", "Song [4K Remaster]" -> "Song".
     */
    fun cleanTitleForLyrics(title: String): String {
        var name = title
        Regex("[\\[(][^\\])]*[\\])]").findAll(name).toList().forEach { match ->
            val inner = match.value
            val noise = listOf(
                "video", "lyric", "audio", "official", "hd", "4k", "remaster", "mv", "m/v",
                "visualizer", "live", "clip", "version", "explicit", "hq"
            ).any { inner.lowercase().contains(it) }
            if (noise) name = name.replace(match.value, " ")
        }
        // Trailing dashes/braces and repeated whitespace.
        name = name.replace(Regex("\\s{2,}"), " ").trim()
        name = name.trimEnd('-', '–', '—', '(', '[', ' ').trim()
        return name.ifEmpty { title.trim() }
    }
}
