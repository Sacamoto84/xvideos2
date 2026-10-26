package com.client.xvideos.xlive.ui.record

import android.content.ContextWrapper
import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.XLiveRecord
import com.client.xvideos.xlive.model.XLiveRecordStatus
import com.client.xvideos.xlive.model.isRecordPlaybackFinished
import com.client.xvideos.xlive.record.XLiveRecordStore
import com.client.xvideos.xlive.saved.testRoomStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// Без runTest: неограниченный диспетчер гонит корутины screenModelScope внутри вызова.
@OptIn(ExperimentalCoroutinesApi::class)
class XLiveRecordSMTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val records by lazy { XLiveRecordStore(tmp.newFolder("records").path, CoroutineScope(Job()), Dispatchers.Unconfined) }
    private val saved by lazy { testRoomStore(tmp.newFolder("my")) }
    private val storyboardFile by lazy { File(tmp.root, "sb.jpg") }

    private val record = XLiveRecord(
        id = 7L,
        username = "room_a",
        status = XLiveRecordStatus.READY,
        bio = XLiveBio(realName = "Anna", age = 24),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun model(id: Long = 7L) = XLiveRecordSM(
        id = id,
        store = records,
        saved = saved,
        loadStoryboard = { if (it == 7L) storyboardFile else null },
    )

    @Test
    fun `запись открывается без звука, кнопка его включает`() {
        records.put(record)
        val sm = model()
        assertTrue(sm.muted.value)

        sm.toggleMute()

        assertFalse(sm.muted.value)
    }

    @Test
    fun `позиция просмотра пишется в запись`() {
        records.put(record)
        val sm = model()

        sm.savePosition(12.5f)

        assertEquals(12.5f, records.records.value.single().positionSec)
    }

    @Test
    fun `позиция в конце сбрасывается в начало`() {
        records.put(record.copy(durationMs = 26_000L))
        val sm = model()

        // 25.5с из 26с — досмотрено до конца, сброс в 0
        sm.savePosition(25.5f, 26)
        assertEquals(0f, records.records.value.single().positionSec)

        // 10с из 26с — середина, сохраняется как есть
        sm.savePosition(10f, 26)
        assertEquals(10f, records.records.value.single().positionSec)
    }

    @Test
    fun `isRecordPlaybackFinished правильно определяет конец воспроизведения`() {
        // Досмотрено почти до конца (в пределах 2 сек)
        assertTrue(isRecordPlaybackFinished(25f, 26f))
        // Досмотрено более 95%
        assertTrue(isRecordPlaybackFinished(96f, 100f))
        // В середине
        assertFalse(isRecordPlaybackFinished(10f, 26f))
        // Нулевая позиция
        assertFalse(isRecordPlaybackFinished(0f, 26f))
        // Нулевая длительность
        assertFalse(isRecordPlaybackFinished(10f, 0f))
    }

    @Test
    fun `сохранение в галерею передаёт файл записи и чистое имя`() {
        records.put(record)
        val sm = model()

        var savedFile: File? = null
        var savedName: String? = null
        val dummyContext = object : ContextWrapper(null) {}

        sm.saveToGallery(dummyContext) { _, file, name ->
            savedFile = file
            savedName = name
        }

        assertEquals(File(records.dirOf(7L), XLiveRecordStore.VIDEO), savedFile)
        assertEquals("xlive_room_a_7.mp4", savedName)
    }

    @Test
    fun `в мои — ник с возрастом из анкеты, второе нажатие убирает`() {
        records.put(record)
        val sm = model()

        sm.toggleSaved()
        assertEquals(24, saved.rooms.value.single { it.username == "room_a" }.age)
        assertTrue(sm.isSaved.value)

        sm.toggleSaved()
        assertTrue(saved.rooms.value.isEmpty())
    }

    @Test
    fun `записи нет — пусто`() {
        records.put(record)

        assertNull(model(id = 99L).record.value)
    }

    @Test
    fun `весь экран — включается и выходится`() {
        records.put(record)
        val sm = model()

        sm.toggleFullScreen()
        assertTrue(sm.isFullScreen.value)
        sm.exitFullScreen()
        assertFalse(sm.isFullScreen.value)
    }

    @Test
    fun `сетка кадров для превью перемотки берётся по id`() {
        records.put(record)

        assertEquals(storyboardFile, model().storyboard.value)
    }

    @Test
    fun `адрес видео — файл записи`() {
        records.put(record)

        assertEquals("file://" + File(records.dirOf(7L), XLiveRecordStore.VIDEO).absolutePath, model().videoUrl)
    }
}
