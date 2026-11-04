package com.client.xvideos.common.ui.atom

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
}
