package com.client.xvideos.calculator

import com.client.xvideos.common.ui.adaptive.WidthClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeypadWidthTest {

    @Test
    fun `на телефоне ограничения нет`() {
        assertNull(keypadWidthDp(WidthClass.Compact, availableWidthDp = 379, availableHeightDp = 800))
    }

    @Test
    fun `широкое окно с запасом по высоте - 420dp`() {
        assertEquals(420, keypadWidthDp(WidthClass.Expanded, availableWidthDp = 820, availableHeightDp = 828))
        assertEquals(420, keypadWidthDp(WidthClass.Medium, availableWidthDp = 668, availableHeightDp = 816))
    }

    @Test
    fun `широкое низкое окно - ширина, при которой пять рядов влезают по высоте`() {
        val height = 676
        val width = requireNotNull(keypadWidthDp(WidthClass.Expanded, availableWidthDp = 808, availableHeightDp = height))
        assertEquals(400, width)
        val button = (width - 3 * KEYPAD_SPACING_DP) / 4
        assertTrue(5 * (button + KEYPAD_SPACING_DP) <= height - DISPLAY_RESERVE_DP)
    }

    @Test
    fun `не шире доступной ширины и не отрицательная`() {
        assertEquals(380, keypadWidthDp(WidthClass.Medium, availableWidthDp = 380, availableHeightDp = 900))
        assertEquals(0, keypadWidthDp(WidthClass.Medium, availableWidthDp = 600, availableHeightDp = 100))
    }
}
