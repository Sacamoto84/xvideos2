package com.client.xvideos.x.search

import com.client.xvideos.x.search.model.Channel
import com.client.xvideos.x.search.model.Keyword
import com.client.xvideos.x.search.model.Pornstar
import com.client.xvideos.x.search.model.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchModelTest {

    @Test
    fun `Keyword properties and parsing`() {
        val empty = Keyword.EMPTY
        assertFalse(empty.isValid)
        assertFalse(empty.hasRating)
        assertNull(empty.ratingDoubleOrNull)

        val kw = Keyword(N = "japanese", R = "94.5")
        assertTrue(kw.isValid)
        assertEquals("japanese", kw.name)
        assertEquals("94.5", kw.rating)
        assertTrue(kw.hasRating)
        assertEquals(94.5, kw.ratingDoubleOrNull ?: 0.0, 0.001)

        val badRating = Keyword(N = "test", R = "not-a-number")
        assertNull(badRating.ratingDoubleOrNull)
    }

    @Test
    fun `Pornstar properties and helpers`() {
        val empty = Pornstar.EMPTY
        assertFalse(empty.isValid)
        assertFalse(empty.hasAvatar)
        assertFalse(empty.hasSubscribers)
        assertFalse(empty.hasVideos)

        val star = Pornstar(
            N = "Star Name",
            F = "/profiles/star",
            T = "pornstar",
            MV = 15,
            M = 0,
            L = 0,
            P = "https://cdn/avatar.jpg",
            RF = "12.5k"
        )
        assertTrue(star.isValid)
        assertEquals("Star Name", star.name)
        assertEquals("/profiles/star", star.profilePath)
        assertEquals("https://cdn/avatar.jpg", star.avatarUrl)
        assertEquals(15, star.videoCount)
        assertEquals("12.5k", star.subscribers)
        assertTrue(star.hasAvatar)
        assertTrue(star.hasSubscribers)
        assertTrue(star.hasVideos)
    }

    @Test
    fun `Channel properties and helpers`() {
        val empty = Channel.EMPTY
        assertFalse(empty.isValid)
        assertFalse(empty.hasAvatar)
        assertFalse(empty.hasSubscribers)
        assertFalse(empty.isCpv)

        val ch = Channel(
            N = "Best Channel",
            F = "/channels/best",
            T = "channel",
            CPV = true,
            M = 0,
            L = 0,
            P = "https://cdn/channel.jpg",
            RF = "500"
        )
        assertTrue(ch.isValid)
        assertEquals("Best Channel", ch.name)
        assertEquals("/channels/best", ch.profilePath)
        assertEquals("https://cdn/channel.jpg", ch.avatarUrl)
        assertEquals("500", ch.subscribers)
        assertTrue(ch.hasAvatar)
        assertTrue(ch.hasSubscribers)
        assertTrue(ch.isCpv)
    }

    @Test
    fun `SearchResult inspection flags`() {
        val empty = SearchResult.EMPTY
        assertTrue(empty.isEmpty)
        assertFalse(empty.isNotEmpty)
        assertFalse(empty.hasKeywords)
        assertFalse(empty.hasPornstars)
        assertFalse(empty.hasChannels)
        assertFalse(empty.isBlacklisted)

        val populated = SearchResult(
            result = true,
            code = 200,
            keywords = listOf(Keyword(N = "tag", R = "10.0")),
            pornstar = listOf(Pornstar.EMPTY),
            channel = listOf(Channel.EMPTY),
            BLACKLISTED = true
        )
        assertFalse(populated.isEmpty)
        assertTrue(populated.isNotEmpty)
        assertTrue(populated.hasKeywords)
        assertTrue(populated.hasPornstars)
        assertTrue(populated.hasChannels)
        assertTrue(populated.isBlacklisted)
    }
}
