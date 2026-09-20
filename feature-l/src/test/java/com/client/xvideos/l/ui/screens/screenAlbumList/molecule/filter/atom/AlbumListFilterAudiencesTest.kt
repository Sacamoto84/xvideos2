package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import com.client.xvideos.l.net.graphQl.Audience
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumListFilterAudiencesTest {

    @Test
    fun `parseAudienceIds parses positive tokens`() {
        val result = parseAudienceIds("+1+2+3")
        assertEquals(setOf("1", "2", "3"), result)
    }

    @Test
    fun `parseAudienceIds handles empty and blank input`() {
        assertTrue(parseAudienceIds("").isEmpty())
        assertTrue(parseAudienceIds("   ").isEmpty())
    }

    @Test
    fun `parseAudienceIds ignores malformed tokens`() {
        val result = parseAudienceIds("no_plus+valid-id")
        assertEquals(setOf("valid"), result)
    }

    @Test
    fun `encodeAudienceIds orders tokens matching audiences list`() {
        val audiences = listOf(
            Audience(id = "1", title = "Audience 1", description = "", posterUrl = "", url = ""),
            Audience(id = "2", title = "Audience 2", description = "", posterUrl = "", url = ""),
            Audience(id = "3", title = "Audience 3", description = "", posterUrl = "", url = ""),
        )
        val selected = setOf("3", "1")
        val encoded = encodeAudienceIds(selected, audiences)
        assertEquals("+1+3", encoded)
    }

    @Test
    fun `fallbackAudiences produces non-empty list of audiences`() {
        val fallback = fallbackAudiences()
        assertTrue(fallback.isNotEmpty())
        assertTrue(fallback.all { it.id.isNotBlank() && it.title.isNotBlank() })
    }
}
