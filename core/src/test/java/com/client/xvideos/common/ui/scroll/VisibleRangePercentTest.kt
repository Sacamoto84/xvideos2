package com.client.xvideos.common.ui.scroll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibleRangePercentTest {

    @Test
    fun `пустой список или нулевые колонки возвращают полный диапазон`() {
        val zeroCols = VisibleRangeMetrics(
            totalItemsCount = 10,
            itemsToIgnore = 0,
            numberOfColumns = 0,
            viewportStartOffset = 0,
            viewportEndOffset = 1000,
            hasVisibleItems = true,
            allVisibleIgnored = false,
            firstRelevantIndex = 0,
            firstItemTopEdge = 0f,
            firstItemHeight = 100f,
            lastIndex = 5,
            lastItemTopEdge = 400f,
            lastItemHeight = 100f,
        )
        assertEquals(0f to 1f, calculateVisibleRangePercent(zeroCols))

        val ignoredCoversAll = zeroCols.copy(totalItemsCount = 2, itemsToIgnore = 3, numberOfColumns = 2)
        assertEquals(0f to 1f, calculateVisibleRangePercent(ignoredCoversAll))
    }

    @Test
    fun `если видны только игнорируемые шапки, возвращается начальная доля`() {
        val headerOnly = VisibleRangeMetrics(
            totalItemsCount = 20,
            itemsToIgnore = 2,
            numberOfColumns = 2,
            viewportStartOffset = 0,
            viewportEndOffset = 500,
            hasVisibleItems = true,
            allVisibleIgnored = true,
            firstRelevantIndex = null,
            firstItemTopEdge = 0f,
            firstItemHeight = 0f,
            lastIndex = null,
            lastItemTopEdge = 0f,
            lastItemHeight = 0f,
        )
        val (start, end) = calculateVisibleRangePercent(headerOnly)
        assertEquals(0f, start, 1e-6f)
        assertTrue(end in 0.01f..0.5f)
    }

    @Test
    fun `при корректных индексах start и end лежат монотонно от 0 до 1`() {
        val metrics = VisibleRangeMetrics(
            totalItemsCount = 12,
            itemsToIgnore = 2,
            numberOfColumns = 2,
            viewportStartOffset = 0,
            viewportEndOffset = 300,
            hasVisibleItems = true,
            allVisibleIgnored = false,
            firstRelevantIndex = 4, // 2-й значимый элемент -> строка 1
            firstItemTopEdge = -50f, // половина элемента скрыта сверху
            firstItemHeight = 100f,
            lastIndex = 7, // строка 2
            lastItemTopEdge = 250f, // видна половина нижнего элемента (50 из 100)
            lastItemHeight = 100f,
        )
        val (start, end) = calculateVisibleRangePercent(metrics)
        // totalRelevantRows = 10 / 2 = 5
        // start = (1 + 0.5) / 5 = 0.30, end = (2 + 0.5) / 5 = 0.50
        assertEquals(0.30f, start, 1e-4f)
        assertEquals(0.50f, end, 1e-4f)
        assertTrue(start <= end)
    }
}
