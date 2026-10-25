package com.example.spoyt.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.spoyt.R
import com.example.spoyt.ui.nav.Screen

private data class Tab(val screen: Screen, val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Screen.Home, R.string.nav_home, Icons.Default.Home),
    Tab(Screen.Search, R.string.nav_search, Icons.Default.Search),
    Tab(Screen.Library, R.string.nav_library, Icons.Default.LibraryMusic)
)

/** Bottom navigation: Home / Search / Library. */
@Composable
fun SpoytBottomBar(
    currentRoute: String?,
    onSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.screen.route,
                onClick = { onSelected(tab.screen) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
