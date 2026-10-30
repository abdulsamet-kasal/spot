package com.example.spoyt.player

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Append-only playback trail in the same file the fatal crash handler uses, so Settings'
 * existing "copy error log" row exposes it without any new UI.
 *
 * Background playback failures are device specific and cannot be reproduced here; every stop,
 * retry and lock decision is recorded with its Media3 error code so one paste identifies the
 * actual cause instead of a guess.
 */
object PlaybackLog {

    private var file: File? = null

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun init(context: Context) {
        if (file == null) file = File(context.filesDir, "crash.log")
    }

    fun line(text: String) {
        val target = file ?: return
        runCatching {
            // One listening session writes hundreds of lines; keep the file small enough that
            // Settings' "copy error log" (last 16 KB) always contains the interesting tail.
            if (target.length() > MAX_BYTES) target.writeText(target.readText().takeLast(KEEP_CHARS))
            target.appendText("${timeFormat.format(Date())} $text\n")
        }
    }

    private const val MAX_BYTES = 256_000L
    private const val KEEP_CHARS = 64_000
}
