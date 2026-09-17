package com.client.xvideos.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ToPrettyCountTest {

    @Test
    fun `toPrettyCount formats numbers below 1000 without suffix`() {
        assertEquals("0", 0L.toPrettyCount())
        assertEquals("1", 1L.toPrettyCount())
        assertEquals("500", 500L.toPrettyCount())
        assertEquals("999", 999L.toPrettyCount())
    }

    @Test
    fun `toPrettyCount formats thousands with 1 decimal place`() {
        assertEquals("1.0K", 1_000L.toPrettyCount())
        assertEquals("1.2K", 1_200L.toPrettyCount())
        assertEquals("1.3K", 1_250L.toPrettyCount())
        assertEquals("68.5K", 68_500L.toPrettyCount())
    }

    @Test
    fun `toPrettyCount formats millions with 1 decimal place`() {
        assertEquals("1.0M", 1_000_000L.toPrettyCount())
        assertEquals("1.5M", 1_450_000L.toPrettyCount())
    }

    @Test
    fun `toPrettyCount formats billions with 1 decimal place`() {
        assertEquals("1.0B", 1_000_000_000L.toPrettyCount())
        assertEquals("2.5B", 2_500_000_000L.toPrettyCount())
    }

    @Test
    fun `toPrettyCount handles negative numbers via absolute value`() {
        assertEquals("500", (-500L).toPrettyCount())
        assertEquals("1.2K", (-1_200L).toPrettyCount())
        assertEquals("1.3K", (-1_250L).toPrettyCount())
    }

    @Test
    fun `toPrettyCount handles extreme bounds without 2s complement overflow`() {
        // abs(Long.MIN_VALUE) in 2's complement overflows back to Long.MIN_VALUE if unhandled
        val minResult = Long.MIN_VALUE.toPrettyCount()
        val maxResult = Long.MAX_VALUE.toPrettyCount()
        assertEquals(maxResult, minResult)
        assertEquals(Long.MAX_VALUE.toPrettyCount2(), Long.MIN_VALUE.toPrettyCount2())
        assertEquals(Long.MAX_VALUE.toPrettyCount3(), Long.MIN_VALUE.toPrettyCount3())
        assertEquals(Long.MAX_VALUE.toPrettyCountInt(), Long.MIN_VALUE.toPrettyCountInt())
    }

    @Test
    fun `toPrettyCount2 formats with 2 decimal places`() {
        assertEquals("0", 0L.toPrettyCount2())
        assertEquals("999", 999L.toPrettyCount2())
        assertEquals("1.25K", 1_250L.toPrettyCount2())
        assertEquals("1.45M", 1_450_000L.toPrettyCount2())
        assertEquals("2.50B", 2_500_000_000L.toPrettyCount2())
    }

    @Test
    fun `toPrettyCount3 formats with 3 decimal places`() {
        assertEquals("0", 0L.toPrettyCount3())
        assertEquals("999", 999L.toPrettyCount3())
        assertEquals("1.234K", 1_234L.toPrettyCount3())
        assertEquals("1.450M", 1_450_000L.toPrettyCount3())
        assertEquals("2.500B", 2_500_000_000L.toPrettyCount3())
    }

    @Test
    fun `toPrettyCountInt formats with 0 decimal places`() {
        assertEquals("0", 0L.toPrettyCountInt())
        assertEquals("999", 999L.toPrettyCountInt())
        assertEquals("1K", 1_250L.toPrettyCountInt())
        assertEquals("1M", 1_450_000L.toPrettyCountInt())
        assertEquals("3B", 2_500_000_000L.toPrettyCountInt())
    }
}
