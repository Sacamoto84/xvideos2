package com.client.xvideos.r.model

import com.client.xvideos.r.model.search.SearchCreatorsResponse
import com.client.xvideos.r.model.search.SearchItemCreatorsResponse
import com.client.xvideos.r.model.search.SearchItemNichesResponse
import com.client.xvideos.r.model.search.SearchItemTagsResponse
import com.client.xvideos.r.model.search.SearchNichesShortResponse
import com.client.xvideos.r.model.tag.TagSuggestion
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.r.network.json.RJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Тесты сериализации моделей R:
 * 1. Разбор через kotlinx.serialization (сетевой вход).
 * 2. Полная обратная совместимость с форматом диска (дисковые хранилища FileDB и CollectionDB).
 */
class RSerializationCompatibilityTest {

    /* ---------- kotlinx.serialization: сетевые DTO ---------- */

    @Test
    fun `MediaResponse разбирается через kotlinx даже с неизвестными полями и пропусками`() {
        val json = """
            {
                "page": 2,
                "pages": 10,
                "total": 100,
                "gifs": [
                    {
                        "id": "cool-gif-1",
                        "contentType": "Solo Female",
                        "tags": ["tag1", "tag2"],
                        "urls": {
                            "thumbnail": "https://cdn/thumb.jpg",
                            "sd": "https://cdn/video.mp4"
                        }
                    }
                ],
                "users": [
                    {
                        "username": "creator1",
                        "name": "Creator One"
                    }
                ],
                "niches": [
                    {
                        "id": "niche1",
                        "name": "Niche One"
                    }
                ],
                "tags": ["tag1"],
                "unexpected_new_api_field": "some_value"
            }
        """.trimIndent()

        val response = RJson.decodeFromString<MediaResponse>(json)

        assertEquals(2, response.page)
        assertEquals(10, response.pages)
        assertEquals(100, response.total)
        assertEquals(1, response.gifs.size)
        assertEquals("cool-gif-1", response.gifs[0].id)
        assertEquals("https://cdn/video.mp4", response.gifs[0].urls.sd)
        assertEquals(1, response.users.size)
        assertEquals("creator1", response.users[0].username)
        assertEquals(1, response.niches.size)
        assertEquals("niche1", response.niches[0].id)
        assertEquals(false, response.isFirstPage)
        assertEquals(true, response.hasGifs)
        assertEquals(true, response.hasUsers)
        assertEquals(true, response.hasNiches)
        assertEquals(true, response.hasTags)
    }

    @Test
    fun `CreatorResponse разбирается через kotlinx`() {
        val json = """
            {
                "page": 1,
                "pages": 5,
                "total": 50,
                "gifs": [{"id": "g1"}],
                "users": [{"username": "u1"}]
            }
        """.trimIndent()

        val response = RJson.decodeFromString<CreatorResponse>(json)

        assertEquals(1, response.page)
        assertEquals(1, response.gifs.size)
        assertEquals("g1", response.gifs[0].id)
    }

    @Test
    fun `NichesResponse разбирается через kotlinx`() {
        val json = """
            {
                "page": 1,
                "pages": 2,
                "total": 20,
                "niches": [
                    {
                        "id": "n1",
                        "name": "Niche 1",
                        "gifs": 100,
                        "subscribers": 50,
                        "thumbnail": "https://cdn/n1.jpg",
                        "previews": [{"id": "p1", "thumbnail": "https://cdn/p1.jpg"}]
                    }
                ]
            }
        """.trimIndent()

        val response = RJson.decodeFromString<NichesResponse>(json)

        val niche = response.niches[0]
        assertEquals(1, response.niches.size)
        assertEquals("n1", niche.id)
        assertEquals("p1", niche.previews?.firstOrNull()?.id)
        assertEquals("Niche 1", niche.displayName)
        assertEquals(true, niche.hasThumbnail)
        assertEquals(true, niche.hasPreviews)
        assertEquals(true, niche.hasGifs)
        assertEquals(true, niche.hasSubscribers)
    }

    @Test
    fun `TopCreatorsResponse разбирается через kotlinx`() {
        val json = """
            {
                "creators": [
                    {
                        "username": "top1",
                        "name": "Top One",
                        "followers": 1000,
                        "gifs": 200
                    }
                ]
            }
        """.trimIndent()

        val response = RJson.decodeFromString<TopCreatorsResponse>(json)

        assertEquals(1, response.creators.size)
        assertEquals("top1", response.creators[0].username)
        assertEquals(1000, response.creators[0].followers)
    }

    @Test
    fun `SearchNichesShortResponse и SearchItemNichesResponse разбираются через kotlinx`() {
        val json = """
            {
                "page": 1,
                "pages": 1,
                "total": 1,
                "niches": [
                    {
                        "id": "search-niche",
                        "name": "Search Niche",
                        "gifs": 10,
                        "subscribers": 20,
                        "tags": ["s1"],
                        "thumbnail": "https://cdn/s.jpg"
                    }
                ]
            }
        """.trimIndent()

        val response = RJson.decodeFromString<SearchNichesShortResponse>(json)

        assertEquals(1, response.niches.size)
        assertEquals("search-niche", response.niches[0].id)
    }

    @Test
    fun `TagSuggestion разбирается через kotlinx`() {
        val json = """{"gifs": 500, "text": "brunette", "type": "tag"}"""

        val suggestion = RJson.decodeFromString<TagSuggestion>(json)

        assertEquals(500L, suggestion.gifs)
        assertEquals("brunette", suggestion.text)
        assertEquals("tag", suggestion.type)
    }

    /* ---------- Обратная совместимость со старыми данными диска (ранее писавшимися Gson) ---------- */

    @Test
    fun `AppJson корректно читает и сериализует GifsInfo формата диска`() {
        val legacyGsonJson = """
            {
                "id": "disk-id-123",
                "contentType": "Solo Female",
                "likes": 42,
                "width": 1920,
                "height": 1080,
                "tags": ["tagA", "tagB"],
                "description": "Test Description",
                "views": 999,
                "userName": "author1",
                "urls": {
                    "thumbnail": "https://cdn/t.jpg",
                    "sd": "https://cdn/sd.mp4"
                }
            }
        """.trimIndent()

        val deserialized = AppJson.decodeFromString<GifsInfo>(legacyGsonJson)

        assertEquals("disk-id-123", deserialized.id)
        assertEquals(42, deserialized.likes)
        assertEquals(listOf("tagA", "tagB"), deserialized.tags)
        assertEquals("https://cdn/t.jpg", deserialized.urls.thumbnail)
        assertEquals("https://cdn/sd.mp4", deserialized.urls.sd)

        val encoded = AppJson.encodeToString(deserialized)
        val roundTrip = AppJson.decodeFromString<GifsInfo>(encoded)
        assertEquals(deserialized.id, roundTrip.id)
        assertEquals(deserialized.urls.thumbnail, roundTrip.urls.thumbnail)
    }

    @Test
    fun `AppJson корректно читает старые UserInfo и NichesInfo с диска`() {
        val userJson = """{"username":"disk_user","name":"Disk User","followers":500}"""
        val user = AppJson.decodeFromString<UserInfo>(userJson)
        assertEquals("disk_user", user.username)
        assertEquals(500L, user.followers)

        val nicheJson = """{"id":"disk_niche","name":"Disk Niche","gifs":100}"""
        val niche = AppJson.decodeFromString<NichesInfo>(nicheJson)
        assertEquals("disk_niche", niche.id)
        assertEquals(100L, niche.gifs)
    }

    @Test
    fun `NichesInfo and NichesResponse helper properties operate correctly`() {
        val emptyNiche = NichesInfo.EMPTY
        assertEquals(true, emptyNiche.isEmpty)
        assertEquals(false, emptyNiche.isNotEmpty)
        assertEquals(false, emptyNiche.isValid)
        assertEquals(null, emptyNiche.cover)
        assertEquals("", emptyNiche.description)

        val validNiche = NichesInfo(id = "niche-1", name = "Niche 1")
        assertEquals(false, validNiche.isEmpty)
        assertEquals(true, validNiche.isNotEmpty)
        assertEquals(true, validNiche.isValid)

        val emptyResponse = NichesResponse.EMPTY
        assertEquals(true, emptyResponse.isEmpty)
        assertEquals(false, emptyResponse.isNotEmpty)
        assertEquals(false, emptyResponse.hasMorePages)

        val pagedResponse = NichesResponse(niches = listOf(Niche(id = "n1")), page = 1, pages = 3)
        assertEquals(false, pagedResponse.isEmpty)
        assertEquals(true, pagedResponse.isNotEmpty)
        assertEquals(true, pagedResponse.hasMorePages)
    }

    @Test
    fun `URL1 and UserInfo helper properties and defaults operate correctly`() {
        val emptyUrl = URL1.EMPTY
        assertEquals(false, emptyUrl.hasHd)
        assertEquals(false, emptyUrl.hasSilent)
        assertEquals(false, emptyUrl.isValid)
        assertEquals("", emptyUrl.bestVideoUrl)

        val fullUrl = URL1(thumbnail = "https://cdn/t.jpg", sd = "https://cdn/sd.mp4", hd = "https://cdn/hd.mp4")
        assertEquals(true, fullUrl.hasHd)
        assertEquals(true, fullUrl.isValid)
        assertEquals("https://cdn/hd.mp4", fullUrl.bestVideoUrl)

        val emptyUser = UserInfo.EMPTY
        assertEquals(false, emptyUser.isValid)
        assertEquals(false, emptyUser.hasAvatar)
        assertEquals("", emptyUser.displayName)

        val user = UserInfo(username = "bob", name = "Bob Dylan", profileImageUrl = "https://cdn/a.png")
        assertEquals(true, user.isValid)
        assertEquals(true, user.hasAvatar)
        assertEquals("Bob Dylan", user.displayName)
    }

    @Test
    fun `Order and MediaType helper methods operate correctly`() {
        assertEquals(Order.TOP, Order.fromValue("top"))
        assertEquals(null, Order.fromValue("unknown_order"))
        assertEquals(true, Order.NICHES_POST_A.isNichesOrder)
        assertEquals(false, Order.TOP.isNichesOrder)

        assertEquals(MediaType.IMAGE, MediaType.fromValue("i"))
        assertEquals(MediaType.ALL, MediaType.fromValue("unknown"))
        assertEquals(true, MediaType.ALL.isAll)

        assertEquals(Order.TOP, Order.RELEVANT.nearestIn(listOf(Order.TOP, Order.LATEST)))
    }

    @Test
    fun `Search models helpers and defaults operate correctly`() {
        val emptyCreators = SearchCreatorsResponse.EMPTY
        assertEquals(true, emptyCreators.isEmpty)
        assertEquals(false, emptyCreators.isNotEmpty)

        val creatorItem = SearchItemCreatorsResponse(text = "@test", name = "Test")
        assertEquals(true, creatorItem.isValid)
        assertEquals("test", creatorItem.username)

        val emptyShortNiches = SearchNichesShortResponse.EMPTY
        assertEquals(true, emptyShortNiches.isEmpty)
        assertEquals(false, emptyShortNiches.isNotEmpty)

        val nicheItem = SearchItemNichesResponse(id = "niche-1", name = "Niche")
        assertEquals(true, nicheItem.isValid)

        val tagItem = SearchItemTagsResponse.EMPTY
        assertEquals(false, tagItem.isValid)
    }

    @Test
    fun `GifsInfo helper properties and defaults operate correctly`() {
        val emptyGif = GifsInfo.EMPTY
        assertEquals(false, emptyGif.isValid)
        assertEquals("Описание", emptyGif.description)
        assertEquals("userName", emptyGif.userName)
        assertEquals(true, emptyGif.isGif)
        assertEquals(false, emptyGif.isImage)

        val imageGif = GifsInfo(id = "img1", type = 2)
        assertEquals(true, imageGif.isValid)
        assertEquals(true, imageGif.isImage)
        assertEquals(false, imageGif.isGif)
    }

    @Test
    fun `TopCreatorsResponse and CreatorResponse helper properties operate correctly`() {
        val emptyTop = TopCreatorsResponse.EMPTY
        assertEquals(true, emptyTop.isEmpty)
        assertEquals(0, emptyTop.size)

        val creator = TopCreator(
            username = "alice",
            name = "Alice Wonder",
            profileImageUrl = "https://cdn/alice.jpg",
            gifs = 10,
            followers = 500
        )
        assertEquals(true, creator.isValid)
        assertEquals(false, creator.isEmpty)
        assertEquals(true, creator.isNotEmpty)
        assertEquals("Alice Wonder", creator.displayName)
        assertEquals(true, creator.hasAvatar)
        assertEquals(true, creator.hasGifs)
        assertEquals(true, creator.hasFollowers)

        val creatorNoName = TopCreator(username = "bob")
        assertEquals("bob", creatorNoName.displayName)
        assertEquals(false, creatorNoName.hasAvatar)
        assertEquals(false, creatorNoName.hasGifs)
        assertEquals(false, creatorNoName.hasFollowers)

        val emptyCreatorResponse = CreatorResponse.EMPTY
        assertEquals(true, emptyCreatorResponse.isEmpty)
        assertEquals(false, emptyCreatorResponse.isNotEmpty)
        assertEquals(true, emptyCreatorResponse.isFirstPage)
        assertEquals(false, emptyCreatorResponse.hasMorePages)
        assertEquals(null, emptyCreatorResponse.primaryUser)
        assertEquals(false, emptyCreatorResponse.hasGifs)
        assertEquals(false, emptyCreatorResponse.hasUsers)
        assertEquals(false, emptyCreatorResponse.hasTags)

        val user = UserInfo(username = "star", name = "Star")
        val populatedResponse = CreatorResponse(
            gifs = listOf(GifsInfo(id = "g1")),
            users = listOf(user),
            tags = listOf("tag1"),
            page = 2,
            pages = 5
        )
        assertEquals(false, populatedResponse.isEmpty)
        assertEquals(true, populatedResponse.isNotEmpty)
        assertEquals(false, populatedResponse.isFirstPage)
        assertEquals(true, populatedResponse.hasMorePages)
        assertEquals(user, populatedResponse.primaryUser)
        assertEquals(true, populatedResponse.hasGifs)
        assertEquals(true, populatedResponse.hasUsers)
        assertEquals(true, populatedResponse.hasTags)
    }

    @Test
    fun `Tag models and search response extensions inspect state accurately`() {
        val tagInfo = com.client.xvideos.r.model.tag.TagInfo(name = "anal", count = 50L)
        assertEquals(true, tagInfo.isValid)
        assertEquals(true, tagInfo.hasCount)
        assertEquals(false, com.client.xvideos.r.model.tag.TagInfo.EMPTY.hasCount)

        val suggestion = com.client.xvideos.r.model.tag.TagSuggestion(text = "blowjob", gifs = 100L, type = "tag")
        assertEquals(true, suggestion.isValid)
        assertEquals(true, suggestion.hasGifs)
        assertEquals(true, suggestion.hasType)

        val tagsResponse = com.client.xvideos.r.model.tag.TagsResponse(listOf(tagInfo))
        assertEquals(1, tagsResponse.count)
        assertEquals(tagInfo, tagsResponse.firstOrNull)

        val creatorItem = com.client.xvideos.r.model.search.SearchItemCreatorsResponse(
            text = "@alice",
            name = "",
            image = "https://cdn/alice.png",
            followers = 500L
        )
        assertEquals("alice", creatorItem.displayName)
        assertEquals(true, creatorItem.hasImage)
        assertEquals(true, creatorItem.hasFollowers)

        val searchCreators = com.client.xvideos.r.model.search.SearchCreatorsResponse(listOf(creatorItem))
        assertEquals(1, searchCreators.size)

        val nicheItem = com.client.xvideos.r.model.search.SearchItemNichesResponse(
            id = "hentai",
            name = "Hentai Art",
            thumbnail = "https://cdn/thumb.jpg",
            gifs = 2000L,
            subscribers = 5000L,
            tags = listOf("tag1")
        )
        assertEquals("Hentai Art", nicheItem.displayName)
        assertEquals(true, nicheItem.hasThumbnail)
        assertEquals(true, nicheItem.hasGifs)
        assertEquals(true, nicheItem.hasSubscribers)
        assertEquals(true, nicheItem.hasTags)

        val searchNiches = com.client.xvideos.r.model.search.SearchNichesShortResponse(
            page = 1,
            pages = 5,
            niches = listOf(nicheItem)
        )
        assertEquals(1, searchNiches.size)
        assertEquals(true, searchNiches.isFirstPage)

        val tagSearchItem = com.client.xvideos.r.model.search.SearchItemTagsResponse(text = "tag1", gifs = 50L)
        assertEquals(true, tagSearchItem.isValid)
        assertEquals(true, tagSearchItem.hasGifs)
    }
}
