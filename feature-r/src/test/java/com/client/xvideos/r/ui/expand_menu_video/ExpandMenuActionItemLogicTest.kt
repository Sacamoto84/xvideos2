package com.client.xvideos.r.ui.expand_menu_video

import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.UserInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandMenuActionItemLogicTest {

    @Test
    fun `isSubscribed correctly resolves from creators list`() {
        val subscribedCreators = listOf(
            UserInfo(username = "creator_alpha"),
            UserInfo(username = "creator_beta"),
        )

        fun checkSubscribed(item: GifsInfo?): Boolean {
            return item?.userName?.takeIf { it.isNotBlank() }?.let { name ->
                subscribedCreators.any { it.username == name }
            } ?: false
        }

        assertTrue(checkSubscribed(GifsInfo(id = "1", userName = "creator_alpha")))
        assertTrue(checkSubscribed(GifsInfo(id = "2", userName = "creator_beta")))
        assertFalse(checkSubscribed(GifsInfo(id = "3", userName = "creator_unknown")))
        assertFalse(checkSubscribed(GifsInfo(id = "4", userName = "")))
        assertFalse(checkSubscribed(GifsInfo(id = "5", userName = "   ")))
        assertFalse(checkSubscribed(null))
    }

    @Test
    fun `isFollowed correctly resolves from following list`() {
        val followedCreators = listOf(
            UserInfo(username = "star_model"),
        )

        fun checkFollowed(item: GifsInfo?): Boolean {
            return item?.userName?.takeIf { it.isNotBlank() }?.let { name ->
                followedCreators.any { it.username == name }
            } ?: false
        }

        assertTrue(checkFollowed(GifsInfo(id = "1", userName = "star_model")))
        assertFalse(checkFollowed(GifsInfo(id = "2", userName = "other_model")))
        assertFalse(checkFollowed(GifsInfo(id = "3", userName = "")))
        assertFalse(checkFollowed(null))
    }
}
