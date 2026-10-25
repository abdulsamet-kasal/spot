package com.example.spoyt.di

import android.app.Application
import com.example.spoyt.data.remote.NewPipeDownloader
import org.schabi.newpipe.extractor.NewPipe

/**
 * Application subclass used to one-time initialize NewPipe Extractor with our
 * [NewPipeDownloader] implementation and to expose the shared [AppContainer].
 */
class SpoytApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize NewPipe Extractor with a browser-like OkHttp downloader first,
        // because YoutubeSource reads NewPipe.getDownloader() at construction time.
        NewPipe.init(NewPipeDownloader())
        // Initialize manual DI container (database + repositories + YoutubeSource).
        AppContainer.init(this)
    }
}
