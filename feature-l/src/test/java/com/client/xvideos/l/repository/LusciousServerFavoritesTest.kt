package com.client.xvideos.l.repository

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.graphQl.getFavoritesByDatePictureSet
import kotlinx.serialization.json.decodeFromJsonElement
import com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes.ScreenLServerLikesSM
import com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.ScreenLSubscribedAlbumsSM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LusciousServerFavoritesTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getFavoritesByDatePictureSet generates query without userId when userId is null`() {
        val queryJson = getFavoritesByDatePictureSet(userId = null, page = 1)
        assertFalse(queryJson.contains("\$user_id: ID!"))
        assertFalse(queryJson.contains("\"user_id\""))
        assertTrue(queryJson.contains("\"operationName\":\"FavoritesByDatePictureSet\""))
        assertTrue(queryJson.contains("list_by_date(show_likes: \$show_likes)"))
    }

    @Test
    fun `getFavoritesByDatePictureSet generates query with userId when userId is provided`() {
        val queryJson = getFavoritesByDatePictureSet(userId = "12345", page = 2)
        assertTrue(queryJson.contains("\$user_id: ID!"))
        assertTrue(queryJson.contains("\"user_id\":\"12345\""))
        assertTrue(queryJson.contains("list_by_date(user_id: \$user_id, show_likes: \$show_likes)"))
    }

    @Test
    fun `ScreenLSubscribedAlbumsSM loads initial data and handles pagination`() = runTest {
        val fakeRepo = object : LusciousServerFavoritesRepository {
            override suspend fun getSessionUserId(): Result<String> = Result.success("123")

            override suspend fun getSubscribedAlbumsRaw(userId: String?, page: Int): Result<String> {
                return Result.success("""{"data":{"favorite":{"list_by_date":{"picture_sets":{"items":[]}}}}}""")
            }

            override suspend fun getSubscribedAlbums(page: Int): Result<List<AlbumDetails>> {
                return if (page == 1) {
                    Result.success(listOf(AlbumDetails(id = "1", title = "Album 1")))
                } else if (page == 2) {
                    Result.success(listOf(AlbumDetails(id = "2", title = "Album 2")))
                } else {
                    Result.success(emptyList())
                }
            }

            override suspend fun getServerLikedPicturesRaw(userId: String?, page: Int): Result<String> =
                Result.success("{}")

            override suspend fun getServerLikedPictures(page: Int): Result<List<PicsDetails>> =
                Result.success(emptyList())
        }

        val sm = ScreenLSubscribedAlbumsSM(fakeRepo)
        advanceUntilIdle()

        assertEquals(1, sm.albums.value.size)
        assertEquals("Album 1", sm.albums.value.first().title)
        assertTrue(sm.hasMore)
        assertNull(sm.errorMessage.value)

        sm.loadNextPage()
        advanceUntilIdle()

        assertEquals(2, sm.albums.value.size)
        assertEquals("Album 2", sm.albums.value.last().title)

        sm.loadNextPage()
        advanceUntilIdle()

        assertEquals(2, sm.albums.value.size)
        assertFalse(sm.hasMore)

        sm.refresh()
        advanceUntilIdle()

        assertEquals(1, sm.albums.value.size)
        assertTrue(sm.hasMore)
    }

    @Test
    fun `ScreenLServerLikesSM loads initial pictures and syncs with host`() = runTest {
        val fakeRepo = object : LusciousServerFavoritesRepository {
            override suspend fun getSessionUserId(): Result<String> = Result.success("123")

            override suspend fun getSubscribedAlbumsRaw(userId: String?, page: Int): Result<String> =
                Result.success("{}")

            override suspend fun getSubscribedAlbums(page: Int): Result<List<AlbumDetails>> =
                Result.success(emptyList())

            override suspend fun getServerLikedPicturesRaw(userId: String?, page: Int): Result<String> =
                Result.success("{}")

            override suspend fun getServerLikedPictures(page: Int): Result<List<PicsDetails>> {
                return if (page == 1) {
                    Result.success(listOf(PicsDetails(url_to_original = "https://cdn/pic1.jpg")))
                } else if (page == 2) {
                    Result.success(listOf(PicsDetails(url_to_original = "https://cdn/pic2.jpg")))
                } else {
                    Result.success(emptyList())
                }
            }
        }

        val sm = ScreenLServerLikesSM(fakeRepo)
        advanceUntilIdle()

        assertEquals(1, sm.pictures.value.size)
        assertEquals(1, sm.host.filteredPic.size)
        assertEquals("https://cdn/pic1.jpg", sm.pictures.value.first().url_to_original)
        assertTrue(sm.hasMore)
        assertNull(sm.errorMessage.value)

        sm.loadNextPage()
        advanceUntilIdle()

        assertEquals(2, sm.pictures.value.size)
        assertEquals(2, sm.host.filteredPic.size)
        assertEquals("https://cdn/pic2.jpg", sm.pictures.value.last().url_to_original)

        sm.refresh()
        advanceUntilIdle()

        assertEquals(1, sm.pictures.value.size)
        assertEquals(1, sm.host.filteredPic.size)
    }

    @Test
    fun `ScreenLServerLikesSM handles error and recovers on retry`() = runTest {
        var shouldFail = true
        val fakeRepo = object : LusciousServerFavoritesRepository {
            override suspend fun getSessionUserId(): Result<String> = Result.success("123")
            override suspend fun getSubscribedAlbumsRaw(userId: String?, page: Int): Result<String> = Result.success("{}")
            override suspend fun getSubscribedAlbums(page: Int): Result<List<AlbumDetails>> = Result.success(emptyList())
            override suspend fun getServerLikedPicturesRaw(userId: String?, page: Int): Result<String> = Result.success("{}")
            override suspend fun getServerLikedPictures(page: Int): Result<List<PicsDetails>> {
                return if (shouldFail) {
                    Result.failure(IllegalStateException("Network connection timeout"))
                } else {
                    Result.success(listOf(PicsDetails(url_to_original = "https://cdn/pic1.jpg")))
                }
            }
        }

        val sm = ScreenLServerLikesSM(fakeRepo)
        advanceUntilIdle()

        assertTrue(sm.pictures.value.isEmpty())
        assertEquals("Network connection timeout", sm.errorMessage.value)

        shouldFail = false
        sm.loadInitial()
        advanceUntilIdle()

        assertEquals(1, sm.pictures.value.size)
        assertNull(sm.errorMessage.value)
    }

    @Test
    fun `getFavoritesByDatePicture generates query without userId when userId is null`() {
        val queryJson = com.client.xvideos.l.net.graphQl.getFavoritesByDatePicture(userId = null, page = 1)
        assertFalse(queryJson.contains("\$user_id: ID!"))
        assertFalse(queryJson.contains("\"user_id\""))
        assertTrue(queryJson.contains("\"operationName\":\"FavoritesByDatePicture\""))
        assertTrue(queryJson.contains("list_by_date(show_likes: \$show_likes)"))
    }

    @Test
    fun `getFavoritesByDatePicture generates query with userId when userId is provided`() {
        val queryJson = com.client.xvideos.l.net.graphQl.getFavoritesByDatePicture(userId = "2470381", page = 1)
        assertTrue(queryJson.contains("\$user_id: ID!"))
        assertTrue(queryJson.contains("\"user_id\":\"2470381\""))
        assertTrue(queryJson.contains("list_by_date(user_id: \$user_id, show_likes: \$show_likes)"))
    }

    @Test
    fun `FavoritesByDatePicture response with album object correctly decodes PicsDetails`() {
        val sampleResponse = """
        {
          "data": {
            "favorite": {
              "list_by_date": {
                "pictures": {
                  "info": {
                    "page": 1,
                    "has_next_page": false,
                    "has_previous_page": false,
                    "total_items": 1,
                    "total_pages": 1,
                    "items_per_page": 20
                  },
                  "items": [
                    {
                      "__typename": "Picture",
                      "id": "59362744",
                      "title": "Demon Latex Seductress",
                      "created": 1770995944.0,
                      "like_status": "like",
                      "width": 1920,
                      "height": 2803,
                      "resolution": "1920x2803",
                      "aspect_ratio": "1920:2803",
                      "url_to_original": null,
                      "url_to_video": null,
                      "is_animated": false,
                      "position": 42,
                      "url": "/pictures/album/millie_603323/id/59362744/",
                      "thumbnails": [
                        {
                          "width": 1680,
                          "height": 2453,
                          "size": "xMax",
                          "url": "https://cdni.luscious.net/test_xmax.jpg"
                        },
                        {
                          "width": 315,
                          "height": 460,
                          "size": "large_thumbnail",
                          "url": "https://cdni.luscious.net/test_thumb.jpg"
                        }
                      ],
                      "album": {
                        "id": "603323",
                        "url": "/albums/millie_603323/"
                      }
                    }
                  ]
                }
              }
            }
          }
        }
        """.trimIndent()

        val json = com.client.xvideos.l.net.json.LJson.parseToJsonElement(sampleResponse) as kotlinx.serialization.json.JsonObject
        val itemsElement = json["data"]?.let { it as kotlinx.serialization.json.JsonObject }
            ?.get("favorite")?.let { it as kotlinx.serialization.json.JsonObject }
            ?.get("list_by_date")?.let { it as kotlinx.serialization.json.JsonObject }
            ?.get("pictures")?.let { it as kotlinx.serialization.json.JsonObject }
            ?.get("items") as kotlinx.serialization.json.JsonArray

        val sanitized = kotlinx.serialization.json.buildJsonArray {
            itemsElement.forEach { elem ->
                if (elem is kotlinx.serialization.json.JsonObject) {
                    val albumElem = elem["album"]
                    val albumId = if (albumElem is kotlinx.serialization.json.JsonObject) {
                        (albumElem["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                    } else if (albumElem is kotlinx.serialization.json.JsonPrimitive) {
                        albumElem.content
                    } else {
                        null
                    }
                    add(kotlinx.serialization.json.buildJsonObject {
                        elem.forEach { (k, v) ->
                            if (k == "album") {
                                if (albumId != null) {
                                    put("album", kotlinx.serialization.json.JsonPrimitive(albumId))
                                } else {
                                    put("album", kotlinx.serialization.json.JsonPrimitive("null"))
                                }
                            } else {
                                put(k, v)
                            }
                        }
                    })
                } else {
                    add(elem)
                }
            }
        }

        val decoded = com.client.xvideos.l.net.json.LJson.decodeFromJsonElement<List<PicsDetails>>(sanitized)
        val normalized = com.client.xvideos.l.net.normalizePictureUrls(decoded)

        assertEquals(1, normalized.size)
        val item = normalized.first()
        assertEquals("603323", item.album)
        assertEquals(1920, item.width)
        assertEquals(2803, item.height)
        assertEquals("https://cdni.luscious.net/test_xmax.jpg", item.url_to_original)
        assertFalse(item.is_animated)
    }
}
