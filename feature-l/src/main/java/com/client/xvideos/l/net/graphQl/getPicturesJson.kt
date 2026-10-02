package com.client.xvideos.l.net.graphQl

import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Генерирует GraphQL-запрос страницы картинок альбома — сайтовый
 * `PictureListInsideAlbum` ([SITE_PICTURE_LIST_INSIDE_ALBUM_QUERY]) с
 * переменными в порядке сайта и по [SITE_PICTURES_PER_PAGE] на страницу.
 * Путь ответа прежний: `data.picture.list`, нужные поля в нём есть.
 *
 * @param albumId Числовой ID альбома.
 * @param page Номер страницы (начиная с 1).
 * @return JSON-строка тела запроса.
 */
fun getPicturesJson(albumId: Int, page: Int = 1): String = buildJsonObject {
    put("operationName", "PictureListInsideAlbum")
    put("query", SITE_PICTURE_LIST_INSIDE_ALBUM_QUERY)
    putJsonObject("variables") {
        putJsonObject("input") {
            putJsonArray("filters") {
                addJsonObject {
                    put("name", "album_id")
                    put("value", albumId.toString())
                }
            }
            put("display", "position")
            put("items_per_page", SITE_PICTURES_PER_PAGE)
            put("page", page)
        }
    }
}.toString()
