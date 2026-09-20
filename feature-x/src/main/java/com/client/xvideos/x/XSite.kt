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
    if (trimmed.isBlank()) return ""
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    if (trimmed.startsWith("//")) return "https:$trimmed"
    return "$urlStart/${trimmed.removePrefix("/")}"
}

/**
 * Извлекает числовой или буквенно-цифровой идентификатор видео из URL xvideos.
 *
 * Поддерживает:
 * - Числовой формат: `/video12345/title` -> `12345L`
 * - Точечный формат с числом: `/video.12345/title` -> `12345L`
 * - Современный формат с токеном: `/video.uicfdab07bd/_` -> стабильный детерминированный положительный Long ID
 */
fun extractXVideoId(href: String): Long? {
    if (href.isBlank()) return null
    // 1. Числовой id: /video12345/ или /video.12345/
    val numericRegex = Regex("""/video\.?(\d+)""")
    val numericMatch = numericRegex.find(href)?.groupValues?.get(1)?.toLongOrNull()
    if (numericMatch != null && numericMatch > 0L) return numericMatch

    // 2. Буквенно-цифровой id (современные ссылки X: /video.uicfdab07bd/_ или /video_uicfdab07bd/):
    val slugRegex = Regex("""/video[._-]?([a-zA-Z0-9]+)""")
    val slugMatch = slugRegex.find(href)?.groupValues?.get(1)
    if (!slugMatch.isNullOrBlank()) {
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
fun parseDurationToMs(raw: String): Long {
    val text = raw.trim().lowercase()
    if (text.isBlank()) return 0L

    if (text.contains(":")) {
        val parts = text.split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60 + parts[1]) * 1000L
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
            else -> 0L
        }
    }

    var totalMs = 0L
    val hoursRegex = Regex("""(\d+)\s*(?:hr|ч|hour)""")
    val minutesRegex = Regex("""(\d+)\s*(?:min|мин|m)""")
    val secondsRegex = Regex("""(\d+)\s*(?:sec|сек|s)""")

    hoursRegex.find(text)?.groupValues?.get(1)?.toLongOrNull()?.let {
        totalMs += it * 3600_000L
    }
    minutesRegex.find(text)?.groupValues?.get(1)?.toLongOrNull()?.let {
        totalMs += it * 60_000L
    }
    secondsRegex.find(text)?.groupValues?.get(1)?.toLongOrNull()?.let {
        totalMs += it * 1000L
    }

    if (totalMs == 0L) {
        val digitsOnly = text.filter { it.isDigit() }.toLongOrNull() ?: 0L
        if (digitsOnly > 0L) {
            totalMs = if (digitsOnly <= 180) digitsOnly * 60_000L else digitsOnly * 1000L
        }
    }

    return totalMs
}

