package com.client.xvideos.screenSettings

import com.client.xvideos.common.net.doh.DohDiagnosticResult
import com.client.xvideos.screenSettings.model.dohDiagnosticsMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

/**
 * Текст итога проверки DNS.
 *
 * Проверка показывала зелёное «Cloudflare: …» и тогда, когда DoH молчал, а имена
 * нашёл системный DNS: неработающий DoH от работающего она не отличала.
 */
class DohDiagnosticsMessageTest {

    private fun found(ms: Long, usedFallback: Boolean = false) = Result.success(
        DohDiagnosticResult(
            host = "example.com",
            addresses = listOf("203.0.113.1"),
            elapsedMs = ms,
            providerTitle = "Cloudflare",
            isDoh = !usedFallback,
            usedFallback = usedFallback,
        )
    )

    private fun notFound() = Result.failure<DohDiagnosticResult>(UnknownHostException("нет"))

    @Test
    fun `все имена нашёл DoH — успех с названием провайдера`() {
        val message = dohDiagnosticsMessage(listOf("X" to found(40), "L" to found(35), "R" to found(20)))

        assertEquals("Cloudflare: X 40 мс, L 35 мс, R 20 мс", message.text)
        assertFalse(message.isError)
    }

    @Test
    fun `DoH молчал, имена нашёл системный DNS — это ошибка, а не успех провайдера`() {
        val message = dohDiagnosticsMessage(
            listOf("X" to found(40, usedFallback = true), "L" to found(35, usedFallback = true))
        )

        assertEquals("Cloudflare не ответил, имена нашёл системный DNS: X 40 мс, L 35 мс", message.text)
        assertTrue(message.isError)
    }

    @Test
    fun `часть имён нашёл системный DNS — помечены они, итог ошибка`() {
        val message = dohDiagnosticsMessage(
            listOf("X" to found(40), "L" to found(35, usedFallback = true), "R" to found(20))
        )

        assertEquals("Cloudflare: X 40 мс, L 35 мс через системный DNS, R 20 мс", message.text)
        assertTrue(message.isError)
    }

    @Test
    fun `ненайденное имя — ошибка`() {
        val message = dohDiagnosticsMessage(listOf("X" to found(40), "L" to notFound()))

        assertEquals("Cloudflare: X 40 мс, L не найден", message.text)
        assertTrue(message.isError)
    }

    @Test
    fun `ни одно имя не найдено — ошибка DNS без названия провайдера`() {
        val message = dohDiagnosticsMessage(listOf("X" to notFound(), "L" to notFound()))

        assertEquals("Ошибка DNS: X не найден, L не найден", message.text)
        assertTrue(message.isError)
    }
}
