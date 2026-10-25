package com.example.spoyt.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** LRC format parsing used by the synced-lyrics view. */
class LrcParserTest {

    @Test
    fun `parses timestamped lines`() {
        val lrc = """
            [00:12.50] First line
            [01:02.250] Second line
        """.trimIndent()

        val lines = LrcParser.parse(lrc)

        assertEquals(2, lines.size)
        assertEquals(12_500L, lines[0].timeMs)
        assertEquals("First line", lines[0].text)
        assertEquals(62_250L, lines[1].timeMs)
        assertEquals("Second line", lines[1].text)
    }

    @Test
    fun `sorts lines by time even if file is unordered`() {
        val lrc = """
            [01:00.00] Later
            [00:30.00] Earlier
        """.trimIndent()

        val lines = LrcParser.parse(lrc)

        assertEquals("Earlier", lines.first().text)
        assertEquals("Later", lines.last().text)
    }

    @Test
    fun `skips metadata and empty lines`() {
        val lrc = """
            [ti:Some Song]
            [ar:Some Artist]
            [00:10.00] Real line
            [00:15.00]
        """.trimIndent()

        val lines = LrcParser.parse(lrc)

        assertEquals(1, lines.size)
        assertEquals("Real line", lines[0].text)
    }

    @Test
    fun `handles comma fraction separator`() {
        val lines = LrcParser.parse("[00:05,75] Comma")

        assertEquals(1, lines.size)
        assertEquals(5_750L, lines[0].timeMs)
    }

    @Test
    fun `returns empty list for garbage input`() {
        assertTrue(LrcParser.parse("").isEmpty())
        assertTrue(LrcParser.parse("just some lyrics text").isEmpty())
    }
}
