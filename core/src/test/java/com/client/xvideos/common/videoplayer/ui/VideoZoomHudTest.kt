package com.client.xvideos.common.videoplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoZoomHudTest {

    @Test
    fun `formatZoomLabel handles standard 100 percent scale`() {
        assertEquals("100%", formatZoomLabel(1.0f))
        assertEquals("100%", formatZoomLabel(1.01f))
        assertEquals("100%", formatZoomLabel(0.99f))
    }

    @Test
    fun `formatZoomLabel handles non-finite and negative scale`() {
        assertEquals("100%", formatZoomLabel(Float.NaN))
        assertEquals("100%", formatZoomLabel(Float.POSITIVE_INFINITY))
        assertEquals("100%", formatZoomLabel(-1.5f))
        assertEquals("100%", formatZoomLabel(0f))
    }

    @Test
    fun `formatZoomLabel detects fill scale match`() {
        val fillScale = 1.33f
        assertEquals("Во весь экран", formatZoomLabel(1.33f, fillScale))
        assertEquals("Во весь экран", formatZoomLabel(1.31f, fillScale))
        assertEquals("Во весь экран", formatZoomLabel(1.35f, fillScale))
    }

    @Test
    fun `formatZoomLabel handles non-finite fillScale gracefully`() {
        assertEquals("150%", formatZoomLabel(1.5f, Float.NaN))
        assertEquals("150%", formatZoomLabel(1.5f, Float.POSITIVE_INFINITY))
    }

    @Test
    fun `formatZoomLabel clamps extreme scale bounds`() {
        assertEquals("1000%", formatZoomLabel(50.0f))
        assertEquals("10%", formatZoomLabel(0.01f))
    }

    @Test
    fun `formatZoomLabel formats arbitrary percentage correctly`() {
        assertEquals("150%", formatZoomLabel(1.5f))
        assertEquals("200%", formatZoomLabel(2.0f))
        assertEquals("250%", formatZoomLabel(2.5f))
        assertEquals("300%", formatZoomLabel(3.0f))
    }

    @Test
    fun `isZoomActive detects whether scale is greater than threshold`() {
        assertFalse(isZoomActive(1.0f))
        assertFalse(isZoomActive(1.01f))
        assertFalse(isZoomActive(Float.NaN))
        assertFalse(isZoomActive(Float.POSITIVE_INFINITY))
        assertFalse(isZoomActive(-2.0f))

        assertTrue(isZoomActive(1.03f))
        assertTrue(isZoomActive(1.5f))
        assertTrue(isZoomActive(2.5f))
    }
}
