package com.client.xvideos.x.screens.common.bottomKeyboard

import com.client.xvideos.common.ui.adaptive.WidthClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisiblePageNumbersTest {

    @Test
    fun `на телефоне пять номеров, на широком окне больше`() {
        assertEquals(5, visiblePageNumbers(WidthClass.Compact))
        assertEquals(9, visiblePageNumbers(WidthClass.Medium))
        assertEquals(11, visiblePageNumbers(WidthClass.Expanded))
    }

    @Test
    fun `число номеров нечётное - текущая страница встаёт по центру`() {
        WidthClass.entries.forEach { assertTrue(visiblePageNumbers(it) % 2 == 1) }
    }
}
