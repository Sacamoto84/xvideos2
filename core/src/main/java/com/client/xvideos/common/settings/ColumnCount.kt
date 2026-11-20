package com.client.xvideos.common.settings

import android.content.SharedPreferences
import com.client.xvideos.common.settings.element.SettingElementInt

private const val DEFAULT_COLUMN_COUNT = 2
private const val MAX_COLUMN_COUNT = 4

internal fun SharedPreferences.columnCountSetting(name: String) =
    SettingElementInt(this, name, default = DEFAULT_COLUMN_COUNT) { count ->
        count.takeIf { it in DEFAULT_COLUMN_COUNT..MAX_COLUMN_COUNT } ?: DEFAULT_COLUMN_COUNT
    }

fun advanceColumnCount(setting: SettingElementInt) {
    val current = setting.field.value
    val next = if (current in DEFAULT_COLUMN_COUNT until MAX_COLUMN_COUNT) current + 1 else DEFAULT_COLUMN_COUNT
    setting.setValue(next)
}
