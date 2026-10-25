package com.example.spoyt.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Stream URL expiry bookkeeping (YouTube CDN links die after ~6h; we allow 90 minutes). */
class ResolvedStreamTest {

    private fun stream(resolvedAt: Long) = ResolvedStream(
        url = "https://cdn.example.com/audio",
        mimeType = "audio/mp4",
        fileExtension = "m4a",
        resolvedAtMs = resolvedAt
    )

    @Test
    fun `fresh stream is not expired`() {
        val now = 1_000_000L
        assertFalse(stream(resolvedAt = now).isExpired(now = now))
        // 30 minutes later it is still fine.
        assertFalse(stream(resolvedAt = now).isExpired(now = now + 30L * 60_000))
    }

    @Test
    fun `stream older than max age is expired`() {
        val now = 1_000_000L
        // 91 minutes later -> expired (limit is 90).
        assertTrue(stream(resolvedAt = now).isExpired(now = now + ResolvedStream.MAX_AGE_MS + 60_000))
    }

    @Test
    fun `boundary is exclusive`() {
        val now = 1_000_000L
        // Exactly at the limit: not yet expired (>) — a minute before: fresh.
        assertFalse(stream(resolvedAt = now).isExpired(now = now + ResolvedStream.MAX_AGE_MS))
        assertTrue(
            stream(resolvedAt = now).isExpired(now = now + ResolvedStream.MAX_AGE_MS + 1)
        )
    }
}
