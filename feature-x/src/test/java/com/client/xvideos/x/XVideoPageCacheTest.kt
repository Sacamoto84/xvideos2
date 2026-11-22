package com.client.xvideos.x

import com.client.xvideos.x.screens.videoplayer.XVideoPageCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XVideoPageCacheTest {
    @Test
    fun `evicts the least recently used page and ignores oversized pages`() {
        val cache = XVideoPageCache(maxBytes = 16)
        cache.put("a", "one")
        cache.put("b", "two")
        assertEquals("one", cache["a"])
        cache.put("c", "three is too large")
        assertNull(cache["c"])
        cache.put("d", "new")
        assertNull(cache["b"])
        assertEquals("one", cache["a"])
        assertEquals("new", cache["d"])
        cache.remove("d")
        assertNull(cache["d"])
    }
}
