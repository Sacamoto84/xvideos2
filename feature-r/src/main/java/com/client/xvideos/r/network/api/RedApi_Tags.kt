package com.client.xvideos.r.network.api

import com.client.xvideos.r.model.tag.TagsResponse
import com.client.xvideos.r.network.http.ApiClient
import com.client.xvideos.r.network.http.Route

/** Маршрут запроса полного справочника тегов RedGifs. */
private val ROUTE_TAGS = Route("GET", "/v1/tags")

/**
 * Подраздел API RedGifs для работы с глобальным каталогом тегов.
 *
 * @property api HTTP-клиент модуля.
 */
class RedApi_Tags(val api: ApiClient) {

    /**
     * Возвращает полный список всех зарегистрированных тегов (около 7 000 элементов с именем и количеством).
     *
     * @return [Result] с коллекцией [TagsResponse].
     */
    suspend fun getTags(): Result<TagsResponse> {
        return api.request(ROUTE_TAGS)
    }

    // Здесь был getTrendingTags() на /v2/search/trending. Вызовов не имел, а
    // сам адрес отвечает 404 — проверено 06.08.2026, см. docs/redgifs-api.md.
    // Если популярные теги понадобятся, начинать надо с поиска нового адреса.
}
