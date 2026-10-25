package com.example.spoyt.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.spoyt.ui.screens.HomeScreen
import com.example.spoyt.ui.screens.LibraryScreen
import com.example.spoyt.ui.screens.PlayerScreen
import com.example.spoyt.ui.screens.PlaylistDetailScreen
import com.example.spoyt.ui.screens.SearchScreen
import com.example.spoyt.ui.screens.SettingsScreen

@Composable
fun SpoytNavHost(
    navController: androidx.navigation.NavHostController,
    onOpenDrawer: () -> Unit = {}
) {
    // Expose the player navigation globally so ViewModels can trigger it.
    PlayerNavigationController.instance = navController

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenSearch = { navController.navigate(Screen.Search.route) },
                onOpenPlaylist = { id -> navController.navigate(Screen.PlaylistDetail.create(id)) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenPlayer = { navController.navigate(Screen.Player.route) },
                onOpenLibrary = {
                    navController.navigate(Screen.Library.route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onOpenPlayer = { navController.navigate(Screen.Player.route) },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onOpenPlaylist = { id -> navController.navigate(Screen.PlaylistDetail.create(id)) },
                onOpenPlayer = { navController.navigate(Screen.Player.route) },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable(
            route = "${Screen.PlaylistDetail.route}/{${Screen.PlaylistDetail.argName}}",
            arguments = listOf(navArgument(Screen.PlaylistDetail.argName) { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt(Screen.PlaylistDetail.argName) ?: 0
            PlaylistDetailScreen(
                playlistId = id,
                onBack = { navController.popBackStack() },
                onOpenPlayer = { navController.navigate(Screen.Player.route) }
            )
        }
        composable(Screen.Player.route) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * Allows ViewModels to navigate to the full player screen without holding Compose references.
 */
object PlayerNavigationController {
    var instance: androidx.navigation.NavHostController? = null

    fun navigateToPlayer() {
        val nav = instance ?: return
        if (nav.currentDestination?.route != Screen.Player.route) {
            nav.navigate(Screen.Player.route)
        }
    }
}
