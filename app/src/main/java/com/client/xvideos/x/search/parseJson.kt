package com.client.xvideos.x.screens.search

import com.client.xvideos.x.screens.search.model.SearchResult
import kotlinx.serialization.json.Json
import timber.log.Timber

// Лояльный парсер: новые/неизвестные поля в ответе API не должны ронять поиск.
private val searchJson = Json { ignoreUnknownKeys = true }

fun parseJson(json: String): SearchResult? {
    return try {
        searchJson.decodeFromString(SearchResult.serializer(), json)
    } catch (e: Exception) {
        Timber.e(e, "parseJson: не удалось разобрать ответ поиска")
        null
    }
}
