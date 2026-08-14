package com.client.xvideos.common.videoplayer.feed

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedPreloadPolicyTest {

    @Test
    fun `текущая страница и соседи греются полностью`() {
        assertEquals(FeedPreloadTier.NEAR_LOADED, FeedPreloadPolicy.tierFor(5, 5, 3))
        assertEquals(FeedPreloadTier.NEAR_LOADED, FeedPreloadPolicy.tierFor(4, 5, 3))
        assertEquals(FeedPreloadTier.NEAR_LOADED, FeedPreloadPolicy.tierFor(6, 5, 3))
    }

    @Test
    fun `в пределах ёмкости пула греем коротко`() {
        assertEquals(FeedPreloadTier.FAR_LOADED, FeedPreloadPolicy.tierFor(7, 5, 3))
        assertEquals(FeedPreloadTier.FAR_LOADED, FeedPreloadPolicy.tierFor(8, 5, 3))
        assertEquals(FeedPreloadTier.FAR_LOADED, FeedPreloadPolicy.tierFor(2, 5, 3))
    }

    @Test
    fun `дальше ёмкости пула только кеш на диск`() {
        assertEquals(FeedPreloadTier.CACHED_ONLY, FeedPreloadPolicy.tierFor(9, 5, 3))
        assertEquals(FeedPreloadTier.CACHED_ONLY, FeedPreloadPolicy.tierFor(0, 5, 3))
    }

    @Test
    fun `до первого свайпа текущая страница неизвестна и ничего не греем`() {
        assertEquals(FeedPreloadTier.CACHED_ONLY, FeedPreloadPolicy.tierFor(0, -1, 3))
        assertEquals(FeedPreloadTier.CACHED_ONLY, FeedPreloadPolicy.tierFor(1, -1, 3))
    }

    @Test
    fun `длительности прогрева убывают с расстоянием`() {
        assertEquals(3_000L, FeedPreloadPolicy.NEAR_LOADED_MS)
        assertEquals(1_000L, FeedPreloadPolicy.FAR_LOADED_MS)
        assertEquals(5_000L, FeedPreloadPolicy.CACHED_ONLY_MS)
    }
}
