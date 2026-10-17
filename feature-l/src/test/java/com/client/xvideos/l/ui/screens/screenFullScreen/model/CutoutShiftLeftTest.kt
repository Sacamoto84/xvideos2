package com.client.xvideos.l.ui.screens.screenFullScreen.model

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Сдвиг правой группы кнопок полноэкранного просмотра L от выреза камеры.
 * На внутреннем экране складного камера в правом верхнем углу, и кнопка
 * «три точки» ложилась прямо на неё. Координаты — в системе группы 234x117.
 */
class CutoutShiftLeftTest {

    @Test
    fun `камера над правой частью группы сдвигает её левее выреза`() {
        val camera = Rect(left = 97f, top = -11f, right = 233f, bottom = 125f)
        assertEquals(147, cutoutShiftLeft(listOf(camera), width = 234, height = 117, gap = 10))
    }

    @Test
    fun `камера левее группы не сдвигает её`() {
        val camera = Rect(left = -1362f, top = 9f, right = -1250f, bottom = 141f)
        assertEquals(0, cutoutShiftLeft(listOf(camera), width = 234, height = 117, gap = 10))
    }

    @Test
    fun `вырез ниже группы не учитывается`() {
        val camera = Rect(left = 97f, top = 1929f, right = 233f, bottom = 2065f)
        assertEquals(0, cutoutShiftLeft(listOf(camera), width = 234, height = 117, gap = 10))
    }

    @Test
    fun `вырез без значений не учитывается`() {
        val unknown = Rect(Float.NaN, Float.NaN, Float.NaN, Float.NaN)
        assertEquals(0, cutoutShiftLeft(listOf(unknown), width = 234, height = 117, gap = 10))
    }
}
