package com.client.xvideos.common.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Восстановление из архива, в котором раздел неполон.
 *
 * Архив собирают и из части папок раздела — например, X без загрузок. Дерево
 * восстановления отмечает раздел целиком, и замена стирала в нём всё, чего в
 * архиве нет: скачанные видео, историю, подписки. Папка, о которой архив
 * ничего не знает, не трогается ни в одном режиме.
 */
class XlrRestorePartialArchiveTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun File.writeFile(relative: String, text: String): File {
        val file = File(this, relative)
        file.parentFile?.mkdirs()
        file.writeText(text)
        return file
    }

    /** Пишет ZIP из записей «имя → содержимое»; имя с `/` на конце — запись каталога. */
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

    private fun restore(main: File, zip: File, paths: Set<String>, mode: XlrRestoreMode) =
        XlrBackupManager.restoreArchive(main, paths, mode) { ZipInputStream(zip.inputStream()) }

    @Test
    fun `замена раздела не трогает папку, которой нет в архиве`() {
        val main = tmp.newFolder("main")
        main.writeFile("X/Download/video.mp4", "видео")
        main.writeFile("X/Favorites/old.json", "старое")
        val zip = archive("X/" to "", "X/Favorites/" to "", "X/Favorites/new.json" to "новое")

        restore(main, zip, setOf("X"), XlrRestoreMode.REPLACE)

        assertEquals("видео", File(main, "X/Download/video.mp4").readText())
        assertEquals("новое", File(main, "X/Favorites/new.json").readText())
        assertFalse("папка из архива заменяется целиком", File(main, "X/Favorites/old.json").exists())
    }

    @Test
    fun `папка, пустая в архиве, при замене становится пустой`() {
        val main = tmp.newFolder("main")
        main.writeFile("X/History/1.json", "просмотр")
        val zip = archive("X/" to "", "X/History/" to "", "X/Favorites/a.json" to "избранное")

        restore(main, zip, setOf("X"), XlrRestoreMode.REPLACE)

        assertTrue(File(main, "X/History").isDirectory)
        assertEquals(0, File(main, "X/History").listFiles()?.size ?: -1)
    }

    @Test
    fun `раздел, от которого в архиве одна запись каталога, не трогается`() {
        val main = tmp.newFolder("main")
        main.writeFile("R/Likes/1.json", "лайк")
        val zip = archive("R/" to "", "X/Favorites/a.json" to "избранное")

        restore(main, zip, setOf("R", "X"), XlrRestoreMode.REPLACE)

        assertEquals("лайк", File(main, "R/Likes/1.json").readText())
        assertEquals("избранное", File(main, "X/Favorites/a.json").readText())
    }

    @Test
    fun `выбран только раздел без папок в архиве — отказ, данные на месте`() {
        val main = tmp.newFolder("main")
        main.writeFile("R/Likes/1.json", "лайк")
        val zip = archive("R/" to "", "X/Favorites/a.json" to "избранное")

        val result = runCatching { restore(main, zip, setOf("R"), XlrRestoreMode.REPLACE) }

        assertTrue("восстанавливать нечего — это отказ, а не молчаливый успех", result.isFailure)
        assertEquals("лайк", File(main, "R/Likes/1.json").readText())
    }

    @Test
    fun `отчёт считает только распакованные файлы`() {
        val main = tmp.newFolder("main")
        val zip = archive("X/Favorites/a.json" to "12345", "L/Likes/b/metadata.json" to "1")

        val report = restore(main, zip, setOf("X"), XlrRestoreMode.REPLACE)

        assertEquals(1, report.files)
        assertEquals(5L, report.bytes)
    }
}
