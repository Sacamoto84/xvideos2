package com.client.xvideos.l.net.graphQl

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private val FAVORITE_REMOVE_QUERY = """
    mutation FavoriteRemove(${'$'}input: FavoriteInput!) {
      favorite {
        remove_favorite(input: ${'$'}input) {
          errors {
            code
            message
          }
        }
      }
    }
""".trimIndent()

/**
 * Генерирует тело GraphQL POST-запроса для FavoriteRemove (удаление лайка/избранного).
 *
 * @param anchorId ID объекта (картинки, альбома).
 * @param anchorType Тип объекта ("picture", "album"). По умолчанию "picture".
 * @param favoriteType Тип действия ("like", "favorite"). По умолчанию "like".
 */
fun getFavoriteRemove(
    anchorId: String,
    anchorType: String = "picture",
    favoriteType: String = "like"
): String {
    return buildJsonObject {
        put("id", "9")
        put("operationName", "FavoriteRemove")
        put("query", FAVORITE_REMOVE_QUERY)
        putJsonObject("variables") {
            putJsonObject("input") {
                put("anchor_id", anchorId.trim())
                put("anchor_type", anchorType.trim())
                put("favorite_type", favoriteType.trim())
            }
        }
    }.toString()
}
