package com.client.xvideos.xlive.record

import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.XLiveRecord
import com.client.xvideos.xlive.model.XLiveRecordRoom
import com.client.xvideos.xlive.model.XLiveRecordStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class XLiveRecordStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun store(dir: File) = XLiveRecordStore(dir.path, CoroutineScope(Job()), Dispatchers.Unconfined)

    private fun record(id: Long) = XLiveRecord(id = id, username = "room_$id", room = XLiveRecordRoom(title = "тема $id"))

    @Test
    fun `записи читаются от новых к старым, и новым хранилищем тоже`() {
        val dir = tmp.newFolder("records")
        val store = store(dir)
        store.put(record(1))
        store.put(record(3))
        store.put(record(2))

        assertEquals(listOf(3L, 2L, 1L), store.records.value.map { it.id })
        assertEquals(listOf(3L, 2L, 1L), store(dir).records.value.map { it.id })
        assertEquals("тема 2", store(dir).records.value.single { it.id == 2L }.room.title)
    }

    @Test
    fun `update меняет только свою запись`() {
        val store = store(tmp.newFolder("records"))
        store.put(record(1))
        store.put(record(2))

        store.update(2) { it.copy(status = XLiveRecordStatus.READY, sizeBytes = 5) }

        val byId = store.records.value.associateBy { it.id }
        assertEquals(XLiveRecordStatus.READY, byId.getValue(2).status)
        assertEquals(5L, byId.getValue(2).sizeBytes)
        assertEquals(XLiveRecordStatus.RECORDING, byId.getValue(1).status)
    }

    @Test
    fun `remove удаляет папку записи вместе с файлами`() {
        val store = store(tmp.newFolder("records"))
        store.put(record(1))
        val video = File(store.dirOf(1), XLiveRecordStore.VIDEO).apply { writeText("видео") }

        store.remove(1)

        assertFalse(video.exists())
        assertFalse(store.dirOf(1).exists())
        assertTrue(store.records.value.isEmpty())
    }

    @Test
    fun `испорченный record_json пропускается`() {
        val dir = tmp.newFolder("records")
        File(dir, "5").mkdirs()
        File(dir, "5/${XLiveRecordStore.RECORD_JSON}").writeText("{мусор")
        val store = store(dir)
        store.put(record(1))

        assertEquals(listOf(1L), store(dir).records.value.map { it.id })
    }

    @Test
    fun `анкета сохраняется и читается обратно`() {
        val dir = tmp.newFolder("records")
        val bio = XLiveBio(realName = "Anna", age = 24, interestedIn = listOf("Men"), aboutMe = "о себе")
        store(dir).put(record(1).copy(bio = bio))

        assertEquals(bio, store(dir).records.value.single().bio)
    }
}
