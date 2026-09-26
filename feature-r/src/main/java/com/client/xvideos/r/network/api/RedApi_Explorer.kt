package com.client.xvideos.r.network.api

import com.client.xvideos.r.model.NichesResponse
import com.client.xvideos.r.network.http.ApiClient
import com.client.xvideos.r.network.http.Route

/** Шаблон пути для постраничной выгрузки ниш RedGifs с предпросмотрами. */
private const val PATH_EXPLORER_NICHES = "/v2/niches?order=subscribers&previews=yes&sort=desc&page={page}&count={count}"

/**
 * Подраздел API RedGifs для работы со списком ниш (раздел Explorer).
 *
 * @property api HTTP-клиент модуля.
 */
class RedApi_Explorer(val api: ApiClient) {

    /**
     * Загружает страницу каталога ниш.
     *
     * URL запроса:
     * ```
     * https://api.redgifs.com/v2/niches?order=subscribers&previews=yes&sort=desc&page=1&count=100
     * ```
     *
     * Порядок сортировки зашит на уровне запроса (`order=subscribers`, `sort=desc`):
     * сервер RedGifs не поддерживает сортировку по имени на бэкенде,
     * поэтому клиентская сортировка выполняется поверх всего загруженного кэша ниш.
     *
     * @param count Размер страницы (ограничивается диапазоном 1..100).
     * @param page Номер страницы (1-based, не менее 1).
     * @return [Result] с ответом [NichesResponse].
     */
    suspend fun getExplorerNiches(
        count: Int = 100,
        page: Int = 1
    ): Result<NichesResponse> {
        val validPage = page.coerceAtLeast(1)
        val validCount = count.coerceIn(1, 100)
        val route = Route(
            method = "GET",
            path = PATH_EXPLORER_NICHES,
            "page" to validPage,
            "count" to validCount
        )

        return api.request(route)
    }
}
