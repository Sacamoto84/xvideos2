package com.client.xvideos.l.net.graphQl

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Генерирует тело GraphQL POST-запроса для FavoritesByDatePicture (лайкнутые картинки).
 *
 * @param userId ID пользователя.
 * @param page Номер страницы (начиная с 1).
 * @param showLikes Показывать ли лайки (по умолчанию true).
 */
fun getFavoritesByDatePicture(
    userId: String? = null,
    page: Int = 1,
    showLikes: Boolean = true
): String {
    val cleanUserId = userId?.trim()
    val hasUserId = !cleanUserId.isNullOrBlank()

    val queryHeader = if (hasUserId) {
        "query FavoritesByDatePicture(\$user_id: ID!, \$page: Int!, \$show_likes: Boolean!)"
    } else {
        "query FavoritesByDatePicture(\$page: Int!, \$show_likes: Boolean!)"
    }

    val listByDateCall = if (hasUserId) {
        "list_by_date(user_id: \$user_id, show_likes: \$show_likes)"
    } else {
        "list_by_date(show_likes: \$show_likes)"
    }

    val query = """
    $queryHeader {
      favorite {
        $listByDateCall {
          ... on MutationError {
            errors {
              code
              message
              name
            }
          }
          ... on FavoritesByDate {
            pictures(page: ${'$'}page) {
              info {
                ...CollectionInfo
              }
              items {
                __typename
                id
                title
                created
                like_status
                number_of_comments
                number_of_favorites
                width
                height
                resolution
                aspect_ratio
                url_to_original
                url_to_video
                is_animated
                position
                url: simple_url
                thumbnails {
                  width
                  height
                  size
                  url
                  target_width
                }
                album {
                  id
                  url
                }
                like_status
              }
            }
          }
        }
      }
    }
    
    fragment CollectionInfo on CollectionInfo {
      page
      has_next_page
      has_previous_page
      total_items
      total_pages
      items_per_page
    }
    """.trimIndent()

    return buildJsonObject {
        put("id", "31")
        put("operationName", "FavoritesByDatePicture")
        put("query", query)
        putJsonObject("variables") {
            if (hasUserId) {
                put("user_id", cleanUserId)
            }
            put("show_likes", showLikes)
            put("page", page.coerceAtLeast(1))
        }
    }.toString()
}
