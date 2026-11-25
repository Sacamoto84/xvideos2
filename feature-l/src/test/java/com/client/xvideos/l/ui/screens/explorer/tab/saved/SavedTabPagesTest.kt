package com.client.xvideos.l.ui.screens.explorer.tab.saved

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedTabPagesTest {

    @Test
    fun `серверные вкладки доступны для входа в L`() {
        assertEquals(5, L_SAVED_TAB_COUNT)
        assertEquals(3, savedTabPage(3))
        assertEquals(4, savedTabPage(4))
    }

    @Test
    fun `страница ограничена числом вкладок`() {
        assertEquals(0, savedTabPage(-1))
        assertEquals(2, savedTabPage(2))
        assertEquals(4, savedTabPage(5))
    }
}
