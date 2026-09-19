package com.client.xvideos.r.network.api

import com.client.xvideos.r.model.search.SearchCreatorsResponse
import com.client.xvideos.r.model.search.SearchItemCreatorsResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RedSearchCreatorsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `deserialize SearchCreatorsResponse parses creator items accurately`() {
        val payload = """
            {
                "items": [
                    {
                        "type": "creator",
                        "text": "@elfsandi",
                        "name": "Ana 💋",
                        "image": "https://userpic.redgifs.com/5/3f/53f9367f4b1d523a032f5fa2475de70d.png",
                        "verified": true,
                        "studio": false,
                        "followers": 274
                    },
                    {
                        "type": "creator",
                        "text": "@ana-fernandez",
                        "name": "ana-fernandez",
                        "image": null,
                        "verified": false,
                        "studio": false,
                        "followers": 77
                    }
                ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<SearchCreatorsResponse>(payload)
        assertEquals(2, parsed.items.size)

        val first = parsed.items[0]
        assertEquals("creator", first.type)
        assertEquals("@elfsandi", first.text)
        assertEquals("Ana 💋", first.name)
        assertEquals("https://userpic.redgifs.com/5/3f/53f9367f4b1d523a032f5fa2475de70d.png", first.image)
        assertTrue(first.verified)
        assertFalse(first.studio)
        assertEquals(274L, first.followers)

        val second = parsed.items[1]
        assertEquals("@ana-fernandez", second.text)
        assertEquals("ana-fernandez", second.name)
        assertNull(second.image)
        assertFalse(second.verified)
        assertEquals(77L, second.followers)
    }

    @Test
    fun `handle normalization strips @ prefix and falls back to name`() {
        val itemWithAt = SearchItemCreatorsResponse(text = "@cosplay_queen", name = "Queen")
        val handle1 = itemWithAt.text.removePrefix("@").ifBlank { itemWithAt.name }
        assertEquals("cosplay_queen", handle1)

        val itemWithoutAt = SearchItemCreatorsResponse(text = "cosplay_queen", name = "Queen")
        val handle2 = itemWithoutAt.text.removePrefix("@").ifBlank { itemWithoutAt.name }
        assertEquals("cosplay_queen", handle2)

        val itemEmptyText = SearchItemCreatorsResponse(text = "@", name = "FallbackName")
        val handle3 = itemEmptyText.text.removePrefix("@").ifBlank { itemEmptyText.name }
        assertEquals("FallbackName", handle3)
    }
}
