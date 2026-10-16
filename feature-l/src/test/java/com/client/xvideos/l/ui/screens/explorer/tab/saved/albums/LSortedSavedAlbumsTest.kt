package com.client.xvideos.l.ui.screens.explorer.tab.saved.albums

import com.client.xvideos.l.model.AlbumDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class LSortedSavedAlbumsTest {

    // Сохранённые альбомы приходят от последних сохранённых к первым.
    private val newest = AlbumDetails(id = "3", number_of_pictures = 10)
    private val middle = AlbumDetails(id = "2", number_of_pictures = 120)
    private val oldest = AlbumDetails(id = "1", number_of_pictures = 10)
    private val albums = listOf(newest, middle, oldest)

    private fun ids(order: LSavedAlbumsOrder) = lSortedSavedAlbums(albums, order).map { it.id }

    @Test
    fun `сначала новые - как в списке, сначала старые - наоборот`() {
        assertEquals(listOf("3", "2", "1"), ids(LSavedAlbumsOrder.NEWEST))
        assertEquals(listOf("1", "2", "3"), ids(LSavedAlbumsOrder.OLDEST))
    }

    @Test
    fun `больше элементов - от большего, при равенстве порядок сохранения`() {
        assertEquals(listOf("2", "3", "1"), ids(LSavedAlbumsOrder.MOST_ITEMS))
    }

    @Test
    fun `номер из настроек - вне списка даёт порядок по умолчанию`() {
        assertEquals(LSavedAlbumsOrder.MOST_ITEMS, lSavedAlbumsOrderOf(LSavedAlbumsOrder.MOST_ITEMS.ordinal))
        assertEquals(LSavedAlbumsOrder.NEWEST, lSavedAlbumsOrderOf(-1))
        assertEquals(LSavedAlbumsOrder.NEWEST, lSavedAlbumsOrderOf(42))
    }

    @Test
    fun `пункты меню - по порядку`() {
        assertEquals(
            listOf("Сначала новые", "Сначала старые", "Больше элементов"),
            LSavedAlbumsOrder.entries.map { it.title }
        )
    }
}
