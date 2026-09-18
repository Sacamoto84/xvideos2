package com.client.xvideos.x.feature.saved

import androidx.compose.runtime.Stable
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.XHistoryItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Хранилище истории просмотров раздела X.
 *
 * Сохраняет прогресс для роликов длительностью >= 2 минут (120 000 мс).
 * При досмотре до >= 95% позиция сбрасывается на 0L (следующий запуск начнётся сначала).
 * Ёмкость ограничена [MAX_HISTORY_ITEMS] записями (FIFO вытеснение самых старых).
 */
@Stable
class SavedX_History(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val historyDb = FileDB(AppPath.x_history, EXTENSION, XHistoryItem.serializer())

    /** Реактивный список записей для UI экрана истории. */
    val list = historyDb.list

    /** Быстрый in-memory доступ по id для плеера без лишнего дискового I/O. */
    private val historyMap = ConcurrentHashMap<Long, XHistoryItem>()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    /**
     * Возвращает сохранённый элемент истории по ID ролика.
     * Сначала проверяется in-memory кэш, затем чтение с диска.
     */
    fun get(id: Long): XHistoryItem? {
        if (id <= 0L) return null
        return historyMap[id] ?: historyDb.read(id.toString()).getOrNull()?.also {
            historyMap[id] = it
        }
    }

    /**
     * Обновляет прогресс воспроизведения ролика.
     *
     * Игнорирует ролики короче 2 минут (120 000 мс) и случайные открытия (< 5 сек).
     * При достижении >= 95% длительности позиция сбрасывается на 0 мс (досмотрено).
     */
    fun updateProgress(item: ItemsX, positionMs: Long, totalDurationMs: Long) {
        if (item.id <= 0L) return
        if (totalDurationMs < MIN_DURATION_FOR_HISTORY_MS) return

        val isFinished = positionMs >= (totalDurationMs * COMPLETION_THRESHOLD)
        val targetPosition = if (isFinished) 0L else positionMs

        // Защита от случайных открытий: не создаём новую запись, если просмотрено меньше 5 секунд
        if (!isFinished && targetPosition < MIN_PLAYBACK_FOR_SAVE_MS && !historyMap.containsKey(item.id)) {
            return
        }

        val entry = XHistoryItem(
            item = item,
            lastPositionMs = targetPosition,
            totalDurationMs = totalDurationMs,
            updatedAt = System.currentTimeMillis()
        )

        historyMap[item.id] = entry

        scope.launch(ioDispatcher) {
            historyDb.insert(item.id.toString(), entry)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.item.id == item.id }
                        list.add(0, entry)
                        pruneExcessItemsLocked()
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "SavedX_History: не удалось сохранить прогресс для %d", item.id)
                }
        }
    }

    /**
     * Удаляет запись из истории по ID видео.
     */
    fun delete(item: ItemsX) {
        if (item.id <= 0L) return
        scope.launch(ioDispatcher) {
            historyDb.delete(item.id.toString())
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        historyMap.remove(item.id)
                        list.removeAll { it.item.id == item.id }
                    }
                    SnackBar.info("Удалено из истории")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка удаления: ${e.message}")
                }
        }
    }

    /**
     * Полная очистка истории просмотров.
     */
    fun clearAll() {
        scope.launch(ioDispatcher) {
            val dir = File(AppPath.x_history)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { file -> file.extension == EXTENSION }?.forEach { it.delete() }
            }
            withContext(Dispatchers.Main) {
                historyMap.clear()
                list.clear()
            }
            SnackBar.info("История очищена")
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(ioDispatcher) {
            historyDb.refresh()
            withContext(Dispatchers.Main) {
                // Сортировка по времени последнего просмотра от новых к старым
                val sorted = list.sortedByDescending { it.updatedAt }
                list.clear()
                list.addAll(sorted)
                historyMap.clear()
                sorted.forEach { historyMap[it.item.id] = it }
                pruneExcessItemsLocked()
            }
        }
    }

    /** Удаляет старые записи сверх лимита [MAX_HISTORY_ITEMS] по принципу FIFO. */
    private fun pruneExcessItemsLocked() {
        if (list.size <= MAX_HISTORY_ITEMS) return
        val excess = list.subList(MAX_HISTORY_ITEMS, list.size).toList()
        list.removeAll(excess)
        scope.launch(ioDispatcher) {
            excess.forEach { entry ->
                historyMap.remove(entry.item.id)
                historyDb.delete(entry.item.id.toString())
            }
        }
    }

    companion object {
        private const val EXTENSION = "XHistoryItem"
        const val MAX_HISTORY_ITEMS = 200
        const val MIN_DURATION_FOR_HISTORY_MS = 120_000L // 2 минуты
        const val MIN_PLAYBACK_FOR_SAVE_MS = 5_000L      // 5 секунд
        const val COMPLETION_THRESHOLD = 0.95f           // 95% длительности
    }
}
