package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId

fun getAlbumListTopHitsQuery(
    display: String = "date_newest",
    albumType: AlbumType = AlbumType.Pictures,
    contentId: ContentId = ContentId.Hentai,
    hitsFrom: String = "genre_ids"
): String {
    val query = """
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
    """.trimIndent().replace("\n", "\\n") // только заменяем переносы строк

    return """
        {
          "operationName": "AlbumListTopHits",
          "query": "$query",
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

