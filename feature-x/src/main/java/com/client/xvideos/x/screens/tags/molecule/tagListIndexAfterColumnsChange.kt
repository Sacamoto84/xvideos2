package com.client.xvideos.x.screens.tags.molecule

/**
 * Индекс элемента списка тега после смены числа видео в ряду.
 *
 * Ряды страницы тега собираются вручную (`chunked`), поэтому список хранит
 * позицию по номеру ряда: при раскрытии складного 2 → 4 видео в ряду тот же
 * номер ряда показывал бы видео вдвое дальше. Пересчёт идёт через индекс
 * первого видео видимого ряда. Пока видна шапка, позиция не меняется.
 */
internal fun tagListIndexAfterColumnsChange(
    firstVisibleIndex: Int,
    headerCount: Int,
    oldPerRow: Int,
    newPerRow: Int,
): Int {
    if (oldPerRow == newPerRow || firstVisibleIndex < headerCount) return firstVisibleIndex
    val firstVideo = (firstVisibleIndex - headerCount) * oldPerRow
    return headerCount + firstVideo / newPerRow
}
