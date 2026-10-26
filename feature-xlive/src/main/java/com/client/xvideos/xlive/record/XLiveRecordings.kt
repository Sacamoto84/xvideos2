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
import com.client.xvideos.xlive.record.XLiveRecordStore.Companion.FRAME
import com.client.xvideos.xlive.record.XLiveRecordStore.Companion.MUX_TEMP
import com.client.xvideos.xlive.record.XLiveRecordStore.Companion.SNAPSHOT
import com.client.xvideos.xlive.record.XLiveRecordStore.Companion.VIDEO
import com.client.xvideos.xlive.record.XLiveRecordStore.Companion.VIDEO_RAW
import com.client.xvideos.xlive.saved.XLiveSaved
import kotlinx.coroutines.CancellationException
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
            frameGrabber = RetrieverFrameGrabber,
            usableBytes = { File(AppPath.xlive_records).usableSpace },
            newId = System::currentTimeMillis,
            elapsedMs = SystemClock::elapsedRealtime,
            fetchSnapshot = { name ->
                XLiveApi.fetchBytes(xLiveSnapshotUrl(name, System.currentTimeMillis() / MILLIS_PER_SECOND)).body
            },
            notify = { message -> EventBus.postEvent(Event.ShowSnackBar(message)) },
            onVideoReplaced = { id ->
                // Идущая сборка читает прежний файл: её сетка готовой стать не должна.
                storyboards.cancel(id)
                storyboards.file(id).delete()
            },
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
    private val frameGrabber: XLiveFrameGrabber,
    private val usableBytes: () -> Long,
    private val newId: () -> Long,
    private val elapsedMs: () -> Long,
    private val fetchSnapshot: suspend (String) -> ByteArray?,
    private val notify: (UiMessage) -> Unit,
    /** `video.mp4` записи переклеен: сетка превью по прежнему файлу больше не годится. */
    private val onVideoReplaced: (Long) -> Unit = {},
) {

    private enum class Outcome { READY, EMPTY, BROKEN }

    private companion object {
        /** Запас места сверх копии видео при переклейке — как у записи. */
        const val REMUX_RESERVE_BYTES = 300L * 1024 * 1024
    }

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
            // Сессия не должна ни уронить процесс, ни оставить запись «идущей»:
            // что бы ни случилось, записанное склеивается, а занятость снимается.
            try {
                val reason = runRecorder(recorder)
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
            } finally {
                session.done()
                synchronized(lock) {
                    busy -= id
                    if (current === session) current = null
                }
            }
        }
        return session
    }

    /** Запись до остановки; неожиданное исключение — тоже остановка, с сохранением записанного. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun runRecorder(recorder: XLiveRecorder): XLiveRecordStop = try {
        recorder.run()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w("XLive: запись прервалась — %s", e.javaClass.simpleName)
        XLiveRecordStop.ERROR
    }

    @Suppress("TooGenericExceptionCaught")
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Одна испорченная запись не мешает доклеить остальные.
                    Timber.w("XLive: доклейка не удалась — %s", e.javaClass.simpleName)
                } finally {
                    synchronized(lock) { busy -= record.id }
                }
            }
            // Записи со старой разметкой ключевых кадров переклеиваются один раз.
            val staleKeyFrames = store.snapshot().filter {
                it.status == XLiveRecordStatus.READY && it.keyFramesVersion < XLIVE_KEY_FRAMES_VERSION &&
                    File(store.dirOf(it.id), VIDEO).isFile
            }
            for (record in staleKeyFrames) {
                if (!synchronized(lock) { busy.add(record.id) }) continue
                try {
                    remux(record.id)
                } finally {
                    synchronized(lock) { busy -= record.id }
                }
            }
            // Записи без кадра нынешнего способа (готовые раньше или со старым кадром) получают его сейчас.
            val withoutFrame = store.snapshot().filter {
                it.status == XLiveRecordStatus.READY && it.frameVersion < XLIVE_FRAME_VERSION &&
                    File(store.dirOf(it.id), VIDEO).isFile
            }
            for (record in withoutFrame) {
                if (!synchronized(lock) { busy.add(record.id) }) continue
                try {
                    grabFrame(record.id)
                } finally {
                    synchronized(lock) { busy -= record.id }
                }
            }
        }
    }

    /**
     * Готовое видео записи [id] — заново через склейку, сама в себя: ключевые
     * кадры размечаются по потоку ([XLIVE_KEY_FRAMES_VERSION]). Места на копию
     * нет — в другой раз. Не склеилось — прежнее видео остаётся, а версия
     * пишется, чтобы файл не перечитывался при каждом входе во вкладку.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun remux(id: Long) {
        val dir = store.dirOf(id)
        val video = File(dir, VIDEO)
        if (usableBytes() < video.length() + REMUX_RESERVE_BYTES) return
        val temp = File(dir, MUX_TEMP).apply { delete() }
        try {
            muxer.mux(video, null, temp)
            if (!temp.renameTo(video)) throw IOException("Переклейка не переименована")
            onVideoReplaced(id)
            Timber.i("XLive: запись переклеена — ключевые кадры по потоку")
        } catch (e: CancellationException) {
            temp.delete()
            throw e
        } catch (e: Exception) {
            // MediaMuxer бросает и не IOException.
            Timber.w("XLive: переклейка не удалась — %s", e.javaClass.simpleName)
            temp.delete()
        }
        store.update(id) { it.copy(keyFramesVersion = XLIVE_KEY_FRAMES_VERSION, sizeBytes = video.length()) }
    }

    /** Кадр начала из готового видео — картинка карточки; не вышел — остаётся снимок сайта. */
    private suspend fun grabFrame(id: Long) {
        val dir = store.dirOf(id)
        val grabbed = frameGrabber.grab(File(dir, VIDEO), File(dir, FRAME))
        if (grabbed) {
            dir.listFiles { file -> file.name.startsWith(XLiveRecordStore.FRAME_PREFIX) && file.name != FRAME }
                ?.forEach { it.delete() }
        }
        // Версия пишется и при неудаче: иначе кадр пробовался бы при каждом входе во вкладку.
        store.update(id) { it.copy(hasFrame = grabbed || it.hasFrame, frameVersion = XLIVE_FRAME_VERSION) }
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
    @Suppress("TooGenericExceptionCaught")
    private suspend fun finishRecord(id: Long, hasVideo: Boolean): Outcome {
        val dir = store.dirOf(id)
        val videoRaw = File(dir, VIDEO_RAW)
        val audioRaw = File(dir, AUDIO_RAW)
        val video = File(dir, VIDEO)
        // Склейка уже прошла, а статус в JSON не успел стать «готова» (процесс
        // убит между переименованием и записью JSON): готовый файл не трогаем.
        if (!videoRaw.isFile && video.isFile && video.length() > 0L) {
            store.update(id) {
                it.copy(status = XLiveRecordStatus.READY, sizeBytes = video.length(), keyFramesVersion = XLIVE_KEY_FRAMES_VERSION)
            }
            return Outcome.READY
        }
        if (!hasVideo || !videoRaw.isFile || videoRaw.length() == 0L) {
            store.remove(id)
            return Outcome.EMPTY
        }
        val temp = File(dir, MUX_TEMP).apply { delete() }
        return try {
            val durationMs = muxer.mux(videoRaw, audioRaw.takeIf { it.isFile && it.length() > 0L }, temp)
            if (!temp.renameTo(video)) throw IOException("Склейка не переименована")
            videoRaw.delete()
            audioRaw.delete()
            store.update(id) {
                it.copy(
                    status = XLiveRecordStatus.READY,
                    durationMs = durationMs,
                    sizeBytes = video.length(),
                    keyFramesVersion = XLIVE_KEY_FRAMES_VERSION,
                )
            }
            grabFrame(id)
            Outcome.READY
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // MediaMuxer бросает и не IOException; сырые дорожки остаются для повтора.
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
