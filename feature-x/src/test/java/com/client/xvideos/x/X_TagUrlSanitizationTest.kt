package com.client.xvideos.x

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.URI

class X_TagUrlSanitizationTest {

    private fun formatTagPath(tag: String, pageIndex: Int): String {
        val formattedTag = tag.trim()
            .replace(Regex("[#?&/\\\\]+"), "-")
            .replace(Regex("\\s+"), "-")
            .trim('-')
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
    fun `hyphenated tag remains valid`() {
        val url = formatTagPath("step-mom", 2)
        assertEquals("$urlStart/tags/step-mom/2", url)
        assertFalse(url.contains(" "))
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
        val sanitized = "###///???".trim()
            .replace(Regex("[#?&/\\\\]+"), "-")
            .replace(Regex("\\s+"), "-")
            .trim('-')
        assertEquals("", sanitized)
    }
}
