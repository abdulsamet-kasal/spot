package com.example.spoyt.download

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.spoyt.data.local.entities.DownloadEntity
import com.example.spoyt.data.local.entities.TrackEntity
import com.example.spoyt.di.AppContainer
import com.example.spoyt.player.DownloadPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Thin facade the UI calls to start/cancel downloads. Keeps WorkManager + Room bookkeeping
 * (QUEUED row first, then the worker flips it to DOWNLOADING / DONE / FAILED) in one place.
 */
object Downloads {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

            val data = androidx.work.Data.Builder()
                .putString(DownloadWorker.KEY_VIDEO_ID, track.videoId)
                .putString(DownloadWorker.KEY_TITLE, track.title)
                .putString(DownloadWorker.KEY_ARTIST, track.artist)
                .build()

            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .addTag(DownloadWorker.UNIQUE_PREFIX + track.videoId)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    DownloadWorker.UNIQUE_PREFIX + track.videoId,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }
    }

    /** Deletes a downloaded file (if any) and its Room row. */
    fun delete(context: Context, videoId: String) {
        scope.launch {
            runCatching {
                val row = AppContainer.downloads.getDownload(videoId)
                if (row != null && row.filePath.isNotEmpty()) {
                    File(row.filePath).delete()
                }
                AppContainer.downloads.delete(videoId)
                DownloadPaths.remove(videoId)
            }
        }
    }
}
