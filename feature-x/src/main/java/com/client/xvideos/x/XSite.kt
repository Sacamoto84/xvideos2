package com.client.xvideos.x

/**
 * Базовый URL сайта раздела X.
 *
 * Лежал в `MainActivity.kt` как константа верхнего уровня, хотя пользуется им
 * только X: парсер, поиск, загрузки и список стран.
 */
const val urlStart = "https://www.xv-ru.com"

/**
 * Нормализует относительный или абсолютный URL контента раздела X к каноническому виду.
 *
 * - Защищает от отсутствующих ведущих слэшей (например, `"video123"` -> `"https://www.xv-ru.com/video123"`).
 * - Нормализует протокольно-относительные ссылки (например, `"//cdn.xv-ru.com/video.mp4"` -> `"https://cdn.xv-ru.com/video.mp4"`).
 * - Сохраняет уже абсолютные протокольные ссылки (`http://`, `https://`).
 * - Возвращает пустую строку для пустых/пробельных ссылок.
 */
fun normalizeXUrl(href: String): String {
    val trimmed = href.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    if (trimmed.startsWith("//")) return "https:$trimmed"
    return "$urlStart/${trimmed.removePrefix("/")}"
}

private val NUMERIC_VIDEO_ID_REGEX = Regex("""/video\.?(\d+)""")
private val SLUG_VIDEO_ID_REGEX = Regex("""/video[._-]?([a-zA-Z0-9]+)""")
private val HOURS_REGEX = Regex("""(\d+)\s*(?:hr|ч|hour)""")
private val MINUTES_REGEX = Regex("""(\d+)\s*(?:min|мин|m)""")
private val SECONDS_REGEX = Regex("""(\d+)\s*(?:sec|сек|s)""")

/**
 * Извлекает числовой или буквенно-цифровой идентификатор видео из URL xvideos.
 *
 * Поддерживает:
 * - Числовой формат: `/video12345/title` -> `12345L`
 * - Точечный формат с числом: `/video.12345/title` -> `12345L`
 * - Современный формат с токеном: `/video.uicfdab07bd/_` -> стабильный детерминированный положительный Long ID
 */
fun extractXVideoId(href: String): Long? {
    if (href.isEmpty() || !href.contains("/video")) return null
    // 1. Числовой id: /video12345/ или /video.12345/
    val numericMatch = NUMERIC_VIDEO_ID_REGEX.find(href)?.groupValues?.get(1)?.toLongOrNull()
    if (numericMatch != null && numericMatch > 0L) return numericMatch

    // 2. Буквенно-цифровой id (современные ссылки X: /video.uicfdab07bd/_ или /video_uicfdab07bd/):
    val slugMatch = SLUG_VIDEO_ID_REGEX.find(href)?.groupValues?.get(1)
    if (!slugMatch.isNullOrEmpty()) {
        val bits = java.util.UUID.nameUUIDFromBytes(slugMatch.toByteArray(Charsets.UTF_8)).mostSignificantBits
        val positive = bits and Long.MAX_VALUE
        if (positive > 0L) return positive
    }
    return null
}

/**
 * Разбирает текстовую длительность видео (например, "10 мин.", "15 min", "1 hr 12 min", "12:34")
 * в миллисекунды. При невозможности разбора возвращает 0L.
 */
private fun parseColonDuration(text: String): Long {
    val firstColon = text.indexOf(':')
    if (firstColon == -1) return 0L

    val secondColon = text.indexOf(':', firstColon + 1)
    if (secondColon != -1) {
        val thirdColon = text.indexOf(':', secondColon + 1)
        if (thirdColon == -1) {
            val p1 = text.substring(0, firstColon).trim().toLongOrNull()
            val p2 = text.substring(firstColon + 1, secondColon).trim().toLongOrNull()
            val p3 = text.substring(secondColon + 1).trim().toLongOrNull()
            if (p1 != null && p2 != null && p3 != null) {
                return (p1 * 3600 + p2 * 60 + p3) * 1000L
            }
        }
    } else {
        val p1 = text.substring(0, firstColon).trim().toLongOrNull()
        val p2 = text.substring(firstColon + 1).trim().toLongOrNull()
        if (p1 != null && p2 != null) {
            return (p1 * 60 + p2) * 1000L
        }
    }
    return 0L
}

fun parseDurationToMs(raw: String): Long {
    val text = raw.trim().lowercase()
    if (text.isBlank()) return 0L

    if (text.contains(':')) {
        return parseColonDuration(text)
    }

    var totalMs = 0L
    HOURS_REGEX.find(text)?.groupValues?.get(1)?.toLongOrNull()?.let {
        totalMs += it * 3600_000L
    }
    MINUTES_REGEX.find(text)?.groupValues?.get(1)?.toLongOrNull()?.let {
        totalMs += it * 60_000L
    }
    SECONDS_REGEX.find(text)?.groupValues?.get(1)?.toLongOrNull()?.let {
        totalMs += it * 1000L
    }

    if (totalMs == 0L) {
        var digitsOnly = 0L
        var hasDigits = false
        for (i in 0 until text.length) {
            val c = text[i]
            if (c in '0'..'9') {
                hasDigits = true
                if (digitsOnly < Long.MAX_VALUE / 10) {
                    digitsOnly = digitsOnly * 10L + (c - '0')
                }
            }
        }
        if (hasDigits && digitsOnly > 0L) {
            totalMs = if (digitsOnly <= 180) digitsOnly * 60_000L else digitsOnly * 1000L
        }
    }

    return totalMs
}

