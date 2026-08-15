package com.client.xvideos.common.videoplayer.feed

import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableIntSet

/**
 * Что вызывающему нужно сделать с менеджером прогрева после изменения реестра.
 *
 * Существует, потому что `BasePreloadManager.add()` в media3 не идемпотентен: он
 * всегда создаёт новый `MediaSource` и кладёт holder поверх старого через `put()`,
 * не освобождая предыдущий. Повторный `add` одного и того же элемента течёт, а
 * играющий плеер остаётся с источником, которого уже нет в карте менеджера.
 * Поэтому решение «добавлять или нет» принимает реестр, а не вызывающий код.
 */
sealed interface PreloadAction<out T> {

    /** Индекс уже отдан ровно с этим элементом — трогать менеджер нельзя. */
    data object None : PreloadAction<Nothing>

    /** Индекса в прогреве не было — добавить. */
    data class Add<T>(val item: T) : PreloadAction<T>

    /** На индексе был другой элемент — сначала снять [old], затем добавить [new]. */
    data class Replace<T>(val old: T, val new: T) : PreloadAction<T>
}

/**
 * Учёт того, что именно отдано в прогрев: индекс в ленте → добавленный элемент.
 *
 * Собрать элемент заново по данным пейджинга в момент удаления нельзя: к тому
 * времени, когда индекс покидает окно, пейджинг уже мог выбросить его из списка —
 * url не найдётся, снятие с прогрева не случится, и источник останется в
 * менеджере навсегда. Поэтому помним сами.
 *
 * Generic и без media3-типов намеренно: так вся арифметика проверяется обычным
 * JVM-тестом, а работа с `DefaultPreloadManager` остаётся в [FeedPlayerState].
 *
 * `MutableIntObjectMap`/`MutableIntSet` вместо `Map<Int, _>`/`Set<Int>`: ключ —
 * примитивный индекс, и на каждом свайпе окно правится целыми диапазонами.
 * Обычные коллекции боксили бы каждый индекс в `Integer`.
 *
 * Только главный поток — см. контракт [FeedPlayerState].
 */
class FeedPreloadRegistry<T : Any> {

    private val tracked = MutableIntObjectMap<T>()

    /**
     * Индексы, которые вошли в окно, но не имели элемента на тот момент.
     *
     * `SlidingWindowEffect` считает диапазон вошедшим сразу и второй раз
     * `onRangeEnterWindow` для него не позовёт. Помним такие индексы, чтобы
     * догреть их, когда данные приедут.
     */
    private val pending = MutableIntSet()

    /**
     * Индекс отдан в прогрев с элементом [item].
     *
     * Снимает индекс с ожидания: элемент нашёлся, догревать больше нечего —
     * именно здесь закрывается сценарий, в котором страница добавляла источник
     * сама, индекс оставался в ожидании, и догрев добавлял его повторно.
     */
    fun track(index: Int, item: T): PreloadAction<T> {
        pending -= index
        val old = tracked.put(index, item)
        return when {
            old == null -> PreloadAction.Add(item)
            old == item -> PreloadAction.None
            else -> PreloadAction.Replace(old, item)
        }
    }

    /** Индекс вошёл в окно, но элемента для него ещё нет. */
    fun markPending(index: Int) {
        pending += index
    }

    /** Индекс вышел из окна. Возвращает элемент, который нужно снять с прогрева. */
    fun forget(index: Int): T? {
        pending -= index
        return tracked.remove(index)
    }

    /**
     * Ожидающие индексы отдельным списком.
     *
     * Именно копия: вызывающий будет звать [track] прямо во время обхода, а
     * править множество во время итерации нельзя.
     */
    fun pendingIndices(): List<Int> {
        if (pending.isEmpty()) return emptyList()
        val result = ArrayList<Int>(pending.size)
        pending.forEach { result += it }
        return result
    }

    fun clear() {
        tracked.clear()
        pending.clear()
    }
}
