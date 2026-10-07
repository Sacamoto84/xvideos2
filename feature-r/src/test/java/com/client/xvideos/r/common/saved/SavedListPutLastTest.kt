package com.client.xvideos.r.common.saved

import androidx.compose.runtime.mutableStateListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Порядок списка сохранённого при добавлении записи.
 *
 * Лайки, авторы и ниши R держали эту логику тремя копиями, и все три падали
 * на пустом списке: «записи нет» и «последняя запись пустого списка» — один и
 * тот же индекс -1. Первый лайк после установки записывался на диск, но в
 * список не попадал и на экране появлялся только после перезапуска.
 */
class SavedListPutLastTest {

    private data class Item(val id: String, val note: String = "")

    private fun saved(vararg items: Item) = mutableStateListOf(*items)

    private fun MutableList<Item>.put(item: Item) = putLast(item) { it.id == item.id }

    @Test
    fun `первая запись попадает в пустой список`() {
        val list = saved()

        list.put(Item("a"))

        assertEquals(listOf(Item("a")), list.toList())
    }

    @Test
    fun `новая запись встаёт в конец`() {
        val list = saved(Item("a"), Item("b"))

        list.put(Item("c"))

        assertEquals(listOf("a", "b", "c"), list.map { it.id })
    }

    @Test
    fun `сохранённая повторно запись переезжает в конец`() {
        val list = saved(Item("a"), Item("b"), Item("c"))

        list.put(Item("a"))

        assertEquals(listOf("b", "c", "a"), list.map { it.id })
    }

    @Test
    fun `последняя запись заменяется обновлённой`() {
        val list = saved(Item("a"), Item("b", note = "старая"))

        list.put(Item("b", note = "новая"))

        assertEquals(listOf(Item("a"), Item("b", note = "новая")), list.toList())
    }

    @Test
    fun `та же запись в конце списка его не меняет`() {
        val list = saved(Item("a"), Item("b"))

        list.put(Item("b"))

        assertEquals(listOf(Item("a"), Item("b")), list.toList())
    }
}
