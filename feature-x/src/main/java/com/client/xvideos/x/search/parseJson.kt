package com.client.xvideos.x.search

import com.client.xvideos.x.search.model.SearchResult
import kotlinx.serialization.json.Json
import timber.log.Timber

// Лояльный парсер: новые/неизвестные поля в ответе API не должны ронять поиск.
private val searchJson = Json { ignoreUnknownKeys = true }

fun parseJson(json: String?): SearchResult? {
    if (json.isNullOrBlank()) return null
    val trimmed = json.trim()
    if (trimmed.length < 2 || trimmed.first() != '{' || trimmed.last() != '}') return null
    return try {
        searchJson.decodeFromString(SearchResult.serializer(), trimmed)
    } catch (e: Exception) {
        Timber.e(e, "parseJson: не удалось разобрать ответ поиска")
        null
    }
}
