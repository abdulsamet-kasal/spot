package com.example.spoyt.player

import android.content.Intent
import android.os.Bundle
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Media3 [MediaSessionService] that keeps playback alive in the background and publishes
 * the media notification and lock-screen controls.
 *
 * The service owns [PlayerHolder] initialization and exposes the [androidx.media3.exoplayer.ExoPlayer]
 * through a [MediaSession] so system UI, media buttons, and the lock screen can control playback.
 *
 * A custom [SpoytNotificationProvider] builds the notification (title/artist/artwork plus
 * previous / play-pause / next buttons), and custom session commands back the skip buttons
 * on the Android 13+ system media card — the player holds a single media item, so ExoPlayer's
 * own seek commands are unavailable and the default notification would have no skip buttons.
 */
class MusicPlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    /** Authorizes the skip commands and handles them for the system media card. */
    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(CUSTOM_COMMAND_PREV, Bundle.EMPTY))
                .add(SessionCommand(CUSTOM_COMMAND_NEXT, Bundle.EMPTY))
                .add(SessionCommand(CUSTOM_COMMAND_SHUFFLE, Bundle.EMPTY))
                .add(SessionCommand(CUSTOM_COMMAND_REPEAT, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            command: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return when (command.customAction) {
                CUSTOM_COMMAND_PREV -> {
                    PlayerHolder.playPrevious()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                CUSTOM_COMMAND_NEXT -> {
                    PlayerHolder.playNext()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                CUSTOM_COMMAND_SHUFFLE -> {
                    PlayerHolder.toggleShuffle()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                CUSTOM_COMMAND_REPEAT -> {
                    PlayerHolder.cycleRepeatMode()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                else -> Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            PlayerHolder.init(applicationContext)
            setMediaNotificationProvider(SpoytNotificationProvider(this))
            val builder = MediaSession.Builder(this, PlayerHolder.engine)
                .setCallback(sessionCallback)
            pendingLaunchIntent()?.let { builder.setSessionActivity(it) }
            session = builder.build()
            // Skip, shuffle and repeat buttons on the Android 13+ system media card
            // (lock screen / quick settings).
            applyMediaCardButtons()
            // Repeat/shuffle are app-owned state, invisible to ExoPlayer: without a nudge the
            // notification would keep the old icons after a toggle.
            PlayerHolder.onTransportControlsChanged = {
                applyMediaCardButtons()
                runCatching { session?.let { onUpdateNotification(it, /* startInForeground = */ true) } }
            }
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
        // Transport buttons from the notification come back as service intents.
        when (intent?.action) {
            ACTION_MEDIA_PREV -> PlayerHolder.playPrevious()
            ACTION_MEDIA_PLAY_PAUSE -> PlayerHolder.playPause()
            ACTION_MEDIA_NEXT -> PlayerHolder.playNext()
            ACTION_MEDIA_SHUFFLE -> PlayerHolder.toggleShuffle()
            ACTION_MEDIA_REPEAT -> PlayerHolder.cycleRepeatMode()
        }
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
        PlayerHolder.onTransportControlsChanged = null
        runCatching { session?.release() }
        session = null
        PlayerHolder.release()
        super.onDestroy()
    }

    /** Publishing shuffle/repeat state as buttons requires rebuilding the custom layout. */
    private fun applyMediaCardButtons() {
        val s = session ?: return
        runCatching {
            s.setCustomLayout(
                SpoytNotificationProvider.mediaCardButtons(
                    shuffling = PlayerHolder.isShuffling(),
                    repeatMode = PlayerHolder.repeatModeState.value
                )
            )
        }
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
