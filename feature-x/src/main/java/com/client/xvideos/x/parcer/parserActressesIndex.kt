package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.ActressesIndexCatalog
import com.client.xvideos.x.model.ActressesIndexDropdownType
import com.client.xvideos.x.model.ActressesIndexFilterGroup
import com.client.xvideos.x.model.ActressesIndexFilterOption
import com.client.xvideos.x.model.ActressesIndexItem
import com.client.xvideos.x.xProfileSlug
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Парсер страницы каталога актрис/моделей и рейтингов (например, `/porn-actresses-index/from/russia/ever`).
 */
fun parseActressesIndexPage(html: String): ActressesIndexCatalog {
    if (html.isBlank()) return ActressesIndexCatalog.EMPTY
    return parseActressesIndexPage(Jsoup.parse(html))
}

/**
 * Парсер DOM-документа страницы каталога актрис/моделей.
 */
fun parseActressesIndexPage(document: Document): ActressesIndexCatalog {
    // 1. Общий заголовок с числом моделей (например, «Топ 76 230 порноактрис»)
    val totalCountTitle = document.selectFirst(".ordered-label-list strong.btn-text")?.text()?.trim()
        ?: document.selectFirst(".ordered-label-list strong")?.text()?.trim()
        ?: document.selectFirst("h1")?.text()?.trim()
        ?: ""

    // 2. Подзаголовок страницы (например, «Рейтинг на этой странице основан на...»)
    val subtitle = document.selectFirst("h5.bg-title")?.text()?.trim().orEmpty()

    // 3. Выпадающие фильтры в верхней панели
    val geoFilter = parseFilterGroup(
        document = document,
        buttonClass = "btn-geo-links",
        listClass = "geo-links",
        type = ActressesIndexDropdownType.GEO,
        defaultTitle = "Мировые модели"
    )

    val profileTypeFilter = parseFilterGroup(
        document = document,
        buttonClass = "btn-profile-links",
        listClass = "profile-links",
        type = ActressesIndexDropdownType.PROFILE_TYPE,
        defaultTitle = "Порноактрисы"
    )

    val timeSortFilter = parseFilterGroup(
        document = document,
        buttonClass = "btn-time-links",
        listClass = "time-links",
        type = ActressesIndexDropdownType.TIME_SORT,
        defaultTitle = "Рейтинг"
    )

    // 4. Список карточек актрис/моделей
    val cards = document.select(".thumb-block.thumb-block-profile")
    val items = cards.mapNotNull { parseActressesCard(it) }

    // 5. Пагинация
    val paginationEl = document.selectFirst(".pagination")
    val hasNextPage = paginationEl?.selectFirst("a.next-page") != null
    val nextPageUrl = paginationEl?.selectFirst("a.next-page")?.attr("href")?.trim().orEmpty()
    val totalPages = paginationEl?.selectFirst("a.last-page")?.text()?.trim()?.toIntOrNull() ?: 1
    val currentPage = paginationEl?.selectFirst("a.active")?.text()?.trim()?.toIntOrNull()?.minus(1)?.coerceAtLeast(0) ?: 0

    return ActressesIndexCatalog(
        totalCountTitle = totalCountTitle,
        subtitle = subtitle,
        geoFilter = geoFilter,
        profileTypeFilter = profileTypeFilter,
        timeSortFilter = timeSortFilter,
        items = items,
        currentPage = currentPage,
        totalPages = totalPages,
        hasNextPage = hasNextPage,
        nextPageUrl = nextPageUrl,
    )
}

private fun parseFilterGroup(
    document: Document,
    buttonClass: String,
    listClass: String,
    type: ActressesIndexDropdownType,
    defaultTitle: String,
): ActressesIndexFilterGroup {
    val buttonText = document.selectFirst(".$buttonClass")?.text()?.trim()
        ?.replace("▼", "")
        ?.trim()
        ?.ifBlank { defaultTitle }
        ?: defaultTitle

    val optionsList = ArrayList<ActressesIndexFilterOption>()
    val liElements = document.select("ul.$listClass li")

    for (li in liElements) {
        val a = li.selectFirst("a") ?: continue
        val href = a.attr("href").trim()
        val text = a.text().trim()
        val isActive = li.hasClass("active")
        if (text.isNotBlank() && href.isNotBlank()) {
            optionsList.add(
                ActressesIndexFilterOption(
                    title = text,
                    urlPath = href,
                    isActive = isActive,
                )
            )
        }
    }

    return ActressesIndexFilterGroup(
        type = type,
        activeTitle = buttonText,
        options = optionsList,
    )
}

// Запасные источники аватара в скрипте карточки; собраны один раз, а не на каждую карточку.
private val AVATAR_THUMB_REGEX = Regex("https?://[^'\"\\s]+_t\\.jpg")
private val AVATAR_PROFILE_THUMB_REGEX = Regex("profile_thumb:\\s*['\"](https?://[^'\"]+)['\"]")
private val AVATAR_ESCAPED_SRC_REGEX = Regex("src=\\\\['\"](https?://[^'\\s]+)\\\\['\"]")

private fun parseCardAvatarUrl(card: Element): String {
    val imgEl = card.selectFirst(".thumb img") ?: card.selectFirst("img")
    var avatarUrl = imgEl?.attr("src")?.trim().orEmpty()
    if (avatarUrl.isBlank() || avatarUrl.contains("blank.gif") || avatarUrl.contains("lightbox-blank")) {
        avatarUrl = imgEl?.attr("data-src")?.trim().orEmpty()
    }
    if (avatarUrl.isBlank() || avatarUrl.contains("blank.gif") || avatarUrl.contains("lightbox-blank")) {
        val scriptHtml = card.select("script").html()
        val match = AVATAR_THUMB_REGEX.find(scriptHtml)
            ?: AVATAR_PROFILE_THUMB_REGEX.find(scriptHtml)
            ?: AVATAR_ESCAPED_SRC_REGEX.find(scriptHtml)
        avatarUrl = match?.groupValues?.getOrNull(1) ?: match?.value.orEmpty()
    }
    if (avatarUrl.startsWith("//")) avatarUrl = "https:$avatarUrl"
    return avatarUrl
}

private fun parseActressesCard(card: Element): ActressesIndexItem? {
    val rawId = card.id().trim()
    val nameLink = card.selectFirst(".profile-name a")
    val href = nameLink?.attr("href")?.trim().orEmpty()

    val slug = rawId.removePrefix("profile_").trim().ifBlank { xProfileSlug(href) }

    val name = nameLink?.text()?.trim().orEmpty()
    val rankText = card.selectFirst(".profile-name strong")?.text()?.trim().orEmpty()
    val videoCount = card.selectFirst(".profile-counts .with-sub")?.text()?.trim().orEmpty()

    val flagEl = card.selectFirst("span.flag")
    val country = flagEl?.attr("title")?.trim().orEmpty()
    val flagClass = flagEl?.classNames()?.firstOrNull { it.startsWith("flag-") && it != "flag-small" }
    val countryCode = flagClass?.removePrefix("flag-")?.trim().orEmpty()

    val avatarUrl = parseCardAvatarUrl(card)

    if (slug.isBlank() && name.isBlank()) return null
    return ActressesIndexItem(
        slug = slug,
        name = name,
        rankText = rankText,
        avatarUrl = avatarUrl,
        country = country,
        countryCode = countryCode,
        videoCount = videoCount,
        profileUrl = href,
    )
}
