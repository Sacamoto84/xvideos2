package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveSearchFilters
import io.ktor.http.encodeURLQueryComponent

/** Комнат на страницу — столько же берёт сам сайт; сервер принимает до 100. */
const val ROOM_PAGE_SIZE = 90

/**
 * Query-строка списка комнат: страница с [offset], пол [gender], подборка
 * [collection] и, если заданы, тег [hashtag], слово поиска [keywords] и
 * фильтры [filters].
 *
 * Коды пола, подборки и фильтров — фиксированные, без спецсимволов (запятая
 * в списке цен сервером принимается как есть); тег приходит от сайта, слово
 * поиска — от пользователя, оба кодируются.
 *
 * `search_sort` не передаётся: с ним выдача по слову становится нечёткой —
 * по проверке на живом API под слово подходит меньше комнат, чем без него.
 */
fun roomListQuery(
    gender: XLiveGender,
    collection: XLiveCollection,
    offset: Int,
    limit: Int = ROOM_PAGE_SIZE,
    hashtag: String = "",
    keywords: String = "",
    filters: XLiveSearchFilters = XLiveSearchFilters(),
): String = buildList {
    add("limit" to "$limit")
    add("offset" to "$offset")
    if (gender.code.isNotEmpty()) add("genders" to gender.code)
    addAll(collection.params)
    if (hashtag.isNotBlank()) add("hashtags" to hashtag.trim().encodeURLQueryComponent())
    if (keywords.isNotBlank()) add("keywords" to keywords.trim().encodeURLQueryComponent())
    addAll(filters.params)
}.joinToString("&") { (key, value) -> "$key=$value" }
