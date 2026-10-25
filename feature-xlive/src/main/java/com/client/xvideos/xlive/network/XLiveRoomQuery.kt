package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender

/** Комнат на страницу — столько же берёт сам сайт; сервер принимает до 100. */
const val ROOM_PAGE_SIZE = 90

/**
 * Query-строка списка комнат: страница с [offset], пол [gender], подборка [collection].
 *
 * Значения — фиксированные коды без спецсимволов, поэтому кодировать их не нужно.
 */
fun roomListQuery(
    gender: XLiveGender,
    collection: XLiveCollection,
    offset: Int,
    limit: Int = ROOM_PAGE_SIZE,
): String = buildList {
    add("limit" to "$limit")
    add("offset" to "$offset")
    if (gender.code.isNotEmpty()) add("genders" to gender.code)
    addAll(collection.params)
}.joinToString("&") { (key, value) -> "$key=$value" }
