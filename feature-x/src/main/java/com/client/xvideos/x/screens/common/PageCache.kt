package com.client.xvideos.x.screens.common

/**
 * Уже загруженные страницы ленты по номеру.
 *
 * Пейджер держит в композиции одну соседнюю страницу, а экран уходит из
 * композиции при переходе в плеер: без кэша в ScreenModel возврат к странице
 * грузил её из сети заново. Хранит [maxPages] последних страниц; страница
 * старше [ttlMs] считается устаревшей и грузится снова.
 *
 * Не потокобезопасен: обращения — с главного потока, как из LaunchedEffect.
 *
 * @param nowMs Монотонное время в миллисекундах; подменяется в тестах.
 */
internal class PageCache<T : Any>(
    private val maxPages: Int,
    private val ttlMs: Long = Long.MAX_VALUE,
    private val nowMs: () -> Long = { System.nanoTime() / NANOS_IN_MILLI },
) {
    private class Entry<T>(val value: T, val savedAtMs: Long)

    private val entries = object : LinkedHashMap<Int, Entry<T>>(maxPages, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Entry<T>>?): Boolean =
            size > maxPages
    }

    /** Страница [page], если она загружена и не устарела. */
    operator fun get(page: Int): T? {
        val entry = entries[page] ?: return null
        if (nowMs() - entry.savedAtMs > ttlMs) {
            entries.remove(page)
            return null
        }
        return entry.value
    }

    operator fun set(page: Int, value: T) {
        entries[page] = Entry(value, nowMs())
    }

    fun clear() {
        entries.clear()
    }

    private companion object {
        const val NANOS_IN_MILLI = 1_000_000L
    }
}
