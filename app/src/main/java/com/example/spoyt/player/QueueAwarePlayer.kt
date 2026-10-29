package com.example.spoyt.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

/**
 * Session-facing view of the player.
 *
 * The queue, the shuffle flag and the repeat mode are owned by [PlayerHolder] and ExoPlayer only
 * ever holds the current track, so the engine reports "no next item" and no shuffle/repeat
 * support. Android then renders the lock screen and system media card buttons greyed out. This
 * wrapper advertises those controls as standard player commands and routes them back to
 * [PlayerHolder], so the system UI, media buttons and any controller drive the app natively.
 *
 * Nothing is forwarded to ExoPlayer for shuffle/repeat on purpose: with a single-item player
 * REPEAT_MODE_ALL would make the engine loop the one item and STATE_ENDED would never fire,
 * which is how the queue advances to the next track.
 */
class QueueAwarePlayer(engine: Player) : ForwardingPlayer(engine) {

    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands()
            .buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .add(Player.COMMAND_SET_SHUFFLE_MODE)
            .add(Player.COMMAND_SET_REPEAT_MODE)
            .build()

    override fun seekToNext() = PlayerHolder.playNext()

    override fun seekToNextMediaItem() = PlayerHolder.playNext()

    override fun seekToPrevious() = PlayerHolder.playPrevious()

    override fun seekToPreviousMediaItem() = PlayerHolder.playPrevious()

    override fun hasNext(): Boolean = hasNextMediaItem()

    override fun hasNextMediaItem(): Boolean {
        val queue = PlayerHolder.queueState.value
        if (queue.isEmpty()) return false
        // Repeating one track still "advances", repeat-all wraps; without either, the last
        // track has no next and the button must stay disabled.
        return PlayerHolder.repeatModeState.value != RepeatMode.OFF ||
            PlayerHolder.currentIndexState.value < queue.lastIndex
    }

    override fun hasPrevious(): Boolean = hasPreviousMediaItem()

    /** Previous always restarts the current track, so a non-empty queue is enough. */
    override fun hasPreviousMediaItem(): Boolean = PlayerHolder.queueState.value.isNotEmpty()

    override fun getRepeatMode(): Int = when (PlayerHolder.repeatModeState.value) {
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    }

    override fun setRepeatMode(repeatMode: Int) {
        PlayerHolder.setRepeatMode(
            when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
        )
    }

    override fun getShuffleModeEnabled(): Boolean = PlayerHolder.isShuffling()

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        if (shuffleModeEnabled != PlayerHolder.isShuffling()) PlayerHolder.toggleShuffle()
    }
}
