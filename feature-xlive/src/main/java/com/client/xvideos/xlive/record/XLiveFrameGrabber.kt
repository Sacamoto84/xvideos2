package com.client.xvideos.xlive.record

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Версия способа брать кадр. Кадр записи с версией ниже пересобирается при
 * следующем входе в «Записи»: первый способ брал один кадр на 1-й секунде, и
 * системный ретривер для части файлов отдавал почти однотонный тёмный кадр;
 * у промежуточной сборки (версия 1) кадр лежал под именем без версии.
 */
const val XLIVE_FRAME_VERSION = 2

/** Разброс яркости, с которого кадр считается не однотонным. */
const val FRAME_DETAIL_ENOUGH = 12.0

private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL = 0xFF
private const val LUMA_R = 0.299
private const val LUMA_G = 0.587
private const val LUMA_B = 0.114

/** Насколько кадр не однотонный: разброс яркости пикселей ARGB [pixels]. */
fun frameDetail(pixels: IntArray): Double {
    if (pixels.isEmpty()) return 0.0
    val luma = DoubleArray(pixels.size) { index ->
        val color = pixels[index]
        LUMA_R * (color shr RED_SHIFT and CHANNEL) + LUMA_G * (color shr GREEN_SHIFT and CHANNEL) + LUMA_B * (color and CHANNEL)
    }
    val mean = luma.average()
    return kotlin.math.sqrt(luma.sumOf { (it - mean) * (it - mean) } / luma.size)
}

/** Картинка карточки записи — кадр из самого видео. Подменяется в тестах. */
fun interface XLiveFrameGrabber {

    /** Кадр начала [video] — в [out] (JPEG). `false` — кадр не достался. */
    suspend fun grab(video: File, out: File): Boolean
}

/**
 * Кадр начала записи системным [MediaMetadataRetriever], уменьшенный до
 * ширины карточки. Снимок сайта в начале записи бывает устаревшим или чёрным,
 * а этот кадр — то, что в файле. Пробуются 1-я, 3-я и 6-я секунды: годится
 * первый кадр с деталями, иначе — самый детальный из них.
 */
object RetrieverFrameGrabber : XLiveFrameGrabber {

    private val FRAME_TIMES_US = longArrayOf(1_000_000L, 3_000_000L, 6_000_000L)
    private const val FRAME_WIDTH = 640
    private const val FRAME_HEIGHT = 360
    private const val JPEG_QUALITY = 85

    // Не открылся файл, нет кадра, не записался JPEG — кадра нет, карточка
    // остаётся со снимком сайта. Ретривер бросает и RuntimeException.
    override suspend fun grab(video: File, out: File): Boolean = withContext(Dispatchers.IO) {
        runCatching { grabNow(video, out) }.getOrDefault(false)
    }

    private fun grabNow(video: File, out: File): Boolean = MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(video.absolutePath)
        val frame = pickFrame(retriever) ?: return false
        try {
            // Временный файл и переименование: оборванный кадр готовым не выглядит.
            val temp = File(out.parentFile, "${out.name}.tmp")
            val written = temp.outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            if (!written) throw IOException("Кадр не записан")
            temp.renameTo(out)
        } finally {
            frame.recycle()
        }
    }

    /** Первый кадр с деталями из [FRAME_TIMES_US]; ни одного — самый детальный. */
    private fun pickFrame(retriever: MediaMetadataRetriever): Bitmap? {
        var best: Bitmap? = null
        var bestDetail = -1.0
        for (timeUs in FRAME_TIMES_US) {
            // Точный кадр, а не ближайший ключевой: для части файлов ключевой
            // отдавался почти однотонным.
            val frame = retriever.getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST,
                FRAME_WIDTH,
                FRAME_HEIGHT,
            ) ?: continue
            val pixels = IntArray(frame.width * frame.height)
            frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
            val detail = frameDetail(pixels)
            if (detail > bestDetail) {
                best?.recycle()
                best = frame
                bestDetail = detail
            } else {
                frame.recycle()
            }
            if (detail >= FRAME_DETAIL_ENOUGH) break
        }
        return best
    }
}
