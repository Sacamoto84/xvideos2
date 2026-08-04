package com.client.xvideos.l.net.graphQl

import com.google.gson.Gson

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

    val json = mapOf(
        "query" to query,
        "variables" to mapOf(
            "input" to mapOf(
                "display" to "position",
                "filters" to listOf(
                    mapOf("name" to "album_id", "value" to albumId.toString())
                ),
                "page" to page
            )
        )
    )

    return Gson().toJson(json)
}
