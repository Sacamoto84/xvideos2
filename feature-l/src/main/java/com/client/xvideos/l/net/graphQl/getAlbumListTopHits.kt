package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId

private val TOP_HITS_QUERY = """
    query AlbumListTopHits(${'$'}input: AlbumListInput!, ${'$'}hits_from: AlbumAggregationNames!) {
      album {
        list_top_hits(input: ${'$'}input, hits_from: ${'$'}hits_from) {
          title
          url
          count
          item_type
          items {
            ...AlbumInSearchList
          }
        }
      }
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
""".trimIndent().replace("\n", "\\n")

private fun buildAlbumListTopHitsPayload(
    display: String,
    albumType: AlbumType,
    contentId: ContentId,
    hitsFrom: String
): String {
    return """
        {
          "operationName": "AlbumListTopHits",
          "query": "$TOP_HITS_QUERY",
          "variables": {
            "hits_from": "$hitsFrom",
            "input": {
              "display": "$display",
              "filters": [
                {
                  "name": "album_type",
                  "value": "${albumType.value}"
                },
                {
                  "name": "content_id",
                  "value": "${contentId.value}"
                }
              ],
              "page": 1
            }
          }
        }
    """.trimIndent()
}

private val DEFAULT_ALBUM_LIST_TOP_HITS_QUERY = buildAlbumListTopHitsPayload(
    display = "date_newest",
    albumType = AlbumType.Pictures,
    contentId = ContentId.Hentai,
    hitsFrom = "genre_ids"
)

/**
 * Генерирует тело GraphQL POST-запроса `AlbumListTopHits` для получения подборки самых популярных альбомов.
 *
 * @param display Тип сортировки/отображения (по умолчанию `"date_newest"`).
 * @param albumType Тип альбомов ([AlbumType]).
 * @param contentId Категория контента ([ContentId]).
 * @param hitsFrom Поле агрегации (по умолчанию `"genre_ids"`).
 * @return JSON-строка тела GraphQL-запроса.
 */
fun getAlbumListTopHitsQuery(
    display: String = "date_newest",
    albumType: AlbumType = AlbumType.Pictures,
    contentId: ContentId = ContentId.Hentai,
    hitsFrom: String = "genre_ids"
): String {
    val isDefaultFilter = albumType == AlbumType.Pictures && contentId == ContentId.Hentai
    if (isDefaultFilter && display == "date_newest" && hitsFrom == "genre_ids") {
        return DEFAULT_ALBUM_LIST_TOP_HITS_QUERY
    }
    return buildAlbumListTopHitsPayload(display, albumType, contentId, hitsFrom)
}
