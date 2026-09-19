package com.client.xvideos.r.model

import com.client.xvideos.r.network.json.RJson
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class UserInfoSerializationTest {

    @Test
    fun `deserialize UserInfo with verified flag true and rich data`() {
        val json = """
            {
                "creationtime": 1600000000,
                "description": "Content creator & animator",
                "followers": 150240,
                "gifs": 820,
                "name": "Super Creator",
                "profileImageUrl": "https://userpic.redgifs.com/avatar.png",
                "profileUrl": "https://linktr.ee/creator",
                "publishedGifs": 750,
                "url": "https://www.redgifs.com/users/supercreator",
                "username": "supercreator",
                "verified": true,
                "views": 987654321
            }
        """.trimIndent()

        val user = RJson.decodeFromString<UserInfo>(json)

        assertEquals("supercreator", user.username)
        assertEquals("Super Creator", user.name)
        assertTrue(user.verified)
        assertEquals(150240L, user.followers)
        assertEquals(820L, user.gifs)
        assertEquals(750L, user.publishedGifs)
        assertEquals(987654321L, user.views)
        assertEquals("Content creator & animator", user.description)
        assertEquals("https://userpic.redgifs.com/avatar.png", user.profileImageUrl)
    }

    @Test
    fun `deserialize UserInfo with verified false or omitted`() {
        val jsonExplicitFalse = """
            {
                "username": "unverified_user",
                "name": "Regular User",
                "verified": false
            }
        """.trimIndent()

        val userExplicit = RJson.decodeFromString<UserInfo>(jsonExplicitFalse)
        assertFalse(userExplicit.verified)

        val jsonOmitted = """
            {
                "username": "legacy_user",
                "name": "Legacy User"
            }
        """.trimIndent()

        val userOmitted = RJson.decodeFromString<UserInfo>(jsonOmitted)
        assertFalse(userOmitted.verified)
        assertEquals(0L, userOmitted.followers)
        assertEquals(0L, userOmitted.views)
        assertNull(userOmitted.description)
        assertNull(userOmitted.profileImageUrl)
    }

    @Test
    fun `UserInfo roundtrip serialization preserves all fields`() {
        val original = UserInfo(
            description = "Bio text",
            creationtime = 123456789L,
            followers = 5000L,
            gifs = 42L,
            name = "Display Name",
            profileImageUrl = "https://cdn.example.com/pic.jpg",
            profileUrl = "https://beacons.ai/test",
            publishedGifs = 40L,
            url = "https://www.redgifs.com/users/testuser",
            username = "testuser",
            verified = true,
            views = 1000000L
        )

        val serialized = RJson.encodeToString(original)
        val deserialized = RJson.decodeFromString<UserInfo>(serialized)

        assertEquals(original, deserialized)
        assertTrue(deserialized.verified)
    }

    @Test
    fun `display name fallback logic behaves consistently`() {
        val userWithName = UserInfo(username = "alice", name = "Alice Wonderland")
        val userWithoutName = UserInfo(username = "bob", name = "")
        val userWithBlankName = UserInfo(username = "charlie", name = "   ")

        assertEquals("Alice Wonderland", userWithName.name.ifBlank { userWithName.username })
        assertEquals("bob", userWithoutName.name.ifBlank { userWithoutName.username })
        assertEquals("charlie", userWithBlankName.name.ifBlank { userWithBlankName.username })
    }

    @Test
    fun `ScreenRedProfile survives Java object serialization in saved state`() {
        val screen = ScreenRedProfile("supercreator")

        val byteOut = ByteArrayOutputStream()
        ObjectOutputStream(byteOut).use { it.writeObject(screen) }

        val byteIn = ByteArrayInputStream(byteOut.toByteArray())
        val restored = ObjectInputStream(byteIn).use { it.readObject() } as ScreenRedProfile

        assertNotNull(restored)
        assertEquals("supercreator", restored.profileName)
    }
}
