package com.client.xvideos.l.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LikeStatusTest {

    @Test
    fun `статус лайка означает избранное`() {
        assertTrue("like".isLFavoriteLikeStatus())
        assertTrue("favorite".isLFavoriteLikeStatus())
    }

    @Test
    fun `нет статуса, none и dislike — не избранное`() {
        assertFalse(null.isLFavoriteLikeStatus())
        assertFalse("".isLFavoriteLikeStatus())
        assertFalse("  ".isLFavoriteLikeStatus())
        assertFalse("none".isLFavoriteLikeStatus())
        assertFalse("dislike".isLFavoriteLikeStatus())
    }
}
