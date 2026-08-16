package com.client.xvideos.common.collectionDB

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

data class TestItem(val id: String, val url: String)

class CollectionDBTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun db(root: File) = CollectionDB(root.absolutePath, TestItem::class.java)

    @Test
    fun `insert пишет через временный файл и не оставляет мусора`() {
        val root = tmp.newFolder("collections")
        val db = db(root)

        assertTrue(db.insert("item1", "МояКоллекция", TestItem("item1", "https://x/1")).isSuccess)

        val dir = File(root, "МояКоллекция")
        val names = dir.listFiles()?.map { it.name }.orEmpty()
        assertEquals(listOf("item1.collection"), names)
    }

    @Test
    fun `insert перезаписывает существующий элемент целиком`() {
        val root = tmp.newFolder("collections2")
        val db = db(root)

        db.insert("item1", "К", TestItem("item1", "https://x/старый-очень-длинный-адрес"))
        db.insert("item1", "К", TestItem("item1", "https://x/1"))

        val items = db.readAllCollections().getOrThrow().single().items
        assertEquals(listOf(TestItem("item1", "https://x/1")), items)
    }

    @Test
    fun `обрезанный файл не ломает чтение остальных`() {
        val root = tmp.newFolder("collections3")
        val db = db(root)
        db.insert("ok", "К", TestItem("ok", "https://x/ok"))
        File(root, "К/broken.collection").writeText("{\"id\":\"bro")

        val items = db.readAllCollections().getOrThrow().single().items
        assertEquals(listOf(TestItem("ok", "https://x/ok")), items)
    }

    @Test
    fun `insert с недопустимым именем коллекции не создаёт папку`() {
        val root = tmp.newFolder("collections4")
        val db = db(root)

        assertTrue(db.insert("item1", "../побег", TestItem("item1", "https://x/1")).isFailure)
        assertEquals(0, root.listFiles()?.size ?: -1)
    }
}
