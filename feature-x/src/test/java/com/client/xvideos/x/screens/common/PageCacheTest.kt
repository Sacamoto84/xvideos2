package com.client.xvideos.x.screens.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageCacheTest {

    @Test
    fun `страница отдаётся из памяти до истечения срока`() {
        var now = 0L
        val cache = PageCache<String>(maxPages = 5, ttlMs = 1_000L, nowMs = { now })
        cache[3] = "page3"

        now = 1_000L
        assertEquals("page3", cache[3])

        now = 1_001L
        assertNull(cache[3])
    }

    @Test
    fun `сверх лимита вытесняется давно не открытая страница`() {
        val cache = PageCache<String>(maxPages = 2)
        cache[0] = "p0"
        cache[1] = "p1"
        cache[0] // страница 0 открыта снова — теперь вытесняется 1
        cache[2] = "p2"

        assertEquals("p0", cache[0])
        assertNull(cache[1])
        assertEquals("p2", cache[2])
    }

    @Test
    fun `clear забывает все страницы`() {
        val cache = PageCache<String>(maxPages = 2)
        cache[0] = "p0"
        cache.clear()
        assertNull(cache[0])
    }
}
