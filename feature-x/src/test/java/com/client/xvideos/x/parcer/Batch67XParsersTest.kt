package com.client.xvideos.x.parcer

import com.client.xvideos.x.search.buildSuggestUrl
import com.client.xvideos.x.search.encodeSuggestQuery
import com.client.xvideos.x.search.isValidSearchJson
import com.client.xvideos.x.search.parseJsonKeywords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch67XParsersTest {

    @Test
    fun `preview url helpers detect static and preview formats`() {
        val staticUrl = "https://cdn77-pic.xvideos-cdn.com/videos/thumbs169ll/6a/4f/6b/6a4f6bafe3abb03b5ea6108ab18ff1ad/6a4f6bafe3abb03b5ea6108ab18ff1ad.30.jpg"
        assertTrue(isStaticThumbUrl(staticUrl))
        assertFalse(isVideoPreviewUrl(staticUrl))

        val previewUrl = parserVideoPreviewFromImageUrl(staticUrl)
        assertNotNull(previewUrl)
        assertTrue(isVideoPreviewUrl(previewUrl))
        assertFalse(isStaticThumbUrl(previewUrl))

        assertEquals("6a4f6bafe3abb03b5ea6108ab18ff1ad", extractVideoHashFromPreviewUrl(previewUrl))
        assertEquals(previewUrl, staticUrl.toVideoPreviewUrl())
    }

    @Test
    fun `parserScreenTags extracts headers and counts`() {
        val html = """
            <html>
            <body>
                <h2 class="page-title">Amateur Videos <span class="sub">Page 1</span></h2>
                <div class="pagination">
                    <a href="/page/1">1</a>
                    <a href="/page/2">2</a>
                    <a href="/page/5" class="last-page">5</a>
                </div>
                <div class="mozaique">
                    <div class="frame-block thumb-block" data-id="123">
                        <p class="title"><a href="/video123" title="Test Video">Test</a></p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val (title0, title1) = parseScreenTagTitle(html)
        assertEquals("Amateur Videos", title0)
        assertEquals("Page 1", title1)

        assertEquals(5, parseScreenTagPageCount(html))
        assertEquals(1, parseTagItemCount(html))
    }

    @Test
    fun `parserItemVideo script lines and property counts`() {
        val script = """
            var dummy = 1;
            html5player.setVideoTitle('Cool Video');
            html5player.setVideoUrlHigh('https://cdn.com/high.mp4');
            html5player.setVideoUrlLow('https://cdn.com/low.mp4');
        """.trimIndent()

        assertTrue(hasPlayerMarker(script))
        assertFalse(hasPlayerMarker("var foo = 'bar';"))

        val lines = extractPlayerScriptLines(script)
        assertEquals(3, lines.size)
        assertEquals(3, countPlayerProperties(script))
    }

    @Test
    fun `parserListVideo extracts IDs and counts`() {
        val html = """
            <div class="frame-block" data-id="101"></div>
            <div class="frame-block" data-id="102"></div>
            <div class="frame-block" data-id="0"></div>
            <div class="frame-block" data-id="invalid"></div>
        """.trimIndent()

        val ids = parseVideoIds(html)
        assertEquals(listOf(101L, 102L), ids)
        assertEquals(4, parseVideoCount(html))
        assertTrue(hasVideosInList(html))
        assertFalse(hasVideosInList(null))
    }

    @Test
    fun `parserItemVideoTags parses keywords and uploaders`() {
        val html = """
            <ul>
                <li class="main-uploader"><a href="/profiles/uploader"><span class="name">MainUser</span><span class="count">100</span></a></li>
                <li class="model"><a href="/pornstars/star"><span class="name">StarGirl</span><span class="count">50</span></a></li>
                <li><a class="is-keyword" href="/tag/blonde">Blonde</a></li>
                <li><a class="is-keyword" href="/tag/teen">Teen</a></li>
            </ul>
        """.trimIndent()

        val keywords = parseKeywordsOnly(html)
        assertEquals(listOf("Blonde", "Teen"), keywords)

        val uploaders = parseUploadersOnly(html)
        assertEquals(1, uploaders.size)
        assertEquals("MainUser", uploaders.first().name)

        val pornstars = parsePornstarsOnly(html)
        assertEquals(1, pornstars.size)
        assertEquals("StarGirl", pornstars.first().name)
    }

    @Test
    fun `parseHTML5Player extractions and stream checking`() {
        val script = """
            html5player.setVideoTitle('My Best Clip');
            html5player.setVideoUrlHigh('https:\/\/cdn.com\/h.mp4');
            html5player.setVideoUrlLow('https:\/\/cdn.com\/l.mp4');
        """.trimIndent()

        assertEquals("My Best Clip", extractVideoTitle(script))
        val (high, low, hls) = extractVideoUrls(script)
        assertEquals("https://cdn.com/h.mp4", high)
        assertEquals("https://cdn.com/l.mp4", low)
        assertEquals("", hls)

        assertTrue(hasPlayableStream(script))
        assertFalse(hasPlayableStream("html5player.setVideoTitle('No stream');"))
    }

    @Test
    fun `search json parsing and url building`() {
        val json = """{"result":true,"code":200,"keywords":[{"N":"blonde","R":"99"},{"N":"brunette","R":"88"}]}"""
        assertTrue(isValidSearchJson(json))
        assertFalse(isValidSearchJson("not a json"))

        val kws = parseJsonKeywords(json)
        assertEquals(listOf("blonde", "brunette"), kws)

        assertEquals("hello%20world", encodeSuggestQuery("hello world"))
        assertEquals("https://www.xv-ru.com/search-suggest/test%20query", buildSuggestUrl("test query"))
    }
}
