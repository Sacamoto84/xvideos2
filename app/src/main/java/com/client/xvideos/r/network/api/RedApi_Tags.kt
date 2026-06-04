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

    /**
     * #### Получить список 20 популярных тегов (Trending Tags).
     */
    suspend fun getTrendingTags(): Result<TagsResponse> {
        val route = Route(method = "GET", path = "/v2/search/trending")
        return api.request(route)
    }

}