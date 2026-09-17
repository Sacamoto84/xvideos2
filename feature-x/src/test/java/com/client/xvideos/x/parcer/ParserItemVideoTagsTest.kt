package com.client.xvideos.x.parcer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserItemVideoTagsTest {

    @Test
    fun tagsAreCleanedDistinctAndSortedAlphabetically() {
        val html = """
            <html><body>
              <ul>
                <li><a class="is-keyword" href="/tags/zulu">zulu</a></li>
                <li><a class="is-keyword" href="/tags/alpha"> alpha </a></li>
                <li><a class="is-keyword" href="/tags/beta">beta</a></li>
                <li><a class="is-keyword" href="/tags/alpha">alpha</a></li>
                <li><a class="is-keyword" href="/tags/empty">   </a></li>
              </ul>
            </body></html>
        """.trimIndent()

        val result = parserItemVideoTags(html)
        assertEquals(listOf("alpha", "beta", "zulu"), result.tags)
    }

    @Test
    fun emptyHtmlReturnsEmptyTagsModel() {
        val result = parserItemVideoTags("<html><body></body></html>")
        assertTrue(result.tags.isEmpty())
        assertTrue(result.mainUploader.isEmpty())
        assertTrue(result.pornstars.isEmpty())
    }

    @Test
    fun mainUploaderAndPornstarsParsedCorrectly() {
        val html = """
            <html><body>
              <ul>
                <li class="main-uploader">
                  <a href="/channels/studio1">
                    <span class="name">Studio One</span>
                    <span class="count">12k</span>
                  </a>
                </li>
                <li class="model">
                  <a href="/pornstars/actor1">
                    <span class="name">Actor One</span>
                    <span class="count">99k</span>
                  </a>
                </li>
              </ul>
            </body></html>
        """.trimIndent()

        val result = parserItemVideoTags(html)
        assertEquals(1, result.mainUploader.size)
        assertEquals("Studio One", result.mainUploader[0].name)
        assertEquals("/channels/studio1", result.mainUploader[0].href)
        assertEquals("12k", result.mainUploader[0].count)

        assertEquals(1, result.pornstars.size)
        assertEquals("Actor One", result.pornstars[0].name)
        assertEquals("/pornstars/actor1", result.pornstars[0].href)
        assertEquals("99k", result.pornstars[0].count)
    }
}
