package com.client.xvideos.common.videoplayer.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `пул на один плеер оставляет только ближний прогрев и кеш`() {
        assertEquals(FeedPreloadTier.NEAR_LOADED, FeedPreloadPolicy.tierFor(5, 5, 1))
        assertEquals(FeedPreloadTier.NEAR_LOADED, FeedPreloadPolicy.tierFor(6, 5, 1))
        // Диапазон 2..1 пустой, поэтому FAR_LOADED при таком пуле не существует:
        // греть в память некуда, всё дальше соседей уходит на диск.
        assertEquals(FeedPreloadTier.CACHED_ONLY, FeedPreloadPolicy.tierFor(7, 5, 1))
        assertEquals(FeedPreloadTier.CACHED_ONLY, FeedPreloadPolicy.tierFor(3, 5, 1))
    }

    @Test
    fun `в памяти дальний прогрев короче ближнего`() {
        assertTrue(FeedPreloadTier.FAR_LOADED.durationMs < FeedPreloadTier.NEAR_LOADED.durationMs)
    }

    @Test
    fun `на диск тянем длиннее, чем греем в память`() {
        // 5000 против 3000 — не опечатка и не нарушение «чем дальше, тем короче»:
        // CACHED_ONLY уходит в specifiedRangeCached(), то есть на диск, и оперативную
        // память не занимает вовсе. Шкала другая, сравнивать с NEAR/FAR как «больше
        // прогрева» нельзя. Дешёвый дисковый отрезок берём с запасом — при быстрой
        // прокрутке ролик тогда стартует с диска, а не из сети.
        assertTrue(FeedPreloadTier.CACHED_ONLY.durationMs > FeedPreloadTier.NEAR_LOADED.durationMs)
    }
}
