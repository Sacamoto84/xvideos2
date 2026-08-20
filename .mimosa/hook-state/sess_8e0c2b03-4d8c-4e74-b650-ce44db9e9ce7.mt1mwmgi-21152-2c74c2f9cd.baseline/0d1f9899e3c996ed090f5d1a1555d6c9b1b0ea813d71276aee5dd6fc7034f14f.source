package com.client.xvideos.common.fileDB

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class Row(val id: String, val value: String)

class FileDBTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun db(root: File) = FileDB(root.absolutePath, "row", Row::class.java)

    @Test
    fun `insert и read возвращают записанное`() {
        val root = tmp.newFolder("db")
        val db = db(root)

        assertTrue(db.insert("a", Row("a", "one")).isSuccess)
        assertEquals(Row("a", "one"), db.read("a").getOrThrow())
    }

    @Test
    fun `refresh наполняет список только валидными записями`() {
        val root = tmp.newFolder("db2")
        val db = db(root)
        db.insert("a", Row("a", "one"))
        db.insert("b", Row("b", "two"))
        File(root, "broken.row").writeText("{\"id\":\"bro")

        assertTrue(db.refresh().isSuccess)
        assertEquals(setOf("a", "b"), db.list.map { it.id }.toSet())
    }

    @Test
    fun `параллельные update и read не наблюдают пропажу файла`() {
        val root = tmp.newFolder("db3")
        val db = db(root)
        db.insert("a", Row("a", "start"))

        val start = CountDownLatch(1)
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())

        val writer = Thread {
            start.await()
            repeat(300) { i -> db.update("a", Row("a", "v$i")) }
        }
        val reader = Thread {
            start.await()
            repeat(300) {
                val result = db.read("a")
                if (result.isFailure) failures += result.exceptionOrNull()?.toString().orEmpty()
            }
        }

        writer.start(); reader.start(); start.countDown()
        writer.join(30_000); reader.join(30_000)

        assertEquals("read не должен видеть окно, в котором файла нет: $failures", 0, failures.size)
    }

    @Test
    fun `параллельные refresh не оставляют список в устаревшем состоянии`() {
        val root = tmp.newFolder("db4")
        val db = db(root)
        repeat(20) { i -> db.insert("k$i", Row("k$i", "v$i")) }

        val pool = java.util.concurrent.Executors.newFixedThreadPool(4)
        repeat(40) { pool.submit { db.refresh() } }
        pool.shutdown()
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))

        assertEquals(20, db.list.size)
    }
}
