package com.client.xvideos.common.ui.adaptive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AdaptiveRulesTest {

    @Test
    fun `границы классов ширины - 600 и 840dp`() {
        assertEquals(WidthClass.Compact, widthClassOf(0))
        assertEquals(WidthClass.Compact, widthClassOf(599))
        assertEquals(WidthClass.Medium, widthClassOf(600))
        assertEquals(WidthClass.Medium, widthClassOf(839))
        assertEquals(WidthClass.Expanded, widthClassOf(840))
        assertEquals(WidthClass.Expanded, widthClassOf(883))
    }

    @Test
    fun `жёсткие сетки - 2, 3 и 4 колонки`() {
        assertEquals(2, gridColumns(WidthClass.Compact))
        assertEquals(3, gridColumns(WidthClass.Medium))
        assertEquals(4, gridColumns(WidthClass.Expanded))
    }

    @Test
    fun `пользовательский выбор плюс запас широкого окна`() {
        // R выбирает 2..4, L — из разрешённых в настройках, крайние 1 и 5.
        listOf(1, 2, 3, 4, 5).forEach { selected ->
            assertEquals(selected, userGridColumns(selected, WidthClass.Compact))
            assertEquals(selected + 1, userGridColumns(selected, WidthClass.Medium))
            assertEquals(selected + 2, userGridColumns(selected, WidthClass.Expanded))
        }
    }

    @Test
    fun `видимых карточек горизонтального ряда - 3, 4 и 5`() {
        assertEquals(3, rowVisibleItems(WidthClass.Compact))
        assertEquals(4, rowVisibleItems(WidthClass.Medium))
        assertEquals(5, rowVisibleItems(WidthClass.Expanded))
    }

    @Test
    fun `боковой отступ появляется только шире лимита`() {
        assertEquals(0, contentSidePaddingDp(411))
        assertEquals(0, contentSidePaddingDp(640))
        assertEquals(0, contentSidePaddingDp(641))
        assertEquals(30, contentSidePaddingDp(700))
        assertEquals(106, contentSidePaddingDp(852))
    }

    @Test
    fun `пустые места добивают неполную строку секции, но не на телефоне`() {
        // Телефон, 3 в строке: прежняя раскладка, даже если строка неполная.
        assertEquals(0, sectionRowFillers(itemCount = 9, itemsPerRow = 3))
        assertEquals(0, sectionRowFillers(itemCount = 7, itemsPerRow = 3))
        // Широкое окно: последняя строка добивается до полной.
        assertEquals(1, sectionRowFillers(itemCount = 9, itemsPerRow = 5))
        assertEquals(3, sectionRowFillers(itemCount = 12, itemsPerRow = 5))
        assertEquals(3, sectionRowFillers(itemCount = 9, itemsPerRow = 4))
        assertEquals(0, sectionRowFillers(itemCount = 10, itemsPerRow = 5))
        assertEquals(0, sectionRowFillers(itemCount = 0, itemsPerRow = 5))
    }

    @Test
    fun `узкий лимит действует только вне Compact`() {
        // Форма замка 420dp: на широком телефоне (окно ~500dp, Compact) она
        // обязана остаться прежней, а не сузиться.
        assertEquals(null, capOutsideCompactDp(WidthClass.Compact, 420))
        assertEquals(420, capOutsideCompactDp(WidthClass.Medium, 420))
        assertEquals(420, capOutsideCompactDp(WidthClass.Expanded, 420))
    }

    @Test
    fun `смещение по центру не бывает отрицательным`() {
        assertEquals(200, centeredOffsetPx(availablePx = 1000, contentPx = 600))
        assertEquals(0, centeredOffsetPx(availablePx = 600, contentPx = 600))
        assertEquals(0, centeredOffsetPx(availablePx = 500, contentPx = 600))
    }

    @Test
    fun `пока родитель не шире лимита, ограничения не меняются`() {
        assertNull(contentWidthConstraints(Constraints(maxWidth = 640), capPx = 640))
        assertNull(contentWidthConstraints(Constraints(minWidth = 500, maxWidth = 500), capPx = 640))
        // Неограниченная ширина (горизонтальный скролл): ограничивать нечего.
        assertNull(contentWidthConstraints(Constraints(), capPx = 640))
    }

    @Test
    fun `шире лимита - содержимое не шире лимита, высота прежняя`() {
        assertEquals(
            Constraints(minWidth = 640, maxWidth = 640, minHeight = 10, maxHeight = 2000),
            contentWidthConstraints(
                Constraints(minWidth = 1000, maxWidth = 1000, minHeight = 10, maxHeight = 2000),
                capPx = 640,
            ),
        )
        // Нестрогая ширина остаётся нестрогой.
        assertEquals(
            Constraints(minWidth = 0, maxWidth = 640),
            contentWidthConstraints(Constraints(maxWidth = 1000), capPx = 640),
        )
    }

    @Test
    fun `модификатор с тем же лимитом равен себе`() {
        // Равенство даёт Compose пропускать обновление узла при перекомпоновке.
        assertEquals(Modifier.adaptiveContentWidth(), Modifier.adaptiveContentWidth())
        assertEquals(Modifier.adaptiveContentWidth(420.dp), Modifier.adaptiveContentWidth(420.dp))
        assertNotEquals(Modifier.adaptiveContentWidth(420.dp), Modifier.adaptiveContentWidth())
    }

    @Test
    fun `нулевой боковой отступ возвращает тот же PaddingValues`() {
        val base = PaddingValues(top = 8.dp)
        assertSame(base, base.plusSides(0.dp))
    }

    @Test
    fun `боковой отступ прибавляется слева и справа, верх и низ прежние`() {
        val padded = PaddingValues(start = 4.dp, top = 8.dp, end = 6.dp, bottom = 10.dp).plusSides(30.dp)
        assertEquals(34.dp, padded.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(36.dp, padded.calculateRightPadding(LayoutDirection.Ltr))
        assertEquals(8.dp, padded.calculateTopPadding())
        assertEquals(10.dp, padded.calculateBottomPadding())
    }
}
