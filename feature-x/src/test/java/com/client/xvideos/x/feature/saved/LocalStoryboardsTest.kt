package com.client.xvideos.x.feature.saved

import com.client.xvideos.common.kdownloader.utils.getTempPath
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Сетка кадров скачанного ролика собирается один раз и лежит рядом с `.mp4`.
 * Сборок одного ролика не бывает две, недособранная сетка не выглядит готовой,
 * а сетка удалённого ролика не появляется.
 *
 * Планировщик свой, без `runTest`: тот собирает необработанные исключения всех
 * корутин процесса, а другие тесты X оставляют свои.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalStoryboardsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private lateinit var dir: File
    private var renders = 0

    @Before
    fun setUp() {
        dir = tmp.newFolder("Download")
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    /** Рендер-подделка: пишет [bytes] в выходной файл, ждёт [gate] и отвечает [results] по очереди. */
    private fun renderer(
        gate: CompletableDeferred<Unit>? = null,
        results: ArrayDeque<() -> Boolean> = ArrayDeque(listOf({ true })),
        bytes: ByteArray = byteArrayOf(7, 7),
    ) = StoryboardRenderer { _, out ->
        renders++
        out.writeBytes(bytes)
        gate?.await()
        results.removeFirst().invoke()
    }

    private fun storyboards(renderer: StoryboardRenderer) = LocalStoryboards(dir.path, scope, renderer, dispatcher)

    private fun writeVideo(id: Long) = File(dir, "$id.mp4").writeBytes(byteArrayOf(1))

    private fun ready(id: Long) = File(dir, "$id.storyboard.jpg")

    private fun temp(id: Long) = File(getTempPath(dir.path, "$id.storyboard.jpg"))

    private fun LocalStoryboards.obtainAsync(id: Long): Deferred<File?> = scope.async { obtain(id) }

    @Test
    fun `готовая сетка на диске — без сборки`() {
        writeVideo(5L)
        ready(5L).writeBytes(byteArrayOf(1, 2))

        val result = storyboards(renderer()).obtainAsync(5L)
        scheduler.advanceUntilIdle()

        assertEquals(ready(5L), result.getCompleted())
        assertEquals(0, renders)
    }

    @Test
    fun `нет ролика — null без сборки`() {
        val storyboards = storyboards(renderer())

        val missing = storyboards.obtainAsync(5L)
        val zero = storyboards.obtainAsync(0L)
        scheduler.advanceUntilIdle()

        assertNull(missing.getCompleted())
        assertNull(zero.getCompleted())
        assertEquals(0, renders)
    }

    @Test
    fun `два запроса во время сборки — одна сборка`() {
        writeVideo(5L)
        val gate = CompletableDeferred<Unit>()
        val storyboards = storyboards(renderer(gate))

        val first = storyboards.obtainAsync(5L)
        val second = storyboards.obtainAsync(5L)
        scheduler.advanceUntilIdle()
        assertEquals(1, renders)

        gate.complete(Unit)
        scheduler.advanceUntilIdle()

        assertEquals(ready(5L), first.getCompleted())
        assertEquals(ready(5L), second.getCompleted())
        assertTrue(ready(5L).exists())
        assertFalse(temp(5L).exists())
    }

    @Test
    fun `сбой сборки — null, следующий запрос собирает заново`() {
        writeVideo(5L)
        val storyboards = storyboards(renderer(results = ArrayDeque(listOf({ false }, { true }))))

        val failed = storyboards.obtainAsync(5L)
        scheduler.advanceUntilIdle()
        assertNull(failed.getCompleted())
        assertFalse(temp(5L).exists())

        val retried = storyboards.obtainAsync(5L)
        scheduler.advanceUntilIdle()

        assertEquals(ready(5L), retried.getCompleted())
        assertEquals(2, renders)
    }

    @Test
    fun `исключение рендера — null и без временного файла`() {
        writeVideo(5L)
        val storyboards = storyboards(renderer(results = ArrayDeque(listOf({ throw IllegalStateException("битый файл") }))))

        val result = storyboards.obtainAsync(5L)
        scheduler.advanceUntilIdle()

        assertNull(result.getCompleted())
        assertFalse(ready(5L).exists())
        assertFalse(temp(5L).exists())
    }

    @Test
    fun `остаток оборванной сборки перезаписывается`() {
        writeVideo(5L)
        temp(5L).writeText("мусор от убитого процесса")

        val result = storyboards(renderer(bytes = byteArrayOf(9))).obtainAsync(5L)
        scheduler.advanceUntilIdle()

        assertEquals(ready(5L), result.getCompleted())
        assertArrayEquals(byteArrayOf(9), ready(5L).readBytes())
        assertFalse(temp(5L).exists())
    }

    @Test
    fun `плеер закрыли посреди сборки — сетка всё равно готова`() {
        writeVideo(5L)
        val gate = CompletableDeferred<Unit>()
        val storyboards = storyboards(renderer(gate))

        val player = scope.launch { storyboards.obtain(5L) }
        scheduler.advanceUntilIdle()
        player.cancel()
        gate.complete(Unit)
        scheduler.advanceUntilIdle()

        assertTrue(ready(5L).exists())
        assertFalse(temp(5L).exists())
    }

    @Test
    fun `ролик удалили посреди сборки — ни сетки, ни временного файла`() {
        writeVideo(5L)
        val gate = CompletableDeferred<Unit>()
        val storyboards = storyboards(renderer(gate))

        val result = storyboards.obtainAsync(5L)
        scheduler.advanceUntilIdle()
        assertTrue("рендер уже пишет временный файл", temp(5L).exists())

        File(dir, "5.mp4").delete()
        storyboards.cancel(5L)
        gate.complete(Unit)
        scheduler.advanceUntilIdle()

        assertNull(result.getCompleted())
        assertFalse(ready(5L).exists())
        assertFalse(temp(5L).exists())
    }
}
