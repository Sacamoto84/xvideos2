package com.client.xvideos.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingUtilsTest {

    @Test
    fun `toMinSec formats durations correctly`() {
        assertEquals("00:00", 0.0.toMinSec())
        assertEquals("00:09", 9.2.toMinSec())
        assertEquals("01:08", 68.7.toMinSec())
        assertEquals("02:14", 134.0.toMinSec())
        assertEquals("10:00", 600.0.toMinSec())
    }

    @Test
    fun `toMinSec handles edge cases gracefully`() {
        assertEquals("00:00", (-5.0).toMinSec())
        assertEquals("00:00", Double.NaN.toMinSec())
        assertEquals("00:00", (-1.0f).toMinSec())
        assertEquals("01:00", 60.0f.toMinSec())
    }

    @Test
    fun `formatBytes formats byte sizes correctly`() {
        assertEquals("0 B", formatBytes(-10L))
        assertEquals("0 B", formatBytes(0L))
        assertEquals("500 B", formatBytes(500L))
        assertEquals("1 KB", formatBytes(1024L))
        assertEquals("2 KB", formatBytes(2048L))
        assertEquals("1.0 MB", formatBytes(1024L * 1024L))
        assertEquals("1.5 MB", formatBytes((1.5 * 1024L * 1024L).toLong()))
        assertEquals("1.0 GB", formatBytes(1024L * 1024L * 1024L))
        assertEquals("2.5 GB", formatBytes((2.5 * 1024L * 1024L * 1024L).toLong()))
    }

    @Test
    fun `formatSpeed formats transfer speeds correctly`() {
        assertEquals("0 Bs", formatSpeed(-1L))
        assertEquals("0 Bs", formatSpeed(0L))
        assertEquals("512 Bs", formatSpeed(512L))
        assertEquals("1 KBs", formatSpeed(1024L))
        assertEquals("5.0 MBs", formatSpeed(5 * 1024L * 1024L))
        assertEquals("1.2 GBs", formatSpeed((1.2 * 1024L * 1024L * 1024L).toLong()))
    }

    @Test
    fun `toTwoDecimalPlacesWithColon formats floats with colon separator`() {
        assertEquals("2:00", 2.0f.toTwoDecimalPlacesWithColon())
        assertEquals("2:98", 2.984f.toTwoDecimalPlacesWithColon())
        assertEquals("10:00", 10.0f.toTwoDecimalPlacesWithColon())
        assertEquals("123:46", 123.456f.toTwoDecimalPlacesWithColon())
        assertEquals("0:00", Float.NaN.toTwoDecimalPlacesWithColon())
        assertEquals("0:00", Float.POSITIVE_INFINITY.toTwoDecimalPlacesWithColon())
    }

    @Test
    fun `toMD5 calculates deterministic hex hashes including unicode`() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", "".toMD5())
        assertEquals("900150983cd24fb0d6963f7d28e17f72", "abc".toMD5())
        assertEquals("5d41402abc4b2a76b9719d911017c592", "hello".toMD5())
        assertEquals("ebb5e89e8a94e9dd22abf5d915d112b2", "тест".toMD5())
    }
}

