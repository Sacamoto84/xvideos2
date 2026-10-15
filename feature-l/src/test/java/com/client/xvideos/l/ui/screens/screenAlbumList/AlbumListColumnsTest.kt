package com.client.xvideos.l.ui.screens.screenAlbumList

import com.client.xvideos.common.settings.calculateNextColumn
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumListColumnsTest {

    @Test
    fun `выбор колонок на вкладке доходит до сетки`() {
        assertEquals(2, albumListColumns(2))
        assertEquals(3, albumListColumns(3))
        assertEquals(4, albumListColumns(4))
    }

    @Test
    fun `одна колонка неудобна - список альбомов минимум в две`() {
        assertEquals(2, albumListColumns(1))
    }

    @Test
    fun `значение вне 2-4 - две колонки, как показывают точки на вкладке`() {
        assertEquals(2, albumListColumns(0))
        assertEquals(2, albumListColumns(5))
        assertEquals(2, albumListColumns(-1))
    }

    @Test
    fun `повторный тап по вкладке перебирает 2, 3, 4 и не заходит на 1`() {
        // Флаги по умолчанию разрешают 1..4 колонки; список альбомов начинает с двух.
        val flags = listOf(false, true, true, true, true)
        assertEquals(3, calculateNextColumn(2, flags, minCol = ALBUM_LIST_MIN_COLUMNS))
        assertEquals(4, calculateNextColumn(3, flags, minCol = ALBUM_LIST_MIN_COLUMNS))
        assertEquals(2, calculateNextColumn(4, flags, minCol = ALBUM_LIST_MIN_COLUMNS))
        assertEquals(2, calculateNextColumn(1, flags, minCol = ALBUM_LIST_MIN_COLUMNS))
    }
}
