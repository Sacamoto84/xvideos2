package com.client.xvideos.l.ui.screens.explorer.tab.saved

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedTabPagesTest {

    @Test
    fun `без аккаунта L - только локальные вкладки, серверные скрыты`() {
        // 0 лайки, 1 альбомы, 2 коллекции; 3 подписки и 4 лайки на сервере — только с аккаунтом.
        assertEquals(3, savedTabPageCount(hasAccount = false))
        assertEquals(5, savedTabPageCount(hasAccount = true))
    }

    @Test
    fun `страница скрытой вкладки после выхода из аккаунта - последняя видимая`() {
        assertEquals(2, savedTabPage(page = 4, pageCount = 3))
        assertEquals(2, savedTabPage(page = 3, pageCount = 3))
    }

    @Test
    fun `видимая страница не меняется`() {
        assertEquals(0, savedTabPage(page = 0, pageCount = 3))
        assertEquals(2, savedTabPage(page = 2, pageCount = 3))
        assertEquals(4, savedTabPage(page = 4, pageCount = 5))
        assertEquals(0, savedTabPage(page = -1, pageCount = 5))
    }
}
