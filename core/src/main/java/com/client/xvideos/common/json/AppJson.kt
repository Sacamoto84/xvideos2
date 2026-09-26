package com.client.xvideos.common.json

import kotlinx.serialization.json.Json

/**
 * Единый экземпляр Json для дисковых хранилищ, кэша и бэкапа приложения.
 *
 * - ignoreUnknownKeys = true: устойчивость к расширению схем данных и миграциям.
 * - coerceInputValues = true: если в поле со значением по умолчанию приходит null,
 *   подставляется значение по умолчанию.
 * - isLenient = true: устойчивость к нестандартному форматированию JSON.
 * - encodeDefaults = true: сохраняет дефолтные значения полей для полной обратной совместимости.
 * - prettyPrint = true: читаемый формат файлов на диске (FileDB, бэкап).
 */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    encodeDefaults = true
    prettyPrint = true
}

/**
 * Компактный Json для однострочных настроек и внутренних ключей.
 */
val AppJsonCompact: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    encodeDefaults = true
    prettyPrint = false
}

inline fun <reified T> Json.decodeFromStringOrNull(string: String?): T? {
    if (string.isNullOrBlank()) return null
    return runCatching { decodeFromString<T>(string) }.getOrNull()
}

inline fun <reified T> Json.encodeToStringOrNull(value: T): String? =
    runCatching { encodeToString(value) }.getOrNull()

/**
 * Быстро проверяет, начинается и заканчивается ли строка корректными скобками JSON ({...} или [...]).
 */
fun isValidJsonStructure(string: String?): Boolean {
    if (string.isNullOrBlank()) return false
    val trimmed = string.trim()
    if (trimmed.length < 2) return false
    val first = trimmed.first()
    val last = trimmed.last()
    return (first == '{' && last == '}') || (first == '[' && last == ']')
}


