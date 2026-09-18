package com.client.xvideos.l.net.graphQl

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getPicturesJson(albumId: Int, page: Int = 1): String {
    val query = """
        query ListAlbumPictures(${'$'}input: PictureListInput!) {
            picture {
                list(input: ${'$'}input) {
                    info { ...pageInfo }
                    items { ...PicUrls }
                }
            }
        }
        
        fragment pageInfo on FacetCollectionInfo {
           page
           total_items
           total_pages
           items_per_page
           url_complete
        }
        
        fragment PicUrls on Picture {
           id
           height
           width
           is_animated
           url_to_original
           url_to_video
           thumbnails {
               width
               height
               size
               url
           }
        }
        
    """.trimIndent()

    val json = buildJsonObject {
        put("query", query)
        putJsonObject("variables") {
            putJsonObject("input") {
                put("display", "position")
                put("filters", buildJsonArray {
                    add(buildJsonObject {
                        put("name", "album_id")
                        put("value", albumId.toString())
                    })
                })
                put("page", page)
            }
        }
    }

    return json.toString()
}
