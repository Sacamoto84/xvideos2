package com.client.xvideos.r.common.block

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.r.common.block.useCase.blockGetAllBlockedGifsInfo
import com.client.xvideos.r.common.block.useCase.getBlockFile
import com.client.xvideos.r.common.block.useCase.unblockItem
import com.client.xvideos.r.model.GifsInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Блокировки R: каждая блокировка правит набор в памяти сама, не перечитывая
 * каталог. Раньше она запускала полное чтение, и чтения публиковались по мере
 * готовности — раннее могло лечь поверх позднего и вернуть ролик в ленты.
 */
class BlockRedMutationTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Чтение каталога блокировок, которое тест может придержать; вызовы считаются. */
    private class DiskReads {
        val count = AtomicInteger()

        /** Защёлка для ближайшего чтения: оно прочитает диск и повиснет до её открытия. */
        @Volatile
        var holdNext: CountDownLatch? = null

        fun read(): List<GifsInfo> {
            count.incrementAndGet()
            val snapshot = blockGetAllBlockedGifsInfo()
            holdNext?.let { gate ->
                holdNext = null
                gate.await(5, TimeUnit.SECONDS)
            }
            return snapshot
        }
    }

    private val reads = DiskReads()

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_r_block")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun gif(id: String) = GifsInfo(id = id, userName = "author")

    private suspend fun awaitUntil(condition: () -> Boolean) {
        withTimeout(5_000) { while (!condition()) delay(10) }
    }

    /** Сколько раз каталог прочитан к началу теста: чтение при создании и контрольное. */
    private var readsAtStart = 0

    private suspend fun blockRed(): BlockRed {
        val block = BlockRed(scope, reads::read)
        // Стартовое чтение уже внутри замка — контрольное встанет за ним, и по
        // его завершении закончены оба. Без ожидания контрольное могло
        // проскочить первым, а стартовое — прийти посреди теста.
        awaitUntil { reads.count.get() >= 1 }
        block.refresh().join()
        readsAtStart = reads.count.get()
        return block
    }

    @Test
    fun `блокировка не перечитывает каталог блокировок`() = runBlocking {
        val block = blockRed()

        block.blockItem(gif("a"))
        block.blockItem(gif("b"))
        awaitUntil { block.blockedIds.value == setOf("a", "b") }

        assertEquals("каталог перечитан после блокировки", readsAtStart, reads.count.get())
        assertEquals(listOf("a", "b"), block.blockList.value.map { it.id })
        assertTrue(getBlockFile("author", "a")?.exists() == true)
        assertTrue(getBlockFile("author", "b")?.exists() == true)
    }

    @Test
    fun `разблокировка убирает ролик из набора без перечитывания каталога`() = runBlocking {
        val block = blockRed()
        block.blockItem(gif("a"))
        block.blockItem(gif("b"))
        awaitUntil { block.blockedIds.value == setOf("a", "b") }

        block.unblockItem(gif("a"))
        awaitUntil { block.blockedIds.value == setOf("b") }

        assertEquals(readsAtStart, reads.count.get())
        assertEquals(listOf("b"), block.blockList.value.map { it.id })
        assertTrue(getBlockFile("author", "a")?.exists() == false)
    }

    @Test
    fun `перечитывание, начатое до блокировки, не теряет её`() = runBlocking {
        val block = blockRed()

        // Перечитывание прочитало пустой каталог и ещё не опубликовало результат.
        val gate = CountDownLatch(1)
        reads.holdNext = gate
        val refresh = block.refresh()
        awaitUntil { reads.count.get() == readsAtStart + 1 }

        block.blockItem(gif("a"))
        delay(200)
        gate.countDown()
        refresh.join()

        awaitUntil { block.blockedIds.value == setOf("a") }
        delay(200)
        assertEquals("устаревшее чтение легло поверх блокировки", setOf("a"), block.blockedIds.value)
    }

    @Test
    fun `разблокировка сообщает об отказе, если файл блокировки не удалился`() {
        // Каталог с содержимым на месте файла блокировки: delete() его не удалит.
        val blockFile = getBlockFile("author", "stuck") ?: error("нет пути к файлу блокировки")
        File(blockFile, "content").apply { parentFile?.mkdirs() }.writeText("x")

        val result = unblockItem(gif("stuck"))

        assertTrue("разблокировка сообщила об успехе, хотя файл на месте", result.isFailure)
        assertTrue(blockFile.exists())
    }
}
