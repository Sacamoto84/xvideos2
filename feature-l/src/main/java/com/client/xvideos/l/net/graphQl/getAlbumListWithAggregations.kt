package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.AlbumListFilter
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Генерирует тело GraphQL POST-запроса `AlbumListWithAggregations` для получения агрегаций и счетчиков доступных фильтров.
 *
 * Как у сайта ([SITE_ALBUM_LIST_WITH_AGGREGATIONS_QUERY]): `input` без
 * `items_per_page` (`display, filters, page`) и агрегации [SITE_ALBUM_AGGREGATIONS].
 * Раньше фильтры подставлялись в JSON без экранирования, и кавычка в поиске
 * ломала запрос.
 *
 * @param page Номер текущей страницы пагинации.
 * @param filter Объект настроек фильтрации [AlbumListFilter].
 * @return JSON-строка тела запроса.
 */
fun getAlbumListWithAggregations(
    page: Int = 1,
    filter: AlbumListFilter,
): String = buildJsonObject {
    put("operationName", "AlbumListWithAggregations")
    put("query", SITE_ALBUM_LIST_WITH_AGGREGATIONS_QUERY)
    putJsonObject("variables") {
        putJsonObject("input") {
            put("display", filter.display)
            put("filters", albumListFilters(filter))
            put("page", page)
        }
        putJsonArray("aggregations") { SITE_ALBUM_AGGREGATIONS.forEach { add(it) } }
    }
}.toString()
