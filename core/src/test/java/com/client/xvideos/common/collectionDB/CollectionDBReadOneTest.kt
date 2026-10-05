package com.client.xvideos.common.collectionDB

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Чтение одной коллекции: после правки в ней незачем перечитывать все остальные. */
class CollectionDBReadOneTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun db(root: File) = CollectionDB(root.absolutePath, TestItem.serializer())

    @Test
    fun `читается только названная коллекция`() {
        val db = db(tmp.newFolder("collections"))
        db.insert("a1", "A", TestItem("a1", "https://x/a1"))
        db.insert("b1", "B", TestItem("b1", "https://x/b1"))

        val read = db.readCollection("A").getOrThrow()

        assertEquals("A", read?.collection)
        assertEquals(listOf("a1"), read?.items?.map { it.id })
    }

    @Test
    fun `имя сравнивается так же, как при записи — без пробелов по краям`() {
        val db = db(tmp.newFolder("collections"))
        db.insert("a1", "A", TestItem("a1", "https://x/a1"))

        assertEquals("A", db.readCollection("  A ").getOrThrow()?.collection)
    }

    @Test
    fun `пустая коллекция читается пустой, а отсутствующая — как null`() {
        val db = db(tmp.newFolder("collections"))
        db.create("Empty")

        assertEquals(emptyList<TestItem>(), db.readCollection("Empty").getOrThrow()?.items)
        assertNull(db.readCollection("Missing").getOrThrow())
    }

    @Test
    fun `недопустимое имя — отказ`() {
        val db = db(tmp.newFolder("collections"))

        assertTrue(db.readCollection("../outside").isFailure)
    }
}
