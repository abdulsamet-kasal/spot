package com.example.spoyt.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.spoyt.data.remote.UpdateChecker
import com.example.spoyt.ui.components.SpoytBackground
import com.example.spoyt.ui.theme.SpoytGreen
import com.example.spoyt.ui.theme.SpoytViolet
import com.example.spoyt.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

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

    // Backup files go through the system file picker (SAF): no storage permissions needed.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { viewModel.exportBackup(context, it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.importBackup(context, it) } }

    LaunchedEffect(Unit) {
        viewModel.load(context)
        // Quiet update check while the user is on this screen; failures are silent.
        viewModel.checkForUpdates()
    }

    // Show each status message for a few seconds, then clear it.
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(3_500)
            viewModel.consumeMessage()
        }
    }

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

                Group(title = stringResource(R.string.settings_backup_group)) {
                    SettingRow(
                        icon = Icons.Default.Backup,
                        title = stringResource(R.string.settings_backup),
                        subtitle = stringResource(R.string.settings_backup_desc),
                        actionLabel = stringResource(R.string.action_save),
                        onAction = { runCatching { exportLauncher.launch("spoyt-yedek.json") } }
                    )
                    Divider()
                    SettingRow(
                        icon = Icons.Default.Restore,
                        title = stringResource(R.string.settings_restore),
                        subtitle = stringResource(R.string.settings_restore_desc),
                        actionLabel = stringResource(R.string.action_open),
                        onAction = { runCatching { importLauncher.launch(arrayOf("application/json")) } }
                    )
                }

                Group(title = stringResource(R.string.settings_update_group)) {
                    val updateAvailable = state.updateAvailable
                    if (updateAvailable != null) {
                        SettingRow(
                            icon = Icons.Default.SystemUpdate,
                            title = stringResource(R.string.settings_update_available, updateAvailable),
                            actionLabel = stringResource(R.string.settings_update_open),
                            onAction = {
                                runCatching {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(UpdateChecker.RELEASES_URL))
                                    context.startActivity(intent)
                                }
                            }
                        )
                        Divider()
                    }
                    SettingRow(
                        icon = Icons.Default.SystemUpdate,
                        title = stringResource(R.string.settings_update_check),
                        subtitle = stringResource(R.string.settings_version) + " " + state.versionName,
                        actionLabel = stringResource(R.string.action_refresh),
                        onAction = { viewModel.checkForUpdates() }
                    )
                }

                Group(title = stringResource(R.string.settings_download)) {
                    SettingRow(
                        icon = Icons.Default.Wifi,
                        title = stringResource(R.string.settings_wifi_only),
                        subtitle = stringResource(R.string.settings_wifi_only_desc),
                        trailing = {
                            Switch(
                                checked = state.wifiOnlyDownloads,
                                onCheckedChange = { viewModel.setWifiOnlyDownloads(it) },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    )
                    Divider()
                    // A new package (or an OEM reset) starts without the exemption, so show the
                    // live state instead of a static hint.
                    val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE)
                        as? android.os.PowerManager
                    val batteryExempt =
                        powerManager?.isIgnoringBatteryOptimizations(context.packageName) != false
                    SettingRow(
                        icon = Icons.Default.Power,
                        title = "Pil optimizasyonu",
                        subtitle = if (batteryExempt) {
                            "Hariç tutulmuş — ekran kapalıyken çalma için gerekli"
                        } else {
                            "Ekran kapalıyken müzik kesilmesin: Sametify'ı pil optimizasyonundan hariç tutun"
                        },
                        actionLabel = "Ayar",
                        onAction = { openBatteryOptimizationSettings(context) }
                    )
                }

                if (state.hasCrashLog) {
                    Group(title = stringResource(R.string.settings_diagnostics)) {
                        SettingRow(
                            icon = Icons.Default.BugReport,
                            title = stringResource(R.string.settings_crash_log),
                            subtitle = stringResource(R.string.settings_crash_log_desc),
                            actionLabel = stringResource(R.string.action_copy),
                            onAction = { viewModel.copyCrashLog(context) }
                        )
                    }
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
    onAction: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
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
        if (trailing != null) {
            trailing()
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

/**
 * Asks the user to exempt Spoyt from battery optimization.
 *
 * Foreground playback plus the player's Wi-Fi wake lock normally suffice, but aggressive
 * OEM power savers (Xiaomi, Samsung, Huawei, ...) still kill background media services.
 * This opens the system dialog that whitelists the app; if that dialog is unavailable the
 * generic battery-optimization list is opened instead.
 */
private fun openBatteryOptimizationSettings(context: android.content.Context) {
    val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE)
        as? android.os.PowerManager
    if (powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true) return
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")
            )
        )
    }.onFailure {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
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
