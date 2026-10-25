package com.example.spoyt.player

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.spoyt.R

/**
 * Home-screen widget: current track + prev / play-pause / next buttons.
 *
 * The buttons are broadcast intents handled right here; the provider touches only the
 * RemoteViews of the widget, never the player directly (PlayerHolder is thread-guarded and
 * the play/pause/next calls hop to the main thread internally).
 */
class SpoytWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> build(context, appWidgetManager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> PlayerHolder.playPause()
            ACTION_NEXT -> PlayerHolder.playNext()
            ACTION_PREV -> PlayerHolder.playPrevious()
        }
        // Reflect the new state immediately (playback state changes also push an update via
        // the service listener; this covers the initial layout and tapping while paused).
        updateAll(context)
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.spoyt.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.spoyt.widget.NEXT"
        const val ACTION_PREV = "com.example.spoyt.widget.PREV"

        fun updateAll(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, SpoytWidgetProvider::class.java)
                )
                ids.forEach { id -> build(context, manager, id) }
            }
        }

        private fun build(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_spoyt)

            val track = PlayerHolder.currentTrackState.value
            views.setTextViewText(
                R.id.widget_title,
                track?.title ?: "Spoyt"
            )
            views.setTextViewText(
                R.id.widget_artist,
                track?.artist?.ifBlank { "" } ?: ""
            )

            views.setImageViewResource(
                R.id.widget_play,
                if (PlayerHolder.isPlayingState.value) {
                    android.R.drawable.ic_media_pause
                } else {
                    android.R.drawable.ic_media_play
                }
            )

            views.setOnClickPendingIntent(
                R.id.widget_prev, pending(context, ACTION_PREV)
            )
            views.setOnClickPendingIntent(
                R.id.widget_play, pending(context, ACTION_PLAY_PAUSE)
            )
            views.setOnClickPendingIntent(
                R.id.widget_next, pending(context, ACTION_NEXT)
            )

            runCatching { manager.updateAppWidget(appWidgetId, views) }
        }

        private fun pending(context: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                // Distinct request codes so the three intents do not collide.
                when (action) {
                    ACTION_PREV -> 1
                    ACTION_PLAY_PAUSE -> 2
                    else -> 3
                },
                Intent(context, SpoytWidgetProvider::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }
}
