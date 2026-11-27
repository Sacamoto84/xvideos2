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
 * @param stallTimeoutMs сколько ждать без нового видеосегмента, прежде чем решить «нет сети».
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

    private data class AddressSnapshot(val generation: Int, val variant: XLiveVariant?)

    @Volatile
    private var address = AddressSnapshot(generation = 0, variant = null)

    @Volatile
    private var lastVideoProgressMs = 0L

    private var lastAddressRequestMs: Long? = null

    /** Закончить запись с причиной [reason]; первая причина побеждает. */
    fun requestStop(reason: XLiveRecordStop) {
        stop.complete(reason)
    }

    /** Пишет до остановки — своей или [requestStop]. Возвращает причину. */
    suspend fun run(): XLiveRecordStop = coroutineScope {
        lastVideoProgressMs = nowMs()
        val watchdog = launch { watchVideoProgress() }
        val work = launch {
            while (address.variant == null && !stop.isCompleted) {
                refreshAddress(seen = 0)
                if (address.variant == null) delay(RETRY_MS)
            }
            val first = address.variant ?: return@launch
            launch { Track(isVideo = true, videoOut).run() }
            if (first.audioUrl != null) launch { Track(isVideo = false, audioOut).run() }
        }
        val reason = stop.await()
        watchdog.cancel()
        work.cancelAndJoin()
        val stats = _stats.value
        Timber.i("XLive: запись — %s, сегментов %d, пропусков %d", reason, stats.videoSegments, stats.gaps)
        reason
    }

    private fun finish(reason: XLiveRecordStop) {
        stop.complete(reason)
    }

    private suspend fun watchVideoProgress() {
        while (!stop.isCompleted) {
            val remaining = stallTimeoutMs - (nowMs() - lastVideoProgressMs)
            if (remaining <= 0L) {
                finish(XLiveRecordStop.NO_NETWORK)
                return
            }
            delay(remaining)
        }
    }

    /** Свежий адрес, если его ещё не взяла другая дорожка после поколения [seen]. */
    private suspend fun refreshAddress(seen: Int) = addressLock.withLock {
        if (address.generation != seen) return@withLock
        // Новый master может постоянно отдавать недоступные медиаплейлисты.
        lastAddressRequestMs?.let { previous -> delay(RETRY_MS - (nowMs() - previous)) }
        lastAddressRequestMs = nowMs()
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
        address = AddressSnapshot(generation = address.generation + 1, variant = best)
        _stats.update { it.copy(width = best.width, height = best.height) }
    }

    /** Запрос; сеть упала — пауза и `null`. */
    private suspend fun <T> request(block: suspend () -> XLiveHttp<T>): XLiveHttp<T>? = try {
        block()
    } catch (e: IOException) {
        Timber.w("XLive: запись — сбой сети, %s", e.javaClass.simpleName)
        delay(RETRY_MS)
        null
    }

    private fun hasSpace(): Boolean = usableBytes() >= _stats.value.bytes + reserveBytes

    private fun written(isVideo: Boolean, size: Int) {
        if (isVideo) lastVideoProgressMs = nowMs()
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
        private var trackEnded = false

        private fun AddressSnapshot.url(): String? =
            variant?.let { if (isVideo) it.videoUrl else it.audioUrl }

        suspend fun run() {
            try {
                FileOutputStream(out).use { stream ->
                    while (!stop.isCompleted && !trackEnded) {
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
            val snap = address
            noteGeneration(snap.generation)
            val playlist = readPlaylist(snap) ?: return
            val last = playlist.lastMsn ?: return delay(RETRY_MS)
            if (!initWritten && !writeInit(stream, playlist.initUrl, snap.generation)) return
            var expected = next ?: last
            if (afterRefresh && last < expected) {
                gap(isVideo)
                expected = last
            }
            afterRefresh = false
            next = expected
            val fresh = playlist.segments.filter { it.msn >= expected }
            if (fresh.isEmpty()) return delay(RETRY_MS)
            if (fresh.first().msn > expected) gap(isVideo)
            writeSegments(stream, fresh, snap.generation)
        }

        /** Адрес сменился с прошлого шага — возможно, его взяла другая дорожка. */
        private fun noteGeneration(gen: Int) {
            if (gen == seenGeneration) return
            if (seenGeneration >= 0) afterRefresh = true
            seenGeneration = gen
        }

        /** Медиаплейлист текущего адреса; `null` — шаг повторится (пауза, свежий адрес или конец). */
        private suspend fun readPlaylist(snap: AddressSnapshot): XLiveMediaPlaylist? {
            val url = snap.url()
            if (url == null) {
                if (isVideo) finish(XLiveRecordStop.STREAM_ENDED) else trackEnded = true
                return null
            }
            // После смены адреса — без блокировки: номера на новом могли начаться
            // заново, и сервер ждал бы сегмент, которого не будет.
            val wanted = next?.takeUnless { afterRefresh }
            val response = request { net.text(wanted?.let { blockingReloadUrl(url, it) } ?: url) } ?: return null
            if (response.code in NEW_ADDRESS || response.code in GONE) return null.also { renew(snap.generation) }
            // 400 на блокирующее перечитывание — такого номера сервер не ждёт
            // (нумерация началась заново): перечитать без номера.
            if (response.code == BAD_REQUEST && wanted != null) {
                afterRefresh = true
                return null
            }
            val playlist = response.body?.takeIf { response.isSuccess }?.let { parseMediaPlaylist(it, url) }
            if (playlist == null) delay(RETRY_MS)
            return playlist
        }

        private suspend fun renew(gen: Int) {
            refreshAddress(gen)
            afterRefresh = true
            if (address.generation == gen) delay(RETRY_MS)
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
                    delay(RETRY_MS)
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
                    else -> return delay(RETRY_MS)
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
