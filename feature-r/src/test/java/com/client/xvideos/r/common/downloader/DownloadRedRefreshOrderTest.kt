package com.client.xvideos.r.common.downloader

import android.content.ContextWrapper
import com.client.xvideos.common.kdownloader.KDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Обходы каталога загрузок R идут по одному (проход 209, T1). Отмена прежнего
 * обхода его не останавливает: в обходе нет точек приостановки, а
 * `MutableStateFlow.emit` отмену не проверяет. Без очереди ранний обход,
 * закончив позже, клал устаревший список поверх свежего.
 */
class DownloadRedRefreshOrderTest {

    private val scope = CoroutineScope(SupervisorJob())

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `следующий обход читает каталог только после того, как прежний опубликовал результат`() {
        val stale = RedDownloadsScan(emptyList(), setOf("Creator/Old"))
        val fresh = RedDownloadsScan(emptyList(), setOf("Creator/Old", "Creator/New"))
        val firstStarted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondRead = CountDownLatch(1)
        val reads = AtomicInteger()
        val keysAtSecondRead = AtomicReference<Set<String>?>()
        lateinit var downloadRed: DownloadRed

        // Первое чтение (обход из init) висит, пока тест его не отпустит, и
        // возвращает каталог до изменения; второе — уже после.
        downloadRed = DownloadRed(Downloader(KDownloader.createForTesting()), scope, ContextWrapper(null)) {
            if (reads.incrementAndGet() == 1) {
                firstStarted.countDown()
                releaseFirst.await(5, TimeUnit.SECONDS)
                stale
            } else {
                keysAtSecondRead.set(downloadRed.downloadedVideoKeys.value)
                secondRead.countDown()
                fresh
            }
        }
        assertTrue(firstStarted.await(5, TimeUnit.SECONDS))

        // Каталог изменился, пока первый обход ещё идёт.
        downloadRed.refreshDownloadList()
        // Без очереди второй обход читает сразу; с очередью он ждёт первого.
        secondRead.await(500, TimeUnit.MILLISECONDS)
        releaseFirst.countDown()

        assertTrue(secondRead.await(5, TimeUnit.SECONDS))
        assertEquals(stale.videoKeys, keysAtSecondRead.get())
        val keys = runBlocking {
            withTimeout(5_000) { downloadRed.downloadedVideoKeys.first { it == fresh.videoKeys } }
        }
        assertEquals(fresh.videoKeys, keys)
    }
}
