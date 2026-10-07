package com.client.xvideos.r.common.saved

/**
 * Ставит [item] в конец списка сохранённого: новая запись добавляется, а уже
 * сохранённая переезжает в конец. [sameKey] узнаёт прежнюю копию записи.
 *
 * Если запись уже стоит последней и не изменилась, список не трогается —
 * экраны не перерисовываются зря. Проверка «запись найдена» здесь обязательна:
 * у пустого списка и «не найдено», и «последний индекс» равны -1.
 */
internal fun <T> MutableList<T>.putLast(item: T, sameKey: (T) -> Boolean) {
    val existingIndex = indexOfFirst(sameKey)
    if (existingIndex >= 0 && existingIndex == lastIndex && this[existingIndex] == item) {
        return
    }
    if (existingIndex >= 0) {
        removeAt(existingIndex)
    }
    add(item)
}
