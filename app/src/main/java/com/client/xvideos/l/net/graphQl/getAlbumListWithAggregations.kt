package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import java.util.Locale.getDefault

fun getAlbumListWithAggregations(
    page: Int = 1,
    filter: AlbumListFilter,
): String {

    val query = """
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
        .replace("\n", "\\n")  // превращаем новые строки в \n для JSON
        .replace("\"", "\\\"")  // экранируем кавычки

    val str = StringBuilder()


    str.append(
        """
        {
          "operationName": "AlbumListWithAggregations",
          "query": "$query",
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

    if (filter.tagPlus.isNotEmpty() or filter.tagMinus.isNotEmpty()) {
        val tags = StringBuilder()
        filter.tagPlus.forEach {
            tags.append("+${it.replace(" ", "_").lowercase(getDefault())}")
        }
        filter.tagMinus.forEach {
            tags.append("-${it.replace(" ", "_").lowercase(getDefault())}")
        }
        str.append("""{ "name": "tagged", "value": "$tags" },""")
    }

    if (filter.genresPlus.isNotEmpty() or filter.genresMinus.isNotEmpty()) {
        val genres = StringBuilder()
        filter.genresPlus.forEach {
            genres.append("+${it.id}")
        }
        filter.genresMinus.forEach {
            genres.append("-${it.id}")
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