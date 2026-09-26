package com.client.xvideos.x

import com.client.xvideos.x.parcer.hasPlayerScript
import com.client.xvideos.x.parcer.parseHTML5PlayerOrNull
import com.client.xvideos.x.parcer.parserListVideoOrEmpty
import com.client.xvideos.x.search.model.Channel
import com.client.xvideos.x.search.model.Keyword
import com.client.xvideos.x.search.model.Pornstar
import com.client.xvideos.x.search.model.SearchResult
import com.client.xvideos.x.search.parseJsonOrDefault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch56XSearchAndParsersTest {

    @Test
    fun `SearchResult totalSuggestionsCount and inspection properties`() {
        val emptyResult = SearchResult.EMPTY
        assertEquals(0, emptyResult.totalSuggestionsCount)
        assertTrue(emptyResult.isEmpty)
        assertFalse(emptyResult.isNotEmpty)

        val populatedResult = SearchResult(
            result = true,
            code = 200,
            keywords = listOf(Keyword("tag1", "100"), Keyword("tag2", "200")),
            pornstar = listOf(Pornstar(N = "Star 1", F = "/star1", T = "pornstar", MV = 10, M = 0, L = 0, P = "", RF = "1000")),
            channel = listOf(Channel(N = "Chan 1", F = "/chan1", T = "channel", CPV = true, M = 0, L = 0, P = "", RF = "500"))
        )
        assertEquals(4, populatedResult.totalSuggestionsCount)
        assertTrue(populatedResult.hasKeywords)
        assertTrue(populatedResult.hasPornstars)
        assertTrue(populatedResult.hasChannels)
        assertFalse(populatedResult.isEmpty)
        assertTrue(populatedResult.isNotEmpty)
    }

    @Test
    fun `parseJsonOrDefault returns parsed object or fallback default`() {
        val fallback = SearchResult(result = false, code = 404)
        val defaultResult = parseJsonOrDefault(null, fallback)
        assertEquals(fallback, defaultResult)

        val invalidJsonResult = parseJsonOrDefault("invalid-json", fallback)
        assertEquals(fallback, invalidJsonResult)

        val validJson = """{"result":true,"code":200,"keywords":[{"N":"test","R":"100"}]}"""
        val parsed = parseJsonOrDefault(validJson, fallback)
        assertTrue(parsed.result)
        assertEquals(200, parsed.code)
        assertEquals(1, parsed.keywords.size)
        assertEquals("test", parsed.keywords.first().N)
    }

    @Test
    fun `parseHTML5PlayerOrNull safe handling`() {
        assertNull(parseHTML5PlayerOrNull(null))
        assertNull(parseHTML5PlayerOrNull(""))
        assertNull(parseHTML5PlayerOrNull("<html>no player</html>"))

        val scriptWithSources = """
            html5player.setVideoTitle('Test Video');
            html5player.setVideoUrlHigh('https:\/\/cdn.example.com\/video.mp4');
        """.trimIndent()
        val config = parseHTML5PlayerOrNull(scriptWithSources)
        assertNotNull(config)
        assertEquals("Test Video", config?.videoTitle)
        assertEquals("https://cdn.example.com/video.mp4", config?.videoUrlHigh)
    }

    @Test
    fun `hasPlayerScript detects presence of marker`() {
        assertFalse(hasPlayerScript(null))
        assertFalse(hasPlayerScript(""))
        assertFalse(hasPlayerScript("<div>hello world</div>"))
        assertTrue(hasPlayerScript("<script>html5player.setVideoTitle('a');</script>"))
    }

    @Test
    fun `parserListVideoOrEmpty safe handling of null and blank`() {
        assertTrue(parserListVideoOrEmpty(null).isEmpty())
        assertTrue(parserListVideoOrEmpty("").isEmpty())
        assertTrue(parserListVideoOrEmpty("   ").isEmpty())
        assertTrue(parserListVideoOrEmpty("<div>no video blocks</div>").isEmpty())
    }
}
