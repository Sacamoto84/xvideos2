package com.client.xvideos.xlive.record

import com.client.xvideos.common.snackbar.UiMessage
import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.XLiveRecord
import com.client.xvideos.xlive.model.XLiveRecordStatus
import com.client.xvideos.xlive.model.XLiveRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.io.IOException

// Без runTest: свой планировщик, время двигает тест.
@OptIn(ExperimentalCoroutinesApi::class)
class XLiveRecordingsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(20)

    private val scheduler = TestCoroutineScheduler()
    private val scope = TestScope(StandardTestDispatcher(scheduler))
    private val net = FakeRecordNet { scheduler.currentTime }
    private val messages = mutableListOf<UiMessage>()
    private val muxed = mutableListOf<Pair<File, File?>>()
    private var muxFails = false

    private val muxer = XLiveMuxer { video, audio, out ->
        if (muxFails) throw IOException("muxer")
        muxed += video to audio
        out.writeText("muxed")
        1_234L
    }

    private val dir by lazy { tmp.newFolder("records") }
    private var clockId = 1_000L

    private fun core(): XLiveRecordingsCore {
        val store = XLiveRecordStore(dir.path, scope, Dispatchers.Unconfined)
        return XLiveRecordingsCore(
            store = store,
            scope = scope,
            net = net,
            muxer = muxer,
            usableBytes = { Long.MAX_VALUE },
            // Остальное — подмены: часы планировщика, без снимка и без EventBus.
            newId = { clockId++ },
            elapsedMs = { scheduler.currentTime },
            fetchSnapshot = { null },
            notify = { messages += it },
        )
    }

    private val room = XLiveRoom(status = "public", hlsUrl = "", username = "room_a", title = "тема #тег", viewers = 5)

    private fun advance(ms: Long) {
        scheduler.advanceTimeBy(ms)
        scheduler.runCurrent()
    }

    private fun XLiveRecordingsCore.only(): XLiveRecord = store.records.value.single()

    @Test
    fun `остановка склеивает, помечает готовой и убирает сырые файлы`() {
        val core = core()
        val session = core.start(room, null)
        advance(1_700)
        assertTrue(session.state.value.isRecording)

        session.stop(XLiveRecordStop.BUTTON)
        scheduler.advanceUntilIdle()

        val record = core.only()
        val folder = core.store.dirOf(record.id)
        assertEquals(XLiveRecordStatus.READY, record.status)
        assertEquals("muxed", File(folder, XLiveRecordStore.VIDEO).readText())
        assertEquals(5L, record.sizeBytes)
        assertEquals(1_234L, record.durationMs)
        assertEquals(1080, record.height)
        assertEquals("тема #тег", record.room.title)
        assertEquals(listOf(XLiveRecordStore.RECORD_JSON, XLiveRecordStore.VIDEO), folder.list()!!.sorted())
        assertEquals(listOf<UiMessage>(UiMessage.Success("Запись сохранена")), messages)
        assertFalse(session.state.value.isRecording || session.state.value.isFinishing)
    }

    @Test
    fun `без единого сегмента видео запись удаляется`() {
        val core = core()
        val session = core.start(room, null)
        session.stop(XLiveRecordStop.BUTTON)
        scheduler.advanceUntilIdle()

        assertTrue(core.store.records.value.isEmpty())
        assertEquals(0, dir.list()!!.size)
        assertEquals(listOf<UiMessage>(UiMessage.Error("Запись не сохранена: нет данных")), messages)
    }

    @Test
    fun `склейка не удалась — запись не завершена, сырые файлы целы`() {
        muxFails = true
        val core = core()
        val session = core.start(room, null)
        advance(1_700)
        session.stop(XLiveRecordStop.BUTTON)
        scheduler.advanceUntilIdle()

        val record = core.only()
        assertEquals(XLiveRecordStatus.BROKEN, record.status)
        assertTrue(File(core.store.dirOf(record.id), XLiveRecordStore.VIDEO_RAW).length() > 0)
        assertEquals(listOf<UiMessage>(UiMessage.Error("Запись не склеилась")), messages)
    }

    private fun leftover(id: Long, withMuxTemp: Boolean = false) {
        val folder = File(dir, id.toString()).apply { mkdirs() }
        File(folder, XLiveRecordStore.RECORD_JSON).writeText("""{"id":$id,"username":"room_b","status":"RECORDING"}""")
        File(folder, XLiveRecordStore.VIDEO_RAW).writeText("vinitv100")
        File(folder, XLiveRecordStore.AUDIO_RAW).writeText("ainita100")
        if (withMuxTemp) File(folder, XLiveRecordStore.MUX_TEMP).writeText("обрывок")
    }

    @Test
    fun `доклейка доделывает запись, оборванную вместе с процессом`() {
        leftover(7)
        val core = core()

        core.finishPending()
        scheduler.advanceUntilIdle()

        val record = core.only()
        assertEquals(XLiveRecordStatus.READY, record.status)
        assertEquals(listOf(XLiveRecordStore.RECORD_JSON, XLiveRecordStore.VIDEO), File(dir, "7").list()!!.sorted())
        assertTrue("доклейка — без снекбара", messages.isEmpty())
    }

    @Test
    fun `доклейка переписывает обрывок прежней склейки`() {
        leftover(7, withMuxTemp = true)
        val core = core()

        core.finishPending()
        scheduler.advanceUntilIdle()

        assertEquals("muxed", File(dir, "7/${XLiveRecordStore.VIDEO}").readText())
        assertFalse(File(dir, "7/${XLiveRecordStore.MUX_TEMP}").exists())
    }

    @Test
    fun `доклейка без сырого видео удаляет запись`() {
        leftover(7)
        File(dir, "7/${XLiveRecordStore.VIDEO_RAW}").delete()
        val core = core()

        core.finishPending()
        scheduler.advanceUntilIdle()

        assertTrue(core.store.records.value.isEmpty())
        assertFalse(File(dir, "7").exists())
    }

    @Test
    fun `доклейка не трогает идущую запись`() {
        val core = core()
        val session = core.start(room, null)
        advance(1_700)

        core.finishPending()
        advance(100)

        assertEquals(XLiveRecordStatus.RECORDING, core.only().status)
        assertTrue(muxed.isEmpty())
        session.stop(XLiveRecordStop.BUTTON)
        scheduler.advanceUntilIdle()
    }

    @Test
    fun `новая запись останавливает прежнюю как уход из комнаты`() {
        val core = core()
        val first = core.start(room, null)
        advance(1_700)

        val second = core.start(room.copy(username = "room_b"), null)
        advance(1_700)

        assertEquals(XLiveRecordStatus.READY, core.store.records.value.single { it.username == "room_a" }.status)
        assertTrue(second.state.value.isRecording)
        assertFalse(first.state.value.isRecording)
        second.stop(XLiveRecordStop.BUTTON)
        scheduler.advanceUntilIdle()
    }

    @Test
    fun `анкета, пришедшая после старта, дописывается в запись`() {
        val core = core()
        val session = core.start(room, null)
        session.attachBio(XLiveBio(realName = "Anna", age = 24))
        advance(100)

        assertEquals(24, core.only().bio?.age)
        session.stop(XLiveRecordStop.BUTTON)
        scheduler.advanceUntilIdle()
    }
}
