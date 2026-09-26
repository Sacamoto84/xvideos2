package com.client.xvideos.l.net.graphQl

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaCategoriesModelsTest {

    @Test
    fun `MediaCategories emptiness checks`() {
        val empty = MediaCategories()
        assertTrue(empty.isEmpty)
        assertFalse(empty.isNotEmpty)

        val withLanguages = empty.copy(languages = listOf(Language(id = "en", title = "English")))
        assertFalse(withLanguages.isEmpty)
        assertTrue(withLanguages.isNotEmpty)
    }

    @Test
    fun `FilterSettings inspection helpers`() {
        val defaultSettings = FilterSettings()
        assertFalse(defaultSettings.hasAudienceFilter)
        assertFalse(defaultSettings.hasBlockedGenres)
        assertFalse(defaultSettings.hasSubscribedGenres)
        assertFalse(defaultSettings.hasPreferredLanguages)

        val custom = defaultSettings.copy(
            audienceIds = listOf("1"),
            genresBlockedIds = listOf("2"),
            genresSubscribedIds = listOf("3"),
            preferredLanguageIds = listOf("en")
        )
        assertTrue(custom.hasAudienceFilter)
        assertTrue(custom.hasBlockedGenres)
        assertTrue(custom.hasSubscribedGenres)
        assertTrue(custom.hasPreferredLanguages)
    }

    @Test
    fun `Language ContentType Audience validation and helpers`() {
        val lang = Language(id = "1", title = "English", url = "/lang/1")
        assertTrue(lang.isValid)
        assertFalse(Language.EMPTY.isValid)

        val ct = ContentType(id = "c1", title = "Comics", url = "/ct/1")
        assertTrue(ct.isValid)
        assertFalse(ContentType.EMPTY.isValid)

        val audience = Audience(
            id = "a1",
            title = "Straight",
            description = "Men with women",
            posterUrl = "https://cdn/poster.jpg",
            url = "/aud/1"
        )
        assertTrue(audience.isValid)
        assertTrue(audience.hasDescription)
        assertTrue(audience.hasPoster)

        val emptyAudience = Audience.EMPTY
        assertFalse(emptyAudience.isValid)
        assertFalse(emptyAudience.hasDescription)
        assertFalse(emptyAudience.hasPoster)
    }
}
