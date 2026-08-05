package com.client.xvideos.common.util

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import timber.log.Timber

/**
 * Атомарно заменяет содержимое списка на [items].
 *
 * `clear()` + `addAll()` — две отдельные записи в snapshot-состояние, и между
 * ними Compose успевает отрисовать пустой список: на длинных лентах это видимое
 * мигание, а два параллельных обновления могут переплестись. Внутри
 * [Snapshot.withMutableSnapshot] обе записи публикуются подписчикам разом.
 *
 * Если вложенный mutable-снапшот недоступен (например, текущий snapshot
 * read-only), обновляем как раньше, без атомарности — это хуже мигания, но
 * лучше потери данных.
 */
fun <T> SnapshotStateList<T>.replaceWith(items: Collection<T>) {
    runCatching {
        Snapshot.withMutableSnapshot {
            clear()
            addAll(items)
        }
    }.onFailure { e ->
        Timber.w(e, "replaceWith: атомарное обновление недоступно, fallback")
        clear()
        addAll(items)
    }
}
