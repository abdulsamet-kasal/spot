package com.example.spoyt

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.spoyt.di.AppContainer
import com.example.spoyt.download.Downloads
import com.example.spoyt.player.PlaybackEvents
import com.example.spoyt.ui.components.MiniPlayer
import com.example.spoyt.ui.components.SpoytBottomBar
import com.example.spoyt.ui.nav.PlayerNavigationController
import com.example.spoyt.ui.nav.Screen
import com.example.spoyt.ui.nav.SpoytNavHost
import com.example.spoyt.ui.theme.SpoytTheme
import com.example.spoyt.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.init(applicationContext)
        setContent {
            SpoytTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpoytApp()
                }
            }
        }
    }
}

@Composable
private fun SpoytApp() {
    val navController: NavHostController = rememberNavController()
    PlayerNavigationController.instance = navController

    RequestNotificationPermission()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val playerViewModel: PlayerViewModel = viewModel()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()

    // Playback and download layers report user-facing failures as text; surface them here.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        launch {
            PlaybackEvents.messages.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }
        launch {
            Downloads.messages.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    // The full player has its own layout; hide the bottom bar + mini player there.
    val isPlayerRoute = currentRoute == Screen.Player.route
    val showChrome = !isPlayerRoute && currentRoute != null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showChrome) {
                Column {
                    if (playerState.track != null) {
                        MiniPlayer(
                            state = playerState,
                            onTogglePlay = playerViewModel::playPause,
                            onNext = playerViewModel::next,
                            onExpand = { navController.navigate(Screen.Player.route) }
                        )
                    }
                    SpoytBottomBar(
                        currentRoute = currentRoute,
                        onSelected = { screen ->
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            SpoytNavHost(navController = navController)
        }
    }
}

/**
 * Android 13+ requires an explicit runtime grant before the media notification can be shown.
 * Older versions get it automatically from the manifest.
 */
@Composable
private fun RequestNotificationPermission() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* no-op: playback still works without the notification */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
