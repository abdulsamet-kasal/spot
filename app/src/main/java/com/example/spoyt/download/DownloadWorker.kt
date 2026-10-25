package com.example.spoyt.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.spoyt.di.AppContainer
import com.example.spoyt.player.DownloadPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Downloads a single track's audio stream to app-specific internal storage and records the
 * outcome in the Room `downloads` table (QUEUED -> DOWNLOADING -> DONE / FAILED).
 *
 * The stream URL is resolved at execution time (URLs expire, so they must not be persisted).
 */
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val videoId = inputData.getString(KEY_VIDEO_ID) ?: return@withContext Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: ""
        val artist = inputData.getString(KEY_ARTIST) ?: ""

        val downloads = AppContainer.downloads
        val youtube = AppContainer.youtube

        try {
            downloads.setState(videoId, STATE_DOWNLOADING)

            val stream = youtube.resolveAudioStream(videoId)
                ?: throw IllegalStateException("stream cozulemedi")

            val ext = stream.fileExtension?.takeIf { it.isNotBlank() } ?: "m4a"
            val dir = File(applicationContext.filesDir, "downloads").apply { mkdirs() }
            val target = File(dir, "$videoId.$ext")
            val tmp = File(dir, "$videoId.part")

            val client = OkHttpClient.Builder().build()
            val request = Request.Builder()
                .url(stream.url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
                val body = response.body ?: throw IllegalStateException("bos govde")
                body.byteStream().use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
            }

            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) throw IllegalStateException("dosya tasinamadi")

            val size = target.length()
            downloads.markDone(videoId, target.absolutePath, size)
            DownloadPaths.set(videoId, target.absolutePath)
            Result.success()
        } catch (t: Throwable) {
            runCatching { downloads.setState(videoId, STATE_FAILED) }
            // Keep the partial file out of the way.
            runCatching {
                File(applicationContext.filesDir, "downloads/$videoId.part").delete()
            }
            // Retry once; then give up and leave the row in FAILED state.
            if (runAttemptCount < 1) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_VIDEO_ID = "videoId"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"

        const val STATE_QUEUED = "QUEUED"
        const val STATE_DOWNLOADING = "DOWNLOADING"
        const val STATE_DONE = "DONE"
        const val STATE_FAILED = "FAILED"

        const val UNIQUE_PREFIX = "download-"
    }
}
