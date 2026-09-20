package com.client.xvideos.common.ui.keyboard

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardNumberTest {

    @Test
    fun `clampPageNumber clamps within valid range`() {
        assertEquals(5, clampPageNumber(5, 10))
        assertEquals(1, clampPageNumber(1, 10))
        assertEquals(10, clampPageNumber(10, 10))
    }

    @Test
    fun `clampPageNumber clamps out-of-bounds values`() {
        assertEquals(1, clampPageNumber(0, 10))
        assertEquals(1, clampPageNumber(-10, 10))
        assertEquals(10, clampPageNumber(25, 10))
    }

    @Test
    fun `clampPageNumber protects against zero or negative max`() {
        // Standard coerceIn(1, 0) throws IllegalArgumentException.
        // clampPageNumber should safely clamp to 1.
        assertEquals(1, clampPageNumber(0, 0))
        assertEquals(1, clampPageNumber(5, 0))
        assertEquals(1, clampPageNumber(5, -5))
    }

    @Test
    fun `addCharToTextField appends char and moves cursor to end`() {
        val initial = TextFieldValue("12", TextRange(2))
        val updated = addCharToTextField(initial, "3")
        assertEquals("123", updated.text)
        assertEquals(3, updated.selection.start)
        assertEquals(3, updated.selection.end)
    }

    @Test
    fun `addCharToTextField works with empty string`() {
        val initial = TextFieldValue("", TextRange(0))
        val updated = addCharToTextField(initial, "7")
        assertEquals("7", updated.text)
        assertEquals(1, updated.selection.start)
    }
}
