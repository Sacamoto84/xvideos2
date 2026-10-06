package com.client.xvideos.x.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CountryFlagTest {

    @Test
    fun `valid country code returns corresponding emoji flag`() {
        // "US" -> U (0x1F1FA), S (0x1F1F8)
        val usFlag = "\uD83C\uDDFA\uD83C\uDDF8"
        assertEquals(usFlag, getFlagEmoji("flag-us"))
        assertEquals(usFlag, getFlagEmoji("flag-US"))

        // "RU" -> R (0x1F1F7), U (0x1F1FA)
        val ruFlag = "\uD83C\uDDF7\uD83C\uDDFA"
        assertEquals(ruFlag, getFlagEmoji("flag-ru"))

        // "DE" -> D (0x1F1E9), E (0x1F1EA)
        val deFlag = "\uD83C\uDDE9\uD83C\uDDEA"
        assertEquals(deFlag, getFlagEmoji("flag-de"))
    }

    @Test
    fun `invalid length returns question mark emoji`() {
        assertEquals("❓", getFlagEmoji(""))
        assertEquals("❓", getFlagEmoji("flag-"))
        assertEquals("❓", getFlagEmoji("flag-a"))
        assertEquals("❓", getFlagEmoji("flag-usa"))
        assertEquals("❓", getFlagEmoji("toolongcountryname"))
    }

    @Test
    fun `non-letter characters return question mark emoji without crashing`() {
        assertEquals("❓", getFlagEmoji("flag-12"))
        assertEquals("❓", getFlagEmoji("flag-a1"))
        assertEquals("❓", getFlagEmoji("flag-1a"))
        assertEquals("❓", getFlagEmoji("flag-??"))
        assertEquals("❓", getFlagEmoji("flag-!_"))
        assertEquals("❓", getFlagEmoji("flag-.."))
    }

    @Test
    fun `code without flag prefix but with 2 valid letters is parsed correctly`() {
        val gbFlag = "\uD83C\uDDEC\uD83C\uDDE7"
        assertEquals(gbFlag, getFlagEmoji("GB"))
        assertEquals(gbFlag, getFlagEmoji("gb"))
    }
}
