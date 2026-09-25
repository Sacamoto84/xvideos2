package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.getFlagEmoji
import com.client.xvideos.x.model.ItemsX
import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import timber.log.Timber

private val FLAG_CODE_REGEX = Regex("""\bflag-([a-z]{2})\b""")
private val VIEWS_TOKEN_REGEX = Regex("""\d[\d., \s]*[KkMmGgКкМмБб]?""")

/**
 * X4: извлечение флага текущей страны вынесено в отдельную чистую функцию.
 * Раньше [parserListVideo] как побочный эффект писал глобальную `currentCountries`.
 *
 * @return emoji-флаг (напр. "🇸🇪") или null, если не удалось определить.
 */
fun parseSiteCountryFlag(document: Document): String? {
    val element = document.selectFirst("#site-localisation") ?: return null
    val code = FLAG_CODE_REGEX
        .find(element.outerHtml())
        ?.groupValues?.get(1) ?: return null
    return getFlagEmoji("flag-$code")
}

fun parseSiteCountryFlag(html: String): String? =
    if (html.isBlank()) null else parseSiteCountryFlag(Jsoup.parse(html))

fun parserListVideo(html: String): List<ItemsX> =
    if (html.isBlank()) emptyList() else parserListVideo(Jsoup.parse(html))

fun parserListVideo(document: Document): List<ItemsX> {
    // Находим все видео-блоки
    val videoBlocks = document.select("div.frame-block")
    if (videoBlocks.isEmpty()) return emptyList()

    val list = ArrayList<ItemsX>(videoBlocks.size)

    for (block: Element in videoBlocks) {
        try {
            // X1: data-id обязателен и должен быть положительным числом. Если его нет/он битый/неположительный —
            // пропускаем карточку, а не роняем парсинг всей страницы.
            val videoId = block.attr("data-id").toLongOrNull() ?: continue
            if (videoId <= 0L) continue

            val titleAnchor = block.selectFirst("p.title a")
            val videoTitle = titleAnchor?.text() ?: "No title"
            val href = titleAnchor?.attr("href")?.trim().orEmpty()
            if (href.isBlank() || href == "No link") continue
            val videoDuration = block.selectFirst("span.duration")?.text() ?: "No duration"

            // Пусто, а не "null": ItemsX.previewImage — non-null String со значением
            // по умолчанию "", и строка-заглушка отсюда уезжала в модель и на экран.
            val dataSrc: String = block.selectFirst("img[data-src]")?.attr("data-src").orEmpty()
            val videoPreviewUrl = parserVideoPreviewFromImageUrl(dataSrc).orEmpty()

            val metadataEl = block.selectFirst("p.metadata")
            val channelName = metadataEl?.selectFirst(".name")?.text() ?: "No channel"
            val views = extractViews(metadataEl?.text())
            val channelLink = metadataEl?.selectFirst("a")?.attr("href") ?: "No channel link"

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
    val lastToken = VIEWS_TOKEN_REGEX.findAll(metadata).lastOrNull()?.value?.trim()
    return if (!lastToken.isNullOrEmpty()) lastToken else metadata.trim()
}
