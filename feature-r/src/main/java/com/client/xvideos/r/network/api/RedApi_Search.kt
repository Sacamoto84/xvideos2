package com.client.xvideos.r.network.api

import com.client.xvideos.r.model.MediaResponse
import com.client.xvideos.r.model.MediaType
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.search.SearchCreatorsResponse
import com.client.xvideos.r.network.http.ApiClient
import com.client.xvideos.r.network.http.Route

/** Пустой ответ создателей для возврата при пустом поисковом запросе. */
private val EMPTY_CREATORS_RESPONSE = SearchCreatorsResponse()
/** Пустой медиа-ответ для возврата при пустом поисковом запросе. */
private val EMPTY_MEDIA_RESPONSE = MediaResponse()
/** Шаблон пути для полнотекстового поиска гифок. */
private const val SEARCH_GIFS_PATH = "/v2/gifs/search?query={search_text}&order={order}&count={count}&page={page}&type={type}"
/** Шаблон пути для полнотекстового поиска только среди верифицированных авторов. */
private const val SEARCH_GIFS_VERIFIED_PATH = "/v2/gifs/search?query={search_text}&order={order}&count={count}&page={page}&type={type}&verified=yes"

/**
 * Подраздел API RedGifs для поиска авторов и медиаконтента.
 *
 * @property api HTTP-клиент модуля.
 */
class RedApi_Search(val api: ApiClient) {

    /**
     * Быстрый поиск / автодополнение авторов по частичному вводу никнейма.
     * Эндпоинт: `/v2/creators/suggest?query=...` (возвращает до 5 элементов).
     *
     * @param text Строка поиска (если пустая — сразу возвращается пустой результат без сети).
     */
    suspend fun searchCreatorsShort(text: String): Result<SearchCreatorsResponse> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return Result.success(EMPTY_CREATORS_RESPONSE)
        }
        val route =
            Route(method = "GET", path = "/v2/creators/suggest?query={text}", "text" to trimmed)
        return api.request<SearchCreatorsResponse>(route)
    }

    /**
     * Полнотекстовый поиск гифок по ключевой фразе или тегу.
     *
     * Эндпоинт: `/v2/gifs/search?query=...`
     * Поддерживает сортировку (top, trending, latest), пагинацию и фильтр по верифицированным создателям.
     *
     * Внимание: параметр поиска на стороне RedGifs называется именно `query`. Если передать неверное имя параметра,
     * сервер вернет HTTP 200, но отдаст общую дефолтную ленту без фильтрации.
     *
     * @param searchText Поисковая фраза.
     * @param order Порядок сортировки результатов (по умолчанию [Order.TOP]).
     * @param count Размер страницы.
     * @param page Номер страницы (1-based).
     * @param verified Если true — искать только среди проверенных (verified) авторов.
     */
    suspend fun searchGifs(
        searchText: String,             // строка поиска.
        order: Order = Order.TOP,       // порядок сортировки.
        count: Int = 100,               // сколько элементов вернуть.
        page: Int = 1,                  // номер страницы (1-based).
        verified: Boolean = false,
    ): Result <MediaResponse> {
        val trimmed = searchText.trim()
        if (trimmed.isEmpty()) {
            return Result.success(EMPTY_MEDIA_RESPONSE)
        }

        val path = if (!verified) SEARCH_GIFS_PATH else SEARCH_GIFS_VERIFIED_PATH
        val route = Route(
            method = "GET",
            path = path,
            "search_text" to trimmed,
            "order" to order.value,
            "count" to count,
            "page" to page,
            "type" to MediaType.GIF.value,
        )
        //return cacheMediaResponse(route)
        return api.request(route)
    }
}
