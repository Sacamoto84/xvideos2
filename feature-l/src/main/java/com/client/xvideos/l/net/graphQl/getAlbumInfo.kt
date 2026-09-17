package com.client.xvideos.l.net.graphQl

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

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
           modified
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

    val json = buildJsonObject {
        put("query", query)
        putJsonObject("variables") {
            put("id", albumId.toString())
        }
    }
    return json.toString()
}
