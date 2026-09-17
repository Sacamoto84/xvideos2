package com.client.xvideos.l.net.graphQl

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getLandingPageAlbumTag(tag: String): String {
    val query = """
    query LandingPageAlbumTag(${'$'}id: ID!, ${'$'}limit: Int) {
  landing_page_album {
    tag(id: ${'$'}id, limit: ${'$'}limit) {
      ... on LandingPage {
        title
  sections {
 ... on AlbumTopHits {
 title
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
    slug
    url
  }
}
    """.trimIndent()

    return buildJsonObject {
        put("operationName", "LandingPageAlbumTag")
        put("query", query)
        putJsonObject("variables") {
            put("id", tag)
        }
    }.toString()
}
