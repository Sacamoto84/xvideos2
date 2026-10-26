package com.client.xvideos.xlive.record

import com.client.xvideos.xlive.network.XLiveHttp
import com.client.xvideos.xlive.network.XLiveRecordNet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Сколько записано.
 *
 * @property gaps сколько раз видео отстало от эфира и потеряло кусок.
 */
data class XLiveRecordStats(
    val videoSegments: Int = 0,
    val audioSegments: Int = 0,
    val gaps: Int = 0,
    val bytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
)

/**
 * Пишет эфир комнаты [username] в два сырых файла: видео лучшего варианта в
 * [videoOut], его звук в [audioOut] — init-сегмент и полные сегменты fMP4 по
 * порядку номеров.
 *
 * Адрес потока — свой, не плеера: сайт отдаёт адрес на одного потребителя.
 * Окно эфира — около четырёх сегментов, поэтому плейлист перечитывается с
 * блокировкой (`_HLS_msn`): сервер отвечает, когда готов следующий сегмент.
 * Не успели — кусок потерян, запись идёт дальше и считает пропуск.
 *
 * @param usableBytes свободное место на томе записи.
 * @param nowMs монотонные часы, мс.
 * @param stallTimeoutMs сколько ждать без единого записанного сегмента, прежде чем решить «нет сети».
 * @param reserveBytes запас места: склейке нужно ещё столько же, сколько записано.
 */
class XLiveRecorder(
    private val username: String,
    private val videoOut: File,
    private val audioOut: File,
    private val net: XLiveRecordNet,
    private val usableBytes: () -> Long,
    private val nowMs: () -> Long,
    private val stallTimeoutMs: Long = STALL_TIMEOUT_MS,
    private val reserveBytes: Long = RESERVE_BYTES,
) {

    private val _stats = MutableStateFlow(XLiveRecordStats())

    /** Сколько записано — для метаданных записи. */
    val stats: StateFlow<XLiveRecordStats> = _stats.asStateFlow()

    private val stop = CompletableDeferred<XLiveRecordStop>()
    private val addressLock = Mutex()

    @Volatile
    private var variant: XLiveVariant? = null

    /** Растёт с каждым новым адресом: две дорожки с 403 не берут его дважды. */
    @Volatile
    private var generation = 0

    @Volatile
    private var lastProgressMs = 0L

    /** Закончить запись с причиной [reason]; первая причина побеждает. */
    fun requestStop(reason: XLiveRecordStop) {
        stop.complete(reason)
    }

    /** Пишет до остановки — своей или [requestStop]. Возвращает причину. */
    suspend fun run(): XLiveRecordStop = coroutineScope {
        lastProgressMs = nowMs()
        val work = launch {
            while (variant == null && !stop.isCompleted) {
                refreshAddress(seen = 0)
                if (variant == null) waitRetry()
            }
            val first = variant ?: return@launch
            launch { Track(isVideo = true, videoOut).run() }
            if (first.audioUrl != null) launch { Track(isVideo = false, audioOut).run() }
        }
        val reason = stop.await()
        work.cancelAndJoin()
        val stats = _stats.value
        Timber.i("XLive: запись — %s, сегментов %d, пропусков %d", reason, stats.videoSegments, stats.gaps)
        reason
    }

    private fun finish(reason: XLiveRecordStop) {
        stop.complete(reason)
    }

    /** Пауза перед повтором; без единого сегмента дольше предела — «нет сети». */
    private suspend fun waitRetry() {
        if (nowMs() - lastProgressMs > stallTimeoutMs) finish(XLiveRecordStop.NO_NETWORK)
        delay(RETRY_MS)
    }

    /** Свежий адрес, если его ещё не взяла другая дорожка после поколения [seen]. */
    private suspend fun refreshAddress(seen: Int) = addressLock.withLock {
        if (generation != seen) return@withLock
        val room = try {
            net.room(username)
        } catch (e: IOException) {
            Timber.w("XLive: запись — комната не получена, %s", e.javaClass.simpleName)
            return@withLock
        }
        if (!room.isLive) {
            finish(XLiveRecordStop.STREAM_ENDED)
            return@withLock
        }
        val master = request { net.text(room.hlsUrl) } ?: return@withLock
        val best = master.body?.takeIf { master.isSuccess }?.let { parseBestVariant(it, room.hlsUrl) } ?: return@withLock
        variant = best
        generation++
        _stats.update { it.copy(width = best.width, height = best.height) }
    }

    /** Запрос; сеть упала — пауза и `null`. */
    private suspend fun <T> request(block: suspend () -> XLiveHttp<T>): XLiveHttp<T>? = try {
        block()
    } catch (e: IOException) {
        Timber.w("XLive: запись — сбой сети, %s", e.javaClass.simpleName)
        waitRetry()
        null
    }

    private fun hasSpace(): Boolean = usableBytes() >= _stats.value.bytes + reserveBytes

    private fun written(isVideo: Boolean, size: Int) {
        lastProgressMs = nowMs()
        _stats.update {
            if (isVideo) {
                it.copy(videoSegments = it.videoSegments + 1, bytes = it.bytes + size)
            } else {
                it.copy(audioSegments = it.audioSegments + 1, bytes = it.bytes + size)
            }
        }
    }

    private fun gap(isVideo: Boolean) {
        // Видео и звук теряют куски вместе: пропуск считается по видео.
        if (isVideo) _stats.update { it.copy(gaps = it.gaps + 1) }
    }

    /** Одна дорожка: свой файл, свой номер следующего сегмента. */
    private inner class Track(private val isVideo: Boolean, private val out: File) {

        private var next: Long? = null
        private var initWritten = false

        /** Адрес только что сменился: номера на новом могли начаться заново. */
        private var afterRefresh = false

        /**
         * Поколение адреса, с которым дорожка работала. Свежий адрес могла взять
         * другая дорожка — тогда эта узнаёт о смене здесь, а не по своему 403.
         */
        private var seenGeneration = -1

        private val playlistUrl: String?
            get() = variant?.let { if (isVideo) it.videoUrl else it.audioUrl }

        suspend fun run() {
            // Остановка проверяется в цикле, а не только отменой: шаг, который
            // сам решил остановить запись (нет места), возвращается без
            // приостановки, и без проверки дорожка крутилась бы, не отдав поток.
            // Файл не открылся или запись на диск не прошла (обычно — место
            // кончилось между проверками): запись заканчивается, записанное
            // остаётся, исключение дальше не идёт.
            try {
                FileOutputStream(out).use { stream ->
                    while (!stop.isCompleted) {
                        currentCoroutineContext().ensureActive()
                        step(stream)
                    }
                }
            } catch (e: IOException) {
                Timber.w("XLive: запись — файл не пишется, %s", e.javaClass.simpleName)
                finish(XLiveRecordStop.NO_SPACE)
            }
        }

        private suspend fun step(stream: FileOutputStream) {
            val gen = generation
            noteGeneration(gen)
            val playlist = readPlaylist(gen) ?: return
            val last = playlist.lastMsn ?: return waitRetry()
            if (!initWritten && !writeInit(stream, playlist.initUrl, gen)) return
            var expected = next ?: last
            if (afterRefresh && last < expected) {
                gap(isVideo)
                expected = last
            }
            afterRefresh = false
            next = expected
            val fresh = playlist.segments.filter { it.msn >= expected }
            if (fresh.isEmpty()) return waitRetry()
            if (fresh.first().msn > expected) gap(isVideo)
            writeSegments(stream, fresh, gen)
        }

        /** Адрес сменился с прошлого шага — возможно, его взяла другая дорожка. */
        private fun noteGeneration(gen: Int) {
            if (gen == seenGeneration) return
            if (seenGeneration >= 0) afterRefresh = true
            seenGeneration = gen
        }

        /** Медиаплейлист текущего адреса; `null` — шаг повторится (пауза, свежий адрес или конец). */
        private suspend fun readPlaylist(gen: Int): XLiveMediaPlaylist? {
            val url = playlistUrl ?: return null.also { finish(XLiveRecordStop.STREAM_ENDED) }
            // После смены адреса — без блокировки: номера на новом могли начаться
            // заново, и сервер ждал бы сегмент, которого не будет.
            val wanted = next?.takeUnless { afterRefresh }
            val response = request { net.text(wanted?.let { blockingReloadUrl(url, it) } ?: url) } ?: return null
            if (response.code in NEW_ADDRESS || response.code in GONE) return null.also { renew(gen) }
            // 400 на блокирующее перечитывание — такого номера сервер не ждёт
            // (нумерация началась заново): перечитать без номера.
            if (response.code == BAD_REQUEST && wanted != null) {
                afterRefresh = true
                return null
            }
            val playlist = response.body?.takeIf { response.isSuccess }?.let { parseMediaPlaylist(it, url) }
            if (playlist == null) waitRetry()
            return playlist
        }

        private suspend fun renew(gen: Int) {
            refreshAddress(gen)
            afterRefresh = true
            // Свежий адрес не получен (сайт комнаты не ответил): пауза и предел
            // «нет сети», а не шквал запросов, который сам вызвал бы 429.
            if (generation == gen) waitRetry()
        }

        /** `false` — init не записан, шаг повторится. */
        private suspend fun writeInit(stream: FileOutputStream, initUrl: String?, gen: Int): Boolean {
            if (initUrl == null) {
                initWritten = true
                return true
            }
            val init = request { net.bytes(initUrl) } ?: return false
            val body = init.body
            return when {
                init.isSuccess && body != null -> {
                    stream.write(body)
                    initWritten = true
                    true
                }
                init.code in NEW_ADDRESS -> {
                    renew(gen)
                    false
                }
                else -> {
                    waitRetry()
                    false
                }
            }
        }

        private suspend fun writeSegments(stream: FileOutputStream, segments: List<XLiveSegment>, gen: Int) {
            for (segment in segments) {
                if (!hasSpace()) return finish(XLiveRecordStop.NO_SPACE)
                val response = request { net.bytes(segment.url) } ?: return
                val body = response.body
                when {
                    response.isSuccess && body != null -> {
                        stream.write(body)
                        written(isVideo, body.size)
                        next = segment.msn + 1
                    }
                    response.code in GONE -> {
                        gap(isVideo)
                        next = segment.msn + 1
                    }
                    response.code in NEW_ADDRESS -> return renew(gen)
                    else -> return waitRetry()
                }
            }
        }
    }

    private companion object {
        const val STALL_TIMEOUT_MS = 25_000L
        const val RESERVE_BYTES = 300L * 1024 * 1024
        const val RETRY_MS = 1_000L

        /** Адрес истёк или сменился — нужен свежий. */
        val NEW_ADDRESS = setOf(403, 401)

        const val BAD_REQUEST = 400

        /** Сегмента уже нет: выпал из окна. */
        val GONE = setOf(404, 410)
    }
}
