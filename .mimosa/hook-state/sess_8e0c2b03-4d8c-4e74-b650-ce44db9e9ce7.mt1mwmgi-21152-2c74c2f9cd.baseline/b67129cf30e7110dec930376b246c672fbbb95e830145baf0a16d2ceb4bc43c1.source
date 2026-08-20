package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ModelScreenTag
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun parserScreenTags(html: String): ModelScreenTag {

    val listItems = mutableListOf<ItemsX>()

    val document: Document = Jsoup.parse(html)

    val pageTitle = document.selectFirst("h2.page-title")
    val title0 = pageTitle?.ownText() ?: "?"                       // Текст внутри h2 без дочерних элементов
    val title1 = pageTitle?.selectFirst("span.sub")?.text() ?: "?" // Текст внутри span.sub

    // Число страниц выдачи. Последняя помечена классом:
    //     <li><a href="/tags/public/148" class="last-page">149</a></li>
    // Когда страниц мало, весь список умещается целиком и last-page не ставится —
    // тогда берём наибольшую числовую метку. Нет блока вовсе — одна страница.
    val pagination = document.selectFirst("div.pagination")
    val lastPage = pagination?.selectFirst("a.last-page")?.text()?.trim()?.toIntOrNull()
        ?: pagination?.select("a")?.mapNotNull { it.text().trim().toIntOrNull() }?.maxOrNull()
        ?: 1

    val container = document.selectFirst("#content > div.mozaique.cust-nb-cols")
    val videos = container?.select("div.frame-block.thumb-block")

    videos?.forEach { video ->
        val titleElement = video.selectFirst("p.title a")
        val title = titleElement?.attr("title") ?: "Без названия"
        val href = titleElement?.attr("href") ?: "Нет ссылки"
        val duration = video.selectFirst("p.title .duration")?.text() ?: "Нет информации"

        val channelName = video.selectFirst("p.metadata .name")?.text() ?: "Нет имени канала"
        val views = video.selectFirst("p.metadata .bg > span > span")?.ownText()?.trim() ?: "-"
        val profileLink = video.selectFirst("p.metadata a")?.attr("href") ?: ""

        // Реальный id из data-id; иначе — стабильный id из href.
        // Раньше всем карточкам присваивался id = 0 и литералы "TODO()", из-за чего
        // ключи списка и идентификация «избранного» схлопывались в один элемент.
        val id = video.attr("data-id").toLongOrNull() ?: href.hashCode().toLong()
        // Пусто, а не "null": ItemsX.previewImage — non-null String со значением
        // по умолчанию "", и строка-заглушка отсюда уезжала в модель и на экран.
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
    }

    return ModelScreenTag(title0 = title0, title1 = title1, items = listItems, lastPage = lastPage)
}
