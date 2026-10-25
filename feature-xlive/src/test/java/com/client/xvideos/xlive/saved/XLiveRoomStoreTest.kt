package com.client.xvideos.xlive.saved

import com.client.xvideos.xlive.model.XLiveSavedRoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class XLiveRoomStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private var clock = 0L
    private val tick: () -> Long = { ++clock }

    private fun XLiveRoomStore.names() = rooms.value.map { it.username }

    @Test
    fun `новая комната первой, повтор поднимается наверх без дубля`() {
        val store = testRoomStore(tmp.root, now = tick)

        store.put(XLiveSavedRoom("a"))
        store.put(XLiveSavedRoom("b"))
        store.put(XLiveSavedRoom("a"))

        assertEquals(listOf("a", "b"), store.names())
        assertEquals(3L, store.rooms.value.first().savedAt)
        assertTrue("a" in store)
    }

    @Test
    fun `возраст без анкеты остаётся от прежней записи`() {
        val store = testRoomStore(tmp.root, now = tick)

        store.put(XLiveSavedRoom("a", age = 24))
        store.put(XLiveSavedRoom("a"))

        assertEquals(24, store.rooms.value.single().age)
    }

    @Test
    fun `сверх лимита самые старые уходят и с диска`() {
        val store = testRoomStore(tmp.root, limit = 2, now = tick)

        store.put(XLiveSavedRoom("a"))
        store.put(XLiveSavedRoom("b"))
        store.put(XLiveSavedRoom("c"))

        assertEquals(listOf("c", "b"), store.names())
        assertEquals(listOf("b", "c"), tmp.root.list()!!.map { it.substringBefore('.') }.sorted())
    }

    @Test
    fun `убранная комната уходит из списка и с диска`() {
        val store = testRoomStore(tmp.root, now = tick)
        store.put(XLiveSavedRoom("a"))
        store.put(XLiveSavedRoom("b"))

        store.remove("a")

        assertEquals(listOf("b"), store.names())
        assertFalse("a" in store)
        assertEquals(listOf("b.room"), tmp.root.list()!!.toList())
    }

    @Test
    fun `новое хранилище читает диск от новых к старым`() {
        val first = testRoomStore(tmp.root, now = tick)
        first.put(XLiveSavedRoom("a", age = 30))
        first.put(XLiveSavedRoom("b"))
        first.put(XLiveSavedRoom("c"))
        first.put(XLiveSavedRoom("a"))

        val second = testRoomStore(tmp.root)

        assertEquals(listOf("a", "c", "b"), second.names())
        assertEquals(30, second.rooms.value.first().age)
    }

    @Test
    fun `перечитывание подхватывает файлы, изменённые мимо хранилища`() {
        val store = testRoomStore(tmp.root, now = tick)
        store.put(XLiveSavedRoom("a"))
        testRoomStore(tmp.root, now = { 100L }).put(XLiveSavedRoom("b"))

        store.refresh()

        assertEquals(listOf("b", "a"), store.names())
    }
}
