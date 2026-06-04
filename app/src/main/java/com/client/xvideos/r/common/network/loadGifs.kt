package com.client.xvideos.r.common.network

import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.model.CreatorResponse
import com.client.xvideos.r.model.MediaType
import com.client.xvideos.r.model.Order

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





