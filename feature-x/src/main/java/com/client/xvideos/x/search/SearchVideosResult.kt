package com.client.xvideos.x.search

import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.parcer.parserListVideo
import com.client.xvideos.x.urlStart
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder

/**
 * Результат скрапинга страницы поисковой выдачи видеороликов.
 *
 * @property items Список найденных карточек видео.
 * @property maxPages Максимальное количество доступных страниц пагинации (минимум 1).
 */
data class SearchVideosResult(
    val items: List<ItemsX>,
    val maxPages: Int
)

/**
 * Кодирует поисковый запрос в URL-безопасную строку.
 */
fun encodeSearchVideosQuery(query: String): String {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return ""
    return URLEncoder.encode(trimmed, Charsets.UTF_8.name())
}

/**
 * Формирует полный URL страницы выдачи видеопоиска X для заданного номера страницы.
 *
 * @param query Поисковый запрос.
 * @param page 0-based индекс страницы (0 = первая страница).
 */
fun buildSearchVideosUrl(query: String, page: Int): String {
    val encoded = encodeSearchVideosQuery(query)
    val pageSuffix = if (page <= 0) "" else "&p=$page"
    return "$urlStart/?k=$encoded$pageSuffix"
}

/**
 * Парсит HTML разметку страницы поиска в [SearchVideosResult].
 */
fun parseSearchVideosPage(html: String): SearchVideosResult {
    if (html.isBlank()) return SearchVideosResult(emptyList(), 1)
    val doc = Jsoup.parse(html)
    val items = parserListVideo(doc)
        .filter { !it.href.contains("THUMBNUM") }
        .distinctBy { it.id }
    val maxPages = parseSearchLastPage(doc)
    return SearchVideosResult(items = items, maxPages = maxPages)
}

/**
 * Извлекает максимальный номер страницы из блока пагинации `div.pagination`.
 */
internal fun parseSearchLastPage(document: Document): Int {
    val pagination = document.selectFirst("div.pagination") ?: return 1
    val lastPage = pagination.selectFirst("a.last-page")?.text()?.trim()?.toIntOrNull()
    if (lastPage != null) return lastPage.coerceAtLeast(1)

    var maxPage = 1
    for (el in pagination.getElementsByTag("a")) {
        val p = el.text().trim().toIntOrNull()
        if (p != null && p > maxPage) {
            maxPage = p
        }
    }
    return maxPage
}
