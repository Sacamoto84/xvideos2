package com.client.xvideos.x

import com.client.xvideos.x.search.model.Channel
import com.client.xvideos.x.search.model.Keyword
import com.client.xvideos.x.search.model.Pornstar
import com.client.xvideos.x.search.model.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

}
