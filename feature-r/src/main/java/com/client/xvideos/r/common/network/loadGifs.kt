package com.client.xvideos.r.common.network

import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.model.CreatorResponse
import com.client.xvideos.r.model.MediaType
import com.client.xvideos.r.model.Order

/**
 * Вспомогательная функция для загрузки страницы GIF конкретного автора.
 *
 * @param userName Имя автора.
 * @param items Количество элементов на страницу.
 * @param page Номер страницы.
 * @param ord Порядок сортировки.
 * @param type Тип медиаконтента (GIF, Image, All).
 * @param redApi Экземпляр [RedApi] для выполнения запроса.
 * @return [Result] с ответом [CreatorResponse].
 */
suspend fun loadGifs(
    userName: String = "lilijunex",
    items: Int = 100,
    page: Int = 1,
    ord: Order = Order.LATEST,
    type: MediaType = MediaType.GIF,
    redApi: RedApi
): Result<CreatorResponse> {
    val res = redApi.searchCreator(userName = userName, count = items, page = page, type = type, order = ord)
    return res
}
