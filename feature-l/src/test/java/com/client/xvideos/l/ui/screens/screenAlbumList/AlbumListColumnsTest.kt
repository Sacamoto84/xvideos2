package com.client.xvideos.l.ui.screens.screenAlbumList

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumListColumnsTest {

    @Test
    fun `выбор колонок на вкладке доходит до сетки`() {
        assertEquals(1, albumListColumns(1))
        assertEquals(2, albumListColumns(2))
        assertEquals(3, albumListColumns(3))
        assertEquals(4, albumListColumns(4))
    }

    @Test
    fun `значение вне 1-4 - две колонки, как показывают точки на вкладке`() {
        assertEquals(2, albumListColumns(0))
        assertEquals(2, albumListColumns(5))
        assertEquals(2, albumListColumns(-1))
    }
}
