package com.client.xvideos.common.backup

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Перенос распакованного бэкапа поверх текущих данных.
 *
 * Раньше целевая папка удалялась до переноса: сбой посреди цикла оставлял
 * пользователя без части данных и без отката. Тесты фиксируют, что откат есть.
 */
class XlrRestoreApplyTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun File.writeFile(relative: String, text: String): File {
        val file = File(this, relative)
        file.parentFile?.mkdirs()
        file.writeText(text)
        return file
    }

    @Test
    fun `восстановление заменяет содержимое папок`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")

        main.writeFile("L/старое.txt", "старое")
        temp.writeFile("L/новое.txt", "новое")

        XlrBackupManager.applyRestoredPaths(main, temp, listOf("L"))

        assertEquals("новое", File(main, "L/новое.txt").readText())
        assertFalse("старое содержимое должно быть заменено", File(main, "L/старое.txt").exists())
    }

    @Test
    fun `после успеха отодвинутых копий не остаётся`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")

        main.writeFile("L/файл.txt", "старое")
        temp.writeFile("L/файл.txt", "новое")

        XlrBackupManager.applyRestoredPaths(main, temp, listOf("L"))

        val leftovers = main.listFiles()?.filter { it.name.startsWith(".xlr_old_") }.orEmpty()
        assertTrue("временные копии должны быть удалены: $leftovers", leftovers.isEmpty())
    }

    @Test
    fun `сбой на втором пути откатывает первый`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")

        main.writeFile("L/файл.txt", "старое")
        temp.writeFile("L/файл.txt", "новое")

        // Второй путь ведёт за пределы корня — applyRestoredPaths обязан упасть
        // уже после того, как первый путь перенесён.
        val bad = "../снаружи"

        runCatching { XlrBackupManager.applyRestoredPaths(main, temp, listOf("L", bad)) }
            .onSuccess { error("ожидалось падение на пути за пределами корня") }

        assertEquals(
            "данные первого пути должны вернуться к исходным",
            "старое",
            File(main, "L/файл.txt").readText()
        )
        val leftovers = main.listFiles()?.filter { it.name.startsWith(".xlr_old_") }.orEmpty()
        assertTrue("после отката временных копий быть не должно: $leftovers", leftovers.isEmpty())
    }

    @Test
    fun `откат убирает папку, которой до восстановления не существовало`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")

        // X появляется только из бэкапа: отодвигать нечего, значит в movedAside
        // записи не будет и очистить target при откате может только `written`.
        temp.writeFile("X/новое.txt", "новое")

        // Второй путь ведёт за пределы корня — падение случится после того, как
        // X уже перенесён.
        runCatching { XlrBackupManager.applyRestoredPaths(main, temp, listOf("X", "../снаружи")) }
            .onSuccess { error("ожидалось падение на пути за пределами корня") }

        assertFalse(
            "папка, созданная только этим восстановлением, должна быть убрана",
            File(main, "X").exists()
        )
    }

    @Test
    fun `отсутствующая в бэкапе папка становится пустой`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")

        main.writeFile("R/файл.txt", "старое")

        XlrBackupManager.applyRestoredPaths(main, temp, listOf("R"))

        val target = File(main, "R")
        assertTrue(target.isDirectory)
        assertEquals(0, target.listFiles()?.size ?: -1)
    }

    @Test
    fun `shouldIncludeBackupEntry исключает скрытые файлы и служебные папки с точкой`() {
        val options = XlrBackupOptions()
        assertTrue(XlrBackupManager.shouldIncludeBackupEntry("L/likes/metadata.json", options))
        assertTrue(XlrBackupManager.shouldIncludeBackupEntry("R/downloads/video.mp4", options))
        assertTrue(XlrBackupManager.shouldIncludeBackupEntry("X/history.json", options))

        // Служебные папки откатов и скрытые файлы
        assertFalse(XlrBackupManager.shouldIncludeBackupEntry("L/.xlr_old_12345/data.txt", options))
        assertFalse(XlrBackupManager.shouldIncludeBackupEntry("R/.xlr_old_67890", options))
        assertFalse(XlrBackupManager.shouldIncludeBackupEntry("X/.nomedia", options))
        assertFalse(XlrBackupManager.shouldIncludeBackupEntry("L/sub/.cache/temp.bin", options))
    }

    @Test
    fun `currentBackupItems исключает служебные папки с точкой из списка`() = runBlocking {
        val base = tmp.newFolder("backup_base")
        base.writeFile("L/visible_folder/data.txt", "123")
        base.writeFile("L/.xlr_old_failed_rollback/stale.txt", "456")
        base.writeFile("L/.nomedia", "")

        val items = XlrBackupManager.currentBackupItems(baseDir = base)
        val lPaths = items.filter { it.section == "L" }.map { it.path }

        assertTrue("видимая папка должна быть в списке: $lPaths", lPaths.contains("L/visible_folder"))
        assertFalse("служебная папка отката не должна быть в списке: $lPaths", lPaths.any { it.contains(".xlr_old_") })
        assertFalse("скрытые элементы не должны быть в списке: $lPaths", lPaths.any { it.contains("/.") })
    }
}

