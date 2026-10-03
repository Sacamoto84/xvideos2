package com.client.xvideos.common.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Восстановление бэкапа без медиа (режим MINI) поверх раздела, где медиа уже
 * скачаны.
 *
 * Раздел заменялся целиком, и скачанные файлы удалялись вместе со старой
 * копией — всё приходилось качать заново. Файлы элементов, которые есть в
 * бэкапе, теперь переезжают в восстановленный раздел.
 */
class XlrRestoreCarryOverTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun File.writeFile(relative: String, text: String): File {
        val file = File(this, relative)
        file.parentFile?.mkdirs()
        file.writeText(text)
        return file
    }

    @Test
    fun `скачанное видео R переезжает к своему info из бэкапа`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("R/Download/creator/a.info", "старое описание")
        main.writeFile("R/Download/creator/a.mp4", "видео a")
        main.writeFile("R/Download/creator/a.jpg", "превью a")
        temp.writeFile("R/Download/creator/a.info", "описание из бэкапа")

        XlrBackupManager.applyRestoredPaths(main, temp, listOf("R"))

        assertEquals("описание из бэкапа", File(main, "R/Download/creator/a.info").readText())
        assertEquals("видео a", File(main, "R/Download/creator/a.mp4").readText())
        assertEquals("превью a", File(main, "R/Download/creator/a.jpg").readText())
    }

    @Test
    fun `ролик R, которого нет в бэкапе, не переезжает`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("R/Download/creator/a.info", "a")
        main.writeFile("R/Download/creator/b.info", "b")
        main.writeFile("R/Download/creator/b.mp4", "видео b")
        temp.writeFile("R/Download/creator/a.info", "a")

        XlrBackupManager.applyRestoredPaths(main, temp, listOf("R"))

        assertFalse(File(main, "R/Download/creator/b.mp4").exists())
        assertFalse(File(main, "R/Download/creator/b.info").exists())
    }

    @Test
    fun `скачанные файлы элемента L переезжают к его метаданным из бэкапа`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("L/Likes/item1/metadata.json", "{}")
        main.writeFile("L/Likes/item1/media.jpg", "картинка")
        main.writeFile("L/Likes/item1/preview.100x100.jpg", "превью")
        main.writeFile("L/Likes/item2/metadata.json", "{}")
        main.writeFile("L/Likes/item2/media.jpg", "лишняя")
        temp.writeFile("L/Likes/item1/metadata.json", "{\"из\":\"бэкапа\"}")

        XlrBackupManager.applyRestoredPaths(main, temp, listOf("L/Likes"))

        assertEquals("картинка", File(main, "L/Likes/item1/media.jpg").readText())
        assertEquals("превью", File(main, "L/Likes/item1/preview.100x100.jpg").readText())
        assertEquals("{\"из\":\"бэкапа\"}", File(main, "L/Likes/item1/metadata.json").readText())
        assertFalse("элемента нет в бэкапе", File(main, "L/Likes/item2").exists())
    }

    @Test
    fun `файл из бэкапа важнее скачанного`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("R/Download/creator/a.info", "a")
        main.writeFile("R/Download/creator/a.mp4", "старое видео")
        temp.writeFile("R/Download/creator/a.info", "a")
        temp.writeFile("R/Download/creator/a.mp4", "видео из бэкапа")

        XlrBackupManager.applyRestoredPaths(main, temp, listOf("R"))

        assertEquals("видео из бэкапа", File(main, "R/Download/creator/a.mp4").readText())
    }

    @Test
    fun `недокачанные файлы не переезжают`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("R/Download/creator/a.info", "a")
        main.writeFile("R/Download/creator/a.mp4.temp", "половина видео")
        main.writeFile("L/Likes/item1/metadata.json", "{}")
        main.writeFile("L/Likes/item1/media.jpg.part", "половина картинки")
        temp.writeFile("R/Download/creator/a.info", "a")
        temp.writeFile("L/Likes/item1/metadata.json", "{}")

        XlrBackupManager.applyRestoredPaths(main, temp, listOf("L", "R"))

        assertFalse(File(main, "R/Download/creator/a.mp4.temp").exists())
        assertFalse(File(main, "L/Likes/item1/media.jpg.part").exists())
    }

    @Test
    fun `в разделе без облегчённого режима скачанное не переезжает`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("X/Download/1.info", "1")
        main.writeFile("X/Download/1.mp4", "видео")
        temp.writeFile("X/Download/1.info", "1")

        XlrBackupManager.applyRestoredPaths(main, temp, listOf("X"))

        assertFalse("раздел X всегда восстанавливается как в бэкапе", File(main, "X/Download/1.mp4").exists())
    }

    @Test
    fun `неудачное восстановление возвращает скачанное на место`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("R/Download/creator/a.info", "a")
        main.writeFile("R/Download/creator/a.mp4", "видео a")
        temp.writeFile("R/Download/creator/a.info", "a из бэкапа")

        val result = runCatching { XlrBackupManager.applyRestoredPaths(main, temp, listOf("R", "../снаружи")) }

        assertTrue(result.isFailure)
        assertEquals("a", File(main, "R/Download/creator/a.info").readText())
        assertEquals("видео a", File(main, "R/Download/creator/a.mp4").readText())
    }

    @Test
    fun `уборка после гибели процесса тоже переносит скачанное`() {
        val main = tmp.newFolder("main")
        main.writeFile(".xlr_old_R/Download/creator/a.info", "a")
        main.writeFile(".xlr_old_R/Download/creator/a.mp4", "видео a")
        main.writeFile("R/Download/creator/a.info", "a из бэкапа")
        XlrBackupManager.writeRestoreJournal(
            main,
            committed = true,
            entries = listOf(XlrBackupManager.RestoreJournalEntry("R", hadTarget = true)),
        )

        XlrBackupManager.recoverInterruptedRestore(main)

        assertEquals("видео a", File(main, "R/Download/creator/a.mp4").readText())
        assertEquals("a из бэкапа", File(main, "R/Download/creator/a.info").readText())
        assertFalse(File(main, ".xlr_old_R").exists())
    }
}
