package com.example.spoyt.player

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sleep timer: pauses playback after the chosen countdown elapses. Ticking happens on a
 * coroutine (no Android CountDownTimer, no leaks) and never throws — the timer is a nice-to-have,
 * so any failure just cancels it silently.
 */
object SleepTimer {

    private val crashGuard = CoroutineExceptionHandler { _, _ -> }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + crashGuard)

    private var job: Job? = null

    /** Milliseconds left, 0 when idle. */
    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    val isActive: Boolean
        get() = _remainingMs.value > 0

    /** Starts (or restarts) the countdown. Zero/negative cancels. */
    fun start(minutes: Int) {
        cancel()
        if (minutes <= 0) return
        job = scope.launch {
            try {
                val endAt = System.currentTimeMillis() + minutes * 60_000L
                while (isActive) {
                    val remaining = endAt - System.currentTimeMillis()
                    if (remaining <= 0) break
                    _remainingMs.value = remaining
                    delay(1_000)
                }
                _remainingMs.value = 0
                PlayerHolder.pause()
                PlaybackEvents.post("Uyku zamanlayıcısı tamamlandı, oynatma durduruldu.")
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (t: Throwable) {
                _remainingMs.value = 0
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = 0
    }
}
