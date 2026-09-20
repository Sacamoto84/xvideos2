package com.client.xvideos.x

import com.client.xvideos.x.screens.videoplayer.atom.formatTime
import org.junit.Assert.assertEquals
import org.junit.Test

class X_PlayerBottomBarTest {

    @Test
    fun `formatTime форматирует отрицательные секунды как 0 00`() {
        assertEquals("0:00", formatTime(-10))
    }

    @Test
    fun `formatTime форматирует ноль как 0 00`() {
        assertEquals("0:00", formatTime(0))
    }

    @Test
    fun `formatTime форматирует секунды меньше минуты с лидирующим нулем`() {
        assertEquals("0:05", formatTime(5))
        assertEquals("0:59", formatTime(59))
    }

    @Test
    fun `formatTime форматирует минуты и секунды`() {
        assertEquals("1:00", formatTime(60))
        assertEquals("2:05", formatTime(125))
        assertEquals("59:59", formatTime(3599))
    }

    @Test
    fun `formatTime форматирует часы для видео дольше часа`() {
        assertEquals("1:00:00", formatTime(3600))
        assertEquals("1:01:01", formatTime(3661))
        assertEquals("10:00:00", formatTime(36000))
    }

    @Test
    fun `formatTime корректно формирует текст возобновления воспроизведения`() {
        val shortResume = "Возобновлено с ${formatTime(125)}"
        assertEquals("Возобновлено с 2:05", shortResume)

        val longResume = "Возобновлено с ${formatTime(3665)}"
        assertEquals("Возобновлено с 1:01:05", longResume)
    }
}
