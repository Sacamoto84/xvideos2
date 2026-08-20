package com.client.xvideos.x.parcer

//https://cdn77-pic.xvideos-cdn.com/videos/thumbs169ll/6a/4f/6b/6a4f6bafe3abb03b5ea6108ab18ff1ad/6a4f6bafe3abb03b5ea6108ab18ff1ad.30.jpg
//https://cdn77-pic.xvideos-cdn.com/videos/videopreview/6a/4f/6b/6a4f6bafe3abb03b5ea6108ab18ff1ad_169.mp4

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

    val source = s?.trim().orEmpty()
    // Строку "null" продолжаем узнавать на входе: она уже записана в файлы
    // избранного прошлыми версиями и приходит оттуда через Gson.
    if (source.isBlank() || source.equals("null", ignoreCase = true)) {
        return null
    }

    val url = source.substringBefore('?').substringBefore('#')
    val parts = url.split("/")
    val newCdnPreview = parserNewCdnPreviewUrl(parts)
    if (newCdnPreview != null) {
        return newCdnPreview
    }

    val videosIndex = parts.indexOf("videos")
    val fileName = parts.lastOrNull().orEmpty()
    val hash = fileName
        .substringBefore('.')
        .replace(Regex("-\\d+$"), "")
        .takeIf { it.isNotBlank() }
        ?: return null

    val folders = if (videosIndex >= 0 && parts.size > videosIndex + 4) {
        parts.subList(videosIndex + 2, videosIndex + 5)
    } else if (hash.length >= 6) {
        listOf(hash.substring(0, 2), hash.substring(2, 4), hash.substring(4, 6))
    } else {
        return null
    }

    if (videosIndex < 0) return null

    val previewParts = buildList {
        addAll(parts.take(videosIndex + 1))
        add("videopreview")
        addAll(folders)
        add("${hash}_169.mp4")
    }
    return previewParts.joinToString("/")
}

private fun parserNewCdnPreviewUrl(parts: List<String>): String? {
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

    val pathParts = parts.drop(hostIndex + 1)
    if (pathParts.size < 3) return null

    return parts.dropLast(1).joinToString("/") + "/preview.mp4"
}
