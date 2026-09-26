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

/**
 * Безопасное чтение строки из [SharedPreferences] с гарантированным дефолтом при null или ошибке типа.
 */
fun SharedPreferences.getStringOrDefault(key: String, default: String): String =
    runCatching { getString(key, default) }.getOrNull() ?: default

/**
 * Безопасное чтение строки из [SharedPreferences] с null при отсутствии или ошибке типа.
 */
fun SharedPreferences.getStringOrNull(key: String): String? =
    runCatching { getString(key, null) }.getOrNull()

/**
 * Безопасное чтение булевого флага из [SharedPreferences] с fallback при сбое или несовпадении типа.
 */
fun SharedPreferences.getBooleanOrDefault(key: String, default: Boolean): Boolean =
    runCatching { getBoolean(key, default) }.getOrDefault(default)

/**
 * Безопасное чтение Int из [SharedPreferences] с fallback при сбое или несовпадении типа.
 */
fun SharedPreferences.getIntOrDefault(key: String, default: Int): Int =
    runCatching { getInt(key, default) }.getOrDefault(default)

/**
 * Безопасное чтение Long из [SharedPreferences] с fallback при сбое или несовпадении типа.
 */
fun SharedPreferences.getLongOrDefault(key: String, default: Long): Long =
    runCatching { getLong(key, default) }.getOrDefault(default)

/**
 * Безопасное чтение Float из [SharedPreferences] с fallback при сбое или несовпадении типа.
 */
fun SharedPreferences.getFloatOrDefault(key: String, default: Float): Float =
    runCatching { getFloat(key, default) }.getOrDefault(default)
