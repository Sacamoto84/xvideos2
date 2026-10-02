package com.client.xvideos.l.net.graphQl

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Генерирует GraphQL-запрос сведений об альбоме по его ID — сайтовый
 * `AlbumGet` ([SITE_ALBUM_GET_QUERY]), чтобы при отказе сервера ответ мог
 * прийти из кэша Cloudflare. Путь ответа прежний: `data.album.get`.
 *
 * @param albumId Числовой идентификатор альбома.
 * @return JSON-строка тела запроса либо пустая строка для недопустимого ID.
 */
fun getAlbumInfo(albumId: Int): String {
    if (albumId <= 0) return ""
    return buildJsonObject {
        put("operationName", "AlbumGet")
        put("query", SITE_ALBUM_GET_QUERY)
        putJsonObject("variables") {
            put("id", albumId.toString())
        }
    }.toString()
}
