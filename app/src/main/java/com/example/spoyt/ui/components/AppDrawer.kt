package com.example.spoyt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.spoyt.R
import com.example.spoyt.data.local.entities.PlaylistEntity
import com.example.spoyt.ui.theme.SpoytGreen
import com.example.spoyt.ui.theme.SpoytViolet

/**
 * Hamburger menu content: primary navigation, the user's playlists and settings.
 *
 * Kept as a pure composable (no navigation types) so it can be used from the drawer in
 * [com.example.spoyt.MainActivity] and stays easy to preview/test.
 */
@Composable
fun SpoytDrawer(
    currentRoute: String?,
    playlists: List<PlaylistEntity>,
    onClose: () -> Unit,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    onPlaylist: (Int) -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
    val appName = stringResource(R.string.app_name)

    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        DrawerHeader()

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
        ) {
            item {
                DrawerItem(
                    icon = Icons.Default.Home,
                    label = "Ana Sayfa",
                    selected = currentRoute == "home",
                    onClick = { onClose(); onHome() }
                )
            }
            item {
                DrawerItem(
                    icon = Icons.Default.Search,
                    label = "Ara",
                    selected = currentRoute == "search",
                    onClick = { onClose(); onSearch() }
                )
            }
            item {
                DrawerItem(
                    icon = Icons.Default.LibraryMusic,
                    label = "Kütüphane",
                    selected = currentRoute == "library",
                    onClick = { onClose(); onLibrary() }
                )
            }
            item {
                DrawerItem(
                    icon = Icons.Default.Favorite,
                    label = "Beğenilenler",
                    selected = false,
                    onClick = { onClose(); onLibrary() }
                )
            }

            item { DrawerSectionTitle("Çalma listeleri") }

            if (playlists.isEmpty()) {
                item {
                    Text(
                        text = "Henüz çalma listesi yok",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 26.dp, vertical = 4.dp)
                    )
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    DrawerItem(
                        icon = Icons.Default.QueueMusic,
                        label = playlist.name,
                        selected = false,
                        onClick = { onClose(); onPlaylist(playlist.id) }
                    )
                }
            }

            item { DrawerSectionTitle("Daha fazla") }

            item {
                DrawerItem(
                    icon = Icons.Default.GetApp,
                    label = "İndirilenler",
                    selected = false,
                    onClick = { onClose(); onLibrary() }
                )
            }
            item {
                DrawerItem(
                    icon = Icons.Default.Settings,
                    label = "Ayarlar",
                    selected = currentRoute == "settings",
                    onClick = { onClose(); onSettings() }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Text(
            text = if (versionName.isBlank()) appName else "$appName  ·  v$versionName",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(SpoytViolet.copy(alpha = 0.85f), SpoytGreen.copy(alpha = 0.75f))
                )
            )
            .padding(horizontal = 22.dp, vertical = 26.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sametify_mark),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Text(
                text = "Müziğin, senin ritminde.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 26.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
