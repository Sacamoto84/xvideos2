package com.client.xvideos.x.parcer

import com.client.xvideos.x.search.buildSuggestUrl
import com.client.xvideos.x.search.encodeSuggestQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class Batch67XParsersTest {

    @Test
    fun `preview url helpers detect static and preview formats`() {
        val staticUrl = "https://cdn77-pic.xvideos-cdn.com/videos/thumbs169ll/6a/4f/6b/6a4f6bafe3abb03b5ea6108ab18ff1ad/6a4f6bafe3abb03b5ea6108ab18ff1ad.30.jpg"

        val previewUrl = parserVideoPreviewFromImageUrl(staticUrl)
        assertNotNull(previewUrl)
    }

    @Test
    fun `search json parsing and url building`() {
        assertEquals("hello%20world", encodeSuggestQuery("hello world"))
        assertEquals("https://www.xv-ru.com/search-suggest/test%20query", buildSuggestUrl("test query"))
    }
}
