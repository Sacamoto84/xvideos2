package com.client.xvideos.l.net.graphQl

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Генерирует тело GraphQL POST-запроса для FavoritesByDatePictureSet.
 *
 * @param userId ID пользователя. Если null или пустой, генерируется вариант запроса
 *               без аргумента user_id для проверки, умеет ли бэкенд брать пользователя
 *               из текущей авторизованной сессии/куки.
 * @param page Номер страницы (начиная с 1).
 * @param showLikes Показывать ли лайки (по умолчанию true).
 */
fun getFavoritesByDatePictureSet(
    userId: String? = null,
    page: Int = 1,
    showLikes: Boolean = true
): String {
    val cleanUserId = userId?.trim()
    val hasUserId = !cleanUserId.isNullOrBlank()

    val queryHeader = if (hasUserId) {
        "query FavoritesByDatePictureSet(\$user_id: ID!, \$page: Int!, \$show_likes: Boolean!)"
    } else {
        "query FavoritesByDatePictureSet(\$page: Int!, \$show_likes: Boolean!)"
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
            picture_sets(page: ${'$'}page) {
              info {
                ...CollectionInfo
              }
              items {
                ...AlbumInSearchList
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

    fragment AlbumInSearchList on Album {
      __typename
      id
      title
      description
      created
      modified
      like_status
      moderation_status
      number_of_favorites
      number_of_dislikes
      number_of_pictures
      number_of_animated_pictures
      number_of_duplicates
      slug
      is_manga
      url
      download_url
      labels
      permissions
      cover {
        width
        height
        size
        url
      }
      created_by {
        id
        url
        name
        display_name
        user_title
        avatar_url
      }
      language {
        id
        title
        url
      }
      tags {
        category
        text
        url
        count
      }
      genres {
        id
        title
        url
        acts_as_warning
      }
    }
    """.trimIndent()

    return buildJsonObject {
        put("id", "30")
        put("operationName", "FavoritesByDatePictureSet")
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
