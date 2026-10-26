package com.client.xvideos.common.backup

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** XLive — четвёртый раздел резервной копии, со своими папками. */
class XlrBackupSectionsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun File.writeFile(relative: String, text: String): File {
        val file = File(this, relative)
        file.parentFile?.mkdirs()
        file.writeText(text)
        return file
    }

    @Test
    fun `в дереве копии есть раздел XLive с его папками`() {
        val main = tmp.newFolder("main")
        main.writeFile("XLive/Saved/a.room", "комната")
        main.writeFile("XLive/Records/1/record.json", "{}")

        val items = runBlocking { XlrBackupManager.currentBackupItems(baseDir = main) }

        val section = items.single { it.path == "XLive" }
        assertEquals(null, section.parentPath)
        assertEquals(
            listOf("XLive/Records", "XLive/Saved"),
            items.filter { it.parentPath == "XLive" }.map { it.path },
        )
    }

    @Test
    fun `пути XLive проходят нормализацию, чужой раздел — нет`() {
        assertEquals(
            listOf("XLive/Records"),
            XlrBackupManager.normalizeSelectedPaths(setOf("XLive/Records", "Y/z")),
        )
    }

    @Test
    fun `недописанные файлы записи в копию не идут`() {
        val options = XlrBackupOptions.DEFAULT
        assertFalse(XlrBackupManager.shouldIncludeBackupEntry("XLive/Records/1/video.part", options))
        assertFalse(XlrBackupManager.shouldIncludeBackupEntry("XLive/Records/1/video.mp4.part", options))
        assertFalse(XlrBackupManager.shouldIncludeBackupEntry("XLive/Records/1/.storyboard.jpg", options))
        assertTrue(XlrBackupManager.shouldIncludeBackupEntry("XLive/Records/1/video.mp4", options))
    }
}
