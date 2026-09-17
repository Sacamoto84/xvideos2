package com.client.xvideos.l.featured.saved

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LFindCollectionItemFolderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `lFindCollectionItemFolder returns null for empty or whitespace identifiers`() {
        val root = tempFolder.newFolder("collection_root")
        assertNull(lFindCollectionItemFolder(root, emptyList()))
        assertNull(lFindCollectionItemFolder(root, listOf("", "   ", "\t\n")))
    }

    @Test
    fun `lFindCollectionItemFolder returns null for path outside root`() {
        val root = tempFolder.newFolder("collection_root")
        val outside = tempFolder.newFile("outside.jpg")
        assertNull(lFindCollectionItemFolder(root, listOf(outside.absolutePath)))
    }

    @Test
    fun `lFindCollectionItemFolder finds folder by direct file path`() {
        val root = tempFolder.newFolder("collection_root")
        val itemFolder = File(root, "item_1").apply { mkdirs() }
        val mediaFile = File(itemFolder, "media.jpg").apply { writeText("data") }
        val metadata = LSavedLikeMetadata(
            folderName = "item_1",
            mediaFileName = "media.jpg",
            sourceMediaUrl = "https://example.com/media.jpg"
        )
        writeCollectionMetadata(File(itemFolder, L_METADATA_FILE_NAME), metadata)

        val found = lFindCollectionItemFolder(root, listOf(mediaFile.absolutePath))
        assertEquals(itemFolder.canonicalPath, found?.canonicalPath)
    }

    @Test
    fun `lFindCollectionItemFolder finds folder by remote source url`() {
        val root = tempFolder.newFolder("collection_root")
        val itemFolder = File(root, "item_2").apply { mkdirs() }
        val mediaFile = File(itemFolder, "video.mp4").apply { writeText("data") }
        val metadata = LSavedLikeMetadata(
            folderName = "item_2",
            mediaFileName = "video.mp4",
            sourceMediaUrl = "https://cdn.luscious.net/item_2.mp4",
            sourceOriginalUrl = "https://luscious.net/pictures/item_2/"
        )
        writeCollectionMetadata(File(itemFolder, L_METADATA_FILE_NAME), metadata)

        val found = lFindCollectionItemFolder(root, listOf("https://luscious.net/pictures/item_2/"))
        assertEquals(itemFolder.canonicalPath, found?.canonicalPath)
    }

    @Test
    fun `lFindCollectionItemFolder returns null when identifiers do not match`() {
        val root = tempFolder.newFolder("collection_root")
        val itemFolder = File(root, "item_3").apply { mkdirs() }
        val metadata = LSavedLikeMetadata(
            folderName = "item_3",
            mediaFileName = "pic.jpg",
            sourceMediaUrl = "https://cdn.luscious.net/pic.jpg"
        )
        writeCollectionMetadata(File(itemFolder, L_METADATA_FILE_NAME), metadata)

        val found = lFindCollectionItemFolder(root, listOf("https://unrelated.com/image.jpg"))
        assertNull(found)
    }
}
