package com.client.xvideos.x.screens.tags

import com.client.xvideos.x.screens.tags.molecule.tagListIndexAfterColumnsChange
import org.junit.Assert.assertEquals
import org.junit.Test

class TagListIndexAfterColumnsChangeTest {

    @Test
    fun `раскрытие - ряд пересчитывается через индекс видео`() {
        // Шапка + ряд 5 по 2 видео: первое видео ряда — 10-е; по 4 в ряду это ряд 2.
        assertEquals(1 + 2, tagListIndexAfterColumnsChange(firstVisibleIndex = 1 + 5, headerCount = 1, oldPerRow = 2, newPerRow = 4))
    }

    @Test
    fun `сложение - обратный пересчёт`() {
        // Ряд 2 по 4: видео 8; по 2 в ряду это ряд 4.
        assertEquals(1 + 4, tagListIndexAfterColumnsChange(firstVisibleIndex = 1 + 2, headerCount = 1, oldPerRow = 4, newPerRow = 2))
    }

    @Test
    fun `без шапки`() {
        assertEquals(3, tagListIndexAfterColumnsChange(firstVisibleIndex = 4, headerCount = 0, oldPerRow = 3, newPerRow = 4))
    }

    @Test
    fun `видна шапка - позиция не меняется`() {
        assertEquals(0, tagListIndexAfterColumnsChange(firstVisibleIndex = 0, headerCount = 1, oldPerRow = 2, newPerRow = 4))
    }

    @Test
    fun `число колонок прежнее - позиция прежняя`() {
        assertEquals(7, tagListIndexAfterColumnsChange(firstVisibleIndex = 7, headerCount = 1, oldPerRow = 3, newPerRow = 3))
    }
}
