package com.client.xvideos.common

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppBuildInfoTest {

    @Before
    @After
    fun reset() {
        AppBuildInfo.resetForTesting()
    }

    @Test
    fun `AppBuildInfo tracks initialization and flags correctly`() {
        assertFalse(AppBuildInfo.isInitialized)
        assertFalse(AppBuildInfo.debug)
        assertTrue(AppBuildInfo.isRelease)
        assertEquals("?", AppBuildInfo.versionName)

        AppBuildInfo.init(debug = true, versionName = "2.58.0")
        assertTrue(AppBuildInfo.isInitialized)
        assertTrue(AppBuildInfo.debug)
        assertFalse(AppBuildInfo.isRelease)
        assertEquals("2.58.0", AppBuildInfo.versionName)

        AppBuildInfo.resetForTesting()
        assertFalse(AppBuildInfo.isInitialized)
        assertFalse(AppBuildInfo.debug)
        assertTrue(AppBuildInfo.isRelease)
    }
}
