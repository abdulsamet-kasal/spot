package com.example.spoyt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.spoyt.R
import com.example.spoyt.ui.nav.Screen

private data class Tab(val screen: Screen, val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Screen.Home, R.string.nav_home, Icons.Default.Home),
    Tab(Screen.Search, R.string.nav_search, Icons.Default.Search),
    Tab(Screen.Library, R.string.nav_library, Icons.Default.LibraryMusic)
)

/**
 * Bottom navigation built as a floating pill bar: the selected tab gets a tinted icon chip,
 * the label fades in only for the active tab so the bar stays quiet.
 */
@Composable
fun SpoytBottomBar(
    currentRoute: String?,
    onSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
        tonalElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    NavTab(
                        tab = tab,
                        selected = currentRoute == tab.screen.route,
                        onClick = { if (currentRoute != tab.screen.route) onSelected(tab.screen) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTab(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val chipSize by animateDpAsState(if (selected) 44.dp else 40.dp, label = "nav-chip")
    val iconAlpha by animateFloatAsState(if (selected) 1f else 0.65f, label = "nav-alpha")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(chipSize)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = stringResource(tab.labelRes),
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = iconAlpha),
                modifier = Modifier.size(22.dp)
            )
        }
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = stringResource(tab.labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
