package com.client.xvideos.l.ui.screens.screenAlbum.model

import com.client.xvideos.l.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumTagGroupTest {

    private fun tag(id: String, category: String?, text: String) =
        Tag(id = id, category = category, text = text, count = 1)

    @Test
    fun `groupAlbumTags keeps first-appearance order and puts plain tags last`() {
        val plain = tag("1", null, "twintails")
        val character1 = tag("2", "Character", "Character: frieren")
        val ethnicity = tag("3", "Ethnicity", "Ethnicity: elf")
        val character2 = tag("4", "Character", "Character: fern")

        val groups = groupAlbumTags(listOf(plain, character1, ethnicity, character2))

        assertEquals(
            listOf(
                AlbumTagGroup("Character", listOf(character1, character2)),
                AlbumTagGroup("Ethnicity", listOf(ethnicity)),
                AlbumTagGroup(null, listOf(plain)),
            ),
            groups
        )
    }

    @Test
    fun `groupAlbumTags treats blank category as no category`() {
        val blank = tag("1", "  ", "twintails")

        assertEquals(listOf(AlbumTagGroup(null, listOf(blank))), groupAlbumTags(listOf(blank)))
    }

    @Test
    fun `groupAlbumTags returns nothing for no tags`() {
        assertEquals(emptyList<AlbumTagGroup>(), groupAlbumTags(emptyList()))
    }

    @Test
    fun `textWithoutCategory strips the category prefix ignoring case`() {
        assertEquals("frieren", tag("1", "Character", "Character: frieren").textWithoutCategory())
        assertEquals("frieren", tag("1", "Character", "character:frieren").textWithoutCategory())
    }

    @Test
    fun `textWithoutCategory keeps text without the prefix`() {
        assertEquals("twintails", tag("1", null, "twintails").textWithoutCategory())
        assertEquals("x", tag("1", "artist", "x").textWithoutCategory())
        assertEquals("Character:", tag("1", "Character", "Character:").textWithoutCategory())
    }
}
