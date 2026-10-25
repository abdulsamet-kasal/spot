package com.example.spoyt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spoyt.R
import com.example.spoyt.ui.components.SpoytBackground
import com.example.spoyt.ui.theme.SpoytGreen
import com.example.spoyt.ui.theme.SpoytViolet
import com.example.spoyt.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearDownloadsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load(context) }

    SpoytBackground(accent = SpoytViolet) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Group(title = stringResource(R.string.settings_storage)) {
                    SettingRow(
                        icon = Icons.Default.Storage,
                        title = stringResource(R.string.settings_downloads_size),
                        subtitle = stringResource(R.string.library_song_count, state.downloadsCount) +
                            " · " + state.downloadsSizeLabel
                    )
                    Divider()
                    SettingRow(
                        icon = Icons.Default.DeleteSweep,
                        title = stringResource(R.string.settings_clear_downloads),
                        subtitle = stringResource(R.string.settings_clear_downloads_desc),
                        actionLabel = stringResource(R.string.action_delete),
                        onAction = { showClearDownloadsDialog = true }
                    )
                    Divider()
                    SettingRow(
                        icon = Icons.Default.Storage,
                        title = stringResource(R.string.settings_cache_size),
                        subtitle = state.cacheSizeLabel
                    )
                    Divider()
                    SettingRow(
                        icon = Icons.Default.History,
                        title = stringResource(R.string.settings_clear_history),
                        actionLabel = stringResource(R.string.action_delete),
                        onAction = { showClearHistoryDialog = true }
                    )
                }

                Group(title = stringResource(R.string.settings_about)) {
                    SettingRow(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_version),
                        subtitle = state.versionName
                    )
                }

                state.message?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_legal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    ConfirmDialog(
        visible = showClearCacheDialog,
        title = stringResource(R.string.settings_clear_cache),
        text = "Önbellekteki tüm dosyalar silinecek. Devam edilsin mi?",
        onConfirm = { viewModel.clearCache(context); showClearCacheDialog = false },
        onDismiss = { showClearCacheDialog = false }
    )

    ConfirmDialog(
        visible = showClearHistoryDialog,
        title = stringResource(R.string.settings_clear_history),
        text = "Dinleme geçmişi silinecek. Devam edilsin mi?",
        onConfirm = { viewModel.clearHistory(); showClearHistoryDialog = false },
        onDismiss = { showClearHistoryDialog = false }
    )

    ConfirmDialog(
        visible = showClearDownloadsDialog,
        title = stringResource(R.string.settings_clear_downloads),
        text = "İndirilen tüm şarkılar cihazdan silinecek. Devam edilsin mi?",
        onConfirm = { viewModel.clearDownloads(context); showClearDownloadsDialog = false },
        onDismiss = { showClearDownloadsDialog = false }
    )
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 18.dp, bottom = 8.dp)
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            SpoytGreen.copy(alpha = 0.18f),
                            SpoytViolet.copy(alpha = 0.16f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp,
        modifier = Modifier.padding(start = 62.dp)
    )
}

@Composable
private fun ConfirmDialog(
    visible: Boolean,
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
