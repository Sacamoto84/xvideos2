package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.getFlagEmoji
import com.client.xvideos.x.model.ItemsX
import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import timber.log.Timber

/**
 * X4: извлечение флага текущей страны вынесено в отдельную чистую функцию.
 * Раньше [parserListVideo] как побочный эффект писал глобальную `currentCountries`.
 *
 * @return emoji-флаг (напр. "🇸🇪") или null, если не удалось определить.
 */
fun parseSiteCountryFlag(html: String): String? {
    val document = Jsoup.parse(html)
    val code = Regex("""\bflag-([a-z]{2})\b""")
        .find(document.select("#site-localisation").toString())
        ?.groupValues?.get(1) ?: return null
    return getFlagEmoji("flag-$code")
}

fun parserListVideo(html: String): List<ItemsX> {

    val list = mutableListOf<ItemsX>()

    // Парсим HTML-документ
    val document: Document = Jsoup.parse(html)

    // Находим все видео-блоки
    val videoBlocks = document.select("div.frame-block")

    for (block: Element in videoBlocks) {
        try {
            // X1: data-id обязателен и должен быть числом. Если его нет/он битый —
            // пропускаем карточку, а не роняем парсинг всей страницы.
            val videoId = block.attr("data-id").toLongOrNull() ?: continue

            val videoTitle = block.selectFirst("p.title a")?.text() ?: "No title"
            val href = block.selectFirst("p.title a")?.attr("href") ?: "No link"
            val videoDuration = block.selectFirst("span.duration")?.text() ?: "No duration"

            // Пусто, а не "null": ItemsX.previewImage — non-null String со значением
            // по умолчанию "", и строка-заглушка отсюда уезжала в модель и на экран.
            val dataSrc: String = block.selectFirst("img[data-src]")?.attr("data-src").orEmpty()
            val videoPreviewUrl = parserVideoPreviewFromImageUrl(dataSrc).orEmpty()

            val channelName = block.selectFirst("p.metadata .name")?.text() ?: "No channel"
            val views = extractViews(block.selectFirst("p.metadata")?.text())
            val channelLink = block.selectFirst("p.metadata a")?.attr("href") ?: "No channel link"

            list.add(
                ItemsX(
                    id = videoId,
                    title = videoTitle,
                    href = href,
                    duration = videoDuration,
                    views = views,
                    channel = channelName,
                    previewImage = dataSrc,
                    previewVideo = videoPreviewUrl,
                    nameProfile = channelName, // X2: реальное имя профиля вместо литерала "TODO()"
                    linkProfile = channelLink
                )
            )
        } catch (e: Exception) {
            // X1: одна некорректная карточка не должна ломать всю ленту.
            Timber.w(e, "parserListVideo: блок пропущен из-за ошибки парсинга")
        }
    }

    return list
}

/**
 * X3: извлекает счётчик просмотров без привязки к языку слова «просмотры».
 * Берём последний числовой токен (1.2M / 530k / 12 345) — это и есть счётчик,
 * даже если ответ пришёл не на русском.
 */
private fun extractViews(metadata: String?): String {
    if (metadata.isNullOrBlank()) return "No views"
    val token = Regex("""\d[\d., \s]*[KkMmGgКкМмБб]?""")
        .findAll(metadata)
        .map { it.value.trim() }
        .lastOrNull { it.isNotBlank() }
    return token ?: metadata.trim()
}
