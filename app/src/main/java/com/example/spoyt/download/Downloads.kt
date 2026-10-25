package com.example.spoyt.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.spoyt.data.local.entities.DownloadEntity
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.data.prefs.AppPrefs
import com.example.spoyt.di.AppContainer
import com.example.spoyt.player.DownloadPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Thin facade the UI calls to start/cancel downloads. Keeps WorkManager + Room bookkeeping
 * (QUEUED row first, then the worker flips it to DOWNLOADING / DONE / FAILED) in one place,
 * plus the live download progress that the UI shows as a percentage.
 */
object Downloads {

    private val crashGuard = kotlinx.coroutines.CoroutineExceptionHandler { _, t ->
        if (t !is kotlinx.coroutines.CancellationException) {
            postMessage("İndirme sırasında bir sorun oluştu.")
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + crashGuard)

    /** videoId -> 0..100 while a download is running. Removed when it finishes. */
    private val _progress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val progress: StateFlow<Map<String, Int>> = _progress.asStateFlow()

    /** One-shot user-facing messages (download finished / failed). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun setProgress(videoId: String, percent: Int) {
        _progress.update { it + (videoId to percent.coerceIn(0, 100)) }
    }

    fun clearProgress(videoId: String) {
        _progress.update { it - videoId }
    }

    fun postMessage(message: String) {
        _messages.tryEmit(message)
    }

    /** Enqueues a download for [track]. No-op if it is already queued or finished. */
    fun enqueue(context: Context, track: TrackEntity) {
        scope.launch {
            val existing = runCatching { AppContainer.downloads.getDownload(track.videoId) }.getOrNull()
            if (existing != null && existing.state in listOf(
                    DownloadWorker.STATE_QUEUED,
                    DownloadWorker.STATE_DOWNLOADING,
                    DownloadWorker.STATE_DONE
                )
            ) {
                // Re-download only when the previous attempt actually failed.
                return@launch
            }

            runCatching {
                AppContainer.downloads.insertOrUpdate(
                    DownloadEntity(
                        videoId = track.videoId,
                        filePath = "",
                        state = DownloadWorker.STATE_QUEUED
                    )
                )
            }

            val data = Data.Builder()
                .putString(DownloadWorker.KEY_VIDEO_ID, track.videoId)
                .putString(DownloadWorker.KEY_TITLE, track.title)
                .putString(DownloadWorker.KEY_ARTIST, track.artist)
                .build()

            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .addTag(DownloadWorker.UNIQUE_PREFIX + track.videoId)
                .apply {
                    // "Sadece Wi-Fi" preference gates the job at the WorkManager level, so a
                    // queued download simply waits (with no retries burned) until unmetered.
                    if (AppPrefs.wifiOnlyDownloads) {
                        setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.UNMETERED)
                                .build()
                        )
                    }
                }
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    DownloadWorker.UNIQUE_PREFIX + track.videoId,
                    ExistingWorkPolicy.REPLACE,
                    request
                )

            postMessage("\"${track.title}\" indirme sırasına alındı.")
        }
    }

    /** Deletes a downloaded file (if any) and its Room row. */
    fun delete(context: Context, videoId: String) {
        scope.launch {
            runCatching {
                WorkManager.getInstance(context)
                    .cancelUniqueWork(DownloadWorker.UNIQUE_PREFIX + videoId)
                val row = AppContainer.downloads.getDownload(videoId)
                if (row != null && row.filePath.isNotEmpty()) {
                    File(row.filePath).delete()
                }
                AppContainer.downloads.delete(videoId)
                DownloadPaths.remove(videoId)
                clearProgress(videoId)
            }
        }
    }

    /** Removes every downloaded file and its Room row (used by "clear cache" in Settings). */
    fun deleteAll(context: Context) {
        scope.launch {
            runCatching {
                val rows = AppContainer.downloads.getAllDownloads().first()
                rows.forEach { row ->
                    if (row.filePath.isNotEmpty()) File(row.filePath).delete()
                    DownloadPaths.remove(row.videoId)
                    AppContainer.downloads.delete(row.videoId)
                }
                File(context.filesDir, "downloads").deleteRecursively()
                _progress.value = emptyMap()
            }
        }
    }
}
