package com.client.xvideos.xlive.record

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout
import java.io.File

// Без runTest: свой планировщик, время двигает тест (как в остальных тестах раздела).
@OptIn(ExperimentalCoroutinesApi::class)
class XLiveRecorderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Зависший цикл записи не должен держать сборку. */
    @get:Rule
    val timeout: Timeout = Timeout.seconds(20)

    private val scheduler = TestCoroutineScheduler()
    private val scope = TestScope(StandardTestDispatcher(scheduler))
    private val net = FakeRecordNet { scheduler.currentTime }

    private val video by lazy { tmp.newFile("video.part") }
    private val audio by lazy { tmp.newFile("audio.part") }

    private fun recorder(usable: Long = Long.MAX_VALUE) = XLiveRecorder(
        username = "room_a",
        videoOut = video,
        audioOut = audio,
        net = net,
        usableBytes = { usable },
        nowMs = { scheduler.currentTime },
    )

    private fun XLiveRecorder.start(): Deferred<XLiveRecordStop> = scope.async { run() }

    private fun advance(ms: Long) {
        scheduler.advanceTimeBy(ms)
        scheduler.runCurrent()
    }

    private fun stop(recorder: XLiveRecorder, result: Deferred<XLiveRecordStop>): XLiveRecordStop {
        recorder.requestStop(XLiveRecordStop.BUTTON)
        scheduler.advanceUntilIdle()
        return result.getCompleted()
    }

    private fun File.text() = readText()

    @Test
    fun `пишет init и сегменты по порядку, обе дорожки`() {
        val recorder = recorder()
        val result = recorder.start()
        advance(3_300)

        assertEquals(XLiveRecordStop.BUTTON, stop(recorder, result))
        assertEquals("vinit" + "v100" + "v101" + "v102", video.text())
        assertEquals("ainit" + "a100" + "a101" + "a102", audio.text())
        assertEquals(0, recorder.stats.value.gaps)
        assertEquals(1080, recorder.stats.value.height)
    }

    @Test
    fun `скачок номеров — пропуск считается, запись идёт дальше`() {
        val recorder = recorder()
        val result = recorder.start()
        advance(1_700)
        net.jump = 6
        advance(1_700)
        stop(recorder, result)

        assertEquals(1, recorder.stats.value.gaps)
        assertTrue(video.text().contains("v107"))
        assertFalse(video.text().contains("v102"))
    }

    @Test
    fun `403 на плейлисте — свежий адрес, номера без пропуска`() {
        val recorder = recorder()
        val result = recorder.start()
        advance(1_700)
        net.expireBefore = 2
        advance(3_300)
        stop(recorder, result)

        assertEquals(2, net.roomCalls)
        assertEquals("vinit" + "v100" + "v101" + "v102" + "v103", video.text())
        assertEquals(0, recorder.stats.value.gaps)
    }

    @Test
    fun `новый адрес с номерами заново — запись с края, без зацикливания`() {
        val recorder = recorder()
        val result = recorder.start()
        advance(1_700)
        net.numberShift = { gen -> if (gen >= 2) -95L else 0L }
        net.expireBefore = 2
        advance(3_300)
        stop(recorder, result)

        assertEquals(1, recorder.stats.value.gaps)
        assertTrue(video.text().contains("v8"))
        assertTrue("запросов плейлиста: ${net.textCalls}", net.textCalls < 40)
    }

    @Test
    fun `комната не в эфире при обновлении адреса — эфир закончился`() {
        val recorder = recorder()
        val result = recorder.start()
        advance(1_700)
        net.roomStatus = "offline"
        net.expireBefore = 2
        advance(2_000)

        assertEquals(XLiveRecordStop.STREAM_ENDED, result.getCompleted())
        assertEquals("vinit" + "v100" + "v101", video.text())
    }

    @Test
    fun `сеть пропала дольше предела — нет сети`() {
        val recorder = recorder()
        val result = recorder.start()
        advance(1_700)
        net.networkDown = true
        advance(27_000)

        assertEquals(XLiveRecordStop.NO_NETWORK, result.getCompleted())
        assertTrue(video.text().startsWith("vinit" + "v100" + "v101"))
    }

    @Test
    fun `короткий сбой сети — запись продолжается`() {
        val recorder = recorder()
        val result = recorder.start()
        advance(1_700)
        net.failNext = 3
        advance(6_500)
        stop(recorder, result)

        assertTrue(video.text().contains("v104"))
    }

    @Test
    fun `мало места — остановка до записи сегмента`() {
        val recorder = recorder(usable = 100)
        val result = recorder.start()
        advance(500)

        assertEquals(XLiveRecordStop.NO_SPACE, result.getCompleted())
        assertFalse(video.text().contains("v100"))
    }

    @Test
    fun `после 403 сайт комнаты недоступен — запросы с паузой, через предел нет сети`() {
        val recorder = recorder()
        val result = recorder.start()
        advance(1_700)
        net.expireBefore = 2
        net.roomDown = true
        advance(30_000)

        assertEquals(XLiveRecordStop.NO_NETWORK, result.getCompleted())
        assertTrue("запросов комнаты: ${net.roomCalls}", net.roomCalls < 100)
    }

    @Test
    fun `перенумерация на новом адресе — обе дорожки продолжают с края`() {
        net.recheckAfterWait = false
        val recorder = recorder()
        val result = recorder.start()
        advance(1_700)
        net.numberShift = { gen -> if (gen >= 2) -95L else 0L }
        net.expireBefore = 2
        advance(5_000)
        stop(recorder, result)

        assertTrue(video.text().contains("v8"))
        assertTrue("звук не застрял на старом номере", audio.text().contains("a8"))
    }

    /** Номер последнего записанного сегмента дорожки [kind] ("v" или "a"). */
    private fun File.lastNumber(kind: String): Long =
        Regex("$kind(\\d+)").findAll(text()).last().groupValues[1].toLong()

    @Test
    fun `номера на том же адресе начались заново — 400 и запись с края`() {
        val recorder = recorder()
        val result = recorder.start()
        advance(1_700)
        net.numberShift = { -95L }
        advance(14_000)
        stop(recorder, result)

        assertTrue("видео: ${video.lastNumber("v")}", video.lastNumber("v") < 50)
        assertTrue("звук: ${audio.lastNumber("a")}", audio.lastNumber("a") < 50)
        assertTrue(recorder.stats.value.gaps >= 1)
    }

    @Test
    fun `сбой записи на диск — остановка с сохранением, без исключения`() {
        val recorder = XLiveRecorder(
            username = "room_a",
            videoOut = File(tmp.root, "нет-папки/video.part"),
            audioOut = audio,
            net = net,
            usableBytes = { Long.MAX_VALUE },
            nowMs = { scheduler.currentTime },
        )
        val result = recorder.start()
        advance(1_700)

        assertEquals(XLiveRecordStop.NO_SPACE, result.getCompleted())
    }

    @Test
    fun `остановка снаружи возвращает её причину`() {
        val recorder = recorder()
        val result = recorder.start()
        advance(500)
        recorder.requestStop(XLiveRecordStop.LEFT_ROOM)
        scheduler.advanceUntilIdle()

        assertEquals(XLiveRecordStop.LEFT_ROOM, result.getCompleted())
    }
}
