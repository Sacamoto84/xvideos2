package com.client.xvideos.common.backup

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
    fun `отсутствующая в бэкапе папка становится пустой`() {
        val main = tmp.newFolder("main")
        val temp = tmp.newFolder("temp")

        main.writeFile("R/файл.txt", "старое")

        XlrBackupManager.applyRestoredPaths(main, temp, listOf("R"))

        val target = File(main, "R")
        assertTrue(target.isDirectory)
        assertEquals(0, target.listFiles()?.size ?: -1)
    }
}
