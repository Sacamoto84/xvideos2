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
private fun jsonEscape(value: String): String {
    var needsEscape = false
    for (i in 0 until value.length) {
        val c = value[i]
        if (c == '\\' || c == '"' || c < ' ') {
            needsEscape = true
            break
        }
    }
    if (!needsEscape) return value

    return buildString(value.length + 8) {
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
}

private val ALBUM_LIST_QUERY = """
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
    .replace("\n", "\\n")
    .replace("\"", "\\\"")

/**
 * Генерирует тело GraphQL-запроса `AlbumList` с полным набором параметров фильтрации [AlbumListFilter].
 *
 * @param page Номер запрашиваемой страницы.
 * @param filter Объект настроек фильтрации (тип альбома, язык, аудитория, жанры, теги, поиск).
 * @return JSON-строка запроса к GraphQL endpoint.
 */
fun getAlbumListGraphQL1(
    page: Int = 3,
    filter: AlbumListFilter,
): String {
    val str = StringBuilder(1024)
    str.append(
        """
        {
          "operationName": "AlbumList",
          "query": "$ALBUM_LIST_QUERY",
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

    if (filter.tagPlus.isNotEmpty() || filter.tagMinus.isNotEmpty()) {
        val tags = StringBuilder()
        filter.tagPlus.forEach {
            tags.append('+').append(it.replace(' ', '_').lowercase(getDefault()))
        }
        filter.tagMinus.forEach {
            tags.append('-').append(it.replace(' ', '_').lowercase(getDefault()))
        }
        str.append("""{ "name": "tagged", "value": "${jsonEscape(tags.toString())}" },""")
    }

    if (filter.selection.isNotEmpty()) {
        str.append("""{ "name": "selection", "value": "${filter.selection}" },""")
    }

    str.append("""{ "name": "audience_ids", "value": "${jsonEscape(filter.audienceIds)}" },""")
    str.append("""{ "name": "language_ids", "value": "${jsonEscape(filter.languageIds)}" }""")

    if (filter.genresPlus.isNotEmpty() || filter.genresMinus.isNotEmpty()) {
        val genres = StringBuilder()
        filter.genresPlus.forEach {
            tags_placeholder -> genres.append('+').append(tags_placeholder.id)
        }
        filter.genresMinus.forEach {
            tags_placeholder -> genres.append('-').append(tags_placeholder.id)
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

/**
 * Базовый генератор GraphQL-запроса `AlbumList` с упрощенными параметрами.
 */
fun getAlbumListGraphQL(
    page: Int = 3,
    itemsPerPage: Int = 30,
    display: String = "rating_14_days",
    audienceIds: String = "+1+10+12+2+3+5+6+8+9",
    languageIds: String = "+1+100+101+2+3+4+5+6+7+8+9+99" //Все языки
): String {
    return """
        {
          "operationName": "AlbumList",
          "query": "$ALBUM_LIST_QUERY",
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
