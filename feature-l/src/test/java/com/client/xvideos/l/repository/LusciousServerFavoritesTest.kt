package com.client.xvideos.l.repository

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.extractAnchorId
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

            override suspend fun addFavorite(
                anchorId: String,
                anchorType: String,
                favoriteType: String
            ): Result<Unit> = Result.success(Unit)

            override suspend fun removeFavorite(
                anchorId: String,
                anchorType: String,
                favoriteType: String
            ): Result<Unit> = Result.success(Unit)

            override suspend fun resolvePictureId(
                albumId: String,
                mediaUrlOrFileName: String
            ): Result<String> = Result.success("123")
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

            override suspend fun addFavorite(
                anchorId: String,
                anchorType: String,
                favoriteType: String
            ): Result<Unit> = Result.success(Unit)

            override suspend fun removeFavorite(
                anchorId: String,
                anchorType: String,
                favoriteType: String
            ): Result<Unit> = Result.success(Unit)

            override suspend fun resolvePictureId(
                albumId: String,
                mediaUrlOrFileName: String
            ): Result<String> = Result.success("123")
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
            override suspend fun addFavorite(
                anchorId: String,
                anchorType: String,
                favoriteType: String
            ): Result<Unit> = Result.success(Unit)

            override suspend fun removeFavorite(
                anchorId: String,
                anchorType: String,
                favoriteType: String
            ): Result<Unit> = Result.success(Unit)

            override suspend fun resolvePictureId(
                albumId: String,
                mediaUrlOrFileName: String
            ): Result<String> = Result.success("123")
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

    @Test
    fun `getFavoriteAdd generates valid GraphQL payload for picture like`() {
        val payload = com.client.xvideos.l.net.graphQl.getFavoriteAdd(
            anchorId = "62276966",
            anchorType = "picture",
            favoriteType = "like"
        )
        assertTrue(payload.contains("\"id\":\"51\""))
        assertTrue(payload.contains("\"operationName\":\"FavoriteAdd\""))
        assertTrue(payload.contains("mutation FavoriteAdd(\$input: FavoriteInput!)"))
        assertTrue(payload.contains("\"anchor_id\":\"62276966\""))
        assertTrue(payload.contains("\"anchor_type\":\"picture\""))
        assertTrue(payload.contains("\"favorite_type\":\"like\""))
    }

    @Test
    fun `FavoriteAdd success response parses without errors`() {
        val successResponse = """
        {
          "data": {
            "favorite": {
              "add_favorite": {
                "errors": []
              }
            }
          }
        }
        """.trimIndent()

        val json = com.client.xvideos.l.net.json.LJson.parseToJsonElement(successResponse) as kotlinx.serialization.json.JsonObject
        val rootErrors = json["errors"] as? kotlinx.serialization.json.JsonArray
        assertNull(rootErrors)

        val addFavoriteObj = json["data"]?.let { it as kotlinx.serialization.json.JsonObject }
            ?.get("favorite")?.let { it as kotlinx.serialization.json.JsonObject }
            ?.get("add_favorite")?.let { it as kotlinx.serialization.json.JsonObject }

        val mutationErrors = addFavoriteObj?.get("errors") as? kotlinx.serialization.json.JsonArray
        assertTrue(mutationErrors?.isEmpty() == true)
    }

    @Test
    fun `FavoriteAdd mutation error response contains error messages`() {
        val errorResponse = """
        {
          "data": {
            "favorite": {
              "add_favorite": {
                "errors": [
                  {
                    "code": "ALREADY_LIKED",
                    "message": "You have already liked this picture"
                  }
                ]
              }
            }
          }
        }
        """.trimIndent()

        val json = com.client.xvideos.l.net.json.LJson.parseToJsonElement(errorResponse) as kotlinx.serialization.json.JsonObject
        val addFavoriteObj = json["data"]?.let { it as kotlinx.serialization.json.JsonObject }
            ?.get("favorite")?.let { it as kotlinx.serialization.json.JsonObject }
            ?.get("add_favorite")?.let { it as kotlinx.serialization.json.JsonObject }

        val mutationErrors = addFavoriteObj?.get("errors") as? kotlinx.serialization.json.JsonArray
        assertEquals(1, mutationErrors?.size)
        val firstError = mutationErrors?.first() as kotlinx.serialization.json.JsonObject
        assertEquals("You have already liked this picture", (firstError["message"] as kotlinx.serialization.json.JsonPrimitive).content)
    }

    @Test
    fun `extractAnchorId extracts ID from id property, url, and fallback`() {
        val picWithId = PicsDetails(id = "62276966")
        assertEquals("62276966", picWithId.extractAnchorId())

        val picWithUrlField = PicsDetails(url = "/pictures/album/test/id/88888888/@name")
        assertEquals("88888888", picWithUrlField.extractAnchorId())

        val picWithOriginalUrl = PicsDetails(url_to_original = "/pictures/album/test/id/12345678/@name")
        assertEquals("12345678", picWithOriginalUrl.extractAnchorId())

        val picEmpty = PicsDetails()
        assertNull(picEmpty.extractAnchorId())
    }

    @Test
    fun `LSavedLikeMetadata preserves pictureId and pictureUrl through AppJson serialization`() {
        val metadata = com.client.xvideos.l.featured.saved.LSavedLikeMetadata(
            albumId = "603323",
            pictureId = "59362744",
            pictureUrl = "/pictures/album/test/id/59362744/@test",
            picture = PicsDetails(id = "59362744", width = 100, height = 200)
        )

        val jsonString = com.client.xvideos.common.json.AppJson.encodeToString(
            com.client.xvideos.l.featured.saved.LSavedLikeMetadata.serializer(),
            metadata
        )

        assertTrue(jsonString.contains("\"pictureId\": \"59362744\"") || jsonString.contains("\"pictureId\":\"59362744\""))

        val decoded = com.client.xvideos.common.json.AppJson.decodeFromString<com.client.xvideos.l.featured.saved.LSavedLikeMetadata>(jsonString)
        assertEquals("59362744", decoded.pictureId)
        assertEquals("/pictures/album/test/id/59362744/@test", decoded.pictureUrl)
        assertEquals("59362744", decoded.picture.id)
    }

    @Test
    fun `LSavedLikeMetadata backward compatibility with old JSON missing pictureId`() {
        val oldJson = """
        {
          "schemaVersion": 1,
          "savedAt": 1726000000000,
          "site": "luscious",
          "folderName": "603323_hash_slug",
          "mediaFileName": "media.jpg",
          "albumId": "603323",
          "picture": {
            "height": 2803,
            "width": 1920,
            "album": "603323"
          }
        }
        """.trimIndent()

        val decoded = com.client.xvideos.common.json.AppJson.decodeFromString<com.client.xvideos.l.featured.saved.LSavedLikeMetadata>(oldJson)
        assertNull(decoded.pictureId)
        assertNull(decoded.pictureUrl)
        assertNull(decoded.picture.id)
        assertEquals("603323", decoded.albumId)
    }

    @Test
    fun `extractSlugCandidate extracts ULID or slug from media URL or folder name`() {
        val url = "https://cdni.luscious.net/user/603323/millie_beachside_dem_01KHBSB2THB9YFJCQT22P9NGCS.1680x0.jpg?md5=xxx"
        assertEquals("01KHBSB2THB9YFJCQT22P9NGCS", extractSlugCandidate(url))

        val folder = "603323_3a8b4c5d6e7f_millie_beachside_dem_01KHBSB2THB9YFJCQT22P9NGCS"
        assertEquals("01KHBSB2THB9YFJCQT22P9NGCS", extractSlugCandidate(folder))

        val simpleName = "https://cdni.luscious.net/user/603323/simple_image_name.jpg"
        assertEquals("name", extractSlugCandidate(simpleName))
    }

    @Test
    fun `getFavoriteRemove generates valid GraphQL mutation and variables`() {
        val queryJson = com.client.xvideos.l.net.graphQl.getFavoriteRemove(
            anchorId = "53066694",
            anchorType = "picture",
            favoriteType = "like"
        )

        val json = com.client.xvideos.l.net.json.LJson.parseToJsonElement(queryJson) as kotlinx.serialization.json.JsonObject
        assertEquals("9", (json["id"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("FavoriteRemove", (json["operationName"] as kotlinx.serialization.json.JsonPrimitive).content)

        val queryStr = (json["query"] as kotlinx.serialization.json.JsonPrimitive).content
        assertTrue(queryStr.contains("mutation FavoriteRemove"))
        assertTrue(queryStr.contains("remove_favorite(input: ${'$'}input)"))

        val variables = json["variables"] as kotlinx.serialization.json.JsonObject
        val input = variables["input"] as kotlinx.serialization.json.JsonObject
        assertEquals("53066694", (input["anchor_id"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("picture", (input["anchor_type"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("like", (input["favorite_type"] as kotlinx.serialization.json.JsonPrimitive).content)
    }

    @Test
    fun `FavoriteRemove success response parses with empty errors`() {
        val successResponse = """
        {
          "data": {
            "favorite": {
              "remove_favorite": {
                "errors": []
              }
            }
          }
        }
        """.trimIndent()

        val json = com.client.xvideos.l.net.json.LJson.parseToJsonElement(successResponse) as kotlinx.serialization.json.JsonObject
        val removeFavoriteObj = json["data"]?.let { it as kotlinx.serialization.json.JsonObject }
            ?.get("favorite")?.let { it as kotlinx.serialization.json.JsonObject }
            ?.get("remove_favorite")?.let { it as kotlinx.serialization.json.JsonObject }

        val mutationErrors = removeFavoriteObj?.get("errors") as? kotlinx.serialization.json.JsonArray
        assertTrue(mutationErrors?.isEmpty() == true)
    }

    @Test
    fun `FavoriteRemove mutation error response contains error messages`() {
        val errorResponse = """
        {
          "data": {
            "favorite": {
              "remove_favorite": {
                "errors": [
                  {
                    "code": "NOT_FAVORITED",
                    "message": "Item is not in favorites"
                  }
                ]
              }
            }
          }
        }
        """.trimIndent()

        val json = com.client.xvideos.l.net.json.LJson.parseToJsonElement(errorResponse) as kotlinx.serialization.json.JsonObject
        val removeFavoriteObj = json["data"]?.let { it as kotlinx.serialization.json.JsonObject }
            ?.get("favorite")?.let { it as kotlinx.serialization.json.JsonObject }
            ?.get("remove_favorite")?.let { it as kotlinx.serialization.json.JsonObject }

        val mutationErrors = removeFavoriteObj?.get("errors") as? kotlinx.serialization.json.JsonArray
        assertEquals(1, mutationErrors?.size)
        val firstError = mutationErrors?.first() as kotlinx.serialization.json.JsonObject
        assertEquals("Item is not in favorites", (firstError["message"] as kotlinx.serialization.json.JsonPrimitive).content)
    }

    @Test
    fun `getFavoriteAdd generates valid GraphQL payload for album like with id 32 and anchor_type album`() {
        val payload = com.client.xvideos.l.net.graphQl.getFavoriteAdd(
            anchorId = "587656",
            anchorType = "album",
            favoriteType = "like"
        )

        val json = com.client.xvideos.l.net.json.LJson.parseToJsonElement(payload) as kotlinx.serialization.json.JsonObject
        assertEquals("32", (json["id"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("FavoriteAdd", (json["operationName"] as kotlinx.serialization.json.JsonPrimitive).content)

        val variables = json["variables"] as kotlinx.serialization.json.JsonObject
        val input = variables["input"] as kotlinx.serialization.json.JsonObject
        assertEquals("587656", (input["anchor_id"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("album", (input["anchor_type"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("like", (input["favorite_type"] as kotlinx.serialization.json.JsonPrimitive).content)
    }

    @Test
    fun `getFavoriteRemove generates valid GraphQL payload for album unlike`() {
        val payload = com.client.xvideos.l.net.graphQl.getFavoriteRemove(
            anchorId = "587656",
            anchorType = "album",
            favoriteType = "like"
        )

        val json = com.client.xvideos.l.net.json.LJson.parseToJsonElement(payload) as kotlinx.serialization.json.JsonObject
        assertEquals("9", (json["id"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("FavoriteRemove", (json["operationName"] as kotlinx.serialization.json.JsonPrimitive).content)

        val variables = json["variables"] as kotlinx.serialization.json.JsonObject
        val input = variables["input"] as kotlinx.serialization.json.JsonObject
        assertEquals("587656", (input["anchor_id"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("album", (input["anchor_type"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("like", (input["favorite_type"] as kotlinx.serialization.json.JsonPrimitive).content)
    }

    @Test
    fun `ScreenLSubscribedAlbumsSM unlikeAlbum removes album from list and calls repository`() = runTest {
        var unlikedId: String? = null
        val fakeRepo = object : LusciousServerFavoritesRepository {
            override suspend fun getSessionUserId(): Result<String> = Result.success("123")
            override suspend fun getSubscribedAlbumsRaw(userId: String?, page: Int): Result<String> =
                Result.success("{}")
            override suspend fun getSubscribedAlbums(page: Int): Result<List<AlbumDetails>> =
                Result.success(listOf(AlbumDetails(id = "100", title = "To Remove"), AlbumDetails(id = "200", title = "Keep")))
            override suspend fun getServerLikedPicturesRaw(userId: String?, page: Int): Result<String> =
                Result.success("{}")
            override suspend fun getServerLikedPictures(page: Int): Result<List<PicsDetails>> =
                Result.success(emptyList())
            override suspend fun addFavorite(anchorId: String, anchorType: String, favoriteType: String): Result<Unit> =
                Result.success(Unit)
            override suspend fun removeFavorite(anchorId: String, anchorType: String, favoriteType: String): Result<Unit> {
                unlikedId = anchorId
                return Result.success(Unit)
            }
            override suspend fun resolvePictureId(albumId: String, mediaUrlOrFileName: String): Result<String> =
                Result.success("123")
        }

        val sm = ScreenLSubscribedAlbumsSM(fakeRepo)
        advanceUntilIdle()

        assertEquals(2, sm.albums.value.size)
        sm.unlikeAlbum(AlbumDetails(id = "100", title = "To Remove"))
        advanceUntilIdle()

        assertEquals("100", unlikedId)
        assertEquals(1, sm.albums.value.size)
        assertEquals("200", sm.albums.value.first().id)
    }
}
