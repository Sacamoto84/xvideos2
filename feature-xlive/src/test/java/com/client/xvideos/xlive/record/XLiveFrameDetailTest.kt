package com.client.xvideos.xlive.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Насколько кадр не однотонный: по нему выбирается картинка карточки записи. */
class XLiveFrameDetailTest {

    private fun gray(level: Int): Int = (0xFF shl 24) or (level shl 16) or (level shl 8) or level

    @Test
    fun `однотонный кадр — без деталей`() {
        assertEquals(0.0, frameDetail(IntArray(100) { gray(40) }), 1e-9)
    }

    @Test
    fun `чёрное с белым — много деталей`() {
        val pixels = IntArray(100) { if (it % 2 == 0) gray(0) else gray(255) }

        assertTrue(frameDetail(pixels) > FRAME_DETAIL_ENOUGH)
    }

    @Test
    fun `почти однотонный тёмный кадр — меньше порога`() {
        val pixels = IntArray(100) { gray(38 + it % 5) }

        assertTrue(frameDetail(pixels) < FRAME_DETAIL_ENOUGH)
    }
}
