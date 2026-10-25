package com.example.spoyt.player

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * One-shot messages the player wants to show on screen ("Bu şarkı oynatılamıyor", ...).
 *
 * The player layer has no Context and must not touch UI, so it posts text here and
 * [com.example.spoyt.MainActivity] renders it in a Snackbar. The text is already localized
 * (Turkish) because the player layer is the only place that knows *why* playback failed.
 */
object PlaybackEvents {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun post(message: String) {
        _messages.tryEmit(message)
    }
}
