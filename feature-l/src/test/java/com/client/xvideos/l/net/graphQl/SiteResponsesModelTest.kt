package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.Album
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.FacetCollectionInfo
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.json.LJson
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Модели приложения читают ответы в той форме, в какой их получает сайт:
 * после перехода на сайтовые запросы набор полей в ответе стал сайтовым
 * (лишние поля, у жанров нет `slug`, у картинок есть `target_width`).
 * JSON синтетический, но с полным набором полей сайтовых запросов.
 */
class SiteResponsesModelTest {

    @Test
    fun `элемент списка альбомов в форме сайта`() {
        val album = LJson.decodeFromString<Album>(
            """{"__typename":"Album","id":"11","title":"T","description":"D","created":1.5,"modified":2.5,
               "like_status":"none","moderation_status":"approved","number_of_favorites":3,"number_of_dislikes":1,
               "number_of_pictures":40,"number_of_animated_pictures":2,"number_of_duplicates":0,"slug":"t",
               "is_manga":true,"url":"/albums/t_11/","download_url":"/download/11/","labels":["hot"],
               "permissions":["view"],"cover":{"width":300,"height":400,"size":"m","url":"https://img/c.jpg"},
               "created_by":{"id":"5","url":"/users/5/","name":"n","display_name":"N","user_title":"u","avatar_url":"https://img/a.jpg"},
               "language":{"id":"1","title":"English","url":"/en/"},
               "tags":[{"category":"artist","text":"x","url":"/tags/x/","count":7}],
               "genres":[{"id":"9","title":"G","url":"/genres/g/","acts_as_warning":false}]}"""
        )

        assertEquals("11", album.id)
        assertEquals(40, album.numberOfPictures)
        assertEquals("https://img/c.jpg", album.cover?.url)
    }

    @Test
    fun `сведения о странице списка в форме сайта`() {
        val info = LJson.decodeFromString<FacetCollectionInfo>(
            """{"page":2,"has_next_page":true,"has_previous_page":true,"total_items":1000,"total_pages":34,
               "items_per_page":30,"url_complete":"/manga/?page=2"}"""
        )

        assertEquals(34, info.totalPages)
    }

    @Test
    fun `альбом из AlbumGet в форме сайта`() {
        val details = LJson.decodeFromString<AlbumDetails>(
            """{"__typename":"Album","id":"614544","title":"T","labels":[],"description":"D","created":1.5,"modified":2.5,
               "like_status":"none","number_of_favorites":3,"number_of_dislikes":1,"moderation_status":"approved",
               "marked_for_deletion":false,"marked_for_processing":false,"number_of_pictures":12,
               "number_of_animated_pictures":0,"number_of_duplicates":0,"slug":"t","is_manga":true,"url":"/albums/t_614544/",
               "download_url":"/download/614544/","permissions":[],"cover":{"width":300,"height":400,"size":"m","url":"https://img/c.jpg"},
               "created_by":{"id":"5","url":"/users/5/","name":"n","display_name":"N","user_title":"u","avatar_url":"https://img/a.jpg"},
               "content":{"id":"2","title":"Hentai","url":"/hentai/"},"language":{"id":"1","title":"English","url":"/en/"},
               "tags":[{"category":"artist","text":"x","url":"/tags/x/","count":7}],
               "genres":[{"id":"9","title":"G","url":"/genres/g/","acts_as_warning":false}],
               "audiences":[{"id":"1","title":"A","url":"/a/"}],"is_featured":false,"featured_date":null,"featured_by":null,
               "original_owner":{"id":"5","url":"/users/5/","name":"n","display_name":"N","user_title":"u","avatar_url":"x"}}"""
        )

        assertEquals("614544", details.id)
        assertEquals(12, details.number_of_pictures)
        assertEquals(1, details.tags.size)
        assertEquals(1, details.genres.size)
        assertEquals(1, details.audiences.size)
        assertEquals("Hentai", details.content.title)
    }

    @Test
    fun `картинка из PictureListInsideAlbum в форме сайта`() {
        val picture = LJson.decodeFromString<PicsDetails>(
            """{"__typename":"Picture","id":"77","title":"p","description":"","created":1.5,"like_status":"none",
               "number_of_comments":0,"number_of_favorites":1,"moderation_status":"approved","width":800,"height":1200,
               "resolution":"800x1200","aspect_ratio":"2:3","url_to_original":"https://img/o.jpg","url_to_video":null,
               "is_animated":false,"position":1,"permissions":[],"url":"https://img/p.jpg",
               "tags":[{"category":"artist","text":"x","url":"/tags/x/"}],
               "thumbnails":[{"width":300,"height":450,"size":"m","url":"https://img/t.jpg","target_width":300}]}"""
        )

        assertEquals("77", picture.id)
        assertEquals("https://img/o.jpg", picture.url_to_original)
        assertEquals(1, picture.thumbnails?.size)
    }
}
