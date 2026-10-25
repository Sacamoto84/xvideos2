package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveTagSort

/** Тегов на страницу — столько же берёт сам сайт; больше сервер не отдаёт. */
const val TAG_PAGE_SIZE = 100

/**
 * Query-строка таблицы тегов: страница [page] с единицы, пол [gender], порядок [sort].
 *
 * Все значения — фиксированные коды без спецсимволов.
 */
fun tagTableQuery(
    gender: XLiveGender,
    sort: XLiveTagSort,
    page: Int,
    limit: Int = TAG_PAGE_SIZE,
): String = buildList {
    add("sort" to sort.code)
    add("page" to "$page")
    if (gender.code.isNotEmpty()) add("g" to gender.code)
    add("limit" to "$limit")
}.joinToString("&") { (key, value) -> "$key=$value" }
