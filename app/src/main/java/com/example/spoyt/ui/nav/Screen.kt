package com.example.spoyt.ui.nav

/**
 * Destinations used by Navigation Compose.
 *
 * Routes are plain strings; [PlaylistDetail] carries the playlist id in the route itself
 * for simplicity (personal-use app, no deep links).
 */
sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Ana Sayfa")
    object Search : Screen("search", "Arama")
    object Library : Screen("library", "Kütüphane")
    object Player : Screen("player", "Oynatıcı")
    object Settings : Screen("settings", "Ayarlar")

    object PlaylistDetail {
        const val route = "playlist"
        const val argName = "playlistId"
        fun create(playlistId: Int) = "$route/$playlistId"
    }
}
