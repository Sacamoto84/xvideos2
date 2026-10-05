package com.client.xvideos.l.featured.saved

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Сохранённый элемент ищут и в лайках, и в коллекциях. «В галерею» смотрела
 * только в лайки: картинка из коллекции качалась заново, а без сети — ошибка,
 * хотя файл лежит на устройстве.
 */
class LFindSavedItemFolderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun savedItem(parent: File, name: String): File {
        val folder = File(parent, name).apply { mkdirs() }
        File(folder, "media.jpg").writeText("dummy")
        writeLSavedLikeMetadata(
            File(folder, L_METADATA_FILE_NAME),
            LSavedLikeMetadata(folderName = name, mediaFileName = "media.jpg"),
        )
        return folder
    }

    @Test
    fun `элемент коллекции находится по пути к его файлу`() {
        val likes = tmp.newFolder("likes")
        val collections = tmp.newFolder("collections")
        val item = savedItem(File(collections, "favorites"), "item_1")

        val found = lFindSavedItemFolder(likes, collections, File(item, "media.jpg").absolutePath)

        assertEquals(item.canonicalPath, found?.canonicalPath)
    }

    @Test
    fun `лайк находится раньше коллекции`() {
        val likes = tmp.newFolder("likes")
        val collections = tmp.newFolder("collections")
        val like = savedItem(likes, "like_1")

        val found = lFindSavedItemFolder(likes, collections, File(like, "media.jpg").absolutePath)

        assertEquals(like.canonicalPath, found?.canonicalPath)
    }

    @Test
    fun `несохранённый элемент не находится`() {
        val likes = tmp.newFolder("likes")
        val collections = tmp.newFolder("collections")

        assertNull(lFindSavedItemFolder(likes, collections, "https://example.com/unknown.jpg"))
    }
}
