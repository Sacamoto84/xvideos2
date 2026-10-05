package com.client.xvideos.common.net.doh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Свой адрес DoH проверяется до сохранения. Раньше сохранялся любой текст с
 * сообщением «сохранён»: адрес без схемы молча отбрасывался, а `http://`
 * принимался — DNS «по HTTPS» уходил открытым текстом.
 */
class DohUrlTest {

    @Test
    fun `https-адрес принимается и очищается от пробелов по краям`() {
        assertEquals("https://dns.example.com/dns-query", parseDohUrl("  https://dns.example.com/dns-query ").getOrNull())
    }

    @Test
    fun `открытый http отклоняется`() {
        val error = parseDohUrl("http://dns.example.com/dns-query").exceptionOrNull()

        assertTrue(error?.message.orEmpty(), error?.message.orEmpty().contains("https://"))
    }

    @Test
    fun `адрес без схемы отклоняется`() {
        assertNull(parseDohUrl("dns.example.com/dns-query").getOrNull())
    }

    @Test
    fun `адрес без имени сервера отклоняется`() {
        assertNull(parseDohUrl("https:///dns-query").getOrNull())
        assertNull(parseDohUrl("https://").getOrNull())
    }

    @Test
    fun `пустой адрес и текст с пробелом внутри отклоняются`() {
        assertNull(parseDohUrl("   ").getOrNull())
        assertNull(parseDohUrl("https://dns example.com/dns-query").getOrNull())
    }
}
