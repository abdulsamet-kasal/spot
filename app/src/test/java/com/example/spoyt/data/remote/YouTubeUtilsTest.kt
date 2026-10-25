package com.example.spoyt.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure string tests for [YouTubeUtils] — no NewPipe, no network, no Android runtime.
 */
class YouTubeUtilsTest {

    // ---------------------------------------------------------- extractVideoId

    @Test
    fun `extracts id from desktop watch url`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUtils.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        )
    }

    @Test
    fun `extracts id from music youtube url`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUtils.extractVideoId("https://music.youtube.com/watch?v=dQw4w9WgXcQ&si=abc")
        )
    }

    @Test
    fun `extracts id from short url with extra params`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUtils.extractVideoId("https://youtu.be/dQw4w9WgXcQ?t=42")
        )
    }

    @Test
    fun `extracts id from mobile url`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUtils.extractVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ")
        )
    }

    @Test
    fun `returns null for plain text queries`() {
        assertNull(YouTubeUtils.extractVideoId("pink floyd shine on"))
        assertNull(YouTubeUtils.extractVideoId(""))
        assertNull(YouTubeUtils.extractVideoId("   "))
    }

    @Test
    fun `returns null for non youtube links`() {
        assertNull(YouTubeUtils.extractVideoId("https://example.com/watch?v=abc"))
    }

    // ---------------------------------------------------------- looksLikeLink

    @Test
    fun `detects youtube links`() {
        assertTrue(YouTubeUtils.looksLikeLink("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(YouTubeUtils.looksLikeLink("  https://www.youtube.com/watch?v=x "))
    }

    @Test
    fun `rejects non links`() {
        assertFalse(YouTubeUtils.looksLikeLink("youtube.com daki şarkı"))
        assertFalse(YouTubeUtils.looksLikeLink("watch?v="))
        assertFalse(YouTubeUtils.looksLikeLink("ftp://youtu.be/x"))
    }

    // ---------------------------------------------------------- cleanArtistName

    @Test
    fun `strips topic suffix`() {
        assertEquals("Pink Floyd", YouTubeUtils.cleanArtistName("Pink Floyd - Topic"))
    }

    @Test
    fun `strips vevo suffix`() {
        assertEquals("Daft Punk", YouTubeUtils.cleanArtistName("Daft Punk - VEVO"))
    }

    @Test
    fun `keeps plain names`() {
        assertEquals("Sezen Aksu", YouTubeUtils.cleanArtistName("Sezen Aksu"))
    }

    @Test
    fun `falls back to original when everything is stripped`() {
        assertEquals("- -", YouTubeUtils.cleanArtistName("- -"))
    }

    // ---------------------------------------------------------- cleanTitleForLyrics

    @Test
    fun `removes official video markers`() {
        assertEquals("Wish You Were Here", YouTubeUtils.cleanTitleForLyrics("Wish You Were Here (Official Video)"))
        assertEquals("Take On Me", YouTubeUtils.cleanTitleForLyrics("Take On Me [Official Music Video] [4K]"))
    }

    @Test
    fun `keeps meaningful parentheses`() {
        // "(Live)" is filtered only when flagged as noise; regular words survive.
        assertEquals("Kırık Kalpler", YouTubeUtils.cleanTitleForLyrics("Kırık Kalpler"))
    }

    @Test
    fun `never returns empty`() {
        assertEquals("(?)", YouTubeUtils.cleanTitleForLyrics("(?)"))
    }
}
