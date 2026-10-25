package com.example.spoyt.ui.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One-shot UI requests that come from outside the Compose tree (app shortcuts written by
 * [android.content.Intent] from the launcher). The activity writes, the composition consumes.
 *
 * A (serial, tab) pair is used so the same tab can be requested twice in a row: the serial
 * changes while the tab text may repeat.
 */
object UiRequests {

    private val _shortcut = MutableStateFlow(0L to (null as String?))
    val shortcut: StateFlow<Pair<Long, String?>> = _shortcut.asStateFlow()

    /** Requested Library tab index (0 liked, 1 downloads), -1 = none. */
    val libraryTab = MutableStateFlow(-1)

    fun requestShortcut(tab: String) {
        val (serial, _) = _shortcut.value
        _shortcut.value = (serial + 1) to tab
    }

    fun consume() {
        val (serial, _) = _shortcut.value
        _shortcut.value = serial to null
    }
}
