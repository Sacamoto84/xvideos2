package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.enum.PictureCountRank
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.Locale.getDefault

/** Фильтр без изменений: всё отмечено. Такие аудитории и языки сайт не шлёт вовсе. */
private val DEFAULT_FILTER = AlbumListFilter()

/**
 * Фильтры списка альбомов в порядке сайта: тип альбома, затем тип контента
 * (так шлёт сайт в своих разделах), остальное — после.
 *
 * Аудитории и языки уходят только если пользователь их сузил. Сайт для
 * анонима их не шлёт, и с ними URL не совпал бы с сайтовым, а значит, мимо
 * кэша Cloudflare. По умолчанию отмечено всё, так что смысл запроса тот же.
 */
internal fun albumListFilters(filter: AlbumListFilter): JsonArray = buildJsonArray {
    fun filter(name: String, value: String) = addJsonObject {
        put("name", name)
        put("value", value)
    }
    if (filter.album_type != AlbumType.All) filter("album_type", filter.album_type.value)
    if (filter.content_id != ContentId.All) filter("content_id", filter.content_id.value.toString())
    if (filter.picture_count_rank != PictureCountRank.All) {
        filter("picture_count_rank", filter.picture_count_rank.count.toString())
    }
    if (filter.searchQuery.isNotBlank()) filter("search_query", filter.searchQuery)
    if (filter.tagPlus.isNotEmpty() || filter.tagMinus.isNotEmpty()) {
        val tags = StringBuilder()
        filter.tagPlus.forEach { tags.append('+').append(it.replace(' ', '_').lowercase(getDefault())) }
        filter.tagMinus.forEach { tags.append('-').append(it.replace(' ', '_').lowercase(getDefault())) }
        filter("tagged", tags.toString())
    }
    if (filter.selection.isNotEmpty()) filter("selection", filter.selection)
    if (filter.genresPlus.isNotEmpty() || filter.genresMinus.isNotEmpty()) {
        val genres = StringBuilder()
        filter.genresPlus.forEach { genres.append('+').append(it.id) }
        filter.genresMinus.forEach { genres.append('-').append(it.id) }
        filter("genre_ids", genres.toString())
    }
    if (filter.audienceIds != DEFAULT_FILTER.audienceIds) filter("audience_ids", filter.audienceIds)
    if (filter.languageIds != DEFAULT_FILTER.languageIds) filter("language_ids", filter.languageIds)
}

/**
 * Генерирует тело GraphQL-запроса `AlbumList` с полным набором параметров фильтрации [AlbumListFilter].
 *
 * Текст запроса и порядок ключей — как у сайта ([SITE_ALBUM_LIST_QUERY]):
 * `items_per_page, display, filters, page`.
 *
 * @param page Номер запрашиваемой страницы.
 * @param filter Объект настроек фильтрации (тип альбома, язык, аудитория, жанры, теги, поиск).
 * @return JSON-строка запроса к GraphQL endpoint.
 */
fun getAlbumListGraphQL1(
    page: Int = 3,
    filter: AlbumListFilter,
): String = buildJsonObject {
    put("operationName", "AlbumList")
    put("query", SITE_ALBUM_LIST_QUERY)
    putJsonObject("variables") {
        putJsonObject("input") {
            put("items_per_page", filter.itemsPerPage)
            put("display", filter.display)
            put("filters", albumListFilters(filter))
            put("page", page)
        }
    }
}.toString()
