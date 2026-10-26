package com.client.xvideos.xlive.record

import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * Версия разметки ключевых кадров в `video.mp4`. Запись с версией ниже
 * переклеивается при следующем входе в «Записи»: первая склейка брала флаг
 * «ключевой» из сырых фрагментов, а сайт ставит его на начало каждого
 * фрагмента (0,8 с), а не на IDR. Ретривер и плеер прыгали на такой кадр и
 * декодировали P-кадр без опорного — зелёный экран в превью перемотки.
 */
const val XLIVE_KEY_FRAMES_VERSION = 1

private const val AVC_TYPE_MASK = 0x1F
private const val AVC_IDR = 5
private val AVC_SLICES = 1..5
private const val HEVC_TYPE_SHIFT = 1
private const val HEVC_TYPE_MASK = 0x3F
private val HEVC_SLICES = 0..31
private val HEVC_IRAP = 16..23
private const val BYTE_MASK = 0xFF

/**
 * Ключевой ли кадр [sample] (первые [size] байт, стартовые коды Annex B —
 * так их отдаёт `MediaExtractor`) по самому потоку: у H.264 — IDR, у HEVC —
 * IRAP. Смотрит первый срез кадра. `null` — не видео этих кодеков или срез не
 * нашёлся; тогда верят флагу контейнера.
 */
fun isKeyFrameInStream(mime: String?, sample: ByteBuffer, size: Int): Boolean? {
    val hevc = when (mime) {
        MediaFormat.MIMETYPE_VIDEO_AVC -> false
        MediaFormat.MIMETYPE_VIDEO_HEVC -> true
        else -> return null
    }
    var i = 0
    while (i + 3 < size) {
        if (sample.get(i).toInt() != 0 || sample.get(i + 1).toInt() != 0 || sample.get(i + 2).toInt() != 1) {
            i++
            continue
        }
        val header = sample.get(i + 3).toInt() and BYTE_MASK
        if (hevc) {
            val type = (header shr HEVC_TYPE_SHIFT) and HEVC_TYPE_MASK
            if (type in HEVC_SLICES) return type in HEVC_IRAP
        } else {
            val type = header and AVC_TYPE_MASK
            if (type in AVC_SLICES) return type == AVC_IDR
        }
        i += 3
    }
    return null
}
