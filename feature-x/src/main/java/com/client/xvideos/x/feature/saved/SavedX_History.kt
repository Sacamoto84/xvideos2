package com.client.xvideos.x.feature.saved

import androidx.compose.runtime.Stable
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.XHistoryItem
import com.client.xvideos.x.parseDurationToMs
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
 *
 * @property scope CoroutineScope для выполнения операций дискового ввода-вывода.
 * @property ioDispatcher Диспетчер для выполнения фоновых файловых операций (по умолчанию [Dispatchers.IO]).
 */
@Stable
class SavedX_History(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val historyDb = FileDB(AppPath.x_history, EXTENSION, XHistoryItem.serializer())

    /** Реактивный список записей для UI экрана истории. */
    val list = historyDb.list

    val isEmpty: Boolean get() = list.isEmpty()
    val isNotEmpty: Boolean get() = list.isNotEmpty()
    val count: Int get() = list.size

    /** Быстрый in-memory доступ по id для плеера без лишнего дискового I/O. */
    private val historyMap = ConcurrentHashMap<Long, XHistoryItem>()

    /**
     * Все операции с файлами истории — по одной и в порядке вызова. Плеер сохраняет
     * позицию раз в 3 секунды, при паузе и при закрытии; параллельные записи одного
     * файла завершались вразнобой — позиция откатывалась, удалённая запись возвращалась.
     */
    private val writeDispatcher = ioDispatcher.limitedParallelism(1)

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    /**
     * Возвращает сохранённый элемент истории по ID ролика.
     * Сначала проверяется in-memory кэш, затем чтение с диска.
     *
     * Чтение с диска идёт в очереди файловых операций, а не в потоке вызова: до конца
     * первого [refresh] записи в памяти может не быть, а синхронное чтение с главного
     * потока ждало бы, пока `refresh` под тем же замком `FileDB` читает до 200 файлов.
     *
     * @param id Числовой ID видео.
     * @return Объект [XHistoryItem] или `null`, если ролик не найден в истории.
     */
    suspend fun get(id: Long): XHistoryItem? {
        if (id <= 0L) return null
        historyMap[id]?.let { return it }
        return withContext(writeDispatcher) {
            historyMap[id] ?: historyDb.read(id.toString()).getOrNull()?.also {
                historyMap[id] = it
            }
        }
    }

    /**
     * Позиция возобновления ролика в секундах или `null`, если продолжать нечего
     * (ролика нет в истории, он короткий, досмотрен или едва начат).
     */
    suspend fun resumePositionSeconds(id: Long): Float? =
        get(id)?.takeIf { it.isEligibleForResume }?.let { it.lastPositionMs / 1000f }

    /**
     * Сохраняет позицию, которую сообщил плеер.
     *
     * Длительность берётся у плеера, а пока он её не знает — из текста карточки.
     * Нечисловая или отрицательная позиция считается нулём и не выходит за длительность.
     *
     * Плеер, который ещё ничего не сообщил (позиция 0 и длительность неизвестна),
     * ничего не записывает: плеер при открытии сохраняет позицию сразу, и такая
     * запись затирала точку возобновления ролика нулём.
     *
     * @param item Объект видео.
     * @param positionSeconds Позиция плеера в секундах.
     * @param playerDurationSeconds Длительность по данным плеера в секундах; 0 — ещё неизвестна.
     */
    fun savePlayerProgress(item: ItemsX, positionSeconds: Float, playerDurationSeconds: Int) {
        val safeSeconds = positionSeconds.takeIf { it.isFinite() && it >= 0f } ?: 0f
        if (safeSeconds == 0f && playerDurationSeconds <= 0) return
        val playerDurationMs = playerDurationSeconds.coerceAtLeast(0) * 1000L
        val durationMs = if (playerDurationMs > 0L) playerDurationMs else parseDurationToMs(item.duration)
        val maxPos = if (durationMs > 0L) durationMs else Long.MAX_VALUE
        val positionMs = (safeSeconds * 1000f).toLong().coerceIn(0L, maxPos)
        updateProgress(item, positionMs, durationMs)
    }

    /**
     * Обновляет прогресс воспроизведения ролика в истории.
     *
     * - Ролик фиксируется в истории, если воспроизведение длилось более 1 секунды.
     * - Позиция возобновления сохраняется только для роликов длительностью >= 2 минут (120 000 мс)
     *   и если просмотрено не менее 5 секунд. Для коротких (< 2 мин) или досмотренных (>= 95%)
     *   позиция сбрасывается на 0L (ролик начнётся сначала).
     *
     * @param item Объект видео.
     * @param positionMs Текущая позиция воспроизведения в миллисекундах.
     * @param totalDurationMs Общая длительность видео в миллисекундах.
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

        scope.launch(writeDispatcher) {
            historyDb.insert(item.id.toString(), entry)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val existingIndex = list.indexOfFirst { it.item.id == item.id }
                        if (existingIndex >= 0) {
                            list.removeAt(existingIndex)
                        }
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
     *
     * @param item Объект ролика для удаления.
     */
    fun delete(item: ItemsX) {
        if (item.id <= 0L) return
        scope.launch(writeDispatcher) {
            historyDb.delete(item.id.toString())
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        historyMap.remove(item.id)
                        val existingIndex = list.indexOfFirst { it.item.id == item.id }
                        if (existingIndex >= 0) {
                            list.removeAt(existingIndex)
                        }
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
     *
     * @param ids Набор идентификаторов видео для удаления.
     */
    fun deleteBatchByIds(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        scope.launch(writeDispatcher) {
            val deletedIds = HashSet<Long>(ids.size)
            ids.forEach { id ->
                if (id > 0L) {
                    historyDb.delete(id.toString())
                        .onSuccess { deletedIds.add(id) }
                        .onFailure { e ->
                            Timber.e(e, "SavedX_History: не удалось удалить %d", id)
                        }
                }
            }
            if (deletedIds.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    deletedIds.forEach { historyMap.remove(it) }
                    list.removeAll { it.item.id in deletedIds }
                }
                SnackBar.info("Удалено ${deletedIds.size} из истории")
            }
        }
    }

    /**
     * Пакетное удаление записей из истории по списку элементов.
     *
     * @param items Коллекция удаляемых элементов.
     */
    fun deleteBatch(items: Collection<ItemsX>) {
        if (items.isEmpty()) return
        deleteBatchByIds(items.map { it.id })
    }

    /**
     * Полная очистка истории просмотров.
     */
    fun clearAll() {
        scope.launch(writeDispatcher) {
            historyDb.clear()
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        historyMap.clear()
                        list.clear()
                    }
                    SnackBar.info("История очищена")
                }
                .onFailure { e ->
                    Timber.e(e, "SavedX_History: ошибка при очистке истории")
                    SnackBar.error("Ошибка очистки истории: ${e.message}")
                }
        }
    }

    /**
     * Перечитывает записи из файловой БД и сортирует их по времени последнего просмотра.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(writeDispatcher) {
            historyDb.refresh()
            withContext(Dispatchers.Main) {
                // Карта чистится до проверки на пустоту: после восстановления
                // бэкапа история на диске может оказаться пустой, и ранний выход
                // оставлял в памяти позиции роликов, которых на диске уже нет.
                historyMap.clear()
                if (list.isEmpty()) return@withContext
                // Сортировка по времени последнего просмотра от новых к старым
                val sorted = list.sortedByDescending { it.updatedAt }
                list.clear()
                list.addAll(sorted)
                sorted.forEach { historyMap[it.item.id] = it }
                pruneExcessItemsLocked()
            }
        }
    }

    /** Удаляет старые записи сверх лимита [MAX_HISTORY_ITEMS] по принципу FIFO. */
    private fun pruneExcessItemsLocked() {
        if (list.size <= MAX_HISTORY_ITEMS) return
        val excess = list.subList(MAX_HISTORY_ITEMS, list.size).toList()
        while (list.size > MAX_HISTORY_ITEMS) {
            list.removeAt(list.lastIndex)
        }
        scope.launch(writeDispatcher) {
            excess.forEach { entry ->
                historyMap.remove(entry.item.id)
                historyDb.delete(entry.item.id.toString())
            }
        }
    }

    companion object {
        private const val EXTENSION = "XHistoryItem"
        /** Максимальное количество записей в истории (200 штук). */
        const val MAX_HISTORY_ITEMS = 200
        /** Минимальная общая длительность ролика (2 минуты), при которой сохраняется позиция возобновления. */
        const val MIN_DURATION_FOR_HISTORY_MS = 120_000L
        /** Минимальное время просмотра (5 секунд) для сохранения позиции возобновления. */
        const val MIN_PLAYBACK_FOR_SAVE_MS = 5_000L
        /** Минимальное время просмотра (1 секунда) для добавления в историю. */
        const val MIN_PLAYBACK_START_MS = 1_000L
        /** Порог досмотра ролика (95%), после которого просмотр считается завершенным. */
        const val COMPLETION_THRESHOLD = 0.95f
    }
}
