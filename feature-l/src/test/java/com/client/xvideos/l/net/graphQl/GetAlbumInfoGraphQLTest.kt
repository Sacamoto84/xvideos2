package com.client.xvideos.l.net.graphQl

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAlbumInfoGraphQLTest {

    @Test
    fun `getAlbumInfo generates valid JSON with expected query and variables`() {
        val albumId = 42819
        val jsonString = getAlbumInfo(albumId)

        val jsonElement = Json.parseToJsonElement(jsonString)
        val jsonObject = jsonElement.jsonObject

        val query = jsonObject["query"]?.jsonPrimitive?.content
        assertNotNull(query)
        assertTrue(query!!.contains("query getAlbumInfo(\$id: ID!)"))
        assertTrue(query.contains("fragment AlbumStandard on Album"))

        val variables = jsonObject["variables"]?.jsonObject
        assertNotNull(variables)
        assertEquals(albumId.toString(), variables!!["id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `getAlbumInfo with non-positive albumId returns empty string`() {
        assertEquals("", getAlbumInfo(0))
        assertEquals("", getAlbumInfo(-1))
    }
}
