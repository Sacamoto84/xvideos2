package com.client.xvideos.l.net.graphQl

private val LIST_ALBUM_PICTURES_QUERY_ESCAPED = """
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
   url
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
""".trimIndent().replace("\n", "\\n")

/**
 * Генерирует тело GraphQL-запроса `ListAlbumPictures` для получения списка картинок альбома с информацией о разрешении и URL.
 *
 * @param albumId Числовой ID альбома.
 * @param page Номер страницы (начиная с 1).
 * @return JSON-строка тела запроса.
 */
fun getPicturesJson(albumId: Int, page: Int = 1): String {
    return """{"query":"$LIST_ALBUM_PICTURES_QUERY_ESCAPED","variables":{"input":{"display":"position","filters":[{"name":"album_id","value":"$albumId"}],"page":$page}}}"""
}
