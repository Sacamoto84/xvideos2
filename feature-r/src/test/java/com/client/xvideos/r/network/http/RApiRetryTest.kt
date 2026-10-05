package com.client.xvideos.r.network.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Повторы запросов клиента R: только там, где следующая попытка может пройти.
 * Раньше повторялось любое исключение — без сети лента показывала ошибку после
 * четырёх попыток с паузами, а при зависшем сервере четыре таймаута по 30 секунд
 * давали около двух минут индикатора.
 */
class RApiRetryTest {

    private var requests = 0

    private fun client(answer: suspend MockRequestHandleScope.(attempt: Int) -> HttpResponseData) = HttpClient(
        MockEngine {
            requests++
            answer(requests)
        }
    ) {
        installRRetry(retryDelayMs = 1)
        expectSuccess = true
    }

    private fun attemptsOf(answer: suspend MockRequestHandleScope.(attempt: Int) -> HttpResponseData): Int {
        runBlocking { runCatching { client(answer).get("https://example.com/feed") } }
        return requests
    }

    @Test
    fun `без сети запрос не повторяется`() {
        assertEquals(1, attemptsOf { throw UnknownHostException("example.com") })
    }

    @Test
    fun `отказ в соединении не повторяется`() {
        assertEquals(1, attemptsOf { throw ConnectException("refused") })
    }

    @Test
    fun `таймаут не повторяется`() {
        assertEquals(1, attemptsOf { throw SocketTimeoutException("timeout") })
    }

    @Test
    fun `обрыв соединения повторяется, и запрос проходит`() = runBlocking {
        val client = client { attempt ->
            if (attempt == 1) throw IOException("Connection reset")
            respond("ok", HttpStatusCode.OK)
        }

        val body = client.get("https://example.com/feed").bodyAsText()

        assertEquals("ok", body)
        assertEquals(2, requests)
    }

    @Test
    fun `отказ шлюза повторяется ограниченное число раз`() {
        assertEquals(1 + R_MAX_RETRIES, attemptsOf { respond("", HttpStatusCode.BadGateway) })
    }

    @Test
    fun `ошибка приложения сервера не повторяется`() {
        assertEquals(1, attemptsOf { respond("", HttpStatusCode.InternalServerError) })
    }

    @Test
    fun `ошибка запроса не повторяется`() {
        assertEquals(1, attemptsOf { respond("", HttpStatusCode.NotFound) })
    }

    @Test
    fun `повторов меньше, чем было`() {
        assertTrue(R_MAX_RETRIES < 3)
    }
}
