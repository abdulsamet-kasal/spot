package com.example.spoyt.player

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Media3 [MediaSessionService] that keeps playback alive in the background and publishes
 * the media notification and lock-screen controls.
 *
 * The service owns [PlayerHolder] initialization and exposes the [androidx.media3.exoplayer.ExoPlayer]
 * through a [MediaSession] so system UI, media buttons, and the lock screen can control playback.
 */
class MusicPlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        try {
            PlayerHolder.init(applicationContext)
            val builder = MediaSession.Builder(this, PlayerHolder.engine)
            pendingLaunchIntent()?.let { builder.setSessionActivity(it) }
            session = builder.build()
            // Mirror playback state changes onto the home-screen widget. The widget reads the
            // PlayerHolder flows, so updating right after the player reports a change is enough.
            PlayerHolder.engine.addListener(
                object : androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        SpoytWidgetProvider.updateAll(applicationContext)
                    }

                    override fun onMediaItemTransition(
                        mediaItem: androidx.media3.common.MediaItem?,
                        reason: Int
                    ) {
                        SpoytWidgetProvider.updateAll(applicationContext)
                    }
                }
            )
        } catch (t: Throwable) {
            // A broken media session must not take the whole app down.
            PlaybackEvents.post("Oynatıcı başlatılamadı. Lütfen uygulamayı yeniden açın.")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        // Nudge Media3 into posting the media notification / foreground promotion right away.
        // Safe: MediaNotificationManager.startForeground() calls startForegroundService() and
        // Service.startForeground() back to back, so there is no un-notified window.
        val s = session
        if (s != null) {
            runCatching { onUpdateNotification(s, /* startInForeground = */ true) }
        }
        return result
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        // Release the session first; PlayerHolder owns the ExoPlayer lifecycle and releases it.
        runCatching { session?.release() }
        session = null
        PlayerHolder.release()
        super.onDestroy()
    }

    /** Tapping the media notification opens the app. */
    private fun pendingLaunchIntent(): android.app.PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return android.app.PendingIntent.getActivity(
            this,
            0,
            launch,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }
}
