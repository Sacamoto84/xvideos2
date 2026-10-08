package com.client.xvideos.common.fileDB

/**
 * Ставит запись в начало списка сохранённого; прежняя запись с тем же ключом
 * уходит.
 *
 * В начало — потому что [FileDB.refresh] читает записи от новых к старым:
 * запись, дописанная в конец, стояла внизу списка, а после перезапуска —
 * наверху.
 *
 * Запись, которая уже стоит первой и не изменилась, список не трогает: лишняя
 * запись в список состояния — это лишняя перекомпозиция экрана.
 *
 * @param sameKey Та же ли это запись: сравнение по ключу, а не по содержимому.
 */
fun <T> MutableList<T>.putFirst(item: T, sameKey: (T) -> Boolean) {
    val existingIndex = indexOfFirst(sameKey)
    if (existingIndex == 0 && this[0] == item) return
    if (existingIndex >= 0) removeAt(existingIndex)
    add(0, item)
}
