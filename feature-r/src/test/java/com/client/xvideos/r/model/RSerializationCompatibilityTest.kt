package com.client.xvideos.r.model

import com.client.xvideos.r.model.search.SearchNichesShortResponse
import com.client.xvideos.r.model.tag.TagSuggestion
import com.client.xvideos.r.network.json.RJson
import com.google.gson.Gson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Тесты сериализации моделей R:
 * 1. Разбор через kotlinx.serialization (сетевой вход).
 * 2. Полная обратная совместимость с Gson (дисковые хранилища FileDB и CollectionDB).
 */
class RSerializationCompatibilityTest {

    private val gson = Gson()

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

        assertEquals(1, response.niches.size)
        assertEquals("n1", response.niches[0].id)
        assertEquals("p1", response.niches[0].previews?.firstOrNull()?.id)
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

    /* ---------- Обратная совместимость с Gson (дисковые хранилища) ---------- */

    @Test
    fun `Gson по-прежнему корректно сериализует и десериализует GifsInfo`() {
        val original = GifsInfo(
            id = "disk-id-123",
            contentType = "Solo Female",
            likes = 42,
            width = 1920,
            height = 1080,
            tags = listOf("tagA", "tagB"),
            description = "Test Description",
            views = 999L,
            userName = "author1",
            urls = URL1(thumbnail = "https://cdn/t.jpg", sd = "https://cdn/sd.mp4")
        )

        val jsonFromGson = gson.toJson(original)
        val deserialized = gson.fromJson(jsonFromGson, GifsInfo::class.java)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.likes, deserialized.likes)
        assertEquals(original.tags, deserialized.tags)
        assertEquals(original.urls.thumbnail, deserialized.urls.thumbnail)
        assertEquals(original.urls.sd, deserialized.urls.sd)
    }

    @Test
    fun `kotlinx и Gson взаимно читают сериализованные GifsInfo`() {
        val item = GifsInfo(
            id = "cross-test-456",
            description = "Round trip",
            urls = URL1(thumbnail = "https://t.jpg", sd = "https://s.mp4")
        )

        // kotlinx -> Gson
        val jsonFromKtx = RJson.encodeToString(item)
        val fromGson = gson.fromJson(jsonFromKtx, GifsInfo::class.java)
        assertEquals(item.id, fromGson.id)
        assertEquals(item.urls.thumbnail, fromGson.urls.thumbnail)

        // Gson -> kotlinx
        val jsonFromGson = gson.toJson(item)
        val fromKtx = RJson.decodeFromString<GifsInfo>(jsonFromGson)
        assertEquals(item.id, fromKtx.id)
        assertEquals(item.urls.thumbnail, fromKtx.urls.thumbnail)
    }

    @Test
    fun `Gson по-прежнему корректно читает UserInfo и NichesInfo`() {
        val userJson = """{"username":"disk_user","name":"Disk User","followers":500}"""
        val user = gson.fromJson(userJson, UserInfo::class.java)
        assertEquals("disk_user", user.username)
        assertEquals(500L, user.followers)

        val nicheJson = """{"id":"disk_niche","name":"Disk Niche","gifs":100}"""
        val niche = gson.fromJson(nicheJson, NichesInfo::class.java)
        assertEquals("disk_niche", niche.id)
        assertEquals(100L, niche.gifs)
    }
}
