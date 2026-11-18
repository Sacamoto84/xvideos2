package com.client.xvideos.n.ui.atom

import org.junit.Assert.assertEquals
import org.junit.Test

class NAlbumListPageSelectorTest {

    @Test
    fun `calculatePrevNPage correctly returns previous page index and clamps at 1`() {
        assertEquals(1, calculatePrevNPage(2))
        assertEquals(2, calculatePrevNPage(3))
        assertEquals(4, calculatePrevNPage(5))
        assertEquals(1, calculatePrevNPage(1))
        assertEquals(1, calculatePrevNPage(0))
        assertEquals(1, calculatePrevNPage(-5))
    }

    @Test
    fun `calculateNextNPage advances page and clamps to pageMax`() {
        val pageMax = 10

        assertEquals(2, calculateNextNPage(1, pageMax))
        assertEquals(8, calculateNextNPage(7, pageMax))
        assertEquals(10, calculateNextNPage(9, pageMax))
        assertEquals(10, calculateNextNPage(10, pageMax))
        assertEquals(10, calculateNextNPage(11, pageMax))
    }

    @Test
    fun `calculateNextNPage handles edge cases gracefully`() {
        assertEquals(1, calculateNextNPage(1, 1))
        assertEquals(1, calculateNextNPage(1, 0))
        assertEquals(1, calculateNextNPage(1, -5))
    }
}
