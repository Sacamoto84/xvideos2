package com.client.xvideos.common.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Восстановление, которое оборвала гибель процесса.
 *
 * Прежние данные в этот момент лежат в отодвинутой копии `.xlr_old_*`. Раньше
 * их никто не возвращал, а следующее восстановление удаляло как мусор. Журнал
 * восстановления говорит, что делать при следующем запуске: откатить или
 * довести уборку до конца.
 */
class XlrRestoreRecoveryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun File.writeFile(relative: String, text: String): File {
        val file = File(this, relative)
        file.parentFile?.mkdirs()
        file.writeText(text)
        return file
    }

    private fun entry(path: String, hadTarget: Boolean) = XlrBackupManager.RestoreJournalEntry(path, hadTarget)

    private fun File.journal() = File(this, ".xlr_restore_journal")

    @Test
    fun `прерванное восстановление откатывается к прежним данным`() {
        val main = tmp.newFolder("main")
        main.writeFile(".xlr_old_L/прежнее.txt", "прежнее")
        main.writeFile("L/новое.txt", "недописано")
        XlrBackupManager.writeRestoreJournal(main, committed = false, entries = listOf(entry("L", hadTarget = true)))

        XlrBackupManager.recoverInterruptedRestore(main)

        assertEquals("прежнее", File(main, "L/прежнее.txt").readText())
        assertFalse(File(main, "L/новое.txt").exists())
        assertFalse(File(main, ".xlr_old_L").exists())
        assertFalse(main.journal().exists())
    }

    @Test
    fun `цель исчезла, а копия осталась — копия возвращается на место`() {
        val main = tmp.newFolder("main")
        main.writeFile("L/.xlr_old_Likes/прежнее.txt", "прежнее")
        XlrBackupManager.writeRestoreJournal(main, committed = false, entries = listOf(entry("L/Likes", hadTarget = true)))

        XlrBackupManager.recoverInterruptedRestore(main)

        assertEquals("прежнее", File(main, "L/Likes/прежнее.txt").readText())
        assertFalse(File(main, "L/.xlr_old_Likes").exists())
    }

    @Test
    fun `цель, которую не успели отодвинуть, остаётся нетронутой`() {
        val main = tmp.newFolder("main")
        main.writeFile("R/прежнее.txt", "прежнее")
        XlrBackupManager.writeRestoreJournal(main, committed = false, entries = listOf(entry("R", hadTarget = true)))

        XlrBackupManager.recoverInterruptedRestore(main)

        assertEquals("прежнее", File(main, "R/прежнее.txt").readText())
        assertFalse(main.journal().exists())
    }

    @Test
    fun `откат убирает папку, которой до восстановления не было`() {
        val main = tmp.newFolder("main")
        main.writeFile("X/новое.txt", "недописано")
        XlrBackupManager.writeRestoreJournal(main, committed = false, entries = listOf(entry("X", hadTarget = false)))

        XlrBackupManager.recoverInterruptedRestore(main)

        assertFalse(File(main, "X").exists())
    }

    @Test
    fun `завершённое восстановление доводит уборку до конца`() {
        val main = tmp.newFolder("main")
        main.writeFile(".xlr_old_L/прежнее.txt", "прежнее")
        main.writeFile("L/новое.txt", "новое")
        XlrBackupManager.writeRestoreJournal(main, committed = true, entries = listOf(entry("L", hadTarget = true)))

        XlrBackupManager.recoverInterruptedRestore(main)

        assertEquals("новое", File(main, "L/новое.txt").readText())
        assertFalse(File(main, ".xlr_old_L").exists())
        assertFalse(main.journal().exists())
    }

    @Test
    fun `путь за пределами корня в журнале игнорируется`() {
        val main = tmp.newFolder("main")
        val outside = tmp.newFolder("снаружи")
        outside.writeFile("чужое.txt", "чужое")
        XlrBackupManager.writeRestoreJournal(
            main,
            committed = false,
            entries = listOf(entry("../снаружи", hadTarget = false)),
        )

        XlrBackupManager.recoverInterruptedRestore(main)

        assertEquals("чужое", File(outside, "чужое.txt").readText())
    }

    @Test
    fun `без журнала ничего не меняется`() {
        val main = tmp.newFolder("main")
        main.writeFile("L/файл.txt", "данные")

        XlrBackupManager.recoverInterruptedRestore(main)

        assertEquals("данные", File(main, "L/файл.txt").readText())
    }

    @Test
    fun `успешное восстановление не оставляет журнала`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("L/файл.txt", "старое")
        temp.writeFile("L/файл.txt", "новое")

        XlrBackupManager.applyRestoredPaths(main, temp, listOf("L"))

        assertFalse(main.journal().exists())
        assertEquals("новое", File(main, "L/файл.txt").readText())
    }

    @Test
    fun `неудачное восстановление после отката не оставляет журнала`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")
        main.writeFile("L/файл.txt", "старое")
        temp.writeFile("L/файл.txt", "новое")

        val result = runCatching { XlrBackupManager.applyRestoredPaths(main, temp, listOf("L", "../снаружи")) }

        assertTrue(result.isFailure)
        assertFalse(main.journal().exists())
        assertEquals("старое", File(main, "L/файл.txt").readText())
    }
}
