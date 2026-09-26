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
 * @param document Разобранный HTML-документ страницы.
 * @return emoji-флаг (напр. "🇸🇪") или null, если не удалось определить.
 */
fun parseSiteCountryFlag(document: Document): String? {
    val element = document.getElementById("site-localisation") ?: return null
    val code = FLAG_CODE_REGEX
        .find(element.outerHtml())
        ?.groupValues?.get(1) ?: return null
    return getFlagEmoji("flag-$code")
}

/**
 * Извлекает флаг текущей локализации сайта из строки HTML.
 */
fun parseSiteCountryFlag(html: String): String? =
    if (html.isBlank()) null else parseSiteCountryFlag(Jsoup.parse(html))

/**
 * Парсит список карточек видеороликов из HTML-строки главной страницы или раздела сайта X.
 *
 * @param html Текст HTML-страницы.
 * @return Список моделей [ItemsX].
 */
fun parserListVideo(html: String): List<ItemsX> =
    if (html.isBlank()) emptyList() else parserListVideo(Jsoup.parse(html))

/**
 * Безопасная перегрузка парсинга карточек видео для nullable HTML-строки.
 */
fun parserListVideoOrEmpty(html: String?): List<ItemsX> =
    if (html.isNullOrBlank()) emptyList() else parserListVideo(html)

/**
 * Быстро извлекает только список числовых ID видеороликов из разметки страницы.
 */
fun parseVideoIds(html: String): List<Long> {
    if (html.isBlank()) return emptyList()
    val doc = Jsoup.parse(html)
    return doc.select("div.frame-block[data-id]")
        .mapNotNull { it.attr("data-id").toLongOrNull() }
        .filter { it > 0L }
}

/**
 * Быстро извлекает первый числовой ID видеоролика из разметки страницы или null.
 */
fun parseFirstVideoIdOrNull(html: String): Long? =
    parseVideoIds(html).firstOrNull()

/**
 * Возвращает количество карточек видеороликов в переданной HTML-разметке.
 */
fun parseVideoCount(html: String): Int {
    if (html.isBlank()) return 0
    return Jsoup.parse(html).select("div.frame-block").size
}

/**
 * Проверяет наличие карточек видео в переданной HTML-разметке.
 */
fun hasVideosInList(html: String?): Boolean {
    if (html.isNullOrBlank()) return false
    return html.contains("frame-block") && parseVideoCount(html) > 0
}

/**
 * Парсит список карточек видеороликов из DOM-документа страницы раздела X.
 *
 * Ищет блоки карточек `div.frame-block`, извлекает ID из `data-id`, название,
 * ссылки на видео и превью, длительность и имя канала.
 *
 * @param document Разобранный документ Jsoup.
 * @return Список валидных моделей [ItemsX].
 */
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
            if (href.isEmpty() || href == "No link") continue
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
