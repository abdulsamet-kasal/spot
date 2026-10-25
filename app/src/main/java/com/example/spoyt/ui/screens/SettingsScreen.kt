package com.example.spoyt.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spoyt.R
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

    LaunchedEffect(Unit) { viewModel.load(context) }

    LaunchedEffect(state.message) {
        // Messages are transient; nothing to do beyond rendering (Snackbar-less by design).
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ListItem(
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(state.versionName) }
            )
            HorizontalDivider()

            ListItem(
                leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.settings_cache_size)) },
                supportingContent = { Text(state.cacheSizeLabel) }
            )
            HorizontalDivider()

            ListItem(
                modifier = Modifier.fillMaxWidth(),
                leadingContent = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.settings_clear_cache)) },
                supportingContent = state.message?.let { { Text(it) } },
                trailingContent = {
                    TextButton(onClick = { showClearCacheDialog = true }) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            )
            HorizontalDivider()

            ListItem(
                modifier = Modifier.fillMaxWidth(),
                leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.settings_clear_history)) },
                trailingContent = {
                    TextButton(onClick = { showClearHistoryDialog = true }) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            )
            HorizontalDivider()

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Spoyt — kişisel kullanım içindir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.settings_clear_cache)) },
            text = { Text("Önbellekteki tüm dosyalar silinecek. Devam edilsin mi?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache(context)
                        showClearCacheDialog = false
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.settings_clear_history)) },
            text = { Text("Dinleme geçmişi silinecek. Devam edilsin mi?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryDialog = false
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
