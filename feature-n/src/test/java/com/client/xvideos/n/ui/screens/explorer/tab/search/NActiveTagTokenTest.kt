package com.client.xvideos.n.ui.screens.explorer.tab.search

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.client.xvideos.n.model.NTag
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

    @Test
    fun `a suggestion replaces the token under the caret and keeps the rest of the query`() {
        val query = """title (-artist:"foo (bar)") language:english"""
        val caret = query.indexOf("foo") + "foo".length
        val token = activeNTagToken(query, caret)

        assertEquals("foo", token.searchTerm)
        assertEquals(NTagType.ARTIST, token.type)
        assertEquals("""-artist:"foo (bar)"""", query.substring(token.start, token.endExclusive))

        val replacement = replaceActiveNTagToken(
            TextFieldValue(query, selection = TextRange(caret)),
            NTag(id = 21L, type = "artist", name = "Foo Studio")
        )
        assertEquals("""title (-artist:"Foo Studio") language:english""", replacement.text)
        assertEquals(TextRange(replacement.text.indexOf(") language:")), replacement.selection)
    }

    @Test
    fun `a caret in whitespace does not use the nearby token`() {
        val query = "title  artist:foo"
        val token = activeNTagToken(query, caret = 6)

        assertEquals("", token.searchTerm)
        assertEquals(token.start, token.endExclusive)
    }
}
