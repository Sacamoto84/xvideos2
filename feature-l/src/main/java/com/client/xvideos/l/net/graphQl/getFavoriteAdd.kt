package com.client.xvideos.l.net.graphQl

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Генерирует тело GraphQL POST-запроса для FavoriteAdd (добавление лайка на сервер Luscious).
 *
 * @param anchorId ID объекта (картинки, альбома и т.д.).
 * @param anchorType Тип объекта ("picture", "album"). По умолчанию "picture".
 * @param favoriteType Тип действия ("like", "favorite"). По умолчанию "like".
 */
fun getFavoriteAdd(
    anchorId: String,
    anchorType: String = "picture",
    favoriteType: String = "like"
): String {
    val cleanAnchorId = anchorId.trim()

    val query = """
    mutation FavoriteAdd(${'$'}input: FavoriteInput!) {
      favorite {
        add_favorite(input: ${'$'}input) {
          errors {
            code
            message
          }
        }
      }
    }
    """.trimIndent()

    val id = if (anchorType == "album") "32" else "51"

    return buildJsonObject {
        put("id", id)
        put("operationName", "FavoriteAdd")
        put("query", query)
        putJsonObject("variables") {
            putJsonObject("input") {
                put("anchor_id", cleanAnchorId)
                put("anchor_type", anchorType)
                put("favorite_type", favoriteType)
            }
        }
    }.toString()
}
