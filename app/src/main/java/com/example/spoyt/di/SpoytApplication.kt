package com.example.spoyt.di

import android.app.Application
import android.util.Log
import com.example.spoyt.data.prefs.AppPrefs
import com.example.spoyt.data.remote.NewPipeDownloader
import com.example.spoyt.player.PlayerHolder
import org.schabi.newpipe.extractor.NewPipe
import java.io.File
import java.util.Date

/**
 * Application subclass used to one-time initialize NewPipe Extractor with our
 * [NewPipeDownloader] implementation, to create the shared ExoPlayer and to expose the
 * shared [AppContainer].
 */
class SpoytApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize NewPipe Extractor with a browser-like OkHttp downloader first,
        // because YoutubeSource reads NewPipe.getDownloader() at construction time.
        NewPipe.init(NewPipeDownloader())
        // Create the ExoPlayer up front. MusicPlaybackService.init() is idempotent, but if the
        // player were only created by the service, the very first play() would run before the
        // service existed and playback would silently do nothing.
        PlayerHolder.init(this)
        // Initialize manual DI container (database + repositories + YoutubeSource).
        AppContainer.init(this)
        installCrashLogger()
    }

    /**
     * Before handing a fatal exception to the platform handler, append it to filesDir/crash.log
     * (kept small). There is no analytics service, so this local file is the only way to look
     * at a crash that happened on the phone.
     */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val file = File(filesDir, "crash.log")
                val entry = "\n=== ${Date()} · ${thread.name} ===\n" +
                    Log.getStackTraceString(throwable)
                file.appendText(entry)
                if (file.length() > 256 * 1024) {
                    file.writeText(file.readText().takeLast(128 * 1024))
                }
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
