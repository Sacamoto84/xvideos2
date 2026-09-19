package com.client.xvideos.common.webserver

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files

class LocalLibraryProviderSecurityTest {

    companion object {
        private lateinit var tempDir: File

        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            tempDir = Files.createTempDirectory("web_server_test").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }
    }

    @Test
    fun `resolveVideo отклоняет path traversal попытки`() {
        assertNull(LocalLibraryProvider.resolveVideo("x", "../../etc/passwd"))
        assertNull(LocalLibraryProvider.resolveVideo("x", "123/../../456"))
        assertNull(LocalLibraryProvider.resolveVideo("x", "..\\windows\\system32"))
        assertNull(LocalLibraryProvider.resolveVideo("r", "../secret"))
        assertNull(LocalLibraryProvider.resolveVideo("r", ""))
        assertNull(LocalLibraryProvider.resolveVideo("unknown", "123"))
    }

    @Test
    fun `resolvePoster отклоняет path traversal попытки`() {
        assertNull(LocalLibraryProvider.resolvePoster("x", "../../../root"))
        assertNull(LocalLibraryProvider.resolvePoster("r", "..\\..\\secret"))
        assertNull(LocalLibraryProvider.resolvePoster("x", ""))
    }

    @Test
    fun `resolveLMedia отклоняет небезопасные имена папок и файлов`() {
        assertNull(LocalLibraryProvider.resolveLMedia("../../root", "media.mp4"))
        assertNull(LocalLibraryProvider.resolveLMedia("safe_folder", "../../secret.mp4"))
        assertNull(LocalLibraryProvider.resolveLMedia(".", "."))
        assertNull(LocalLibraryProvider.resolveLMedia("..", ".."))
        assertNull(LocalLibraryProvider.resolveLMedia("folder", "file/with/slash"))
    }

    @Test
    fun `resolveLMedia корректно резолвит папки с точками в именах`() {
        val likesDir = File(AppPath.l_likes)
        val itemDir = File(likesDir, "album_hash_name.01").apply { mkdirs() }
        val mediaFile = File(itemDir, "media.mp4").apply { writeText("dummy-video-data") }

        val resolved = LocalLibraryProvider.resolveLMedia("album_hash_name.01", "media.mp4")
        assertNotNull(resolved)
        assertEquals(mediaFile.canonicalPath, resolved?.first?.canonicalPath)
        assertEquals("media.mp4", resolved?.second)
    }

    @Test
    fun `resolveLCollectionMedia отклоняет path traversal и корректно находит элементы с точками`() {
        assertNull(LocalLibraryProvider.resolveLCollectionMedia("../../root", "item", "media.mp4"))
        assertNull(LocalLibraryProvider.resolveLCollectionMedia("valid_col", "../../item", "media.mp4"))
        assertNull(LocalLibraryProvider.resolveLCollectionMedia("valid_col", "item", "../secret.mp4"))

        val colDir = File(AppPath.l_collection, "Summer_2026").apply { mkdirs() }
        val itemDir = File(colDir, "item.v2.preview").apply { mkdirs() }
        val targetFile = File(itemDir, "preview.jpg").apply { writeText("dummy-image-data") }

        val resolved = LocalLibraryProvider.resolveLCollectionMedia("Summer_2026", "item.v2.preview", "preview.jpg")
        assertNotNull(resolved)
        assertEquals(targetFile.canonicalPath, resolved?.first?.canonicalPath)
        assertEquals("preview.jpg", resolved?.second)
    }
}
