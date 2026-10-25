package com.example.spoyt

import com.example.spoyt.data.remote.NewPipeDownloader
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.File

/**
 * Manual diagnostic, NOT part of the app and NOT run by normal builds.
 *
 * Run with:  gradlew.bat :app:testDebugUnitTest --tests "*StreamDiagnosticsTest*"
 *
 * It performs the exact same NewPipe calls the app performs (search -> resolve audio
 * stream) and then checks whether the resulting URL is actually playable by issuing an
 * HTTP GET with a Range header, exactly like ExoPlayer does.
 *
 * Results are written to `diag.txt` in the project root so they can be inspected
 * without Gradle log configuration.
 */
class StreamDiagnosticsTest {

    private val log = StringBuilder()

    private fun line(text: String) {
        log.append(text).append('\n')
        println(text)
    }

    @Test
    fun diagnose() {
        val out = File("../diag.txt")

        try {
            val provider = NewPipe.getDownloader()
            if (provider == null) {
                NewPipe.init(NewPipeDownloader())
            } else {
                line("downloader already set: " + provider.javaClass.name)
            }
            line("NewPipe extractor version = " + NewPipe.getVersion())

            val service = NewPipe.getService("YouTube")
            line("service = " + service.serviceInfo.name)

            // ---- 1. SEARCH ----------------------------------------------------
            val query = "pink floyd another brick in the wall"
            val search = service.getSearchExtractor(query, listOf("music_songs"), "")
            search.fetchPage()
            val page = search.getInitialPage()
            line("search items = " + page.items.size)
            page.items.take(5).forEach { item ->
                line("  item: name=" + item.name + " | url=" + item.url)
            }

            // ---- 2. RESUMEN ---------------------------------------------------
            val items = page.items.filterIsInstance<StreamInfoItem>()
            if (items.isEmpty()) {
                line("!! no StreamInfoItem in search results")
                return
            }

            for (item in items.take(2)) {
                line("")
                line("=== stream: " + item.name)
                line("url = " + item.url)

                val extractor = service.getStreamExtractor(item.url)
                extractor.fetchPage()
                line("name = " + extractor.name)
                line("duration = " + extractor.length)
                line("audioStreams.count = " + extractor.audioStreams.size)
                extractor.audioStreams.forEach { s ->
                    line(
                        "  audio: itag=" + s.itagItem?.id +
                            " fmt=" + s.format?.name +
                            " mime=" + s.format?.mimeType +
                            " bitrate=" + s.averageBitrate +
                            " contentPrefix=" + s.content.take(90)
                    )
                }

                val best = extractor.audioStreams.maxByOrNull { it.averageBitrate }
                if (best == null) {
                    line("!! NO AUDIO STREAMS -> app shows 'cannot play'")
                    continue
                }

                // ---- 3. Is the URL actually playable? -------------------------
                val client = OkHttpClient.Builder().build()
                val ua = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
                for (withUa in listOf(false, true)) {
                    val req = Request.Builder()
                        .url(best.content)
                        .header("Range", "bytes=0-65535")
                        .apply { if (withUa) header("User-Agent", ua) }
                        .build()
                    try {
                        client.newCall(req).execute().use { resp ->
                            val body = resp.body
                            val head = body?.byteStream()?.let { ins ->
                                val buf = ByteArray(16)
                                val n = ins.read(buf)
                                if (n > 0) buf.copyOf(n).joinToString(" ") { b -> "%02x".format(b) } else "eof"
                            } ?: "null"
                            line(
                                "  GET range ua=" + withUa +
                                    " -> " + resp.code + " len=" + body?.contentLength() +
                                    " type=" + resp.header("Content-Type") +
                                    " firstBytes=" + head
                            )
                        }
                    } catch (e: Exception) {
                        line("  GET range ua=$withUa -> EXCEPTION " + e.message)
                    }
                }
            }
        } catch (t: Throwable) {
            line("!!! FATAL: " + t)
            t.printStackTrace()
            log.append(t.stackTraceToString())
        } finally {
            out.writeText(log.toString())
            println("written to " + out.absolutePath)
        }
    }
}
