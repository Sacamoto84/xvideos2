package com.client.xvideos.l.featured.saved

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Список коллекций показывает число дублей. Ключ элемента лежит в его
 * `metadata.json`, и раньше ради этого числа метаданные всех элементов
 * разбирались при каждом обновлении списка.
 */
class LCollectionListTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Создаёт сохранённый элемент коллекции и возвращает файл его метаданных. */
    private fun writeItem(collection: File, folder: String, originalUrl: String, savedAt: Long = 1L): File {
        val dir = File(collection, folder).apply { mkdirs() }
        File(dir, "media.jpg").writeText("data")
        val metadataFile = File(dir, L_METADATA_FILE_NAME)
        writeCollectionMetadata(
            metadataFile,
            LSavedLikeMetadata(
                savedAt = savedAt,
                folderName = folder,
                mediaFileName = "media.jpg",
                sourceMediaUrl = originalUrl,
                sourceOriginalUrl = originalUrl,
            ),
        )
        return metadataFile
    }

    @Test
    fun `список коллекций считает лишние копии одинаковых элементов`() {
        val root = tmp.newFolder("collections")
        val collection = File(root, "c1")
        writeItem(collection, "a", "http://host/one.jpg")
        writeItem(collection, "b", "http://host/one.jpg")
        writeItem(collection, "c", "http://host/one.jpg")
        writeItem(collection, "d", "http://host/two.jpg")

        val entity = lReadCollections(root).single()

        assertEquals(2, entity.duplicateCount)
        assertEquals(4, entity.itemsCount)
    }

    @Test
    fun `метаданные с прежним размером и временем записи повторно не разбираются`() {
        val root = tmp.newFolder("collections")
        val collection = File(root, "c1")
        writeItem(collection, "a", "http://host/one.jpg")
        val second = writeItem(collection, "b", "http://host/one.jpg")
        assertEquals(1, lReadCollections(root).single().duplicateCount)

        // Содержимое другое, а размер и время записи прежние: по ним файл считается
        // неизменённым. Если его разобрать заново, дубль исчезнет.
        val stamp = second.lastModified()
        val length = second.length()
        second.writeText(second.readText().replace("http://host/one.jpg", "http://host/six.jpg"))
        second.setLastModified(stamp)
        assertEquals(length, second.length())

        assertEquals("метаданные разобраны заново", 1, lReadCollections(root).single().duplicateCount)
    }

    @Test
    fun `изменённые метаданные разбираются заново`() {
        val root = tmp.newFolder("collections")
        val collection = File(root, "c1")
        writeItem(collection, "a", "http://host/one.jpg")
        val second = writeItem(collection, "b", "http://host/one.jpg")
        assertEquals(1, lReadCollections(root).single().duplicateCount)

        second.writeText(second.readText().replace("http://host/one.jpg", "http://host/another.jpg"))

        assertEquals(0, lReadCollections(root).single().duplicateCount)
    }

    @Test
    fun `давность коллекции — по времени сохранения элементов, а не по времени папок`() {
        val root = tmp.newFolder("collections")
        val older = File(root, "older")
        val newer = File(root, "newer")
        writeItem(older, "a", "http://host/a.jpg", savedAt = 1_000L)
        writeItem(newer, "b", "http://host/b.jpg", savedAt = 2_000L)
        // Восстановление бэкапа времена папок не сохраняет: папка старого элемента
        // после него может оказаться свежее.
        assertTrue(File(newer, "b").setLastModified(10_000L))
        assertTrue(File(older, "a").setLastModified(20_000L))

        val collections = lReadCollections(root, LCollectionSortOrder.RECENT)

        assertEquals(listOf("newer", "older"), collections.map { it.collection })
        assertEquals(2_000L, collections.first().lastModifiedAt)
    }

    @Test
    fun `автообложка — последний сохранённый элемент, а не самая свежая папка`() {
        val root = tmp.newFolder("collections")
        val collection = File(root, "c1")
        writeItem(collection, "old", "http://host/old.jpg", savedAt = 1_000L)
        writeItem(collection, "new", "http://host/new.jpg", savedAt = 2_000L)
        assertTrue(File(collection, "new").setLastModified(10_000L))
        assertTrue(File(collection, "old").setLastModified(20_000L))

        val entity = lReadCollections(root).single()

        assertEquals(File(collection, "new/media.jpg").absolutePath, entity.previewUrl)
    }

    @Test
    fun `давность пустой коллекции — время её папки`() {
        val root = tmp.newFolder("collections")
        val empty = File(root, "empty").apply { mkdirs() }
        assertTrue(empty.setLastModified(5_000L))

        assertEquals(5_000L, lReadCollections(root).single().lastModifiedAt)
    }
}
