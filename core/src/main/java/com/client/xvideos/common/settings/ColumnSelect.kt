package com.client.xvideos.common.settings

import com.client.xvideos.common.settings.element.SettingElementInt
import com.client.xvideos.common.settings.element.SettingElementList

/**
 * Возвращает список индексов разрешённых колонок в диапазоне [minCol]..[maxCol].
 */
fun getEnabledColumns(flags: List<Boolean>, minCol: Int = 1, maxCol: Int = 4): List<Int> {
    if (flags.isEmpty()) return emptyList()
    return flags.mapIndexedNotNull { index, enabled ->
        if (enabled && index in minCol..maxCol) index else null
    }
}

/**
 * Вычисляет следующее разрешённое количество колонок циклически.
 * Если список доступных пуст, возвращает [currentIndex].
 */
fun calculateNextColumn(
    currentIndex: Int,
    flags: List<Boolean>,
    minCol: Int = 1,
    maxCol: Int = 4
): Int {
    val enabledIndices = getEnabledColumns(flags, minCol, maxCol)
    if (enabledIndices.isEmpty()) return currentIndex
    val currentPos = enabledIndices.indexOf(currentIndex)
    val nextPos = if (currentPos == -1) 0 else (currentPos + 1) % enabledIndices.size
    return enabledIndices[nextPos]
}

/**
 * Вычисляет предыдущее разрешённое количество колонок циклически.
 * Если список доступных пуст, возвращает [currentIndex].
 */
fun calculatePrevColumn(
    currentIndex: Int,
    flags: List<Boolean>,
    minCol: Int = 1,
    maxCol: Int = 4
): Int {
    val enabledIndices = getEnabledColumns(flags, minCol, maxCol)
    if (enabledIndices.isEmpty()) return currentIndex
    val currentPos = enabledIndices.indexOf(currentIndex)
    val prevPos = when {
        currentPos == -1 -> enabledIndices.size - 1
        currentPos == 0 -> enabledIndices.size - 1
        else -> currentPos - 1
    }
    return enabledIndices[prevPos]
}

/**
 * Переключает число колонок на следующее разрешённое значение.
 *
 * [list] — флаги «этот вариант доступен» для колонок 1..4, [pref] — текущий
 * выбор. Ключи настроек приходят параметрами: помощник ничего не знает про
 * конкретный раздел и одинаково работает и для L, и для R.
 *
 * Если ни один вариант не включён, значение не меняется.
 */
fun ColumnSelect_AddColumn(pref: SettingElementInt, list: SettingElementList<Boolean>) {
    val current = pref.field.value
    val next = calculateNextColumn(current, list.field.value)
    if (next != current) {
        pref.setValue(next)
    }
}

/**
 * Переключает число колонок на предыдущее разрешённое значение.
 */
fun ColumnSelect_PrevColumn(pref: SettingElementInt, list: SettingElementList<Boolean>) {
    val current = pref.field.value
    val prev = calculatePrevColumn(current, list.field.value)
    if (prev != current) {
        pref.setValue(prev)
    }
}
