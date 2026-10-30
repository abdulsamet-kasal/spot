package com.example.spoyt.player

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager

/**
 * CPU and Wi-Fi keep-awake for background playback.
 *
 * ExoPlayer's own `WAKE_MODE_NETWORK` locks follow its state machine: both are released at
 * STATE_IDLE and STATE_ENDED, which is exactly the window where a track change happens and where
 * the next stream URL is fetched over the network. With the screen off the radio can sleep inside
 * that window and the following track never starts. These locks follow `playWhenReady` instead, so
 * they are held as long as the user wants music, independent of player state.
 */
@Suppress("DEPRECATION")
class PlaybackWakeLocks(context: Context) {

    private val app = context.applicationContext

    private val wakeLock: PowerManager.WakeLock? =
        (app.getSystemService(Context.POWER_SERVICE) as? PowerManager)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sametify:playback")
            ?.apply { setReferenceCounted(false) }

    private val wifiLock: WifiManager.WifiLock? =
        (app.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
            ?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "sametify:playback-wifi")
            ?.apply { setReferenceCounted(false) }

    /** True once a keep-awake request was seen, so transitions are logged only once each. */
    private var needed = false

    /** Acquires while [value] is true, releases otherwise. Safe to call repeatedly. */
    fun setNeeded(value: Boolean) {
        if (value == needed) return
        needed = value
        runCatching { if (value) wakeLock?.acquire(MAX_HOLD_MS) else wakeLock?.release() }
        runCatching { if (value) wifiLock?.acquire() else wifiLock?.release() }
        if (value) {
            val cpu = wakeLock?.isHeld == true
            val wifi = wifiLock?.isHeld == true
            PlaybackLog.line("keepAwake cpu=$cpu wifi=$wifi")
            if (!cpu || !wifi) {
                PlaybackEvents.post(
                    if (!cpu) "Cihaz uykuya geçiyor: ekran kapalıyken müzik kesilebilir."
                    else "Wi-Fi uykuya geçiyor: ekran kapalıyken akış kesilebilir."
                )
            }
        }
    }

    fun release() = setNeeded(false)

    private companion object {
        /** A leaked lock must not drain the battery: the player re-requests it whenever it plays. */
        const val MAX_HOLD_MS = 6L * 60 * 60 * 1000
    }
}
