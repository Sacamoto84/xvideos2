package com.client.xvideos.common.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Доступ к тем же `SharedPreferences`, что отдавал устаревший
 * `android.preference.PreferenceManager.getDefaultSharedPreferences()`,
 * но без зависимости от deprecated API.
 *
 * Имя файла намеренно совпадает с дефолтным (`"<packageName>_preferences"`,
 * режим [Context.MODE_PRIVATE]) — это ровно то, что использовал старый
 * `PreferenceManager` под капотом, поэтому все ранее сохранённые ключи
 * (настройки, хэш пароля app-lock, выбранная рабочая папка) остаются доступны.
 */
fun Context.defaultSharedPreferences(): SharedPreferences =
    getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
