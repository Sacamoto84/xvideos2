package com.client.xvideos.common.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Восстановление в режиме объединения: содержимое архива добавляется к тому,
 * что уже есть.
 *
 * Раньше восстановление умело только заменять: запись, сохранённая после
 * создания бэкапа, пропадала вместе с прежней копией раздела, и вернуть в
 * приложение старый архив, не потеряв нового, было нельзя.
 */
class XlrRestoreMergeTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun File.writeFile(relative: String, text: String): File {
        val file = File(this, relative)
        file.parentFile?.mkdirs()
        file.writeText(text)
        return file
    }

    private fun entry(path: String) = XlrBackupManager.RestoreJournalEntry(path, hadTarget = true)

    private fun File.leftovers(): List<String> =
        walkTopDown().filter { it.name.startsWith(".xlr_") }.map { it.name }.toList()

    private fun merge(main: File, temp: File, vararg paths: String) =
        XlrBackupManager.applyRestoredPaths(main, temp, paths.toList(), XlrRestoreMode.MERGE)

    @Test
    fun `объединение оставляет записи, которых нет в архиве`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("X/Favorites/после_бэкапа.ItemsX", "локальная")
        main.writeFile("X/Favorites/общая.ItemsX", "локальная версия")
        temp.writeFile("X/Favorites/общая.ItemsX", "версия архива")
        temp.writeFile("X/Favorites/только_в_архиве.ItemsX", "архивная")

        merge(main, temp, "X")

        assertEquals("локальная", File(main, "X/Favorites/после_бэкапа.ItemsX").readText())
        assertEquals("архивная", File(main, "X/Favorites/только_в_архиве.ItemsX").readText())
        assertEquals(
            "запись, которая есть и там и там, берётся из архива",
            "версия архива",
            File(main, "X/Favorites/общая.ItemsX").readText(),
        )
    }

    @Test
    fun `замена убирает записи, которых нет в архиве`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("X/Favorites/после_бэкапа.ItemsX", "локальная")
        temp.writeFile("X/Favorites/только_в_архиве.ItemsX", "архивная")

        XlrBackupManager.applyRestoredPaths(main, temp, listOf("X"), XlrRestoreMode.REPLACE)

        assertFalse(File(main, "X/Favorites/после_бэкапа.ItemsX").exists())
        assertTrue(File(main, "X/Favorites/только_в_архиве.ItemsX").exists())
    }

    @Test
    fun `объединение сохраняет скачанное и у своих записей, и у записей архива`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        // Запись есть только на устройстве.
        main.writeFile("R/Download/автор/свой.info", "{}")
        main.writeFile("R/Download/автор/свой.mp4", "видео-1")
        // Запись есть и в MINI-архиве: он несёт метаданные без медиа.
        main.writeFile("R/Download/автор/общий.info", "старые метаданные")
        main.writeFile("R/Download/автор/общий.mp4", "видео-2")
        temp.writeFile("R/Download/автор/общий.info", "метаданные архива")

        merge(main, temp, "R")

        assertEquals("видео-1", File(main, "R/Download/автор/свой.mp4").readText())
        assertEquals("{}", File(main, "R/Download/автор/свой.info").readText())
        assertEquals("видео-2", File(main, "R/Download/автор/общий.mp4").readText())
        assertEquals("метаданные архива", File(main, "R/Download/автор/общий.info").readText())
    }

    @Test
    fun `объединение оставляет пустую папку, которой нет в архиве`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        // Пустая папка в коллекциях — это коллекция без записей.
        File(main, "R/Collection/пустая").mkdirs()
        temp.writeFile("R/Collection/из_архива/ролик.collection", "{}")

        merge(main, temp, "R")

        assertTrue(File(main, "R/Collection/пустая").isDirectory)
        assertTrue(File(main, "R/Collection/из_архива/ролик.collection").isFile)
    }

    @Test
    fun `папка, которой нет в архиве, при объединении не меняется`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("L/Likes/1/metadata.json", "{}")
        main.writeFile("L/Likes/1/картинка.jpg", "jpg")

        merge(main, temp, "L/Likes")

        assertEquals("{}", File(main, "L/Likes/1/metadata.json").readText())
        assertEquals("jpg", File(main, "L/Likes/1/картинка.jpg").readText())
    }

    @Test
    fun `остатки прерванных загрузок при объединении не переносятся`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("R/Download/автор/клип.info", "{}")
        main.writeFile("R/Download/автор/клип.mp4.temp", "половина")
        temp.writeFile("R/Download/автор/другой.info", "{}")

        merge(main, temp, "R")

        assertTrue(File(main, "R/Download/автор/клип.info").isFile)
        assertFalse(File(main, "R/Download/автор/клип.mp4.temp").exists())
    }

    @Test
    fun `после объединения не остаётся ни отодвинутых копий, ни журнала`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("X/Favorites/локальная.ItemsX", "локальная")
        main.writeFile("R/Likes/локальный.likes", "локальный")
        temp.writeFile("X/Favorites/архивная.ItemsX", "архивная")
        temp.writeFile("R/Likes/архивный.likes", "архивный")

        merge(main, temp, "R", "X")

        assertEquals(emptyList<String>(), main.leftovers())
    }

    @Test
    fun `объединение, оборванное на уборке, доводится при следующем запуске`() {
        val main = tmp.newFolder("main")
        // Архив уже на месте, прежняя копия ещё не разобрана — процесс убит.
        main.writeFile("X/Favorites/архивная.ItemsX", "архивная")
        main.writeFile(".xlr_old_X/Favorites/локальная.ItemsX", "локальная")
        XlrBackupManager.writeRestoreJournal(main, committed = true, entries = listOf(entry("X")), mode = XlrRestoreMode.MERGE)

        val clean = XlrBackupManager.recoverInterruptedRestore(main)

        assertTrue(clean)
        assertEquals("локальная", File(main, "X/Favorites/локальная.ItemsX").readText())
        assertEquals("архивная", File(main, "X/Favorites/архивная.ItemsX").readText())
        assertEquals(emptyList<String>(), main.leftovers())
    }

    @Test
    fun `журнал без режима доводится как замена`() {
        val main = tmp.newFolder("main")
        main.writeFile("X/Favorites/архивная.ItemsX", "архивная")
        main.writeFile(".xlr_old_X/Favorites/локальная.ItemsX", "локальная")
        // Журнал, записанный версией до появления режима восстановления.
        main.writeFile(".xlr_restore_journal", """{"committed":true,"entries":[{"path":"X","hadTarget":true}]}""")

        val clean = XlrBackupManager.recoverInterruptedRestore(main)

        assertTrue(clean)
        assertFalse(File(main, "X/Favorites/локальная.ItemsX").exists())
        assertEquals(emptyList<String>(), main.leftovers())
    }
}
