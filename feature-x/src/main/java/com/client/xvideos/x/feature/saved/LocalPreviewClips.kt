package com.client.xvideos.x.feature.saved

import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import com.client.xvideos.common.videoplayer.model.MediaClip
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/** Отрезки превью: начала в этих процентах длительности. */
private val PREVIEW_CLIP_PERCENTS = longArrayOf(10, 30, 50, 70, 90)
private const val PREVIEW_CLIP_MS = 1_000L
private const val PERCENT = 100L

/** Короче — играет целиком: пять секундных отрезков не помещаются. */
private const val MIN_CLIPPED_DURATION_MS = 5_000L

/**
 * Превью сохранённого ролика X — 5 отрезков по секунде из самого файла,
 * начала в 10, 30, 50, 70 и 90% длительности [durationMs]; конец отрезка не
 * дальше длительности. Файл короче 5 с или без длительности — пустой список:
 * плеер играет файл целиком.
 */
fun localPreviewClips(durationMs: Long?): List<MediaClip> {
    if (durationMs == null || durationMs < MIN_CLIPPED_DURATION_MS) return emptyList()
    return PREVIEW_CLIP_PERCENTS.map { percent ->
        val start = durationMs * percent / PERCENT
        MediaClip(start, minOf(start + PREVIEW_CLIP_MS, durationMs))
    }
}

/**
 * Длительность скачанного ролика по его `file://`-URI [fileUrl] — из самого
 * файла: строка «10 мин.» карточки неточна. Сбой — `null`. Читает файл —
 * звать не с главного потока.
 */
fun videoDurationMs(fileUrl: String): Long? = try {
    val path = fileUrl.toUri().path ?: return null
    MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.takeIf { it > 0L }
    }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // В сообщении бывает путь к ролику — в журнал только класс.
    Timber.w("X превью из файла: длительность не прочитана: %s", e.javaClass.simpleName)
    null
}
