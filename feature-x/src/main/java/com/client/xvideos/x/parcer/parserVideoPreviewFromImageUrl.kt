package com.client.xvideos.x.parcer

//https://cdn77-pic.xvideos-cdn.com/videos/thumbs169ll/6a/4f/6b/6a4f6bafe3abb03b5ea6108ab18ff1ad/6a4f6bafe3abb03b5ea6108ab18ff1ad.30.jpg
//https://cdn77-pic.xvideos-cdn.com/videos/videopreview/6a/4f/6b/6a4f6bafe3abb03b5ea6108ab18ff1ad_169.mp4

private val TRAILING_INDEX_REGEX = Regex("-\\d+$")

/**
 * Собирает адрес видео-превью из адреса картинки-превью. `null` — не получилось.
 *
 * Признаком неудачи раньше служила **строка** `"null"`. Проверял её один
 * вызывающий из трёх, поэтому она успевала лечь в `ItemsX.previewVideo` и
 * вернуться сюда же на следующем экране: разбор снова давал `"null"`, откат
 * `?: item.previewVideo` доставал из модели ту же строку, и в плеер уходил
 * адрес из четырёх букв. Настоящий `null` такого круга не даёт — его нельзя
 * забыть проверить.
 *
 * Параметр нullable намеренно: сюда приходят поля моделей, разобранных Gson, а он
 * умеет положить `null` в поле с типом `String` (см. [com.client.xvideos.x.model.ItemsX]).
 * Раньше сигнатура была non-null, и такой `null` ронял приложение прямо в
 * композиции — рантайм-проверкой Kotlin на входе в функцию.
 */
fun parserVideoPreviewFromImageUrl(s: String?): String? {
    if (s.isNullOrBlank()) return null
    val source = s.trim()
    // Строку "null" продолжаем узнавать на входе: она уже записана в файлы
    // избранного прошлыми версиями и приходит оттуда через Gson.
    if (source.equals("null", ignoreCase = true)) {
        return null
    }

    val url = source.substringBefore('?').substringBefore('#')
    if (!url.contains("videos") && !url.contains("xvideos-cdn.com", ignoreCase = true)) {
        return null
    }
    val parts = url.split("/")
    val newCdnPreview = parserNewCdnPreviewUrl(url, parts)
    if (newCdnPreview != null) {
        return newCdnPreview
    }

    val videosIndex = parts.indexOf("videos")
    if (videosIndex < 0) return null

    val fileName = parts.lastOrNull().orEmpty()
    val rawHash = fileName.substringBefore('.')
    if (rawHash.isEmpty()) return null
    val hash = if (rawHash.contains('-')) rawHash.replace(TRAILING_INDEX_REGEX, "") else rawHash
    if (hash.isEmpty()) return null

    val f0: String
    val f1: String
    val f2: String
    if (parts.size > videosIndex + 4) {
        f0 = parts[videosIndex + 2]
        f1 = parts[videosIndex + 3]
        f2 = parts[videosIndex + 4]
    } else if (hash.length >= 6) {
        f0 = hash.substring(0, 2)
        f1 = hash.substring(2, 4)
        f2 = hash.substring(4, 6)
    } else {
        return null
    }

    val sb = StringBuilder(url.length + 20)
    for (i in 0..videosIndex) {
        if (i > 0) sb.append('/')
        sb.append(parts[i])
    }
    sb.append("/videopreview/")
        .append(f0).append('/')
        .append(f1).append('/')
        .append(f2).append('/')
        .append(hash).append("_169.mp4")
    return sb.toString()
}

private fun parserNewCdnPreviewUrl(url: String, parts: List<String>): String? {
    val hostIndex = parts.indexOfFirst { it.contains("xvideos-cdn.com", ignoreCase = true) }
    if (hostIndex < 0) return null

    val host = parts[hostIndex]
    if (!host.startsWith("thumb", ignoreCase = true)) return null

    val fileName = parts.lastOrNull().orEmpty()
    if (!fileName.equals("preview.mp4", ignoreCase = true) &&
        !fileName.startsWith("xv_", ignoreCase = true) &&
        !fileName.startsWith("mozaique", ignoreCase = true)
    ) {
        return null
    }

    if (parts.size - (hostIndex + 1) < 3) return null

    return url.substringBeforeLast('/') + "/preview.mp4"
}
