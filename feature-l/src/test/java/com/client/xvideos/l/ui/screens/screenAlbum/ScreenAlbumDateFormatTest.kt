package com.client.xvideos.l.ui.screens.screenAlbum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset

class ScreenAlbumDateFormatTest {

    @Test
    fun `formatEpochSeconds formats valid timestamp correctly in UTC`() {
        // 1780919842 -> 2026-06-08T11:57:22Z
        val result = formatEpochSeconds(1780919842.393262, ZoneOffset.UTC)
        assertNotNull(result)
        assertEquals("08.06.2026 11:57", result)
    }

    @Test
    fun `formatEpochSeconds returns null for zero timestamp`() {
        assertNull(formatEpochSeconds(0.0, ZoneOffset.UTC))
    }

    @Test
    fun `formatEpochSeconds returns null for negative timestamp`() {
        assertNull(formatEpochSeconds(-1.0, ZoneOffset.UTC))
        assertNull(formatEpochSeconds(-1000.0, ZoneOffset.UTC))
    }

    @Test
    fun `formatEpochSeconds returns null for NaN and infinite values`() {
        assertNull(formatEpochSeconds(Double.NaN, ZoneOffset.UTC))
        assertNull(formatEpochSeconds(Double.POSITIVE_INFINITY, ZoneOffset.UTC))
        assertNull(formatEpochSeconds(Double.NEGATIVE_INFINITY, ZoneOffset.UTC))
    }

    @Test
    fun `formatEpochSeconds handles extreme values safely without crashing`() {
        assertNull(formatEpochSeconds(Double.MAX_VALUE, ZoneOffset.UTC))
    }
}
