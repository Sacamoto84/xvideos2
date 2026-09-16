package com.client.xvideos.x

import com.client.xvideos.x.screens.tags.sanitizeTagForUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.URI

class X_TagUrlSanitizationTest {

    private fun formatTagPath(tag: String, pageIndex: Int): String {
        val formattedTag = sanitizeTagForUrl(tag)
        return "$urlStart/tags/$formattedTag/$pageIndex"
    }

    @Test
    fun `single word tag is unchanged`() {
        val url = formatTagPath("blonde", 0)
        assertEquals("$urlStart/tags/blonde/0", url)
        assertFalse(url.contains(" "))
        URI.create(url)
    }

    @Test
    fun `multi word tag replaces spaces with hyphens`() {
        val url = formatTagPath("big tits", 1)
        assertEquals("$urlStart/tags/big-tits/1", url)
        assertFalse(url.contains(" "))
        URI.create(url)
    }

    @Test
    fun `tag with multiple spaces and leading trailing whitespace is cleaned`() {
        val url = formatTagPath("   hot   asian   babe   ", 3)
        assertEquals("$urlStart/tags/hot-asian-babe/3", url)
        assertFalse(url.contains(" "))
        URI.create(url)
    }

    @Test
    fun `tag with non-breaking spaces is sanitized to hyphens`() {
        val url = formatTagPath("teen\u00A0petite\u00A0brunette", 0)
        assertEquals("$urlStart/tags/teen-petite-brunette/0", url)
        assertFalse(url.contains(" "))
        assertFalse(url.contains("\u00A0"))
        URI.create(url)
    }

    @Test
    fun `hyphenated tag remains valid`() {
        val url = formatTagPath("step-mom", 2)
        assertEquals("$urlStart/tags/step-mom/2", url)
        assertFalse(url.contains(" "))
        URI.create(url)
    }

    @Test
    fun `consecutive special characters and spaces are collapsed to single hyphen`() {
        val url = formatTagPath("tag # & / query", 0)
        assertEquals("$urlStart/tags/tag-query/0", url)
        assertFalse(url.contains("--"))
        URI.create(url)
    }

    @Test
    fun `tag with special url characters is sanitized without corrupting path`() {
        val url = formatTagPath("tag#with?query&slash/and\\backslash", 0)
        assertEquals("$urlStart/tags/tag-with-query-slash-and-backslash/0", url)
        assertFalse(url.contains("#"))
        assertFalse(url.contains("?"))
        assertFalse(url.contains("&"))
        assertFalse(url.contains("\\"))
        URI.create(url)
    }

    @Test
    fun `tag with only special characters sanitizes to empty string`() {
        val sanitized = sanitizeTagForUrl("###///???---")
        assertEquals("", sanitized)
    }
}
