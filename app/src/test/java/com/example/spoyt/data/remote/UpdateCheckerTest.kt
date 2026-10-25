package com.example.spoyt.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** GitHub release tag comparison for the in-app update check. */
class UpdateCheckerTest {

    @Test
    fun `detects newer patch version`() {
        assertTrue(UpdateChecker.isNewer("v1.4.1", "1.4.0"))
    }

    @Test
    fun `detects newer minor and major versions`() {
        assertTrue(UpdateChecker.isNewer("v1.5.0", "1.4.0"))
        assertTrue(UpdateChecker.isNewer("v2.0.0", "1.9.9"))
    }

    @Test
    fun `same version is not newer`() {
        assertFalse(UpdateChecker.isNewer("v1.4.0", "1.4.0"))
        assertFalse(UpdateChecker.isNewer("1.4.0", "v1.4.0"))
    }

    @Test
    fun `older version is not newer`() {
        assertFalse(UpdateChecker.isNewer("v1.3.9", "1.4.0"))
    }

    @Test
    fun `handles different component counts`() {
        assertTrue(UpdateChecker.isNewer("v1.5", "1.4.0"))
        assertFalse(UpdateChecker.isNewer("v1", "1.0.1"))
        assertTrue(UpdateChecker.isNewer("v1.0.0.1", "1.0.0"))
    }
}
