package com.client.xvideos.r.network.api

import com.client.xvideos.r.model.tag.TagsResponse
import com.client.xvideos.r.network.http.ApiClient
import com.client.xvideos.r.network.http.Route

class RedApi_Tags(val api: ApiClient) {

    /**
     * #### Возвращает список всех существующих тегов. 7к штук (имя, количество)
     */
    suspend fun getTags(): Result<TagsResponse> {
        return api.request(Route("GET", "/v1/tags"))
    }

    // Здесь был getTrendingTags() на /v2/search/trending. Вызовов не имел, а
    // сам адрес отвечает 404 — проверено 06.08.2026, см. docs/redgifs-api.md.
    // Если популярные теги понадобятся, начинать надо с поиска нового адреса.
}
