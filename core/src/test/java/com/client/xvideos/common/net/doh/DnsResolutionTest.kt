package com.client.xvideos.common.net.doh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

/**
 * Кто на самом деле нашёл имя.
 *
 * Диагностика DNS подписывала любой ответ названием выбранного провайдера DoH,
 * хотя при молчащем DoH отвечал системный DNS, а при повторной проверке — кэш.
 */
class DnsResolutionTest {

    private fun resolve(
        cached: List<String>? = null,
        fallbackToSystem: Boolean = true,
        doh: () -> DohOutcome<String> = { DohOutcome(emptyList(), null) },
        system: () -> List<String> = { listOf("system") },
    ) = resolveViaDoh(
        cached = cached,
        fallbackToSystem = fallbackToSystem,
        queryDoh = doh,
        systemLookup = system,
        onUnresolved = { cause -> throw UnknownHostException("нет ответа").apply { cause?.let(::initCause) } },
    )

    @Test
    fun `ответ из кэша помечен кэшем, DoH и системный DNS не опрашиваются`() {
        val asked = mutableListOf<String>()

        val result = resolve(
            cached = listOf("cached"),
            doh = { asked += "doh"; DohOutcome(listOf("doh"), null) },
            system = { asked += "system"; listOf("system") },
        )

        assertEquals(listOf("cached"), result.addresses)
        assertEquals(DnsSource.DOH_CACHE, result.source)
        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `ответ DoH помечен DoH`() {
        val result = resolve(doh = { DohOutcome(listOf("doh"), null) })

        assertEquals(listOf("doh"), result.addresses)
        assertEquals(DnsSource.DOH, result.source)
    }

    @Test
    fun `молчащий DoH при включённом откате — ответ системного DNS помечен откатом`() {
        val result = resolve(doh = { DohOutcome(emptyList(), IOException("timeout")) })

        assertEquals(listOf("system"), result.addresses)
        assertEquals(DnsSource.SYSTEM_FALLBACK, result.source)
    }

    @Test
    fun `молчащий DoH без отката — ошибка, системный DNS не опрашивается`() {
        var systemAsked = false

        assertThrows(UnknownHostException::class.java) {
            resolve(
                fallbackToSystem = false,
                doh = { DohOutcome(emptyList(), IOException("timeout")) },
                system = { systemAsked = true; listOf("system") },
            )
        }
        assertEquals(false, systemAsked)
    }
}
