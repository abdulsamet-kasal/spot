package com.example.spoyt.ui.components

/**
 * Thumbnail URLs.
 *
 * YouTube Music hands back small images (`mqdefault.jpg` is only 320x180, YouTube Music image
 * URLs often carry a `=w60-h60` size hint). Blowing those up to full-player size looks blurry,
 * so big surfaces request a high-resolution variant of the same picture instead.
 *
 * The transformation is pure string work: no extra network call, and Coil caches each variant.
 */
object Thumbnails {

    /** 1280x720 where available; [medium] is the safe fallback if YouTube has no maxres. */
    fun high(url: String?): String? = rewrite(url, "maxresdefault.jpg", 1080)

    /** 480x360 / ~544px: plenty for cards and carousels, safe for every video. */
    fun medium(url: String?): String? = rewrite(url, "hqdefault.jpg", 544)

    private fun rewrite(url: String?, youtubeFile: String, googleSize: Int): String? {
        val value = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        // i.ytimg.com/vi/<id>/<size>.jpg[?...] -> always request the requested size.
        if (value.contains("ytimg.com/vi/") || value.contains("youtube.com/vi/")) {
            val videoId = value.substringAfter("/vi/").substringBefore('/').substringBefore('?')
            if (videoId.isNotEmpty()) return "https://i.ytimg.com/vi/$videoId/$youtubeFile"
        }

        // lh3.googleusercontent.com/...=w60-h60-l90-rj -> same image at a bigger size.
        if (value.contains("googleusercontent.com") && value.contains('=')) {
            return value.substringBefore('=') + "=w$googleSize-h$googleSize-l90-rj"
        }

        return value
    }
}
