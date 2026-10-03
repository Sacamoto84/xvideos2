package com.client.xvideos.l.featured.saved

import org.junit.Assert.assertEquals
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
    private fun writeItem(collection: File, folder: String, originalUrl: String): File {
        val dir = File(collection, folder).apply { mkdirs() }
        File(dir, "media.jpg").writeText("data")
        val metadataFile = File(dir, L_METADATA_FILE_NAME)
        writeCollectionMetadata(
            metadataFile,
            LSavedLikeMetadata(
                savedAt = 1L,
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
}
