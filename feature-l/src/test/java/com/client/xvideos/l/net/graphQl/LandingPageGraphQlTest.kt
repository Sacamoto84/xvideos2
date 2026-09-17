package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.net.json.LJson
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тестирование безопасной сериализации GraphQL-запросов лендингов Luscious.
 */
class LandingPageGraphQlTest {

    @Test
    fun `landing page search builds valid json with variables`() {
        val jsonString = getLandingPageAlbumSearch(search = "cosplay", limit = 15)
        val element = LJson.parseToJsonElement(jsonString).jsonObject

        assertEquals("LandingPageAlbumSearch", element["operationName"]?.jsonPrimitive?.content)
        assertTrue(element["query"]?.jsonPrimitive?.content?.contains("query LandingPageAlbumSearch") == true)

        val variables = element["variables"]?.jsonObject
        assertNotNull(variables)
        assertEquals("cosplay", variables?.get("id")?.jsonPrimitive?.content)
        assertEquals(15, variables?.get("limit")?.jsonPrimitive?.int)
    }

    @Test
    fun `landing page search safely escapes quotes and special characters`() {
        val dangerousInput = """test "query" with \slash and newline
"""
        val jsonString = getLandingPageAlbumSearch(search = dangerousInput, limit = 5)
        val element = LJson.parseToJsonElement(jsonString).jsonObject
        val variables = element["variables"]?.jsonObject

        assertEquals(dangerousInput, variables?.get("id")?.jsonPrimitive?.content)
        assertEquals(5, variables?.get("limit")?.jsonPrimitive?.int)
    }

    @Test
    fun `landing page tag builds valid json with variables`() {
        val jsonString = getLandingPageAlbumTag(tag = "12345")
        val element = LJson.parseToJsonElement(jsonString).jsonObject

        assertEquals("LandingPageAlbumTag", element["operationName"]?.jsonPrimitive?.content)
        assertTrue(element["query"]?.jsonPrimitive?.content?.contains("query LandingPageAlbumTag") == true)

        val variables = element["variables"]?.jsonObject
        assertNotNull(variables)
        assertEquals("12345", variables?.get("id")?.jsonPrimitive?.content)
    }

    @Test
    fun `landing page tag safely escapes special characters`() {
        val tagWithQuotes = """tag: "featured" & 'safe'"""
        val jsonString = getLandingPageAlbumTag(tag = tagWithQuotes)
        val element = LJson.parseToJsonElement(jsonString).jsonObject
        val variables = element["variables"]?.jsonObject

        assertEquals(tagWithQuotes, variables?.get("id")?.jsonPrimitive?.content)
    }
}
