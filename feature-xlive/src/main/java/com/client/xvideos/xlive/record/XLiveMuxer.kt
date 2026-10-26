package com.client.xvideos.xlive.record

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/** Склейка сырых дорожек записи в один mp4. Подменяется в тестах. */
fun interface XLiveMuxer {

    /**
     * Видео [video] и звук [audio] — в [out], без перекодирования.
     *
     * @return длительность, мс.
     * @throws IOException не склеилось.
     */
    suspend fun mux(video: File, audio: File?, out: File): Long
}

/**
 * Склейка средствами Android: [MediaExtractor] читает фрагментированные mp4
 * дорожек, [MediaMuxer] пишет обычный mp4. Время в сырых файлах — время
 * эфира (часы от начала трансляции), поэтому из каждой метки вычитается
 * самая ранняя метка обеих дорожек: запись начинается с нуля.
 */
object MediaMuxerJoin : XLiveMuxer {

    private const val SAMPLE_BUFFER_BYTES = 8 * 1024 * 1024
    private const val US_PER_MS = 1_000L

    /** Расхождение длительности, которое ещё не считается обрывом: последний кадр и округления. */
    private const val VERIFY_TOLERANCE_US = 2_000_000L

    override suspend fun mux(video: File, audio: File?, out: File): Long = withContext(Dispatchers.IO) {
        try {
            join(listOfNotNull(video, audio), out)
        } catch (e: IllegalStateException) {
            throw IOException("Склейка: ${e.javaClass.simpleName}", e)
        } catch (e: IllegalArgumentException) {
            throw IOException("Склейка: ${e.javaClass.simpleName}", e)
        }
    }

    private fun join(inputs: List<File>, out: File): Long {
        val extractors = inputs.map { file ->
            MediaExtractor().apply {
                setDataSource(file.path)
                if (trackCount < 1) throw IOException("В дорожке нет данных")
                selectTrack(0)
            }
        }
        val muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        try {
            val tracks = extractors.map { muxer.addTrack(it.getTrackFormat(0)) }
            val base = extractors.map { it.sampleTime }.filter { it >= 0 }.minOrNull()
                ?: throw IOException("В записи нет кадров")
            muxer.start()
            val lastUs = writeInterleaved(extractors, tracks, muxer, base)
            muxer.stop()
            verify(out, lastUs)
            return lastUs / US_PER_MS
        } finally {
            extractors.forEach { it.release() }
            muxer.release()
        }
    }

    /**
     * Готовый файл не короче записанного. MediaMuxer пишет mp4 с 32-битными
     * смещениями и на пределе ~4 ГБ молча обрывает файл, а `stop()` проходит
     * без ошибки. Без проверки сырые дорожки удалились бы после «успеха».
     */
    private fun verify(out: File, expectedUs: Long) {
        val check = MediaExtractor()
        try {
            check.setDataSource(out.path)
            val durationUs = (0 until check.trackCount)
                .map { check.getTrackFormat(it) }
                .filter { it.containsKey(MediaFormat.KEY_DURATION) }
                .maxOfOrNull { it.getLong(MediaFormat.KEY_DURATION) } ?: 0L
            if (durationUs < expectedUs - VERIFY_TOLERANCE_US) {
                throw IOException("Склейка неполная: ${durationUs / US_PER_MS} из ${expectedUs / US_PER_MS} мс")
            }
        } finally {
            check.release()
        }
    }

    /** Кадры обеих дорожек по возрастанию времени — так mp4 выходит перемежённым. */
    private fun writeInterleaved(
        extractors: List<MediaExtractor>,
        tracks: List<Int>,
        muxer: MediaMuxer,
        base: Long,
    ): Long {
        val buffer = ByteBuffer.allocateDirect(SAMPLE_BUFFER_BYTES)
        val info = MediaCodec.BufferInfo()
        var lastUs = 0L
        val done = BooleanArray(extractors.size)
        while (true) {
            val index = extractors.indices
                .filter { !done[it] && extractors[it].sampleTime >= 0 }
                .minByOrNull { extractors[it].sampleTime } ?: break
            val extractor = extractors[index]
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) {
                done[index] = true
                continue
            }
            val timeUs = extractor.sampleTime - base
            val flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                MediaCodec.BUFFER_FLAG_KEY_FRAME
            } else {
                0
            }
            info.set(0, size, timeUs, flags)
            muxer.writeSampleData(tracks[index], buffer, info)
            lastUs = maxOf(lastUs, timeUs)
            extractor.advance()
        }
        return lastUs
    }
}
