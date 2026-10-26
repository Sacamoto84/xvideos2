package com.client.xvideos.xlive.record

import android.os.SystemClock
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.snackbar.UiMessage
import com.client.xvideos.common.videoplayer.storyboard.LocalStoryboards
import com.client.xvideos.common.videoplayer.storyboard.RetrieverStoryboardRenderer
import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.XLiveRecord
import com.client.xvideos.xlive.model.XLiveRecordRoom
import com.client.xvideos.xlive.model.XLiveRecordStatus
import com.client.xvideos.xlive.model.XLiveRoom
import com.client.xvideos.xlive.network.XLiveApi
import com.client.xvideos.xlive.network.XLiveApiRecordNet
import com.client.xvideos.xlive.network.XLiveRecordNet
import com.client.xvideos.xlive.network.xLiveSnapshotUrl
import com.client.xvideos.xlive.record.XLiveRecordStore.Companion.AUDIO_RAW
import com.client.xvideos.xlive.record.XLiveRecordStore.Companion.MUX_TEMP
import com.client.xvideos.xlive.record.XLiveRecordStore.Companion.SNAPSHOT
import com.client.xvideos.xlive.record.XLiveRecordStore.Companion.VIDEO
import com.client.xvideos.xlive.record.XLiveRecordStore.Companion.VIDEO_RAW
import com.client.xvideos.xlive.saved.XLiveSaved
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Записи эфиров на процесс: хранилище, идущая запись, доклейка.
 *
 * Объект, как [XLiveSaved]: у раздела нет графа зависимостей. Запись живёт
 * здесь, а не в ScreenModel комнаты: уход с экрана останавливает запись, но
 * склейка после остановки должна дойти до конца.
 */
object XLiveRecordings {

    private const val MILLIS_PER_SECOND = 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val coreLazy = lazy {
        XLiveSaved.ensureLegacyMoved()
        XLiveRecordingsCore(
            store = XLiveRecordStore(AppPath.xlive_records, scope),
            scope = scope,
            net = XLiveApiRecordNet,
            muxer = MediaMuxerJoin,
            usableBytes = { File(AppPath.xlive_records).usableSpace },
            newId = System::currentTimeMillis,
            elapsedMs = SystemClock::elapsedRealtime,
            fetchSnapshot = { name ->
                XLiveApi.fetchBytes(xLiveSnapshotUrl(name, System.currentTimeMillis() / MILLIS_PER_SECOND)).body
            },
            notify = { message -> EventBus.postEvent(Event.ShowSnackBar(message)) },
        )
    }
    private val core by coreLazy

    /** Сетки кадров записей для превью перемотки: `.storyboard.jpg` в папке записи, в копию не идёт. */
    private val storyboards by lazy {
        LocalStoryboards(
            dir = AppPath.xlive_records,
            scope = scope,
            renderer = RetrieverStoryboardRenderer,
            videoFile = { File(store.dirOf(it), VIDEO) },
            storyboardFile = { File(store.dirOf(it), XLiveRecordStore.STORYBOARD) },
        )
    }

    /** Записи на диске. */
    val store: XLiveRecordStore get() = core.store

    /** Сетка кадров записи [id]: готовая или собранная сейчас; `null` — не вышло. */
    suspend fun storyboard(id: Long): File? = storyboards.obtain(id)

    /** Начать запись комнаты [room]; идущая запись другой комнаты останавливается. */
    fun start(room: XLiveRoom, bio: XLiveBio?): XLiveRecordingHandle = core.start(room, bio)

    /** Доклеить записи, оборванные вместе с процессом. */
    fun finishPending() = core.finishPending()

    /** Перечитать записи с диска — после восстановления резервной копии. */
    fun refresh() {
        if (coreLazy.isInitialized()) core.store.refresh()
    }
}

/** Логика [XLiveRecordings] с зависимостями снаружи — для тестов. */
internal class XLiveRecordingsCore(
    val store: XLiveRecordStore,
    private val scope: CoroutineScope,
    private val net: XLiveRecordNet,
    private val muxer: XLiveMuxer,
    private val usableBytes: () -> Long,
    private val newId: () -> Long,
    private val elapsedMs: () -> Long,
    private val fetchSnapshot: suspend (String) -> ByteArray?,
    private val notify: (UiMessage) -> Unit,
) {

    private enum class Outcome { READY, EMPTY, BROKEN }

    private val lock = Any()
    private var current: XLiveRecordingSession? = null

    /** Записи, которые сейчас пишутся или склеиваются: доклейка их не трогает. */
    private val busy = mutableSetOf<Long>()

    fun start(room: XLiveRoom, bio: XLiveBio?): XLiveRecordingSession {
        val id = newId()
        val dir = store.dirOf(id).apply { mkdirs() }
        store.put(XLiveRecord(id = id, username = room.username, room = room.toRecordRoom(), bio = bio))
        val recorder = XLiveRecorder(
            username = room.username,
            videoOut = File(dir, VIDEO_RAW),
            audioOut = File(dir, AUDIO_RAW),
            net = net,
            usableBytes = usableBytes,
            nowMs = elapsedMs,
        )
        val session = XLiveRecordingSession(id, recorder, store, elapsedMs())
        val previous = synchronized(lock) {
            busy += id
            current.also { current = session }
        }
        previous?.stop(XLiveRecordStop.LEFT_ROOM)
        scope.launch { saveSnapshot(room.username, dir) }
        scope.launch {
            val reason = recorder.run()
            session.finishing()
            val stats = recorder.stats.value
            store.update(id) {
                it.copy(status = XLiveRecordStatus.FINISHING, gaps = stats.gaps, width = stats.width, height = stats.height)
            }
            val outcome = finishRecord(id, hasVideo = stats.videoSegments > 0)
            notify(
                when (outcome) {
                    Outcome.READY -> if (reason.isError) UiMessage.Error(reason.message) else UiMessage.Success(reason.message)
                    Outcome.EMPTY -> UiMessage.Error("Запись не сохранена: нет данных")
                    Outcome.BROKEN -> UiMessage.Error("Запись не склеилась")
                }
            )
            session.done()
            synchronized(lock) {
                busy -= id
                if (current === session) current = null
            }
        }
        return session
    }

    fun finishPending() {
        scope.launch {
            val pending = store.snapshot().filter {
                it.status == XLiveRecordStatus.RECORDING || it.status == XLiveRecordStatus.FINISHING
            }
            for (record in pending) {
                if (!synchronized(lock) { busy.add(record.id) }) continue
                try {
                    val outcome = finishRecord(record.id, hasVideo = true)
                    Timber.i("XLive: доклейка записи — %s", outcome)
                } finally {
                    synchronized(lock) { busy -= record.id }
                }
            }
        }
    }

    private suspend fun saveSnapshot(username: String, dir: File) {
        try {
            val bytes = fetchSnapshot(username) ?: return
            // Папку не создаём: пустую запись могли уже удалить.
            if (dir.isDirectory) File(dir, SNAPSHOT).writeBytes(bytes)
        } catch (e: IOException) {
            Timber.w("XLive: снимок записи не сохранён — %s", e.javaClass.simpleName)
        }
    }

    /** Склеить сырые дорожки записи [id] в `video.mp4`; без видео — удалить запись. */
    private suspend fun finishRecord(id: Long, hasVideo: Boolean): Outcome {
        val dir = store.dirOf(id)
        val videoRaw = File(dir, VIDEO_RAW)
        val audioRaw = File(dir, AUDIO_RAW)
        if (!hasVideo || !videoRaw.isFile || videoRaw.length() == 0L) {
            store.remove(id)
            return Outcome.EMPTY
        }
        val temp = File(dir, MUX_TEMP).apply { delete() }
        return try {
            val durationMs = muxer.mux(videoRaw, audioRaw.takeIf { it.isFile && it.length() > 0L }, temp)
            val video = File(dir, VIDEO)
            if (!temp.renameTo(video)) throw IOException("Склейка не переименована")
            videoRaw.delete()
            audioRaw.delete()
            store.update(id) {
                it.copy(status = XLiveRecordStatus.READY, durationMs = durationMs, sizeBytes = video.length())
            }
            Outcome.READY
        } catch (e: IOException) {
            Timber.w("XLive: запись не склеилась — %s", e.javaClass.simpleName)
            temp.delete()
            store.update(id) { it.copy(status = XLiveRecordStatus.BROKEN) }
            Outcome.BROKEN
        }
    }

    private fun XLiveRoom.toRecordRoom() = XLiveRecordRoom(
        title = title,
        gender = gender,
        viewers = viewers,
        ratingPercent = ratingPercent,
        broadcastStartedAt = startedAt,
    )
}
