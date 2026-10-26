package com.client.xvideos.common.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Переезд «Моих» и «Истории» XLive из `X/Live` в собственную папку `XLive`:
 * при первом обращении к разделу и после восстановления старого архива.
 */
class XLiveLegacyFolderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun File.writeFile(relative: String, text: String): File {
        val file = File(this, relative)
        file.parentFile?.mkdirs()
        file.writeText(text)
        return file
    }

    private fun File.names(relative: String): List<String> =
        File(this, relative).list()?.sorted().orEmpty()

    @Test
    fun `старой папки нет — ничего не трогается`() {
        val main = tmp.newFolder("main")
        main.writeFile("XLive/Saved/a.room", "new")

        assertFalse(XLiveLegacyFolder.move(main, XlrRestoreMode.MERGE))
        assertEquals("new", File(main, "XLive/Saved/a.room").readText())
    }

    @Test
    fun `объединение переносит недостающее, совпавший файл остаётся новым`() {
        val main = tmp.newFolder("main")
        main.writeFile("X/Live/Saved/a.room", "old")
        main.writeFile("X/Live/Saved/b.room", "old")
        main.writeFile("XLive/Saved/b.room", "new")

        assertTrue(XLiveLegacyFolder.move(main, XlrRestoreMode.MERGE))

        assertEquals("old", File(main, "XLive/Saved/a.room").readText())
        assertEquals("new", File(main, "XLive/Saved/b.room").readText())
        assertFalse(File(main, "X/Live").exists())
    }

    @Test
    fun `замена подменяет папку целиком`() {
        val main = tmp.newFolder("main")
        main.writeFile("X/Live/History/a.room", "old")
        main.writeFile("XLive/History/a.room", "new")
        main.writeFile("XLive/History/c.room", "new")

        assertTrue(XLiveLegacyFolder.move(main, XlrRestoreMode.REPLACE))

        assertEquals(listOf("a.room"), main.names("XLive/History"))
        assertEquals("old", File(main, "XLive/History/a.room").readText())
        assertFalse(File(main, "X/Live").exists())
    }

    @Test
    fun `замена не трогает папку, которой в старом месте нет`() {
        val main = tmp.newFolder("main")
        main.writeFile("X/Live/Saved/a.room", "old")
        main.writeFile("XLive/History/h.room", "new")

        XLiveLegacyFolder.move(main, XlrRestoreMode.REPLACE)

        assertEquals("new", File(main, "XLive/History/h.room").readText())
        assertEquals("old", File(main, "XLive/Saved/a.room").readText())
    }

    @Test
    fun `файл не перенёсся — старая папка остаётся, ничего не теряется`() {
        val main = tmp.newFolder("main")
        main.writeFile("X/Live/Saved/a.room", "old")
        // На месте папки нового места — файл: перенос в неё невозможен.
        main.writeFile("XLive/Saved", "не папка")

        XLiveLegacyFolder.move(main, XlrRestoreMode.MERGE)

        assertEquals("old", File(main, "X/Live/Saved/a.room").readText())
    }

    @Test
    fun `пустые папки нового места, созданные при запуске, заполняются`() {
        val main = tmp.newFolder("main")
        File(main, "XLive/Saved").mkdirs()
        File(main, "XLive/History").mkdirs()
        main.writeFile("X/Live/Saved/a.room", "old")

        assertTrue(XLiveLegacyFolder.move(main, XlrRestoreMode.MERGE))

        assertEquals(listOf("a.room"), main.names("XLive/Saved"))
        assertTrue(File(main, "X").exists())
        assertFalse(File(main, "X/Live").exists())
    }
}
