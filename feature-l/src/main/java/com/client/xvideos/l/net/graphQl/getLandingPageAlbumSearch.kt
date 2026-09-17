package com.client.xvideos.l.net.graphQl

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getLandingPageAlbumSearch(search: String, limit: Int = 9): String {
    val query = """
    query LandingPageAlbumSearch(${'$'}id: String!, ${'$'}limit: Int) {
  landing_page_album {
    search(search_string: ${'$'}id, limit: ${'$'}limit) {
      ... on LandingPage {
        title
        description
        sections {
          ... on AlbumTopHits {
            title
            description
            count
            item_type
            url
            items {
              ...AlbumInSearchList
            }
          }
        }
      }
      ... on MutationError {
        errors {
          code
          message
        }
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
    url
    acts_as_warning
  }
}
    """.trimIndent()

    return buildJsonObject {
        put("operationName", "LandingPageAlbumSearch")
        put("query", query)
        putJsonObject("variables") {
            put("id", search)
            put("limit", limit)
        }
    }.toString()
}
