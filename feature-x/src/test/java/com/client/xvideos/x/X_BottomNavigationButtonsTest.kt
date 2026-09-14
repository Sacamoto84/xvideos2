package com.client.xvideos.x

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class X_BottomNavigationButtonsTest {

    private fun computeNavigation(value: Int, max: Int): Pair<Boolean, Boolean> {
        val safeMax = max.coerceAtLeast(1)
        val maxPageIndex = safeMax - 1
        val canGoBack = value > 0
        val canGoForward = value < maxPageIndex
        return canGoBack to canGoForward
    }

    private fun nextPageIndex(value: Int, max: Int): Int {
        val safeMax = max.coerceAtLeast(1)
        val maxPageIndex = safeMax - 1
        return (value + 1).coerceIn(0, maxPageIndex)
    }

    private fun prevPageIndex(value: Int, max: Int): Int {
        val safeMax = max.coerceAtLeast(1)
        val maxPageIndex = safeMax - 1
        return (value - 1).coerceIn(0, maxPageIndex)
    }

    @Test
    fun `navigation on first page permits forward only`() {
        val (canGoBack, canGoForward) = computeNavigation(value = 0, max = 10)
        assertFalse(canGoBack)
        assertTrue(canGoForward)
        assertEquals(1, nextPageIndex(value = 0, max = 10))
        assertEquals(0, prevPageIndex(value = 0, max = 10))
    }

    @Test
    fun `navigation on middle page permits both directions`() {
        val (canGoBack, canGoForward) = computeNavigation(value = 4, max = 10)
        assertTrue(canGoBack)
        assertTrue(canGoForward)
        assertEquals(5, nextPageIndex(value = 4, max = 10))
        assertEquals(3, prevPageIndex(value = 4, max = 10))
    }

    @Test
    fun `navigation on last page disables forward and clamps to max minus one`() {
        val (canGoBack, canGoForward) = computeNavigation(value = 9, max = 10)
        assertTrue(canGoBack)
        assertFalse(canGoForward)
        // Ensure next page index does not produce 10 (which crashes PagerState.scrollToPage(max))
        assertEquals(9, nextPageIndex(value = 9, max = 10))
        assertEquals(8, prevPageIndex(value = 9, max = 10))
    }

    @Test
    fun `single page feed disables both directions and clamps to zero`() {
        val (canGoBack, canGoForward) = computeNavigation(value = 0, max = 1)
        assertFalse(canGoBack)
        assertFalse(canGoForward)
        assertEquals(0, nextPageIndex(value = 0, max = 1))
        assertEquals(0, prevPageIndex(value = 0, max = 1))
    }

    @Test
    fun `invalid or non-positive max is safely coerced to at least one`() {
        val (canGoBack, canGoForward) = computeNavigation(value = 0, max = 0)
        assertFalse(canGoBack)
        assertFalse(canGoForward)
        assertEquals(0, nextPageIndex(value = 0, max = 0))
    }
}
