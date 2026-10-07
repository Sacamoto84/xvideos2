package com.client.xvideos.screenSettings.backup

import com.client.xvideos.common.backup.XlrBackupContentMode
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.common.backup.XlrBackupOptions
import com.client.xvideos.common.backup.XlrBackupReport
import com.client.xvideos.common.backup.XlrBackupType
import com.client.xvideos.common.backup.XlrRestoreMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Операции бэкапа живут в держателе вне композиции: пересоздание экрана не
 * теряет ни пароль, ни признак идущей операции. Раньше всё это лежало в
 * состоянии Composable — потерянный пароль давал незашифрованный архив, а
 * признак операции после пересоздания залипал и запирал страницу.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupControllerTest {

    /** Движок бэкапа: записывает вызовы и отвечает так, как велит тест. */
    private class FakeEngine : BackupEngine {
        var items = listOf(
            XlrBackupItem(path = "X", title = "X", section = "X", files = 1, bytes = 10),
            XlrBackupItem(path = "L", title = "L", section = "L", files = 2, bytes = 20),
        )
        var archiveType = XlrBackupType.ENCRYPTED_XLR
        var inspectError: Throwable? = null
        var createGate: CompletableDeferred<Unit>? = null
        var afterRestoreGate: CompletableDeferred<Unit>? = null

        val createdWith = mutableListOf<CharArray>()
        val inspectedWith = mutableListOf<String?>()
        val restoredWith = mutableListOf<String?>()
        val restoredModes = mutableListOf<XlrRestoreMode>()

        override suspend fun currentItems(options: XlrBackupOptions): List<XlrBackupItem> = items

        override suspend fun create(
            uri: String,
            paths: Set<String>,
            options: XlrBackupOptions,
            password: CharArray,
        ): Result<XlrBackupReport> {
            createdWith += password.copyOf()
            createGate?.await()
            return Result.success(XlrBackupReport(files = 3, bytes = 30))
        }

        override suspend fun detectType(uri: String): XlrBackupType = archiveType

        override suspend fun inspect(uri: String, password: CharArray?): Result<List<XlrBackupItem>> {
            inspectedWith += password?.concatToString()
            inspectError?.let { return Result.failure(it) }
            return Result.success(items)
        }

        override suspend fun restore(
            uri: String,
            paths: Set<String>,
            password: CharArray?,
            mode: XlrRestoreMode,
        ): Result<XlrBackupReport> {
            restoredWith += password?.concatToString()
            restoredModes += mode
            return if (password == null && archiveType == XlrBackupType.ENCRYPTED_XLR) {
                Result.failure(IllegalStateException("Password required"))
            } else {
                Result.success(XlrBackupReport(files = 3, bytes = 30))
            }
        }

        override suspend fun afterRestore(paths: Set<String>, log: (String) -> Unit) {
            afterRestoreGate?.await()
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val engine = FakeEngine()

    private fun TestScope.controller() = BackupController(engine, scope = this, mainDispatcher = dispatcher)

    // --- Создание ---

    @Test
    fun `без пароля архив не пишется вовсе`() = runTest(dispatcher) {
        val backup = controller()
        advanceUntilIdle()

        // Пароль потерян: выбор файла вернулся, а пароля в держателе нет.
        backup.createBackup("content://backup.xlr")
        advanceUntilIdle()

        assertTrue("архив записан без пароля", engine.createdWith.isEmpty())
        assertFalse(backup.isWorking)
        assertTrue(backup.console.last(), backup.console.last().contains("не создан"))
    }

    @Test
    fun `пароль, введённый до выбора файла, доходит до записи архива`() = runTest(dispatcher) {
        val backup = controller()
        advanceUntilIdle()

        backup.setCreatePassword("secret".toCharArray())
        // Между вводом пароля и выбором файла экран могли пересоздать — держателю всё равно.
        backup.createBackup("content://backup.xlr")
        advanceUntilIdle()

        assertArrayEquals("secret".toCharArray(), engine.createdWith.single())
    }

    @Test
    fun `отмена выбора файла стирает введённый пароль`() = runTest(dispatcher) {
        val backup = controller()
        advanceUntilIdle()
        backup.setCreatePassword("secret".toCharArray())

        backup.createBackup(uri = null)
        backup.createBackup("content://backup.xlr")
        advanceUntilIdle()

        assertTrue(engine.createdWith.isEmpty())
    }

    @Test
    fun `признак операции поднят, пока архив пишется, и снят по окончании`() = runTest(dispatcher) {
        engine.createGate = CompletableDeferred()
        val backup = controller()
        advanceUntilIdle()
        backup.setCreatePassword("secret".toCharArray())

        backup.createBackup("content://backup.xlr")
        advanceUntilIdle()
        assertTrue(backup.isWorking)

        engine.createGate?.complete(Unit)
        advanceUntilIdle()
        assertFalse("признак операции остался поднятым", backup.isWorking)
    }

    @Test
    fun `вторая операция не начинается, пока идёт первая`() = runTest(dispatcher) {
        engine.createGate = CompletableDeferred()
        val backup = controller()
        advanceUntilIdle()
        backup.setCreatePassword("secret".toCharArray())
        backup.createBackup("content://backup.xlr")
        advanceUntilIdle()

        backup.setCreatePassword("other".toCharArray())
        backup.createBackup("content://second.xlr")
        advanceUntilIdle()

        assertEquals(1, engine.createdWith.size)
        engine.createGate?.complete(Unit)
        advanceUntilIdle()
    }

    // --- Выбор папок ---

    @Test
    fun `снятый выбор не возвращается при смене режима бэкапа`() = runTest(dispatcher) {
        val backup = controller()
        advanceUntilIdle()
        assertEquals(setOf("X", "L"), backup.selectedBackupPaths)

        backup.selectNoneBackup()
        backup.rMode = XlrBackupContentMode.FULL
        backup.refreshBackupItems()
        advanceUntilIdle()

        assertTrue("после смены режима все папки снова отмечены", backup.selectedBackupPaths.isEmpty())
    }

    // --- Восстановление ---

    @Test
    fun `повторное восстановление из открытого зашифрованного архива использует тот же пароль`() = runTest(dispatcher) {
        val backup = controller()
        advanceUntilIdle()
        backup.openArchive("content://backup.xlr")
        advanceUntilIdle()
        assertTrue(backup.showRestorePasswordDialog)
        backup.decryptArchive("secret".toCharArray())
        advanceUntilIdle()

        backup.restore(XlrRestoreMode.REPLACE)
        advanceUntilIdle()
        backup.restore(XlrRestoreMode.REPLACE)
        advanceUntilIdle()

        assertEquals(listOf<String?>("secret", "secret"), engine.restoredWith)
        assertEquals(2, backup.restoreCount)
    }

    @Test
    fun `выбранный режим восстановления доходит до движка и до консоли`() = runTest(dispatcher) {
        val backup = controller()
        advanceUntilIdle()
        backup.openArchive("content://backup.xlr")
        advanceUntilIdle()
        backup.decryptArchive("secret".toCharArray())
        advanceUntilIdle()

        backup.restore(XlrRestoreMode.MERGE)
        advanceUntilIdle()
        backup.restore(XlrRestoreMode.REPLACE)
        advanceUntilIdle()

        assertEquals(listOf(XlrRestoreMode.MERGE, XlrRestoreMode.REPLACE), engine.restoredModes)
        val started = backup.console.filter { it.startsWith("Восстановление backup") }
        assertEquals(2, started.size)
        assertTrue("в консоли не видно, что шло объединение: ${started[0]}", "объединение" in started[0])
        assertTrue("в консоли не видно, что шла замена: ${started[1]}", "замена" in started[1])
    }

    @Test
    fun `закрытие архива стирает пароль и список папок`() = runTest(dispatcher) {
        val backup = controller()
        advanceUntilIdle()
        backup.openArchive("content://backup.xlr")
        advanceUntilIdle()
        backup.decryptArchive("secret".toCharArray())
        advanceUntilIdle()

        backup.closeArchive()
        backup.restore(XlrRestoreMode.REPLACE)
        advanceUntilIdle()

        assertNull(backup.restoreUri)
        assertTrue(backup.restoreItems.isEmpty())
        assertTrue("восстановление без открытого архива дошло до движка", engine.restoredWith.isEmpty())
    }

    @Test
    fun `файл неподдерживаемого формата закрывает открытый прежде архив`() = runTest(dispatcher) {
        engine.archiveType = XlrBackupType.LEGACY_ZIP
        val backup = controller()
        advanceUntilIdle()
        backup.openArchive("content://backup.zip")
        advanceUntilIdle()
        assertEquals("content://backup.zip", backup.restoreUri)

        engine.archiveType = XlrBackupType.UNSUPPORTED
        backup.openArchive("content://photo.jpg")
        advanceUntilIdle()

        assertNull("прежний архив остался открытым", backup.restoreUri)
        assertTrue(backup.restoreItems.isEmpty())
        assertTrue(backup.selectedRestorePaths.isEmpty())
        assertFalse(backup.isWorking)
    }

    @Test
    fun `архив, который не удалось прочитать, закрывает открытый прежде`() = runTest(dispatcher) {
        engine.archiveType = XlrBackupType.LEGACY_ZIP
        val backup = controller()
        advanceUntilIdle()
        backup.openArchive("content://backup.zip")
        advanceUntilIdle()
        assertEquals("content://backup.zip", backup.restoreUri)

        engine.inspectError = IllegalStateException("архив повреждён")
        backup.openArchive("content://broken.zip")
        advanceUntilIdle()

        assertNull("прежний архив остался открытым", backup.restoreUri)
        assertTrue(backup.restoreItems.isEmpty())
        assertFalse(backup.isWorking)
    }

    @Test
    fun `пока идёт докачка после восстановления, новый бэкап и восстановление не начинаются`() = runTest(dispatcher) {
        engine.archiveType = XlrBackupType.LEGACY_ZIP
        engine.afterRestoreGate = CompletableDeferred()
        val backup = controller()
        advanceUntilIdle()
        backup.openArchive("content://backup.zip")
        advanceUntilIdle()

        backup.restore(XlrRestoreMode.REPLACE)
        advanceUntilIdle()
        assertFalse("архив уже распакован — страницу запирать незачем", backup.isWorking)
        assertTrue(backup.isRecovering)

        backup.restore(XlrRestoreMode.REPLACE)
        backup.setCreatePassword("secret".toCharArray())
        backup.createBackup("content://backup.xlr")
        advanceUntilIdle()
        assertEquals(1, engine.restoredWith.size)
        assertTrue(engine.createdWith.isEmpty())

        engine.afterRestoreGate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(backup.isRecovering)
    }
}
