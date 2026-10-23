package com.client.xvideos.x.screens.actresses.model

import com.client.xvideos.x.model.ActressesIndexFilterOption
import com.client.xvideos.x.model.CatalogCountry
import com.client.xvideos.x.model.CatalogProfileHit
import com.client.xvideos.x.model.CatalogProfileKind
import com.client.xvideos.x.search.model.SearchResult
import com.client.xvideos.x.xProfileSlug
import java.util.IllformedLocaleException
import java.util.Locale

/** Место страны в шаблоне адреса рейтинга. */
private const val COUNTRY_TOKEN = "%country_token%"

/**
 * Страны каталога, чьё название начинается с [query]: на языке [locale] или
 * по-английски — как в адресе рейтинга (`united_states` → «united states»).
 * Цифры в запросе не учитываются, как у поиска страны на сайте.
 *
 * @return Пункты фильтра с адресом рейтинга страны по [urlTemplate].
 */
fun searchCatalogCountries(
    countries: List<CatalogCountry>,
    urlTemplate: String,
    query: String,
    locale: Locale = Locale.getDefault(),
): List<ActressesIndexFilterOption> {
    val text = query.filterNot { it.isDigit() }.trim()
    if (text.isEmpty() || !urlTemplate.contains(COUNTRY_TOKEN)) return emptyList()
    return countries
        .mapNotNull { country ->
            val name = countryName(country, locale)
            val english = country.token.replace('_', ' ')
            if (name.startsWith(text, ignoreCase = true) || english.startsWith(text, ignoreCase = true)) {
                ActressesIndexFilterOption(title = name, urlPath = urlTemplate.replace(COUNTRY_TOKEN, country.token))
            } else {
                null
            }
        }
        .sortedBy { it.title.lowercase(locale) }
}

/** Название страны на языке [locale]; неизвестный код — английское название из адреса. */
private fun countryName(country: CatalogCountry, locale: Locale): String {
    val display = try {
        Locale.Builder().setRegion(country.code).build().getDisplayCountry(locale)
    } catch (_: IllformedLocaleException) {
        ""
    }
    return if (display.isBlank() || display.equals(country.code, ignoreCase = true)) {
        country.token.replace('_', ' ').replaceFirstChar { it.titlecase(locale) }
    } else {
        display
    }
}

/**
 * Найденные общим поиском сайта профили для поиска по имени в каталоге:
 * на рейтинге моделей — модели, на рейтинге каналов — каналы. Как на сайте,
 * остаются только те, чьё имя содержит [query].
 */
fun catalogProfileHits(result: SearchResult, kind: CatalogProfileKind, query: String): List<CatalogProfileHit> {
    val text = query.trim()
    val hits = when (kind) {
        CatalogProfileKind.MODELS -> result.resolvedPornstars.map {
            CatalogProfileHit(
                name = it.name,
                slug = xProfileSlug(it.profilePath),
                avatarUrl = it.avatarUrl,
                subscribers = if (it.hasSubscribers) it.subscribers else "",
                isModel = true,
            )
        }
        CatalogProfileKind.CHANNELS -> result.resolvedChannels.map {
            CatalogProfileHit(
                name = it.name,
                slug = xProfileSlug(it.profilePath),
                avatarUrl = it.avatarUrl,
                subscribers = if (it.hasSubscribers) it.subscribers else "",
                isModel = false,
            )
        }
    }
    return hits.filter { it.slug.isNotBlank() && it.name.contains(text, ignoreCase = true) }
}
