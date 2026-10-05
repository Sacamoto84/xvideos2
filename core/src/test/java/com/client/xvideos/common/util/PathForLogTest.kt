package com.client.xvideos.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PathForLogTest {

    @Test
    fun `pathForLog drops scheme, host, query and fragment`() {
        assertEquals("a/b.jpg", "https://cdn.example.org/a/b.jpg".pathForLog())
        assertEquals("a/b", "https://example.org/a/b?page=2&q=x".pathForLog())
        assertEquals("a/b", "http://example.org:8080/a/b#top".pathForLog())
        assertEquals("p", "https://user:secret@example.org/p".pathForLog())
    }

    @Test
    fun `pathForLog never returns the host`() {
        assertEquals("", "https://example.org".pathForLog())
        assertEquals("", "https://example.org/".pathForLog())
        assertEquals("", "https://example.org?k=v".pathForLog())
        assertEquals("", "example.org".pathForLog())
        assertEquals("a/b", "//cdn.example.org/a/b".pathForLog())
    }

    @Test
    fun `pathForLog ignores an address inside the query`() {
        assertEquals("a", "https://example.org/a?next=https://other.example.org/b".pathForLog())
        assertEquals("a/b", "/a/b?next=https://other.example.org/c".pathForLog())
    }

    @Test
    fun `pathForLog keeps a path that has no host`() {
        assertEquals("a/b", "/a/b".pathForLog())
        assertEquals("storage/dir/x.jpg", "file:///storage/dir/x.jpg".pathForLog())
        assertEquals("", "".pathForLog())
    }

    @Test
    fun `fileNameForLog returns the last path segment`() {
        assertEquals("b.jpg", "https://cdn.example.org/a/b.jpg?token=1".fileNameForLog())
        assertEquals("x.jpg", "/storage/dir/x.jpg".fileNameForLog())
        assertEquals("b", "https://example.org/a/b/".fileNameForLog())
        assertEquals("12", "content://authority/items/12".fileNameForLog())
    }

    @Test
    fun `fileNameForLog is empty when there is no path`() {
        assertEquals("", "https://example.org".fileNameForLog())
        assertEquals("", "example.org".fileNameForLog())
    }
}
