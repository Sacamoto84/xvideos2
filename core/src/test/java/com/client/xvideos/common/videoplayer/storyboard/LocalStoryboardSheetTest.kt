package com.client.xvideos.common.videoplayer.storyboard

import androidx.compose.ui.unit.IntOffset
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Сетка кадров скачанного ролика X собирается из самого файла: 10×10 клеток
 * 192×108, кадр `i` — начало `i`-го процента длины, кадр по центру клетки.
 */
class LocalStoryboardSheetTest {

    @Test
    fun `имя файла сетки`() {
        assertEquals("42.storyboard.jpg", localStoryboardFileName(42L))
    }

    @Test
    fun `время кадра — начало его процента`() {
        assertEquals(0L, storyboardFrameTimeUs(0, 600_000L))
        assertEquals(300_000_000L, storyboardFrameTimeUs(50, 600_000L))
        assertEquals(990_000L, storyboardFrameTimeUs(99, 1_000L))
        assertEquals(35_640_000_000L, storyboardFrameTimeUs(99, 36_000_000L)) // 10 часов — без переполнения
    }

    @Test
    fun `кадр по центру своей клетки`() {
        assertEquals(IntOffset(0, 0), storyboardCellOrigin(0, 192, 108))
        assertEquals(IntOffset(641, 108), storyboardCellOrigin(13, 61, 108)) // вертикальный ролик
        assertEquals(IntOffset(1728, 985), storyboardCellOrigin(99, 192, 81)) // широкий ролик
        assertEquals(IntOffset(576, 108), storyboardCellOrigin(13, 200, 120)) // больше клетки — не выходит за её угол
    }
}
