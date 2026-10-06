package com.client.xvideos.x.search

import com.client.xvideos.x.urlStart
import java.net.URLEncoder

/**
 * Кодирует пользовательский поисковый запрос для передачи в URL подсказок.
 */
fun encodeSuggestQuery(query: String): String {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return ""
    val rawEncoded = URLEncoder.encode(trimmed, Charsets.UTF_8.name())
    return if (rawEncoded.contains('+')) rawEncoded.replace("+", "%20") else rawEncoded
}

/**
 * Формирует полный URL эндпоинта подсказок поиска X.
 */
fun buildSuggestUrl(query: String): String {
    val encoded = encodeSuggestQuery(query)
    return "$urlStart/search-suggest/$encoded"
}

