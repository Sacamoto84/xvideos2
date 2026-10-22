package com.client.xvideos.x.feature.saved

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaMetadataRetriever
import androidx.core.graphics.createBitmap
import com.client.xvideos.x.model.STORYBOARD_COLUMNS
import com.client.xvideos.x.model.STORYBOARD_ROWS
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

/** Качество JPEG сетки: кадр 192×108 в маленьком окошке превью, больше не нужно. */
private const val STORYBOARD_JPEG_QUALITY = 80

/**
 * Сетка кадров скачанного ролика из самого файла — системным
 * [MediaMetadataRetriever], без новых зависимостей. Кадр — ближайший ключевой
 * к началу своего процента: точный кадр пришлось бы декодировать от ключевого,
 * а для превью над ползунком разницы не видно.
 *
 * Кадр, который не достался, оставляет клетку чёрной; ни одного кадра или
 * сетка не записалась целиком — сетки нет. Отмену проверяет между кадрами.
 */
object RetrieverStoryboardRenderer : StoryboardRenderer {

    override suspend fun render(video: File, out: File): Boolean {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(video.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
                ?: return false
            val sheet = createBitmap(LOCAL_STORYBOARD_WIDTH, LOCAL_STORYBOARD_HEIGHT)
            try {
                val canvas = Canvas(sheet)
                canvas.drawColor(Color.BLACK)
                var frames = 0
                for (index in 0 until STORYBOARD_COLUMNS * STORYBOARD_ROWS) {
                    currentCoroutineContext().ensureActive()
                    val frame = retriever.getScaledFrameAtTime(
                        storyboardFrameTimeUs(index, durationMs),
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        LOCAL_STORYBOARD_CELL_WIDTH,
                        LOCAL_STORYBOARD_CELL_HEIGHT,
                    ) ?: continue
                    val origin = storyboardCellOrigin(index, frame.width, frame.height)
                    canvas.drawBitmap(frame, origin.x.toFloat(), origin.y.toFloat(), null)
                    frame.recycle()
                    frames++
                }
                if (frames == 0) return false
                // Сбой записи (нет места) compress не бросает, а отвечает false:
                // без проверки недописанная сетка становилась готовой (проход 211, C2).
                return out.outputStream().use { sheet.compress(Bitmap.CompressFormat.JPEG, STORYBOARD_JPEG_QUALITY, it) }
            } finally {
                sheet.recycle()
            }
        }
    }
}
