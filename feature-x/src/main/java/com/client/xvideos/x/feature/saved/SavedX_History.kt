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
     * Обновляет прогресс воспроизведения ролика в истории.
     *
     * - Ролик фиксируется в истории, если воспроизведение длилось более 1 секунды.
     * - Позиция возобновления сохраняется только для роликов длительностью >= 2 минут (120 000 мс)
     *   и если просмотрено не менее 5 секунд. Для коротких (< 2 мин) или досмотренных (>= 95%)
     *   позиция сбрасывается на 0L (ролик начнётся сначала).
     */
    fun updateProgress(item: ItemsX, positionMs: Long, totalDurationMs: Long) {
        if (item.id <= 0L) return

        val isFinished = totalDurationMs > 0L && positionMs >= (totalDurationMs * COMPLETION_THRESHOLD)
        val isEligibleForResume = totalDurationMs >= MIN_DURATION_FOR_HISTORY_MS && !isFinished

        // Защита от случайных мисскликов: если просмотр длился менее 1 секунды и ролик ещё не в истории
        if (positionMs < MIN_PLAYBACK_START_MS && !historyMap.containsKey(item.id)) {
            return
        }

        val previouslyCompleted = historyMap[item.id]?.isCompleted == true
        val markCompleted = isFinished || (previouslyCompleted && positionMs < MIN_PLAYBACK_FOR_SAVE_MS)

        // Позиция возобновления сохраняется только для длинных роликов (>= 2 мин) при просмотре от 5 секунд
        val targetPosition = when {
            markCompleted -> 0L
            !isEligibleForResume -> 0L
            positionMs < MIN_PLAYBACK_FOR_SAVE_MS -> 0L
            else -> positionMs
        }

        val entry = XHistoryItem(
            item = item,
            lastPositionMs = targetPosition,
            totalDurationMs = totalDurationMs,
            updatedAt = System.currentTimeMillis(),
            isCompleted = markCompleted
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
     * Пакетное удаление записей из истории по ID роликов.
     */
    fun deleteBatchByIds(ids: Collection<Long>) {
        val validIds = ids.filter { it > 0L }
        if (validIds.isEmpty()) return
        scope.launch(ioDispatcher) {
            val deletedIds = mutableListOf<Long>()
            validIds.forEach { id ->
                historyDb.delete(id.toString())
                    .onSuccess { deletedIds.add(id) }
                    .onFailure { e ->
                        Timber.e(e, "SavedX_History: не удалось удалить %d", id)
                    }
            }
            if (deletedIds.isNotEmpty()) {
                val set = deletedIds.toSet()
                withContext(Dispatchers.Main) {
                    deletedIds.forEach { historyMap.remove(it) }
                    list.removeAll { it.item.id in set }
                }
                SnackBar.info("Удалено ${deletedIds.size} из истории")
            }
        }
    }

    /**
     * Пакетное удаление записей из истории по списку элементов.
     */
    fun deleteBatch(items: Collection<ItemsX>) {
        deleteBatchByIds(items.map { it.id })
    }

    /**
     * Полная очистка истории просмотров.
     */
    fun clearAll() {
        scope.launch(ioDispatcher) {
            historyDb.clear()
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        historyMap.clear()
                    }
                    SnackBar.info("История очищена")
                }
                .onFailure { e ->
                    Timber.e(e, "SavedX_History: ошибка при очистке истории")
                    SnackBar.error("Ошибка очистки истории: ${e.message}")
                }
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
        const val MIN_PLAYBACK_FOR_SAVE_MS = 5_000L      // 5 секунд (порог возобновления)
        const val MIN_PLAYBACK_START_MS = 1_000L         // 1 секунда (порог фиксации в истории)
        const val COMPLETION_THRESHOLD = 0.95f           // 95% длительности
    }
}
