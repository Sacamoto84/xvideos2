package com.client.xvideos.xlive.record

import com.client.xvideos.xlive.model.XLiveRoom
import com.client.xvideos.xlive.network.XLiveHttp
import com.client.xvideos.xlive.network.XLiveRecordNet
import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Поддельный эфир на виртуальных часах [clock]: каждые [segmentMs] появляется
 * новый полный сегмент, в окне плейлиста — последние [window]. Каждый вызов
 * [room] выдаёт новый адрес (поколение `g<N>`). Перечитывание с `_HLS_msn`
 * ждёт сегмент, как настоящий сервер.
 */
class FakeRecordNet(private val clock: () -> Long) : XLiveRecordNet {

    var segmentMs = 1600L
    var startMsn = 100L
    var window = 4
    var roomStatus = "public"

    /** Адреса поколений меньше этого отвечают 403. */
    var expireBefore = 0

    /** Сдвиг номеров сегментов для поколения — «нумерация с нуля» на новом адресе. */
    var numberShift: (Int) -> Long = { 0L }

    /** Добавка к краю эфира — эфир «перескочил» вперёд. */
    var jump = 0L

    /** Сеть пропала: всё бросает IOException. */
    var networkDown = false

    /** Сколько следующих запросов бросят IOException. */
    var failNext = 0

    var roomCalls = 0
        private set
    var textCalls = 0
        private set

    private var generation = 0

    private fun edge(gen: Int): Long = startMsn + clock() / segmentMs + jump + numberShift(gen)

    private fun failIfDown() {
        if (networkDown) throw IOException("down")
        if (failNext > 0) {
            failNext--
            throw IOException("blip")
        }
    }

    private fun genOf(url: String): Int = Regex("/g(\\d+)/").find(url)!!.groupValues[1].toInt()

    override suspend fun room(name: String): XLiveRoom {
        failIfDown()
        roomCalls++
        generation++
        val live = roomStatus == XLiveRoom.STATUS_PUBLIC
        return XLiveRoom(
            status = roomStatus,
            hlsUrl = if (live) "https://edge.test/g$generation/master.m3u8?t=$generation" else "",
            username = name,
        )
    }

    override suspend fun text(url: String): XLiveHttp<String> {
        failIfDown()
        textCalls++
        val gen = genOf(url)
        if (gen < expireBefore) return XLiveHttp(403, null)
        if ("master.m3u8" in url) return XLiveHttp(200, master())
        val kind = if ("/v.m3u8" in url) "v" else "a"
        val wanted = Regex("_HLS_msn=(\\d+)").find(url)?.groupValues?.get(1)?.toLong()
        if (wanted != null) {
            while (edge(gen) < wanted) delay(segmentMs / 4)
        }
        if (gen < expireBefore) return XLiveHttp(403, null)
        return XLiveHttp(200, media(kind, edge(gen)))
    }

    override suspend fun bytes(url: String): XLiveHttp<ByteArray> {
        failIfDown()
        if (genOf(url) < expireBefore) return XLiveHttp(403, null)
        val name = url.substringAfterLast('/').substringBefore('?')
        val body = when {
            name.startsWith("init_") -> name.removePrefix("init_").removeSuffix(".m4s") + "init"
            else -> name.removeSuffix(".m4s").replace("seg_", "")
        }
        return XLiveHttp(200, body.toByteArray())
    }

    private fun master() = """
        #EXTM3U
        #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aac",NAME="audio",URI="a.m3u8"
        #EXT-X-STREAM-INF:BANDWIDTH=896000,RESOLUTION=640x360,AUDIO="aac"
        low.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=7128000,RESOLUTION=1920x1080,AUDIO="aac"
        v.m3u8
    """.trimIndent()

    private fun media(kind: String, edge: Long): String = buildString {
        val first = edge - window + 1
        appendLine("#EXTM3U")
        appendLine("#EXT-X-TARGETDURATION:2")
        appendLine("#EXT-X-MEDIA-SEQUENCE:$first")
        appendLine("#EXT-X-MAP:URI=\"init_$kind.m4s\"")
        for (msn in first..edge) {
            appendLine("#EXTINF:1.6,")
            appendLine("seg_$kind$msn.m4s")
        }
    }
}
