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

    @Test
    fun `readAllCollections подчищает временные файлы от прерванной записи`() {
        val root = tmp.newFolder("collections5")
        val db = db(root)
        db.insert("ok", "К", TestItem("ok", "https://x/ok"))
        // Так выглядит папка после обрыва процесса посреди writeTextAtomically.
        File(root, "К/битый.collection.tmp").writeText("{\"id\":\"би")

        db.readAllCollections().getOrThrow()

        val leftovers = File(root, "К").listFiles()?.map { it.name }.orEmpty()
        assertEquals(listOf("ok.collection"), leftovers)
    }

    @Test
    fun `параллельные insert и readAllCollections не мешают друг другу`() {
        val root = tmp.newFolder("collections6")
        val db = db(root)
        db.insert("seed", "К", TestItem("seed", "https://x/seed"))

        val start = java.util.concurrent.CountDownLatch(1)
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())

        val writer = Thread {
            start.await()
            repeat(200) { i -> db.insert("item$i", "К", TestItem("item$i", "https://x/$i")) }
        }
        val reader = Thread {
            start.await()
            repeat(200) {
                val result = db.readAllCollections()
                if (result.isFailure) failures += result.exceptionOrNull()?.toString().orEmpty()
            }
        }

        writer.start(); reader.start(); start.countDown()
        writer.join(30_000); reader.join(30_000)

        assertEquals("чтение не должно падать на параллельной записи: $failures", 0, failures.size)
    }
}
