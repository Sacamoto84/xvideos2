package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import java.util.Locale.getDefault

private val ALBUM_LIST_WITH_AGGREGATIONS_QUERY = """
query AlbumListWithAggregations(${'$'}input: AlbumListInput!, ${'$'}aggregations: [AlbumAggregationNames!]!) {
  album {
    list_with_aggregations(input: ${'$'}input, aggregations: ${'$'}aggregations) {
      active_filters {
        field {
          short_name
          url_name
          long_name
          supports_intersection
          supports_summation
        }
        terms_in_search
        values {
          term
          url_to_remove
          description
          neg_in_search
        }
      }
      aggregations {
        field {
          short_name
          url_name
          long_name
          supports_intersection
          supports_summation
        }
        values {
          count
          term
          is_active
        }
      }
    }
  }
}
""".trimIndent()
    .replace("\n", "\\n")
    .replace("\"", "\\\"")

/**
 * Генерирует тело GraphQL POST-запроса `AlbumListWithAggregations` для получения агрегаций и счетчиков доступных фильтров.
 *
 * @param page Номер текущей страницы пагинации.
 * @param filter Объект настроек фильтрации [AlbumListFilter].
 * @return JSON-строка тела запроса.
 */
fun getAlbumListWithAggregations(
    page: Int = 1,
    filter: AlbumListFilter,
): String {
    val str = StringBuilder(1024)

    str.append(
        """
        {
          "operationName": "AlbumListWithAggregations",
          "query": "$ALBUM_LIST_WITH_AGGREGATIONS_QUERY",
          "variables": {
            "input": {
             "display": "${filter.display}",
              "filters": ["""
    )

    if (filter.album_type != AlbumType.All) {
        str.append("""{ "name": "album_type", "value": "${filter.album_type.value}" },""")
    }

    str.append("""{ "name": "audience_ids", "value": "${filter.audienceIds}" },""")

    if (filter.content_id != ContentId.All) {
        str.append("""{ "name": "content_id", "value": "${filter.content_id.value}" },""")
    }

    if (filter.searchQuery.isNotBlank()) {
        str.append("""{ "name": "search_query", "value": "${filter.searchQuery}" },""")
    }

    if (filter.tagPlus.isNotEmpty() || filter.tagMinus.isNotEmpty()) {
        val tags = StringBuilder()
        filter.tagPlus.forEach {
            tags.append('+').append(it.replace(' ', '_').lowercase(getDefault()))
        }
        filter.tagMinus.forEach {
            tags.append('-').append(it.replace(' ', '_').lowercase(getDefault()))
        }
        str.append("""{ "name": "tagged", "value": "$tags" },""")
    }

    if (filter.genresPlus.isNotEmpty() || filter.genresMinus.isNotEmpty()) {
        val genres = StringBuilder()
        filter.genresPlus.forEach {
            genres.append('+').append(it.id)
        }
        filter.genresMinus.forEach {
            genres.append('-').append(it.id)
        }
        str.append("""{ "name": "genre_ids", "value": "$genres" },""")
    }

    str.append("""{ "name": "language_ids", "value": "${filter.languageIds}" }""")

    str.append(
        """    
        ],
              "page": $page
            },
        "aggregations": [
          "restrict_genres",
          "album_type",
          "audience_ids",
          "content_id",
          "genre_ids",
          "language_ids",
          "picture_count_rank",
          "tagged"
    ]
  }
}
        """
    )

    return str.toString()
}
