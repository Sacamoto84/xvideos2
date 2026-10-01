package com.client.xvideos.x.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ActressesIndexItemFlagTest {

    @Test
    fun `код страны превращается в флаг тем же способом, что и getFlagEmoji`() {
        assertEquals(getFlagEmoji("ru"), ActressesIndexItem(countryCode = "ru").flagEmoji)
        assertEquals(getFlagEmoji("us"), ActressesIndexItem(countryCode = " US ").flagEmoji)
    }

    @Test
    fun `нераспознанный код даёт пустую строку, а не заглушку`() {
        assertEquals("", ActressesIndexItem(countryCode = "").flagEmoji)
        assertEquals("", ActressesIndexItem(countryCode = "1x").flagEmoji)
        assertEquals("", ActressesIndexItem(countryCode = "rus").flagEmoji)
    }
}
