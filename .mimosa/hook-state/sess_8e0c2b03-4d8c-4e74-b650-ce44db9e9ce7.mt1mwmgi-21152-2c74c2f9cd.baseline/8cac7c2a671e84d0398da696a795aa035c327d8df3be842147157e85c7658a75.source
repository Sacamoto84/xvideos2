package com.client.xvideos.l.net.graphQl

fun getLandingPageAlbumSearch( search : String, limit : Int = 9): String {

    val str = """
    {
    "operationName": "LandingPageAlbumSearch",
    "query": "\n    query LandingPageAlbumSearch(${'$'}id: String!, ${'$'}limit: Int) {\n  landing_page_album {\n    search(search_string: ${'$'}id, limit: ${'$'}limit) {\n      ... on LandingPage {\n        title\n        description\n        sections {\n          ... on AlbumTopHits {\n            title\n            description\n            count\n            item_type\n            url\n            items {\n              ...AlbumInSearchList\n            }\n          }\n        }\n      }\n      ... on MutationError {\n        errors {\n          code\n          message\n        }\n      }\n    }\n  }\n}\n    \n    fragment AlbumInSearchList on Album {\n  __typename\n  id\n  title\n  description\n  created\n  modified\n  like_status\n  moderation_status\n  number_of_favorites\n  number_of_dislikes\n  number_of_pictures\n  number_of_animated_pictures\n  number_of_duplicates\n  slug\n  is_manga\n  url\n  download_url\n  labels\n  permissions\n  cover {\n    width\n    height\n    size\n    url\n  }\n  created_by {\n    id\n    url\n    name\n    display_name\n    user_title\n    avatar_url\n  }\n  language {\n    id\n    title\n    url\n  }\n  tags {\n    category\n    text\n    url\n    count\n  }\n  genres {\n    id\n    title\n    url\n    acts_as_warning\n  }\n}\n    ",
    "variables": { "id": "$search", "limit": $limit }
    }
    """.trimIndent()

    return str
}
