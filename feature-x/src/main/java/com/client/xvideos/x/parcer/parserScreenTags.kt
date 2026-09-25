package com.client.xvideos.x.parcer

import com.client.xvideos.x.extractXVideoId
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ModelScreenTag
import kotlin.math.abs
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber

private val EMPTY_MODEL_SCREEN_TAG = ModelScreenTag(title0 = "?", title1 = "?", items = emptyList(), lastPage = 1)

fun parserScreenTags(html: String): ModelScreenTag {
    if (html.isBlank()) {
        return EMPTY_MODEL_SCREEN_TAG
    }

    val document: Document = Jsoup.parse(html)

    val pageTitle = document.selectFirst("h2.page-title")
    val title0 = pageTitle?.ownText() ?: "?"                       // Текст внутри h2 без дочерних элементов
    val title1 = pageTitle?.selectFirst("span.sub")?.text() ?: "?" // Текст внутри span.sub

    // Число страниц выдачи. Последняя помечена классом:
    //     <li><a href="/tags/public/148" class="last-page">149</a></li>
    // Когда страниц мало, весь список умещается целиком и last-page не ставится —
    // тогда берём наибольшую числовую метку. Нет блока вовсе — одна страница.
    val pagination = document.selectFirst("div.pagination")
    val lastPage = (pagination?.selectFirst("a.last-page")?.text()?.trim()?.toIntOrNull()
        ?: pagination?.select("a")?.fold(1) { acc, el ->
            maxOf(acc, el.text().trim().toIntOrNull() ?: 1)
        }
        ?: 1).coerceAtLeast(1)

    val container = document.selectFirst("#content > div.mozaique.cust-nb-cols") ?: document.selectFirst("div.mozaique")
    val videos = container?.select("div.frame-block.thumb-block") ?: document.select("div.frame-block.thumb-block")
    val listItems = ArrayList<ItemsX>(videos.size)

    videos.forEach { video ->
        try {
            val titleElement = video.selectFirst("p.title a")
            val title = titleElement?.attr("title") ?: "Без названия"
            val href = titleElement?.attr("href")?.trim().orEmpty()
            if (href.isBlank() || href == "Нет ссылки") return@forEach
            val duration = video.selectFirst("p.title .duration")?.text() ?: "Нет информации"

            val channelName = video.selectFirst("p.metadata .name")?.text() ?: "Нет имени канала"
            val views = video.selectFirst("p.metadata .bg > span > span")?.ownText()?.trim() ?: "-"
            val profileLink = video.selectFirst("p.metadata a")?.attr("href") ?: ""

            // Реальный id из data-id; иначе — извлекаем id из href, и только потом abs(hash)
            val id = video.attr("data-id").toLongOrNull()
                ?: extractXVideoId(href)
                ?: abs(href.hashCode().toLong()).coerceAtLeast(1L)
            val dataSrc = video.selectFirst("img[data-src]")?.attr("data-src").orEmpty()

            listItems.add(
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
            )
        } catch (e: Exception) {
            Timber.w(e, "parserScreenTags: карточка пропущена из-за ошибки парсинга")
        }
    }

    return ModelScreenTag(title0 = title0, title1 = title1, items = listItems, lastPage = lastPage)
}
