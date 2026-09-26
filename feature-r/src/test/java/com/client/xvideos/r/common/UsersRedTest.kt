package com.client.xvideos.r.common

import com.client.xvideos.r.model.UserInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UsersRedTest {

    @Before
    @After
    fun setup() {
        UsersRed.clear()
    }

    @Test
    fun `UsersRed manages user cache correctly`() {
        assertTrue(UsersRed.isEmpty)
        assertFalse(UsersRed.isNotEmpty)
        assertEquals(0, UsersRed.count)
        assertEquals(emptyList<UserInfo>(), UsersRed.listAllUsers)

        val user1 = UserInfo(username = "alice", name = "Alice")
        UsersRed.addUser(user1)

        assertFalse(UsersRed.isEmpty)
        assertTrue(UsersRed.isNotEmpty)
        assertEquals(1, UsersRed.count)
        assertTrue(UsersRed.containsUser("alice"))
        assertFalse(UsersRed.containsUser("bob"))
        assertEquals(user1, UsersRed.findUser("alice"))
        assertNull(UsersRed.findUser("bob"))
        assertNull(UsersRed.findUser(""))

        // Ignore blank user
        UsersRed.addUser(UserInfo(username = ""))
        assertEquals(1, UsersRed.count)

        // Remove user
        UsersRed.removeUser("alice")
        assertEquals(0, UsersRed.count)
        assertFalse(UsersRed.containsUser("alice"))
    }
}
