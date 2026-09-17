package com.client.xvideos.l.ui.screens.screenAlbumList

import com.client.xvideos.l.ui.screens.screenAlbumList.atom.calculateNextAlbumPage
import com.client.xvideos.l.ui.screens.screenAlbumList.atom.calculatePrevAlbumPage
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumListPageSelectorTest {

    @Test
    fun `calculatePrevAlbumPage correctly returns previous page index and allows reaching first page 0`() {
        assertEquals(0, calculatePrevAlbumPage(1))
        assertEquals(1, calculatePrevAlbumPage(2))
        assertEquals(4, calculatePrevAlbumPage(5))
        assertEquals(0, calculatePrevAlbumPage(0))
        assertEquals(0, calculatePrevAlbumPage(-1))
    }

    @Test
    fun `calculateNextAlbumPage advances page and clamps to max valid index`() {
        val pageMax = 10 // Total pages 1..10, 0-based indices 0..9

        assertEquals(1, calculateNextAlbumPage(0, pageMax))
        assertEquals(8, calculateNextAlbumPage(7, pageMax))
        assertEquals(9, calculateNextAlbumPage(8, pageMax))
        // Clamps at 9 (pageMax - 1)
        assertEquals(9, calculateNextAlbumPage(9, pageMax))
        assertEquals(9, calculateNextAlbumPage(10, pageMax))
    }

    @Test
    fun `calculateNextAlbumPage handles single page and zero page gracefully`() {
        assertEquals(0, calculateNextAlbumPage(0, 1))
        assertEquals(0, calculateNextAlbumPage(0, 0))
    }
}
