package com.example.spoyt.data.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * Tiny key/value preferences (SharedPreferences rather than DataStore: one boolean today,
 * and keeping the dependency surface small matters more in this app).
 */
object AppPrefs {

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext
                .getSharedPreferences("spoyt_prefs", Context.MODE_PRIVATE)
        }
    }

    /** When true, download jobs only run on an unmetered (Wi-Fi) network. */
    var wifiOnlyDownloads: Boolean
        get() = runCatching {
            prefs?.getBoolean("wifi_only_downloads", false) ?: false
        }.getOrDefault(false)
        set(value) {
            runCatching { prefs?.edit()?.putBoolean("wifi_only_downloads", value)?.apply() }
        }

    /**
     * What was playing, written periodically and when the media service is torn down.
     *
     * An aggressive ROM can stop the service in the middle of a song (v1.5.4's log showed
     * `service destroyed (playing=true)` with no player error at all). The queue lived only in
     * memory, so the restart had nothing to resume and the music was simply gone. Committed,
     * not applied: a process kill never flushes pending writes.
     */
    fun savePlaybackSnapshot(videoIds: List<String>, index: Int, positionMs: Long, playWhenReady: Boolean) {
        runCatching {
            prefs?.edit()
                ?.putString("resume_queue", videoIds.joinToString(","))
                ?.putInt("resume_index", index)
                ?.putLong("resume_position_ms", positionMs)
                ?.putBoolean("resume_playing", playWhenReady)
                ?.putLong("resume_at", System.currentTimeMillis())
                ?.commit()
        }
    }

    /** Last saved snapshot, or null when nothing playable was ever recorded. */
    fun loadPlaybackSnapshot(): PlaybackSnapshot? {
        val store = prefs ?: return null
        val queue = runCatching { store.getString("resume_queue", "") }.getOrNull().orEmpty()
        if (queue.isEmpty()) return null
        return PlaybackSnapshot(
            videoIds = queue.split(",").filter { it.isNotEmpty() },
            index = runCatching { store.getInt("resume_index", 0) }.getOrDefault(0),
            positionMs = runCatching { store.getLong("resume_position_ms", 0L) }.getOrDefault(0L),
            playWhenReady = runCatching { store.getBoolean("resume_playing", false) }.getOrDefault(false),
            savedAt = runCatching { store.getLong("resume_at", 0L) }.getOrDefault(0L)
        )
    }

    /** Forgets the snapshot so a stopped session is never resumed again (user paused it). */
    fun clearPlaybackSnapshot() {
        runCatching {
            prefs?.edit()?.remove("resume_queue")?.remove("resume_playing")?.commit()
        }
    }

    /**
     * Bounded auto-revive budget: a ROM that keeps stopping the service must not turn into a
     * restart loop that fights the user and drains the battery.
     */
    fun claimRevive(maximum: Int, windowMs: Long): Boolean {
        val store = prefs ?: return false
        val now = System.currentTimeMillis()
        val windowStart = runCatching { store.getLong("revive_window_at", 0L) }.getOrDefault(0L)
        val count = if (now - windowStart > windowMs) 0 else runCatching {
            store.getInt("revive_count", 0)
        }.getOrDefault(0)
        if (count >= maximum) return false
        runCatching {
            store.edit()
                ?.putLong("revive_window_at", if (count == 0) now else windowStart)
                ?.putInt("revive_count", count + 1)
                ?.commit()
        }
        return true
    }

    /** Restored playback state; see [savePlaybackSnapshot]. */
    data class PlaybackSnapshot(
        val videoIds: List<String>,
        val index: Int,
        val positionMs: Long,
        val playWhenReady: Boolean,
        val savedAt: Long
    )
}
