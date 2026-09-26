package com.client.xvideos.x.search

import com.client.xvideos.x.search.model.SearchResult
import kotlinx.serialization.json.Json
import timber.log.Timber

// Лояльный парсер: новые/неизвестные поля в ответе API не должны ронять поиск.
private val searchJson = Json { ignoreUnknownKeys = true }

/**
 * Десериализует JSON-ответ автодополнения поиска в типизированную модель [SearchResult].
 *
 * Безопасно валидирует формат JSON и подавляет ошибки несовместимости схемы, возвращая `null` при сбое.
 *
 * @param json Сырая строка JSON.
 * @return Распарсенный объект [SearchResult] или `null`.
 */
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

/**
 * Десериализует JSON-ответ поиска с возможностью возврата значения по умолчанию [default].
 */
fun parseJsonOrDefault(json: String?, default: SearchResult = SearchResult.EMPTY): SearchResult =
    parseJson(json) ?: default

/**
 * Проверяет, является ли строка синтаксически корректным JSON-ответом поиска.
 */
fun isValidSearchJson(json: String?): Boolean =
    parseJson(json) != null

/**
 * Быстро извлекает только список подсказок-ключевых слов из JSON-ответа.
 */
fun parseJsonKeywords(json: String?): List<String> =
    parseJson(json)?.keywords?.map { it.name } ?: emptyList()

/**
 * Быстро извлекает список найденных порнозвезд из JSON-ответа.
 */
fun parseJsonPornstars(json: String?): List<com.client.xvideos.x.search.model.Pornstar> =
    parseJson(json)?.pornstar.orEmpty()

/**
 * Быстро извлекает список найденных каналов из JSON-ответа.
 */
fun parseJsonChannels(json: String?): List<com.client.xvideos.x.search.model.Channel> =
    parseJson(json)?.channel.orEmpty()

/**
 * Возвращает суммарное количество подсказок (ключевые слова + модели + каналы) из ответа.
 */
fun parseJsonTotalCount(json: String?): Int =
    parseJson(json)?.totalSuggestionsCount ?: 0

