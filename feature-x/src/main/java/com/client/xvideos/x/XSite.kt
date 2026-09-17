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

