package com.client.xvideos.common.videoplayer.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Свайп по видео — длинное горизонтальное движение одного пальца: вправо —
 * вперёд, влево — назад. Короткое движение остаётся тапом, диагональное и
 * вертикальное не перематывают.
 */
class SwipeSeekTest {

    @Test
    fun `свайп — длинное горизонтальное движение`() {
        assertEquals(1, swipeSeekDirection(dx = 100f, dy = 10f, minDistancePx = 48f))
        assertEquals(-1, swipeSeekDirection(dx = -100f, dy = -10f, minDistancePx = 48f))
    }

    @Test
    fun `короткое, диагональное и вертикальное движение — не свайп`() {
        assertEquals(0, swipeSeekDirection(dx = 30f, dy = 0f, minDistancePx = 48f))
        assertEquals(0, swipeSeekDirection(dx = 100f, dy = 60f, minDistancePx = 48f))
        assertEquals(0, swipeSeekDirection(dx = 0f, dy = 200f, minDistancePx = 48f))
    }
}
