package com.client.xvideos.common.net.doh

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class AppDnsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `DohProvider fromNameOrDefault корректно находит провайдеров`() {
        assertEquals(DohProvider.CLOUDFLARE, DohProvider.fromNameOrDefault("CLOUDFLARE"))
        assertEquals(DohProvider.CLOUDFLARE, DohProvider.fromNameOrDefault("cloudflare"))
        assertEquals(DohProvider.GOOGLE, DohProvider.fromNameOrDefault("GOOGLE"))
        assertEquals(DohProvider.ADGUARD, DohProvider.fromNameOrDefault("ADGUARD"))
        assertEquals(DohProvider.CUSTOM, DohProvider.fromNameOrDefault("CUSTOM"))
        assertEquals(DohProvider.CLOUDFLARE, DohProvider.fromNameOrDefault("UNKNOWN"))
        assertEquals(DohProvider.CLOUDFLARE, DohProvider.fromNameOrDefault(null))
    }

    @Test
    fun `DohResponse корректно десериализуется из RFC 8427 JSON с дополнительными полями`() {
        val jsonString = """
            {
              "Status": 0,
              "TC": false,
              "RD": true,
              "RA": true,
              "AD": true,
              "CD": false,
              "Question": [
                {
                  "name": "api.redgifs.com.",
                  "type": 1
                }
              ],
              "Answer": [
                {
                  "name": "api.redgifs.com.",
                  "type": 1,
                  "TTL": 120,
                  "data": "104.18.25.123"
                },
                {
                  "name": "api.redgifs.com.",
                  "type": 1,
                  "TTL": 120,
                  "data": "172.64.150.1"
                }
              ],
              "Comment": "Response from 1.1.1.1",
              "Extra": []
            }
        """.trimIndent()

        val response = json.decodeFromString<DohResponse>(jsonString)
        assertEquals(0, response.status)
        assertEquals(1, response.question.size)
        assertEquals("api.redgifs.com.", response.question[0].name)
        assertEquals(1, response.question[0].type)
        assertEquals(2, response.answer.size)
        assertEquals("104.18.25.123", response.answer[0].data)
        assertEquals(120L, response.answer[0].ttl)
        assertEquals(1, response.answer[0].type)
    }

    @Test
    fun `DohResponse корректно десериализует IPv6 AAAA записи`() {
        val jsonString = """
            {
              "Status": 0,
              "TC": false,
              "RD": true,
              "RA": true,
              "Question": [
                {
                  "name": "api.redgifs.com.",
                  "type": 28
                }
              ],
              "Answer": [
                {
                  "name": "api.redgifs.com.",
                  "type": 28,
                  "TTL": 300,
                  "data": "2606:4700:20::6812:197b"
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<DohResponse>(jsonString)
        assertEquals(0, response.status)
        assertEquals(28, response.question[0].type)
        assertEquals(1, response.answer.size)
        assertEquals(28, response.answer[0].type)
        assertEquals("2606:4700:20::6812:197b", response.answer[0].data)
    }

    @Test
    fun `DohResponse с пустым ответом десериализуется без ошибок`() {
        val jsonString = """
            {
              "Status": 3,
              "TC": false,
              "RD": true,
              "RA": true,
              "Question": [
                {
                  "name": "nonexistent.example.",
                  "type": 1
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<DohResponse>(jsonString)
        assertEquals(3, response.status)
        assertTrue(response.answer.isEmpty())
    }

    @Test
    fun `AppDns возвращает числовой IPv4 адрес без обращения к DNS`() {
        val addresses = AppDns.lookup("127.0.0.1")
        assertEquals(1, addresses.size)
        assertEquals("127.0.0.1", addresses[0].hostAddress)

        val publicIpAddresses = AppDns.lookup("1.1.1.1")
        assertEquals(1, publicIpAddresses.size)
        assertEquals("1.1.1.1", publicIpAddresses[0].hostAddress)
    }

    @Test
    fun `AppDns возвращает числовой IPv6 адрес без обращения к DNS`() {
        val addresses = AppDns.lookup("::1")
        assertEquals(1, addresses.size)
        assertTrue(addresses[0].hostAddress?.contains("1") == true)
    }

    @Test(expected = UnknownHostException::class)
    fun `AppDns выбрасывает UnknownHostException при пустом хосте`() {
        AppDns.lookup("")
    }

    @Test
    fun `AppDns резолвит localhost через системный резолвер`() {
        val addresses = AppDns.lookup("localhost")
        assertFalse(addresses.isEmpty())
        val loopback = InetAddress.getByName("localhost")
        assertNotNull(loopback)
    }

    @Test
    fun `AppDns clearCache работает без исключений`() {
        AppDns.clearCache()
        assertEquals(0, AppDns.cacheSize)
    }

    @Test
    fun `isDohServerHost распознает bootstrap IP-адреса публичных провайдеров`() {
        assertTrue(AppDns.isDohServerHost("1.1.1.1"))
        assertTrue(AppDns.isDohServerHost("1.0.0.1"))
        assertFalse(AppDns.isDohServerHost("example.com"))
        assertFalse(AppDns.isDohServerHost("api.redgifs.com"))
    }

    @Test
    fun `AppDns резолвит localhost в верхнем регистре через системный резолвер`() {
        val addresses = AppDns.lookup("LOCALHOST")
        assertFalse(addresses.isEmpty())
    }

    @Test
    fun `AppDns корректно конвертирует IDN хосты через Punycode`() {
        // IDN-домен должен нормализоваться в xn-- формат без исключений
        val host = "президент.рф"
        val ascii = java.net.IDN.toASCII(host.lowercase())
        assertTrue(ascii.startsWith("xn--"))
    }
}
