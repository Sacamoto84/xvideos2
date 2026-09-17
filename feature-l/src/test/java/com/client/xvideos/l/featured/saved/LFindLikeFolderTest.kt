package com.client.xvideos.l.featured.saved

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LFindLikeFolderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `lFindLikeFolder returns null for empty or whitespace query`() {
        val root = tempFolder.newFolder("likes")
        assertNull(lFindLikeFolder(root, ""))
        assertNull(lFindLikeFolder(root, "   "))
        assertNull(lFindLikeFolder(root, "\t\n"))
    }

    @Test
    fun `lFindLikeFolder returns null for path outside root`() {
        val root = tempFolder.newFolder("likes")
        val outside = tempFolder.newFile("outside.jpg")
        assertNull(lFindLikeFolder(root, outside.absolutePath))
    }

    @Test
    fun `lFindLikeFolder finds folder by local media path`() {
        val root = tempFolder.newFolder("likes")
        val likeFolder = File(root, "like_123").apply { mkdirs() }
        val mediaFile = File(likeFolder, "image.jpg").apply { writeText("dummy") }
        val metadata = LSavedLikeMetadata(
            folderName = "like_123",
            mediaFileName = "image.jpg",
            sourceMediaUrl = "https://example.com/image.jpg"
        )
        writeLSavedLikeMetadata(File(likeFolder, L_METADATA_FILE_NAME), metadata)

        val found = lFindLikeFolder(root, mediaFile.absolutePath)
        assertEquals(likeFolder.canonicalPath, found?.canonicalPath)
    }

    @Test
    fun `lFindLikeFolder finds folder by remote source url`() {
        val root = tempFolder.newFolder("likes")
        val likeFolder = File(root, "like_456").apply { mkdirs() }
        val metadata = LSavedLikeMetadata(
            folderName = "like_456",
            mediaFileName = "video.mp4",
            sourceMediaUrl = "https://cdn.luscious.net/videos/video.mp4",
            sourceOriginalUrl = "https://luscious.net/pictures/456/"
        )
        writeLSavedLikeMetadata(File(likeFolder, L_METADATA_FILE_NAME), metadata)

        val foundByMedia = lFindLikeFolder(root, "https://cdn.luscious.net/videos/video.mp4")
        assertEquals(likeFolder.canonicalPath, foundByMedia?.canonicalPath)

        val foundByOriginal = lFindLikeFolder(root, "https://luscious.net/pictures/456/")
        assertEquals(likeFolder.canonicalPath, foundByOriginal?.canonicalPath)
    }

    @Test
    fun `lFindLikeFolder returns null when url does not match any like`() {
        val root = tempFolder.newFolder("likes")
        val likeFolder = File(root, "like_789").apply { mkdirs() }
        val metadata = LSavedLikeMetadata(
            folderName = "like_789",
            mediaFileName = "media.jpg",
            sourceMediaUrl = "https://cdn.example.com/media.jpg"
        )
        writeLSavedLikeMetadata(File(likeFolder, L_METADATA_FILE_NAME), metadata)

        val found = lFindLikeFolder(root, "https://cdn.example.com/unrelated.jpg")
        assertNull(found)
    }
}
