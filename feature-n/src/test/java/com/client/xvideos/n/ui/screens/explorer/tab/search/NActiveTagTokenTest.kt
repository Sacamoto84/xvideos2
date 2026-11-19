package com.client.xvideos.n.ui.screens.explorer.tab.search

import com.client.xvideos.n.model.NTagType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NActiveTagTokenTest {

    @Test
    fun `unfinished quoted tag keeps all words in one token`() {
        val query = """language:english artist:"foo ba"""
        val token = activeNTagToken(query)

        assertEquals("language:english ", query.take(token.start))
        assertEquals("foo ba", token.searchTerm)
        assertEquals(NTagType.ARTIST, token.type)
    }

    @Test
    fun `escaped quotes and excluded tags are handled`() {
        val query = """artist:"foo \"bar\"" -group:"baz qu"""
        val token = activeNTagToken(query)

        assertEquals("""artist:"foo \"bar\"" """, query.take(token.start))
        assertEquals("baz qu", token.searchTerm)
        assertEquals(NTagType.GROUP, token.type)
        assertTrue(token.excluded)
    }

    @Test
    fun `an escaped final quote stays part of the active search term`() {
        val query = """artist:"foo \""""
        assertEquals("foo \"", activeNTagToken(query).searchTerm)
    }
}
