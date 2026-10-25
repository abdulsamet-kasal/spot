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
}
