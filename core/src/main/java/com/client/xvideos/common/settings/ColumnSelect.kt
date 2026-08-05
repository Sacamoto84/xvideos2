package com.client.xvideos.common.settings

import com.client.xvideos.common.settings.element.SettingElementInt
import com.client.xvideos.common.settings.element.SettingElementList

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
    val flags = list.field.value
    val enabledIndices = flags.mapIndexedNotNull { index, enabled -> if (enabled && index in 1..4) index else null }
    if (enabledIndices.isEmpty()) return
    val currentIndex = pref.field.value
    val currentPos = enabledIndices.indexOf(currentIndex)
    val nextPos = if (currentPos == -1) 0 else (currentPos + 1) % enabledIndices.size

    pref.setValue(enabledIndices[nextPos])
}
