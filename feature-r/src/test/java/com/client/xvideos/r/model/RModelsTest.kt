package com.client.xvideos.r.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RModelsTest {

    @Test
    fun `Order properties and safe parsing`() {
        assertEquals(Order.LATEST, Order.DEFAULT)
        assertTrue(Order.LATEST.isLatest)
        assertFalse(Order.LATEST.isTrending)
        assertTrue(Order.TOP.isTop)
        assertTrue(Order.TRENDING.isTrending)
        assertTrue(Order.OLDEST.isOldest)
        assertTrue(Order.RELEVANT.isRelevant)

        assertEquals(Order.LATEST, Order.fromValueOrDefault("latest"))
        assertEquals(Order.TOP, Order.fromValueOrDefault("top"))
        assertEquals(Order.LATEST, Order.fromValueOrDefault(null))
        assertEquals(Order.LATEST, Order.fromValueOrDefault("nonexistent"))
        assertEquals(Order.TOP, Order.fromValueOrDefault("nonexistent", default = Order.TOP))

        assertEquals(MediaType.ALL, MediaType.DEFAULT)
        assertTrue(MediaType.ALL.isAll)
        assertTrue(MediaType.IMAGE.isImage)
        assertTrue(MediaType.GIF.isGif)

        assertEquals(MediaType.GIF, MediaType.fromValue("g"))
        assertEquals(MediaType.IMAGE, MediaType.fromValue("i"))
        assertEquals(MediaType.ALL, MediaType.fromValue(null))
        assertNull(MediaType.fromValueOrNull("unknown"))
        assertNull(MediaType.fromValueOrNull(null))
    }

    @Test
    fun `URL1 inspection properties`() {
        val empty = URL1.EMPTY
        assertFalse(empty.isValid)
        assertFalse(empty.hasThumbnail)
        assertFalse(empty.hasSd)
        assertFalse(empty.hasHd)
        assertFalse(empty.hasSilent)
        assertFalse(empty.hasPoster)
        assertFalse(empty.hasHtml)

        val full = URL1(
            thumbnail = "https://cdn/thumb.jpg",
            sd = "https://cdn/sd.mp4",
            hd = "https://cdn/hd.mp4",
            silent = "https://cdn/silent.mp4",
            poster = "https://cdn/poster.jpg",
            html = "https://cdn/embed"
        )
        assertTrue(full.isValid)
        assertTrue(full.hasThumbnail)
        assertTrue(full.hasSd)
        assertTrue(full.hasHd)
        assertTrue(full.hasSilent)
        assertTrue(full.hasPoster)
        assertTrue(full.hasHtml)
        assertEquals("https://cdn/hd.mp4", full.bestVideoUrl)
        assertEquals("https://cdn/poster.jpg", full.bestImageUrl)
    }

    @Test
    fun `GifsInfo inspection properties`() {
        val empty = GifsInfo.EMPTY
        assertFalse(empty.isValid)
        assertFalse(empty.hasTags)
        assertFalse(empty.hasNiches)
        assertFalse(empty.hasDescription)
        assertFalse(empty.hasDuration)
        assertFalse(empty.hasViews)
        assertFalse(empty.hasLikes)
        assertFalse(empty.hasUrls)
        assertFalse(empty.hasUserName)

        val populated = GifsInfo(
            id = "gif123",
            contentType = "Solo Female",
            likes = 42,
            tags = listOf("tag1", "tag2"),
            description = "Custom description",
            views = 1000L,
            userName = "creative_user",
            urls = URL1(thumbnail = "https://cdn/thumb.jpg"),
            duration = 12.5,
            niches = listOf("niche1")
        )
        assertTrue(populated.isValid)
        assertTrue(populated.hasTags)
        assertTrue(populated.hasNiches)
        assertTrue(populated.hasDescription)
        assertTrue(populated.hasDuration)
        assertTrue(populated.hasViews)
        assertTrue(populated.hasLikes)
        assertTrue(populated.hasUrls)
        assertTrue(populated.hasUserName)
    }

    @Test
    fun `UserInfo inspection properties`() {
        val empty = UserInfo.EMPTY
        assertFalse(empty.isValid)
        assertFalse(empty.hasAvatar)
        assertFalse(empty.hasDescription)
        assertFalse(empty.hasCreationTime)
        assertFalse(empty.hasFollowers)
        assertFalse(empty.hasGifs)
        assertFalse(empty.hasProfileUrl)
        assertFalse(empty.isVerified)

        val user = UserInfo(
            username = "jane_doe",
            name = "Jane",
            description = "Bio text",
            profileImageUrl = "https://cdn/avatar.png",
            creationtime = 123456789L,
            followers = 1500L,
            gifs = 25L,
            profileUrl = "https://social.link",
            verified = true
        )
        assertTrue(user.isValid)
        assertEquals("Jane", user.displayName)
        assertTrue(user.hasAvatar)
        assertTrue(user.hasDescription)
        assertTrue(user.hasCreationTime)
        assertTrue(user.hasFollowers)
        assertTrue(user.hasGifs)
        assertTrue(user.hasProfileUrl)
        assertTrue(user.isVerified)
    }
}
