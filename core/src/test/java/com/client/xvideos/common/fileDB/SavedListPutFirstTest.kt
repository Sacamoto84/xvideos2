package com.client.xvideos.common.fileDB

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Порядок списка сохранённого при добавлении записи.
 *
 * [FileDB.refresh] читает записи от новых к старым, а добавление в сеансе
 * дописывало запись в конец: новое стояло внизу списка, а после перезапуска —
 * наверху. Теперь новая запись встаёт туда же, куда её поставит чтение с диска.
 */
class SavedListPutFirstTest {

    private data class Item(val id: String, val note: String = "")

    private fun MutableList<Item>.put(item: Item) = putFirst(item) { it.id == item.id }

    @Test
    fun `первая запись попадает в пустой список`() {
        val list = mutableListOf<Item>()

        list.put(Item("a"))

        assertEquals(listOf(Item("a")), list)
    }

    @Test
    fun `новая запись встаёт в начало — как после чтения с диска`() {
        val list = mutableListOf(Item("b"), Item("a"))

        list.put(Item("c"))

        assertEquals(listOf("c", "b", "a"), list.map { it.id })
    }

    @Test
    fun `сохранённая повторно запись переезжает в начало`() {
        val list = mutableListOf(Item("c"), Item("b"), Item("a"))

        list.put(Item("a"))

        assertEquals(listOf("a", "c", "b"), list.map { it.id })
    }

    @Test
    fun `первая запись заменяется обновлённой`() {
        val list = mutableListOf(Item("b", note = "старая"), Item("a"))

        list.put(Item("b", note = "новая"))

        assertEquals(listOf(Item("b", note = "новая"), Item("a")), list)
    }

    @Test
    fun `та же запись в начале списка его не меняет`() {
        val list = mutableListOf(Item("b"), Item("a"))

        list.put(Item("b"))

        assertEquals(listOf(Item("b"), Item("a")), list)
    }
}
