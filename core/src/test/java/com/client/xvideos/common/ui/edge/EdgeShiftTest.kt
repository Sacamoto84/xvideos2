package com.client.xvideos.common.ui.edge

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Сдвиг элементов у правого верхнего края от камеры и скругления экрана.
 * На внутреннем экране складного камера в правом верхнем углу, а на экранах со
 * скруглёнными углами дуга срезает текст у края (монитор скорости).
 */
class EdgeShiftTest {

    // Вырез: координаты в системе блока 234x117 (правая группа кнопок L).

    @Test
    fun `камера над правой частью блока сдвигает его левее выреза`() {
        val camera = Rect(left = 97f, top = -11f, right = 233f, bottom = 125f)
        assertEquals(147, cutoutShiftLeft(listOf(camera), width = 234, height = 117, gap = 10))
    }

    @Test
    fun `камера левее блока не сдвигает его`() {
        val camera = Rect(left = -1362f, top = 9f, right = -1250f, bottom = 141f)
        assertEquals(0, cutoutShiftLeft(listOf(camera), width = 234, height = 117, gap = 10))
    }

    @Test
    fun `вырез ниже блока не учитывается`() {
        val camera = Rect(left = 97f, top = 1929f, right = 233f, bottom = 2065f)
        assertEquals(0, cutoutShiftLeft(listOf(camera), width = 234, height = 117, gap = 10))
    }

    @Test
    fun `вырез без значений не учитывается`() {
        val unknown = Rect(Float.NaN, Float.NaN, Float.NaN, Float.NaN)
        assertEquals(0, cutoutShiftLeft(listOf(unknown), width = 234, height = 117, gap = 10))
    }

    // Скругление: блок шириной 300, радиус 100.

    @Test
    fun `блок у самого верха упирается в центр дуги`() {
        // Верх блока на высоте верхней точки дуги: видно только левее центра.
        assertEquals(60, roundedCornerShiftLeft(width = 300, centerX = 250f, centerY = 100f, radius = 100f, gap = 10))
    }

    @Test
    fun `блок ниже по дуге сдвигается меньше`() {
        // dy = 60: дуга на высоте верха блока заканчивается на 250 + 80 = 330.
        assertEquals(0, roundedCornerShiftLeft(width = 300, centerX = 250f, centerY = 60f, radius = 100f, gap = 10))
        assertEquals(31, roundedCornerShiftLeft(width = 351, centerX = 250f, centerY = 60f, radius = 100f, gap = 10))
    }

    @Test
    fun `блок левее дуги или ниже её центра не сдвигается`() {
        assertEquals(0, roundedCornerShiftLeft(width = 200, centerX = 250f, centerY = 100f, radius = 100f, gap = 10))
        assertEquals(0, roundedCornerShiftLeft(width = 300, centerX = 250f, centerY = -5f, radius = 100f, gap = 10))
    }

    @Test
    fun `прямоугольный экран без скругления не сдвигает`() {
        assertEquals(0, roundedCornerShiftLeft(width = 300, centerX = 300f, centerY = 0f, radius = 0f, gap = 10))
        assertEquals(0, roundedCornerShiftLeft(width = 300, centerX = Float.NaN, centerY = Float.NaN, radius = Float.NaN, gap = 10))
    }
}
