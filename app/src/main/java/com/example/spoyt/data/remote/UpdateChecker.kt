package com.example.spoyt.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.example.spoyt.player.PlayerHolder
import java.util.concurrent.TimeUnit

/**
 * Checks GitHub Releases for a newer app version. No account, no key: the repository is
 * public and the `latest` release endpoint is anonymous-friendly.
 */
object UpdateChecker {

    private const val LATEST_URL =
        "https://api.github.com/repos/abdulsamet-kasal/spot/releases/latest"

    /** Release page the "open" action points at. */
    const val RELEASES_URL = "https://github.com/abdulsamet-kasal/spot/releases/latest"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Returns the latest release tag (e.g. "v1.4.0") or null when offline / rate-limited.
     * Must run off the main thread.
     */
    fun latestVersion(): String? = try {
        val request = Request.Builder()
            .url(LATEST_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", PlayerHolder.HTTP_USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            JSONObject(body).optString("tag_name", "").takeIf { it.isNotBlank() }
        }
    } catch (t: Throwable) {
        null
    }

    /** True when [candidate] (e.g. "v1.5.0") is newer than [current] (e.g. "1.4.0"). */
    fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = normalize(candidate)
        val currentParts = normalize(current)
        for (index in 0 until maxOf(candidateParts.size, currentParts.size)) {
            val c = candidateParts.getOrElse(index) { 0 }
            val cur = currentParts.getOrElse(index) { 0 }
            if (c != cur) return c > cur
        }
        return false
    }

    private fun normalize(version: String): List<Int> =
        version.trim().removePrefix("v").removePrefix("V")
            .split('.', '-')
            .mapNotNull { part -> part.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toIntOrNull() }
}
