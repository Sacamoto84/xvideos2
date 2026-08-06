package com.client.xvideos.r.network.api

import com.client.xvideos.r.model.NichesResponse
import com.client.xvideos.r.network.http.ApiClient
import com.client.xvideos.r.network.http.Route

class RedApi_Explorer(val api: ApiClient) {

    /**
     * Страница списка ниш.
     *
     * ```
     * https://api.redgifs.com/v2/niches?order=subscribers&previews=yes&sort=desc&page=1&count=100
     * ```
     *
     * Порядок зашит и параметром не управляется — так и было на деле. Раньше
     * функция принимала `order: Order`, выводила из него `sort` и подставляла
     * оба в запрос, но **ни один вызывающий его не передавал**:
     * `R_Saved_NichesCaches` качает весь список постранично с умолчанием, а
     * сортирует уже загруженное `R_ScreenNichesTab.filterAndSortNiches`, на
     * клиенте.
     *
     * Проверено 06.08.2026 (таблица в `docs/redgifs-api.md`), и параметр стоило
     * убрать не только за неиспользуемость:
     *
     * - `/v2/niches` принимает всего два значения — `posts` и `subscribers`.
     *   `NICHES_NAME_A_Z`/`NICHES_NAME_Z_A` дают `name`, на который сервер
     *   отвечает `400 BadOrder`. То есть половина `Order`, которую параметр
     *   принимал по типу, сломала бы запрос;
     * - `sort` сервер игнорирует: при `order=subscribers` и `order=posts`
     *   выдача с `sort=asc` и `sort=desc` совпадает побайтово. Ветка `when`,
     *   вычислявшая его, не влияла ни на что.
     *
     * Сортировка ниш остаётся клиентской, и иначе быть не может: по имени
     * серверной сортировки у `/v2/niches` нет вовсе.
     */
    suspend fun getExplorerNiches(
        count: Int = 100,
        page: Int = 1
    ): Result<NichesResponse> {
        val route = Route(
            method = "GET",
            path = "/v2/niches?order=subscribers&previews=yes&sort=desc&page={page}&count={count}",
            "page" to page,
            "count" to count
        )

        return api.request(route)
    }
}
