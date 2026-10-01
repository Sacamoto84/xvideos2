package com.client.xvideos.x.feature.net

import com.client.xvideos.x.search.buildSuggestUrl
import com.client.xvideos.x.urlStart
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Заголовки запроса подсказок. Клиент общий с [fetchHtml]: свой пул соединений
 * и потоков ради одного эндпоинта не нужен, отличаются только заголовки.
 */
private val SUGGEST_HEADERS = mapOf(
    HttpHeaders.Referrer to "$urlStart/",
    HttpHeaders.Origin to urlStart,
    HttpHeaders.UserAgent to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 YaBrowser/25.6.0.0 Safari/537.36",
    HttpHeaders.Accept to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    HttpHeaders.AcceptEncoding to "identity",
    HttpHeaders.AcceptLanguage to "ru,en;q=0.9",
)

/**
 * Запрашивает поисковые подсказки (автокомплит) по введенному префиксу запроса.
 *
 * @param query Пользовательский поисковый запрос.
 * @return Сырой JSON-ответ API либо `null` при ошибке сети или пустом запросе.
 */
suspend fun getSearchResults(query: String): String? {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return null
    val url = buildSuggestUrl(trimmed)

    return try {
        fetchHtml(url, SUGGEST_HEADERS)
    } catch (e: CancellationException) {
        // Иначе отмена возвращалась как null и трактовалась как «ничего не найдено».
        throw e
    } catch (e: HttpStatusException) {
        Timber.w("getSearchResults: HTTP ${e.code} for $url")
        null
    } catch (e: Exception) {
        Timber.e(e, "Ошибка getSearchResults: ${e.message}")
        null
    }
}
