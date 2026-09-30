package com.client.xvideos.r.ui.explorer.tab.gifs

import com.client.xvideos.common.settings.element.SettingElementInt

private val rColumnOptions = listOf(2, 3, 4)

fun normalizeRColumnCount(value: Int): Int {
    return value.takeIf { it in rColumnOptions } ?: rColumnOptions.first()
}

fun ColumnSelect_AddRColumn(pref: SettingElementInt) {
    val currentIndex = normalizeRColumnCount(pref.field.value)
    val currentPos = rColumnOptions.indexOf(currentIndex)
    val nextPos = (currentPos + 1) % rColumnOptions.size
    pref.setValue(rColumnOptions[nextPos])
}
