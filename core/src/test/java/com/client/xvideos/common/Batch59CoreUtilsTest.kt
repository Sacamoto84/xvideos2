package com.client.xvideos.common

import com.client.xvideos.common.util.capitalizeFirstWord
import com.client.xvideos.common.util.capitalizeFirstWordOrEmpty
import com.client.xvideos.common.util.formatAsBytes
import com.client.xvideos.common.util.formatAsSpeed
import com.client.xvideos.common.util.formatBytesOrDefault
import com.client.xvideos.common.util.formatSpeedOrDefault
import com.client.xvideos.common.util.isValidMD5
import com.client.xvideos.common.util.toMD5
import com.client.xvideos.common.util.toMD5OrDefault
import com.client.xvideos.common.util.toMinSecOrDefault
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.common.util.toTwoDecimalPlacesWithColon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch59CoreUtilsTest {

    @Test
    fun `formatBytes overloads and formatBytesOrDefault`() {
        assertEquals("0 B", Double.NaN.formatAsBytes())
        assertEquals("0 B", Double.POSITIVE_INFINITY.formatAsBytes())
        assertEquals("0 B", (-10.5).formatAsBytes())
        assertEquals("0 B", 0.0.formatAsBytes())
        assertEquals("1 KB", 1024.0.formatAsBytes())
        assertEquals("1.0 MB", (1024.0 * 1024.0).formatAsBytes())

        assertEquals("0 B", Float.NaN.formatAsBytes())
        assertEquals("0 B", Float.NEGATIVE_INFINITY.formatAsBytes())
        assertEquals("0 B", (-1.0f).formatAsBytes())
        assertEquals("500 B", 500.0f.formatAsBytes())

        assertEquals("0 B", formatBytesOrDefault(null))
        assertEquals("N/A", formatBytesOrDefault(null, default = "N/A"))
        assertEquals("2 KB", formatBytesOrDefault(2048L))
    }

    @Test
    fun `formatSpeed overloads and formatSpeedOrDefault`() {
        assertEquals("0 Bs", 0.formatAsSpeed())
        assertEquals("1 KBs", 1024.formatAsSpeed())

        assertEquals("0 Bs", Double.NaN.formatAsSpeed())
        assertEquals("0 Bs", Double.NEGATIVE_INFINITY.formatAsSpeed())
        assertEquals("0 Bs", (-5.0).formatAsSpeed())
        assertEquals("5.0 MBs", (5.0 * 1024.0 * 1024.0).formatAsSpeed())

        assertEquals("0 Bs", formatSpeedOrDefault(null))
        assertEquals("Idle", formatSpeedOrDefault(null, default = "Idle"))
        assertEquals("512 Bs", formatSpeedOrDefault(512L))
    }

    @Test
    fun `toMinSecOrDefault handles strings and fallbacks`() {
        assertEquals("01:08", "68.7".toMinSecOrDefault())
        assertEquals("02:14", "134.0".toMinSecOrDefault())
        assertEquals("00:00", "".toMinSecOrDefault())
        assertEquals("00:00", null.toMinSecOrDefault())
        assertEquals("--:--", "invalid-duration".toMinSecOrDefault(default = "--:--"))
    }

    @Test
    fun `toPrettyCount floating-point overloads`() {
        assertEquals("0", Double.NaN.toPrettyCount())
        assertEquals("0", Double.POSITIVE_INFINITY.toPrettyCount())
        assertEquals("1.5K", 1500.0.toPrettyCount())

        assertEquals("0", Float.NaN.toPrettyCount())
        assertEquals("0", Float.NEGATIVE_INFINITY.toPrettyCount())
        assertEquals("2.5M", 2_500_000f.toPrettyCount())
    }

    @Test
    fun `toMD5OrDefault and isValidMD5 validation`() {
        val emptyMd5 = "".toMD5()
        assertEquals(emptyMd5, null.toMD5OrDefault())
        assertEquals("custom_fallback", null.toMD5OrDefault(default = "custom_fallback"))
        assertEquals("5d41402abc4b2a76b9719d911017c592", "hello".toMD5OrDefault())

        assertTrue("5d41402abc4b2a76b9719d911017c592".isValidMD5())
        assertTrue("5D41402ABC4B2A76B9719D911017C592".isValidMD5())
        assertFalse("5d41402abc4b2a76b9719d911017c59".isValidMD5()) // 31 chars
        assertFalse("5d41402abc4b2a76b9719d911017c5922".isValidMD5()) // 33 chars
        assertFalse("5d41402abc4b2a76b9719d911017c59g".isValidMD5()) // non-hex char 'g'
        assertFalse("".isValidMD5())
        assertFalse((null as String?).isValidMD5())
    }

    @Test
    fun `capitalizeFirstWord and capitalizeFirstWordOrEmpty`() {
        assertEquals("Hello world", "hello world".capitalizeFirstWord())
        assertEquals("WORLD", "WORLD".capitalizeFirstWord())
        assertEquals("", "".capitalizeFirstWord())

        assertEquals("Hello world", "hello world".capitalizeFirstWordOrEmpty())
        assertEquals("", null.capitalizeFirstWordOrEmpty())
        assertEquals("", "".capitalizeFirstWordOrEmpty())
        assertEquals("", "   ".capitalizeFirstWordOrEmpty())
    }

    @Test
    fun `toTwoDecimalPlacesWithColon integer and long overloads`() {
        assertEquals("0:00", 0.toTwoDecimalPlacesWithColon())
        assertEquals("0:00", (-5).toTwoDecimalPlacesWithColon())
        assertEquals("5:00", 5.toTwoDecimalPlacesWithColon())
        assertEquals("120:00", 120.toTwoDecimalPlacesWithColon())

        assertEquals("0:00", 0L.toTwoDecimalPlacesWithColon())
        assertEquals("0:00", (-10L).toTwoDecimalPlacesWithColon())
        assertEquals("42:00", 42L.toTwoDecimalPlacesWithColon())
    }
}
