package com.client.xvideos.x

import com.client.xvideos.x.search.model.Channel
import com.client.xvideos.x.search.model.Keyword
import com.client.xvideos.x.search.model.Pornstar
import com.client.xvideos.x.search.model.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch75XSearchAndParsersTest {

    @Test
    fun `Keyword validation and mutation`() {
        val keywordValid = Keyword(N = "blonde", R = "4.8")
        assertTrue(keywordValid.isValidRating)
        assertEquals(4.8, keywordValid.ratingDoubleOrNull ?: 0.0, 0.001)

        val keywordInvalid = Keyword(N = "brunette", R = "abc")
        assertFalse(keywordInvalid.isValidRating)
        assertNull(keywordInvalid.ratingDoubleOrNull)
    }

    @Test
    fun `Pornstar profileUrl and formatters`() {
        val star = Pornstar(
            N = "Eva Elfie",
            F = "profiles/eva-elfie",
            T = "pornstar",
            MV = 1500,
            M = 0,
            L = 0,
            P = "https://cdn.example.com/eva.jpg",
            RF = "450k"
        )
        assertEquals("https://www.xvideos.com/profiles/eva-elfie", star.profileUrl)
        assertTrue(star.hasValidSubscribers)
        assertEquals("1.5k", star.formatVideos())

        val smallStar = star.copy(MV = 42, RF = "0")
        assertEquals("42", smallStar.formatVideos())
        assertFalse(smallStar.hasValidSubscribers)
    }

    @Test
    fun `Channel profileUrl and verification`() {
        val channel = Channel(
            N = "Brazzers",
            F = "profiles/brazzers",
            T = "channel",
            CPV = true,
            M = 0,
            L = 0,
            P = "https://cdn.example.com/logo.jpg",
            RF = "1.2M"
        )
        assertTrue(channel.isVerified)
        assertEquals("https://www.xvideos.com/profiles/brazzers", channel.profileUrl)
    }

    @Test
    fun `SearchResult filtering and suggestions list`() {
        val result = SearchResult(
            result = true,
            code = 200,
            keywords = listOf(Keyword(N = "cosplay", R = "1"), Keyword(N = "anime", R = "2")),
            pornstar = listOf(Pornstar(N = "Sweetie Fox", F = "profiles/sweetie", T = "pornstar", MV = 10, M = 0, L = 0, P = "", RF = "")),
            channel = listOf(Channel(N = "Sweetie Studio", F = "profiles/studio", T = "channel", CPV = false, M = 0, L = 0, P = "", RF = ""))
        )

        assertEquals(listOf("cosplay", "anime", "Sweetie Fox", "Sweetie Studio"), result.allSuggestions)
    }

}
