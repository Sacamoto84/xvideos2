package com.client.xvideos.l.net.graphQl

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private fun buildFavoritesByDatePictureSetQuery(hasUserId: Boolean): String {
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

    return """
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
        slug
        url
      }
    }
    """.trimIndent()
}

private val FAVORITES_PICTURE_SET_WITH_USER_QUERY = buildFavoritesByDatePictureSetQuery(hasUserId = true)
private val FAVORITES_PICTURE_SET_NO_USER_QUERY = buildFavoritesByDatePictureSetQuery(hasUserId = false)

/**
 * Генерирует тело GraphQL POST-запроса для FavoritesByDatePictureSet (лайкнутые/избранные наборы картинок и альбомы).
 *
 * @param userId ID пользователя (или null для текущего авторизованного профиля).
 * @param page Номер страницы выдачи (начиная с 1).
 * @param showLikes Показывать ли лайкнутые наборы.
 * @return JSON-строка тела запроса к GraphQL endpoint.
 */
fun getFavoritesByDatePictureSet(
    userId: String? = null,
    page: Int = 1,
    showLikes: Boolean = true
): String {
    val cleanUserId = userId?.trim()
    val hasUserId = !cleanUserId.isNullOrBlank()
    val query = if (hasUserId) FAVORITES_PICTURE_SET_WITH_USER_QUERY else FAVORITES_PICTURE_SET_NO_USER_QUERY

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
