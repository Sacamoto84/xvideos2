package com.client.xvideos.l.featured.saved

import com.client.xvideos.l.model.AlbumDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedL_AlbumsTest {

    @Test
    fun `валидация id альбома отклоняет пустые и нечисловые значения`() {
        assertNull("".toLongOrNull())
        assertNull("   ".toLongOrNull())
        assertNull("abc".toLongOrNull())
        assertNull("album_123".toLongOrNull())
        assertNull("12.34".toLongOrNull())
        assertNull("-".toLongOrNull())

        assertEquals(12345L, "12345".toLongOrNull())
        assertEquals(1L, "1".toLongOrNull())
        assertEquals(9876543210L, "9876543210".toLongOrNull())
    }

    @Test
    fun `идемпотентное сохранение альбома заменяет существующий элемент с тем же id`() {
        val initialAlbum = AlbumDetails(id = "101", title = "Original Album", number_of_pictures = 5)
        val list = mutableListOf(initialAlbum)

        val updatedAlbum = AlbumDetails(id = "101", title = "Updated Album Title", number_of_pictures = 12)

        // Семантика add/addAndPicsDetails в SavedL_Albums:
        list.removeAll { it.id == updatedAlbum.id }
        list.add(updatedAlbum)

        assertEquals(1, list.size)
        assertEquals("Updated Album Title", list.first().title)
        assertEquals(12, list.first().number_of_pictures)
    }

    @Test
    fun `удаление альбома по id удаляет только целевой элемент`() {
        val album1 = AlbumDetails(id = "101", title = "Album 1")
        val album2 = AlbumDetails(id = "102", title = "Album 2")
        val album3 = AlbumDetails(id = "103", title = "Album 3")
        val list = mutableListOf(album1, album2, album3)

        // Семантика remove в SavedL_Albums:
        val removed = list.removeAll { it.id == "102" }

        assertTrue(removed)
        assertEquals(2, list.size)
        assertEquals(listOf("101", "103"), list.map { it.id })
    }

    @Test
    fun `удаление несуществующего альбома не изменяет список`() {
        val album1 = AlbumDetails(id = "101", title = "Album 1")
        val list = mutableListOf(album1)

        val removed = list.removeAll { it.id == "999" }

        assertFalse(removed)
        assertEquals(1, list.size)
        assertEquals("101", list.first().id)
    }
}
