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
     * https://api.redgifs.com/v2/gifs/search?search_text=anal&page=2&count=40&order=top
     *
     * top, trending, latest
     *
     * Адрес именно `/v2/gifs/search` с параметром `search_text`.
     *
     * Раньше здесь стоял `/v2/search/gifs?query=...`, и он работал — примерно
     * до июля 2026. Потом RedGifs его убрал: тот же запрос стал отвечать
     * 404 `HttpNotFoundException`, и поиск по тексту перестал давать
     * результаты. Форма ниже — та, что осталась живой; по ней же ходят ленты
     * (`getTopLatest` и прочие) и поиск картинок (`searchImage`).
     *
     * Если поиск снова начнёт возвращать 404 — сверяться надо в первую очередь
     * с этими соседями: раз ленты работают, значит адрес живой именно у них.
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
                path = "/v2/gifs/search?search_text={search_text}&order={order}&count={count}&page={page}&type={type}",
                "search_text" to searchText,
                "order" to order.value,
                "count" to count,
                "page" to page,
                "type" to MediaType.GIF.value,
            )
        } else {
            Route(
                method = "GET",
                path = "/v2/gifs/search?search_text={search_text}&order={order}&count={count}&page={page}&type={type}&verified=yes",
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
