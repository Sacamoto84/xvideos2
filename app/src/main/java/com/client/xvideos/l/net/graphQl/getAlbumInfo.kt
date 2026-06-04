package com.client.xvideos.l.net.graphQl
fun getAlbumInfo(albumId: Int): String {
    val query = """
        query getAlbumInfo(${'$'}id: ID!) {
            album {
                get(id: ${'$'}id) {
                    ... on Album { ...AlbumStandard }
                    ... on MutationError { errors { code message } }
                }
            }
        }
        fragment AlbumStandard on Album {
           created 
           id 
           title 
           tags 
           is_manga 
           content 
           genres 
           cover 
           description 
           audiences 
           number_of_pictures 
           number_of_animated_pictures
           url
           download_url
           slug
           like_status
           moderation_status
           number_of_favorites
           number_of_dislikes
           number_of_duplicates
           labels
           permissions
           language {
             id
             title
             url
           }
           created_by {
             id
             url
             name
             display_name
           }
        }
    """.trimIndent()

// Формируем JSON-строку
    return """
        {
            "query": "${query.replace("\n", " ").replace("\"", "\\\"")}",
            "variables": {
                "id": "$albumId"
            }
        }
    """.trimIndent()
}

//fun getVideoInfo(videoId: Int): Map<String, Any> {
//    val query = """
//        query getVideoInfo(${'$'}id: ID!) {
//            video {
//                get(id: ${'$'}id) {
//                    ... on Video { ...VideoStandard }
//                    ... on MutationError { errors { code message } }
//                }
//            }
//        }
//        fragment VideoStandard on Video {
//            id title tags content genres description audiences url poster_url subtitle_url
//            v240p v360p v720p v1080p
//        }
//    """.trimIndent()
//
//    return mapOf(
//        "query" to query,
//        "variables" to mapOf("id" to videoId.toString())
//    )
//}
//
//
//
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
//
//fun videoSearchQuery(
//    searchQuery: String,
//    page: Int = 1,
//    display: String = "rating_all_time",
//    contentType: Int = 0
//): Map<String, Any> {
//    val query = """
//        query VideoList(${'$'}input: AlbumListInput!) {
//            video {
//                list(input: ${'$'}input) {
//                    info { ...FacetCollectionInfo }
//                    items { ...VideoMinimal }
//                }
//            }
//        }
//        fragment FacetCollectionInfo on FacetCollectionInfo {
//            page has_next_page has_previous_page total_items total_pages items_per_page url_complete
//        }
//        fragment VideoMinimal on Video {
//            __typename id title
//        }
//    """.trimIndent()
//
//    return mapOf(
//        "query" to query,
//        "variables" to mapOf(
//            "input" to mapOf(
//                "display" to display,
//                "filters" to listOf(
//                    mapOf("name" to "audience_ids", "value" to "+1+2+3+5+6+8+9+10"),
//                    mapOf("name" to "search_query", "value" to searchQuery),
//                    mapOf("name" to "content_id", "value" to contentType)
//                ),
//                "page" to page
//            )
//        )
//    )
//}
//
//fun landingPageQuery(limit: Int = 15): Map<String, Any> {
//    val query = """
//        query getLandingPage(${'$'}LIMIT : Int){
//            landing_page_album {
//                frontpage(limit: ${'$'}LIMIT) {
//                    ...on LandingPage {
//                        sections {
//                            ...on AlbumTopHits { title items }
//                            ...on VideoTopHits { title }
//                        }
//                    }
//                    ...on MutationError { status }
//                }
//            }
//        }
//    """.trimIndent()
//
//    return mapOf(
//        "query" to query,
//        "variables" to mapOf(
//            "LIMIT" to limit
//        )
//    )
//}
