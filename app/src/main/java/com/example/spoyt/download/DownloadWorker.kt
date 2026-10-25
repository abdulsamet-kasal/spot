package com.example.spoyt.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.spoyt.di.AppContainer
import com.example.spoyt.player.DownloadPaths
import com.example.spoyt.player.PlayerHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

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
        val title = inputData.getString(KEY_TITLE).orEmpty()

        val downloads = AppContainer.downloads
        val youtube = AppContainer.youtube

        var target: File? = null
        var tmp: File? = null

        try {
            downloads.setState(videoId, STATE_DOWNLOADING)
            Downloads.setProgress(videoId, 0)

            val stream = youtube.resolveAudioStream(videoId)
                ?: throw IllegalStateException("stream çözümlenemedi")

            val ext = stream.fileExtension?.takeIf { it.isNotBlank() } ?: "m4a"
            val dir = File(applicationContext.filesDir, "downloads").apply { mkdirs() }
            val outFile = File(dir, "$videoId.$ext")
            val partFile = File(dir, "$videoId.part")
            target = outFile
            tmp = partFile

            val request = Request.Builder()
                .url(stream.url)
                .header("User-Agent", PlayerHolder.userAgentFor(stream.url))
                .header("Accept", "*/*")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("boş gövde")
                val total = body.contentLength()

                body.byteStream().use { input ->
                    partFile.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var downloaded = 0L
                        var lastReported = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                val percent = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                                if (percent != lastReported) {
                                    lastReported = percent
                                    Downloads.setProgress(videoId, percent)
                                }
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (partFile.length() <= 0L) throw IllegalStateException("boş dosya")

            if (outFile.exists()) outFile.delete()
            if (!partFile.renameTo(outFile)) throw IllegalStateException("dosya taşınamadı")

            downloads.markDone(videoId, outFile.absolutePath, outFile.length())
            DownloadPaths.set(videoId, outFile.absolutePath)
            Downloads.setProgress(videoId, 100)
            Downloads.postMessage("\"$title\" indirildi.")
            Result.success()
        } catch (t: Throwable) {
            runCatching {
                tmp?.delete()
                target?.takeIf { it.exists() && it.length() == 0L }?.delete()
            }
            runCatching { downloads.setState(videoId, STATE_FAILED) }
            Downloads.clearProgress(videoId)

            // Retry twice (expired URLs are fixed by re-resolving on the next attempt).
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Downloads.postMessage(
                    if (title.isBlank()) "İndirme başarısız oldu."
                    else "\"$title\" indirilemedi."
                )
                Result.failure()
            }
        } finally {
            Downloads.clearProgress(videoId)
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

        private const val MAX_RETRIES = 2
        private const val BUFFER_SIZE = 64 * 1024

        /**
         * A dedicated client: the default 10s read timeout is too tight for multi-megabyte
         * audio downloads on mobile networks.
         */
        private val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }
}
