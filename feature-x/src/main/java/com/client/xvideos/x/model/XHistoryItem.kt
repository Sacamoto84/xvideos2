package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable
import java.io.Serializable

/**
 * Элемент истории просмотров раздела X.
 *
 * Все поля имеют значения по умолчанию для безопасной десериализации из JSON.
 * Хранится файлами `<id>.XHistoryItem` в `AppPath.x_history`.
 *
 * @param item карточка видео (название, превью, длительность и т.д.)
 * @param lastPositionMs сохранённая позиция воспроизведения в миллисекундах
 * @param totalDurationMs общая продолжительность видео в миллисекундах
 * @param updatedAt время последнего просмотра в миллисекундах (`System.currentTimeMillis()`)
 */
@Immutable
@kotlinx.serialization.Serializable
data class XHistoryItem(
    val item: ItemsX = ItemsX(),
    val lastPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val updatedAt: Long = 0L,
    val isCompleted: Boolean = false,
) : Serializable {
    /**
     * Подходит ли ролик для возобновления просмотра:
     * - ролик ещё не досмотрен до конца (isCompleted == false);
     * - общая длина не менее 2 минут (120 000 мс);
     * - просмотрено более 5 секунд (5 000 мс);
     * - просмотрено менее 95% от общей длительности (не досмотрен до конца).
     */
    val isEligibleForResume: Boolean
        get() = !isCompleted &&
            totalDurationMs >= 120_000L &&
            lastPositionMs > 5_000L &&
            lastPositionMs < (totalDurationMs * 0.95)

    /**
     * Доля просмотренного видео в диапазоне 0.0 .. 1.0 для отображения индикатора прогресса.
     */
    val progressFraction: Float
        get() = when {
            isCompleted || (totalDurationMs > 0L && lastPositionMs >= totalDurationMs) -> 1f
            lastPositionMs <= 0L || totalDurationMs <= 0L -> 0f
            else -> (lastPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
        }

    val remainingMs: Long get() = (totalDurationMs - lastPositionMs).coerceAtLeast(0L)
    val hasProgress: Boolean get() = lastPositionMs > 0L
    val hasTotalDuration: Boolean get() = totalDurationMs > 0L
    val isValid: Boolean get() = item.id > 0L
    val isEmpty: Boolean get() = item.id <= 0L
    val isNotEmpty: Boolean get() = item.id > 0L

    companion object {
        val EMPTY = XHistoryItem()
    }
}
