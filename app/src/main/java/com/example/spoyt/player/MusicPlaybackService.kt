package com.example.spoyt.player

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.spoyt.data.prefs.AppPrefs
import com.example.spoyt.di.AppContainer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Media3 [MediaSessionService] that keeps playback alive in the background and publishes
 * the media notification and lock-screen controls.
 *
 * The service owns [PlayerHolder] initialization and exposes playback through a [MediaSession]
 * backed by [QueueAwarePlayer], so system UI, media buttons, and the lock screen can control
 * playback natively.
 *
 * A custom [SpoytNotificationProvider] builds the notification (title/artist/artwork plus
 * previous / play-pause / next buttons). Skipping itself is native: [QueueAwarePlayer]
 * advertises the app-owned queue as standard player commands, so the system media card,
 * the lock screen and media buttons drive [PlayerHolder] through the session instead of
 * relying on app-specific commands. Like, shuffle and repeat, which have no standard
 * system buttons, are published as custom commands.
 */
class MusicPlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    /** CPU + Wi-Fi keep-awake while the user wants music, including between tracks. */
    private var wakeLocks: PlaybackWakeLocks? = null

    /** Set when the user removes the app from recents: no snapshot, no rescue restart. */
    private var stoppedByUser = false

    /**
     * Wired headsets, USB DACs and Bluetooth audio all raise `ACTION_AUDIO_BECOMING_NOISY`
     * when the output disappears. Music players are expected to pause there instead of
     * suddenly shouting out of the phone speaker.
     */
    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AudioManager.ACTION_AUDIO_BECOMING_NOISY) return
            val playing = runCatching { PlayerHolder.engine.isPlaying }.getOrDefault(false)
            if (!playing) return
            PlaybackLog.line("audio output lost (headset unplugged) -> pause")
            PlayerHolder.pause()
        }
    }

    /** Authorizes and handles the custom shuffle/repeat commands; skipping is a player command. */
    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(CUSTOM_COMMAND_LIKE, Bundle.EMPTY))
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
                CUSTOM_COMMAND_LIKE -> {
                    toggleCurrentLike()
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
            // The session must report the app-owned queue controls, otherwise the system
            // greys out next/previous: ExoPlayer holds one item and says it cannot skip.
            val builder = MediaSession.Builder(this, QueueAwarePlayer(PlayerHolder.engine))
                .setCallback(sessionCallback)
            pendingLaunchIntent()?.let { builder.setSessionActivity(it) }
            session = builder.build()
            // Shuffle and repeat buttons on the Android 13+ system media card
            // (lock screen / quick settings); skipping is already a native session command.
            applyMediaCardButtons()
            // Pause when the wired/Bluetooth output disappears (headset unplugged).
            runCatching {
                ContextCompat.registerReceiver(
                    this,
                    becomingNoisyReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
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

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        // Only the transition window (IDLE/BUFFERING with playback wanted) needs
                        // app-owned locks; see needsKeepAwake().
                        wakeLocks?.setNeeded(needsKeepAwake())
                        persistSnapshot()
                        SpoytWidgetProvider.updateAll(applicationContext)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        // Entering or leaving the transition window (READY at last track change,
                        // BUFFERING on a network stall) toggles the locks accordingly.
                        wakeLocks?.setNeeded(needsKeepAwake())
                    }

                    override fun onMediaItemTransition(
                        mediaItem: androidx.media3.common.MediaItem?,
                        reason: Int
                    ) {
                        // Track changes can happen right before the next watchdog heartbeat;
                        // persisting here keeps queue index/position fresh for a revive.
                        persistSnapshot()
                        SpoytWidgetProvider.updateAll(applicationContext)
                    }
                }
            )
            // CPU + Wi-Fi stay locked only for the transition window (IDLE/BUFFERING with music
            // wanted). ExoPlayer releases its own radio lock there, and holding app locks for the
            // whole session made vivo force-stop the process ~60 s after the screen went off.
            wakeLocks = PlaybackWakeLocks(applicationContext).apply {
                setNeeded(needsKeepAwake())
            }
        } catch (t: Throwable) {
            // A broken media session must not take the whole app down.
            PlaybackLog.line("service onCreate failed: ${t.javaClass.simpleName} ${t.message}")
            PlaybackEvents.post("Oynatıcı başlatılamadı. Lütfen uygulamayı yeniden açın.")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A null intent means the system restarted the sticky service after the ROM stopped
        // it mid-song (vivo's power manager stops the service while playing=true with no
        // player error at all). Rebuild the engine and auto-resume the persisted snapshot so
        // music comes back by itself instead of waiting for the user to reopen the app.
        if (intent == null) {
            PlaybackLog.line("service restarted by system (intent=null) -> auto-resume")
            runCatching { PlayerHolder.init(applicationContext) }
            AppContainer.playback.revivePlaybackAsync()
            return Service.START_STICKY
        }
        // Transport buttons from the notification come back as service intents.
        when (intent?.action) {
            ACTION_MEDIA_PREV -> PlayerHolder.playPrevious()
            ACTION_MEDIA_PLAY_PAUSE -> PlayerHolder.playPause()
            ACTION_MEDIA_NEXT -> PlayerHolder.playNext()
            ACTION_MEDIA_LIKE -> toggleCurrentLike()
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
        // Swiping the app out of recents is an explicit "stop": music must not keep playing and
        // the snapshot must not be revived the next time the app opens.
        stoppedByUser = true
        PlaybackLog.line("task removed by user -> stop playback")
        runCatching { PlayerHolder.pause() }
        runCatching { session?.player?.clearMediaItems() }
        AppPrefs.clearPlaybackSnapshot()
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    override fun onDestroy() {
        // Save the live state BEFORE releasing the engine: a ROM that stops the service
        // mid-song must leave behind a resumable snapshot. A paused session is cleared so a
        // later task-removal/restart never resurrects music the user deliberately stopped.
        val live = runCatching { PlayerHolder.snapshot() }.getOrNull()
        if (!stoppedByUser && live != null && live.playWhenReady && live.videoIds.isNotEmpty()) {
            AppPrefs.savePlaybackSnapshot(live.videoIds, live.index, live.positionMs, true)
            PlaybackLog.line("snapshot saved (pos=${live.positionMs / 1000}ms index=${live.index})")
            // vivo's power manager stops the service mid-song. If it merely stopped the
            // service while the process is still alive, one delayed self-restart can bring
            // the music back by itself. A force-stop kills the process before this ever
            // runs — no legal way exists to resurrect it; then only the next app open can
            // continue the session (PlaybackController.reviveAfterAppOpen). The revive
            // budget inside claimRevive caps this rescue to a few attempts per half hour.
            Handler(Looper.getMainLooper()).postDelayed({
                runCatching {
                    PlaybackLog.line("rescue: servisi yeniden baslatma denemesi")
                    AppContainer.startPlaybackService()
                    AppContainer.playback.reviveAfterServiceRescue()
                }
            }, RESCUE_RESTART_DELAY_MS)
        } else {
            AppPrefs.clearPlaybackSnapshot()
        }
        // Release the session first; PlayerHolder owns the ExoPlayer lifecycle and releases it.
        PlaybackLog.line("service destroyed (playing=${runCatching { PlayerHolder.engine.isPlaying }.getOrDefault(false)})")
        runCatching { unregisterReceiver(becomingNoisyReceiver) }
        PlayerHolder.onTransportControlsChanged = null
        wakeLocks?.release()
        wakeLocks = null
        runCatching { session?.release() }
        session = null
        PlayerHolder.release()
        super.onDestroy()
    }

    /**
     * True only while the player is between streams: music is wanted but the next URL has not
     * been fetched yet. That is exactly the window where ExoPlayer drops its own
     * `WAKE_MODE_NETWORK` locks and a sleeping radio would stall the next track.
     *
     * Outside this window the locks are released. v1.5.4 held them for the entire session and
     * vivo answered with a force-stop about a minute after screen-off (log: `keepAwake
     * cpu=true wifi=true` → `service destroyed (playing=true)`, no restart), while v1.5.1 —
     * which held no continuous locks — had no such stops.
     */
    private fun needsKeepAwake(): Boolean {
        val engine = runCatching { PlayerHolder.engine }.getOrNull() ?: return false
        if (!engine.playWhenReady) return false
        return engine.playbackState == androidx.media3.common.Player.STATE_IDLE ||
            engine.playbackState == androidx.media3.common.Player.STATE_BUFFERING
    }

    /** Publishing shuffle/repeat state as buttons requires rebuilding the custom layout. */
    private fun applyMediaCardButtons() {
        val s = session ?: return
        runCatching {
            s.setCustomLayout(
                SpoytNotificationProvider.mediaCardButtons(
                    shuffling = PlayerHolder.isShuffling(),
                    repeatMode = PlayerHolder.repeatModeState.value,
                    liked = PlayerHolder.currentTrackState.value?.liked == true
                )
            )
        }
    }

    /**
     * Like toggle for the current track, shared by the notification action and the custom
     * session command the system player card uses. The heart icon is republished right after
     * the write so card and notification agree immediately.
     */
    private fun toggleCurrentLike() {
        val track = PlayerHolder.currentTrackState.value ?: return
        AppContainer.playback.toggleLikeAsync(track)
    }

    /** Persists the resumable playback state (called on the main thread from player callbacks). */
    private fun persistSnapshot() {
        val live = runCatching { PlayerHolder.snapshot() }.getOrNull() ?: return
        if (live.videoIds.isNotEmpty()) {
            AppPrefs.savePlaybackSnapshot(live.videoIds, live.index, live.positionMs, live.playWhenReady)
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

    private companion object {
        /** How long after the service death the self-restart rescue waits. */
        const val RESCUE_RESTART_DELAY_MS = 1_500L
    }
}
