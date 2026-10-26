package com.client.xvideos.xlive.record

import java.net.URI

/**
 * Вариант эфира для записи: видео и звук его группы.
 *
 * @property audioUrl плейлист звука; `null` — звук внутри видео или его нет.
 */
data class XLiveVariant(
    val videoUrl: String,
    val audioUrl: String?,
    val bandwidth: Long,
    val width: Int,
    val height: Int,
)

/** Полный сегмент медиаплейлиста с его номером (media sequence). */
data class XLiveSegment(val msn: Long, val url: String, val durationSec: Double)

/**
 * Медиаплейлист эфира: init-сегмент и полные сегменты окна. Части LL-HLS
 * (`EXT-X-PART`, `EXT-X-PRELOAD-HINT`) запись не пишет и здесь их нет.
 */
data class XLiveMediaPlaylist(val initUrl: String?, val segments: List<XLiveSegment>) {

    /** Номер последнего полного сегмента; `null` — окно пустое. */
    val lastMsn: Long? get() = segments.lastOrNull()?.msn
}

private const val STREAM_INF = "#EXT-X-STREAM-INF:"
private const val MEDIA = "#EXT-X-MEDIA:"
private const val MEDIA_SEQUENCE = "#EXT-X-MEDIA-SEQUENCE:"
private const val MAP = "#EXT-X-MAP:"
private const val EXTINF = "#EXTINF:"

private val ATTRIBUTE = Regex("""([A-Z0-9-]+)=("[^"]*"|[^,]*)""")

private fun attributes(line: String): Map<String, String> =
    ATTRIBUTE.findAll(line.substringAfter(':')).associate { match ->
        match.groupValues[1] to match.groupValues[2].removeSurrounding("\"")
    }

/** Адрес [ref] относительно [base]; кривой адрес — `null`, такой вариант или сегмент пропускается. */
private fun resolve(base: String, ref: String): String? =
    runCatching { URI(base).resolve(ref.trim()).toString() }.getOrNull()

/** Следующая непустая строка, не тег: адрес после `EXT-X-STREAM-INF` или `EXTINF`. */
private fun List<String>.uriAfter(index: Int): String? =
    drop(index + 1).firstOrNull { it.isNotBlank() && !it.startsWith("#") }

/**
 * Вариант с наибольшим `BANDWIDTH` и плейлист звука его группы `AUDIO`.
 * Относительные адреса — от [masterUrl]. `null` — вариантов нет.
 */
fun parseBestVariant(master: String, masterUrl: String): XLiveVariant? {
    val lines = master.lines().map { it.trim() }
    val audioByGroup = lines.filter { it.startsWith(MEDIA) }
        .map(::attributes)
        .filter { it["TYPE"] == "AUDIO" && it["URI"] != null }
        .associate { it["GROUP-ID"] to it.getValue("URI") }
    val variants = lines.mapIndexedNotNull { index, line ->
        if (!line.startsWith(STREAM_INF)) return@mapIndexedNotNull null
        val attrs = attributes(line)
        val uri = lines.uriAfter(index) ?: return@mapIndexedNotNull null
        val (width, height) = attrs["RESOLUTION"]?.split('x')?.map { it.toIntOrNull() ?: 0 } ?: listOf(0, 0)
        XLiveVariant(
            videoUrl = resolve(masterUrl, uri) ?: return@mapIndexedNotNull null,
            audioUrl = attrs["AUDIO"]?.let(audioByGroup::get)?.let { resolve(masterUrl, it) },
            bandwidth = attrs["BANDWIDTH"]?.toLongOrNull() ?: 0L,
            width = width,
            height = height,
        )
    }
    return variants.maxByOrNull { it.bandwidth }
}

/** Init и полные сегменты медиаплейлиста; `null` — это не плейлист. */
fun parseMediaPlaylist(text: String, playlistUrl: String): XLiveMediaPlaylist? {
    val lines = text.lines().map { it.trim() }
    if (lines.firstOrNull { it.isNotEmpty() } != "#EXTM3U") return null
    val firstMsn = lines.firstOrNull { it.startsWith(MEDIA_SEQUENCE) }
        ?.substringAfter(':')?.toLongOrNull() ?: 0L
    val initUrl = lines.firstOrNull { it.startsWith(MAP) }
        ?.let { attributes(it)["URI"] }
        ?.let { resolve(playlistUrl, it) }
    var msn = firstMsn
    val segments = lines.mapIndexedNotNull { index, line ->
        if (!line.startsWith(EXTINF)) return@mapIndexedNotNull null
        val duration = line.substringAfter(':').substringBefore(',').toDoubleOrNull() ?: 0.0
        val url = lines.uriAfter(index)?.let { resolve(playlistUrl, it) }
        val segment = url?.let { XLiveSegment(msn, it, duration) }
        msn++
        segment
    }
    return XLiveMediaPlaylist(initUrl, segments)
}

/** Адрес блокирующего перечитывания: сервер ответит, когда будет сегмент [nextMsn]. */
fun blockingReloadUrl(playlistUrl: String, nextMsn: Long): String {
    val separator = if ('?' in playlistUrl) '&' else '?'
    return "$playlistUrl${separator}_HLS_msn=$nextMsn"
}
