package com.client.xvideos.common.ui.atom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerticalScrollbarTest {

    @Test
    fun `shouldDrawScrollbar не отображает скроллбар при полной высоте`() {
        assertFalse("Диапазон 0f..1f (полная высота / fallback) не должен отображаться", shouldDrawScrollbar(0f, 1f))
        assertFalse("Почти полная высота в пределах tolerance не должна отображаться", shouldDrawScrollbar(0f, 0.9995f))
    }

    @Test
    fun `shouldDrawScrollbar не отображает скроллбар при некорректном диапазоне`() {
        assertFalse("Равные границы (нулевая высота)", shouldDrawScrollbar(0.5f, 0.5f))
        assertFalse("Обратный диапазон", shouldDrawScrollbar(0.8f, 0.2f))
    }

    @Test
    fun `shouldDrawScrollbar отображает скроллбар при частичной видимости контента`() {
        assertTrue("Частичная видимость в начале списка", shouldDrawScrollbar(0f, 0.25f))
        assertTrue("Частичная видимость в середине списка", shouldDrawScrollbar(0.3f, 0.6f))
        assertTrue("Частичная видимость в конце списка", shouldDrawScrollbar(0.75f, 1f))
    }

    @Test
    fun `calculateScrollbarOffsetYAndHeight при нулевой высоте дорожки или span возвращает нули`() {
        assertEquals(0f to 0f, calculateScrollbarOffsetYAndHeight(0f, 0.2f, 0.5f))
        assertEquals(0f to 0f, calculateScrollbarOffsetYAndHeight(1000f, 0.2f, 0f))
        assertEquals(0f to 0f, calculateScrollbarOffsetYAndHeight(-100f, 0.2f, 0.5f))
    }

    @Test
    fun `calculateScrollbarOffsetYAndHeight в начале списка вычисляет правильные координаты`() {
        val (offsetY, height) = calculateScrollbarOffsetYAndHeight(1000f, 0f, 0.4f)
        assertEquals(0f, offsetY, 0.001f)
        assertEquals(400f, height, 0.001f)
    }

    @Test
    fun `calculateScrollbarOffsetYAndHeight в середине списка вычисляет правильные координаты`() {
        val (offsetY, height) = calculateScrollbarOffsetYAndHeight(1000f, 0.3f, 0.25f)
        assertEquals(300f, offsetY, 0.001f)
        assertEquals(250f, height, 0.001f)
    }

    @Test
    fun `calculateScrollbarOffsetYAndHeight в конце списка не допускает выхода за границы дорожки`() {
        val (offsetY, height) = calculateScrollbarOffsetYAndHeight(1000f, 0.85f, 0.3f)
        assertEquals(700f, offsetY, 0.001f)
        assertEquals(300f, height, 0.001f)
        assertTrue("Индикатор не выходит за нижний край дорожки", offsetY + height <= 1000f)
    }

    @Test
    fun `calculateScrollbarOffsetYAndHeight ограничивает входные доли в диапазоне от 0 до 1`() {
        val (offsetY, height) = calculateScrollbarOffsetYAndHeight(1000f, -0.5f, 1.5f)
        assertEquals(0f, offsetY, 0.001f)
        assertEquals(1000f, height, 0.001f)
    }
}
