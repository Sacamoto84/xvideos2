package com.client.xvideos.x

import com.client.xvideos.x.model.ItemsX
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedX_FavoritesTest {

    @Test
    fun `удаление по id удаляет элемент даже при изменении метаданных`() {
        val original = ItemsX(id = 101L, title = "Original Title", views = "10k views")
        val list = mutableListOf(original)

        val updatedRemote = ItemsX(id = 101L, title = "Original Title", views = "25k views")

        // Обычный remove(item) не находит элемент из-за несовпадения views
        assertFalse(list.remove(updatedRemote))
        assertEquals(1, list.size)

        // removeAll по id удаляет элемент гарантированно
        assertTrue(list.removeAll { it.id == updatedRemote.id })
        assertEquals(0, list.size)
    }

    @Test
    fun `идемпотентное добавление предотвращает дублирование id в списке`() {
        val first = ItemsX(id = 202L, title = "Video 1", views = "1k")
        val list = mutableListOf<ItemsX>()

        // Первое добавление
        list.removeAll { it.id == first.id }
        list.add(first)
        assertEquals(1, list.size)

        // Повторное добавление того же id с обновлёнными данными
        val second = ItemsX(id = 202L, title = "Video 1 Updated", views = "5k")
        list.removeAll { it.id == second.id }
        list.add(second)

        assertEquals(1, list.size)
        assertEquals("Video 1 Updated", list.first().title)
        assertEquals("5k", list.first().views)
    }
}
