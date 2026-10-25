package com.example.spoyt.data.remote

import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

/**
 * NewPipe [Downloader] implementation backed by OkHttp.
 *
 * NewPipe Extractor calls [execute] to fetch pages/resources. We issue a standard browser-like
 * User-Agent so YouTube does not immediately block the request as a bot.
 */
class NewPipeDownloader : Downloader() {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override fun execute(request: Request): Response {
        val okRequest = OkRequest.Builder()
            .url(request.url())

        val method = request.httpMethod()
        val data = request.dataToSend()
        if (data != null) {
            okRequest.method(method, data.toRequestBody("application/octet-stream".toMediaType()))
        } else {
            okRequest.method(method, null)
        }

        // Apply headers from NewPipe's request (e.g. Accept-Language, cookies, and the
        // client-specific User-Agent of the player client being used). `.header()` replaces
        // so no duplicate User-Agent headers can end up on the wire.
        request.headers().forEach { (name, values) ->
            values.forEach { value ->
                okRequest.header(name, value)
            }
        }

        // Fall back to a standard browser-like User-Agent only when NewPipe did not set
        // one: the extractor sends the YouTube-app user agent for its Android/iOS player
        // clients, and overriding it makes those requests fail (streams get cut short).
        val hasUserAgent = request.headers().keys.any { it.equals("User-Agent", ignoreCase = true) }
        if (!hasUserAgent) {
            okRequest.header("User-Agent", BROWSER_USER_AGENT)
        }

        return try {
            val response = client.newCall(okRequest.build()).execute()
            val bodyString = response.body?.string() ?: ""
            Response(
                response.code,
                response.message,
                toHeaderMap(response.headers),
                bodyString,
                response.request.url.toString()
            )
        } catch (e: IOException) {
            throw IOException("NewPipeDownloader request failed: ${e.message}", e)
        }
    }

    private fun toHeaderMap(headers: okhttp3.Headers): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableList<String>>()
        headers.names().forEach { name ->
            map[name] = headers.values(name).toMutableList()
        }
        return map.mapValues { it.value.toList() }
    }

    companion object {
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7 Build/TD1A.220804.031; wv) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.6099.144 Mobile Safari/537.36"
    }
}
