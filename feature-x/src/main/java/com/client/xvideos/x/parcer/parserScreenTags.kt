package com.client.xvideos.x.parcer

import com.client.xvideos.x.extractXVideoId
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ModelScreenTag
import kotlin.math.abs
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber

import org.jsoup.nodes.Element

private val EMPTY_MODEL_SCREEN_TAG = ModelScreenTag(title0 = "?", title1 = "?", items = emptyList(), lastPage = 1)

/**
 * Парсит HTML-страницу категории, тега или порнозвезды в модель [ModelScreenTag].
 *
 * Извлекает заголовки страницы, список карточек видеороликов и номер последней доступной страницы пагинации.
 *
 * @param html Сырой HTML-код страницы тега.
 * @return Модель экрана тега [ModelScreenTag].
 */
fun parserScreenTags(html: String): ModelScreenTag {
    if (html.isBlank()) {
        return EMPTY_MODEL_SCREEN_TAG
    }

    val document: Document = Jsoup.parse(html)

    val pageTitle = document.selectFirst("h2.page-title")
    val title0 = pageTitle?.ownText() ?: "?"                       // Текст внутри h2 без дочерних элементов
    val title1 = pageTitle?.selectFirst("span.sub")?.text() ?: "?" // Текст внутри span.sub
    val lastPage = parseLastPage(document)

    val container = document.selectFirst("#content > div.mozaique.cust-nb-cols") ?: document.selectFirst("div.mozaique")
    val videos = container?.select("div.frame-block.thumb-block") ?: document.select("div.frame-block.thumb-block")
    if (videos.isEmpty()) {
        return ModelScreenTag(title0 = title0, title1 = title1, items = emptyList(), lastPage = lastPage)
    }

    val listItems = ArrayList<ItemsX>(videos.size)
    for (video in videos) {
        parseTagItemVideo(video)?.let { listItems.add(it) }
    }

    return ModelScreenTag(title0 = title0, title1 = title1, items = listItems, lastPage = lastPage)
}

/**
 * Быстро извлекает основной и вспомогательный заголовки страницы тега (title0, title1).
 */
fun parseScreenTagTitle(html: String): Pair<String, String> {
    if (html.isBlank()) return Pair("?", "?")
    val document = Jsoup.parse(html)
    val pageTitle = document.selectFirst("h2.page-title")
    val title0 = pageTitle?.ownText() ?: "?"
    val title1 = pageTitle?.selectFirst("span.sub")?.text() ?: "?"
    return Pair(title0, title1)
}

/**
 * Извлекает количество страниц из HTML-разметки экрана тега.
 */
fun parseScreenTagPageCount(html: String): Int {
    if (html.isBlank()) return 1
    return parseLastPage(Jsoup.parse(html))
}

/**
 * Возвращает количество карточек видео, найденных на странице тега.
 */
fun parseTagItemCount(html: String): Int {
    if (html.isBlank()) return 0
    val document = Jsoup.parse(html)
    val container = document.selectFirst("#content > div.mozaique.cust-nb-cols") ?: document.selectFirst("div.mozaique")
    return (container?.select("div.frame-block.thumb-block") ?: document.select("div.frame-block.thumb-block")).size
}

/**
 * Находит и вычисляет номер последней страницы из блока пагинации `div.pagination`.
 */
private fun parseLastPage(document: Document): Int {
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

/**
 * Разбирает HTML-элемент карточки видео внутри страницы тега.
 */
private fun parseTagItemVideo(video: Element): ItemsX? {
    return try {
        val titleElement = video.selectFirst("p.title a")
        val title = titleElement?.attr("title") ?: "Без названия"
        val href = titleElement?.attr("href")?.trim().orEmpty()
        if (href.isEmpty() || href == "Нет ссылки") return null
        val duration = video.selectFirst("p.title .duration")?.text() ?: "Нет информации"

        val channelName = video.selectFirst("p.metadata .name")?.text() ?: "Нет имени канала"
        val views = video.selectFirst("p.metadata .bg > span > span")?.ownText()?.trim() ?: "-"
        val profileLink = video.selectFirst("p.metadata a")?.attr("href") ?: ""

        // Реальный id из data-id; иначе — извлекаем id из href, и только потом abs(hash)
        val id = video.attr("data-id").toLongOrNull()
            ?: extractXVideoId(href)
            ?: abs(href.hashCode().toLong()).coerceAtLeast(1L)
        val dataSrc = video.selectFirst("img[data-src]")?.attr("data-src").orEmpty()

        ItemsX(
            id = id,
            title = title,
            href = href,
            duration = duration,
            views = views,
            channel = channelName,
            previewImage = dataSrc,
            previewVideo = parserVideoPreviewFromImageUrl(dataSrc).orEmpty(),
            nameProfile = channelName,
            linkProfile = profileLink
        )
    } catch (e: Exception) {
        Timber.w(e, "parserScreenTags: карточка пропущена из-за ошибки парсинга")
        null
    }
}
