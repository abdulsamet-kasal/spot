package com.example.spoyt.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.example.spoyt.player.PlayerHolder
import java.util.concurrent.TimeUnit

/** One synced-lyrics line: [timeMs] is when the line starts being sung. */
data class LrcLine(val timeMs: Long, val text: String)

/** Result of a lyrics lookup; both fields may be missing (some songs have none). */
data class LyricsResult(val plain: String?, val synced: List<LrcLine>)

/**
 * Parses the LRC format ("[01:23.45] text lines") used by LRCLIB's syncedLyrics field.
 */
internal object LrcParser {

    private val lineRegex = Regex("\\[(\\d{1,2}):(\\d{1,2})(?:[.:,](\\d{1,3}))?\\](.*)")

    fun parse(synced: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        synced.lineSequence().forEach { raw ->
            val match = lineRegex.find(raw.trim()) ?: return@forEach
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fraction = match.groupValues[3]
            // ".45" means hundredths, ".456" means milliseconds; normalize to ms.
            val millis = when (fraction.length) {
                0 -> 0L
                1 -> fraction.toLongOrNull()?.times(100) ?: 0L
                2 -> fraction.toLongOrNull()?.times(10) ?: 0L
                else -> fraction.take(3).toLongOrNull() ?: 0L
            }
            val text = match.groupValues[4].trim()
            if (text.isNotEmpty()) {
                lines.add(LrcLine(timeMs = minutes * 60_000 + seconds * 1_000 + millis, text = text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}

/**
 * Free, keyless lyrics provider (lrclib.net). Everything is best-effort: any failure or
 * "not found" returns null so the player just shows the "no lyrics" state.
 */
class LyricsClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /** Looks up lyrics for [title]/[artist]; [durationSeconds] improves matching when known. */
    fun fetch(title: String, artist: String, durationSeconds: Long): LyricsResult? {
        val cleanTitle = YouTubeUtils.cleanTitleForLyrics(title)
        val cleanArtist = YouTubeUtils.cleanArtistName(artist)

        // First try an exact match (with duration), then relax the duration filter.
        return fetchOnce(cleanTitle, cleanArtist, durationSeconds)
            ?: fetchOnce(cleanTitle, cleanArtist, 0)
    }

    private fun fetchOnce(title: String, artist: String, durationSeconds: Long): LyricsResult? {
        return try {
            val url = StringBuilder("https://lrclib.net/api/get")
                .append("?artist_name=").append(encode(artist))
                .append("&track_name=").append(encode(title))
                .apply {
                    if (durationSeconds > 0) {
                        append("&duration=").append(durationSeconds)
                    }
                }
                .toString()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", PlayerHolder.HTTP_USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                toResult(JSONObject(body))
            }
        } catch (t: Throwable) {
            null
        }
    }

    private fun toResult(json: JSONObject): LyricsResult? {
        if (!json.optBoolean("instrumental", false)) {
            val synced = json.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
            val plain = json.optString("plainLyrics", "").takeIf { it.isNotBlank() }
            if (synced == null && plain == null) return null
            return LyricsResult(plain = plain, synced = synced?.let { LrcParser.parse(it) }.orEmpty())
        }
        return null
    }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value.trim(), "UTF-8")
}
