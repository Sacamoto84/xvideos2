package com.client.xvideos.common.videoplayer.util

import com.client.xvideos.common.videoplayer.extension.formatMinSec
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatMinSecTest {

    @Test
    fun `zero seconds formats to double zero minute and second`() {
        assertEquals("00:00", formatMinSec(0))
        assertEquals("00:00", 0.formatMinSec())
    }

    @Test
    fun `negative seconds are coerced to zero`() {
        assertEquals("00:00", formatMinSec(-1))
        assertEquals("00:00", formatMinSec(-3600))
        assertEquals("00:00", (-50).formatMinSec())
    }

    @Test
    fun `seconds under one minute format with leading zeros`() {
        assertEquals("00:05", formatMinSec(5))
        assertEquals("00:30", formatMinSec(30))
        assertEquals("00:59", formatMinSec(59))
    }

    @Test
    fun `minutes under one hour format without hours component`() {
        assertEquals("01:00", formatMinSec(60))
        assertEquals("01:05", formatMinSec(65))
        assertEquals("10:00", formatMinSec(600))
        assertEquals("59:59", formatMinSec(3599))
    }

    @Test
    fun `one hour or more formats with three components`() {
        assertEquals("01:00:00", formatMinSec(3600))
        assertEquals("01:01:01", formatMinSec(3661))
        assertEquals("02:30:15", formatMinSec(9015))
        assertEquals("10:00:00", formatMinSec(36000))
        assertEquals("100:00:00", formatMinSec(360000))
    }
}
