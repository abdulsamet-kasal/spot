package com.example.spoyt.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import com.example.spoyt.R
import com.google.common.collect.ImmutableList

/** Transport button intents routed through [MusicPlaybackService.onStartCommand]. */
const val ACTION_MEDIA_PREV = "com.example.spoyt.action.MEDIA_PREV"
const val ACTION_MEDIA_PLAY_PAUSE = "com.example.spoyt.action.MEDIA_PLAY_PAUSE"
const val ACTION_MEDIA_NEXT = "com.example.spoyt.action.MEDIA_NEXT"
const val ACTION_MEDIA_SHUFFLE = "com.example.spoyt.action.MEDIA_SHUFFLE"
const val ACTION_MEDIA_REPEAT = "com.example.spoyt.action.MEDIA_REPEAT"

/** Custom session commands behind the Android 13+ media card's prev/next buttons. */
const val CUSTOM_COMMAND_PREV = "spoyt.PREV"
const val CUSTOM_COMMAND_NEXT = "spoyt.NEXT"
const val CUSTOM_COMMAND_SHUFFLE = "spoyt.SHUFFLE"
const val CUSTOM_COMMAND_REPEAT = "spoyt.REPEAT"

private const val NOTIFICATION_ID = 2001
private const val CHANNEL_ID = "spoyt_playback"

/**
 * Builds the media notification for the shade and the lock screen: song title, artist and
 * artwork (MediaStyle reads them from the session's MediaMetadata) plus explicit
 * previous / play-pause / next buttons.
 *
 * The transport buttons use explicit service PendingIntents on purpose: the player only
 * ever holds one media item, so ExoPlayer's own seek-to-next/previous commands are
 * unavailable and Media3's default notification would show no skip buttons at all.
 */
class SpoytNotificationProvider(private val context: Context) : MediaNotification.Provider {

    override fun createNotification(
        session: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        callback: MediaNotification.Provider.Callback
    ): MediaNotification {
        createChannel()
        val player = session.player
        val metadata = player.mediaMetadata
        val title = metadata.title?.toString().takeUnless { it.isNullOrEmpty() } ?: "Sametify"
        val artist = metadata.artist?.toString().orEmpty()
        val repeatMode = PlayerHolder.repeatModeState.value
        val shuffling = PlayerHolder.isShuffling()

        val style = MediaStyleNotificationHelper.MediaStyle(session)
            .setShowActionsInCompactView(0, 1, 2)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(session.sessionActivity)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(style)
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_notif_previous, "Önceki", serviceIntent(ACTION_MEDIA_PREV)
                ).build()
            )
            .addAction(playPauseAction(player.isPlaying))
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_notif_next, "Sonraki", serviceIntent(ACTION_MEDIA_NEXT)
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    if (shuffling) R.drawable.ic_notif_shuffle else R.drawable.ic_notif_shuffle_off,
                    if (shuffling) "Karıştırma: açık" else "Karıştırma: kapalı",
                    serviceIntent(ACTION_MEDIA_SHUFFLE)
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    repeatIcon(repeatMode),
                    repeatLabel(repeatMode),
                    serviceIntent(ACTION_MEDIA_REPEAT)
                ).build()
            )
            .build()

        return MediaNotification(NOTIFICATION_ID, notification)
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean = false

    /** Buttons sent through the service so no player seek commands are required. */
    private fun serviceIntent(action: String): PendingIntent =
        PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, MusicPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun playPauseAction(isPlaying: Boolean): NotificationCompat.Action {
        val icon = if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
        val label = if (isPlaying) "Duraklat" else "Oynat"
        return NotificationCompat.Action.Builder(icon, label, serviceIntent(ACTION_MEDIA_PLAY_PAUSE)).build()
    }

    private fun repeatIcon(mode: RepeatMode): Int = when (mode) {
        RepeatMode.OFF -> R.drawable.ic_notif_repeat_off
        RepeatMode.ALL -> R.drawable.ic_notif_repeat
        RepeatMode.ONE -> R.drawable.ic_notif_repeat_one
    }

    private fun repeatLabel(mode: RepeatMode): String = when (mode) {
        RepeatMode.OFF -> "Tekrar: kapalı"
        RepeatMode.ALL -> "Tekrar: tüm liste"
        RepeatMode.ONE -> "Tekrar: bu şarkı"
    }

    private fun createChannel() {
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Müzik çalma",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        /** [MediaNotification] id used when Media3 posts our notification. */
        const val ID = NOTIFICATION_ID

        /** Custom layout (Android 13+ media card) with skip, shuffle and repeat buttons. */
        fun mediaCardButtons(shuffling: Boolean, repeatMode: RepeatMode): List<CommandButton> {
            fun button(label: String, icon: Int, action: String) = CommandButton.Builder()
                .setDisplayName(label)
                .setIconResId(icon)
                .setSessionCommand(SessionCommand(action, Bundle.EMPTY))
                .build()

            return listOf(
                button("Önceki", R.drawable.ic_notif_previous, CUSTOM_COMMAND_PREV),
                button("Sonraki", R.drawable.ic_notif_next, CUSTOM_COMMAND_NEXT),
                button(
                    if (shuffling) "Karıştırma: açık" else "Karıştırma: kapalı",
                    if (shuffling) R.drawable.ic_notif_shuffle else R.drawable.ic_notif_shuffle_off,
                    CUSTOM_COMMAND_SHUFFLE
                ),
                button(
                    when (repeatMode) {
                        RepeatMode.OFF -> "Tekrar: kapalı"
                        RepeatMode.ALL -> "Tekrar: tüm liste"
                        RepeatMode.ONE -> "Tekrar: bu şarkı"
                    },
                    when (repeatMode) {
                        RepeatMode.OFF -> R.drawable.ic_notif_repeat_off
                        RepeatMode.ALL -> R.drawable.ic_notif_repeat
                        RepeatMode.ONE -> R.drawable.ic_notif_repeat_one
                    },
                    CUSTOM_COMMAND_REPEAT
                )
            )
        }
    }
}
