package com.client.xvideos.n.ui.screens.explorer.tab.saved

import org.junit.Assert.assertEquals
import org.junit.Test

class NSavedTabPagesTest {

    @Test
    fun `nSavedTabPageCount returns 3 without account and 4 with account`() {
        assertEquals(3, nSavedTabPageCount(isLoggedIn = false))
        assertEquals(4, nSavedTabPageCount(isLoggedIn = true))
    }

    @Test
    fun `nSavedTabPage clamps page index to valid range`() {
        assertEquals(0, nSavedTabPage(page = 0, pageCount = 3))
        assertEquals(2, nSavedTabPage(page = 2, pageCount = 3))
        assertEquals(2, nSavedTabPage(page = 3, pageCount = 3))
        assertEquals(3, nSavedTabPage(page = 3, pageCount = 4))
        assertEquals(0, nSavedTabPage(page = -1, pageCount = 4))
    }
}
