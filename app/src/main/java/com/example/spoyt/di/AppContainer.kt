package com.example.spoyt.di

import android.content.Context
import android.content.Intent
import com.example.spoyt.data.local.AppDatabase
import com.example.spoyt.data.local.repositories.DefaultDownloadRepository
import com.example.spoyt.data.local.repositories.DefaultHistoryRepository
import com.example.spoyt.data.local.repositories.DefaultPlaylistRepository
import com.example.spoyt.data.local.repositories.DefaultPlaylistTrackRepository
import com.example.spoyt.data.local.repositories.DefaultTrackRepository
import com.example.spoyt.data.local.repositories.DownloadRepository
import com.example.spoyt.data.local.repositories.HistoryRepository
import com.example.spoyt.data.local.repositories.PlaylistRepository
import com.example.spoyt.data.local.repositories.PlaylistTrackRepository
import com.example.spoyt.data.local.repositories.TrackRepository
import com.example.spoyt.data.remote.YoutubeSource
import com.example.spoyt.player.MusicPlaybackService
import com.example.spoyt.player.PlaybackController

/**
 * Manual dependency injection container. Holds the singletons (database, repositories,
 * NewPipe source) so ViewModels can obtain them without Hilt.
 */
object AppContainer {

    private var appContext: Context? = null
    private var database: AppDatabase? = null
    private var trackRepository: TrackRepository? = null
    private var playlistRepository: PlaylistRepository? = null
    private var playlistTrackRepository: PlaylistTrackRepository? = null
    private var downloadRepository: DownloadRepository? = null
    private var historyRepository: HistoryRepository? = null
    private var youtubeSource: YoutubeSource? = null
    private var playbackController: PlaybackController? = null

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        database = AppDatabase.getInstance(app)
        trackRepository = DefaultTrackRepository(database!!)
        playlistRepository = DefaultPlaylistRepository(database!!)
        playlistTrackRepository = DefaultPlaylistTrackRepository(database!!)
        downloadRepository = DefaultDownloadRepository(database!!)
        historyRepository = DefaultHistoryRepository(database!!)
        youtubeSource = YoutubeSource()
        playbackController = PlaybackController(
            trackRepository = trackRepository!!,
            downloads = downloadRepository!!,
            history = historyRepository!!,
            youtube = youtubeSource!!
        )
    }

    val context: Context
        get() = appContext ?: error("AppContainer not initialized. Call init(Context) first.")

    val db: AppDatabase
        get() = database ?: error("AppContainer not initialized. Call init(Context) first.")

    val tracks: TrackRepository
        get() = trackRepository ?: error("AppContainer not initialized")

    val playlists: PlaylistRepository
        get() = playlistRepository ?: error("AppContainer not initialized")

    val playlistTracks: PlaylistTrackRepository
        get() = playlistTrackRepository ?: error("AppContainer not initialized")

    val downloads: DownloadRepository
        get() = downloadRepository ?: error("AppContainer not initialized")

    val history: HistoryRepository
        get() = historyRepository ?: error("AppContainer not initialized")

    val youtube: YoutubeSource
        get() = youtubeSource ?: error("AppContainer not initialized")

    val playback: PlaybackController
        get() = playbackController ?: error("AppContainer not initialized")

    /**
     * Starts [MusicPlaybackService] so background playback and the media notification work.
     *
     * Deliberately uses [Context.startService] (not `startForegroundService`): Media3's own
     * MediaNotificationManager promotes the service and posts the notification atomically, so we
     * avoid the Android 12+ five-second "foreground service did not start in time" crash window.
     */
    fun startPlaybackService() {
        val ctx = appContext ?: return
        runCatching { ctx.startService(Intent(ctx, MusicPlaybackService::class.java)) }
    }
}
