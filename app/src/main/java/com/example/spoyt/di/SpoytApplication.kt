package com.example.spoyt.di

import android.app.Application
import com.example.spoyt.data.remote.NewPipeDownloader
import com.example.spoyt.player.PlayerHolder
import org.schabi.newpipe.extractor.NewPipe

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
    }
}
