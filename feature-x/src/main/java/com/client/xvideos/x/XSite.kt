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
 *
 * @param href Исходный URL или относительный путь.
 * @return Абсолютный канонический URL.
 */
fun normalizeXUrl(href: String): String {
    val trimmed = href.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    if (trimmed.startsWith("//")) return "https:$trimmed"
    return "$urlStart/${trimmed.removePrefix("/")}"
}

/**
 * Расширение для строки: приводит относительный или абсолютный URL к нормализованному виду раздела X.
 */
fun String.toNormalizedXUrl(): String = normalizeXUrl(this)

/**
 * Проверяет, является ли переданная строка валидным URL для ресурсов раздела X.
 *
 * @param href Проверяемый URL.
 * @return `true`, если строка не пуста и имеет корректный префикс схемы или пути.
 */
fun isValidXUrl(href: String): Boolean =
    href.isNotBlank() && (href.startsWith("http://") || href.startsWith("https://") || href.startsWith("/") || href.startsWith("//"))

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
 *
 * @param href URL страницы с видео.
 * @return Уникальный [Long] идентификатор видео либо `null`, если идентификатор не найден.
 */
fun extractXVideoId(href: String): Long? {
    if (href.isBlank() || !href.contains("/video")) return null
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
 * Проверяет, указывает ли данный URL на страницу видео раздела X.
 */
fun isXVideoUrl(href: String): Boolean = extractXVideoId(href) != null

/**
 * Извлекает числовой идентификатор видео из URL либо возвращает значение по умолчанию.
 *
 * @param href URL страницы видео.
 * @param default Значение по умолчанию (по умолчанию 0L).
 * @return Извлеченный идентификатор или [default].
 */
fun extractXVideoIdOrDefault(href: String?, default: Long = 0L): Long =
    if (!href.isNullOrBlank()) extractXVideoId(href) ?: default else default

/**
 * Разбирает строку длительности в формате с двоеточиями (`MM:SS` или `HH:MM:SS`) в миллисекунды.
 * При невозможности разбора возвращает 0L.
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

/**
 * Разбирает произвольную текстовую длительность видео (например, `"10 мин."`, `"15 min"`, `"1 hr 12 min"`, `"12:34"`)
 * в миллисекунды.
 *
 * Поддерживает форматирование с двоеточиями, текстовые обозначения на русском и английском,
 * а также чистые числовые значения (трактуемые как минуты при <= 180 либо секунды).
 * При невозможности разбора возвращает 0L.
 *
 * @param raw Исходный текст длительности.
 * @return Длительность ролика в миллисекундах.
 */
fun parseDurationToMs(raw: String): Long {
    if (raw.isBlank()) return 0L
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

/**
 * Извлекает текстовый слаг/токен ролика из URL (например `"uicfdab07bd"` из `"/video.uicfdab07bd/_"`).
 */
fun extractXVideoSlug(href: String): String? {
    if (href.isBlank() || !href.contains("/video")) return null
    return SLUG_VIDEO_ID_REGEX.find(href)?.groupValues?.get(1)
}

/**
 * Проверяет, начинается ли ссылка с канонического домена раздела X [urlStart].
 */
fun isCanonicalXUrl(url: String): Boolean = url.startsWith(urlStart)

/**
 * Extension-проверка для строки: указывает ли она на URL видео X.
 */
fun String.isXVideoLink(): Boolean = isXVideoUrl(this)

/**
 * Разбирает произвольную текстовую длительность видео в секунды.
 */
fun parseDurationToSeconds(raw: String): Long = parseDurationToMs(raw) / 1000L

/**
 * Форматирует миллисекунды в формат времени `"MM:SS"` или `"H:MM:SS"`.
 */
fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
