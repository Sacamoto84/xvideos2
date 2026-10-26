package com.client.xvideos.xlive.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Ключевой кадр записи — по самому потоку: сайт ставит флаг «ключевой» на
 * начало каждого фрагмента, а не на IDR, и превью перемотки показывало
 * зелёный P-кадр без опорного.
 */
class XLiveKeyFramesTest {

    private val avc = "video/avc"
    private val hevc = "video/hevc"

    /** Кадр из NAL с заголовками [headers]: четырёхбайтовый стартовый код у первого, трёхбайтовый у остальных. */
    private fun sample(vararg headers: Int): ByteBuffer {
        val bytes = headers.withIndex().flatMap { (i, header) ->
            val start = if (i == 0) listOf(0, 0, 0, 1) else listOf(0, 0, 1)
            start + header + listOf(0x88, 0x84, 0x00, 0x21)
        }
        // Запас за данными кадра — как в буфере извлекателя: смотреть только первые size байт.
        return ByteBuffer.wrap((bytes + listOf(0, 0, 1, 0x65)).map { it.toByte() }.toByteArray())
    }

    private fun key(mime: String, buffer: ByteBuffer) = isKeyFrameInStream(mime, buffer, buffer.capacity() - 4)

    @Test
    fun `H_264 — ключевой только кадр с IDR`() {
        // Как в записи: AUD, SPS, PPS, SEI и срез IDR.
        assertEquals(true, key(avc, sample(0x09, 0x67, 0x68, 0x06, 0x65)))
        // Начало фрагмента без IDR — AUD и обычный срез: флаг фрагмента лжёт.
        assertEquals(false, key(avc, sample(0x09, 0x41)))
        assertEquals(false, key(avc, sample(0x01)))
    }

    @Test
    fun `HEVC — ключевой кадр IRAP`() {
        // VPS, SPS, PPS и IDR_W_RADL (19); CRA (21) — тоже точка входа.
        assertEquals(true, key(hevc, sample(0x40, 0x42, 0x44, 19 shl 1)))
        assertEquals(true, key(hevc, sample(21 shl 1)))
        // AUD (35) и TRAIL_R (1).
        assertEquals(false, key(hevc, sample(35 shl 1, 1 shl 1)))
    }

    @Test
    fun `не разобрать — решает флаг контейнера`() {
        assertNull(key("audio/mp4a-latm", sample(0x65)))
        // Без среза: только параметры потока.
        assertNull(key(avc, sample(0x67, 0x68)))
        // Длины вместо стартовых кодов.
        assertNull(isKeyFrameInStream(avc, ByteBuffer.wrap(byteArrayOf(0, 0, 0, 5, 0x65, 1, 2, 3, 4)), 9))
    }
}
