package com.client.xvideos.common.net.doh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Отказавший адрес DoH не опрашивается, пока не истечёт пауза: иначе каждый
 * поиск имени заново ждал бы таймаут на адресе, который не отвечает, и до
 * отката на системный DNS запрос не доживал.
 */
class DohEndpointPolicyTest {

    private var now = 1_000L
    private val health = DohEndpointHealth(retryAfterMs = 30_000L, nowMs = { now })
    private val first = "https://first.example/dns-query"
    private val second = "https://second.example/dns-query"
    private val endpoints = listOf(first, second)
    private val asked = mutableListOf<String>()

    private val allFail: (String) -> List<String> = { endpoint ->
        asked += endpoint
        throw IOException("нет связи с $endpoint")
    }

    private val onlySecondAnswers: (String) -> List<String> = { endpoint ->
        asked += endpoint
        if (endpoint == first) throw IOException("нет связи") else listOf("1.2.3.4")
    }

    @Test
    fun `ответ первого адреса не трогает второй`() {
        val outcome = queryDohEndpoints(endpoints, health, skipDown = true) { endpoint ->
            asked += endpoint
            listOf("1.2.3.4")
        }

        assertEquals(listOf("1.2.3.4"), outcome.addresses)
        assertEquals(listOf(first), asked)
    }

    @Test
    fun `все адреса отказали — следующий поиск их не опрашивает`() {
        queryDohEndpoints(endpoints, health, skipDown = true, query = allFail)
        asked.clear()

        val outcome = queryDohEndpoints(endpoints, health, skipDown = true, query = allFail)

        assertTrue("адреса опрошены повторно: $asked", asked.isEmpty())
        assertTrue(outcome.addresses.isEmpty())
    }

    @Test
    fun `отказавший первый адрес пропускается, пока не истекла пауза`() {
        queryDohEndpoints(endpoints, health, skipDown = true, query = onlySecondAnswers)
        asked.clear()
        now += 29_999L

        val outcome = queryDohEndpoints(endpoints, health, skipDown = true, query = onlySecondAnswers)

        assertEquals(listOf(second), asked)
        assertEquals(listOf("1.2.3.4"), outcome.addresses)
    }

    @Test
    fun `после паузы отказавший адрес опрашивается снова`() {
        queryDohEndpoints(endpoints, health, skipDown = true, query = onlySecondAnswers)
        asked.clear()
        now += 30_000L

        queryDohEndpoints(endpoints, health, skipDown = true, query = onlySecondAnswers)

        assertEquals(listOf(first, second), asked)
    }

    @Test
    fun `без отката на системный DNS отказавшие адреса опрашиваются всё равно`() {
        queryDohEndpoints(endpoints, health, skipDown = false, query = allFail)
        asked.clear()

        val outcome = queryDohEndpoints(endpoints, health, skipDown = false, query = allFail)

        assertEquals(endpoints, asked)
        assertTrue("ожидалась ошибка последнего адреса, пришло ${outcome.error}", outcome.error is IOException)
    }

    @Test
    fun `пустой ответ не считается отказом адреса`() {
        val noRecords: (String) -> List<String> = { endpoint ->
            asked += endpoint
            emptyList()
        }
        queryDohEndpoints(endpoints, health, skipDown = true, query = noRecords)
        asked.clear()

        queryDohEndpoints(endpoints, health, skipDown = true, query = noRecords)

        assertEquals(endpoints, asked)
    }
}
