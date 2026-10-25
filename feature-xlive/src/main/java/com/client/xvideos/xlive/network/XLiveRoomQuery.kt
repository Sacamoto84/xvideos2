package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender
import io.ktor.http.encodeURLQueryComponent

/** Комнат на страницу — столько же берёт сам сайт; сервер принимает до 100. */
const val ROOM_PAGE_SIZE = 90

/**
 * Query-строка списка комнат: страница с [offset], пол [gender], подборка
 * [collection] и, если задан, тег [hashtag].
 *
 * Коды пола и подборки — фиксированные, без спецсимволов; тег приходит от
 * сайта и кодируется.
 */
fun roomListQuery(
    gender: XLiveGender,
    collection: XLiveCollection,
    offset: Int,
    limit: Int = ROOM_PAGE_SIZE,
    hashtag: String = "",
): String = buildList {
    add("limit" to "$limit")
    add("offset" to "$offset")
    if (gender.code.isNotEmpty()) add("genders" to gender.code)
    addAll(collection.params)
    if (hashtag.isNotBlank()) add("hashtags" to hashtag.trim().encodeURLQueryComponent())
}.joinToString("&") { (key, value) -> "$key=$value" }
