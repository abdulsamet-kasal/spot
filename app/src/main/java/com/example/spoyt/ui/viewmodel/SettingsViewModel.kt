package com.example.spoyt.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spoyt.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val versionName: String = "",
    val cacheSizeLabel: String = "0 B",
    val message: String? = null
)

/**
 * Settings screen: app version and cache/queue clearing.
 */
class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun load(context: Context) {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
        _uiState.update { it.copy(versionName = version, cacheSizeLabel = formatSize(cacheBytes(context))) }
    }

    fun clearCache(context: Context) {
        val total = runCatching {
            val before = cacheBytes(context)
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
            before
        }.getOrDefault(0L)

        _uiState.update {
            it.copy(
                cacheSizeLabel = formatSize(cacheBytes(context)),
                message = "Önbellek temizlendi (${formatSize(total)})."
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            runCatching { AppContainer.history.clearHistory() }
            _uiState.update { it.copy(message = "Dinleme geçmişi temizlendi.") }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun cacheBytes(context: Context): Long {
        val internal = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val external = context.externalCacheDir?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
        return internal + external
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
