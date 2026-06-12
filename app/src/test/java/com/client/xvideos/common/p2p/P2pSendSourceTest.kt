package com.client.xvideos.common.p2p

import com.client.xvideos.l.model.PicsDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class P2pSendSourceTest {

    @Test
    fun `DownloadL round-trips PicsDetails through json`() {
        val item = PicsDetails(
            height = 846,
            width = 1280,
            is_animated = false,
            url_to_original = "https://x.example/orig.jpg",
            url_to_video = null,
        )

        val source = P2pSendSource.DownloadL.of(item)
        val restored = source.item()

        assertEquals(item.height, restored?.height)
        assertEquals(item.width, restored?.width)
        assertEquals(item.is_animated, restored?.is_animated)
        assertEquals(item.url_to_original, restored?.url_to_original)
    }
}
