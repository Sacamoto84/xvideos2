package com.client.xvideos.n

import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.repository.NGalleryCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NGalleryCacheTest {

    @Test
    fun `cache evicts the least recently used album`() {
        val cache = NGalleryCache(capacity = 2, nowMillis = { 0L })
        cache.put(NGallery(id = 1), requestNumber = 1)
        cache.put(NGallery(id = 2), requestNumber = 2)
        assertEquals(1L, cache[1]?.id)
        cache.put(NGallery(id = 3), requestNumber = 3)

        assertNull(cache[2])
        assertEquals(1L, cache[1]?.id)
        assertEquals(3L, cache[3]?.id)
    }

    @Test
    fun `cache expires an album even when it was recently read`() {
        var now = 0L
        val cache = NGalleryCache(ttlMillis = 500, nowMillis = { now })
        cache.put(NGallery(id = 1), requestNumber = 1)
        now = 400
        assertEquals(1L, cache[1]?.id)
        now = 500
        assertNull(cache[1])
    }
}
