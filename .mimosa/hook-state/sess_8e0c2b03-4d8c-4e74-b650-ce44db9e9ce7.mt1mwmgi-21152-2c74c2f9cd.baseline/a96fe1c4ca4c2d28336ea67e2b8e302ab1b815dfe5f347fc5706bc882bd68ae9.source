package com.client.xvideos.l.featured.saved

import com.client.xvideos.l.model.PicsDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LP2pSendTest {

    @Test
    fun `DownloadL round-trips PicsDetails through json`() {
        val item = PicsDetails(
            height = 846,
            width = 1280,
            is_animated = false,
            url_to_original = "https://x.example/orig.jpg",
            url_to_video = null,
        )

        val source = lP2pSendSource(item)
        val restored = lP2pItem(source.itemJson)

        assertEquals(item.height, restored?.height)
        assertEquals(item.width, restored?.width)
        assertEquals(item.is_animated, restored?.is_animated)
        assertEquals(item.url_to_original, restored?.url_to_original)
    }

    @Test
    fun `битая строка не роняет разбор`() {
        assertNull(lP2pItem("не json"))
    }
}
