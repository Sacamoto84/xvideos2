package com.client.xvideos.l.ui.screens.screenFullScreen

import com.client.xvideos.l.model.PicsDetails
import java.util.LinkedHashMap
import java.util.UUID

/**
 * Передача списка картинок открывающемуся экрану полноэкранного просмотра Luscious.
 *
 * Список не кладётся в сам Screen: сотни [PicsDetails] в Bundle дают
 * TransactionTooLarge. Раньше вместо этого была одна глобальная переменная, из-за
 * чего два быстрых открытия подряд перетирали список друг друга — второй экран
 * успевал записать свой раньше, чем первый его прочитает, и первый показывал
 * чужой альбом.
 *
 * Теперь у каждого открытия свой ключ, а хранилище ограничено несколькими
 * последними записями, чтобы список закрытого экрана не держался в памяти вечно.
 *
 * Смерть процесса хранилище не переживает — на этот случай экран показывает
 * единственную картинку, которая пришла в самом Screen.
 */
internal object LFullScreenPayload {

    private const val MAX_ENTRIES = 3

    private val store = object : LinkedHashMap<String, List<PicsDetails>>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<PicsDetails>>?
        ): Boolean = size > MAX_ENTRIES
    }

    /** Количество сохраненных полезных нагрузок в кэше. */
    val size: Int
        @Synchronized get() = store.size

    /** Флаг отсутствия сохраненных списков. */
    val isEmpty: Boolean
        @Synchronized get() = store.isEmpty()

    /** Флаг наличия сохраненных списков. */
    val isNotEmpty: Boolean
        @Synchronized get() = store.isNotEmpty()

    /**
     * Проверяет наличие полезной нагрузки по строковому [key].
     */
    @Synchronized
    fun containsKey(key: String?): Boolean = !key.isNullOrBlank() && store.containsKey(key)

    /**
     * Извлекает и удаляет полезную нагрузку по ключу [key].
     */
    @Synchronized
    fun remove(key: String?): List<PicsDetails>? = if (!key.isNullOrBlank()) store.remove(key) else null

    /**
     * Полностью очищает хранилище полезных нагрузок.
     */
    @Synchronized
    fun clear() {
        store.clear()
    }

    /**
     * Сохраняет список картинок [items] в LRU-хранилище и возвращает уникальный строковый ключ UUID.
     *
     * @param items Список картинок для передачи на полноэкранный экран.
     * @return Сгенерированный UUID-ключ.
     */
    @Synchronized
    fun put(items: List<PicsDetails>): String {
        val key = UUID.randomUUID().toString()
        store[key] = items
        return key
    }

    /**
     * Возвращает список картинок по ключу [key] либо пустой список, если ключ не найден.
     */
    @Synchronized
    fun get(key: String?): List<PicsDetails> = if (!key.isNullOrBlank()) store[key].orEmpty() else emptyList()
}
