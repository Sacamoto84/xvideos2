package com.client.xvideos.r.network.api

import com.client.xvideos.r.model.MediaResponse
import com.client.xvideos.r.model.MediaType
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.search.SearchCreatorsResponse
import com.client.xvideos.r.network.http.ApiClient
import com.client.xvideos.r.network.http.Route

class RedApi_Search(val api: ApiClient) {

    //https://api.redgifs.com/v2/creators/suggest?query=Ana
    //Возвращает 5 элементов
    suspend fun searchCreatorsShort(text: String): Result<SearchCreatorsResponse> {
        val route =
            Route(method = "GET", path = "/v2/creators/suggest?query={text}", "text" to text)
        return api.request<SearchCreatorsResponse>(route)
    }


    /**
     * ## Поиск GIF-ов по тексту.
     * https://api.redgifs.com/v2/gifs/search?query=anal&page=2&count=40&order=top
     *
     * top, trending, latest
     *
     * Адрес `/v2/gifs/search`, строка поиска — в параметре **`query`**.
     *
     * Раньше здесь стоял `/v2/search/gifs?query=...`, и он работал примерно до
     * июля 2026. Потом RedGifs перенёс поиск на общий адрес лент, а старую
     * ветку убрал — тот же запрос стал отвечать 404.
     *
     * Ловушка: если написать `search_text` вместо `query`, ответ будет 200, но
     * это будет **лента без всякой фильтрации** — те же 99 элементов и 100
     * страниц, что и без поиска. Проверять поиск надо заведомо бессмысленным
     * словом: правильный запрос отдаёт 0 элементов, игнорируемый — полную
     * ленту.
     */
    suspend fun searchGifs(
        searchText: String,             // строка поиска.
        order: Order = Order.TOP,       // порядок сортировки.
        count: Int = 100,               // сколько элементов вернуть.
        page: Int = 1,                  // номер страницы (1-based).
        verified: Boolean = false,
    ): Result <MediaResponse> {

        val route = if (!verified) {
            Route(
                method = "GET",
                path = "/v2/gifs/search?query={search_text}&order={order}&count={count}&page={page}&type={type}",
                "search_text" to searchText,
                "order" to order.value,
                "count" to count,
                "page" to page,
                "type" to MediaType.GIF.value,
            )
        } else {
            Route(
                method = "GET",
                path = "/v2/gifs/search?query={search_text}&order={order}&count={count}&page={page}&type={type}&verified=yes",
                "search_text" to searchText,
                "order" to order.value,
                "count" to count,
                "page" to page,
                "type" to MediaType.GIF.value,
            )
        }
        //return cacheMediaResponse(route)
        return api.request(route)
    }
}
