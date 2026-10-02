package com.client.xvideos.l.net.graphQl

/*
 * Тексты GraphQL-запросов сайта L, слово в слово как их шлёт его фронтенд
 * (релиз от 22.09.2026, сняты отладочной пробой LWebProbeActivity).
 *
 * Зачем совпадение: анонимные запросы идут GET, и Cloudflare кэширует ответ
 * по полному URL. Когда сервер L отвечает ошибкой, кэш отдаёт только то, что
 * запрашивал сайт, — поэтому и текст запроса, и JSON переменных обязаны
 * совпадать байт в байт. Перед отправкой GET пробельные символы схлопываются
 * в один пробел (anonymousGraphQlGetUrl), поэтому здесь запросы разбиты на
 * строки; ведущий и замыкающий перевод строки дают пробелы в начале и в
 * конце — у сайта они есть. Точность сторожит SiteQueriesTest: если сайт
 * поменяет запросы, их снимают пробой заново.
 */

internal val SITE_ALBUM_LIST_QUERY = """
query AlbumList(${'$'}input: AlbumListInput!) {
  album {
    list(input: ${'$'}input) {
      info { ...FacetCollectionInfo }
      items { ...AlbumInSearchList }
    }
  }
}
fragment FacetCollectionInfo on FacetCollectionInfo {
  page has_next_page has_previous_page total_items total_pages items_per_page url_complete
}
fragment AlbumInSearchList on Album {
  __typename id title description created modified like_status moderation_status
  number_of_favorites number_of_dislikes number_of_pictures number_of_animated_pictures number_of_duplicates
  slug is_manga url download_url labels permissions
  cover { width height size url }
  created_by { id url name display_name user_title avatar_url }
  language { id title url }
  tags { category text url count }
  genres { id title url acts_as_warning }
}
"""

internal val SITE_ALBUM_LIST_WITH_AGGREGATIONS_QUERY = """
query AlbumListWithAggregations(${'$'}input: AlbumListInput!, ${'$'}aggregations: [AlbumAggregationNames!]!) {
  album {
    list_with_aggregations(input: ${'$'}input, aggregations: ${'$'}aggregations) {
      active_filters {
        field { short_name url_name long_name supports_intersection supports_summation }
        terms_in_search
        values { term url_to_remove description neg_in_search }
      }
      aggregations {
        field { short_name url_name long_name supports_intersection supports_summation }
        values { count term is_active url_to_add url_to_remove url_to_must_include url_to_must_not_include }
      }
    }
  }
}
"""

/** Агрегации, которые сайт запрашивает для панели фильтров, в его порядке. */
internal val SITE_ALBUM_AGGREGATIONS = listOf(
    "album_type", "audience_ids", "content_id", "genre_ids",
    "language_ids", "picture_count_rank", "tagged", "selection",
)

internal val SITE_ALBUM_GET_QUERY = """
query AlbumGet(${'$'}id: ID!) {
  album {
    get(id: ${'$'}id) {
      ... on Album { ...AlbumStandard }
      ... on MutationError { errors { code message name } }
    }
  }
}
fragment AlbumStandard on Album {
  __typename id title labels description created modified like_status
  number_of_favorites number_of_dislikes moderation_status marked_for_deletion marked_for_processing
  number_of_pictures number_of_animated_pictures number_of_duplicates
  slug is_manga url download_url permissions
  cover { width height size url }
  created_by { id url name display_name user_title avatar_url }
  content { id title url }
  language { id title url }
  tags { category text url count }
  genres { id title url acts_as_warning }
  audiences { id title url }
  is_featured featured_date
  featured_by { id url name display_name user_title avatar_url }
  original_owner { id url name display_name user_title avatar_url }
}
"""

/** Сайт листает картинки альбома по 50. */
internal const val SITE_PICTURES_PER_PAGE = 50

internal val SITE_PICTURE_LIST_INSIDE_ALBUM_QUERY = """
query PictureListInsideAlbum(${'$'}input: PictureListInput!) {
  picture {
    list(input: ${'$'}input) {
      info { ...FacetCollectionInfo }
      items {
        __typename id title description created like_status number_of_comments number_of_favorites
        moderation_status width height resolution aspect_ratio url_to_original url_to_video is_animated
        position permissions url
        tags { category text url }
        thumbnails { width height size url target_width }
      }
    }
  }
}
fragment FacetCollectionInfo on FacetCollectionInfo {
  page has_next_page has_previous_page total_items total_pages items_per_page url_complete
}
"""
