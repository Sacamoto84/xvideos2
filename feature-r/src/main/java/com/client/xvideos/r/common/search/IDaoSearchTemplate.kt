package com.client.xvideos.r.common.search

import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс DAO для хранения истории поисковых запросов в текстовом виде.
 */
interface IDaoSearchTemplate {

    /** Наблюдает за полным списком сохраненных поисковых строк. */
    fun observeAllTexts(): Flow<List<String>>

    /** Вставляет поисковый запрос [text] в начало и ограничивает размер истории числом [limit]. */
    suspend fun insertAndTrim(text: String, limit: Int = 10)

    /** Удаляет указанный поисковый запрос [text] из истории. */
    suspend fun deleteByTexts(text: String)

    /** Полностью очищает историю поиска. */
    suspend fun deleteAll()
}
