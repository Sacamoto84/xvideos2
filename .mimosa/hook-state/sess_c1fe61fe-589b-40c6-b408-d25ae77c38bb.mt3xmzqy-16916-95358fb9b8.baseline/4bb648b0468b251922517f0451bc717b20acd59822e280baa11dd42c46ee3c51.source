package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.enum.PictureCountRank
import java.util.Locale.getDefault

/**
 * Экранирует строку для безопасной вставки внутрь JSON-строкового литерала
 * (между кавычками). Защищает тело GraphQL-запроса от поломки/инъекции, если
 * пользовательский ввод (поисковый запрос, теги) содержит `"`, `\` и управляющие
 * символы.
 */
private fun jsonEscape(value: String): String = buildString(value.length) {
    for (c in value) {
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
    }
}


fun getAlbumListGraphQL1(
    page: Int = 3,
    filter: AlbumListFilter,
): String {
    val query = """
        query AlbumList(${'$'}input: AlbumListInput!) {
          album {
            list(input: ${'$'}input) {
              info { ...FacetCollectionInfo }
              items { ...AlbumInSearchList }
            }
          }
        }

        fragment FacetCollectionInfo on FacetCollectionInfo {
          page
          has_next_page
          has_previous_page
          total_items
          total_pages
          items_per_page
          url_complete
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
          cover { width height size url }
          created_by { id url name display_name user_title avatar_url }
          language { id title url }
          tags { category text url count }
          genres { id title slug url }
        }
    """.trimIndent()
        .replace("\n", "\\n")  // превращаем новые строки в \n для JSON
        .replace("\"", "\\\"")  // экранируем кавычки

    val str = StringBuilder()
    str.append(
        """
        {
          "operationName": "AlbumList",
          "query": "$query",
          "variables": {
            "input": {
              "items_per_page": ${filter.itemsPerPage},
              "display": "${jsonEscape(filter.display)}",
              "filters": ["""
    )

    if (filter.album_type != AlbumType.All) {
        str.append("""{ "name": "album_type", "value": "${jsonEscape(filter.album_type.value)}" },""")
    }


    if (filter.picture_count_rank != PictureCountRank.All) {
        str.append("""{ "name": "picture_count_rank", "value": "${filter.picture_count_rank.count}" },""")
    }

    if (filter.content_id != ContentId.All) {
        str.append("""{ "name": "content_id", "value": "${filter.content_id.value}" },""")
    }

    if (filter.searchQuery.isNotBlank()) {
        str.append("""{ "name": "search_query", "value": "${jsonEscape(filter.searchQuery)}" },""")
    }

    if (filter.tagPlus.isNotEmpty() or filter.tagMinus.isNotEmpty()) {
        val tags = StringBuilder()
        filter.tagPlus.forEach {
            tags.append("+${it.replace(" ", "_").lowercase(getDefault())}")
        }
        filter.tagMinus.forEach {
            tags.append("-${it.replace(" ", "_").lowercase(getDefault())}")
        }
        str.append("""{ "name": "tagged", "value": "${jsonEscape(tags.toString())}" },""")
    }

    if (filter.selection.isNotEmpty()) {
        str.append("""{ "name": "selection", "value": "${filter.selection}" },""")
    }

    str.append("""{ "name": "audience_ids", "value": "${jsonEscape(filter.audienceIds)}" },""")

    str.append("""{ "name": "language_ids", "value": "${jsonEscape(filter.languageIds)}" }""")

    if (filter.genresPlus.isNotEmpty() or filter.genresMinus.isNotEmpty()) {
        val genres = StringBuilder()
        filter.genresPlus.forEach {
            genres.append("+${it.id}")
        }
        filter.genresMinus.forEach {
            genres.append("-${it.id}")
        }
        str.append(""",{ "name": "genre_ids", "value": "${jsonEscape(genres.toString())}" }""")
    }

    str.append(
        """
        ],
              "page": $page
            }
          }
        }
        """
    )

    return str.toString().trimIndent()
}


fun getAlbumListGraphQL(
    page: Int = 3,
    itemsPerPage: Int = 30,
    display: String = "rating_14_days",
    audienceIds: String = "+1+10+12+2+3+5+6+8+9",
    languageIds: String = "+1+100+101+2+3+4+5+6+7+8+9+99" //Все языки
): String {
    val query = """
        query AlbumList(${'$'}input: AlbumListInput!) {
          album {
            list(input: ${'$'}input) {
              info { ...FacetCollectionInfo }
              items { ...AlbumInSearchList }
            }
          }
        }

        fragment FacetCollectionInfo on FacetCollectionInfo {
          page
          has_next_page
          has_previous_page
          total_items
          total_pages
          items_per_page
          url_complete
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
          cover { width height size url }
          created_by { id url name display_name user_title avatar_url }
          language { id title url }
          tags { category text url count }
          genres { id title slug url }
        }
    """.trimIndent()
        .replace("\n", "\\n")  // превращаем новые строки в \n для JSON
        .replace("\"", "\\\"")  // экранируем кавычки

    return """
        {
          "operationName": "AlbumList",
          "query": "$query",
          "variables": {
            "input": {
              "items_per_page": $itemsPerPage,
              "display": "$display",
              "filters": [
                { "name": "audience_ids", "value": "$audienceIds" },
                { "name": "language_ids", "value": "$languageIds" }
              ],
              "page": $page
            }
          }
        }
    """.trimIndent()
}

//fun albumSearchQuery(
//    searchQuery: String,
//    page: Int = 1,
//    display: String = "rating_all_time",
//    albumType: String = "All",
//    contentType: String = "0"
//): Map<String, Any> {
//    val query = """
//        query AlbumList(${'$'}input: AlbumListInput!) {
//            album {
//                list(input: ${'$'}input) {
//                    info { ...FacetCollectionInfo }
//                    items { ...AlbumMinimal }
//                }
//            }
//        }
//        fragment FacetCollectionInfo on FacetCollectionInfo {
//            page has_next_page has_previous_page total_items total_pages items_per_page url_complete
//        }
//        fragment AlbumMinimal on Album {
//            __typename id title number_of_pictures number_of_animated_pictures
//        }
//    """.trimIndent()
//
//    return mapOf(
//        "query" to query,
//        "variables" to mapOf(
//            "input" to mapOf(
//                "display" to display,
//                "filters" to listOf(
//                    mapOf("name" to "restrict_genres", "value" to "loose"),
//                    mapOf("name" to "audience_ids", "value" to "+1+2+3+5+6+8+9+10"),
//                    mapOf("name" to "album_type", "value" to albumType),
//                    mapOf("name" to "search_query", "value" to searchQuery),
//                    mapOf("name" to "content_id", "value" to contentType)
//                ),
//                "page" to page
//            )
//        )
//    )
//}
