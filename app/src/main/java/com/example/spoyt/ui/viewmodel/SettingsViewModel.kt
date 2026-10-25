package com.example.spoyt.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spoyt.data.local.entities.PlaylistEntity
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.data.prefs.AppPrefs
import com.example.spoyt.data.remote.UpdateChecker
import com.example.spoyt.di.AppContainer
import com.example.spoyt.download.Downloads
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SettingsUiState(
    val versionName: String = "",
    val cacheSizeLabel: String = "0 B",
    val downloadsSizeLabel: String = "0 B",
    val downloadsCount: Int = 0,
    val message: String? = null,
    val wifiOnlyDownloads: Boolean = false,
    /** Non-null when a newer GitHub release exists: "v1.5.0". */
    val updateAvailable: String? = null,
    val hasCrashLog: Boolean = false
)

/**
 * Settings screen: app version, storage figures, download preferences, backup/restore,
 * the update check and the destructive actions.
 */
class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var currentVersion = "1.0.0"

    fun load(context: Context) {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
        currentVersion = version
        _uiState.update {
            it.copy(
                versionName = version,
                cacheSizeLabel = formatSize(cacheBytes(context)),
                wifiOnlyDownloads = AppPrefs.wifiOnlyDownloads,
                hasCrashLog = crashLogFile(context).exists()
            )
        }
        refreshDownloads()
    }

    /** Reads the downloads table so the storage card is accurate after every change. */
    fun refreshDownloads() {
        viewModelScope.launch {
            runCatching {
                val rows = AppContainer.downloads.getAllDownloads().first()
                val bytes = rows.mapNotNull { row ->
                    row.filePath.takeIf { it.isNotEmpty() }?.let { File(it).length() }
                }.sum()
                _uiState.update {
                    it.copy(
                        downloadsSizeLabel = formatSize(bytes),
                        downloadsCount = rows.count { row -> row.state == "DONE" }
                    )
                }
            }
        }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        AppPrefs.wifiOnlyDownloads = enabled
        _uiState.update { it.copy(wifiOnlyDownloads = enabled) }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            val latest = withContext(Dispatchers.IO) {
                runCatching { UpdateChecker.latestVersion() }.getOrNull()
            }
            if (latest == null) {
                _uiState.update { it.copy(message = "Güncelleme kontrol edilemedi.") }
            } else if (UpdateChecker.isNewer(latest, currentVersion)) {
                _uiState.update { it.copy(updateAvailable = latest) }
            } else {
                _uiState.update {
                    it.copy(message = "Güncel sürümü kullanıyorsun ($currentVersion).")
                }
            }
        }
    }

    // ------------------------------------------------------------ backup / restore

    /** Writes tracks + playlists + list membership as one JSON file to [uri]. */
    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val tracks = AppContainer.tracks.getAllTracks().first()
                val playlists = AppContainer.playlists.getAllPlaylists().first()
                val refs = AppContainer.playlistTracks.getAllCrossRefs()

                val json = JSONObject().apply {
                    put("app", "spoyt")
                    put("version", 1)
                    put(
                        "tracks",
                        JSONArray().apply {
                            tracks.forEach { t ->
                                put(
                                    JSONObject().apply {
                                        put("videoId", t.videoId)
                                        put("title", t.title)
                                        put("artist", t.artist)
                                        put("durationSeconds", t.durationSeconds)
                                        put(
                                            "thumbnailUrl",
                                            t.thumbnailUrl ?: JSONObject.NULL
                                        )
                                        put("liked", t.liked)
                                        put("addedAt", t.addedAt)
                                    }
                                )
                            }
                        }
                    )
                    put(
                        "playlists",
                        JSONArray().apply {
                            playlists.forEach { p ->
                                put(
                                    JSONObject().apply {
                                        put("id", p.id)
                                        put("name", p.name)
                                        put("createdAt", p.createdAt)
                                    }
                                )
                            }
                        }
                    )
                    put(
                        "playlistTracks",
                        JSONArray().apply {
                            refs.forEach { r ->
                                put(
                                    JSONObject().apply {
                                        put("playlistId", r.playlistId)
                                        put("videoId", r.videoId)
                                        put("position", r.position)
                                    }
                                )
                            }
                        }
                    )
                }

                val bytes = json.toString(2).toByteArray(Charsets.UTF_8)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                } ?: throw IllegalStateException("dosya açılamadı")
            }.onSuccess {
                _uiState.update { it.copy(message = "Yedek kaydedildi.") }
            }.onFailure {
                _uiState.update { it.copy(message = "Yedekleme başarısız oldu.") }
            }
        }
    }

    /** Restores a backup written by [exportBackup]; existing rows are never overwritten. */
    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: throw IllegalStateException("dosya okunamadı")

                val json = JSONObject(text)
                val tracksJson = json.optJSONArray("tracks") ?: JSONArray()
                val playlistsJson = json.optJSONArray("playlists") ?: JSONArray()
                val refsJson = json.optJSONArray("playlistTracks") ?: JSONArray()

                var trackCount = 0
                for (i in 0 until tracksJson.length()) {
                    val t = tracksJson.getJSONObject(i)
                    val exists = AppContainer.tracks.trackExists(t.getString("videoId"))
                    if (!exists) {
                        AppContainer.tracks.saveTrack(
                            TrackEntity(
                                videoId = t.getString("videoId"),
                                title = t.optString("title", ""),
                                artist = t.optString("artist", ""),
                                durationSeconds = t.optLong("durationSeconds", 0),
                                thumbnailUrl = t.optString("thumbnailUrl", "").ifEmpty { null },
                                liked = t.optBoolean("liked", false),
                                addedAt = t.optLong("addedAt", System.currentTimeMillis())
                            )
                        )
                        trackCount++
                    }
                }

                // Remap old playlist ids to the freshly created ones (createPlaylist returns Long).
                val idMap = HashMap<Int, Long>()
                var playlistCount = 0
                for (i in 0 until playlistsJson.length()) {
                    val p = playlistsJson.getJSONObject(i)
                    val oldId = p.getInt("id")
                    val newId = AppContainer.playlists.createPlaylist(
                        p.optString("name", "Yedek listesi ${i + 1}")
                    )
                    idMap[oldId] = newId
                    playlistCount++
                }

                var refCount = 0
                for (i in 0 until refsJson.length()) {
                    val r = refsJson.getJSONObject(i)
                    val oldId = r.getInt("playlistId")
                    val newId = idMap[oldId] ?: continue
                    AppContainer.playlistTracks.addTrack(
                        newId.toInt(),
                        r.getString("videoId"),
                        r.optInt("position", i)
                    )
                    refCount++
                }

                Triple(trackCount, playlistCount, refCount)
            }.onSuccess { (tracks, playlists, refs) ->
                _uiState.update {
                    it.copy(
                        message = "Yedek geri yüklendi: $tracks şarkı, $playlists liste, " +
                            "$refs liste şarkısı."
                    )
                }
                refreshDownloads()
            }.onFailure {
                _uiState.update { it.copy(message = "Geri yükleme başarısız oldu.") }
            }
        }
    }

    /** Copies the crash log into the clipboard so the user can paste it somewhere. */
    fun copyCrashLog(context: Context) {
        val file = crashLogFile(context)
        if (!file.exists()) {
            _uiState.update { it.copy(message = "Hata kaydı yok.") }
            return
        }
        runCatching {
            val text = file.readText().takeLast(16_000)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Spoyt hata kaydı", text))
            _uiState.update { it.copy(message = "Hata kaydı panoya kopyalandı.") }
        }
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

    fun clearDownloads(context: Context) {
        viewModelScope.launch {
            runCatching { Downloads.deleteAll(context) }
            _uiState.update { it.copy(message = "İndirilenler silindi.") }
            refreshDownloads()
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

    private fun crashLogFile(context: Context): File = File(context.filesDir, "crash.log")

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
