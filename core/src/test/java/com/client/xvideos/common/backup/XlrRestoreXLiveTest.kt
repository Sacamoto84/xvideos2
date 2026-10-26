package com.client.xvideos.common.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Восстановление старого архива, где сохранённое XLive лежит внутри X
 * (`X/Live`): после переноса папок оно оказывается в своей папке `XLive` по
 * выбранному режиму восстановления.
 */
class XlrRestoreXLiveTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun File.writeFile(relative: String, text: String): File {
        val file = File(this, relative)
        file.parentFile?.mkdirs()
        file.writeText(text)
        return file
    }

    private fun archive(vararg entries: Pair<String, String>): File {
        val file = tmp.newFile()
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                if (!name.endsWith("/")) zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private fun restore(main: File, zip: File, mode: XlrRestoreMode) =
        XlrBackupManager.restoreArchive(main, setOf("X"), mode) { ZipInputStream(zip.inputStream()) }

    private fun File.names(relative: String): List<String> =
        File(this, relative).list()?.sorted().orEmpty()

    @Test
    fun `замена старым архивом подменяет Мои в папке XLive`() {
        val main = tmp.newFolder("main")
        main.writeFile("XLive/Saved/b.room", "локальная")
        val zip = archive("X/Live/Saved/a.room" to "из архива")

        restore(main, zip, XlrRestoreMode.REPLACE)

        assertEquals(listOf("a.room"), main.names("XLive/Saved"))
        assertEquals("из архива", File(main, "XLive/Saved/a.room").readText())
        assertFalse(File(main, "X/Live").exists())
    }

    @Test
    fun `объединение со старым архивом добавляет к Моим в папке XLive`() {
        val main = tmp.newFolder("main")
        main.writeFile("XLive/Saved/b.room", "локальная")
        val zip = archive("X/Live/Saved/a.room" to "из архива")

        restore(main, zip, XlrRestoreMode.MERGE)

        assertEquals(listOf("a.room", "b.room"), main.names("XLive/Saved"))
        assertFalse(File(main, "X/Live").exists())
    }
}
