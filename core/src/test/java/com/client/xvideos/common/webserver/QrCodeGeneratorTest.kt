package com.client.xvideos.common.webserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeGeneratorTest {

    @Test
    fun `generateMatrix returns valid BitMatrix for valid URL`() {
        val matrix = QrCodeGenerator.generateMatrix("http://192.168.1.55:8080", sizePx = 256)
        assertNotNull(matrix)
        assertEquals(256, matrix!!.width)
        assertEquals(256, matrix.height)

        // Ensure there is at least one dark pixel
        var hasDarkPixel = false
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) {
                    hasDarkPixel = true
                    break
                }
            }
            if (hasDarkPixel) break
        }
        assertTrue(hasDarkPixel)
    }

    @Test
    fun `generateMatrix returns null for blank content`() {
        assertNull(QrCodeGenerator.generateMatrix(""))
        assertNull(QrCodeGenerator.generateMatrix("   "))
    }
}
